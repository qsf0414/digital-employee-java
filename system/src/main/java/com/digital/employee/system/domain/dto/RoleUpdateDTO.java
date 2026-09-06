package com.digital.employee.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "更新角色请求")
public class RoleUpdateDTO {

    @NotNull(message = "角色ID不能为空")
    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "状态 0-禁用 1-启用")
    private Integer status;

    @Schema(description = "菜单ID列表")
    private List<Long> menuIds;
}
