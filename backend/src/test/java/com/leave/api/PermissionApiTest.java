package com.leave.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 模块五：权限与系统/人员管理接口测试（管理员专属接口的越权防护与基础数据维护）。
 */
@DisplayName("接口测试-权限与系统管理模块")
class PermissionApiTest extends ApiTestBase {

    private static final String STU = "stu04";

    @Test
    @DisplayName("TC-API-36 学生访问学生管理接口被拒绝")
    void studentCannotAccessStudentManage() {
        String token = login(STU, DEFAULT_PWD);

        ApiResponse response = get("/api/students?pageNum=1&pageSize=10", token);

        assertEquals(403, response.code());
        assertEquals("无权限执行该操作", response.message());
    }

    @Test
    @DisplayName("TC-API-37 教师访问角色管理接口被拒绝")
    void teacherCannotAccessRoleManage() {
        String token = login("head02", DEFAULT_PWD);

        ApiResponse response = get("/api/roles?pageNum=1&pageSize=10", token);

        assertEquals(403, response.code());
        assertEquals("无权限执行该操作", response.message());
    }

    @Test
    @DisplayName("TC-API-38 未登录访问请假总览接口返回 401")
    void anonymousCannotAccessLeaveOverview() {
        ApiResponse response = get("/api/leaves/all?pageNum=1&pageSize=10", null);

        assertEquals(401, response.status());
        assertEquals(401, response.code());
    }

    @Test
    @DisplayName("TC-API-39 管理员分页查询学生列表成功")
    void adminCanQueryStudentPage() {
        String token = loginAsAdmin();

        ApiResponse response = get("/api/students?pageNum=1&pageSize=10", token);

        assertEquals(200, response.code());
        assertTrue(response.total() >= 4, "演示数据应至少包含 4 名学生");
        assertTrue(response.records().size() <= 10, "分页大小应生效");
    }

    @Test
    @DisplayName("TC-API-40 管理员查询教师下拉选项成功")
    void adminCanQueryTeacherOptions() {
        String token = loginAsAdmin();

        ApiResponse response = get("/api/teachers/options", token);

        assertEquals(200, response.code(), response.raw());
    }

    @Test
    @DisplayName("TC-API-41 管理员新增学院后可在列表查询到并成功删除")
    void adminCanCreateAndDeleteCollege() {
        String token = loginAsAdmin();
        String code = "TEST-CL-" + System.currentTimeMillis();
        Map<String, Object> body = Map.of(
                "collegeName", "自动化测试学院",
                "collegeCode", code,
                "description", "接口测试临时数据");

        assertEquals(200, post("/api/colleges", body, token).code(), "新增学院应成功");
        ApiResponse list = get("/api/colleges/all", token);
        assertTrue(String.valueOf(list.body()).contains(code), "列表中应包含新建学院");

        ApiResponse page = get("/api/colleges?pageNum=1&pageSize=50&keyword=" + code, token);
        long id = ((Number) page.records().get(0).get("id")).longValue();
        assertEquals(200, delete("/api/colleges/" + id, token).code(), "删除学院应成功");
        assertFalse(String.valueOf(get("/api/colleges/all", token).body()).contains(code), "删除后列表中不应再出现");
    }

    @Test
    @DisplayName("TC-API-42 学院编码重复被拒绝（唯一约束）")
    void duplicateCollegeCodeRejected() {
        String token = loginAsAdmin();
        String code = "TEST-DUP-" + System.currentTimeMillis();
        Map<String, Object> body = Map.of("collegeName", "重复编码学院", "collegeCode", code);

        assertEquals(200, post("/api/colleges", body, token).code());
        try {
            ApiResponse again = post("/api/colleges", body, token);
            assertEquals(400, again.code());
            assertTrue(String.valueOf(again.message()).contains("数据已存在"), "应提示数据已存在：" + again.message());
        } finally {
            ApiResponse page = get("/api/colleges?pageNum=1&pageSize=50&keyword=" + code, token);
            if (!page.records().isEmpty()) {
                long id = ((Number) page.records().get(0).get("id")).longValue();
                delete("/api/colleges/" + id, token);
            }
        }
    }

    @Test
    @DisplayName("TC-API-43 新增学生时班级不存在被拒绝")
    void studentSaveRejectsUnknownClass() {
        String token = loginAsAdmin();

        ApiResponse response = post("/api/students", Map.of(
                "studentNo", "T" + System.currentTimeMillis(),
                "name", "测试学生",
                "gender", "男",
                "classId", 999999,
                "enrollYear", 2026), token);

        assertEquals(400, response.code());
        assertEquals("所属班级不存在", response.message());
    }

    @Test
    @DisplayName("TC-API-44 学号重复被拒绝，新增学生自动生成登录账号")
    void studentSaveRejectsDuplicateNo() {
        String token = loginAsAdmin();
        String studentNo = "T" + System.currentTimeMillis();
        Map<String, Object> body = Map.of(
                "studentNo", studentNo,
                "name", "接口测试学生",
                "gender", "女",
                "phone", "13800000000",
                "classId", 1,
                "enrollYear", 2026);

        assertEquals(200, post("/api/students", body, token).code(), "新增学生应成功");
        try {
            ApiResponse duplicated = post("/api/students", body, token);
            assertEquals(400, duplicated.code());
            assertEquals("学号已存在", duplicated.message());

            ApiResponse login = post("/api/auth/login",
                    Map.of("username", studentNo, "password", DEFAULT_PWD), null);
            assertEquals(200, login.code(), "新增学生应自动创建初始密码为 123456 的登录账号");
        } finally {
            ApiResponse page = get("/api/students?pageNum=1&pageSize=50&keyword=" + studentNo, token);
            if (!page.records().isEmpty()) {
                long id = ((Number) page.records().get(0).get("id")).longValue();
                assertEquals(200, delete("/api/students/" + id, token).code(), "清理：删除临时学生");
            }
        }
    }

    @Test
    @DisplayName("TC-API-45 分页参数边界值 pageNum=0、pageSize=-5 被规范为默认值")
    void paginationBoundaryNormalized() {
        String token = loginAsAdmin();

        ApiResponse response = get("/api/leaves/all?pageNum=0&pageSize=-5", token);

        assertEquals(200, response.code(), response.raw());
        assertTrue(response.records().size() <= 10, "非法分页参数应回落为默认每页 10 条");
    }
}
