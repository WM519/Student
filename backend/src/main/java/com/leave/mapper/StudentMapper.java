package com.leave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leave.entity.Student;
import com.leave.vo.StudentRowVO;
import org.apache.ibatis.annotations.Param;

public interface StudentMapper extends BaseMapper<Student> {

    IPage<StudentRowVO> selectStudentPage(Page<StudentRowVO> page,
                                          @Param("keyword") String keyword,
                                          @Param("classId") Long classId);
}
