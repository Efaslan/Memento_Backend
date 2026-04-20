package com.emiraslan.memento.service;

import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.enums.OtpAction;
import com.emiraslan.memento.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResetPasswordService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final OtpService otpService;


    @Transactional
    public void requestPasswordReset(String email) {
        otpService.generateAndSendOtp(email, OtpAction.PASSWORD_RESET);
    }

    @Transactional
    public void resetPassword(String email, String otpCode, String newPassword){

        otpService.validateOtp(email, otpCode, OtpAction.PASSWORD_RESET);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
