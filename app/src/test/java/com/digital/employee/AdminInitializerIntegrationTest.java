package com.digital.employee;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.digital.employee.system.domain.entity.SysRole;
import com.digital.employee.system.domain.entity.SysUser;
import com.digital.employee.system.initializer.AdminInitializer;
import com.digital.employee.system.service.ISysRoleService;
import com.digital.employee.system.service.ISysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInitializerIntegrationTest {

    @Mock
    private ISysUserService userService;

    @Mock
    private ISysRoleService roleService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AdminInitializer adminInitializer;

    @BeforeEach
    void setUp() {
        adminInitializer = new AdminInitializer(userService, roleService, passwordEncoder);
    }

    @Test
    void shouldThrowWhenUsernameMissing() {
        com.digital.employee.system.initializer.AdminInitializer init =
                new com.digital.employee.system.initializer.AdminInitializer(userService, roleService, passwordEncoder);

        assertThrows(IllegalStateException.class, () -> init.run());
    }

    @Test
    void shouldThrowWhenPasswordMissing() {
        org.springframework.test.util.ReflectionTestUtils.setField(adminInitializer, "adminUsername", "admin");

        assertThrows(IllegalStateException.class, () -> adminInitializer.run());
    }

    @Test
    void shouldCreateAdminWhenNotExists() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(adminInitializer, "adminUsername", "admin");
        org.springframework.test.util.ReflectionTestUtils.setField(adminInitializer, "adminPassword", "Test@12345");

        when(userService.getByUsername("admin")).thenReturn(null);

        SysRole superAdminRole = new SysRole();
        superAdminRole.setId(1L);
        superAdminRole.setRoleKey("super_admin");
        when(roleService.getOne(any(LambdaQueryWrapper.class))).thenReturn(superAdminRole);
        when(userService.save(any(SysUser.class))).thenReturn(true);

        adminInitializer.run();

        verify(userService).save(argThat(user ->
                user.getUsername().equals("admin")
                        && user.getRoleId().equals(1L)
                        && user.getStatus() == 1
                        && user.getMustChangePassword()));
    }

    @Test
    void shouldSkipWhenAdminAlreadyExists() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(adminInitializer, "adminUsername", "admin");
        org.springframework.test.util.ReflectionTestUtils.setField(adminInitializer, "adminPassword", "Test@12345");

        SysUser existingAdmin = new SysUser();
        existingAdmin.setId(10L);
        existingAdmin.setUsername("admin");
        when(userService.getByUsername("admin")).thenReturn(existingAdmin);

        adminInitializer.run();

        verify(userService, never()).save(any(SysUser.class));
    }

    @Test
    void shouldHashPasswordWithBCrypt() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(adminInitializer, "adminUsername", "admin");
        org.springframework.test.util.ReflectionTestUtils.setField(adminInitializer, "adminPassword", "Test@12345");

        when(userService.getByUsername("admin")).thenReturn(null);

        SysRole superAdminRole = new SysRole();
        superAdminRole.setId(1L);
        superAdminRole.setRoleKey("super_admin");
        when(roleService.getOne(any(LambdaQueryWrapper.class))).thenReturn(superAdminRole);
        when(userService.save(any(SysUser.class))).thenReturn(true);

        adminInitializer.run();

        verify(userService).save(argThat(user -> {
            String hash = user.getPasswordHash();
            return hash != null
                    && hash.startsWith("$2a$")
                    && passwordEncoder.matches("Test@12345", hash);
        }));
    }

    @Test
    void shouldBeIdempotentOnSecondRun() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(adminInitializer, "adminUsername", "admin");
        org.springframework.test.util.ReflectionTestUtils.setField(adminInitializer, "adminPassword", "Test@12345");

        when(userService.getByUsername("admin")).thenReturn(null);

        SysRole superAdminRole = new SysRole();
        superAdminRole.setId(1L);
        superAdminRole.setRoleKey("super_admin");
        when(roleService.getOne(any(LambdaQueryWrapper.class))).thenReturn(superAdminRole);
        when(userService.save(any(SysUser.class))).thenReturn(true);

        adminInitializer.run();

        SysUser existingAdmin = new SysUser();
        existingAdmin.setId(10L);
        existingAdmin.setUsername("admin");
        when(userService.getByUsername("admin")).thenReturn(existingAdmin);

        adminInitializer.run();

        verify(userService, times(1)).save(any(SysUser.class));
    }

    @Test
    void shouldSetCorrectDefaultFields() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(adminInitializer, "adminUsername", "admin");
        org.springframework.test.util.ReflectionTestUtils.setField(adminInitializer, "adminPassword", "Test@12345");

        when(userService.getByUsername("admin")).thenReturn(null);

        SysRole superAdminRole = new SysRole();
        superAdminRole.setId(2L);
        superAdminRole.setRoleKey("super_admin");
        when(roleService.getOne(any(LambdaQueryWrapper.class))).thenReturn(superAdminRole);
        when(userService.save(any(SysUser.class))).thenReturn(true);

        adminInitializer.run();

        verify(userService).save(argThat(user ->
                user.getNickname().equals("超级管理员")
                        && user.getStatus() == 1
                        && user.getMustChangePassword()
                        && user.getCreatedAt() != null
                        && user.getUpdatedAt() != null));
    }

    @Test
    void shouldThrowWhenSuperAdminRoleNotExists() {
        org.springframework.test.util.ReflectionTestUtils.setField(adminInitializer, "adminUsername", "admin");
        org.springframework.test.util.ReflectionTestUtils.setField(adminInitializer, "adminPassword", "Test@12345");

        when(userService.getByUsername("admin")).thenReturn(null);
        when(roleService.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> adminInitializer.run());
    }
}
