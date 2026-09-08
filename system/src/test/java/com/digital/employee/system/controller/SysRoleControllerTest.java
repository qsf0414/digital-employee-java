package com.digital.employee.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.digital.employee.common.exception.BusinessException;
import com.digital.employee.system.domain.dto.RoleCreateDTO;
import com.digital.employee.system.domain.dto.RoleUpdateDTO;
import com.digital.employee.system.domain.entity.SysRole;
import com.digital.employee.system.mapper.SysRoleMenuMapper;
import com.digital.employee.system.service.ISysRoleService;
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
class SysRoleControllerTest {

    @Mock
    private ISysRoleService roleService;

    @Mock
    private SysRoleMenuMapper roleMenuMapper;

    @InjectMocks
    private SysRoleController roleController;

    private SysRole sampleRole;

    @BeforeEach
    void setUp() {
        sampleRole = new SysRole();
        sampleRole.setId(1L);
        sampleRole.setRoleKey("admin");
        sampleRole.setRoleName("管理员");
        sampleRole.setStatus(1);
    }

    @Test
    void pageShouldReturnRoles() {
        Page<SysRole> page = new Page<>(1, 10);
        page.setRecords(List.of(sampleRole));
        when(roleService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        var result = roleController.page(1, 10);

        assertEquals("OK", result.getCode());
    }

    @Test
    void getByIdShouldReturnRole() {
        when(roleService.getById(1L)).thenReturn(sampleRole);

        var result = roleController.getById(1L);

        assertEquals("OK", result.getCode());
        assertEquals("admin", result.getData().getRoleKey());
    }

    @Test
    void getByIdShouldThrowWhenNotFound() {
        when(roleService.getById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> roleController.getById(999L));
    }

    @Test
    void createShouldSaveRole() {
        when(roleService.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(roleService.save(any())).thenReturn(true);

        RoleCreateDTO dto = new RoleCreateDTO();
        dto.setRoleKey("editor");
        dto.setRoleName("编辑");
        dto.setMenuIds(List.of(1L, 2L));

        var result = roleController.create(dto);

        assertEquals("OK", result.getCode());
        verify(roleService, times(1)).incrementRoleVersionAfterCommit("editor");
    }

    @Test
    void createShouldThrowWhenKeyExists() {
        when(roleService.getOne(any(LambdaQueryWrapper.class))).thenReturn(sampleRole);

        RoleCreateDTO dto = new RoleCreateDTO();
        dto.setRoleKey("admin");
        dto.setRoleName("管理员");

        var ex = assertThrows(BusinessException.class, () -> roleController.create(dto));
        assertEquals("ROLE_EXISTS", ex.getCode());
    }

    @Test
    void updateShouldModifyRole() {
        when(roleService.getById(1L)).thenReturn(sampleRole);
        when(roleService.updateById(any())).thenReturn(true);

        RoleUpdateDTO dto = new RoleUpdateDTO();
        dto.setId(1L);
        dto.setRoleName("新管理员");
        dto.setMenuIds(List.of(1L));

        var result = roleController.update(dto);

        assertEquals("OK", result.getCode());
        verify(roleMenuMapper).deleteByRoleId(1L);
        verify(roleService).incrementRoleVersionAfterCommit("admin");
    }

    @Test
    void deleteShouldRemoveRole() {
        when(roleService.getById(1L)).thenReturn(sampleRole);
        when(roleService.removeById(1L)).thenReturn(true);

        var result = roleController.delete(1L);

        assertEquals("OK", result.getCode());
        verify(roleService).incrementRoleVersionAfterCommit("admin");
    }

    @Test
    void deleteShouldThrowWhenNotFound() {
        when(roleService.getById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> roleController.delete(999L));
    }
}
