package com.digital.employee.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.digital.employee.common.core.Result;
import com.digital.employee.common.exception.BusinessException;
import com.digital.employee.system.domain.dto.RoleCreateDTO;
import com.digital.employee.system.domain.dto.RoleUpdateDTO;
import com.digital.employee.system.domain.entity.SysRole;
import com.digital.employee.system.domain.entity.SysRoleMenu;
import com.digital.employee.system.mapper.SysRoleMenuMapper;
import com.digital.employee.system.service.ISysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/role")
@Tag(name = "角色管理")
public class SysRoleController {

    private final ISysRoleService roleService;
    private final SysRoleMenuMapper roleMenuMapper;

    public SysRoleController(ISysRoleService roleService, SysRoleMenuMapper roleMenuMapper) {
        this.roleService = roleService;
        this.roleMenuMapper = roleMenuMapper;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询角色")
    public Result<Page<SysRole>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<SysRole> result = roleService.page(new Page<>(page, pageSize),
                new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询角色详情")
    public Result<SysRole> getById(@PathVariable Long id) {
        SysRole role = roleService.getById(id);
        if (role == null) {
            throw new BusinessException("ROLE_NOT_FOUND", "角色不存在");
        }
        return Result.success(role);
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "创建角色")
    public Result<Void> create(@RequestBody @Valid RoleCreateDTO dto) {
        SysRole existing = roleService.getOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, dto.getRoleKey()));
        if (existing != null) {
            throw new BusinessException("ROLE_EXISTS", "角色标识已存在");
        }

        SysRole role = new SysRole();
        role.setRoleKey(dto.getRoleKey());
        role.setRoleName(dto.getRoleName());
        role.setStatus(1);
        roleService.save(role);

        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            for (Long menuId : dto.getMenuIds()) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(role.getId());
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            }
        }

        roleService.incrementRoleVersion(dto.getRoleKey());
        return Result.success();
    }

    @PutMapping
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "更新角色")
    public Result<Void> update(@RequestBody @Valid RoleUpdateDTO dto) {
        SysRole role = roleService.getById(dto.getId());
        if (role == null) {
            throw new BusinessException("ROLE_NOT_FOUND", "角色不存在");
        }

        if (dto.getRoleName() != null) role.setRoleName(dto.getRoleName());
        if (dto.getStatus() != null) role.setStatus(dto.getStatus());
        roleService.updateById(role);

        if (dto.getMenuIds() != null) {
            roleMenuMapper.deleteByRoleId(dto.getId());
            for (Long menuId : dto.getMenuIds()) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(dto.getId());
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            }
            roleService.incrementRoleVersion(role.getRoleKey());
        }

        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        SysRole role = roleService.getById(id);
        if (role == null) {
            throw new BusinessException("ROLE_NOT_FOUND", "角色不存在");
        }
        roleService.removeById(id);
        roleService.incrementRoleVersion(role.getRoleKey());
        return Result.success();
    }
}
