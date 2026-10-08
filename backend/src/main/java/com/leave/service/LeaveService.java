package com.leave.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leave.common.BusinessException;
import com.leave.common.PageResult;
import com.leave.common.constant.LeaveStatus;
import com.leave.common.constant.RoleCodes;
import com.leave.dto.ApproveRequest;
import com.leave.dto.LeaveApplyRequest;
import com.leave.dto.PageQuery;
import com.leave.entity.Clazz;
import com.leave.entity.College;
import com.leave.entity.LeaveAudit;
import com.leave.entity.LeaveRecord;
import com.leave.entity.Student;
import com.leave.entity.Teacher;
import com.leave.mapper.ClassMapper;
import com.leave.mapper.CollegeMapper;
import com.leave.mapper.LeaveAuditMapper;
import com.leave.mapper.LeaveMapper;
import com.leave.mapper.StudentMapper;
import com.leave.mapper.TeacherMapper;
import com.leave.security.LoginPrincipal;
import com.leave.security.SecurityUtil;
import com.leave.vo.AuditVO;
import com.leave.vo.LeaveDetailVO;
import com.leave.vo.LeaveRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 请假业务服务：申请、审批链、撤销、销假
 */
@Service
@RequiredArgsConstructor
public class LeaveService {

    private static final Set<String> VALID_TYPES =
            Set.of(LeaveStatus.LeaveType.PERSONAL, LeaveStatus.LeaveType.SICK, LeaveStatus.LeaveType.OTHER);

    private final LeaveMapper leaveMapper;
    private final LeaveAuditMapper auditMapper;
    private final StudentMapper studentMapper;
    private final ClassMapper classMapper;
    private final CollegeMapper collegeMapper;
    private final TeacherMapper teacherMapper;

    /** 按请假天数计算审批链：1 班主任，2 辅导员，3 院领导 */
    public static List<Integer> requiredLevels(int days) {
        if (days <= 3) {
            return List.of(1);
        }
        if (days <= 7) {
            return List.of(1, 2);
        }
        return List.of(1, 2, 3);
    }

    @Transactional
    public LeaveRowVO apply(LeaveApplyRequest request) {
        LoginPrincipal principal = SecurityUtil.currentPrincipal();
        SecurityUtil.requireRole(RoleCodes.STUDENT);
        if (principal.getRefId() == null) {
            throw new BusinessException("学生资料不完整，请联系管理员");
        }
        Student student = studentMapper.selectById(principal.getRefId());
        if (student == null) {
            throw new BusinessException("学生资料不存在，请联系管理员");
        }
        if (!VALID_TYPES.contains(request.getLeaveType())) {
            throw new BusinessException("请选择合法的请假类型");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new BusinessException("请假起止日期不能为空");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("结束日期不能早于开始日期");
        }
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new BusinessException("请假开始日期不能早于今天");
        }
        if (!StringUtils.hasText(request.getReason())) {
            throw new BusinessException("请填写请假事由");
        }

        int days = (int) (ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1);
        checkOverlap(student.getId(), request.getStartDate(), request.getEndDate());

