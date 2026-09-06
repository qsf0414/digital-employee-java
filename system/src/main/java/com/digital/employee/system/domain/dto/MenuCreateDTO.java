package com.digital.employee.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "创建菜单请求")
public class MenuCreateDTO {

    @Schema(description = "父菜单ID，0表示顶级")
    private Long parentId = 0L;

    @NotBlank(message = "菜单标题不能为空")
    @Schema(description = "菜单标题")
    private String title;

    @NotBlank(message = "菜单类型不能为空")
    @Pattern(regexp = "^[MCF]$", message = "菜单类型必须为 M(目录)、C(菜单)、F(按钮)")
    @Schema(description = "菜单类型：M-目录 C-菜单 F-按钮")
    private String menuType;

    @Schema(description = "路由路径")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "权限标识")
    private String perms;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Integer sortOrder = 0;

    @Schema(description = "是否可见 0-隐藏 1-显示")
    private Integer visible = 1;
}
