package com.emiraslan.memento.service;

import com.emiraslan.memento.entity.DeviceToken;
import com.emiraslan.memento.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheWarmer {

    private final DeviceTokenRepository deviceTokenRepository;
    private final StringRedisTemplate redisTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void loadTokensToRedis() {
        log.info("Starting to warm up Redis with DeviceTokens...");

        // delete oldKeys from last startup, just in case. (in dev, we pull redis from docker anyway so it will always be empty)
        Set<String> oldKeys = redisTemplate.keys("deviceTokens:user:*");
        if (!oldKeys.isEmpty()) {
            redisTemplate.delete(oldKeys);
        }

        // pull all tokens from the db
        List<DeviceToken> allTokens = deviceTokenRepository.findAll();

        // save them into redis as a set
        for (DeviceToken token : allTokens) {
            String redisKey = "deviceTokens:user:" + token.getUser().getUserId();
            redisTemplate.opsForSet().add(redisKey, token.getFcmToken());
        }

        log.info("Successfully loaded {} DeviceTokens Tokens into Redis.", allTokens.size());
    }
}