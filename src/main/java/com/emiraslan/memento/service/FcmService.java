package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.TokenRegisterRequest;
import com.emiraslan.memento.entity.DeviceToken;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.repository.DeviceTokenRepository;
import com.emiraslan.memento.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void saveToken(TokenRegisterRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND: " + request.getUserId()));

        // checks if the token already exists
        Optional<DeviceToken> existingToken = deviceTokenRepository.findByFcmToken(request.getFcmToken());

        if (existingToken.isPresent()) {
            // if it exists, just update its date
            DeviceToken token = existingToken.get();
            token.setLastUpdated(LocalDateTime.now());
            // if another user logged in from the same device, update the token for the new user
            if (!token.getUser().getUserId().equals(user.getUserId())) {
                token.setUser(user);
            }
            deviceTokenRepository.save(token);
            log.info("FCM Token Updated for User: {}", user.getEmail());
        } else {
            // if it does not exist, create a new token
            DeviceToken newToken = DeviceToken.builder()
                    .user(user)
                    .fcmToken(request.getFcmToken())
                    .deviceType(request.getDeviceType())
                    .build();
            deviceTokenRepository.save(newToken);
            log.info("New FCM Token Saved for User: {}", user.getEmail());
        }
    }

    // push notif to a single device
    public void sendNotificationToToken(String token, String title, String body) {
        try {
            // create the message
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            // send it to Google
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Notification Sent. Response: {}", response);

        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage());
            // if the token is invalid, we can delete it from the database. It generally means that the user deleted the app
            if (e.getMessage().contains("registration-token-not-registered") ||
                    e.getMessage().contains("invalid-argument")) {
                deleteInvalidToken(token);
            }
        }
    }

    @Transactional
    private void deleteInvalidToken(String token) {
        deviceTokenRepository.deleteByFcmToken(token);
        log.warn("Invalid token deleted from database: {}", token);
    }
}