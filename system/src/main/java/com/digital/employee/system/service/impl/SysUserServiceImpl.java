package com.digital.employee.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.digital.employee.system.domain.entity.SysUser;
import com.digital.employee.system.mapper.SysUserMapper;
import com.digital.employee.system.service.ISysUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    private final PasswordEncoder passwordEncoder;

    public SysUserServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public SysUser getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
    }

    @Override
    public void createUser(SysUser user, String rawPassword) {
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        if (user.getMustChangePassword() == null) {
            user.setMustChangePassword(true);
        }
        save(user);
    }

    @Override
    public Page<SysUser> pageUsers(int page, int pageSize, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);
        return page(new Page<>(page, pageSize), wrapper);
    }
}
