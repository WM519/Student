package com.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 教师新增/修改请求
 */
@Data
public class TeacherSaveRequest {
    @NotBlank(message = "工号不能为空")
    private String teacherNo;
    @NotBlank(message = "姓名不能为空")
    private String name;
    private String gender;
    private String phone;
    private String email;
    @NotNull(message = "所属学院不能为空")
    private Long collegeId;
    /** 分配的角色 ID 列表（教师类角色） */
    private List<Long> roleIds;
}
