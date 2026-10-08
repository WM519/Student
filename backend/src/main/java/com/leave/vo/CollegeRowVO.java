package com.leave.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学院列表行
 */
@Data
public class CollegeRowVO {
    private Long id;
    private String collegeName;
    private String collegeCode;
    private Long leaderTeacherId;
    private String leaderName;
    private String description;
    private LocalDateTime createTime;
}
