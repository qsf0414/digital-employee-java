package com.digital.employee.system.satoken;

import com.digital.employee.common.redis.RedisConstants;
import com.digital.employee.system.domain.entity.SysRole;
import com.digital.employee.system.service.ISysMenuService;
import com.digital.employee.system.service.ISysRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StpInterfaceImplTest {

    @Mock
    private ISysMenuService menuService;
    @Mock
    private ISysRoleService roleService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private SetOperations<String, String> setOps;

    private StpInterfaceImpl stpInterface;

    @BeforeEach
    void setUp() {
        stpInterface = new StpInterfaceImpl(menuService, roleService, redisTemplate);
    }

    @Test
    void getPermissionListShouldReturnWildcardForSuperAdmin() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleKey("super_admin");
        role.setStatus(1);
        when(roleService.getRoleByUserId(1L)).thenReturn(role);

        List<String> perms = stpInterface.getPermissionList(1L, "login-type");

        assertEquals(List.of("*:*:*"), perms);
    }

    @Test
    void getPermissionListShouldReturnEmptyWhenRoleIsNull() {
        when(roleService.getRoleByUserId(1L)).thenReturn(null);

        List<String> perms = stpInterface.getPermissionList(1L, "login-type");

        assertTrue(perms.isEmpty());
    }

    @Test
    void getPermissionListShouldReturnEmptyWhenRoleDisabled() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleKey("admin");
        role.setStatus(0);
        when(roleService.getRoleByUserId(1L)).thenReturn(role);

        List<String> perms = stpInterface.getPermissionList(1L, "login-type");

        assertTrue(perms.isEmpty());
    }

    @Test
    void getPermissionListShouldReturnCachedPerms() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleKey("admin");
        role.setStatus(1);
        when(roleService.getRoleByUserId(1L)).thenReturn(role);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(RedisConstants.ROLE_VERSION_PREFIX + "admin")).thenReturn("1");
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.members(RedisConstants.ROLE_PERM_CACHE_PREFIX + "admin:1"))
                .thenReturn(Set.of("user:list", "user:create"));

        List<String> perms = stpInterface.getPermissionList(1L, "login-type");

        assertEquals(2, perms.size());
        assertTrue(perms.contains("user:list"));
        assertTrue(perms.contains("user:create"));
    }

    @Test
    void getPermissionListShouldReturnEmptyWhenCacheHasEmptyFlag() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleKey("admin");
        role.setStatus(1);
        when(roleService.getRoleByUserId(1L)).thenReturn(role);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(RedisConstants.ROLE_VERSION_PREFIX + "admin")).thenReturn("1");
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.members(RedisConstants.ROLE_PERM_CACHE_PREFIX + "admin:1"))
                .thenReturn(Set.of(":empty:"));

        List<String> perms = stpInterface.getPermissionList(1L, "login-type");

        assertTrue(perms.isEmpty());
    }

    @Test
    void getPermissionListShouldQueryDBWhenCacheMiss() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleKey("admin");
        role.setStatus(1);
        when(roleService.getRoleByUserId(1L)).thenReturn(role);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(RedisConstants.ROLE_VERSION_PREFIX + "admin")).thenReturn("1");
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.members(anyString()))
                .thenReturn(Set.of())
                .thenReturn(Set.of());
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(menuService.selectPermsByRoleId(1L)).thenReturn(List.of("menu:list"));
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(1L);

        List<String> perms = stpInterface.getPermissionList(1L, "login-type");

        assertEquals(List.of("menu:list"), perms);
        verify(menuService).selectPermsByRoleId(1L);
    }

    @Test
    void getPermissionListShouldStoreEmptyFlagWhenDBReturnsEmpty() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleKey("admin");
        role.setStatus(1);
        when(roleService.getRoleByUserId(1L)).thenReturn(role);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(RedisConstants.ROLE_VERSION_PREFIX + "admin")).thenReturn("1");
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.members(RedisConstants.ROLE_PERM_CACHE_PREFIX + "admin:1"))
                .thenReturn(Set.of())
                .thenReturn(Set.of());
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(menuService.selectPermsByRoleId(1L)).thenReturn(List.of());
        when(setOps.add(anyString(), any())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(1L);

        List<String> perms = stpInterface.getPermissionList(1L, "login-type");

        assertTrue(perms.isEmpty());
        verify(setOps).add(argThat(k -> k.contains("admin")), eq(":empty:"));
    }

    @Test
    void getRoleListShouldReturnRoleKey() {
        SysRole role = new SysRole();
        role.setRoleKey("admin");
        role.setStatus(1);
        when(roleService.getRoleByUserId(1L)).thenReturn(role);

        List<String> roles = stpInterface.getRoleList(1L, "login-type");

        assertEquals(List.of("admin"), roles);
    }

    @Test
    void getRoleListShouldReturnEmptyWhenRoleIsNull() {
        when(roleService.getRoleByUserId(1L)).thenReturn(null);

        List<String> roles = stpInterface.getRoleList(1L, "login-type");

        assertTrue(roles.isEmpty());
    }

    @Test
    void getRoleListShouldReturnEmptyWhenRoleDisabled() {
        SysRole role = new SysRole();
        role.setRoleKey("admin");
        role.setStatus(0);
        when(roleService.getRoleByUserId(1L)).thenReturn(role);

        List<String> roles = stpInterface.getRoleList(1L, "login-type");

        assertTrue(roles.isEmpty());
    }

    @Test
    void getPermissionListShouldSpinWaitWhenLockNotAcquired() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleKey("admin");
        role.setStatus(1);
        when(roleService.getRoleByUserId(1L)).thenReturn(role);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(RedisConstants.ROLE_VERSION_PREFIX + "admin")).thenReturn("1");
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.members(RedisConstants.ROLE_PERM_CACHE_PREFIX + "admin:1"))
                .thenReturn(Set.of());
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        assertThrows(IllegalStateException.class,
                () -> stpInterface.getPermissionList(1L, "login-type"));
    }
}
