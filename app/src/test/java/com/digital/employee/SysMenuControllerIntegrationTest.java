package com.digital.employee;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.digital.employee.common.exception.GlobalExceptionHandler;
import com.digital.employee.system.controller.SysMenuController;
import com.digital.employee.system.domain.entity.SysMenu;
import com.digital.employee.system.service.ISysMenuService;
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
class SysMenuControllerIntegrationTest {

    @Mock
    private ISysMenuService menuService;

    private SysMenuController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new SysMenuController(menuService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private SysMenu buildMenu(long id, Long parentId, String title, String menuType,
                              String path, String component, String perms, String icon,
                              int sortOrder, int visible) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setTitle(title);
        menu.setMenuType(menuType);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setPerms(perms);
        menu.setIcon(icon);
        menu.setSortOrder(sortOrder);
        menu.setVisible(visible);
        return menu;
    }

    @Test
    void treeShouldReturnTreeStructure() throws Exception {
        SysMenu parent = buildMenu(1L, 0L, "系统管理", "M", "/system", null, null, "setting", 1, 1);
        SysMenu child = buildMenu(2L, 1L, "用户管理", "C", "/system/user", "system/user/UserManage", "admin:user:readonly", "user", 1, 1);

        when(menuService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(parent, child));

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(get("/api/v1/system/menu/tree"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].title").value("系统管理"))
                    .andExpect(jsonPath("$.data[0].children.length()").value(1))
                    .andExpect(jsonPath("$.data[0].children[0].title").value("用户管理"));
        }
    }

    @Test
    void listShouldReturnFlatList() throws Exception {
        SysMenu menu1 = buildMenu(1L, 0L, "系统管理", "M", "/system", null, null, "setting", 1, 1);
        SysMenu menu2 = buildMenu(2L, 1L, "用户管理", "C", "/system/user", "system/user/UserManage", "admin:user:readonly", "user", 1, 1);

        when(menuService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(menu1, menu2));

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(get("/api/v1/system/menu/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].title").value("系统管理"))
                    .andExpect(jsonPath("$.data[1].title").value("用户管理"));
        }
    }

    @Test
    void getByIdShouldReturnMenu() throws Exception {
        SysMenu menu = buildMenu(1L, 0L, "系统管理", "M", "/system", null, null, "setting", 1, 1);
        when(menuService.getById(1L)).thenReturn(menu);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(get("/api/v1/system/menu/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"))
                    .andExpect(jsonPath("$.data.title").value("系统管理"))
                    .andExpect(jsonPath("$.data.menuType").value("M"));
        }
    }

    @Test
    void getByIdShouldReturn404WhenMenuNotFound() throws Exception {
        when(menuService.getById(999L)).thenReturn(null);

        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(get("/api/v1/system/menu/999"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MENU_NOT_FOUND"));
        }
    }

    @Test
    void createShouldSucceed() throws Exception {
        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(post("/api/v1/system/menu")
                            .contentType("application/json")
                            .content("{\"parentId\":0,\"title\":\"日志管理\",\"menuType\":\"M\",\"path\":\"/log\",\"icon\":\"log\",\"sortOrder\":2,\"visible\":1}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"));

            verify(menuService).save(any(SysMenu.class));
        }
    }

    @Test
    void updateShouldSucceed() throws Exception {
        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(put("/api/v1/system/menu")
                            .contentType("application/json")
                            .content("{\"id\":1,\"title\":\"系统设置\",\"menuType\":\"M\",\"path\":\"/settings\",\"icon\":\"setting\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"));

            verify(menuService).updateById(argThat(m -> "系统设置".equals(m.getTitle())));
        }
    }

    @Test
    void updateShouldFailWhenNoId() throws Exception {
        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(put("/api/v1/system/menu")
                            .contentType("application/json")
                            .content("{\"title\":\"无ID菜单\",\"menuType\":\"M\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
        }
    }

    @Test
    void deleteShouldSucceed() throws Exception {
        try (var stpMock = mockStatic(StpUtil.class)) {
            mockMvc.perform(delete("/api/v1/system/menu/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OK"));

            verify(menuService).removeById(5L);
        }
    }
}
