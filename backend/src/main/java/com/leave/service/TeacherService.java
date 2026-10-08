package com.leave.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leave.common.BusinessException;
import com.leave.common.PageResult;
import com.leave.dto.PageQuery;
import com.leave.dto.TeacherSaveRequest;
import com.leave.entity.Clazz;
import com.leave.entity.College;
import com.leave.entity.Role;
import com.leave.entity.Teacher;
import com.leave.entity.User;
import com.leave.entity.UserRole;
import com.leave.mapper.ClassMapper;
import com.leave.mapper.CollegeMapper;
import com.leave.mapper.RoleMapper;
import com.leave.mapper.TeacherMapper;
import com.leave.mapper.UserMapper;
import com.leave.mapper.UserRoleMapper;
import com.leave.vo.TeacherOptionVO;
import com.leave.vo.TeacherRoleVO;
import com.leave.vo.TeacherRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 教师管理服务（教师数据 + 登录账号 + 角色分配）
 */
@Service
@RequiredArgsConstructor
public class TeacherService {

    public static final String DEFAULT_PASSWORD = "123456";

    private final TeacherMapper teacherMapper;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final CollegeMapper collegeMapper;
    private final ClassMapper classMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<TeacherRowVO> page(PageQuery query, Long collegeId) {
        IPage<TeacherRowVO> page = teacherMapper.selectTeacherPage(
                new Page<>(query.getPageNum(), query.getPageSize()), query.getKeyword(), collegeId);
        List<TeacherRowVO> records = page.getRecords();
        if (!records.isEmpty()) {
            List<Long> ids = records.stream().map(TeacherRowVO::getId).toList();
            Map<Long, List<TeacherRoleVO>> roleMap = teacherMapper.selectTeacherRoles(ids)
                    .stream().collect(Collectors.groupingBy(TeacherRoleVO::getTeacherId));
            records.forEach(row -> {
                List<TeacherRoleVO> roles = roleMap.getOrDefault(row.getId(), List.of());
                row.setRoleIds(roles.stream().map(TeacherRoleVO::getRoleId).toList());
                row.setRoleCodes(roles.stream().map(TeacherRoleVO::getRoleCode).toList());
                row.setRoleNames(roles.stream().map(TeacherRoleVO::getRoleName).toList());
            });
        }
        return new PageResult<>(page.getTotal(), records);
    }

    public List<TeacherOptionVO> options(String roleCode, Long collegeId) {
        return teacherMapper.selectTeacherOptions(roleCode, collegeId);
    }

    @Transactional
    public void save(TeacherSaveRequest request) {
        validateCollege(request.getCollegeId());
        checkNoUnique(request.getTeacherNo(), null);

        Teacher teacher = new Teacher();
        teacher.setTeacherNo(request.getTeacherNo());
        teacher.setName(request.getName());
        teacher.setGender(request.getGender());
        teacher.setPhone(request.getPhone());
        teacher.setEmail(request.getEmail());
        teacher.setCollegeId(request.getCollegeId());
        teacherMapper.insert(teacher);
        createAccount(teacher, request.getRoleIds());
    }

    @Transactional
    public void update(Long id, TeacherSaveRequest request) {
        Teacher teacher = teacherMapper.selectById(id);
        if (teacher == null) {
            throw new BusinessException("教师不存在");
        }
        validateCollege(request.getCollegeId());
        checkNoUnique(request.getTeacherNo(), id);

        teacher.setTeacherNo(request.getTeacherNo());
        teacher.setName(request.getName());
        teacher.setGender(request.getGender());
        teacher.setPhone(request.getPhone());
        teacher.setEmail(request.getEmail());
        teacher.setCollegeId(request.getCollegeId());
        teacherMapper.updateById(teacher);

        User user = findAccount(id);
        if (user != null) {
            user.setUsername(request.getTeacherNo());
            user.setRealName(request.getName());
            userMapper.updateById(user);
            replaceRoles(user.getId(), request.getRoleIds());
        } else {
            createAccount(teacher, request.getRoleIds());
        }
    }

    @Transactional
    public void delete(Long id) {
        Long headClasses = classMapper.selectCount(new LambdaQueryWrapper<Clazz>().eq(Clazz::getHeadTeacherId, id));
        Long counselorClasses = classMapper.selectCount(new LambdaQueryWrapper<Clazz>().eq(Clazz::getCounselorTeacherId, id));
        if ((headClasses != null && headClasses > 0) || (counselorClasses != null && counselorClasses > 0)) {
            throw new BusinessException("该教师已担任班级的班主任/辅导员，无法删除");
        }
        Long ledColleges = collegeMapper.selectCount(new LambdaQueryWrapper<College>()
                .eq(College::getLeaderTeacherId, id));
        if (ledColleges != null && ledColleges > 0) {
            throw new BusinessException("该教师已担任学院领导，无法删除");
        }
        User user = findAccount(id);
        if (user != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
            userMapper.deleteById(user.getId());
        }
        teacherMapper.deleteById(id);
    }

    public void toggleStatus(Long id, Integer status) {
        User user = findAccount(id);
        if (user == null) {
            throw new BusinessException("教师登录账号不存在");
        }
        user.setStatus(status == null || status != 1 ? 0 : 1);
        userMapper.updateById(user);
    }

    /** 重置密码为默认密码 123456 */
    public void resetPassword(Long id) {
        User user = findAccount(id);
        if (user == null) {
            throw new BusinessException("教师登录账号不存在");
        }
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        userMapper.updateById(user);
    }

    private void createAccount(Teacher teacher, List<Long> roleIds) {
        User user = new User();
        user.setUsername(teacher.getTeacherNo());
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setRealName(teacher.getName());
        user.setUserType("TEACHER");
        user.setRefId(teacher.getId());
        user.setStatus(1);
        userMapper.insert(user);
        replaceRoles(user.getId(), roleIds);
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds.stream().distinct().toList()) {
            Role role = roleMapper.selectById(roleId);
            if (role == null) {
                throw new BusinessException("角色不存在");
            }
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }

    private User findAccount(Long teacherId) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getRefId, teacherId).eq(User::getUserType, "TEACHER"));
    }

    private void validateCollege(Long collegeId) {
        if (collegeId == null || collegeMapper.selectById(collegeId) == null) {
            throw new BusinessException("所属学院不存在");
        }
    }

    private void checkNoUnique(String teacherNo, Long excludeId) {
        if (!StringUtils.hasText(teacherNo)) {
            throw new BusinessException("工号不能为空");
        }
        LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<Teacher>().eq(Teacher::getTeacherNo, teacherNo);
        if (excludeId != null) {
            wrapper.ne(Teacher::getId, excludeId);
        }
        if (teacherMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("工号已存在");
        }
    }
}
