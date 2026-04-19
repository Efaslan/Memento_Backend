package com.emiraslan.memento.service;

import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ResetPasswordService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public void generateAndSendOtp(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND_WITH_EMAIL"));

        SecureRandom secureRandom = new SecureRandom();
        // random code between 0-999,999. Adds zeroes in case e.g. 7 -> 000007
        String otpCode = String.format("%06d", secureRandom.nextInt(1000000));
        // redis (Key: otp:user@gmail.com, Value: 123456)
        String redisKey = "otp:" + user.getEmail();
        // Valid for 15 minutes
        redisTemplate.opsForValue().set(redisKey, otpCode, Duration.ofMinutes(15));

        String subject = "Memento - Şifre Sıfırlama Kodunuz";
        String body = "Merhaba " + user.getFirstName() + ",\n\n"
                + "Şifrenizi sıfırlamak için talebiniz alınmıştır. Şifre sıfırlama kodunuz:\n\n"
                + "KOD: " + otpCode + "\n\n"
                + "Bu kod 15 dakika boyunca geçerlidir.";

        emailService.sendSimpleEmail(user.getEmail(), subject, body);
    }

    @Transactional
    public void resetPassword(String email, String otpCode, String newPassword){
        String redisKey = "otp:" + email;
        // getting the key's(email) value(otp code) from redis
        String cachedOtp = redisTemplate.opsForValue().get(redisKey);

        if(cachedOtp == null){
            throw new IllegalStateException("OTP_EXPIRED_OR_NOT_FOUND");
        }

        if(!cachedOtp.equals(otpCode)){
            throw new IllegalArgumentException("INVALID_OTP");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user); // user'in kalan kisimlarini null yapar mi?

        redisTemplate.delete(redisKey);
    }
}
