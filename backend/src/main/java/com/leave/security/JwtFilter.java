package com.leave.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leave.common.Result;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.web.filter.AccessControlFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Token 拦截器（Shiro 过滤器链中的 JWT 过滤器）：
 * 从 Authorization: Bearer 中取出 token，校验通过后放行，否则返回 401 JSON
 */
public class JwtFilter extends AccessControlFilter {

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        return false;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws IOException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // 跨域预检直接放行（CorsFilter 会提前处理带 Origin 的 OPTIONS）
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            return true;
        }
        String token = getToken(httpRequest);
        if (token == null || token.isEmpty()) {
            write401(response, "未登录，请先登录");
            return false;
        }
        try {
            SecurityUtils.getSubject().login(new JwtToken(token));
            return true;
        } catch (AuthenticationException e) {
            write401(response, "登录已过期，请重新登录");
            return false;
        } catch (Exception e) {
            write401(response, "认证失败：" + e.getMessage());
            return false;
        }
    }

    private String getToken(HttpServletRequest request) {
        String header = request.getHeader(TOKEN_HEADER);
        if (header != null && header.startsWith(TOKEN_PREFIX)) {
            return header.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    private void write401(ServletResponse response, String message) throws IOException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpResponse.setContentType("application/json");
        httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        httpResponse.getWriter().write(objectMapper.writeValueAsString(Result.fail(401, message)));
    }
}
