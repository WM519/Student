package com.leave.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leave.entity.User;
import com.leave.mapper.RoleMapper;
import com.leave.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义 Realm：JWT 无状态认证 + 基于角色的授权
 */
@Component
@RequiredArgsConstructor
public class UserRealm extends AuthorizingRealm {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    /** 认证：校验 token 并加载用户 */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) {
        JwtToken jwtToken = (JwtToken) token;
        try {
            Claims claims = jwtUtil.parse(jwtToken.getToken());
            Long userId = jwtUtil.getUserId(claims);
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new AuthenticationException("用户不存在");
            }
            if (user.getStatus() == null || user.getStatus() != 1) {
                throw new AuthenticationException("账号已被停用");
            }
            LoginPrincipal principal = new LoginPrincipal(
                    user.getId(), user.getUsername(), user.getRealName(),
                    user.getUserType(), user.getRefId());
            return new SimpleAuthenticationInfo(principal, jwtToken.getCredentials(), getName());
        } catch (AuthenticationException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthenticationException("token 无效或已过期", e);
        }
    }

    /** 授权：加载用户角色编码 */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        LoginPrincipal principal = (LoginPrincipal) principals.getPrimaryPrincipal();
        List<String> roleCodes = roleMapper.selectCodesByUserId(principal.getUserId());
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        info.addRoles(roleCodes);
        return info;
    }
}
