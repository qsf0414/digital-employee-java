package com.digital.employee.system.controller;

import com.digital.employee.common.core.Result;
import com.digital.employee.common.exception.BusinessException;
import com.digital.employee.common.redis.LoginRateLimiter;
import com.digital.employee.system.domain.dto.LoginDTO;
import com.digital.employee.system.domain.entity.SysMenu;
import com.digital.employee.system.domain.entity.SysUser;
import com.digital.employee.system.domain.vo.MeVO;
import com.digital.employee.system.domain.vo.MenuTreeVO;
import com.digital.employee.system.service.ISysAuditLogService;
import com.digital.employee.system.service.ISysMenuService;
import com.digital.employee.system.service.ISysRoleService;
import com.digital.employee.system.service.ISysUserService;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final long CAPTCHA_TTL_MINUTES = 5;

    private final ISysUserService userService;
    private final ISysRoleService roleService;
    private final ISysMenuService menuService;
    private final ISysAuditLogService auditLogService;
    private final LoginRateLimiter loginRateLimiter;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    public AuthController(ISysUserService userService, ISysRoleService roleService,
                          ISysMenuService menuService, ISysAuditLogService auditLogService,
                          LoginRateLimiter loginRateLimiter, PasswordEncoder passwordEncoder,
                          StringRedisTemplate redisTemplate) {
        this.userService = userService;
        this.roleService = roleService;
        this.menuService = menuService;
        this.auditLogService = auditLogService;
        this.loginRateLimiter = loginRateLimiter;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginDTO dto, HttpServletRequest request) {
        String ip = getClientIp(request);

        if (!loginRateLimiter.isAllowed(ip, dto.getUsername())) {
            throw new BusinessException("TOO_MANY_REQUESTS", "登录尝试过于频繁，请15分钟后再试");
        }

        String captchaAnswer = redisTemplate.opsForValue().get(CAPTCHA_PREFIX + dto.getCaptchaKey());
        if (captchaAnswer == null) {
            throw new BusinessException("CAPTCHA_EXPIRED", "验证码已过期或已使用");
        }
        redisTemplate.delete(CAPTCHA_PREFIX + dto.getCaptchaKey());

        if (!captchaAnswer.equalsIgnoreCase(dto.getCaptchaCode().trim())) {
            throw new BusinessException("CAPTCHA_INVALID", "验证码错误");
        }

        SysUser user = userService.getByUsername(dto.getUsername());
        if (user == null) {
            auditLogService.recordLogin(null, dto.getUsername(), ip, false, "用户不存在");
            throw new BusinessException("AUTH_FAILED", "用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            auditLogService.recordLogin(user.getId(), user.getUsername(), ip, false, "账号已禁用");
            throw new BusinessException("AUTH_FAILED", "账号已禁用");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            auditLogService.recordLogin(user.getId(), user.getUsername(), ip, false, "密码错误");
            throw new BusinessException("AUTH_FAILED", "用户名或密码错误");
        }

        cn.dev33.satoken.stp.StpUtil.login(user.getId());
        auditLogService.recordLogin(user.getId(), user.getUsername(), ip, true, null);

        return Result.success(StpUtil.getTokenValue());
    }

    @GetMapping("/me")
    public Result<MeVO> me() {
        Long userId = StpUtil.getLoginIdAsLong();

        SysUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }

        var role = roleService.getRoleByUserId(userId);
        List<String> roles = (role != null && role.getStatus() == 1)
                ? List.of(role.getRoleKey()) : List.of();

        List<String> permissions = roleService.getRoleByUserId(userId) != null
                ? getPermissionList(userId) : List.of();

        List<SysMenu> allMenus = role != null
                ? menuService.selectMenusByRoleId(role.getId()) : List.of();
        List<MenuTreeVO> menuTree = buildMenuTree(allMenus, 0L);

        return Result.success(MeVO.builder()
                .user(MeVO.UserInfo.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .build())
                .roles(roles)
                .permissions(permissions)
                .menus(menuTree)
                .build());
    }

    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        String key = UUID.randomUUID().toString();
        int a = new Random().nextInt(50) + 1;
        int b = new Random().nextInt(50) + 1;
        String answer = String.valueOf(a + b);
        String question = a + " + " + b + " = ?";

        redisTemplate.opsForValue().set(CAPTCHA_PREFIX + key, answer, CAPTCHA_TTL_MINUTES, java.util.concurrent.TimeUnit.MINUTES);

        return Result.success(new CaptchaVO(key, question, "MATH"));
    }

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.success(Map.of(
                "status", "UP",
                "timestamp", java.time.OffsetDateTime.now().toString()
        ));
    }

    private List<String> getPermissionList(Long userId) {
        var role = roleService.getRoleByUserId(userId);
        if (role == null || role.getStatus() == 0) {
            return List.of();
        }
        if ("super_admin".equals(role.getRoleKey())) {
            return List.of("*:*:*");
        }
        return menuService.selectPermsByRoleId(role.getId());
    }

    private List<MenuTreeVO> buildMenuTree(List<SysMenu> menus, Long parentId) {
        Map<Long, List<SysMenu>> grouped = menus.stream()
                .filter(m -> !"F".equals(m.getMenuType()))
                .collect(Collectors.groupingBy(SysMenu::getParentId));
        return buildChildren(grouped, parentId);
    }

    private List<MenuTreeVO> buildChildren(Map<Long, List<SysMenu>> grouped, Long parentId) {
        List<SysMenu> children = grouped.getOrDefault(parentId, List.of());
        return children.stream().map(m -> MenuTreeVO.builder()
                .id(m.getId())
                .parentId(m.getParentId())
                .title(m.getTitle())
                .menuType(m.getMenuType())
                .path(m.getPath())
                .component(m.getComponent())
                .icon(m.getIcon())
                .children(buildChildren(grouped, m.getId()))
                .build()).collect(Collectors.toList());
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @Data
    static class CaptchaVO {
        private final String captchaKey;
        private final String captchaImage;
        private final String captchaType;
    }
}
