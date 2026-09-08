package com.digital.employee;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.digital.employee.common.exception.GlobalExceptionHandler;
import com.digital.employee.system.controller.SysRoleController;
import com.digital.employee.system.domain.entity.SysRole;
import com.digital.employee.system.domain.entity.SysRoleMenu;
import com.digital.employee.system.mapper.SysRoleMenuMapper;
import com.digital.employee.system.service.ISysRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SysRoleControllerIntegrationTest {

    @Mock
    private ISysRoleService roleService;

    @Mock
    private SysRoleMenuMapper roleMenuMapper;

    private SysRoleController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new SysRoleController(roleService, roleMenuMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
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
    void pageShouldReturnPagedRoles() throws Exception {
        Page<SysRole> page = new Page<>(1, 10);
        page.setRecords(List.of(
                buildRole(1L, "super_admin", "超级管理员", 1),
                buildRole(2L, "admin", "管理员", 1)
        ));
        page.setTotal(2);

        when(roleService.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(get("/api/v1/system/role/page")
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"))
                    .andExpect(jsonPath("$.data.records.length()").value(2))
                    .andExpect(jsonPath("$.data.records[0].roleKey").value("super_admin"))
                    .andExpect(jsonPath("$.data.records[1].roleKey").value("admin"));
        }
    }

    @Test
    void getByIdShouldReturnRole() throws Exception {
        SysRole role = buildRole(1L, "super_admin", "超级管理员", 1);
        when(roleService.getById(1L)).thenReturn(role);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(get("/api/v1/system/role/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"))
                    .andExpect(jsonPath("$.data.roleKey").value("super_admin"))
                    .andExpect(jsonPath("$.data.roleName").value("超级管理员"));
        }
    }

    @Test
    void getByIdShouldReturn404WhenRoleNotFound() throws Exception {
        when(roleService.getById(999L)).thenReturn(null);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(get("/api/v1/system/role/999"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ROLE_NOT_FOUND"));
        }
    }

    @Test
    void createShouldSucceed() throws Exception {
        when(roleService.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(post("/api/v1/system/role")
                            .contentType("application/json")
                            .content("{\"roleKey\":\"editor\",\"roleName\":\"编辑\",\"menuIds\":[1,2]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"));

            verify(roleService).save(any(SysRole.class));
            verify(roleMenuMapper, times(2)).insert(any(SysRoleMenu.class));
            verify(roleService).incrementRoleVersionAfterCommit("editor");
        }
    }

    @Test
    void createShouldFailWhenRoleKeyExists() throws Exception {
        SysRole existing = buildRole(1L, "admin", "管理员", 1);
        when(roleService.getOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(post("/api/v1/system/role")
                            .contentType("application/json")
                            .content("{\"roleKey\":\"admin\",\"roleName\":\"管理员\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ROLE_EXISTS"));
        }
    }

    @Test
    void updateShouldSucceed() throws Exception {
        SysRole role = buildRole(1L, "admin", "管理员", 1);
        when(roleService.getById(1L)).thenReturn(role);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(put("/api/v1/system/role")
                            .contentType("application/json")
                            .content("{\"id\":1,\"roleName\":\"新管理员\",\"menuIds\":[1,2,3]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"));

            verify(roleService).updateById(argThat(r -> "新管理员".equals(r.getRoleName())));
            verify(roleMenuMapper).deleteByRoleId(1L);
            verify(roleMenuMapper, times(3)).insert(any(SysRoleMenu.class));
            verify(roleService).incrementRoleVersionAfterCommit("admin");
        }
    }

    @Test
    void updateShouldReturn404WhenRoleNotFound() throws Exception {
        when(roleService.getById(999L)).thenReturn(null);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(put("/api/v1/system/role")
                            .contentType("application/json")
                            .content("{\"id\":999,\"roleName\":\"不存在\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ROLE_NOT_FOUND"));
        }
    }

    @Test
    void deleteShouldSucceed() throws Exception {
        SysRole role = buildRole(1L, "admin", "管理员", 1);
        when(roleService.getById(1L)).thenReturn(role);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(delete("/api/v1/system/role/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"));

            verify(roleService).removeById(1L);
            verify(roleService).incrementRoleVersionAfterCommit("admin");
        }
    }

    @Test
    void deleteShouldReturn404WhenRoleNotFound() throws Exception {
        when(roleService.getById(999L)).thenReturn(null);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(delete("/api/v1/system/role/999"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ROLE_NOT_FOUND"));
        }
    }
}
