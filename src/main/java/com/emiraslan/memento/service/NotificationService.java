package com.emiraslan.memento.service;

import com.emiraslan.memento.entity.User;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final StringRedisTemplate redisTemplate;
    private final FcmTokenService fcmTokenService;

    // todo burada user yerine direkt userid kullanabiriz ileride test ettikten sonra loglar icin user kalsin
    public void sendNotificationToUser(User user, String title, String body) {

        String redisKey = "deviceTokens:user:" + user.getUserId();
        Set<String> tokens = redisTemplate.opsForSet().members(redisKey);

        if (tokens == null || tokens.isEmpty()) {
            log.warn("No device tokens found in Redis for UserID: {}", user.getUserId());
            return;
        }

        // token strings directly from redis
        for (String tokenString : tokens) {
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
                log.info("Notification Sent to {}. Response: {}", user.getEmail(), response);

            } catch (Exception e) {
                log.error("Failed to send notification to {}: {}", user.getEmail(), e.getMessage());

                if (e.getMessage() != null && (e.getMessage().contains("registration-token-not-registered") ||
                        e.getMessage().contains("invalid-argument"))) {
                    fcmTokenService.deleteInvalidToken(tokenString); // deleting the invalid token from the db and redis
                }
            }
        }
    }
}