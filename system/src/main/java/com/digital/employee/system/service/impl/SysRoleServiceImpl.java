package com.digital.employee.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.digital.employee.common.redis.RedisConstants;
import com.digital.employee.system.domain.entity.SysRole;
import com.digital.employee.system.domain.entity.SysUser;
import com.digital.employee.system.mapper.SysRoleMapper;
import com.digital.employee.system.mapper.SysUserMapper;
import com.digital.employee.system.service.ISysRoleService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

    private final StringRedisTemplate redisTemplate;
    private final SysUserMapper userMapper;

    public SysRoleServiceImpl(StringRedisTemplate redisTemplate, SysUserMapper userMapper) {
        this.redisTemplate = redisTemplate;
        this.userMapper = userMapper;
    }

    @Override
    public SysRole getRoleByUserId(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return baseMapper.selectById(user.getRoleId());
    }

    @Override
    public void incrementRoleVersion(String roleKey) {
        redisTemplate.opsForValue().increment(RedisConstants.ROLE_VERSION_PREFIX + roleKey);
    }

    @Override
    public void incrementRoleVersionAfterCommit(String roleKey) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    redisTemplate.opsForValue().increment(RedisConstants.ROLE_VERSION_PREFIX + roleKey);
                }
            });
        } else {
            redisTemplate.opsForValue().increment(RedisConstants.ROLE_VERSION_PREFIX + roleKey);
        }
    }
}
