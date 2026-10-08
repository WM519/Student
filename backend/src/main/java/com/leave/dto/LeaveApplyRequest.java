package com.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 学生发起请假请求
 */
@Data
public class LeaveApplyRequest {
    @NotBlank(message = "请假类型不能为空")
    private String leaveType;
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;
    @NotBlank(message = "请假事由不能为空")
    private String reason;
}
