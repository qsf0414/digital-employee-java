package com.digital.employee.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.digital.employee.system.domain.entity.SysRole;

public interface ISysRoleService extends IService<SysRole> {

    SysRole getRoleByUserId(Long userId);

    void incrementRoleVersion(String roleKey);
}
