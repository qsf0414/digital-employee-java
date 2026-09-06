package com.digital.employee;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.digital.employee.common.config.PasswordConfig;
import com.digital.employee.common.exception.GlobalExceptionHandler;
import com.digital.employee.common.redis.LoginRateLimiter;
import com.digital.employee.system.controller.AuthController;
import com.digital.employee.system.domain.entity.SysRole;
import com.digital.employee.system.domain.entity.SysUser;
import com.digital.employee.system.mapper.SysAuditLogMapper;
import com.digital.employee.system.mapper.SysMenuMapper;
import com.digital.employee.system.mapper.SysRoleMapper;
import com.digital.employee.system.mapper.SysRoleMenuMapper;
import com.digital.employee.system.mapper.SysUserMapper;
import com.digital.employee.system.service.ISysAuditLogService;
import com.digital.employee.system.service.ISysMenuService;
import com.digital.employee.system.service.ISysRoleService;
import com.digital.employee.system.service.ISysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(PasswordConfig.class)
class AuthControllerIntegrationTest {

    @Autowired
    private AuthController authController;

    private MockMvc mockMvc;

    @MockitoBean
    private ISysUserService userService;
    @MockitoBean
    private ISysRoleService roleService;
    @MockitoBean
    private ISysMenuService menuService;
    @MockitoBean
    private ISysAuditLogService auditLogService;
    @MockitoBean
    private LoginRateLimiter loginRateLimiter;
    @MockitoBean
    private SysRoleMenuMapper roleMenuMapper;
    @MockitoBean
    private SysRoleMapper roleMapper;
    @MockitoBean
    private SysUserMapper userMapper;
    @MockitoBean
    private SysMenuMapper menuMapper;
    @MockitoBean
    private SysAuditLogMapper auditLogMapper;
    @MockitoBean
    private StringRedisTemplate redisTemplate;
    @MockitoBean
    private PasswordEncoder passwordEncoder;

