package com.digital.employee.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.digital.employee.common.core.Result;
import com.digital.employee.common.exception.BusinessException;
import com.digital.employee.system.domain.dto.MenuCreateDTO;
import com.digital.employee.system.domain.entity.SysMenu;
import com.digital.employee.system.service.ISysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/system/menu")
@Tag(name = "菜单管理")
public class SysMenuController {

    private final ISysMenuService menuService;

    public SysMenuController(ISysMenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/tree")
    @SaCheckPermission("admin:menu:readonly")
    @Operation(summary = "获取完整菜单树")
    public Result<List<Map<String, Object>>> tree() {
        List<SysMenu> menus = menuService.list(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSortOrder));
        List<Map<String, Object>> tree = buildFullTree(menus, 0L);
        return Result.success(tree);
    }

    @GetMapping("/list")
    @SaCheckPermission("admin:menu:readonly")
    @Operation(summary = "获取全部菜单列表")
    public Result<List<SysMenu>> list() {
        return Result.success(menuService.list(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSortOrder)));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("admin:menu:readonly")
    @Operation(summary = "查询菜单详情")
    public Result<SysMenu> getById(@PathVariable Long id) {
        SysMenu menu = menuService.getById(id);
        if (menu == null) {
            throw new BusinessException("MENU_NOT_FOUND", "菜单不存在");
        }
        return Result.success(menu);
    }

    @PostMapping
    @SaCheckPermission("admin:menu:manage")
    @Operation(summary = "创建菜单")
    public Result<Void> create(@RequestBody @Valid MenuCreateDTO dto) {
        SysMenu menu = new SysMenu();
        menu.setParentId(dto.getParentId());
        menu.setTitle(dto.getTitle());
        menu.setMenuType(dto.getMenuType());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setPerms(dto.getPerms());
        menu.setIcon(dto.getIcon());
        menu.setSortOrder(dto.getSortOrder());
        menu.setVisible(dto.getVisible());
        menuService.save(menu);
        return Result.success();
    }

    @PutMapping
    @SaCheckPermission("admin:menu:manage")
    @Operation(summary = "更新菜单")
    public Result<Void> update(@RequestBody SysMenu menu) {
        if (menu.getId() == null) {
            throw new BusinessException("INVALID_PARAM", "菜单ID不能为空");
        }
        menuService.updateById(menu);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("admin:menu:manage")
    @Operation(summary = "删除菜单")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.removeById(id);
        return Result.success();
    }

    private List<Map<String, Object>> buildFullTree(List<SysMenu> menus, Long parentId) {
        Map<Long, List<SysMenu>> grouped = menus.stream()
                .collect(Collectors.groupingBy(SysMenu::getParentId));
        return buildTreeNodes(grouped, parentId);
    }

    private List<Map<String, Object>> buildTreeNodes(Map<Long, List<SysMenu>> grouped, Long parentId) {
        List<SysMenu> children = grouped.getOrDefault(parentId, List.of());
        return children.stream().map(m -> {
            Map<String, Object> node = new java.util.LinkedHashMap<>();
            node.put("id", m.getId());
            node.put("parentId", m.getParentId());
            node.put("title", m.getTitle());
            node.put("menuType", m.getMenuType());
            node.put("path", m.getPath());
            node.put("component", m.getComponent());
            node.put("perms", m.getPerms());
            node.put("icon", m.getIcon());
            node.put("sortOrder", m.getSortOrder());
            node.put("visible", m.getVisible());
            node.put("children", buildTreeNodes(grouped, m.getId()));
            return node;
        }).collect(Collectors.toList());
    }
}
