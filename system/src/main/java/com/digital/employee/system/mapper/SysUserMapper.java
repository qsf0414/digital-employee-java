package com.digital.employee.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.digital.employee.system.domain.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
