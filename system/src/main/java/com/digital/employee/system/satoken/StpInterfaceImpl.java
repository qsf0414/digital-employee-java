package com.digital.employee.system.satoken;

import cn.dev33.satoken.stp.StpInterface;
import com.digital.employee.common.redis.RedisConstants;
import com.digital.employee.system.domain.entity.SysRole;
import com.digital.employee.system.service.ISysMenuService;
import com.digital.employee.system.service.ISysRoleService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class StpInterfaceImpl implements StpInterface {

    private static final String EMPTY_FLAG = ":empty:";
    private static final String LOCK_PREFIX = "lock:role:perms:";
    private static final long LOCK_TIMEOUT_SECONDS = 5;
    private static final long SPIN_SLEEP_MS = 80;
    private static final int MAX_RETRY = 40;

    private static final String RELEASE_LOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) else return 0 end";

    private final ISysMenuService menuService;
    private final ISysRoleService roleService;
    private final StringRedisTemplate redisTemplate;

    public StpInterfaceImpl(ISysMenuService menuService, ISysRoleService roleService,
                            StringRedisTemplate redisTemplate) {
        this.menuService = menuService;
        this.roleService = roleService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        SysRole role = roleService.getRoleByUserId(userId);
        if (role == null || role.getStatus() == 0) {
            return List.of();
        }

        if ("super_admin".equals(role.getRoleKey())) {
            return List.of("*:*:*");
        }

        String roleKey = role.getRoleKey();

        for (int retry = 0; retry < MAX_RETRY; retry++) {
            String ver = getRoleVersion(roleKey);
            String cacheKey = buildCacheKey(roleKey, ver);
            Set<String> cached = redisTemplate.opsForSet().members(cacheKey);
            if (cached != null && !cached.isEmpty()) {
                return toPermissions(cached);
            }

            String lockKey = LOCK_PREFIX + roleKey + ":" + ver;
            String ownerToken = UUID.randomUUID().toString();
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, ownerToken, LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(acquired)) {
                try {
                    String lockedVersion = getRoleVersion(roleKey);
                    if (!ver.equals(lockedVersion)) {
                        continue;
                    }

                    cached = redisTemplate.opsForSet().members(cacheKey);
                    if (cached != null && !cached.isEmpty()) {
                        return toPermissions(cached);
                    }

                    List<String> dbPerms = menuService.selectPermsByRoleId(role.getId());

                    String latestVersion = getRoleVersion(roleKey);
                    if (!ver.equals(latestVersion)) {
                        continue;
                    }

                    if (dbPerms.isEmpty()) {
                        redisTemplate.opsForSet().add(cacheKey, EMPTY_FLAG);
                    } else {
                        redisTemplate.opsForSet().add(cacheKey, dbPerms.toArray(new String[0]));
                    }
                    redisTemplate.expire(cacheKey, RedisConstants.ROLE_PERM_CACHE_TTL_HOURS, TimeUnit.HOURS);
                    return dbPerms;
                } finally {
                    redisTemplate.execute(
                            new org.springframework.data.redis.core.script.DefaultRedisScript<>(
                                    RELEASE_LOCK_LUA, Long.class),
                            List.of(lockKey), ownerToken);
                }
            }

            try {
                Thread.sleep(SPIN_SLEEP_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return List.of();
            }
        }

        throw new IllegalStateException("角色权限缓存回填重试次数超限");
    }

    private String getRoleVersion(String roleKey) {
        String version = redisTemplate.opsForValue()
                .get(RedisConstants.ROLE_VERSION_PREFIX + roleKey);
        return version != null ? version : "0";
    }

    private String buildCacheKey(String roleKey, String version) {
        return RedisConstants.ROLE_PERM_CACHE_PREFIX + roleKey + ":" + version;
    }

    private List<String> toPermissions(Set<String> cached) {
        return cached.contains(EMPTY_FLAG) ? List.of() : new ArrayList<>(cached);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        SysRole role = roleService.getRoleByUserId(userId);
        return (role != null && role.getStatus() == 1) ? List.of(role.getRoleKey()) : List.of();
    }
}
