package com.leave.service;

import com.leave.common.constant.LeaveStatus;
import com.leave.mapper.LeaveMapper;
import com.leave.security.LoginPrincipal;
import com.leave.security.SecurityUtil;
import com.leave.vo.DashboardVO;
import com.leave.vo.NameValueVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 仪表盘统计服务（按角色返回不同维度的数据）
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final LeaveMapper leaveMapper;

    public DashboardVO summary() {
        LoginPrincipal principal = SecurityUtil.currentPrincipal();
        DashboardVO vo = new DashboardVO();

        if ("ADMIN".equals(principal.getUserType())) {
            fillByStudentId(vo, null);
            vo.setCollegeItems(leaveMapper.countGroupByCollege());
            vo.setTypeItems(leaveMapper.countGroupByType(null));
        } else if ("STUDENT".equals(principal.getUserType())) {
            fillByStudentId(vo, principal.getRefId());
            vo.setTypeItems(leaveMapper.countGroupByType(principal.getRefId()));
        } else if ("TEACHER".equals(principal.getUserType())) {
            Long teacherId = principal.getRefId();
            vo.setHeadPending(leaveMapper.countPendingAtLevel(1, teacherId));
            vo.setCounselorPending(leaveMapper.countPendingAtLevel(2, teacherId));
            vo.setLeaderPending(leaveMapper.countPendingAtLevel(3, teacherId));
            vo.setPending(vo.getHeadPending() + vo.getCounselorPending() + vo.getLeaderPending());
        }
        return vo;
    }

    private void fillByStudentId(DashboardVO vo, Long studentId) {
        List<NameValueVO> statusItems = leaveMapper.countGroupByStatus(studentId);
        vo.setStatusItems(statusItems);
        long total = 0;
        long pending = 0;
        for (NameValueVO item : statusItems) {
            long value = item.getValue() == null ? 0 : item.getValue();
            total += value;
            String name = item.getName();
            if (LeaveStatus.PENDING_HEAD.equals(name)
                    || LeaveStatus.PENDING_COUNSELOR.equals(name)
                    || LeaveStatus.PENDING_LEADER.equals(name)) {
                pending += value;
            } else if (LeaveStatus.APPROVED.equals(name)) {
                vo.setApproved(value);
            } else if (LeaveStatus.REJECTED.equals(name)) {
                vo.setRejected(value);
            } else if (LeaveStatus.CANCELLED.equals(name)) {
                vo.setCancelled(value);
            }
        }
        vo.setTotal(total);
        vo.setPending(pending);
    }
}
