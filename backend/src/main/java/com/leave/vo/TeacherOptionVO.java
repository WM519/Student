package com.leave.vo;

import lombok.Data;

/**
 * 教师下拉选项
 */
@Data
public class TeacherOptionVO {
    private Long id;
    private String name;
    private String teacherNo;
    private Long collegeId;
}
