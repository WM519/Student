package com.leave.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leave.common.BusinessException;
import com.leave.common.PageResult;
import com.leave.dto.PageQuery;
import com.leave.dto.StudentSaveRequest;
import com.leave.entity.Clazz;
import com.leave.entity.Role;
import com.leave.entity.Student;
import com.leave.entity.User;
import com.leave.entity.UserRole;
import com.leave.mapper.ClassMapper;
import com.leave.mapper.LeaveMapper;
import com.leave.mapper.RoleMapper;
import com.leave.mapper.StudentMapper;
import com.leave.mapper.UserMapper;
import com.leave.mapper.UserRoleMapper;
import com.leave.vo.StudentRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 学生管理服务（学生数据 + 登录账号 + 自动分配学生角色）
 */
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final ClassMapper classMapper;
    private final LeaveMapper leaveMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<StudentRowVO> page(PageQuery query, Long classId) {
        IPage<StudentRowVO> page = studentMapper.selectStudentPage(
                new Page<>(query.getPageNum(), query.getPageSize()), query.getKeyword(), classId);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    @Transactional
    public void save(StudentSaveRequest request) {
        validateClass(request.getClassId());
        checkNoUnique(request.getStudentNo(), null);

        Student student = new Student();
        copyFields(student, request);
        studentMapper.insert(student);
        createAccount(student);
    }

    @Transactional
    public void update(Long id, StudentSaveRequest request) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }
        validateClass(request.getClassId());
        checkNoUnique(request.getStudentNo(), id);

        copyFields(student, request);
        studentMapper.updateById(student);

        User user = findAccount(id);
        if (user != null) {
            user.setUsername(request.getStudentNo());
            user.setRealName(request.getName());
            userMapper.updateById(user);
        }
    }

    @Transactional
    public void delete(Long id) {
        Long leaves = leaveMapper.selectCount(new LambdaQueryWrapper<com.leave.entity.LeaveRecord>()
                .eq(com.leave.entity.LeaveRecord::getStudentId, id));
        if (leaves != null && leaves > 0) {
            throw new BusinessException("该学生存在请假记录，无法删除");
        }
        User user = findAccount(id);
        if (user != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
            userMapper.deleteById(user.getId());
        }
        studentMapper.deleteById(id);
    }

    public void toggleStatus(Long id, Integer status) {
        User user = findAccount(id);
        if (user == null) {
            throw new BusinessException("学生登录账号不存在");
        }
        user.setStatus(status == null || status != 1 ? 0 : 1);
        userMapper.updateById(user);
    }

    public void resetPassword(Long id) {
        User user = findAccount(id);
        if (user == null) {
            throw new BusinessException("学生登录账号不存在");
        }
        user.setPassword(passwordEncoder.encode(TeacherService.DEFAULT_PASSWORD));
        userMapper.updateById(user);
    }

    private void createAccount(Student student) {
        User user = new User();
        user.setUsername(student.getStudentNo());
        user.setPassword(passwordEncoder.encode(TeacherService.DEFAULT_PASSWORD));
        user.setRealName(student.getName());
        user.setUserType("STUDENT");
        user.setRefId(student.getId());
        user.setStatus(1);
        userMapper.insert(user);

        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleCode, "student").eq(Role::getStatus, 1));
        if (role != null) {
            UserRole ur = new UserRole();
            ur.setUserId(user.getId());
            ur.setRoleId(role.getId());
            userRoleMapper.insert(ur);
        }
    }

    private void copyFields(Student student, StudentSaveRequest request) {
        student.setStudentNo(request.getStudentNo());
        student.setName(request.getName());
        student.setGender(request.getGender());
        student.setPhone(request.getPhone());
        student.setEmail(request.getEmail());
        student.setClassId(request.getClassId());
        student.setEnrollYear(request.getEnrollYear());
    }

    private User findAccount(Long studentId) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getRefId, studentId).eq(User::getUserType, "STUDENT"));
    }

    private void validateClass(Long classId) {
        if (classId == null || classMapper.selectById(classId) == null) {
            throw new BusinessException("所属班级不存在");
        }
    }

    private void checkNoUnique(String studentNo, Long excludeId) {
        if (!StringUtils.hasText(studentNo)) {
            throw new BusinessException("学号不能为空");
        }
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<Student>().eq(Student::getStudentNo, studentNo);
        if (excludeId != null) {
            wrapper.ne(Student::getId, excludeId);
        }
        if (studentMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("学号已存在");
        }
    }
}
