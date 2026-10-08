package com.leave.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leave.common.BusinessException;
import com.leave.dto.LoginRequest;
import com.leave.dto.LoginResponse;
import com.leave.entity.Menu;
import com.leave.entity.Role;
import com.leave.entity.User;
import com.leave.mapper.MenuMapper;
import com.leave.mapper.RoleMapper;
import com.leave.mapper.UserMapper;
import com.leave.security.JwtUtil;
import com.leave.security.LoginPrincipal;
import com.leave.security.SecurityUtil;
import com.leave.vo.LoginUserInfo;
import com.leave.vo.MenuNode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 登录认证服务
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final MenuService menuService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被停用，请联系管理员");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new LoginResponse(token, buildUserInfo(user));
    }

    /** 获取当前登录用户的完整信息（登录后刷新页面时调用） */
    public LoginUserInfo getCurrentUserInfo() {
        LoginPrincipal principal = SecurityUtil.currentPrincipal();
        User user = userMapper.selectById(principal.getUserId());
        if (user == null) {
            throw new BusinessException(401, "用户不存在，请重新登录");
        }
        return buildUserInfo(user);
    }

    public void logout() {
        // JWT 无状态：服务端无需维护会话，前端清除 token 即可
    }

    private LoginUserInfo buildUserInfo(User user) {
        LoginUserInfo info = new LoginUserInfo();
        info.setUserId(user.getId());
        info.setUsername(user.getUsername());
        info.setRealName(user.getRealName());
        info.setUserType(user.getUserType());

        List<Role> roles = roleMapper.selectRolesByUserId(user.getId());
        List<LoginUserInfo.RoleBrief> roleBriefs = roles.stream()
                .map(r -> new LoginUserInfo.RoleBrief(r.getRoleCode(), r.getRoleName()))
                .toList();
        info.setRoles(roleBriefs);

        List<Menu> menus = menuMapper.selectMenusByUserId(user.getId());
        info.setMenus(menuService.buildTree(menus));
        return info;
    }
}
