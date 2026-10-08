package com.leave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leave.entity.Teacher;
import com.leave.vo.TeacherOptionVO;
import com.leave.vo.TeacherRowVO;
import com.leave.vo.TeacherRoleVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TeacherMapper extends BaseMapper<Teacher> {

    IPage<TeacherRowVO> selectTeacherPage(Page<TeacherRowVO> page,
                                          @Param("keyword") String keyword,
                                          @Param("collegeId") Long collegeId);

    List<TeacherOptionVO> selectTeacherOptions(@Param("roleCode") String roleCode,
                                               @Param("collegeId") Long collegeId);

    List<TeacherRoleVO> selectTeacherRoles(@Param("teacherIds") List<Long> teacherIds);
}
