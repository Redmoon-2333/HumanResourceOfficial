package com.redmoon2333.service;

import com.redmoon2333.dto.NotifyEvent;
import com.redmoon2333.dto.NotifyType;
import com.redmoon2333.entity.User;
import com.redmoon2333.mapper.UserMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserMapper userMapper;

    @Value("${app.mail.from:}")
    private String from;

    @Value("${app.mail.from-name:华师软院人力资源中心}")
    private String fromName;

    @Async
    public void sendNotificationEmail(NotifyEvent event) {
        log.info(">>> [邮件发送] 开始处理异步邮件任务: eventId={}, type={}, receiverId={}", event.getEventId(), event.getType(), event.getReceiverId());
        User receiver = userMapper.selectById(event.getReceiverId());
        if (receiver == null) {
            log.warn(">>> [邮件发送] 接收者不存在，跳过发送: userId={}", event.getReceiverId());
            return;
        }
        log.info(">>> [邮件发送] 查询到接收者: userId={}, name={}, studentId={}", receiver.getUserId(), receiver.getName(), receiver.getStudentId());

        String email = resolveEmail(receiver);
        if (email == null) {
            log.warn(">>> [邮件发送] 无法推导邮箱地址，跳过发送: userId={}, studentId={}", receiver.getUserId(), receiver.getStudentId());
            return;
        }
        log.info(">>> [邮件发送] 目标邮箱地址: {}", email);

        try {
            log.info(">>> [邮件发送] 开始构建 MIME 邮件: from={}, to={}, subject={}", from, email, event.getTitle());
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from, fromName);
            helper.setTo(email);
            helper.setSubject(event.getTitle());
            helper.setText(buildEmailContent(event), true);
            log.info(">>> [邮件发送] 邮件构建完成，准备通过 SMTP 发送...");
            mailSender.send(mimeMessage);
            log.info(">>> [邮件发送] 邮件发送成功: to={}, type={}", email, event.getType());
        } catch (MessagingException e) {
            log.error(">>> [邮件发送] 邮件构建失败: to={}, error={}", email, e.getMessage(), e);
            throw new RuntimeException("邮件构建失败", e);
        } catch (Exception e) {
            log.error(">>> [邮件发送] SMTP 发送失败: to={}, error={}", email, e.getMessage(), e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    private String resolveEmail(User user) {
        if (user.getStudentId() == null || user.getStudentId().isBlank()) {
            return null;
        }
        return user.getStudentId() + "@stu.ecnu.edu.cn";
    }

    private String buildEmailContent(NotifyEvent event) {
        String typeLabel = switch (event.getType()) {
            case TASK_ASSIGNED -> "任务分配";
            case TASK_REMIND -> "任务催促";
            case TASK_COMPLETED -> "任务完成";
            case ROLE_CHANGED -> "身份变更";
            case SYSTEM -> "系统通知";
        };

        String html = """
            <div style="max-width:600px;margin:0 auto;font-family:'Microsoft YaHei',sans-serif;">
                <div style="background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);padding:20px;border-radius:8px 8px 0 0;">
                    <h2 style="color:#fff;margin:0;">华师软院人力资源中心</h2>
                </div>
                <div style="padding:20px;border:1px solid #e0e0e0;border-top:none;border-radius:0 0 8px 8px;">
                    <p style="color:#666;font-size:14px;">通知类型：{0}</p>
                    <h3 style="color:#333;">{1}</h3>
                    <p style="color:#555;line-height:1.8;">{2}</p>
                    <hr style="border:none;border-top:1px solid #eee;margin:20px 0;"/>
                    <p style="color:#999;font-size:12px;">此邮件由系统自动发送，请勿直接回复。</p>
                </div>
            </div>
            """;
        return MessageFormat.format(html, typeLabel, event.getTitle(), event.getContent());
    }
}
