package com.leave.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色分配菜单请求
 */
@Data
public class RoleMenuRequest {
    @NotNull(message = "角色 ID 不能为空")
    private Long roleId;
    private List<Long> menuIds;
}
