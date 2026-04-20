package com.emiraslan.memento.service;

import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.enums.OtpAction;
import com.emiraslan.memento.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @Transactional
    public void generateAndSendOtp(String email, OtpAction action) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND"));

        // random code between 0-999,999. Adds zeroes in case e.g. 7 -> 000007
        SecureRandom secureRandom = new SecureRandom();
        String otpCode = String.format("%06d", secureRandom.nextInt(1000000));

        // redis (Key: otp:OTP_ACTION:user@gmail.com, Value: 123456)
        String redisKey = "otp:" + action.name() + ":" + user.getEmail();

        int ttlMinutes = switch (action){
            case PASSWORD_RESET -> 5;
            case RELATIONSHIP_INVITE -> 10;
        };
        // save the key: value pair into redis
        redisTemplate.opsForValue().set(redisKey, otpCode, Duration.ofMinutes(ttlMinutes));

        String subject = switch (action) {
            case PASSWORD_RESET -> "Memento - Şifre Sıfırlama";
            case RELATIONSHIP_INVITE -> "Memento - Yakın Daveti";
        };

        String body = switch (action) {
            case PASSWORD_RESET -> "Merhaba " + user.getFirstName() + ",\n\n"
                    + "Şifrenizi sıfırlamak için talebiniz alınmıştır. Şifre sıfırlama kodunuz:\n\n"
                    + otpCode + "\n\n"
                    + "Bu kod 5 dakika boyunca geçerlidir.";
            case RELATIONSHIP_INVITE -> "Merhaba " + user.getFirstName() + ",\n\n"
                    + "Bir Memento kullanıcısı sizi yakını olarak eklemek istiyor. Onay kodunuz:\n\n"
                    + otpCode + "\n\n"
                    + " (Bu kod 10 dakika boyunca geçerlidir)";
        };

        emailService.sendSimpleEmail(user.getEmail(), subject, body);
    }

    public void validateOtp(String email, String otpCode, OtpAction action) {
        String redisKey = "otp:" + action.name() + ":" + email;
        String cachedOtp = redisTemplate.opsForValue().get(redisKey);

        if (cachedOtp == null) {
            throw new IllegalArgumentException("OTP_EXPIRED_OR_NOT_FOUND");
        }
        if (!cachedOtp.equals(otpCode)) {
            throw new IllegalArgumentException("INVALID_OTP");
        }

        // delete OTP from redis if it was used
        redisTemplate.delete(redisKey);
    }
}
