package com.digital.employee.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.digital.employee.system.domain.entity.SysMenu;
import com.digital.employee.system.mapper.SysMenuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysMenuServiceImplTest {

    @Mock
    private SysMenuMapper menuMapper;

    @InjectMocks
    private SysMenuServiceImpl menuService;

    @BeforeEach
    void setUp() {
        try {
            setBaseMapper(menuService, menuMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void selectPermsByRoleIdShouldReturnPerms() {
        when(menuMapper.selectPermsByRoleId(1L)).thenReturn(List.of("admin:user:readonly", "admin:user:manage"));

        List<String> perms = menuService.selectPermsByRoleId(1L);

        assertEquals(2, perms.size());
        assertTrue(perms.contains("admin:user:readonly"));
    }

    @Test
    void selectPermsByRoleIdShouldReturnEmptyList() {
        when(menuMapper.selectPermsByRoleId(2L)).thenReturn(List.of());

        List<String> perms = menuService.selectPermsByRoleId(2L);

        assertTrue(perms.isEmpty());
    }

    @Test
    void selectMenusByRoleIdShouldReturnMenus() {
        SysMenu menu = new SysMenu();
        menu.setId(1L);
        menu.setTitle("系统管理");
        menu.setMenuType("M");
        when(menuMapper.selectMenusByRoleId(1L)).thenReturn(List.of(menu));

        List<SysMenu> menus = menuService.selectMenusByRoleId(1L);

        assertEquals(1, menus.size());
        assertEquals("系统管理", menus.get(0).getTitle());
    }

    @SuppressWarnings("unchecked")
    private void setBaseMapper(ServiceImpl<?, ?> service, Object mapper) throws Exception {
        Class<?> clazz = service.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField("baseMapper");
                field.setAccessible(true);
                field.set(service, mapper);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("baseMapper not found in class hierarchy");
    }
}
