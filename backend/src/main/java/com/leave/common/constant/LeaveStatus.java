package com.leave.common.constant;

/**
 * 请假状态机
 */
public interface LeaveStatus {
    /** 待班主任审批 */
    String PENDING_HEAD = "PENDING_HEAD";
    /** 待辅导员审批 */
    String PENDING_COUNSELOR = "PENDING_COUNSELOR";
    /** 待院领导审批 */
    String PENDING_LEADER = "PENDING_LEADER";
    /** 已通过（准假） */
    String APPROVED = "APPROVED";
    /** 已驳回 */
    String REJECTED = "REJECTED";
    /** 已销假 */
    String CANCELLED = "CANCELLED";
    /** 已撤销（学生主动撤回） */
    String WITHDRAWN = "WITHDRAWN";

    /** 请假类型 */
    interface LeaveType {
        String PERSONAL = "事假";
        String SICK = "病假";
        String OTHER = "其他";
    }
}
