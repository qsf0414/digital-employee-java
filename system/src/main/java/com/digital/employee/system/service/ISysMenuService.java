package com.digital.employee.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.digital.employee.system.domain.entity.SysMenu;

import java.util.List;

public interface ISysMenuService extends IService<SysMenu> {

    List<String> selectPermsByRoleId(Long roleId);

    List<SysMenu> selectMenusByRoleId(Long roleId);
}
