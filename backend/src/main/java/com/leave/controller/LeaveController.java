package com.leave.controller;

import com.leave.common.PageResult;
import com.leave.common.Result;
import com.leave.common.constant.RoleCodes;
import com.leave.dto.ApproveRequest;
import com.leave.dto.LeaveApplyRequest;
import com.leave.dto.PageQuery;
import com.leave.security.SecurityUtil;
import com.leave.service.LeaveService;
import com.leave.vo.LeaveDetailVO;
import com.leave.vo.LeaveRowVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 请假业务接口：学生申请/销假、教师审批、管理员总览
 */
@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping("/apply")
    public Result<LeaveRowVO> apply(@Valid @RequestBody LeaveApplyRequest request) {
        return Result.ok(leaveService.apply(request));
    }

    @GetMapping("/my")
    public Result<PageResult<LeaveRowVO>> my(PageQuery query,
                                             @RequestParam(required = false) String status) {
        return Result.ok(leaveService.myPage(query, status));
    }

    @GetMapping("/all")
    public Result<PageResult<LeaveRowVO>> all(PageQuery query,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) Long collegeId,
                                              @RequestParam(required = false) String leaveType) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        return Result.ok(leaveService.allPage(query, status, collegeId, leaveType));
    }

    @GetMapping("/approval")
    public Result<PageResult<LeaveRowVO>> approval(PageQuery query,
                                                   @RequestParam Integer level,
                                                   @RequestParam(required = false, defaultValue = "false") Boolean done) {
        return Result.ok(leaveService.approvalPage(level, done, query));
    }

    @GetMapping("/{id}/detail")
    public Result<LeaveDetailVO> detail(@PathVariable Long id) {
        return Result.ok(leaveService.detail(id));
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody ApproveRequest request) {
        leaveService.approve(id, request);
        return Result.ok();
    }

    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody ApproveRequest request) {
        leaveService.reject(id, request);
        return Result.ok();
    }

    @PostMapping("/{id}/withdraw")
    public Result<Void> withdraw(@PathVariable Long id) {
        leaveService.withdraw(id);
        return Result.ok();
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        leaveService.cancelLeave(id);
        return Result.ok();
    }
}
