package com.emiraslan.memento.service;

import com.emiraslan.memento.entity.NotificationToken;
import com.emiraslan.memento.repository.device.NotificationTokenRepository;
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

    private final NotificationTokenRepository notificationTokenRepository;
    private final StringRedisTemplate redisTemplate;

    // loading all fcm tokens to redis when the system starts up to avoid constant db queries while getting user notification tokens
    @EventListener(ApplicationReadyEvent.class)
    public void loadTokensToRedis() {
        log.info("Starting to warm up Redis with NotificationTokens...");

        // delete oldKeys from last startup, just in case
        Set<String> oldKeys = redisTemplate.keys("notificationTokens:user:*");
        if (!oldKeys.isEmpty()) {
            redisTemplate.delete(oldKeys);
        }

        // pull all tokens from the db
        List<NotificationToken> allTokens = notificationTokenRepository.findAllWithDeviceAndUser();

        // save them into redis as a set
        for (NotificationToken token : allTokens) {
            String redisKey = "notificationTokens:user:" + token.getUserDevice().getUser().getUserId();
            redisTemplate.opsForSet().add(redisKey, token.getFcmToken());
        }

        log.info("Successfully loaded {} NotificationTokens Tokens into Redis.", allTokens.size());
    }
}