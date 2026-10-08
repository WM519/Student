package com.leave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leave.entity.LeaveAudit;
import com.leave.vo.AuditVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface LeaveAuditMapper extends BaseMapper<LeaveAudit> {

    @Select("SELECT id,leave_id,level,operator_user_id,operator_name,action,comment,audit_time " +
            "FROM t_leave_audit WHERE leave_id=#{leaveId} ORDER BY audit_time ASC,id ASC")
    List<AuditVO> selectAuditsByLeaveId(@Param("leaveId") Long leaveId);
}
