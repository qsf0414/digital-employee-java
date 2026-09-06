package com.digital.employee.system.initializer;

import com.digital.employee.system.domain.entity.SysRole;
import com.digital.employee.system.domain.entity.SysUser;
import com.digital.employee.system.service.ISysRoleService;
import com.digital.employee.system.service.ISysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final ISysUserService userService;
    private final ISysRoleService roleService;
    private final PasswordEncoder passwordEncoder;

    @Value("${initial.admin.username:}")
    private String adminUsername;

    @Value("${initial.admin.password:}")
    private String adminPassword;

    public AdminInitializer(ISysUserService userService, ISysRoleService roleService,
                            PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(String... args) {
        if (adminUsername == null || adminUsername.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            log.error("FATAL: 环境变量 INITIAL_ADMIN_USERNAME 和 INITIAL_ADMIN_PASSWORD 必须设置，否则系统无法启动");
            throw new IllegalStateException(
                    "环境变量 INITIAL_ADMIN_USERNAME 和 INITIAL_ADMIN_PASSWORD 必须设置，否则系统无法启动");
        }

        SysUser existing = userService.getByUsername(adminUsername);
        if (existing != null) {
            log.info("管理员账号 '{}' 已存在，跳过初始化", adminUsername);
            return;
        }

        SysRole superAdminRole = roleService.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleKey, "super_admin"));
        if (superAdminRole == null) {
            log.error("FATAL: super_admin 角色不存在，请先执行数据库迁移脚本");
            throw new IllegalStateException("super_admin 角色不存在，请先执行数据库迁移脚本");
        }

        SysUser admin = new SysUser();
        admin.setUsername(adminUsername);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setNickname("超级管理员");
        admin.setRoleId(superAdminRole.getId());
        admin.setStatus(1);
        admin.setMustChangePassword(true);
        admin.setCreatedAt(java.time.OffsetDateTime.now());
        admin.setUpdatedAt(java.time.OffsetDateTime.now());
        userService.save(admin);

        log.info("管理员账号 '{}' 初始化成功，role_id={}", adminUsername, superAdminRole.getId());
    }
}