        LeaveRecord record = new LeaveRecord();
        record.setLeaveNo(generateLeaveNo());
        record.setStudentId(student.getId());
        record.setLeaveType(request.getLeaveType());
        record.setStartDate(request.getStartDate());
        record.setEndDate(request.getEndDate());
        record.setDays(days);
        record.setReason(request.getReason().trim());
        record.setStatus(LeaveStatus.PENDING_HEAD);
        record.setCurrentLevel(1);
        record.setApplyTime(LocalDateTime.now());
        leaveMapper.insert(record);
        addAudit(record.getId(), 0, principal.getUserId(), principal.getRealName(),
                "SUBMIT", "提交请假申请");
        return leaveMapper.selectLeaveRowById(record.getId());
    }

    public PageResult<LeaveRowVO> myPage(PageQuery query, String status) {
        LoginPrincipal principal = SecurityUtil.currentPrincipal();
        if (principal.getRefId() == null) {
            throw new BusinessException("学生资料不完整");
        }
        IPage<LeaveRowVO> page = leaveMapper.selectMyLeaves(
                new Page<>(query.getPageNum(), query.getPageSize()),
                principal.getRefId(), status);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public PageResult<LeaveRowVO> allPage(PageQuery query, String status, Long collegeId, String leaveType) {
        IPage<LeaveRowVO> page = leaveMapper.selectAllLeaves(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getKeyword(), status, collegeId, leaveType);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    /** level：1 班主任 / 2 辅导员 / 3 院领导；done=true 表示已办列表 */
    public PageResult<LeaveRowVO> approvalPage(Integer level, Boolean done, PageQuery query) {
        LoginPrincipal principal = SecurityUtil.currentPrincipal();
        requireTeacher(principal);
        requireLevelRole(level);
        Long teacherId = principal.getRefId();
        IPage<LeaveRowVO> page = leaveMapper.selectApprovalLeaves(
                new Page<>(query.getPageNum(), query.getPageSize()),
                level, teacherId, principal.getUserId(), query.getKeyword(),
                Boolean.TRUE.equals(done));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public LeaveDetailVO detail(Long id) {
        LeaveRowVO row = leaveMapper.selectLeaveRowById(id);
        if (row == null) {
            throw new BusinessException("请假记录不存在");
        }
        authorizeDetail(row);
        List<AuditVO> audits = auditMapper.selectAuditsByLeaveId(id);
        LeaveDetailVO vo = new LeaveDetailVO();
        vo.setLeave(row);
        vo.setAudits(audits);
        return vo;
    }

    @Transactional
    public void approve(Long id, ApproveRequest request) {
        LoginPrincipal principal = SecurityUtil.currentPrincipal();
        requireTeacher(principal);
        LeaveRecord record = loadPending(id);
        Long teacherId = principal.getRefId();
        checkScope(record, teacherId, record.getCurrentLevel(), principal);

        addAudit(record.getId(), record.getCurrentLevel(), principal.getUserId(),
                principal.getRealName(), "APPROVE",
                StringUtils.hasText(request.getComment()) ? request.getComment() : "同意");
        advance(record);
    }

    @Transactional
    public void reject(Long id, ApproveRequest request) {
        LoginPrincipal principal = SecurityUtil.currentPrincipal();
        requireTeacher(principal);
        LeaveRecord record = loadPending(id);
        checkScope(record, principal.getRefId(), record.getCurrentLevel(), principal);

        addAudit(record.getId(), record.getCurrentLevel(), principal.getUserId(),
                principal.getRealName(), "REJECT",
                StringUtils.hasText(request.getComment()) ? request.getComment() : "不同意");
        record.setStatus(LeaveStatus.REJECTED);
        record.setCurrentLevel(null);
        leaveMapper.updateById(record);
    }

    /** 学生撤销未处理的申请 */
    @Transactional
    public void withdraw(Long id) {
        LoginPrincipal principal = SecurityUtil.currentPrincipal();
        SecurityUtil.requireRole(RoleCodes.STUDENT);
        LeaveRecord record = leaveMapper.selectById(id);
        if (record == null || !record.getStudentId().equals(principal.getRefId())) {
            throw new BusinessException("请假记录不存在");
        }
        if (!LeaveStatus.PENDING_HEAD.equals(record.getStatus())) {
            throw new BusinessException("当前状态不允许撤销（可能已开始审批或已结束）");
        }
        addAudit(record.getId(), 0, principal.getUserId(), principal.getRealName(), "WITHDRAW", "学生主动撤销");
        record.setStatus(LeaveStatus.WITHDRAWN);
        record.setCurrentLevel(null);
        leaveMapper.updateById(record);
    }

    /** 学生销假：已通过且请假已经开始 */
    @Transactional
    public void cancelLeave(Long id) {
        LoginPrincipal principal = SecurityUtil.currentPrincipal();
        SecurityUtil.requireRole(RoleCodes.STUDENT);
        LeaveRecord record = leaveMapper.selectById(id);
        if (record == null || !record.getStudentId().equals(principal.getRefId())) {
            throw new BusinessException("请假记录不存在");
        }
        if (!LeaveStatus.APPROVED.equals(record.getStatus())) {
            throw new BusinessException("只有已通过的请假才能销假");
        }
        if (LocalDate.now().isBefore(record.getStartDate())) {
            throw new BusinessException("请假尚未开始，暂时不能销假");
        }
        addAudit(record.getId(), 4, principal.getUserId(), principal.getRealName(), "CANCEL", "学生销假");
        record.setStatus(LeaveStatus.CANCELLED);
        record.setActualReturnTime(LocalDateTime.now());
        leaveMapper.updateById(record);
    }

    // ---------------- 内部方法 ----------------

    private void checkOverlap(Long studentId, LocalDate start, LocalDate end) {
        List<LeaveRecord> active = leaveMapper.selectList(new LambdaQueryWrapper<LeaveRecord>()
                .eq(LeaveRecord::getStudentId, studentId)
                .in(LeaveRecord::getStatus, List.of(
                        LeaveStatus.PENDING_HEAD, LeaveStatus.PENDING_COUNSELOR,
                        LeaveStatus.PENDING_LEADER, LeaveStatus.APPROVED)));
        for (LeaveRecord r : active) {
            boolean overlap = !end.isBefore(r.getStartDate()) && !start.isAfter(r.getEndDate());
            if (overlap) {
                throw new BusinessException("您已有时间段重叠且未结束的请假（" + r.getLeaveNo() + "），请先处理");
            }
        }
    }

    /** 详情可见性：本人（学生）/ 管理员 / 与该生请假相关的教师 */
    private void authorizeDetail(LeaveRowVO row) {
        LoginPrincipal principal = SecurityUtil.currentPrincipal();
        if ("ADMIN".equals(principal.getUserType())) {
            return;
        }
        if ("STUDENT".equals(principal.getUserType())) {
            if (!row.getStudentId().equals(principal.getRefId())) {
                throw new BusinessException(403, "无权查看他人的请假记录");
            }
            return;
        }
        if ("TEACHER".equals(principal.getUserType())) {
            Student student = studentMapper.selectById(row.getStudentId());
            if (student == null) {
                throw new BusinessException("学生信息缺失");
            }
            Clazz clazz = classMapper.selectById(student.getClassId());
            if (clazz == null) {
                throw new BusinessException("班级信息缺失");
            }
            Long teacherId = principal.getRefId();
            College college = collegeMapper.selectById(clazz.getCollegeId());
            boolean related = teacherId.equals(clazz.getHeadTeacherId())
                    || teacherId.equals(clazz.getCounselorTeacherId())
                    || (college != null && teacherId.equals(college.getLeaderTeacherId()));
            if (!related) {
                throw new BusinessException(403, "无权查看他人的请假记录");
            }
            return;
        }
        throw new BusinessException(403, "无权查看他人的请假记录");
    }

    private LeaveRecord loadPending(Long id) {
        LeaveRecord record = leaveMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("请假记录不存在");
        }
        String status = record.getStatus();
        if (!LeaveStatus.PENDING_HEAD.equals(status)
                && !LeaveStatus.PENDING_COUNSELOR.equals(status)
                && !LeaveStatus.PENDING_LEADER.equals(status)) {
            throw new BusinessException("该请假已处理完毕，无需重复审批");
        }
        return record;
    }

    private void checkScope(LeaveRecord record, Long teacherId, Integer level, LoginPrincipal principal) {
        requireLevelRole(level);
        Student student = studentMapper.selectById(record.getStudentId());
        if (student == null) {
            throw new BusinessException("学生信息缺失");
        }
        Clazz clazz = classMapper.selectById(student.getClassId());
        if (clazz == null) {
            throw new BusinessException("班级信息缺失");
        }
        boolean ok;
        String roleTip;
        if (level == 1) {
            ok = teacherId.equals(clazz.getHeadTeacherId());
            roleTip = "班主任";
        } else if (level == 2) {
            ok = teacherId.equals(clazz.getCounselorTeacherId());
            roleTip = "辅导员";
        } else {
            College college = collegeMapper.selectById(clazz.getCollegeId());
            ok = college != null && teacherId.equals(college.getLeaderTeacherId());
            roleTip = "院领导";
        }
        if (!ok) {
            throw new BusinessException("您不是该学生的" + roleTip + "，无权处理此申请");
        }
    }

    private void advance(LeaveRecord record) {
        List<Integer> levels = requiredLevels(record.getDays());
        int idx = levels.indexOf(record.getCurrentLevel());
        if (idx < 0) {
            throw new BusinessException("审批环节异常");
        }
        if (idx + 1 < levels.size()) {
            int next = levels.get(idx + 1);
            record.setCurrentLevel(next);
            record.setStatus(statusOfLevel(next));
        } else {
            record.setCurrentLevel(null);
            record.setStatus(LeaveStatus.APPROVED);
        }
        leaveMapper.updateById(record);
    }

    private String statusOfLevel(int level) {
        return switch (level) {
            case 1 -> LeaveStatus.PENDING_HEAD;
            case 2 -> LeaveStatus.PENDING_COUNSELOR;
            case 3 -> LeaveStatus.PENDING_LEADER;
            default -> throw new BusinessException("审批环节参数错误");
        };
    }

    private void requireTeacher(LoginPrincipal principal) {
        if (principal == null || !"TEACHER".equals(principal.getUserType()) || principal.getRefId() == null) {
            throw new BusinessException(403, "仅教师账号可操作审批");
        }
    }

    private void requireLevelRole(Integer level) {
        if (level == null || level < 1 || level > 3) {
            throw new BusinessException("审批环节参数错误");
        }
        String role = switch (level) {
            case 1 -> RoleCodes.HEAD_TEACHER;
            case 2 -> RoleCodes.COUNSELOR;
            default -> RoleCodes.COLLEGE_LEADER;
        };
        SecurityUtil.requireRole(role);
    }

    private void addAudit(Long leaveId, Integer level, Long operatorUserId,
                          String operatorName, String action, String comment) {
        LeaveAudit audit = new LeaveAudit();
        audit.setLeaveId(leaveId);
        audit.setLevel(level);
        audit.setOperatorUserId(operatorUserId);
        audit.setOperatorName(operatorName);
        audit.setAction(action);
        audit.setComment(comment);
        audit.setAuditTime(LocalDateTime.now());
        auditMapper.insert(audit);
    }

    private String generateLeaveNo() {
        String time = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        return "LV" + time + ThreadLocalRandom.current().nextInt(100, 1000);
    }
}
