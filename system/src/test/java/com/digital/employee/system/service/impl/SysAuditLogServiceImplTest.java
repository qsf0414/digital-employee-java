package com.digital.employee.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.digital.employee.system.domain.entity.SysAuditLog;
import com.digital.employee.system.mapper.SysAuditLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysAuditLogServiceImplTest {

    @Mock
    private SysAuditLogMapper auditLogMapper;

    @InjectMocks
    private SysAuditLogServiceImpl auditLogService;

    {
        try {
            setBaseMapper(auditLogService, auditLogMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void recordLoginSuccessShouldSaveLog() {
        when(auditLogMapper.insert(any(SysAuditLog.class))).thenReturn(1);

        auditLogService.recordLogin(1L, "admin", "127.0.0.1", true, null);

        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(auditLogMapper).insert(captor.capture());

        SysAuditLog saved = captor.getValue();
        assertEquals(1L, saved.getUserId());
        assertEquals("admin", saved.getUsername());
        assertEquals("127.0.0.1", saved.getIp());
        assertEquals("auth", saved.getModule());
        assertEquals("login", saved.getAction());
        assertEquals(1, saved.getStatus());
        assertNull(saved.getErrorMsg());
    }

    @Test
    void recordLoginFailureShouldSaveWithError() {
        when(auditLogMapper.insert(any(SysAuditLog.class))).thenReturn(1);

        auditLogService.recordLogin(null, "unknown", "192.168.1.1", false, "用户不存在");

        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(auditLogMapper).insert(captor.capture());

        SysAuditLog saved = captor.getValue();
        assertNull(saved.getUserId());
        assertEquals(0, saved.getStatus());
        assertEquals("用户不存在", saved.getErrorMsg());
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
