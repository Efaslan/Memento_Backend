package com.emiraslan.memento.security;

import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.exception.RateLimitExceededException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final LettuceBasedProxyManager<byte[]> proxyManager;

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        String path = request.getRequestURI();

        String key = resolveKey(request, path); // redis key: ip or id
        BucketConfiguration bucketConfiguration = resolveBucketConfig(path); // bucket limit and refill

        // get the key's bucket from redis, create one if the bucket doesn't exist
        // if the bucket exists, the lambda function doesn't work because the bucket already knows its attributes
        Bucket bucket = proxyManager.builder().build(key.getBytes(), () -> bucketConfiguration);

        // spend 1 token from the bucket
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // send the remaining limit in auth endpoints
            if (path.startsWith("/api/v1/auth")) {
                response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            }
            return true;
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;

            // send the time-out info in auth endpoints
            if (path.startsWith("/api/v1/auth")) {
                response.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            }

            throw new RateLimitExceededException("TOO_MANY_REQUESTS_" + waitForRefill + "_SECONDS_TIME_OUT");
        }
    }

    private String resolveKey(HttpServletRequest request, String path) {
        // the key contains the ip in case of auth endpoints because the user isn't authenticated yet
        if (path.startsWith("/api/v1/auth")) {
            return "rate_limit:ip:" + request.getRemoteAddr();
        }

        // get the security context if the request isn't made to /auth
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // if JWT is valid and user is logged in
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            User user = (User) auth.getPrincipal();
            // the key contains the user's id if they are logged in
            return "rate_limit:user:" + user.getUserId();
        }

        // fallback to ip address for insurance
        return "rate_limit:ip:" + request.getRemoteAddr();
    }

    private BucketConfiguration resolveBucketConfig(String path) {
        // maximum 5 request to /auth endpoints in 1 minute
        if (path.startsWith("/api/v1/auth")) {
            return BucketConfiguration.builder()
                    .addLimit(limit -> limit.capacity(5).refillGreedy(5, Duration.ofMinutes(1)))
                    .build();
        }

        // all remaining endpoints. Starts with 30 tokens for possible multiple requests on start. Refills 30 tokens in 1 minutes
        return BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(30).refillGreedy(30, Duration.ofMinutes(1)))
                .build();
    }
}
