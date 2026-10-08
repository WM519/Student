package com.leave.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leave.common.BusinessException;
import com.leave.common.PageResult;
import com.leave.dto.PageQuery;
import com.leave.entity.Clazz;
import com.leave.entity.College;
import com.leave.entity.Teacher;
import com.leave.mapper.ClassMapper;
import com.leave.mapper.CollegeMapper;
import com.leave.mapper.TeacherMapper;
import com.leave.vo.CollegeRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 学院管理服务
 */
@Service
@RequiredArgsConstructor
public class CollegeService {

    private final CollegeMapper collegeMapper;
    private final ClassMapper classMapper;
    private final TeacherMapper teacherMapper;

    public PageResult<CollegeRowVO> page(PageQuery query) {
        IPage<CollegeRowVO> page = collegeMapper.selectCollegePage(
                new Page<>(query.getPageNum(), query.getPageSize()), query.getKeyword());
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public List<College> listAll() {
        return collegeMapper.selectList(new LambdaQueryWrapper<College>().orderByAsc(College::getId));
    }

    public void save(College college) {
        validateLeader(college.getLeaderTeacherId());
        collegeMapper.insert(college);
    }

    public void update(College college) {
        if (college.getId() == null) {
            throw new BusinessException("学院 ID 不能为空");
        }
        validateLeader(college.getLeaderTeacherId());
        collegeMapper.updateById(college);
    }

    public void delete(Long id) {
        Long classes = classMapper.selectCount(new LambdaQueryWrapper<Clazz>().eq(Clazz::getCollegeId, id));
        if (classes != null && classes > 0) {
            throw new BusinessException("该学院下仍有班级，无法删除");
        }
        collegeMapper.deleteById(id);
    }

    private void validateLeader(Long teacherId) {
        if (teacherId == null) {
            return;
        }
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new BusinessException("院领导教师不存在");
        }
    }
}
