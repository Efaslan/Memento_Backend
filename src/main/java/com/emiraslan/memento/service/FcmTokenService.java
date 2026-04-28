package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.request.TokenRegisterRequestDto;
import com.emiraslan.memento.entity.DeviceToken;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.repository.DeviceTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public void saveToken(TokenRegisterRequestDto request, User user) {
        String redisKey = "deviceTokens:user:" + user.getUserId();
        String newTokenString = request.getFcmToken();

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

            deviceTokenRepository.save(token); // save to db
            redisTemplate.opsForSet().add(redisKey, newTokenString); // save to redis (into the set)

            log.info("DeviceToken Updated for User: {}", user.getEmail());
        } else {
            // if it does not exist, create a new token
            DeviceToken newToken = DeviceToken.builder()
                    .user(user)
                    .fcmToken(request.getFcmToken())
                    .deviceType(request.getDeviceType())
                    .build();
            deviceTokenRepository.save(newToken);
            redisTemplate.opsForSet().add(redisKey, newTokenString);
            log.info("New DeviceToken Saved for User: {}", user.getEmail());
        }
    }

    @Transactional
    public void deleteInvalidToken(String tokenString) {
        Optional<DeviceToken> tokenOpt = deviceTokenRepository.findByFcmToken(tokenString);
        if (tokenOpt.isPresent()) {
            DeviceToken token = tokenOpt.get();

            deviceTokenRepository.delete(token); // delete from db

            String redisKey = "deviceTokens:user:" + token.getUser().getUserId();
            redisTemplate.opsForSet().remove(redisKey, tokenString); // delete from redis

            log.warn("Invalid token deleted from DB and Redis: {}", tokenString);
        }
    }
}