package com.digital.employee;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.digital.employee.common.exception.GlobalExceptionHandler;
import com.digital.employee.system.controller.SysUserController;
import com.digital.employee.system.domain.entity.SysUser;
import com.digital.employee.system.service.ISysUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SysUserControllerIntegrationTest {

    @Mock
    private ISysUserService userService;

    private SysUserController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new SysUserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private SysUser buildUser(long id, String username, String nickname, int status, long roleId) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("$2a$10$hashed");
        user.setNickname(nickname);
        user.setStatus(status);
        user.setRoleId(roleId);
        user.setMustChangePassword(false);
        return user;
    }

    @Test
    void pageShouldReturnPagedUsers() throws Exception {
        Page<SysUser> page = new Page<>(1, 10);
        page.setRecords(List.of(
                buildUser(1L, "admin", "管理员", 1, 1L),
                buildUser(2L, "user", "普通用户", 1, 2L)
        ));
        page.setTotal(2);

        when(userService.pageUsers(1, 10, null)).thenReturn(page);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(get("/api/v1/system/user/page")
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"))
                    .andExpect(jsonPath("$.data.records.length()").value(2))
                    .andExpect(jsonPath("$.data.records[0].username").value("admin"))
                    .andExpect(jsonPath("$.data.records[1].username").value("user"));
        }
    }

    @Test
    void getByIdShouldReturnUserWithNullPassword() throws Exception {
        SysUser user = buildUser(1L, "admin", "管理员", 1, 1L);
        when(userService.getById(1L)).thenReturn(user);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(get("/api/v1/system/user/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.username").value("admin"))
                    .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        }
    }

    @Test
    void getByIdShouldReturn404WhenUserNotFound() throws Exception {
        when(userService.getById(999L)).thenReturn(null);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(get("/api/v1/system/user/999"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        }
    }

    @Test
    void createShouldSucceed() throws Exception {
        when(userService.getByUsername("newuser")).thenReturn(null);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(post("/api/v1/system/user")
                            .contentType("application/json")
                            .content("{\"username\":\"newuser\",\"password\":\"Test@12345\",\"nickname\":\"新用户\",\"phone\":\"13800138000\",\"roleId\":1}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"));

            verify(userService).createUser(any(SysUser.class), eq("Test@12345"));
        }
    }

    @Test
    void createShouldFailWhenUsernameExists() throws Exception {
        SysUser existing = buildUser(1L, "admin", "管理员", 1, 1L);
        when(userService.getByUsername("admin")).thenReturn(existing);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(post("/api/v1/system/user")
                            .contentType("application/json")
                            .content("{\"username\":\"admin\",\"password\":\"Test@12345\",\"nickname\":\"管理员\",\"roleId\":1}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("USER_EXISTS"));
        }
    }

    @Test
    void updateShouldSucceed() throws Exception {
        SysUser user = buildUser(1L, "admin", "管理员", 1, 1L);
        when(userService.getById(1L)).thenReturn(user);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(put("/api/v1/system/user")
                            .contentType("application/json")
                            .content("{\"id\":1,\"nickname\":\"超级管理员\",\"phone\":\"13900139000\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"));

            verify(userService).updateById(argThat(u ->
                    "超级管理员".equals(u.getNickname()) && "13900139000".equals(u.getPhone())));
        }
    }

    @Test
    void updateShouldFailWhenUserNotFound() throws Exception {
        when(userService.getById(999L)).thenReturn(null);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(put("/api/v1/system/user")
                            .contentType("application/json")
                            .content("{\"id\":999,\"nickname\":\"不存在\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        }
    }

    @Test
    void deleteShouldSucceed() throws Exception {
        SysUser user = buildUser(1L, "admin", "管理员", 1, 1L);
        when(userService.getById(1L)).thenReturn(user);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(delete("/api/v1/system/user/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"));

            verify(userService).removeById(1L);
        }
    }

    @Test
    void deleteShouldFailWhenUserNotFound() throws Exception {
        when(userService.getById(999L)).thenReturn(null);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(delete("/api/v1/system/user/999"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        }
    }

    @Test
    void resetPasswordShouldSucceed() throws Exception {
        SysUser user = buildUser(1L, "admin", "管理员", 1, 1L);
        user.setMustChangePassword(false);
        when(userService.getById(1L)).thenReturn(user);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(put("/api/v1/system/user/1/reset-password"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"));

            verify(userService).updateById(argThat(u -> Boolean.TRUE.equals(u.getMustChangePassword())));
        }
    }
}
