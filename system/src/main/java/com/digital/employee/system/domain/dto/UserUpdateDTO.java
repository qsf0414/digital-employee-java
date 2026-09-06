package com.digital.employee.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "更新用户请求")
public class UserUpdateDTO {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "状态 0-禁用 1-启用")
    private Integer status;
}
