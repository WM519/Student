package com.leave.vo;

import lombok.Data;

import java.util.List;

/**
 * 首页仪表盘汇总
 */
@Data
public class DashboardVO {
    private long total;
    private long pending;
    private long approved;
    private long rejected;
    private long cancelled;
    /** 按状态统计 */
    private List<NameValueVO> statusItems;
    /** 按请假类型统计 */
    private List<NameValueVO> typeItems;
    /** 按学院统计（管理员） */
    private List<NameValueVO> collegeItems;
    /** 教师：三个环节的待办数（非教师为空） */
    private long headPending;
    private long counselorPending;
    private long leaderPending;
}
