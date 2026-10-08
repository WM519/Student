package com.leave.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 请假审批流水
 */
@Data
@TableName("t_leave_audit")
public class LeaveAudit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long leaveId;
    /** 环节：0 提交 / 1 班主任 / 2 辅导员 / 3 院领导 / 4 销假 */
    private Integer level;
    /** 操作人（用户 ID） */
    private Long operatorUserId;
    /** 操作人姓名 */
    private String operatorName;
    /** 操作类型：SUBMIT/APPROVE/REJECT/WITHDRAW/CANCEL */
    private String action;
    private String comment;
    private LocalDateTime auditTime;
}
