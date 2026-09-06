package com.digital.employee.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.digital.employee.system.domain.entity.SysUser;

public interface ISysUserService extends IService<SysUser> {

    SysUser getByUsername(String username);

    void createUser(SysUser user, String rawPassword);

    Page<SysUser> pageUsers(int page, int pageSize, String keyword);
}
