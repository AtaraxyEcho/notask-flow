package com.notaskflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.notaskflow.common.enums.BusinessType;
import com.notaskflow.common.enums.NotificationType;
import com.notaskflow.config.MailNotificationProperties;
import com.notaskflow.domain.entity.NotificationSetting;
import com.notaskflow.domain.entity.User;
import com.notaskflow.mapper.NotificationSettingMapper;
import com.notaskflow.mapper.UserMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 邮件通知服务。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailNotificationService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    private final MailNotificationProperties properties;

    private final SystemSettingService systemSettingService;

    private final NotificationSettingMapper notificationSettingMapper;

    private final UserMapper userMapper;

    /**
     * 根据用户通知偏好发送邮件通知。
     *
     * @param userId 用户标识
     * @param type 通知类型
     * @param businessType 业务类型
     * @param title 通知标题
     * @param content 通知内容
     * @return 是否发送成功或无需发送
     */
    public boolean sendIfNecessary(Long userId, NotificationType type, BusinessType businessType, String title,
                                   String content) {
        if (!systemSettingService.isMailEnabled()) {
            return true;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("邮件通知已启用但未配置邮件发送器，userId={}", userId);
            return false;
        }
        NotificationSetting setting = notificationSettingMapper.selectOne(Wrappers.<NotificationSetting>lambdaQuery()
                .eq(NotificationSetting::getUserId, userId));
        if (!shouldSend(setting, type, businessType)) {
            return true;
        }
        User user = userMapper.selectById(userId);
        if (user == null || !StringUtils.hasText(user.getEmail())) {
            log.warn("邮件通知接收用户不存在或邮箱为空，userId={}", userId);
            return true;
        }

        String displayName = resolveDisplayName(user);
        String normalizedTitle = StringUtils.hasText(title) ? title.trim() : "你有一条新的通知";
        String normalizedContent = StringUtils.hasText(content) ? content.trim() : normalizedTitle;
        String subject = "[Notask Flow] " + normalizedTitle;
        String html = buildNotificationMailHtml(displayName, type, businessType, normalizedTitle, normalizedContent);
        boolean sent = sendHtmlMail(user.getEmail(), subject, html);
        if (sent) {
            log.info("邮件通知发送完成，userId={}, type={}", userId, type);
            return true;
        }
        log.error("邮件通知发送失败，userId={}, type={}", userId, type);
        return false;
    }

    /**
     * 发送邮箱通知功能开启确认邮件。
     *
     * @param email 接收邮箱
     * @param displayName 用户显示名称
     * @return 是否发送成功
     */
    public boolean sendNotificationEnabledMail(String email, String displayName) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return sendHtmlMail(email, "[Notask Flow] 邮件通知已开启", buildNotificationEnabledMailHtml(displayName));
    }

    /**
     * 发送找回密码验证码邮件。
     *
     * @param email 接收邮箱
     * @param code 验证码
     * @return 是否发送成功
     */
    public boolean sendPasswordResetCodeMail(String email, String code) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(code)) {
            return false;
        }
        return sendHtmlMail(email, "[Notask Flow] 找回密码验证码", buildPasswordResetCodeHtml(code));
    }

    /**
     * 发送注册邮箱验证码邮件。
     *
     * @param email 接收邮箱
     * @param code 验证码
     * @return 是否发送成功
     */
    public boolean sendRegisterCodeMail(String email, String code) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(code)) {
            return false;
        }
        return sendHtmlMail(email, "[Notask Flow] 注册邮箱验证码", buildRegisterCodeHtml(code));
    }

    /**
     * 发送修改邮箱验证码邮件。
     *
     * @param email 当前旧邮箱
     * @param code 验证码
     * @param newEmail 新邮箱
     * @return 是否发送成功
     */
    public boolean sendEmailChangeCodeMail(String email, String code, String newEmail) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(code) || !StringUtils.hasText(newEmail)) {
            return false;
        }
        return sendHtmlMail(email, "[Notask Flow] 修改邮箱验证码", buildEmailChangeCodeHtml(code, newEmail));
    }

    /**
     * 判断用户偏好是否允许发送邮件。
     *
     * @param setting 通知偏好
     * @param type 通知类型
     * @param businessType 业务类型
     * @return 是否允许发送
     */
    private boolean shouldSend(NotificationSetting setting, NotificationType type, BusinessType businessType) {
        if (setting == null || !Boolean.TRUE.equals(setting.getEmailEnabled())) {
            return false;
        }
        if (Boolean.TRUE.equals(setting.getQuietEnabled()) && inQuietHours(setting)) {
            return false;
        }
        if (NotificationType.COMMENT_MENTIONED.equals(type)) {
            return Boolean.TRUE.equals(setting.getMentionEmailEnabled());
        }
        if (BusinessType.TODO.equals(businessType)) {
            return Boolean.TRUE.equals(setting.getTodoEmailEnabled());
        }
        if (BusinessType.TASK.equals(businessType)) {
            return Boolean.TRUE.equals(setting.getTaskEmailEnabled());
        }
        if (BusinessType.SPACE_JOIN_REQUEST.equals(businessType)) {
            return true;
        }
        return false;
    }

    /**
     * 判断当前时间是否处于免打扰时段。
     *
     * @param setting 通知偏好
     * @return 是否处于免打扰时段
     */
    private boolean inQuietHours(NotificationSetting setting) {
        if (!StringUtils.hasText(setting.getQuietStartTime()) || !StringUtils.hasText(setting.getQuietEndTime())) {
            return false;
        }
        try {
            LocalTime now = LocalTime.now();
            LocalTime start = LocalTime.parse(setting.getQuietStartTime(), TIME_FORMATTER);
            LocalTime end = LocalTime.parse(setting.getQuietEndTime(), TIME_FORMATTER);
            if (start.equals(end)) {
                return false;
            }
            if (start.isBefore(end)) {
                return !now.isBefore(start) && now.isBefore(end);
            }
            return !now.isBefore(start) || now.isBefore(end);
        } catch (DateTimeParseException exception) {
            log.warn("免打扰时间格式异常，settingId={}", setting.getId(), exception);
            return false;
        }
    }

    /**
     * 发送 HTML 邮件。
     *
     * @param email 接收邮箱
     * @param subject 邮件主题
     * @param htmlContent 邮件内容
     * @return 是否发送成功
     */
    private boolean sendHtmlMail(String email, String subject, String htmlContent) {
        if (!systemSettingService.isMailEnabled()) {
            log.info("系统邮件发送已关闭，跳过邮件发送，email={}", email);
            return false;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("邮件发送器未配置，email={}", email);
            return false;
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
            if (StringUtils.hasText(properties.getFrom())) {
                helper.setFrom(properties.getFrom());
            }
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("HTML 邮件发送完成，email={}, subject={}", email, subject);
            return true;
        } catch (MailException | MessagingException exception) {
            log.error("HTML 邮件发送失败，email={}, subject={}", email, subject, exception);
            return false;
        }
    }

    /**
     * 构建通知邮件 HTML。
     *
     * @param displayName 用户显示名称
     * @param type 通知类型
     * @param businessType 业务类型
     * @param title 通知标题
     * @param content 通知内容
     * @return HTML 字符串
     */
    private String buildNotificationMailHtml(String displayName, NotificationType type, BusinessType businessType,
                                             String title, String content) {
        String body = """
                <div style="margin-top:24px;border-radius:20px;background:#ffffff;border:1px solid #ead3cc;padding:22px 24px;">
                  <div style="font-size:12px;letter-spacing:0.2em;text-transform:uppercase;color:#9f4122;font-weight:700;">
                    %s
                  </div>
                  <div style="margin-top:10px;font-family:'Newsreader','Times New Roman',serif;font-size:28px;line-height:1.3;color:#1e1b19;">
                    %s
                  </div>
                  <div style="margin-top:16px;font-size:15px;line-height:1.9;color:#56423c;">
                    %s
                  </div>
                </div>
                <div style="margin-top:18px;display:flex;gap:12px;flex-wrap:wrap;">
                  <div style="flex:1;min-width:160px;border-radius:18px;background:rgba(255,255,255,0.72);padding:16px;border:1px solid #ead3cc;">
                    <div style="font-size:12px;letter-spacing:0.18em;text-transform:uppercase;color:#89726b;font-weight:700;">通知类型</div>
                    <div style="margin-top:8px;font-size:15px;color:#1e1b19;">%s</div>
                  </div>
                  <div style="flex:1;min-width:160px;border-radius:18px;background:rgba(255,255,255,0.72);padding:16px;border:1px solid #ead3cc;">
                    <div style="font-size:12px;letter-spacing:0.18em;text-transform:uppercase;color:#89726b;font-weight:700;">业务模块</div>
                    <div style="margin-top:8px;font-size:15px;color:#1e1b19;">%s</div>
                  </div>
                </div>
                """.formatted(escapeHtml(title), escapeHtml(title), escapeHtml(content),
                escapeHtml(resolveNotificationLabel(type)), escapeHtml(resolveBusinessLabel(businessType)));

        return wrapMailCard(
                "新的协作动态已送达",
                "你好，%s。你在 Notask Flow 中有一条新的协作提醒，请及时查看。".formatted(escapeHtml(displayName)),
                "NF",
                body,
                "如果这不是你主动订阅的通知，请回到个人设置中检查邮件通知开关与免打扰时段。"
        );
    }

    /**
     * 构建邮件通知已开启确认邮件 HTML。
     *
     * @param displayName 用户显示名称
     * @return HTML 字符串
     */
    private String buildNotificationEnabledMailHtml(String displayName) {
        String body = """
                <div style="margin-top:24px;border-radius:20px;background:#ffffff;border:1px solid #ead3cc;padding:22px 24px;">
                  <div style="font-size:12px;letter-spacing:0.2em;text-transform:uppercase;color:#9f4122;font-weight:700;">
                    邮件通知已启用
                  </div>
                  <div style="margin-top:10px;font-family:'Newsreader','Times New Roman',serif;font-size:28px;line-height:1.3;color:#1e1b19;">
                    现在开始，你的重要动态会同步送达邮箱
                  </div>
                  <div style="margin-top:16px;font-size:15px;line-height:1.9;color:#56423c;">
                    你好，%s。你已在个人设置中开启邮件通知。后续任务提醒、待办提醒和提及消息将按照你的偏好发送到当前邮箱。
                  </div>
                </div>
                <div style="margin-top:18px;border-radius:18px;background:rgba(255,255,255,0.72);padding:16px;border:1px solid #ead3cc;">
                  <div style="font-size:12px;letter-spacing:0.18em;text-transform:uppercase;color:#89726b;font-weight:700;">建议检查</div>
                  <div style="margin-top:8px;font-size:15px;line-height:1.8;color:#1e1b19;">
                    1. 确认邮箱地址真实可用。<br/>
                    2. 根据需要开启或关闭免打扰时段。<br/>
                    3. 按模块调整任务、待办和提及邮件通知粒度。
                  </div>
                </div>
                """.formatted(escapeHtml(displayName));

        return wrapMailCard(
                "邮件通知配置已生效",
                "这是一封来自 Notask Flow 的确认邮件，用来帮助你验证当前 SMTP 链路与邮箱收件能力。",
                "OK",
                body,
                "如果你并未进行此操作，请尽快登录账户检查安全设置。"
        );
    }

    /**
     * 构建找回密码验证码邮件 HTML。
     *
     * @param code 验证码
     * @return HTML 字符串
     */
    private String buildPasswordResetCodeHtml(String code) {
        String body = """
                <div style="margin-top:24px;border-radius:20px;background:#ffffff;border:1px solid #ead3cc;padding:18px 20px;text-align:center;">
                  <div style="margin-bottom:10px;font-size:12px;letter-spacing:0.24em;text-transform:uppercase;color:#9f4122;font-weight:700;">
                    Verification Code
                  </div>
                  <div style="font-family:'Newsreader','Times New Roman',serif;font-size:36px;letter-spacing:0.48em;color:#752305;font-weight:700;padding-left:0.48em;">
                    %s
                  </div>
                </div>
                <div style="margin-top:18px;display:flex;gap:12px;flex-wrap:wrap;">
                  <div style="flex:1;min-width:180px;border-radius:18px;background:rgba(255,255,255,0.72);padding:16px;border:1px solid #ead3cc;">
                    <div style="font-size:12px;letter-spacing:0.18em;text-transform:uppercase;color:#89726b;font-weight:700;">有效时间</div>
                    <div style="margin-top:8px;font-size:15px;color:#1e1b19;">10 分钟内有效</div>
                  </div>
                  <div style="flex:1;min-width:180px;border-radius:18px;background:rgba(255,255,255,0.72);padding:16px;border:1px solid #ead3cc;">
                    <div style="font-size:12px;letter-spacing:0.18em;text-transform:uppercase;color:#89726b;font-weight:700;">安全提示</div>
                    <div style="margin-top:8px;font-size:15px;color:#1e1b19;">请勿将验证码透露给他人</div>
                  </div>
                </div>
                """.formatted(escapeHtml(code));

        return wrapMailCard(
                "Verify Your Email",
                "你正在进行 Notask Flow 密码找回操作。请输入下方 6 位验证码，继续完成身份校验。",
                "NF",
                body,
                "如非本人操作，请忽略这封邮件，你的账户不会被修改。"
        );
    }

    /**
     * 构建注册验证码邮件 HTML。
     *
     * @param code 验证码
     * @return HTML 字符串
     */
    private String buildRegisterCodeHtml(String code) {
        String body = """
                <div style="margin-top:24px;border-radius:20px;background:#ffffff;border:1px solid #ead3cc;padding:18px 20px;text-align:center;">
                  <div style="margin-bottom:10px;font-size:12px;letter-spacing:0.24em;text-transform:uppercase;color:#9f4122;font-weight:700;">
                    Verification Code
                  </div>
                  <div style="font-family:'Newsreader','Times New Roman',serif;font-size:36px;letter-spacing:0.48em;color:#752305;font-weight:700;padding-left:0.48em;">
                    %s
                  </div>
                </div>
                <div style="margin-top:18px;display:flex;gap:12px;flex-wrap:wrap;">
                  <div style="flex:1;min-width:180px;border-radius:18px;background:rgba(255,255,255,0.72);padding:16px;border:1px solid #ead3cc;">
                    <div style="font-size:12px;letter-spacing:0.18em;text-transform:uppercase;color:#89726b;font-weight:700;">有效时间</div>
                    <div style="margin-top:8px;font-size:15px;color:#1e1b19;">10 分钟内有效</div>
                  </div>
                  <div style="flex:1;min-width:180px;border-radius:18px;background:rgba(255,255,255,0.72);padding:16px;border:1px solid #ead3cc;">
                    <div style="font-size:12px;letter-spacing:0.18em;text-transform:uppercase;color:#89726b;font-weight:700;">安全提示</div>
                    <div style="margin-top:8px;font-size:15px;color:#1e1b19;">请确认邮箱真实可用，勿向他人泄露验证码</div>
                  </div>
                </div>
                """.formatted(escapeHtml(code));

        return wrapMailCard(
                "Complete Your Sign Up",
                "你正在注册 Notask Flow 账号。请输入下方 6 位验证码，确认该邮箱真实可用后再继续完成注册。",
                "NF",
                body,
                "如非本人操作，请忽略这封邮件。只有通过邮箱验证码验证后，才可以继续创建 Notask Flow 账户。"
        );
    }

    /**
     * 构建修改邮箱验证码邮件 HTML。
     *
     * @param code 验证码
     * @param newEmail 新邮箱
     * @return HTML 字符串
     */
    private String buildEmailChangeCodeHtml(String code, String newEmail) {
        String body = """
                <div style="margin-top:24px;border-radius:20px;background:#ffffff;border:1px solid #ead3cc;padding:18px 20px;text-align:center;">
                  <div style="margin-bottom:10px;font-size:12px;letter-spacing:0.24em;text-transform:uppercase;color:#9f4122;font-weight:700;">
                    Verification Code
                  </div>
                  <div style="font-family:'Newsreader','Times New Roman',serif;font-size:36px;letter-spacing:0.48em;color:#752305;font-weight:700;padding-left:0.48em;">
                    %s
                  </div>
                </div>
                <div style="margin-top:18px;border-radius:18px;background:rgba(255,255,255,0.72);padding:16px;border:1px solid #ead3cc;">
                  <div style="font-size:12px;letter-spacing:0.18em;text-transform:uppercase;color:#89726b;font-weight:700;">目标邮箱</div>
                  <div style="margin-top:8px;font-size:15px;line-height:1.8;color:#1e1b19;">
                    验证通过后，你的账户邮箱将修改为：%s
                  </div>
                </div>
                <div style="margin-top:18px;border-radius:18px;background:rgba(255,255,255,0.72);padding:16px;border:1px solid #ead3cc;">
                  <div style="font-size:12px;letter-spacing:0.18em;text-transform:uppercase;color:#89726b;font-weight:700;">有效时间</div>
                  <div style="margin-top:8px;font-size:15px;color:#1e1b19;">10 分钟内有效，请勿将验证码透露给他人</div>
                </div>
                """.formatted(escapeHtml(code), escapeHtml(newEmail));

        return wrapMailCard(
                "Confirm Email Change",
                "你正在修改 Notask Flow 账户邮箱。请输入下方 6 位验证码，确认这是本人操作。",
                "NF",
                body,
                "如非本人操作，请立即修改密码并检查账户安全。"
        );
    }

    /**
     * 构建统一主题邮件壳。
     *
     * @param heading 主标题
     * @param description 描述文案
     * @param heroText 顶部图标文案
     * @param bodyHtml 主体内容
     * @param footerText 底部说明
     * @return HTML 字符串
     */
    private String wrapMailCard(String heading, String description, String heroText, String bodyHtml, String footerText) {
        return """
            <div style="margin:0;padding:32px 0;background:#fff8f6;font-family:'Plus Jakarta Sans','Segoe UI',Arial,sans-serif;color:#1e1b19;">
              <div style="max-width:560px;margin:0 auto;padding:0 20px;">
                <div style="background:linear-gradient(135deg,#fff8f6 0%%,#f5ece9 58%%,#ffdbd0 100%%);border-radius:28px;padding:28px;box-shadow:0 18px 50px rgba(0,0,0,0.06);border:1px solid #f0d7cf;">
                  <div style="margin:0 auto 18px;width:72px;height:72px;border-radius:999px;background:#ffdbd0;display:flex;align-items:center;justify-content:center;font-size:22px;font-weight:700;line-height:72px;color:#752305;">
                    %s
                  </div>
                  <div style="text-align:center;">
                    <div style="font-family:'Newsreader','Times New Roman',serif;font-size:32px;line-height:1.2;color:#1e1b19;">%s</div>
                    <p style="margin:14px 0 0;color:#56423c;font-size:15px;line-height:1.8;">%s</p>
                  </div>
                  %s
                  <div style="margin-top:24px;padding-top:18px;border-top:1px solid #e3c8c0;text-align:center;color:#6a5650;font-size:13px;line-height:1.8;">
                    %s<br/>
                    Notask Flow 仅会通过官方邮箱向你发送验证与协作通知。
                  </div>
                </div>
              </div>
            </div>
            """.formatted(escapeHtml(heroText), escapeHtml(heading), description, bodyHtml, escapeHtml(footerText));
    }

    /**
     * 解析通知类型文案。
     *
     * @param type 通知类型
     * @return 文案
     */
    private String resolveNotificationLabel(NotificationType type) {
        if (type == null) {
            return "通用提醒";
        }
        return switch (type) {
            case TASK_CREATED -> "任务创建";
            case TASK_CLAIMED -> "任务认领";
            case TASK_MEMBER_COMPLETED -> "成员完成";
            case TASK_COMPLETED -> "任务完成";
            case TODO_CREATED -> "待办生成";
            case COMMENT_MENTIONED -> "评论提及";
            case SPACE_JOIN_APPLIED -> "加入申请";
            case SPACE_JOIN_APPROVED -> "申请通过";
            case SPACE_JOIN_REJECTED -> "申请拒绝";
            case SYSTEM_ANNOUNCEMENT -> "系统公告";
        };
    }

    /**
     * 解析业务类型文案。
     *
     * @param businessType 业务类型
     * @return 文案
     */
    private String resolveBusinessLabel(BusinessType businessType) {
        if (businessType == null) {
            return "通用模块";
        }
        return switch (businessType) {
            case NOTE -> "笔记";
            case TASK -> "任务";
            case TODO -> "待办";
            case SPACE_JOIN_REQUEST -> "团队加入申请";
            case SYSTEM -> "系统通知";
        };
    }

    /**
     * 解析显示名称。
     *
     * @param user 用户实体
     * @return 显示名称
     */
    private String resolveDisplayName(User user) {
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return user.getEmail();
    }

    /**
     * 转义邮件中的 HTML 文本。
     *
     * @param source 原始文本
     * @return 转义后的文本
     */
    private String escapeHtml(String source) {
        if (!StringUtils.hasText(source)) {
            return "";
        }
        return source.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
