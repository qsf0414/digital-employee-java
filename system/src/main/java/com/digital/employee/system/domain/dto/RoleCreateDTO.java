package com.digital.employee.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "创建角色请求")
public class RoleCreateDTO {

    @NotBlank(message = "角色标识不能为空")
    @Schema(description = "角色标识（小写字母、数字、下划线）")
    private String roleKey;

    @NotBlank(message = "角色名称不能为空")
    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "菜单ID列表")
    private List<Long> menuIds;
}
