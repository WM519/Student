package com.leave.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 请假列表行（含学生/班级/学院信息）
 */
@Data
public class LeaveRowVO {
    private Long id;
    private String leaveNo;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String className;
    private String collegeName;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer days;
    private String reason;
    private String status;
    private Integer currentLevel;
    private LocalDateTime applyTime;
    private LocalDateTime actualReturnTime;
}
