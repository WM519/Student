package com.leave.vo;

import lombok.Data;

import java.util.List;

/**
 * 登录用户信息（前端 Pinia loginStore 中同步封装）
 */
@Data
public class LoginUserInfo {
    private Long userId;
    private String username;
    private String realName;
    /** ADMIN/TEACHER/STUDENT */
    private String userType;
    private List<RoleBrief> roles;
    /** 当前用户可见菜单树（用于动态路由） */
    private List<MenuNode> menus;

    @Data
    public static class RoleBrief {
        private String code;
        private String name;

        public RoleBrief(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