    private ValueOperations<String, String> valueOps;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        reset(userService, roleService, menuService, auditLogService,
                loginRateLimiter, redisTemplate, passwordEncoder);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private SysUser buildUser(long id, String username, String nickname, int status, long roleId) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("$2a$10$hashed");
        user.setNickname(nickname);
        user.setStatus(status);
        user.setRoleId(roleId);
        return user;
    }

    private SysRole buildRole(long id, String roleKey, String roleName, int status) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleKey(roleKey);
        role.setRoleName(roleName);
        role.setStatus(status);
        return role;
    }

    @Test
    void healthEndpointShouldReturnOK() throws Exception {
        mockMvc.perform(get("/api/v1/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void captchaEndpointShouldReturnCaptcha() throws Exception {
        mockMvc.perform(get("/api/v1/auth/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.captchaKey").isNotEmpty())
                .andExpect(jsonPath("$.data.captchaImage").isNotEmpty())
                .andExpect(jsonPath("$.data.captchaType").value("MATH"));
    }

    @Test
    void loginShouldFailWhenRateLimited() throws Exception {
        when(loginRateLimiter.isAllowed(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"admin\",\"password\":\"Test@12345\",\"captchaKey\":\"key1\",\"captchaCode\":\"8\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void loginShouldFailWhenCaptchaExpired() throws Exception {
        when(loginRateLimiter.isAllowed(anyString(), anyString())).thenReturn(true);
        when(valueOps.get("captcha:key1")).thenReturn(null);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"admin\",\"password\":\"Test@12345\",\"captchaKey\":\"key1\",\"captchaCode\":\"8\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CAPTCHA_EXPIRED"));
    }

    @Test
    void loginShouldFailWhenCaptchaWrong() throws Exception {
        when(loginRateLimiter.isAllowed(anyString(), anyString())).thenReturn(true);
        when(valueOps.get("captcha:key1")).thenReturn("8");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"admin\",\"password\":\"Test@12345\",\"captchaKey\":\"key1\",\"captchaCode\":\"5\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CAPTCHA_INVALID"));
    }

    @Test
    void loginShouldFailWhenUserNotFound() throws Exception {
        when(loginRateLimiter.isAllowed(anyString(), anyString())).thenReturn(true);
        when(valueOps.get("captcha:key1")).thenReturn("8");
        when(userService.getByUsername("nobody")).thenReturn(null);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"nobody\",\"password\":\"Test@12345\",\"captchaKey\":\"key1\",\"captchaCode\":\"8\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
    }

    @Test
    void loginShouldFailWhenUserDisabled() throws Exception {
        when(loginRateLimiter.isAllowed(anyString(), anyString())).thenReturn(true);
        when(valueOps.get("captcha:key1")).thenReturn("8");
        when(userService.getByUsername("disabled")).thenReturn(buildUser(2L, "disabled", "禁用用户", 0, 2L));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"disabled\",\"password\":\"Test@12345\",\"captchaKey\":\"key1\",\"captchaCode\":\"8\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
    }

    @Test
    void loginShouldFailWhenPasswordWrong() throws Exception {
        when(loginRateLimiter.isAllowed(anyString(), anyString())).thenReturn(true);
        when(valueOps.get("captcha:key1")).thenReturn("8");
        when(userService.getByUsername("admin")).thenReturn(buildUser(1L, "admin", "管理员", 1, 1L));
        when(passwordEncoder.matches("wrong", "$2a$10$hashed")).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"admin\",\"password\":\"wrong\",\"captchaKey\":\"key1\",\"captchaCode\":\"8\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_FAILED"));
    }

    @Test
    void loginShouldSucceedAndReturnToken() throws Exception {
        when(loginRateLimiter.isAllowed(anyString(), anyString())).thenReturn(true);
        when(valueOps.get("captcha:key1")).thenReturn("8");
        when(userService.getByUsername("admin")).thenReturn(buildUser(1L, "admin", "管理员", 1, 1L));
        when(passwordEncoder.matches("Test@12345", "$2a$10$hashed")).thenReturn(true);

        try (var stpMock = mockStatic(StpUtil.class)) {
            stpMock.when(() -> StpUtil.login(anyLong())).then(invocation -> null);
            stpMock.when(StpUtil::getTokenValue).thenReturn("test-token-abc");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType("application/json")
                            .content("{\"username\":\"admin\",\"password\":\"Test@12345\",\"captchaKey\":\"key1\",\"captchaCode\":\"8\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"))
                    .andExpect(jsonPath("$.data").value("test-token-abc"));
        }
    }

    @Test
    void meShouldReturn401WhenNotLoggedIn() throws Exception {
        try (var stpMock = mockStatic(StpUtil.class)) {
            stpMock.when(StpUtil::getLoginIdAsLong).thenThrow(new NotLoginException("未登录", "-", "not-login"));

            mockMvc.perform(get("/api/v1/auth/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void meShouldReturnUserInfoWhenLoggedIn() throws Exception {
        SysUser user = buildUser(1L, "admin", "管理员", 1, 1L);
        SysRole role = buildRole(1L, "super_admin", "超级管理员", 1);

        when(userService.getById(1L)).thenReturn(user);
        when(roleService.getRoleByUserId(1L)).thenReturn(role);
        when(menuService.selectMenusByRoleId(1L)).thenReturn(List.of());

        try (var stpMock = mockStatic(StpUtil.class)) {
            stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            mockMvc.perform(get("/api/v1/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"))
                    .andExpect(jsonPath("$.data.user.nickname").value("管理员"))
                    .andExpect(jsonPath("$.data.roles[0]").value("super_admin"))
                    .andExpect(jsonPath("$.data.permissions[0]").value("*:*:*"));
        }
    }

    @Test
    void meShouldReturnEmptyRolesWhenRoleDisabled() throws Exception {
        SysUser user = buildUser(3L, "guest", "访客", 1, 3L);
        SysRole disabledRole = buildRole(3L, "guest", "访客角色", 0);

        when(userService.getById(3L)).thenReturn(user);
        when(roleService.getRoleByUserId(3L)).thenReturn(disabledRole);

        try (var stpMock = mockStatic(StpUtil.class)) {
            stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(3L);

            mockMvc.perform(get("/api/v1/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.roles").isEmpty())
                    .andExpect(jsonPath("$.data.permissions").isEmpty())
                    .andExpect(jsonPath("$.data.menus").isEmpty());
        }
    }

    @Test
    void meShouldBuildMenuTreeCorrectly() throws Exception {
        SysUser user = buildUser(1L, "admin", "管理员", 1, 1L);
        SysRole role = buildRole(1L, "super_admin", "超级管理员", 1);

        com.digital.employee.system.domain.entity.SysMenu menu =
                new com.digital.employee.system.domain.entity.SysMenu();
        menu.setId(1L);
        menu.setParentId(0L);
        menu.setTitle("系统管理");
        menu.setMenuType("M");
        menu.setPath("/system");
        menu.setIcon("setting");

        com.digital.employee.system.domain.entity.SysMenu childMenu =
                new com.digital.employee.system.domain.entity.SysMenu();
        childMenu.setId(2L);
        childMenu.setParentId(1L);
        childMenu.setTitle("用户管理");
        childMenu.setMenuType("C");
        childMenu.setPath("/system/user");
        childMenu.setComponent("system/user/UserManage");

        when(userService.getById(1L)).thenReturn(user);
        when(roleService.getRoleByUserId(1L)).thenReturn(role);
        when(menuService.selectMenusByRoleId(1L)).thenReturn(List.of(menu, childMenu));

        try (var stpMock = mockStatic(StpUtil.class)) {
            stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            mockMvc.perform(get("/api/v1/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.menus[0].title").value("系统管理"))
                    .andExpect(jsonPath("$.data.menus[0].children[0].title").value("用户管理"));
        }
    }

    @Test
    void loginShouldFailWithEmptyBody() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
