package com.emiraslan.memento.service.notification;

import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.enums.OtpAction;
import com.emiraslan.memento.repository.user.UserRepository;
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

    // Password reset methods:

    @Transactional
    public void generateAndSendOtpForPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND"));

        String otpCode = generateRandomCode();
        String redisKey = "otp:" + OtpAction.PASSWORD_RESET + ":" + user.getEmail();

        redisTemplate.opsForValue().set(redisKey, otpCode, Duration.ofMinutes(5));

        emailService.sendPasswordResetEmail(email, user.getFirstName(), otpCode);
    }

    public void validateOtpForPasswordReset(String email, String otpCode) {
        String redisKey = "otp:" + OtpAction.PASSWORD_RESET + ":"+ email;
        validateAndDeleteFromRedis(redisKey, otpCode);
    }

    // Relationship methods:

    @Transactional
    public void generateAndSendOtpForRelationshipInvitation(String targetEmail, User initiator) {
        User targetUser = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> new EntityNotFoundException("TARGET_USER_NOT_FOUND"));

        String otpCode = generateRandomCode();
        // adding both users' email to the key
        String redisKey = "otp:" + OtpAction.RELATIONSHIP_INVITE + ":" + initiator.getEmail() + ":" + targetUser.getEmail();

        redisTemplate.opsForValue().set(redisKey, otpCode, Duration.ofMinutes(10));

        String initiatorName = initiator.getFirstName() + " " + initiator.getLastName();

        emailService.sendRelationshipInviteEmail(targetEmail, targetUser.getFirstName(), initiatorName, otpCode);
    }

    public void validateOtpForRelationshipInvitation(String targetEmail, User initiator, String otpCode) {
        String redisKey = "otp:" + OtpAction.RELATIONSHIP_INVITE + ":" + initiator.getEmail() + ":" + targetEmail;
        validateAndDeleteFromRedis(redisKey, otpCode);
    }

    // Email update methods:
    @Transactional
    public void generateAndSendOtpForEmailChange(User user, String newEmail) {
        String otpCode = generateRandomCode();

        String redisKey = "otp:" + OtpAction.EMAIL_CHANGE + ":" + user.getUserId() + ":" + newEmail;

        redisTemplate.opsForValue().set(redisKey, otpCode, Duration.ofMinutes(5));

        emailService.sendEmailUpdateEmail(newEmail, user.getFirstName(), otpCode);
    }

    public void validateOtpForEmailChange(Integer userId, String newEmail, String otpCode) {
        String redisKey = "otp:" + OtpAction.EMAIL_CHANGE + ":" + userId + ":" + newEmail;
        validateAndDeleteFromRedis(redisKey, otpCode);
    }

    // mutual methods
    private String generateRandomCode() {
        // generates a number between 0-999999
        SecureRandom secureRandom = new SecureRandom();
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

    private void validateAndDeleteFromRedis(String redisKey, String otpCode) {
        // find the key's value(otp)
        String cachedOtp = redisTemplate.opsForValue().get(redisKey);

        if (cachedOtp == null) {
            throw new IllegalArgumentException("OTP_EXPIRED_OR_NOT_FOUND");
        }
        if (!cachedOtp.equals(otpCode)) {
            throw new IllegalArgumentException("INVALID_OTP");
        }

        // if the code is used, delete it
        redisTemplate.delete(redisKey);
    }
}