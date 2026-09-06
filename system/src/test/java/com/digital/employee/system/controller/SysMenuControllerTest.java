package com.digital.employee.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.digital.employee.common.exception.BusinessException;
import com.digital.employee.system.domain.dto.MenuCreateDTO;
import com.digital.employee.system.domain.entity.SysMenu;
import com.digital.employee.system.service.ISysMenuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysMenuControllerTest {

    @Mock
    private ISysMenuService menuService;

    @InjectMocks
    private SysMenuController menuController;

    private SysMenu sampleMenu;

    @BeforeEach
    void setUp() {
        sampleMenu = new SysMenu();
        sampleMenu.setId(1L);
        sampleMenu.setParentId(0L);
        sampleMenu.setTitle("系统管理");
        sampleMenu.setMenuType("M");
        sampleMenu.setPath("/system");
        sampleMenu.setIcon("setting");
        sampleMenu.setSortOrder(1);
        sampleMenu.setVisible(1);
    }

    @Test
    void listShouldReturnAllMenus() {
        when(menuService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(sampleMenu));

        var result = menuController.list();

        assertEquals("OK", result.getCode());
        assertEquals(1, result.getData().size());
    }

    @Test
    void getByIdShouldReturnMenu() {
        when(menuService.getById(1L)).thenReturn(sampleMenu);

        var result = menuController.getById(1L);

        assertEquals("OK", result.getCode());
        assertEquals("系统管理", result.getData().getTitle());
    }

    @Test
    void getByIdShouldThrowWhenNotFound() {
        when(menuService.getById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> menuController.getById(999L));
    }

    @Test
    void createShouldSaveMenu() {
        when(menuService.save(any())).thenReturn(true);

        MenuCreateDTO dto = new MenuCreateDTO();
        dto.setParentId(0L);
        dto.setTitle("新菜单");
        dto.setMenuType("C");
        dto.setPath("/new");
        dto.setComponent("NewComponent");

        var result = menuController.create(dto);

        assertEquals("OK", result.getCode());
    }

    @Test
    void updateShouldModifyMenu() {
        when(menuService.updateById(any())).thenReturn(true);

        SysMenu menu = new SysMenu();
        menu.setId(1L);
        menu.setTitle("更新标题");

        var result = menuController.update(menu);

        assertEquals("OK", result.getCode());
    }

    @Test
    void updateShouldThrowWhenNoId() {
        SysMenu menu = new SysMenu();

        assertThrows(BusinessException.class, () -> menuController.update(menu));
    }

    @Test
    void deleteShouldRemoveMenu() {
        when(menuService.removeById(1L)).thenReturn(true);

        var result = menuController.delete(1L);

        assertEquals("OK", result.getCode());
    }

    @Test
    void treeShouldReturnTreeStructure() {
        SysMenu child = new SysMenu();
        child.setId(2L);
        child.setParentId(1L);
        child.setTitle("用户管理");
        child.setMenuType("C");
        child.setSortOrder(1);
        child.setVisible(1);

        when(menuService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(sampleMenu, child));

        var result = menuController.tree();

        assertEquals("OK", result.getCode());
        assertEquals(1, result.getData().size());
        @SuppressWarnings("unchecked")
        var children = (List<?>) result.getData().get(0).get("children");
        assertEquals(1, children.size());
    }
}
