package com.leave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leave.entity.LeaveRecord;
import com.leave.vo.LeaveRowVO;
import com.leave.vo.NameValueVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LeaveMapper extends BaseMapper<LeaveRecord> {

    /** 学生：我的请假 */
    IPage<LeaveRowVO> selectMyLeaves(Page<LeaveRowVO> page,
                                     @Param("studentId") Long studentId,
                                     @Param("status") String status);

    /** 管理员：全部请假 */
    IPage<LeaveRowVO> selectAllLeaves(Page<LeaveRowVO> page,
                                      @Param("keyword") String keyword,
                                      @Param("status") String status,
                                      @Param("collegeId") Long collegeId,
                                      @Param("leaveType") String leaveType);

    /** 教师：待办/已办 */
    IPage<LeaveRowVO> selectApprovalLeaves(Page<LeaveRowVO> page,
                                           @Param("level") Integer level,
                                           @Param("teacherId") Long teacherId,
                                           @Param("userId") Long userId,
                                           @Param("keyword") String keyword,
                                           @Param("done") Boolean done);

    /** 根据主键查询请假单行（含学生/班级/学院信息） */
    LeaveRowVO selectLeaveRowById(@Param("leaveId") Long leaveId);

    /** 教师：当前待办数量（班主任/辅导员/院领导，可多角色叠加） */
    long countPendingByTeacher(@Param("teacherId") Long teacherId);

    /** 教师：指定环节待办数量（level=1/2/3） */
    long countPendingAtLevel(@Param("level") Integer level, @Param("teacherId") Long teacherId);

    /** 学生或管理员按状态统计 */
    List<NameValueVO> countGroupByStatus(@Param("studentId") Long studentId);

    /** 学生或管理员按请假类型统计 */
    List<NameValueVO> countGroupByType(@Param("studentId") Long studentId);

    /** 管理员按学院统计 */
    List<NameValueVO> countGroupByCollege();
}
