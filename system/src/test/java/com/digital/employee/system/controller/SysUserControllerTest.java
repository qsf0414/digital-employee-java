package com.digital.employee.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.digital.employee.common.exception.BusinessException;
import com.digital.employee.system.domain.dto.UserCreateDTO;
import com.digital.employee.system.domain.dto.UserUpdateDTO;
import com.digital.employee.system.domain.entity.SysUser;
import com.digital.employee.system.service.ISysUserService;
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
class SysUserControllerTest {

    @Mock
    private ISysUserService userService;

    @InjectMocks
    private SysUserController userController;

    private SysUser sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new SysUser();
        sampleUser.setId(1L);
        sampleUser.setUsername("admin");
        sampleUser.setPasswordHash("$2a$hashed");
        sampleUser.setNickname("管理员");
        sampleUser.setRoleId(1L);
        sampleUser.setStatus(1);
    }

    @Test
    void pageShouldReturnUsers() {
        Page<SysUser> page = new Page<>(1, 10);
        page.setRecords(List.of(sampleUser));
        page.setTotal(1);
        when(userService.pageUsers(1, 10, null)).thenReturn(page);

        var result = userController.page(1, 10, null);

        assertEquals("OK", result.getCode());
        assertEquals(1, result.getData().getRecords().size());
    }

    @Test
    void getByIdShouldReturnUser() {
        when(userService.getById(1L)).thenReturn(sampleUser);

        var result = userController.getById(1L);

        assertEquals("OK", result.getCode());
        assertNull(result.getData().getPasswordHash());
    }

    @Test
    void getByIdShouldThrowWhenNotFound() {
        when(userService.getById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> userController.getById(999L));
    }

    @Test
    void createShouldSaveUser() {
        when(userService.getByUsername("newuser")).thenReturn(null);
        doNothing().when(userService).createUser(any(SysUser.class), anyString());

        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("newuser");
        dto.setPassword("pass");
        dto.setRoleId(1L);

        var result = userController.create(dto);

        assertEquals("OK", result.getCode());
    }

    @Test
    void createShouldThrowWhenUsernameExists() {
        when(userService.getByUsername("admin")).thenReturn(sampleUser);

        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("admin");
        dto.setPassword("pass");
        dto.setRoleId(1L);

        var ex = assertThrows(BusinessException.class, () -> userController.create(dto));
        assertEquals("USER_EXISTS", ex.getCode());
    }

    @Test
    void updateShouldModifyUser() {
        when(userService.getById(1L)).thenReturn(sampleUser);
        when(userService.updateById(any())).thenReturn(true);

        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setId(1L);
        dto.setNickname("新昵称");

        var result = userController.update(dto);

        assertEquals("OK", result.getCode());
        assertEquals("新昵称", sampleUser.getNickname());
    }

    @Test
    void updateShouldThrowWhenNotFound() {
        when(userService.getById(999L)).thenReturn(null);

        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setId(999L);

        assertThrows(BusinessException.class, () -> userController.update(dto));
    }

    @Test
    void deleteShouldRemoveUser() {
        when(userService.getById(1L)).thenReturn(sampleUser);
        when(userService.removeById(1L)).thenReturn(true);

        var result = userController.delete(1L);

        assertEquals("OK", result.getCode());
    }

    @Test
    void deleteShouldThrowWhenNotFound() {
        when(userService.getById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> userController.delete(999L));
    }
}
