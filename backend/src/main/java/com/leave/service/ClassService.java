package com.leave.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leave.common.BusinessException;
import com.leave.common.PageResult;
import com.leave.dto.PageQuery;
import com.leave.entity.Clazz;
import com.leave.entity.College;
import com.leave.entity.Student;
import com.leave.entity.Teacher;
import com.leave.mapper.ClassMapper;
import com.leave.mapper.CollegeMapper;
import com.leave.mapper.StudentMapper;
import com.leave.mapper.TeacherMapper;
import com.leave.vo.ClassRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 班级管理服务
 */
@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassMapper classMapper;
    private final CollegeMapper collegeMapper;
    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;

    public PageResult<ClassRowVO> page(PageQuery query, Long collegeId) {
        IPage<ClassRowVO> page = classMapper.selectClassPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getKeyword(), collegeId);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    /** 全部班级（下拉用，可按学院过滤） */
    public List<Clazz> listAll(Long collegeId) {
        LambdaQueryWrapper<Clazz> wrapper = new LambdaQueryWrapper<>();
        if (collegeId != null) {
            wrapper.eq(Clazz::getCollegeId, collegeId);
        }
        return classMapper.selectList(wrapper.orderByAsc(Clazz::getId));
    }

    public void save(Clazz clazz) {
        validate(clazz);
        classMapper.insert(clazz);
    }

    public void update(Clazz clazz) {
        if (clazz.getId() == null) {
            throw new BusinessException("班级 ID 不能为空");
        }
        validate(clazz);
        classMapper.updateById(clazz);
    }

    public void delete(Long id) {
        Long students = studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getClassId, id));
        if (students != null && students > 0) {
            throw new BusinessException("该班级下仍有学生，无法删除");
        }
        classMapper.deleteById(id);
    }

    private void validate(Clazz clazz) {
        if (clazz.getCollegeId() == null || collegeMapper.selectById(clazz.getCollegeId()) == null) {
            throw new BusinessException("所属学院不存在");
        }
        checkTeacherBelong(clazz.getHeadTeacherId(), clazz.getCollegeId(), "班主任");
        checkTeacherBelong(clazz.getCounselorTeacherId(), clazz.getCollegeId(), "辅导员");
    }

    private void checkTeacherBelong(Long teacherId, Long collegeId, String roleName) {
        if (teacherId == null) {
            throw new BusinessException("请选择" + roleName);
        }
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new BusinessException(roleName + "教师不存在");
        }
        if (!collegeId.equals(teacher.getCollegeId())) {
            throw new BusinessException(roleName + "必须属于所选学院");
        }
    }
}
