package com.digital.employee.common.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class LoginRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;

    public LoginRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = new DefaultRedisScript<>();
        this.rateLimitScript.setResultType(Long.class);
        this.rateLimitScript.setScriptText(
            "local current = redis.call('incr', KEYS[1]); " +
            "if tonumber(current) == 1 then " +
            "    redis.call('expire', KEYS[1], ARGV[1]); " +
            "end; " +
            "return current;"
        );
    }

    public boolean isAllowed(String ip, String username) {
        if (isBlocked(RedisConstants.LOGIN_RATE_IP + ip)) {
            return false;
        }
        return !isBlocked(RedisConstants.LOGIN_RATE_ACCT + username);
    }

    private boolean isBlocked(String key) {
        Long count = redisTemplate.execute(
            rateLimitScript,
            Collections.singletonList(key),
            String.valueOf(RedisConstants.LOGIN_RATE_WINDOW_SECONDS)
        );
        return count != null && count > RedisConstants.LOGIN_RATE_MAX_ATTEMPTS;
    }
}
