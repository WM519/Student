package com.leave.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级列表行
 */
@Data
public class ClassRowVO {
    private Long id;
    private String className;
    private String classCode;
    private Long collegeId;
    private String collegeName;
    private Long headTeacherId;
    private String headTeacherName;
    private Long counselorTeacherId;
    private String counselorTeacherName;
    private LocalDateTime createTime;
}
