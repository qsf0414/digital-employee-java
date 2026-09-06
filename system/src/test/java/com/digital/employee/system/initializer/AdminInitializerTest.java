package com.digital.employee.system.initializer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.digital.employee.system.domain.entity.SysRole;
import com.digital.employee.system.domain.entity.SysUser;
import com.digital.employee.system.service.ISysRoleService;
import com.digital.employee.system.service.ISysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private ISysUserService userService;

    @Mock
    private ISysRoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminInitializer initializer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(initializer, "adminUsername", "admin");
        ReflectionTestUtils.setField(initializer, "adminPassword", "Test@12345");
    }

    @Test
    void shouldCreateAdminWhenNotExists() {
        when(userService.getByUsername("admin")).thenReturn(null);

        SysRole superAdminRole = new SysRole();
        superAdminRole.setId(1L);
        superAdminRole.setRoleKey("super_admin");
        when(roleService.getOne(any(LambdaQueryWrapper.class))).thenReturn(superAdminRole);
        when(passwordEncoder.encode("Test@12345")).thenReturn("$2a$hashed");

        initializer.run();

        verify(userService, times(1)).save(any(SysUser.class));
    }

    @Test
    void shouldSkipWhenAdminAlreadyExists() {
        SysUser existing = new SysUser();
        existing.setId(1L);
        existing.setUsername("admin");
        when(userService.getByUsername("admin")).thenReturn(existing);

        initializer.run();

        verify(userService, never()).save(any(SysUser.class));
    }

    @Test
    void shouldThrowWhenUsernameEmpty() {
        ReflectionTestUtils.setField(initializer, "adminUsername", "");

        assertThrows(IllegalStateException.class, () -> initializer.run());
    }

    @Test
    void shouldThrowWhenPasswordEmpty() {
        ReflectionTestUtils.setField(initializer, "adminPassword", "");

        assertThrows(IllegalStateException.class, () -> initializer.run());
    }

    @Test
    void shouldThrowWhenUsernameNull() {
        ReflectionTestUtils.setField(initializer, "adminUsername", null);

        assertThrows(IllegalStateException.class, () -> initializer.run());
    }

    @Test
    void shouldThrowWhenPasswordNull() {
        ReflectionTestUtils.setField(initializer, "adminPassword", null);

        assertThrows(IllegalStateException.class, () -> initializer.run());
    }

    @Test
    void shouldThrowWhenSuperAdminRoleNotExists() {
        when(userService.getByUsername("admin")).thenReturn(null);
        when(roleService.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> initializer.run());
    }
}
