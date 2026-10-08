package com.leave.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 模块一：登录认证接口测试（对应功能模块“登录认证”）。
 */
@DisplayName("接口测试-登录认证模块")
class AuthApiTest extends ApiTestBase {

    @Test
    @DisplayName("TC-API-01 正确账号密码登录成功并返回 Token")
    void loginSuccessReturnsToken() {
        ApiResponse response = post("/api/auth/login",
                Map.of("username", "admin", "password", "admin123"), null);

        assertEquals(200, response.status());
        assertEquals(200, response.code());
        assertNotNull(response.data().get("token"), "登录成功应返回 JWT Token");
        assertTrue(String.valueOf(response.data().get("token")).split("\\.").length == 3, "Token 应为 JWT 三段式");
    }

    @Test
    @DisplayName("TC-API-02 密码错误登录失败并提示统一错误信息")
    void loginFailsWithWrongPassword() {
        ApiResponse response = post("/api/auth/login",
                Map.of("username", "admin", "password", "wrong-password"), null);

        assertEquals(200, response.status(), "业务错误仍以 HTTP 200 + 业务码返回");
        assertEquals(400, response.code());
        assertEquals("用户名或密码错误", response.message());
    }

    @Test
    @DisplayName("TC-API-03 用户名不存在登录失败（不泄露账号是否存在）")
    void loginFailsWithUnknownUser() {
        ApiResponse response = post("/api/auth/login",
                Map.of("username", "not-exist-user", "password", "123456"), null);

        assertEquals(400, response.code());
        assertEquals("用户名或密码错误", response.message());
    }

    @Test
    @DisplayName("TC-API-04 用户名为空触发参数校验")
    void loginFailsWhenUsernameBlank() {
        ApiResponse response = post("/api/auth/login",
                Map.of("username", "", "password", "admin123"), null);

        assertEquals(400, response.code());
        assertEquals("用户名不能为空", response.message());
    }

    @Test
    @DisplayName("TC-API-05 无 Token 访问受保护接口返回 401")
    void anonymousAccessRejected() {
        ApiResponse response = get("/api/auth/info", null);

        assertEquals(401, response.status(), "JwtFilter 应直接返回 HTTP 401");
        assertEquals(401, response.code());
        assertEquals("未登录，请先登录", response.message());
    }

    @Test
    @DisplayName("TC-API-06 非法或篡改 Token 访问返回 401")
    void invalidTokenRejected() {
        ApiResponse response = get("/api/auth/info", "eyJhbGciOiJIUzI1NiJ9.invalid.signature");

        assertEquals(401, response.status());
        assertEquals(401, response.code());
    }

    @Test
    @DisplayName("TC-API-07 携带合法 Token 可获取当前用户信息与角色菜单")
    void infoReturnsCurrentUser() {
        String token = loginAsAdmin();
        ApiResponse response = get("/api/auth/info", token);

        assertEquals(200, response.code());
        assertEquals("admin", String.valueOf(response.data().get("username")));
        assertEquals("ADMIN", String.valueOf(response.data().get("userType")));
        Object roles = response.data().get("roles");
        assertTrue(roles instanceof List<?> && !((List<?>) roles).isEmpty(), "应返回角色列表");
        Object menus = response.data().get("menus");
        assertTrue(menus instanceof List<?> && !((List<?>) menus).isEmpty(), "应返回动态菜单树");
    }

    @Test
    @DisplayName("TC-API-08 被停用的账号无法登录")
    void disabledAccountCannotLogin() {
        String adminToken = loginAsAdmin();
        String studentNo = "T" + System.currentTimeMillis();
        ApiResponse created = post("/api/students", Map.of(
                "studentNo", studentNo,
                "name", "接口测试停用账号",
                "gender", "男",
                "classId", 1,
                "enrollYear", 2026), adminToken);
        assertEquals(200, created.code(), "前置：创建临时学生成功 " + created.raw());

        ApiResponse page = get("/api/students?pageNum=1&pageSize=50&keyword=" + studentNo, adminToken);
        Map<String, Object> row = page.records().stream()
                .filter(r -> studentNo.equals(String.valueOf(r.get("studentNo"))))
                .findFirst().orElseThrow();
        long id = ((Number) row.get("id")).longValue();

        try {
            assertEquals(200, put("/api/students/" + id + "/status/0", null, adminToken).code(),
                    "前置：停用该账号");

            ApiResponse login = post("/api/auth/login",
                    Map.of("username", studentNo, "password", DEFAULT_PWD), null);
            assertEquals(400, login.code());
            assertEquals("账号已被停用，请联系管理员", login.message());
        } finally {
            assertEquals(200, delete("/api/students/" + id, adminToken).code(), "清理：删除临时学生");
        }
    }

    @Test
    @DisplayName("TC-API-09 登录后登出接口幂等可用")
    void logoutAlwaysSucceeds() {
        String token = login("stu04", DEFAULT_PWD);
        assertFalse(token.isEmpty());

        ApiResponse response = post("/api/auth/logout", Map.of(), token);
        assertEquals(200, response.code(), "JWT 无状态登出应始终返回成功");
    }
}
