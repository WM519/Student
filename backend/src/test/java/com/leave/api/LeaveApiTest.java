package com.leave.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 模块二：请假申请与“我的请假”接口测试（被测学生：stu04 刘婷婷，班级 软工2401）。
 * 说明：为避免与其他用例的请假区间冲突，本类统一使用相对今天 +60 天以后的互不重叠时间窗。
 */
@DisplayName("接口测试-请假申请模块")
class LeaveApiTest extends ApiTestBase {

    private static final String STU = "stu04";

    @Test
    @DisplayName("TC-API-10 学生提交单日请假成功并进入待班主任审批")
    void applySingleDayLeaveSuccessfully() {
        String token = login(STU, DEFAULT_PWD);
        LocalDate day = LocalDate.now().plusDays(60);

        ApiResponse response = post("/api/leaves/apply",
                leaveBody("事假", day.toString(), day.toString(), "[自动化测试] 单日请假"), token);

        assertEquals(200, response.code(), response.raw());
        assertEquals("PENDING_HEAD", String.valueOf(response.data().get("status")));
        assertEquals(1, ((Number) response.data().get("currentLevel")).intValue());
        assertEquals(1, ((Number) response.data().get("days")).intValue());
        assertTrue(String.valueOf(response.data().get("leaveNo")).startsWith("LV"), "请假单号应以 LV 开头");
    }

    @Test
    @DisplayName("TC-API-11 跨 7 天以上的请假天数计算正确（进入三级审批）")
    void applyNineDayLeaveCalculatesDays() {
        String token = login(STU, DEFAULT_PWD);
        LocalDate start = LocalDate.now().plusDays(70);
        LocalDate end = start.plusDays(8);

        ApiResponse response = post("/api/leaves/apply",
                leaveBody("病假", start.toString(), end.toString(), "[自动化测试] 九天病假"), token);

        assertEquals(200, response.code(), response.raw());
        assertEquals(9, ((Number) response.data().get("days")).intValue(), "请假天数应为 9 天");
        assertEquals("PENDING_HEAD", String.valueOf(response.data().get("status")));
        assertEquals(1, ((Number) response.data().get("currentLevel")).intValue(), "审批链首环节为班主任");
    }

    @Test
    @DisplayName("TC-API-12 结束日期早于开始日期被拒绝")
    void applyFailsWhenEndBeforeStart() {
        String token = login(STU, DEFAULT_PWD);
        LocalDate start = LocalDate.now().plusDays(120);

        ApiResponse response = post("/api/leaves/apply",
                leaveBody("事假", start.toString(), start.minusDays(1).toString(), "[自动化测试] 非法日期"), token);

        assertEquals(400, response.code());
        assertEquals("结束日期不能早于开始日期", response.message());
    }

    @Test
    @DisplayName("TC-API-13 开始日期早于今天被拒绝（边界值）")
    void applyFailsWhenStartBeforeToday() {
        String token = login(STU, DEFAULT_PWD);
        LocalDate today = LocalDate.now();

        ApiResponse response = post("/api/leaves/apply",
                leaveBody("事假", today.minusDays(1).toString(), today.toString(), "[自动化测试] 过去日期"), token);

        assertEquals(400, response.code());
        assertEquals("请假开始日期不能早于今天", response.message());
    }

    @Test
    @DisplayName("TC-API-14 请假类型非法被拒绝")
    void applyFailsWhenLeaveTypeIllegal() {
        String token = login(STU, DEFAULT_PWD);
        LocalDate day = LocalDate.now().plusDays(130);

        ApiResponse response = post("/api/leaves/apply",
                leaveBody("年假", day.toString(), day.toString(), "[自动化测试] 非法类型"), token);

        assertEquals(400, response.code());
        assertEquals("请选择合法的请假类型", response.message());
    }

    @Test
    @DisplayName("TC-API-15 请假事由为空触发参数校验")
    void applyFailsWhenReasonBlank() {
        String token = login(STU, DEFAULT_PWD);
        LocalDate day = LocalDate.now().plusDays(132);

        ApiResponse response = post("/api/leaves/apply",
                leaveBody("事假", day.toString(), day.toString(), "   "), token);

        assertEquals(400, response.code());
        assertEquals("请假事由不能为空", response.message());
    }

