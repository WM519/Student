package com.leave.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批流水
 */
@Data
public class AuditVO {
    private Long id;
    private Long leaveId;
    private Integer level;
    private Long operatorUserId;
    private String operatorName;
    private String action;
    private String comment;
    private LocalDateTime auditTime;
}
