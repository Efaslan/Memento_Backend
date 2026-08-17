package com.emiraslan.memento.service;

import com.emiraslan.memento.entity.NotificationToken;
import com.emiraslan.memento.entity.user.PatientRelationship;
import com.emiraslan.memento.repository.device.NotificationTokenRepository;
import com.emiraslan.memento.repository.user.PatientRelationshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheWarmer {

    private final NotificationTokenRepository notificationTokenRepository;
    private final PatientRelationshipRepository patientRelationshipRepository;
    private final StringRedisTemplate redisTemplate;

    // loading all FCM Tokens and Relationships to redis when the system starts up to avoid constant db queries while getting user notification tokens
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpRedis() {
        log.info("Starting to warm up Redis.");
        loadTokensToRedis();
        loadRelationshipsToRedis();
        log.info("Redis warmed up with NotificationTokens and Relationships.");
    }

    private void loadTokensToRedis() {
        log.info("Loading NotificationTokens to Redis...");

        // delete oldKeys from last startup, just in case
        Set<String> oldKeys = redisTemplate.keys("notificationTokens:user:*");
        if (!oldKeys.isEmpty()) {
            redisTemplate.delete(oldKeys);
        }

        // pull all tokens from the db
        List<NotificationToken> allTokens = notificationTokenRepository.findAllWithDeviceAndUser();

        if (!allTokens.isEmpty()) {
            // Pipeline gathers all opsForHash() commands and executes them in a single network request
            redisTemplate.executePipelined(new SessionCallback<>() {
                @Override
                public Object execute(RedisOperations operations) throws DataAccessException {
                    for (NotificationToken token : allTokens) {
                        String redisKey = "notificationTokens:user:" + token.getUserDevice().getUser().getUserId();
                        // We're converting deviceId into a string to use it as a field in the hash.
                        String deviceIdString = String.valueOf(token.getUserDevice().getDeviceId());

                        // Added into the Pipeline first before actually going to Redis
                        redisTemplate.opsForHash().put(redisKey, deviceIdString, token.getFcmToken());
                    }
                    return null;
                }
            });
        }
        log.info("Successfully loaded {} NotificationTokens Tokens into Redis.", allTokens.size());
    }

    private void loadRelationshipsToRedis() {
        log.info("Loading Relationships to Redis...");

        Set<String> oldKeys = redisTemplate.keys("relationships:*");
        if (!oldKeys.isEmpty()) {
            redisTemplate.delete(oldKeys);
        }

        List<PatientRelationship> relationships = patientRelationshipRepository.findAllByIsActiveTrue();

        if (!relationships.isEmpty()) {
            Map<String, String> relationshipMap = new HashMap<>();

            for (PatientRelationship rel : relationships) {
                String redisKey = "relationships:" + rel.getPatient().getUserId() + ":" + rel.getCaregiver().getUserId();
                relationshipMap.put(redisKey, "true");
            }

            redisTemplate.opsForValue().multiSet(relationshipMap);
        }

        log.info("Successfully loaded {} Relationships into Redis.", relationships.size());
    }
}