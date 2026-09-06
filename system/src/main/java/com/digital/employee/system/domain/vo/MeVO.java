package com.digital.employee.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "当前用户信息")
public class MeVO {

    @Schema(description = "用户基本信息")
    private UserInfo user;

    @Schema(description = "角色标识列表")
    private List<String> roles;

    @Schema(description = "权限码列表（扁平）")
    private List<String> permissions;

    @Schema(description = "菜单树（仅 M 和 C）")
    private List<MenuTreeVO> menus;

    @Data
    @Builder
    public static class UserInfo {
        @Schema(description = "用户ID")
        private Long userId;
        @Schema(description = "用户名")
        private String username;
        @Schema(description = "昵称")
        private String nickname;
    }
}
