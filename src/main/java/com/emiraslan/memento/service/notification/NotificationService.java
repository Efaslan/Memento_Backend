package com.emiraslan.memento.service.notification;

import com.emiraslan.memento.dto.request.NotificationTokenRegisterRequestDto;
import com.emiraslan.memento.entity.NotificationToken;
import com.emiraslan.memento.entity.UserDevice;
import com.emiraslan.memento.repository.device.NotificationTokenRepository;
import com.emiraslan.memento.repository.device.UserDeviceRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final StringRedisTemplate redisTemplate;
    private final NotificationTokenRepository notificationTokenRepository;
    private final UserDeviceRepository userDeviceRepository;

    @Transactional
    public void upsertNotificationToken(Integer userId, NotificationTokenRegisterRequestDto dto) {

        // find the user and the device through the refresh token
        UserDevice device = userDeviceRepository.findById(dto.getDeviceId())
                .orElseThrow(() -> new EntityNotFoundException("USER_DEVICE_NOT_FOUND"));

        String redisKey = "notificationTokens:user:" + userId;
        String newFcmToken = dto.getFcmToken();
        String deviceIdString = String.valueOf(device.getDeviceId());

        // check if there's a NotificationToken for this device
        NotificationToken notificationToken = notificationTokenRepository.findByUserDevice_DeviceId(device.getDeviceId())
                .orElse(NotificationToken.builder().userDevice(device).build());

        // set the new fcm token
        notificationToken.setFcmToken(newFcmToken);
        notificationToken.setLastUpdated(LocalDateTime.now());

        // save it to db and redis
        notificationTokenRepository.save(notificationToken);

        // hash structure holds a users every device, automatically replaces old notification tokens
        redisTemplate.opsForHash().put(redisKey, deviceIdString, newFcmToken);

        log.info("FCM Token successfully linked to Device ID: {} and saved to Redis for User: {}",
                device.getDeviceId(), device.getUser().getUserId());
    }

    // no transactional annotation because the request goes to Google FCM
    public void sendNotificationToUser(Integer userId, String title, String body) {

        String redisKey = "notificationTokens:user:" + userId;

        // pull all notification tokens in the hash as a list
        List<Object> tokens = redisTemplate.opsForHash().values(redisKey);

        if (tokens.isEmpty()) {
            log.warn("No Notification Tokens found in Redis for UserID: {}", userId);
            return;
        }

        // token strings directly from redis
        for (Object tokenObj : tokens) {
            String tokenString = (String) tokenObj;
            try {
                // create the message
                Message message = Message.builder()
                        .setToken(tokenString)
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .build();

                // send it to Google
                String response = FirebaseMessaging.getInstance().send(message);
                log.info("Notification Sent to {}. Response: {}", userId, response);

            } catch (Exception e) {
                log.error("Failed to send notification to {}: {}", userId, e.getMessage());

                if (e.getMessage() != null && (e.getMessage().contains("registration-token-not-registered") ||
                        e.getMessage().contains("invalid-argument"))) {
                    deleteInvalidFcmToken(tokenString); // deleting the invalid token from the db and redis
                }
            }
        }
    }

    private void deleteInvalidFcmToken(String fcmTokenString) {
        Optional<NotificationToken> tokenOpt = notificationTokenRepository.findByFcmToken(fcmTokenString);
        if (tokenOpt.isPresent()) {
            NotificationToken token = tokenOpt.get();
            UserDevice device = token.getUserDevice();

            String redisKey = "notificationTokens:user:" + device.getUser().getUserId();
            String deviceIdString = String.valueOf(device.getDeviceId());

            redisTemplate.opsForHash().delete(redisKey, deviceIdString); // delete the deviceId key in Redis hash

            // deletes the NotificationToken. The user will stay logged in, but they will not receive notifications until mobile updates the fcmToken
            notificationTokenRepository.delete(token);

            log.warn("Invalid NotificationToken deleted from DB and Redis: {}", fcmTokenString);
        }
    }
}