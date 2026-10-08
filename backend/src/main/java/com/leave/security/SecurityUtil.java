package com.leave.security;

import com.leave.common.BusinessException;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

/**
 * 当前登录用户工具类
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static LoginPrincipal currentPrincipal() {
        Subject subject = SecurityUtils.getSubject();
        Object principal = subject.getPrincipal();
        if (principal == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        if (principal instanceof LoginPrincipal p) {
            return p;
        }
        // 兼容 principal 存为字符串的情况（正常不会发生）
        throw new BusinessException(401, "登录状态异常，请重新登录");
    }

    public static Long currentUserId() {
        return currentPrincipal().getUserId();
    }

    public static boolean hasRole(String roleCode) {
        return SecurityUtils.getSubject().hasRole(roleCode);
    }

    /**
     * 接口级鉴权：不具备指定角色则拒绝
     */
    public static void requireRole(String roleCode) {
        if (!hasRole(roleCode)) {
            throw new BusinessException(403, "无权限执行该操作");
        }
    }
}
