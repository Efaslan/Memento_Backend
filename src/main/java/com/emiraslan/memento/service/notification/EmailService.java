package com.emiraslan.memento.service.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService{
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine; // Thymeleaf engine

    @Value("${memento.mail.sender}")
    private String fromEmail;

    public void sendEmail(String to, String subject, String templateName, Context context) {

        if(fromEmail == null || fromEmail.trim().isEmpty()){
            log.error("Unable to send email! Please check application.properties file and fill 'spring.mail.username' and 'spring.mail.password'.");
            throw new IllegalStateException("SMTP_NOT_CONFIGURED");
        }
        try {
            // Multipurpose Internet Mail Extensions (Mime) Message, enables HTML and other attachments instead of simple texts in SimpleMailMessage
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);

            // process the HTML template
            String htmlContent = templateEngine.process(templateName, context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("{} email successfully sent to: {}", templateName, to);

        } catch (MessagingException e) {
            log.error("Failed to send {} email to {}", templateName, to, e);
            throw new RuntimeException("SEND_EMAIL_FAILED");
        }
    }

    public void sendPasswordResetEmail(String to, String userName, String otpCode) {
        // Context is a dictionary that will hold variables for the HTML to display
        Context context = new Context();
        context.setVariable("name", userName);
        context.setVariable("otpCode", otpCode);

        sendEmail(to, "Şifre Sıfırlama Talebi", "password-reset", context);
    }

    public void sendRelationshipInviteEmail(String to, String targetName, String initiatorFullName, String otpCode) {
        Context context = new Context();
        context.setVariable("targetName", targetName);
        context.setVariable("initiatorName", initiatorFullName);
        context.setVariable("otpCode", otpCode);

        sendEmail(to, "Yeni Bir Yakın Davetiniz Var", "relationship-invite", context);
    }

    public void sendEmailUpdateEmail(String to, String userName, String otpCode) {
        Context context = new Context();
        context.setVariable("name", userName);
        context.setVariable("otpCode", otpCode);

        sendEmail(to, "E-posta Adresi Güncelleme Doğrulaması", "email-update", context);
    }

    public void sendVerificationEmail(String to, String userName, String token) {
        String verificationLink = "https://emir-memento.me/api/v1/auth/verify?token=" + token;

        Context context = new Context();
        context.setVariable("name", userName);
        context.setVariable("verificationLink", verificationLink);

        sendEmail(to, "Lütfen E-posta Adresinizi Doğrulayın", "email-verification", context);
    }
}