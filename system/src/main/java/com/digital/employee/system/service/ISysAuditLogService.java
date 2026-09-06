package com.digital.employee.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.digital.employee.system.domain.entity.SysAuditLog;

public interface ISysAuditLogService extends IService<SysAuditLog> {

    void recordLogin(Long userId, String username, String ip, boolean success, String errorMsg);
}
