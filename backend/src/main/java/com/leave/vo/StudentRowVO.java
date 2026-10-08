package com.leave.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生列表行
 */
@Data
public class StudentRowVO {
    private Long id;
    private String studentNo;
    private String name;
    private String gender;
    private String phone;
    private String email;
    private Long classId;
    private String className;
    private String collegeName;
    private Integer enrollYear;
    private String username;
    private Integer status;
    private LocalDateTime createTime;
}
