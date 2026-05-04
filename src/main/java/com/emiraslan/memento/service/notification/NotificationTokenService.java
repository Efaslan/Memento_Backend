package com.emiraslan.memento.service.notification;

import com.emiraslan.memento.dto.request.NotificationTokenRegisterRequestDto;
import com.emiraslan.memento.entity.NotificationToken;
import com.emiraslan.memento.entity.RefreshToken;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.entity.UserDevice;
import com.emiraslan.memento.repository.device.NotificationTokenRepository;
import com.emiraslan.memento.repository.device.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationTokenService {

    private final NotificationTokenRepository notificationTokenRepository;
    private final StringRedisTemplate redisTemplate;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void updateNotificationToken(NotificationTokenRegisterRequestDto dto) {

        // find the user and the device through the refresh token
        RefreshToken refreshToken = refreshTokenRepository.findByRefreshToken(dto.getRefreshToken())
                .orElseThrow(() -> new IllegalStateException("INVALID_REFRESH_TOKEN_FOR_FCM"));

        UserDevice device = refreshToken.getUserDevice();
        User user = device.getUser();

        String redisKey = "notificationTokens:user:" + user.getUserId();
        String newFcmToken = dto.getFcmToken();

        // check if there's a NotificationToken for this device
        NotificationToken notificationToken = notificationTokenRepository.findByUserDevice_DeviceId(device.getDeviceId())
                .orElse(NotificationToken.builder().userDevice(device).build());

        // if the device has an FCM token, and it is changing, delete the old one from redis
        if (notificationToken.getFcmToken() != null && !notificationToken.getFcmToken().equals(newFcmToken)) {
            redisTemplate.opsForSet().remove(redisKey, notificationToken.getFcmToken());
            log.info("Old FCM Token removed from Redis for User: {}", device.getUser().getUserId());
        }

        // set the new fcm token
        notificationToken.setFcmToken(newFcmToken);
        notificationToken.setLastUpdated(Instant.now());

        // save it to db and redis
        notificationTokenRepository.save(notificationToken);
        redisTemplate.opsForSet().add(redisKey, newFcmToken);

        log.info("FCM Token successfully linked to Device ID: {} and saved to Redis for User: {}",
                device.getDeviceId(), device.getUser().getUserId());
    }

    @Transactional
    public void deleteInvalidFcmToken(String fcmTokenString) {
        Optional<NotificationToken> tokenOpt = notificationTokenRepository.findByFcmToken(fcmTokenString);
        if (tokenOpt.isPresent()) {
            NotificationToken token = tokenOpt.get();
            UserDevice oldDevice = token.getUserDevice();

            String redisKey = "notificationTokens:user:" + oldDevice.getUser().getUserId();

            redisTemplate.opsForSet().remove(redisKey, fcmTokenString); // delete from redis
            // deletes the NotificationToken. The user will stay logged in, but they will not receive notifications until mobile updates the fcmToken
            notificationTokenRepository.delete(token);

            log.warn("Invalid NotificationToken deleted from DB and Redis: {}", fcmTokenString);
        }
    }
}