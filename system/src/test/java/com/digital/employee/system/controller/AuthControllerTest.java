package com.digital.employee.system.controller;

import com.digital.employee.common.core.Result;
import com.digital.employee.system.domain.entity.SysMenu;
import com.digital.employee.system.domain.entity.SysRole;
import com.digital.employee.system.domain.entity.SysUser;
import com.digital.employee.system.service.ISysAuditLogService;
import com.digital.employee.system.service.ISysMenuService;
import com.digital.employee.system.service.ISysRoleService;
import com.digital.employee.system.service.ISysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private ISysUserService userService;

    @Mock
    private ISysRoleService roleService;

    @Mock
    private ISysMenuService menuService;

    @Mock
    private ISysAuditLogService auditLogService;

    @Mock
    private com.digital.employee.common.redis.LoginRateLimiter loginRateLimiter;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private AuthController authController;

    private SysUser sampleUser;
    private SysRole sampleRole;

    @BeforeEach
    void setUp() {
        cn.dev33.satoken.context.mock.SaTokenContextMockUtil.setMockContext();

        sampleUser = new SysUser();
        sampleUser.setId(1L);
        sampleUser.setUsername("admin");
        sampleUser.setPasswordHash("$2a$hashed");
        sampleUser.setNickname("管理员");
        sampleUser.setStatus(1);
        sampleUser.setRoleId(1L);

        sampleRole = new SysRole();
        sampleRole.setId(1L);
        sampleRole.setRoleKey("super_admin");
        sampleRole.setRoleName("超级管理员");
        sampleRole.setStatus(1);
    }

    @AfterEach
    void tearDown() {
        cn.dev33.satoken.stp.StpUtil.logout();
        cn.dev33.satoken.context.mock.SaTokenContextMockUtil.clearContext();
    }

    @Test
    void healthShouldReturnUP() {
        Result<java.util.Map<String, Object>> result = authController.health();

        assertEquals("OK", result.getCode());
        assertNotNull(result.getData());
        assertEquals("UP", result.getData().get("status"));
    }

    @Test
    void meShouldReturnUserWithSuperAdminPermissions() {
        cn.dev33.satoken.stp.StpUtil.login(1L);

        when(userService.getById(1L)).thenReturn(sampleUser);
        when(roleService.getRoleByUserId(1L)).thenReturn(sampleRole);
        when(menuService.selectMenusByRoleId(1L)).thenReturn(List.of());

        Result<com.digital.employee.system.domain.vo.MeVO> result = authController.me();

        assertEquals("OK", result.getCode());
        assertNotNull(result.getData());
        assertEquals("admin", result.getData().getUser().getUsername());
        assertTrue(result.getData().getPermissions().contains("*:*:*"));

        cn.dev33.satoken.stp.StpUtil.logout();
    }

    @Test
    void captchaShouldGenerateKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        Result<AuthController.CaptchaVO> result = authController.captcha();

        assertEquals("OK", result.getCode());
        assertNotNull(result.getData());
        assertNotNull(result.getData().getCaptchaKey());
        assertNotNull(result.getData().getCaptchaImage());
        assertEquals("MATH", result.getData().getCaptchaType());
    }

    @Test
    void loginShouldFailWhenCaptchaExpired() {
        when(loginRateLimiter.isAllowed(anyString(), anyString())).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);

        com.digital.employee.system.domain.dto.LoginDTO dto = new com.digital.employee.system.domain.dto.LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("Test@12345");
        dto.setCaptchaKey("key123");
        dto.setCaptchaCode("12");

        var ex = assertThrows(com.digital.employee.common.exception.BusinessException.class,
                () -> authController.login(dto, createMockRequest()));
        assertEquals("CAPTCHA_EXPIRED", ex.getCode());
    }

    @Test
    void loginShouldFailWhenRateLimited() {
        when(loginRateLimiter.isAllowed(anyString(), anyString())).thenReturn(false);

        com.digital.employee.system.domain.dto.LoginDTO dto = new com.digital.employee.system.domain.dto.LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("Test@12345");
        dto.setCaptchaKey("key123");
        dto.setCaptchaCode("12");

        var ex = assertThrows(com.digital.employee.common.exception.BusinessException.class,
                () -> authController.login(dto, createMockRequest()));
        assertEquals("TOO_MANY_REQUESTS", ex.getCode());
    }

    @Test
    void loginShouldFailWhenCaptchaWrong() {
        when(loginRateLimiter.isAllowed(anyString(), anyString())).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("captcha:key123")).thenReturn("30");

        com.digital.employee.system.domain.dto.LoginDTO dto = new com.digital.employee.system.domain.dto.LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("Test@12345");
        dto.setCaptchaKey("key123");
        dto.setCaptchaCode("20");

        var ex = assertThrows(com.digital.employee.common.exception.BusinessException.class,
                () -> authController.login(dto, createMockRequest()));
        assertEquals("CAPTCHA_INVALID", ex.getCode());
    }

    @Test
    void loginShouldFailWhenUserNotFound() {
        when(loginRateLimiter.isAllowed(anyString(), anyString())).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("captcha:key123")).thenReturn("30");
        when(userService.getByUsername("admin")).thenReturn(null);

        com.digital.employee.system.domain.dto.LoginDTO dto = new com.digital.employee.system.domain.dto.LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("Test@12345");
        dto.setCaptchaKey("key123");
        dto.setCaptchaCode("30");

        var ex = assertThrows(com.digital.employee.common.exception.BusinessException.class,
                () -> authController.login(dto, createMockRequest()));
        assertEquals("AUTH_FAILED", ex.getCode());
    }

    @Test
    void loginShouldFailWhenPasswordWrong() {
        when(loginRateLimiter.isAllowed(anyString(), anyString())).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("captcha:key123")).thenReturn("30");
        when(userService.getByUsername("admin")).thenReturn(sampleUser);
        when(passwordEncoder.matches("Test@12345", "$2a$hashed")).thenReturn(false);

        com.digital.employee.system.domain.dto.LoginDTO dto = new com.digital.employee.system.domain.dto.LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("Test@12345");
        dto.setCaptchaKey("key123");
        dto.setCaptchaCode("30");

        var ex = assertThrows(com.digital.employee.common.exception.BusinessException.class,
                () -> authController.login(dto, createMockRequest()));
        assertEquals("AUTH_FAILED", ex.getCode());
    }

    @Test
    void loginShouldFailWhenUserDisabled() {
        SysUser disabledUser = new SysUser();
        disabledUser.setId(2L);
        disabledUser.setUsername("disabled");
        disabledUser.setPasswordHash("$2a$hash");
        disabledUser.setStatus(0);

        when(loginRateLimiter.isAllowed(anyString(), anyString())).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("captcha:key123")).thenReturn("30");
        when(userService.getByUsername("disabled")).thenReturn(disabledUser);

        com.digital.employee.system.domain.dto.LoginDTO dto = new com.digital.employee.system.domain.dto.LoginDTO();
        dto.setUsername("disabled");
        dto.setPassword("Test@12345");
        dto.setCaptchaKey("key123");
        dto.setCaptchaCode("30");

        var ex = assertThrows(com.digital.employee.common.exception.BusinessException.class,
                () -> authController.login(dto, createMockRequest()));
        assertEquals("AUTH_FAILED", ex.getCode());
        assertEquals("账号已禁用", ex.getMessage());
    }

    @Test
    void loginShouldSucceed() {
        when(loginRateLimiter.isAllowed(anyString(), anyString())).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("captcha:key123")).thenReturn("30");
        when(userService.getByUsername("admin")).thenReturn(sampleUser);
        when(passwordEncoder.matches("Test@12345", "$2a$hashed")).thenReturn(true);

        com.digital.employee.system.domain.dto.LoginDTO dto = new com.digital.employee.system.domain.dto.LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("Test@12345");
        dto.setCaptchaKey("key123");
        dto.setCaptchaCode("30");

        Result<String> result = authController.login(dto, createMockRequest());

        assertEquals("OK", result.getCode());
        assertNotNull(result.getData());
        verify(auditLogService).recordLogin(eq(1L), eq("admin"), anyString(), eq(true), isNull());
        verify(redisTemplate).delete("captcha:key123");

        cn.dev33.satoken.stp.StpUtil.logout();
    }

    @Test
    void meShouldReturnUserWithNormalRole() {
        cn.dev33.satoken.stp.StpUtil.login(2L);

        SysUser normalUser = new SysUser();
        normalUser.setId(2L);
        normalUser.setUsername("user1");
        normalUser.setNickname("普通用户");
        normalUser.setStatus(1);
        normalUser.setRoleId(2L);

        SysRole normalRole = new SysRole();
        normalRole.setId(2L);
        normalRole.setRoleKey("user");
        normalRole.setStatus(1);

        when(userService.getById(2L)).thenReturn(normalUser);
        when(roleService.getRoleByUserId(2L)).thenReturn(normalRole);
        when(menuService.selectMenusByRoleId(2L)).thenReturn(List.of());

        Result<com.digital.employee.system.domain.vo.MeVO> result = authController.me();

        assertEquals("OK", result.getCode());
        assertEquals("user1", result.getData().getUser().getUsername());
        assertEquals(List.of("user"), result.getData().getRoles());
        assertTrue(result.getData().getPermissions().isEmpty());

        cn.dev33.satoken.stp.StpUtil.logout();
    }

    @Test
    void meShouldBuildMenuTree() {
        cn.dev33.satoken.stp.StpUtil.login(1L);

        SysMenu menu = new SysMenu();
        menu.setId(10L);
        menu.setParentId(0L);
        menu.setTitle("用户管理");
        menu.setMenuType("M");
        menu.setPath("/user");
        menu.setComponent("system/user/index");
        menu.setIcon("user");

        SysMenu button = new SysMenu();
        button.setId(11L);
        button.setParentId(10L);
        button.setTitle("用户列表");
        button.setMenuType("F");
        button.setPath("/user/list");

        when(userService.getById(1L)).thenReturn(sampleUser);
        when(roleService.getRoleByUserId(1L)).thenReturn(sampleRole);
        when(menuService.selectMenusByRoleId(1L)).thenReturn(List.of(menu, button));

        Result<com.digital.employee.system.domain.vo.MeVO> result = authController.me();

        assertEquals("OK", result.getCode());
        assertFalse(result.getData().getMenus().isEmpty());
        assertEquals("用户管理", result.getData().getMenus().get(0).getTitle());

        cn.dev33.satoken.stp.StpUtil.logout();
    }

    @Test
    void meShouldReturnEmptyWhenUserNotFound() {
        cn.dev33.satoken.stp.StpUtil.login(999L);
        when(userService.getById(999L)).thenReturn(null);

        assertThrows(com.digital.employee.common.exception.BusinessException.class,
                () -> authController.me());

        cn.dev33.satoken.stp.StpUtil.logout();
    }

    @Test
    void meShouldReturnEmptyRolesWhenRoleDisabled() {
        cn.dev33.satoken.stp.StpUtil.login(3L);

        SysUser user = new SysUser();
        user.setId(3L);
        user.setUsername("disabled_role_user");
        user.setStatus(1);

        SysRole disabledRole = new SysRole();
        disabledRole.setId(3L);
        disabledRole.setRoleKey("guest");
        disabledRole.setStatus(0);

        when(userService.getById(3L)).thenReturn(user);
        when(roleService.getRoleByUserId(3L)).thenReturn(disabledRole);

        Result<com.digital.employee.system.domain.vo.MeVO> result = authController.me();

        assertEquals("OK", result.getCode());
        assertTrue(result.getData().getRoles().isEmpty());
        assertTrue(result.getData().getPermissions().isEmpty());
        assertTrue(result.getData().getMenus().isEmpty());

        cn.dev33.satoken.stp.StpUtil.logout();
    }

    @Test
    void loginShouldExtractIpFromXForwardedFor() {
        lenient().when(loginRateLimiter.isAllowed(anyString(), anyString())).thenReturn(true);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.get("captcha:key123")).thenReturn("30");
        lenient().when(userService.getByUsername("admin")).thenReturn(sampleUser);
        lenient().when(passwordEncoder.matches("Test@12345", "$2a$hashed")).thenReturn(true);

        jakarta.servlet.http.HttpServletRequest req = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 192.168.1.1");
        lenient().when(req.getHeader("X-Real-IP")).thenReturn(null);
        lenient().when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        com.digital.employee.system.domain.dto.LoginDTO dto = new com.digital.employee.system.domain.dto.LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("Test@12345");
        dto.setCaptchaKey("key123");
        dto.setCaptchaCode("30");

        Result<String> result = authController.login(dto, req);

        assertEquals("OK", result.getCode());
        verify(auditLogService).recordLogin(eq(1L), eq("admin"), eq("10.0.0.1"), eq(true), isNull());

        cn.dev33.satoken.stp.StpUtil.logout();
    }

    @Test
    void loginShouldExtractIpFromXRealIP() {
        lenient().when(loginRateLimiter.isAllowed(anyString(), anyString())).thenReturn(true);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.get("captcha:key123")).thenReturn("30");
        lenient().when(userService.getByUsername("admin")).thenReturn(sampleUser);
        lenient().when(passwordEncoder.matches("Test@12345", "$2a$hashed")).thenReturn(true);

        jakarta.servlet.http.HttpServletRequest req = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(req.getHeader("X-Real-IP")).thenReturn("10.0.0.2");
        lenient().when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        com.digital.employee.system.domain.dto.LoginDTO dto = new com.digital.employee.system.domain.dto.LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("Test@12345");
        dto.setCaptchaKey("key123");
        dto.setCaptchaCode("30");

        Result<String> result = authController.login(dto, req);

        assertEquals("OK", result.getCode());
        verify(auditLogService).recordLogin(eq(1L), eq("admin"), eq("10.0.0.2"), eq(true), isNull());

        cn.dev33.satoken.stp.StpUtil.logout();
    }

    @Test
    void loginShouldExtractIpFromRemoteAddr() {
        when(loginRateLimiter.isAllowed(anyString(), anyString())).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("captcha:key123")).thenReturn("30");
        when(userService.getByUsername("admin")).thenReturn(sampleUser);
        when(passwordEncoder.matches("Test@12345", "$2a$hashed")).thenReturn(true);

        jakarta.servlet.http.HttpServletRequest req = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("192.168.1.100");

        com.digital.employee.system.domain.dto.LoginDTO dto = new com.digital.employee.system.domain.dto.LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("Test@12345");
        dto.setCaptchaKey("key123");
        dto.setCaptchaCode("30");

        Result<String> result = authController.login(dto, req);

        assertEquals("OK", result.getCode());
        verify(auditLogService).recordLogin(eq(1L), eq("admin"), eq("192.168.1.100"), eq(true), isNull());

        cn.dev33.satoken.stp.StpUtil.logout();
    }

    private jakarta.servlet.http.HttpServletRequest createMockRequest() {
        jakarta.servlet.http.HttpServletRequest req = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");
        return req;
    }
}
