package com.leave.vo;

import lombok.Data;

import java.util.List;

/**
 * 请假详情 = 基本信息 + 审批流水
 */
@Data
public class LeaveDetailVO {
    private LeaveRowVO leave;
    private List<AuditVO> audits;
}
