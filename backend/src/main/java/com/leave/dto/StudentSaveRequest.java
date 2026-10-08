package com.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 学生新增/修改请求
 */
@Data
public class StudentSaveRequest {
    @NotBlank(message = "学号不能为空")
    private String studentNo;
    @NotBlank(message = "姓名不能为空")
    private String name;
    private String gender;
    private String phone;
    private String email;
    @NotNull(message = "所属班级不能为空")
    private Long classId;
    private Integer enrollYear;
}
