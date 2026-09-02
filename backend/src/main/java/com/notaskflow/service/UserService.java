package com.notaskflow.service;

import com.notaskflow.domain.dto.request.PasswordUpdateRequest;
import com.notaskflow.domain.dto.request.EmailChangeCodeRequest;
import com.notaskflow.domain.dto.request.EmailChangeConfirmRequest;
import com.notaskflow.domain.dto.request.ProfileUpdateRequest;
import com.notaskflow.domain.vo.UserOptionVO;
import com.notaskflow.domain.vo.UserProfileVO;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户服务接口。
 *
 * @author LIN
 */
public interface UserService {

    /**
     * 获取当前用户资料。
     *
     * @return 当前用户资料
     */
    UserProfileVO profile();

    /**
     * 更新当前用户资料。
     *
     * @param request 更新请求
     * @return 用户资料
     */
    UserProfileVO updateProfile(ProfileUpdateRequest request);

    /**
     * 发送邮箱修改验证码到当前旧邮箱。
     *
     * @param request 验证码发送请求
     */
    void sendEmailChangeCode(EmailChangeCodeRequest request);

    /**
     * 校验旧邮箱验证码并修改邮箱。
     *
     * @param request 邮箱修改确认请求
     * @return 用户资料
     */
    UserProfileVO changeEmail(EmailChangeConfirmRequest request);

    /**
     * 修改当前用户密码。
     *
     * @param request 密码修改请求
     */
    void changePassword(PasswordUpdateRequest request);

    /**
     * 上传当前用户头像。
     *
     * @param file 头像文件
     * @return 用户资料
     */
    UserProfileVO uploadAvatar(MultipartFile file);

    /**
     * 输出当前用户头像流。
     *
     * @param outputStream 输出流
     * @throws IOException 输出失败
     */
    void writeCurrentUserAvatar(OutputStream outputStream) throws IOException;

    /**
     * 输出指定用户头像流。
     *
     * @param userId 用户标识
     * @param outputStream 输出流
     * @throws IOException 输出失败
     */
    void writeUserAvatar(Long userId, OutputStream outputStream) throws IOException;

    /**
     * 查询头像内容类型。
     *
     * @param userId 用户标识
     * @return 内容类型
     */
    String avatarContentType(Long userId);

    /**
     * 根据关键字搜索用户。
     *
     * @param keyword 用户名或邮箱关键字
     * @return 用户选择项列表
     */
    List<UserOptionVO> search(String keyword);
}
