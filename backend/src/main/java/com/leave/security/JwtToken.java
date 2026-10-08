package com.leave.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.shiro.authc.AuthenticationToken;

/**
 * JWT 认证令牌
 */
@Data
@AllArgsConstructor
public class JwtToken implements AuthenticationToken {

    private final String token;

    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }
}
