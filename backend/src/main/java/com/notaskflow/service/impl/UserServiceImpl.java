package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notaskflow.common.constant.RedisKeyConstants;
import com.notaskflow.domain.dto.request.EmailChangeCodeRequest;
import com.notaskflow.domain.dto.request.EmailChangeConfirmRequest;
import com.notaskflow.domain.dto.request.PasswordUpdateRequest;
import com.notaskflow.domain.dto.request.ProfileUpdateRequest;
import com.notaskflow.domain.entity.User;
import com.notaskflow.domain.vo.UserOptionVO;
import com.notaskflow.domain.vo.UserProfileVO;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import com.notaskflow.exception.ResourceNotFoundException;
import com.notaskflow.mapper.UserMapper;
import com.notaskflow.service.MailNotificationService;
import com.notaskflow.service.UserService;
import com.notaskflow.storage.MinioStorageService;
import com.notaskflow.utils.AvatarUrlUtil;
import com.notaskflow.utils.LoginUserUtil;
import com.notaskflow.utils.RedisUtil;
import com.notaskflow.utils.StringGenerator;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户服务实现，处理当前用户资料、密码和头像。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int EMAIL_CHANGE_CODE_LENGTH = 6;

    private static final int EMAIL_CODE_RATE_LIMIT = 5;

    private static final Duration EMAIL_CHANGE_CODE_TTL = Duration.ofMinutes(10);

    private static final Duration EMAIL_CODE_RATE_LIMIT_WINDOW = Duration.ofMinutes(5);

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final MinioStorageService minioStorageService;

    private final RedisUtil redisUtil;

    private final MailNotificationService mailNotificationService;

    /**
     * 查询当前用户资料。
     *
     * @return 用户资料
     */
    @Override
    public UserProfileVO profile() {
        return toUserProfileVO(findCurrentUser());
    }

    /**
     * 更新当前用户资料。
     *
     * @param request 资料更新请求
     * @return 用户资料
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO updateProfile(ProfileUpdateRequest request) {
        User user = findCurrentUser();
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname().trim());
        }
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "修改邮箱请先完成旧邮箱验证码校验");
        }
        if (StringUtils.hasText(request.getAvatarUrl())) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        userMapper.updateById(user);
        log.info("用户资料更新完成，userId={}", user.getId());
        return toUserProfileVO(user);
    }

    /**
     * 发送邮箱修改验证码到当前旧邮箱。
     *
     * @param request 验证码发送请求
     */
    @Override
    public void sendEmailChangeCode(EmailChangeCodeRequest request) {
        User user = findCurrentUser();
        if (!StringUtils.hasText(user.getEmail())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前账号未绑定邮箱，无法验证身份");
        }

        String newEmail = normalizeEmail(request.getNewEmail());
        ensureEmailCanBeChanged(user, newEmail);
        redisUtil.limit(
                RedisKeyConstants.rateLimit("user-email-change", user.getId() + ":" + newEmail),
                EMAIL_CODE_RATE_LIMIT,
                EMAIL_CODE_RATE_LIMIT_WINDOW,
                "验证码发送过于频繁，请稍后再试"
        );

        String code = StringGenerator.randomNumeric(EMAIL_CHANGE_CODE_LENGTH);
        String codeKey = buildEmailChangeCodeKey(user.getId(), newEmail);
        redisUtil.delete(codeKey);
        redisUtil.set(codeKey, code, EMAIL_CHANGE_CODE_TTL);

        boolean mailSent = mailNotificationService.sendEmailChangeCodeMail(user.getEmail(), code, newEmail);
        if (!mailSent) {
            redisUtil.delete(codeKey);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "邮件服务不可用，请稍后再试");
        }
        log.info("邮箱修改验证码发送完成，userId={}, newEmail={}, expireMinutes={}",
                user.getId(), newEmail, EMAIL_CHANGE_CODE_TTL.toMinutes());
    }

    /**
     * 校验旧邮箱验证码并修改邮箱。
     *
     * @param request 邮箱修改确认请求
     * @return 用户资料
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO changeEmail(EmailChangeConfirmRequest request) {
        User user = findCurrentUser();
        String newEmail = normalizeEmail(request.getNewEmail());
        ensureEmailCanBeChanged(user, newEmail);

        String codeKey = buildEmailChangeCodeKey(user.getId(), newEmail);
        String cachedCode = redisUtil.getString(codeKey);
        if (!StringUtils.hasText(cachedCode) || !cachedCode.equals(request.getCode().trim())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码错误或已过期");
        }

        user.setEmail(newEmail);
        userMapper.updateById(user);
        redisUtil.delete(codeKey);
        log.info("用户邮箱修改完成，userId={}, newEmail={}", user.getId(), newEmail);
        return toUserProfileVO(user);
    }

    /**
     * 修改当前用户密码。
     *
     * @param request 密码修改请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(PasswordUpdateRequest request) {
        User user = findCurrentUser();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "旧密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        log.info("用户密码修改完成，userId={}", user.getId());
    }

    /**
     * 上传当前用户头像。
     *
     * @param file 头像文件
     * @return 用户资料
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "头像文件不能为空");
        }
        User user = findCurrentUser();
        String storagePath = minioStorageService.uploadAvatar(user.getId(), file);
        user.setAvatarUrl(storagePath);
        userMapper.updateById(user);
        log.info("用户头像上传完成，userId={}", user.getId());
        return toUserProfileVO(user);
    }

    /**
     * 输出当前用户头像流。
     *
     * @param outputStream 输出流
     * @throws IOException 输出失败
     */
    @Override
    public void writeCurrentUserAvatar(OutputStream outputStream) throws IOException {
        writeAvatar(findCurrentUser(), outputStream);
    }

    /**
     * 输出指定用户头像流。
     *
     * @param userId 用户标识
     * @param outputStream 输出流
     * @throws IOException 输出失败
     */
    @Override
    public void writeUserAvatar(Long userId, OutputStream outputStream) throws IOException {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }
        writeAvatar(user, outputStream);
    }

    private void writeAvatar(User user, OutputStream outputStream) throws IOException {
        String avatarUrl = user.getAvatarUrl();
        if (!StringUtils.hasText(avatarUrl)) {
            throw new ResourceNotFoundException("头像不存在");
        }
        try (InputStream inputStream = minioStorageService.openObject(avatarUrl)) {
            inputStream.transferTo(outputStream);
        }
    }

    /**
     * 查询头像内容类型。
     *
     * @param userId 用户标识
     * @return 内容类型
     */
    @Override
    public String avatarContentType(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !StringUtils.hasText(user.getAvatarUrl())) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return minioStorageService.statObject(user.getAvatarUrl()).contentType();
    }

    /**
     * 根据用户名或邮箱关键字搜索用户。
     *
     * @param keyword 用户名或邮箱关键字
     * @return 用户选择项列表
     */
    @Override
    public List<UserOptionVO> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        String normalizedKeyword = keyword.trim();
        Page<User> page = new Page<>(1, 20, false);
        return userMapper.selectPage(page, Wrappers.<User>lambdaQuery()
                        .and(wrapper -> wrapper.like(User::getUsername, normalizedKeyword)
                                .or()
                                .like(User::getNickname, normalizedKeyword)
                                .or()
                                .like(User::getEmail, normalizedKeyword)))
                .getRecords()
                .stream()
                .map(this::toUserOptionVO)
                .toList();
    }

    /**
     * 查询当前用户实体。
     *
     * @return 当前用户实体
     */
    private User findCurrentUser() {
        User user = userMapper.selectById(LoginUserUtil.currentUserId());
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }
        return user;
    }

    /**
     * 校验邮箱是否唯一。
     *
     * @param email 邮箱
     */
    private void validateUniqueEmail(String email, Long currentUserId) {
        Long count = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getEmail, email)
                .ne(currentUserId != null, User::getId, currentUserId));
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已存在");
        }
    }

    /**
     * 校验邮箱修改目标。
     *
     * @param user 当前用户
     * @param newEmail 新邮箱
     */
    private void ensureEmailCanBeChanged(User user, String newEmail) {
        if (!StringUtils.hasText(newEmail)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新邮箱不能为空");
        }
        String currentEmail = normalizeEmail(user.getEmail());
        if (newEmail.equals(currentEmail)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新邮箱不能与当前邮箱相同");
        }
        validateUniqueEmail(newEmail, user.getId());
    }

    /**
     * 构造邮箱修改验证码缓存键。
     *
     * @param userId 用户标识
     * @param newEmail 规范化新邮箱
     * @return Redis 键
     */
    private String buildEmailChangeCodeKey(Long userId, String newEmail) {
        return RedisKeyConstants.USER_EMAIL_CHANGE_CODE_PREFIX + userId + ":" + newEmail;
    }

    /**
     * 规范化邮箱。
     *
     * @param email 原始邮箱
     * @return 规范化邮箱
     */
    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 转换用户资料视图对象。
     *
     * @param user 用户实体
     * @return 用户资料
     */
    private UserProfileVO toUserProfileVO(User user) {
        return new UserProfileVO(user.getId(), user.getUsername(), user.getNickname(), user.getEmail(),
                AvatarUrlUtil.proxyUrl(user),
                user.getGmtCreate());
    }

    /**
     * 转换用户选择项视图对象。
     *
     * @param user 用户实体
     * @return 用户选择项
     */
    private UserOptionVO toUserOptionVO(User user) {
        return new UserOptionVO(user.getId(), user.getUsername(), user.getNickname(), user.getEmail(),
                AvatarUrlUtil.proxyUrl(user));
    }
}
