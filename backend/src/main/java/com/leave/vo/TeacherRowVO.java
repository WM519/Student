package com.leave.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教师列表行
 */
@Data
public class TeacherRowVO {
    private Long id;
    private String teacherNo;
    private String name;
    private String gender;
    private String phone;
    private String email;
    private Long collegeId;
    private String collegeName;
    private String username;
    private Integer status;
    private List<Long> roleIds;
    private List<String> roleCodes;
    private List<String> roleNames;
    private LocalDateTime createTime;
}
