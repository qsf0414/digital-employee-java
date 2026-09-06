package com.digital.employee.common.redis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RedisConstantsTest {

    @Test
    void constantsShouldHaveCorrectValues() {
        assertEquals("cache:role:perms:", RedisConstants.ROLE_PERM_CACHE_PREFIX);
        assertEquals("sys:role:version:", RedisConstants.ROLE_VERSION_PREFIX);
        assertEquals("login:rate:ip:", RedisConstants.LOGIN_RATE_IP);
        assertEquals("login:rate:acct:", RedisConstants.LOGIN_RATE_ACCT);
        assertEquals(24, RedisConstants.ROLE_PERM_CACHE_TTL_HOURS);
        assertEquals(900, RedisConstants.LOGIN_RATE_WINDOW_SECONDS);
        assertEquals(5, RedisConstants.LOGIN_RATE_MAX_ATTEMPTS);
    }
}
