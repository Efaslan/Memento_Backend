package com.emiraslan.memento.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.lettuce.core.RedisClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Value("${spring.data.redis.url}")
    private String redisUrl;

    @Bean
    public RedisClient redisClient() {
        return RedisClient.create(redisUrl);
    }

    // proxy for bucket4j to speak with redis through lettuce
    @Bean
    public LettuceBasedProxyManager<byte[]> proxyManager(RedisClient redisClient) {
        // client here is our spring project, the server is redis
        ClientSideConfig clientSideConfig = ClientSideConfig.getDefault()
                .withExpirationAfterWriteStrategy(
                        // Garbage Collection of inactive users' buckets:
                        // deletes the token bucket from Redis memory after it remains inactive
                        // for longer than its refill time + 10 seconds
                        // Full bucket = allowed in, empty bucket = timed out (429), no bucket = new user -> full bucket
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10))
                );

        return LettuceBasedProxyManager.builderFor(redisClient)
                .withClientSideConfig(clientSideConfig)
                .build();
    }
}