package com.digital.employee.common.redis;

public final class RedisConstants {

    private RedisConstants() {}

    public static final String ROLE_PERM_CACHE_PREFIX = "cache:role:perms:";

    public static final String ROLE_VERSION_PREFIX = "sys:role:version:";

    public static final String LOGIN_RATE_IP = "login:rate:ip:";

    public static final String LOGIN_RATE_ACCT = "login:rate:acct:";

    public static final long ROLE_PERM_CACHE_TTL_HOURS = 24;

    public static final int LOGIN_RATE_WINDOW_SECONDS = 900;

    public static final int LOGIN_RATE_MAX_ATTEMPTS = 5;
}
