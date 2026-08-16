package com.emiraslan.memento.service.auth;

import com.emiraslan.memento.dto.response.BasicStringResponse;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.repository.user.UserRepository;
import com.emiraslan.memento.service.notification.OtpService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResetPasswordService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;


    @Transactional
    public BasicStringResponse requestPasswordReset(String email) {
        otpService.generateAndSendOtpForPasswordReset(email);
        return new BasicStringResponse("We have sent you a 6-digit code you can use to reset your password. Please check your inbox.");
    }

    @Transactional
    public BasicStringResponse resetPassword(String email, String otpCode, String newPassword){

        otpService.validateOtpForPasswordReset(email, otpCode);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return new BasicStringResponse("Your password has been successfully updated.");
    }
}
