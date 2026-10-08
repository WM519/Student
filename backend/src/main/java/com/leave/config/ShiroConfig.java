package com.leave.config;

import com.leave.security.JwtFilter;
import com.leave.security.UserRealm;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.filter.authc.AnonymousFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import jakarta.servlet.Filter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shiro 配置：无状态安全管理器 + JWT 过滤器链
 */
@Configuration
public class ShiroConfig {

    @Bean
    public DefaultWebSecurityManager securityManager(UserRealm userRealm) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(userRealm);
        securityManager.setRememberMeManager(null);

        // 无状态：不把登录状态写入 Session
        DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();
        DefaultSessionStorageEvaluator sessionStorageEvaluator = new DefaultSessionStorageEvaluator();
        sessionStorageEvaluator.setSessionStorageEnabled(false);
        subjectDAO.setSessionStorageEvaluator(sessionStorageEvaluator);
        securityManager.setSubjectDAO(subjectDAO);
        return securityManager;
    }

    /**
     * Shiro 过滤器链注册（token 拦截器）：
     * 登录接口放行，其余接口全部经过 JwtFilter
     */
    @Bean
    public FilterRegistrationBean<Filter> shiroFilterRegistration(DefaultWebSecurityManager securityManager) throws Exception {
        ShiroFilterFactoryBean factory = new ShiroFilterFactoryBean();
        factory.setSecurityManager(securityManager);

        Map<String, Filter> filters = new LinkedHashMap<>();
        filters.put("anon", new AnonymousFilter());
        filters.put("jwt", new JwtFilter());
        factory.setFilters(filters);

        Map<String, String> chain = new LinkedHashMap<>();
        chain.put("/api/auth/login", "anon");
        chain.put("/**", "jwt");
        factory.setFilterChainDefinitionMap(chain);

        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter((Filter) factory.getObject());
        registration.addUrlPatterns("/*");
        registration.setName("shiroFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
