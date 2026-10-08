package com.leave.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Shiro 认证主体（存于 PrincipalCollection，用于接口鉴权）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginPrincipal implements Serializable {
    private Long userId;
    private String username;
    private String realName;
    private String userType;
    private Long refId;
}
