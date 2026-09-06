package com.digital.employee.system.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "登录请求")
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", example = "admin")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码")
    private String password;

    @NotBlank(message = "验证码Key不能为空")
    @Schema(description = "验证码Key")
    private String captchaKey;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "验证码")
    private String captchaCode;
}
