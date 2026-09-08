package com.digital.employee.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.digital.employee.common.redis.RedisConstants;
import com.digital.employee.system.domain.entity.SysRole;
import com.digital.employee.system.domain.entity.SysUser;
import com.digital.employee.system.mapper.SysRoleMapper;
import com.digital.employee.system.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysRoleServiceImplTest {

    @Mock
    private SysRoleMapper roleMapper;

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private SysRoleServiceImpl roleService;

    private SysRole sampleRole;
    private SysUser sampleUser;

    @BeforeEach
    void setUp() throws Exception {
        setBaseMapper(roleService, roleMapper);
        sampleRole = new SysRole();
        sampleRole.setId(1L);
        sampleRole.setRoleKey("super_admin");
        sampleRole.setRoleName("超级管理员");
        sampleRole.setStatus(1);

        sampleUser = new SysUser();
        sampleUser.setId(10L);
        sampleUser.setUsername("admin");
        sampleUser.setRoleId(1L);
        sampleUser.setStatus(1);
    }

    @Test
    void getRoleByUserIdShouldReturnRole() {
        when(userMapper.selectById(10L)).thenReturn(sampleUser);
        when(roleMapper.selectById(1L)).thenReturn(sampleRole);

        SysRole result = roleService.getRoleByUserId(10L);

        assertNotNull(result);
        assertEquals("super_admin", result.getRoleKey());
    }

    @Test
    void getRoleByUserIdShouldReturnNullWhenUserNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        SysRole result = roleService.getRoleByUserId(999L);

        assertNull(result);
    }

    @Test
    void getRoleByUserIdShouldReturnNullWhenRoleNotFound() {
        when(userMapper.selectById(10L)).thenReturn(sampleUser);
        when(roleMapper.selectById(1L)).thenReturn(null);

        SysRole result = roleService.getRoleByUserId(10L);

        assertNull(result);
    }

    @Test
    void incrementRoleVersionShouldCallRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);

        roleService.incrementRoleVersion("super_admin");

        verify(valueOps).increment(RedisConstants.ROLE_VERSION_PREFIX + "super_admin");
    }

    @SuppressWarnings("unchecked")
    private void setBaseMapper(ServiceImpl<?, ?> service, Object mapper) throws Exception {
        Class<?> clazz = service.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField("baseMapper");
                field.setAccessible(true);
                field.set(service, mapper);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("baseMapper not found in class hierarchy");
    }
}
