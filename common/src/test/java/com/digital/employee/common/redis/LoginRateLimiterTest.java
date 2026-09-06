package com.digital.employee.common.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private LoginRateLimiter loginRateLimiter;

    @BeforeEach
    void setUp() {
        loginRateLimiter = new LoginRateLimiter(redisTemplate);
    }

    @Test
    void shouldAllowWhenBothBucketsBelowThreshold() {
        when(redisTemplate.execute(any(), anyList(), anyString()))
                .thenReturn(1L)
                .thenReturn(1L);

        boolean result = loginRateLimiter.isAllowed("192.168.1.1", "admin");

        assertTrue(result);
        verify(redisTemplate, times(2)).execute(any(), anyList(), anyString());
    }

    @Test
    void shouldAllowWhenExactlyAtThreshold() {
        when(redisTemplate.execute(any(), anyList(), anyString()))
                .thenReturn(5L)
                .thenReturn(5L);

        boolean result = loginRateLimiter.isAllowed("192.168.1.1", "admin");

        assertTrue(result);
    }

    @Test
    void shouldBlockWhenIpBucketExceedsThreshold() {
        when(redisTemplate.execute(any(), anyList(), anyString()))
                .thenReturn(6L);

        boolean result = loginRateLimiter.isAllowed("192.168.1.1", "admin");

        assertFalse(result);
        verify(redisTemplate, times(1)).execute(any(), anyList(), anyString());
    }

    @Test
    void shouldShortCircuitWhenIpBlockedAndNotCallAccountBucket() {
        when(redisTemplate.execute(any(), anyList(), anyString()))
                .thenReturn(6L);

        boolean result = loginRateLimiter.isAllowed("192.168.1.1", "admin");

        assertFalse(result);
        verify(redisTemplate, times(1)).execute(any(), anyList(), anyString());
    }

    @Test
    void shouldBlockWhenAccountBucketExceedsThresholdFromMultipleIps() {
        when(redisTemplate.execute(any(), anyList(), anyString()))
                .thenReturn(1L)
                .thenReturn(6L);

        boolean result = loginRateLimiter.isAllowed("192.168.1.1", "admin");

        assertFalse(result);
        verify(redisTemplate, times(2)).execute(any(), anyList(), anyString());
    }

    @Test
    void shouldBlockWhenBothBucketsExceedThreshold() {
        when(redisTemplate.execute(any(), anyList(), anyString()))
                .thenReturn(6L);

        boolean result = loginRateLimiter.isAllowed("192.168.1.1", "admin");

        assertFalse(result);
        verify(redisTemplate, times(1)).execute(any(), anyList(), anyString());
    }

    @Test
    void shouldAllowWhenRedisReturnsNull() {
        when(redisTemplate.execute(any(), anyList(), anyString()))
                .thenReturn(null);

        boolean result = loginRateLimiter.isAllowed("192.168.1.1", "admin");

        assertTrue(result);
    }

    @Test
    void shouldUseCorrectRedisKeyForIpBucket() {
        when(redisTemplate.execute(any(), anyList(), anyString()))
                .thenReturn(1L)
                .thenReturn(1L);

        loginRateLimiter.isAllowed("10.0.0.1", "user1");

        verify(redisTemplate).execute(
                any(),
                argThat(keys -> keys.size() == 1 && keys.get(0).equals("login:rate:ip:10.0.0.1")),
                eq(String.valueOf(RedisConstants.LOGIN_RATE_WINDOW_SECONDS))
        );
    }

    @Test
    void shouldUseCorrectRedisKeyForAccountBucket() {
        when(redisTemplate.execute(any(), anyList(), anyString()))
                .thenReturn(1L)
                .thenReturn(1L);

        loginRateLimiter.isAllowed("10.0.0.1", "user1");

        verify(redisTemplate).execute(
                any(),
                argThat(keys -> keys.size() == 1 && keys.get(0).equals("login:rate:acct:user1")),
                eq(String.valueOf(RedisConstants.LOGIN_RATE_WINDOW_SECONDS))
        );
    }

    @Test
    void shouldUseCorrectWindowSize() {
        when(redisTemplate.execute(any(), anyList(), anyString()))
                .thenReturn(1L)
                .thenReturn(1L);

        loginRateLimiter.isAllowed("10.0.0.1", "user1");

        verify(redisTemplate, times(2)).execute(
                any(),
                anyList(),
                eq(String.valueOf(900))
        );
    }
}
