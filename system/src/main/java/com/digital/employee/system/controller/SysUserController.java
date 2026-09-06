package com.digital.employee.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.digital.employee.common.core.Result;
import com.digital.employee.common.exception.BusinessException;
import com.digital.employee.system.domain.dto.UserCreateDTO;
import com.digital.employee.system.domain.dto.UserUpdateDTO;
import com.digital.employee.system.domain.entity.SysUser;
import com.digital.employee.system.service.ISysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/user")
@Tag(name = "用户管理")
public class SysUserController {

    private final ISysUserService userService;

    public SysUserController(ISysUserService userService) {
        this.userService = userService;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询用户")
    public Result<Page<SysUser>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(userService.pageUsers(page, pageSize, keyword));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情")
    public Result<SysUser> getById(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }
        user.setPasswordHash(null);
        return Result.success(user);
    }

    @PostMapping
    @Operation(summary = "创建用户")
    public Result<Void> create(@RequestBody @Valid UserCreateDTO dto) {
        SysUser existing = userService.getByUsername(dto.getUsername());
        if (existing != null) {
            throw new BusinessException("USER_EXISTS", "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setRoleId(dto.getRoleId());
        userService.createUser(user, dto.getPassword());
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "更新用户")
    public Result<Void> update(@RequestBody @Valid UserUpdateDTO dto) {
        SysUser user = userService.getById(dto.getId());
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getRoleId() != null) user.setRoleId(dto.getRoleId());
        if (dto.getStatus() != null) user.setStatus(dto.getStatus());
        userService.updateById(user);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }
        userService.removeById(id);
        return Result.success();
    }

    @PutMapping("/{id}/reset-password")
    @Operation(summary = "重置用户密码")
    public Result<Void> resetPassword(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }
        user.setMustChangePassword(true);
        userService.updateById(user);
        return Result.success();
    }
}
