package com.digital.employee.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.digital.employee.system.domain.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") Long roleId);
}
