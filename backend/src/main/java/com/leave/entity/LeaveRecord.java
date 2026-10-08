package com.leave.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 请假申请
 */
@Data
@TableName("t_leave")
public class LeaveRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 请假单号 */
    private String leaveNo;
    private Long studentId;
    /** 请假类型：事假/病假/其他 */
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    /** 请假天数（自然日） */
    private Integer days;
    private String reason;
    private String status;
    /** 当前待审批环节 1 班主任 2 辅导员 3 院领导 */
    private Integer currentLevel;
    private LocalDateTime applyTime;
    /** 实际销假时间 */
    private LocalDateTime actualReturnTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
