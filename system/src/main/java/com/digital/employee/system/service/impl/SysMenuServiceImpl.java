package com.digital.employee.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.digital.employee.system.domain.entity.SysMenu;
import com.digital.employee.system.mapper.SysMenuMapper;
import com.digital.employee.system.service.ISysMenuService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {

    @Override
    public List<String> selectPermsByRoleId(Long roleId) {
        return baseMapper.selectPermsByRoleId(roleId);
    }

    @Override
    public List<SysMenu> selectMenusByRoleId(Long roleId) {
        return baseMapper.selectMenusByRoleId(roleId);
    }
}
