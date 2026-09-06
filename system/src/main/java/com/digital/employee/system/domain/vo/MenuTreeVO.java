package com.digital.employee.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@Schema(description = "菜单树节点")
public class MenuTreeVO {

    @Schema(description = "菜单ID")
    private Long id;

    @Schema(description = "父菜单ID")
    private Long parentId;

    @Schema(description = "菜单标题")
    private String title;

    @Schema(description = "菜单类型：M-目录 C-菜单 F-按钮")
    private String menuType;

    @Schema(description = "路由路径")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "子菜单")
    @Builder.Default
    private List<MenuTreeVO> children = new ArrayList<>();
}
