package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.request.UserConsentRequest;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.entity.user.UserConsent;
import com.emiraslan.memento.repository.user.UserConsentRepository;
import com.emiraslan.memento.repository.user.UserRepository;
import com.emiraslan.memento.service.notification.OtpService;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final UserConsentRepository userConsentRepository;

    @Transactional
    public void requestEmailChange(Integer userId, String newEmail) {

        if (userRepository.existsByEmail(newEmail)) {
            throw new EntityExistsException("EMAIL_ALREADY_EXISTS");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND"));

        if (user.getEmail().equalsIgnoreCase(newEmail)) {
            throw new IllegalArgumentException("SAME_EMAIL_ADDRESS");
        }

        // send 6-digit otp to the new email address
        otpService.generateAndSendOtpForEmailChange(user, newEmail);
        log.info("Email change OTP sent for User ID: {}", userId);
    }

    @Transactional
    public void verifyAndChangeEmail(Integer userId, String newEmail, String otpCode) {

        // validate OTP code
        otpService.validateOtpForEmailChange(userId, newEmail, otpCode);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND"));


        // update email
        String oldEmail = user.getEmail();
        user.setEmail(newEmail);
        userRepository.save(user);

        log.info("User ID: {} successfully changed email from {} to {}", userId, oldEmail, newEmail);
    }

    @Transactional
    public void recordConsent(User user, UserConsentRequest request, String ipAddress, String userAgent){

        // never overwriting a record in this table, we always insert another consent
        UserConsent consent = MapperUtil.toUserConsentEntity(user, request.getConsentType(), request.getDocumentVersion(), request.getIsAccepted(), ipAddress, userAgent);

        userConsentRepository.save(consent);

        log.info("Consent Log: UserID: {}, Type: {}, Version: {}, Accepted: {}, IP: {}",
                user.getUserId(), request.getConsentType(), request.getDocumentVersion(), request.getIsAccepted(), ipAddress);
    }
}