package com.leave.vo;

import lombok.Data;

/**
 * 教师-角色关系（用于列表回显角色）
 */
@Data
public class TeacherRoleVO {
    private Long teacherId;
    private Long roleId;
    private String roleCode;
    private String roleName;
}
