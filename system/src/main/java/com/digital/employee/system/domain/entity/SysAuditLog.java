package com.digital.employee.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sys_audit_log")
public class SysAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String ip;

    private String module;

    private String action;

    private String targetId;

    private String method;

    private String url;

    private Integer durationMs;

    private Integer status;

    private String errorMsg;

    private OffsetDateTime createdAt;
}
