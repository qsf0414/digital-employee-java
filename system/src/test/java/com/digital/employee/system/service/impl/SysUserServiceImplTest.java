package com.digital.employee.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.digital.employee.system.domain.entity.SysUser;
import com.digital.employee.system.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysUserServiceImplTest {

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SysUserServiceImpl userService;

    private SysUser sampleUser;

    @BeforeEach
    void setUp() throws Exception {
        setBaseMapper(userService, userMapper);
        sampleUser = new SysUser();
        sampleUser.setId(1L);
        sampleUser.setUsername("admin");
        sampleUser.setPasswordHash("$2a$hashed");
        sampleUser.setNickname("管理员");
        sampleUser.setPhone("13800138000");
        sampleUser.setRoleId(1L);
        sampleUser.setStatus(1);
        sampleUser.setMustChangePassword(false);
    }

    @Test
    void getByUsernameShouldReturnUser() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(sampleUser);

        SysUser result = userService.getByUsername("admin");

        assertNotNull(result);
        assertEquals("admin", result.getUsername());
        verify(userMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void getByUsernameShouldReturnNullWhenNotFound() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        SysUser result = userService.getByUsername("nonexistent");

        assertNull(result);
    }

    @Test
    void createUserShouldEncodePasswordAndSave() {
        when(passwordEncoder.encode("rawPass")).thenReturn("$2a$encoded");
        when(userMapper.insert(any(SysUser.class))).thenReturn(1);

        SysUser newUser = new SysUser();
        newUser.setUsername("newuser");
        userService.createUser(newUser, "rawPass");

        assertEquals("$2a$encoded", newUser.getPasswordHash());
        assertEquals(1, newUser.getStatus());
        assertTrue(newUser.getMustChangePassword());
        assertNotNull(newUser.getCreatedAt());
        assertNotNull(newUser.getUpdatedAt());
        verify(userMapper, times(1)).insert(any(SysUser.class));
    }

    @Test
    void createUserShouldPreserveExistingStatus() {
        when(passwordEncoder.encode("raw")).thenReturn("$2a$enc");
        when(userMapper.insert(any(SysUser.class))).thenReturn(1);

        SysUser newUser = new SysUser();
        newUser.setUsername("user2");
        newUser.setStatus(0);
        newUser.setMustChangePassword(false);
        userService.createUser(newUser, "raw");

        assertEquals(0, newUser.getStatus());
        assertFalse(newUser.getMustChangePassword());
    }

    @Test
    void pageUsersShouldReturnPage() {
        Page<SysUser> mockPage = new Page<>(1, 10);
        mockPage.setRecords(List.of(sampleUser));
        mockPage.setTotal(1);
        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        Page<SysUser> result = userService.pageUsers(1, 10, null);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals("admin", result.getRecords().get(0).getUsername());
    }

    @Test
    void pageUsersWithKeywordShouldFilter() {
        Page<SysUser> mockPage = new Page<>(1, 10);
        mockPage.setRecords(List.of(sampleUser));
        mockPage.setTotal(1);
        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        Page<SysUser> result = userService.pageUsers(1, 10, "admin");

        assertNotNull(result);
        verify(userMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void pageUsersWithEmptyKeywordShouldNotFilter() {
        Page<SysUser> mockPage = new Page<>(1, 10);
        mockPage.setRecords(List.of());
        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        Page<SysUser> result = userService.pageUsers(1, 10, "");

        assertNotNull(result);
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
