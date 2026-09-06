package com.digital.employee.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.digital.employee.system.domain.entity.SysAuditLog;
import com.digital.employee.system.mapper.SysAuditLogMapper;
import com.digital.employee.system.service.ISysAuditLogService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class SysAuditLogServiceImpl extends ServiceImpl<SysAuditLogMapper, SysAuditLog> implements ISysAuditLogService {

    @Override
    public void recordLogin(Long userId, String username, String ip, boolean success, String errorMsg) {
        SysAuditLog log = new SysAuditLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setIp(ip);
        log.setModule("auth");
        log.setAction("login");
        log.setMethod("POST");
        log.setUrl("/api/v1/auth/login");
        log.setStatus(success ? 1 : 0);
        log.setErrorMsg(errorMsg);
        log.setCreatedAt(OffsetDateTime.now());
        save(log);
    }
}