    @Test
    @DisplayName("TC-API-16 开始日期为空触发参数校验")
    void applyFailsWhenStartDateNull() {
        String token = login(STU, DEFAULT_PWD);
        LocalDate day = LocalDate.now().plusDays(134);

        Map<String, Object> body = leaveBody("事假", null, day.toString(), "[自动化测试] 缺开始日期");
        body.put("startDate", null);
        ApiResponse response = post("/api/leaves/apply", body, token);

        assertEquals(400, response.code());
        assertEquals("开始日期不能为空", response.message());
    }

    @Test
    @DisplayName("TC-API-17 同一学生请假时间段重叠被拒绝")
    void applyFailsWhenPeriodOverlaps() {
        String token = login(STU, DEFAULT_PWD);
        LocalDate start = LocalDate.now().plusDays(140);

        ApiResponse first = post("/api/leaves/apply",
                leaveBody("事假", start.toString(), start.plusDays(1).toString(), "[自动化测试] 重叠基准单"), token);
        assertEquals(200, first.code(), first.raw());

        ApiResponse second = post("/api/leaves/apply",
                leaveBody("事假", start.plusDays(1).toString(), start.plusDays(2).toString(), "[自动化测试] 重叠单"), token);

        assertEquals(400, second.code());
        assertTrue(String.valueOf(second.message()).contains("时间段重叠"), "应提示时间段重叠：" + second.message());
    }

    @Test
    @DisplayName("TC-API-18 学生可撤销待班主任审批的申请，重复撤销被拒绝")
    void withdrawPendingLeave() {
        String token = login(STU, DEFAULT_PWD);
        LocalDate day = LocalDate.now().plusDays(150);

        ApiResponse applied = post("/api/leaves/apply",
                leaveBody("其他", day.toString(), day.toString(), "[自动化测试] 待撤销单"), token);
        long id = ((Number) applied.data().get("id")).longValue();

        assertEquals(200, post("/api/leaves/" + id + "/withdraw", Map.of(), token).code());
        ApiResponse detail = get("/api/leaves/" + id + "/detail", token);
        assertEquals("WITHDRAWN", String.valueOf(detail.data().get("leave") instanceof Map<?, ?> m
                ? m.get("status") : null), "撤销后状态应为 WITHDRAWN");

        ApiResponse again = post("/api/leaves/" + id + "/withdraw", Map.of(), token);
        assertEquals(400, again.code());
        assertTrue(String.valueOf(again.message()).contains("不允许撤销"), "重复撤销应被拒绝：" + again.message());
    }

    @Test
    @DisplayName("TC-API-19 学生不能查看他人请假详情（越权访问）")
    void cannotViewOthersLeaveDetail() {
        String token = login(STU, DEFAULT_PWD);

        ApiResponse response = get("/api/leaves/1/detail", token);

        assertEquals(403, response.code());
        assertEquals("无权查看他人的请假记录", response.message());
    }

    @Test
    @DisplayName("TC-API-20 我的请假列表仅返回本人记录")
    void myLeaveListOnlyContainsOwnRecords() {
        String token = login(STU, DEFAULT_PWD);

        ApiResponse response = get("/api/leaves/my?pageNum=1&pageSize=50", token);

        assertEquals(200, response.code());
        assertTrue(response.total() > 0, "本人应至少有一条请假记录");
        List<Map<String, Object>> records = response.records();
        for (Map<String, Object> row : records) {
            assertEquals("刘婷婷", String.valueOf(row.get("studentName")), "不得返回他人记录：" + row);
        }
    }

    @Test
    @DisplayName("TC-API-21 教师账号调用学生申请接口被拒绝（越权）")
    void teacherCannotApplyLeave() {
        String token = login("head02", DEFAULT_PWD);
        LocalDate day = LocalDate.now().plusDays(160);

        ApiResponse response = post("/api/leaves/apply",
                leaveBody("事假", day.toString(), day.toString(), "[自动化测试] 教师越权申请"), token);

        assertEquals(403, response.code());
        assertEquals("无权限执行该操作", response.message());
    }

    @Test
    @DisplayName("TC-API-22 查询不存在的请假详情返回业务错误")
    void detailOfUnknownLeaveFails() {
        String token = loginAsAdmin();

        ApiResponse response = get("/api/leaves/99999999/detail", token);

        assertEquals(400, response.code());
        assertEquals("请假记录不存在", response.message());
        assertNotEquals(500, response.code());
    }
}
