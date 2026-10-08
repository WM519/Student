package com.leave.api;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 接口测试基类：真实启动 Spring Boot 应用（随机端口），通过 HTTP 访问真实接口与真实 MySQL。
 *
 * <p>被测系统的统一响应体为 {@code Result{code,message,data}}，故断言分两层：
 * 1）HTTP 状态码（网络/认证层，401 由 JwtFilter 直接写出）；
 * 2）业务码（{@code code}：200 成功、400 业务或参数错误、403 无权限、500 系统异常）。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ApiTestBase {

    /** 演示账号统一初始密码 */
    protected static final String DEFAULT_PWD = "123456";

    /** 本用例集创建的请假数据统一带有该前缀，便于测试前后自动清理，保证用例可重复执行 */
    protected static final String TEST_DATA_TAG = "[自动化测试]";

    private static final AtomicBoolean PURGED = new AtomicBoolean(false);

    @Autowired
    protected TestRestTemplate rest;

    @Autowired(required = false)
    protected JdbcTemplate jdbcTemplate;

    @LocalServerPort
    protected int port;

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    /** 每轮测试开始前清理上一轮遗留的自动化测试数据，保证用例可重复执行且演示数据不被污染 */
    @BeforeEach
    void purgeBeforeFirstCase() {
        if (PURGED.compareAndSet(false, true)) {
            purgeTestData();
        }
    }

    /** 测试结束后再次清理，使演示库回到干净状态（用例证据以 surefire 报告为准） */
    @AfterAll
    void purgeAfterAllCases() {
        purgeTestData();
    }

    protected void purgeTestData() {
        if (jdbcTemplate == null) {
            return;
        }
        List<Long> leaveIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_leave WHERE reason LIKE ?", Long.class, TEST_DATA_TAG + "%");
        if (!leaveIds.isEmpty()) {
            String placeholders = String.join(",", leaveIds.stream().map(id -> "?").toList());
            jdbcTemplate.update("DELETE FROM t_leave_audit WHERE leave_id IN (" + placeholders + ")",
                    leaveIds.toArray());
            jdbcTemplate.update("DELETE FROM t_leave WHERE id IN (" + placeholders + ")", leaveIds.toArray());
        }
        jdbcTemplate.update("DELETE FROM t_user_role WHERE user_id IN "
                + "(SELECT id FROM t_user WHERE real_name LIKE '接口测试%')");
        jdbcTemplate.update("DELETE FROM t_user WHERE real_name LIKE '接口测试%'");
        jdbcTemplate.update("DELETE FROM t_student WHERE name LIKE '接口测试%'");
    }

    // ------------------------------ 通用请求封装 ------------------------------

    protected ApiResponse exchange(HttpMethod method, String path, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        ResponseEntity<Map> response = rest.exchange(baseUrl() + path, method,
                new HttpEntity<>(body, headers), Map.class);
        return new ApiResponse(response.getStatusCode().value(), response.getBody());
    }

    protected ApiResponse get(String path, String token) {
        return exchange(HttpMethod.GET, path, null, token);
    }

    protected ApiResponse post(String path, Object body, String token) {
        return exchange(HttpMethod.POST, path, body, token);
    }

    protected ApiResponse put(String path, Object body, String token) {
        return exchange(HttpMethod.PUT, path, body, token);
    }

    protected ApiResponse delete(String path, String token) {
        return exchange(HttpMethod.DELETE, path, null, token);
    }

    /** 登录并返回 JWT Token；登录失败时直接断言失败 */
    protected String login(String username, String password) {
        ApiResponse response = post("/api/auth/login", Map.of("username", username, "password", password), null);
        assertEquals(200, response.status(), "登录应返回 HTTP 200：" + response.raw());
        assertEquals(200, response.code(), "登录应返回业务码 200：" + response.raw());
        Object token = response.data().get("token");
        assertNotNull(token, "登录响应应包含 token");
        return String.valueOf(token);
    }

    protected String loginAsAdmin() {
        return login("admin", "admin123");
    }

    /** 构造请假申请请求体 */
    protected Map<String, Object> leaveBody(String leaveType, String startDate, String endDate, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("leaveType", leaveType);
        body.put("startDate", startDate);
        body.put("endDate", endDate);
        body.put("reason", reason);
        return body;
    }

    // ------------------------------ 响应对象 ------------------------------

    /** 接口返回：HTTP 状态码 + 业务响应体 */
    protected static final class ApiResponse {

        private final int status;
        private final Map<String, Object> body;

        ApiResponse(int status, Map<String, Object> body) {
            this.status = status;
            this.body = body;
        }

        int status() {
            return status;
        }

        @SuppressWarnings("unchecked")
        int code() {
            Object code = body == null ? null : body.get("code");
            return code == null ? -1 : ((Number) code).intValue();
        }

        String message() {
            Object message = body == null ? null : body.get("message");
            return message == null ? null : String.valueOf(message);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data() {
            Object data = body == null ? null : body.get("data");
            return data instanceof Map ? (Map<String, Object>) data : new HashMap<>();
        }

        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> records() {
            Object data = body == null ? null : body.get("data");
            if (data instanceof Map<?, ?> map) {
                Object records = map.get("records");
                if (records instanceof java.util.List<?> list) {
                    return (java.util.List<Map<String, Object>>) list;
                }
            }
            return java.util.List.of();
        }

        long total() {
            Object data = body == null ? null : body.get("data");
            if (data instanceof Map<?, ?> map) {
                Object total = map.get("total");
                if (total instanceof Number number) {
                    return number.longValue();
                }
            }
            return 0L;
        }

        String raw() {
            return "HTTP " + status + " body=" + body;
        }

        /** 原始响应体（用于“列表型 data”等无法按 Map 断言的场景） */
        Map<String, Object> body() {
            return body == null ? new HashMap<>() : body;
        }
    }
}
