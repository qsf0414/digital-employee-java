package com.digital.employee.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String title;

    private String menuType;

    private String path;

    private String component;

    private String perms;

    private String icon;

    private Integer sortOrder;

    private Integer visible;

    private OffsetDateTime createdAt;
}
