package com.digital.employee.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.digital.employee.common.redis.RedisConstants;
import com.digital.employee.system.domain.entity.SysRole;
import com.digital.employee.system.mapper.SysRoleMapper;
import com.digital.employee.system.service.ISysRoleService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

    private final StringRedisTemplate redisTemplate;

    public SysRoleServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public SysRole getRoleByUserId(Long userId) {
        return baseMapper.selectOne(
            new LambdaQueryWrapper<SysRole>()
                .inSql(SysRole::getId, "SELECT role_id FROM sys_user WHERE id = " + userId)
        );
    }

    @Override
    public void incrementRoleVersion(String roleKey) {
        redisTemplate.opsForValue().increment(RedisConstants.ROLE_VERSION_PREFIX + roleKey);
    }
}
