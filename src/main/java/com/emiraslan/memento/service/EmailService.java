package com.emiraslan.memento.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService{
    private final JavaMailSender mailSender;

    @Value("${memento.mail.sender}")
    private String fromEmail;

    public void sendSimpleEmail(String to, String subject, String body) {

        if(fromEmail == null || fromEmail.trim().isEmpty()){
            log.error("Unable to send email! Please check application.properties file and fill 'spring.mail.username' and 'spring.mail.password'.");
            throw new IllegalStateException("EMAIL_NOT_CONFIGURED");
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email successfully sent to: {}", to);
        } catch (Exception e) {
            throw new RuntimeException("SEND_EMAIL_FAILED");
        }
    }
}