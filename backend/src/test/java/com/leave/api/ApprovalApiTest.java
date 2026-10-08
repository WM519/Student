package com.leave.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 模块三：三级审批中心接口测试。
 *
 * <p>被测审批链（stu04 刘婷婷 → 软工2401 → 计算机学院…外国语学院）：
 * 班主任 head02（赵敏）、辅导员 coun02（陈芳）、院领导 leader02（钱学文）；
 * 其他班级班主任 head01 用于越权反向用例。</p>
 */
@DisplayName("接口测试-三级审批中心模块")
class ApprovalApiTest extends ApiTestBase {

    private static final String STU = "stu04";
    private static final String HEAD = "head02";
    private static final String COUN = "coun02";
    private static final String LEADER = "leader02";

    private long apply(String token, LocalDate start, int days) {
        ApiResponse response = post("/api/leaves/apply",
                leaveBody("事假", start.toString(), start.plusDays(days - 1L).toString(),
                        "[自动化测试] 审批链用例"), token);
        assertEquals(200, response.code(), response.raw());
        return ((Number) response.data().get("id")).longValue();
    }

    private Map<String, Object> detailLeave(long id) {
        ApiResponse detail = get("/api/leaves/" + id + "/detail", loginAsAdmin());
        assertEquals(200, detail.code(), detail.raw());
        return (Map<String, Object>) detail.data().get("leave");
    }

    @Test
    @DisplayName("TC-API-23 5 天请假：班主任审批后流转到辅导员环节")
    void headApprovalMovesToCounselor() {
        String stuToken = login(STU, DEFAULT_PWD);
        long id = apply(stuToken, LocalDate.now().plusDays(200), 5);

        assertEquals(200, post("/api/leaves/" + id + "/approve",
                Map.of("comment", "同意，注意安全"), login(HEAD, DEFAULT_PWD)).code());

        Map<String, Object> leave = detailLeave(id);
        assertEquals("PENDING_COUNSELOR", String.valueOf(leave.get("status")));
        assertEquals(2, ((Number) leave.get("currentLevel")).intValue());
    }

    @Test
    @DisplayName("TC-API-24 9 天请假：班主任、辅导员、院领导三级全部通过后为准假")
    void threeLevelChainApproves() {
        String stuToken = login(STU, DEFAULT_PWD);
        long id = apply(stuToken, LocalDate.now().plusDays(210), 9);

        assertEquals(200, post("/api/leaves/" + id + "/approve", Map.of("comment", "班主任同意"), login(HEAD, DEFAULT_PWD)).code());
        assertEquals(200, post("/api/leaves/" + id + "/approve", Map.of("comment", "辅导员同意"), login(COUN, DEFAULT_PWD)).code());
        assertEquals(200, post("/api/leaves/" + id + "/approve", Map.of("comment", "院领导同意"), login(LEADER, DEFAULT_PWD)).code());

        Map<String, Object> leave = detailLeave(id);
        assertEquals("APPROVED", String.valueOf(leave.get("status")));

        ApiResponse audits = get("/api/leaves/" + id + "/detail", loginAsAdmin());
        Object auditList = audits.data().get("audits");
        assertTrue(auditList instanceof java.util.List<?> list && list.size() >= 4,
                "应记录提交 + 三次审批共 4 条流转记录");
    }

    @Test
    @DisplayName("TC-API-25 9 天请假：辅导员审批后流转到院领导环节")
    void counselorApprovalMovesToLeader() {
        String stuToken = login(STU, DEFAULT_PWD);
        long id = apply(stuToken, LocalDate.now().plusDays(220), 9);

        post("/api/leaves/" + id + "/approve", Map.of("comment", "同意"), login(HEAD, DEFAULT_PWD));
        assertEquals(200, post("/api/leaves/" + id + "/approve",
                Map.of("comment", "同意"), login(COUN, DEFAULT_PWD)).code());

        Map<String, Object> leave = detailLeave(id);
        assertEquals("PENDING_LEADER", String.valueOf(leave.get("status")));
        assertEquals(3, ((Number) leave.get("currentLevel")).intValue());
    }

    @Test
    @DisplayName("TC-API-26 非本班班主任审批被拒绝（越权审批）")
    void otherClassHeadCannotApprove() {
        String stuToken = login(STU, DEFAULT_PWD);
        long id = apply(stuToken, LocalDate.now().plusDays(230), 2);

        ApiResponse response = post("/api/leaves/" + id + "/approve",
                Map.of("comment", "越权审批"), login("head01", DEFAULT_PWD));

        assertEquals(400, response.code());
        assertEquals("您不是该学生的班主任，无权处理此申请", response.message());
    }

    @Test
    @DisplayName("TC-API-27 学生账号调用审批接口被拒绝")
    void studentCannotApprove() {
        String stuToken = login(STU, DEFAULT_PWD);
        long id = apply(stuToken, LocalDate.now().plusDays(235), 1);

        ApiResponse response = post("/api/leaves/" + id + "/approve", Map.of("comment", "学生审批"), stuToken);

        assertEquals(403, response.code());
        assertEquals("仅教师账号可操作审批", response.message());
    }

    @Test
    @DisplayName("TC-API-28 审批环节参数非法（level=4）被拒绝")
    void invalidApprovalLevelRejected() {
        String token = login(HEAD, DEFAULT_PWD);

        ApiResponse response = get("/api/leaves/approval?level=4&done=false&pageNum=1&pageSize=10", token);

        assertEquals(400, response.code());
        assertEquals("审批环节参数错误", response.message());
    }

    @Test
    @DisplayName("TC-API-29 已办结的请假重复审批被拒绝")
    void duplicateApprovalRejected() {
        String stuToken = login(STU, DEFAULT_PWD);
        long id = apply(stuToken, LocalDate.now().plusDays(240), 1);
        String headToken = login(HEAD, DEFAULT_PWD);

        assertEquals(200, post("/api/leaves/" + id + "/approve", Map.of("comment", "同意"), headToken).code());
        ApiResponse again = post("/api/leaves/" + id + "/approve", Map.of("comment", "重复审批"), headToken);

        assertEquals(400, again.code());
        assertEquals("该请假已处理完毕，无需重复审批", again.message());
    }

    @Test
    @DisplayName("TC-API-30 班主任驳回后审批链终止，后续环节无法继续审批")
    void rejectTerminatesChain() {
        String stuToken = login(STU, DEFAULT_PWD);
        long id = apply(stuToken, LocalDate.now().plusDays(250), 9);

        assertEquals(200, post("/api/leaves/" + id + "/reject",
                Map.of("comment", "材料不齐，驳回"), login(HEAD, DEFAULT_PWD)).code());
        Map<String, Object> leave = detailLeave(id);
        assertEquals("REJECTED", String.valueOf(leave.get("status")));

        ApiResponse after = post("/api/leaves/" + id + "/approve",
                Map.of("comment", "继续审批"), login(COUN, DEFAULT_PWD));
        assertEquals(400, after.code());
        assertEquals("该请假已处理完毕，无需重复审批", after.message());
    }

    @Test
    @Disabled("DEF-001：已确认缺陷，修复后启用。审批/驳回/撤销结束后 t_leave.current_level 未被置空（MyBatis-Plus updateById 默认忽略 null 字段）")
    @DisplayName("DEF-001 审批结束后当前审批环节应被清空（当前实现不通过）")
    void defectCurrentLevelShouldBeClearedAfterFinish() {
        String stuToken = login(STU, DEFAULT_PWD);
        long id = apply(stuToken, LocalDate.now().plusDays(300), 9);
        post("/api/leaves/" + id + "/approve", Map.of("comment", "同意"), login(HEAD, DEFAULT_PWD));
        post("/api/leaves/" + id + "/approve", Map.of("comment", "同意"), login(COUN, DEFAULT_PWD));
        post("/api/leaves/" + id + "/approve", Map.of("comment", "同意"), login(LEADER, DEFAULT_PWD));

        Map<String, Object> leave = detailLeave(id);
        assertEquals("APPROVED", String.valueOf(leave.get("status")));
        assertNull(leave.get("currentLevel"), "已办结的请假不应再残留当前审批环节");
    }

    @Test
    @DisplayName("TC-API-30b 缺陷复现：审批结束后 current_level 残留（DEF-001 现状取证）")
    void defectCurrentLevelRemainsAfterFinish() {
        String stuToken = login(STU, DEFAULT_PWD);
        long id = apply(stuToken, LocalDate.now().plusDays(320), 9);
        post("/api/leaves/" + id + "/approve", Map.of("comment", "同意"), login(HEAD, DEFAULT_PWD));
        post("/api/leaves/" + id + "/approve", Map.of("comment", "同意"), login(COUN, DEFAULT_PWD));
        post("/api/leaves/" + id + "/approve", Map.of("comment", "同意"), login(LEADER, DEFAULT_PWD));

        Map<String, Object> leave = detailLeave(id);
        assertEquals("APPROVED", String.valueOf(leave.get("status")));
        // 现状：current_level 仍为最后审批环节（3），与“已办结”状态语义不一致 —— 登记为缺陷 DEF-001
        assertEquals(3, ((Number) leave.get("currentLevel")).intValue());
    }

    @Test
    @DisplayName("TC-API-31 已通过且已开始的请假可销假")
    void approvedAndStartedLeaveCanBeCancelled() {
        String stuToken = login(STU, DEFAULT_PWD);
        LocalDate today = LocalDate.now();
        long id = apply(stuToken, today, 1);
        assertEquals(200, post("/api/leaves/" + id + "/approve",
                Map.of("comment", "同意"), login(HEAD, DEFAULT_PWD)).code());
        assertEquals("APPROVED", String.valueOf(detailLeave(id).get("status")));

        ApiResponse cancel = post("/api/leaves/" + id + "/cancel", Map.of(), stuToken);

        assertEquals(200, cancel.code(), cancel.raw());
        Map<String, Object> leave = detailLeave(id);
        assertEquals("CANCELLED", String.valueOf(leave.get("status")));
        assertNotNull(leave.get("actualReturnTime"), "销假后应记录实际返校时间");
    }

    @Test
    @DisplayName("TC-API-32 已通过但尚未开始的请假不能销假")
    void notStartedLeaveCannotBeCancelled() {
        String stuToken = login(STU, DEFAULT_PWD);
        long id = apply(stuToken, LocalDate.now().plusDays(260), 9);
        post("/api/leaves/" + id + "/approve", Map.of("comment", "同意"), login(HEAD, DEFAULT_PWD));
        post("/api/leaves/" + id + "/approve", Map.of("comment", "同意"), login(COUN, DEFAULT_PWD));
        post("/api/leaves/" + id + "/approve", Map.of("comment", "同意"), login(LEADER, DEFAULT_PWD));

        ApiResponse cancel = post("/api/leaves/" + id + "/cancel", Map.of(), stuToken);

        assertEquals(400, cancel.code());
        assertEquals("请假尚未开始，暂时不能销假", cancel.message());
    }

    @Test
    @DisplayName("TC-API-33 学生不能对他人的请假销假")
    void studentCannotCancelOthersLeave() {
        String stuToken = login(STU, DEFAULT_PWD);

        ApiResponse response = post("/api/leaves/2/cancel", Map.of(), stuToken);

        assertEquals(400, response.code());
        assertEquals("请假记录不存在", response.message());
    }

    @Test
    @DisplayName("TC-API-34 辅导员访问班主任环节列表被拒绝（环节越权）")
    void counselorCannotQueryHeadLevelList() {
        String token = login(COUN, DEFAULT_PWD);

        ApiResponse response = get("/api/leaves/approval?level=1&done=false&pageNum=1&pageSize=10", token);

        assertEquals(403, response.code());
        assertEquals("无权限执行该操作", response.message());
    }

    @Test
    @DisplayName("TC-API-35 班主任可查询本人待办审批列表")
    void headCanQueryPendingList() {
        String token = login(HEAD, DEFAULT_PWD);

        ApiResponse response = get("/api/leaves/approval?level=1&done=false&pageNum=1&pageSize=10", token);

        assertEquals(200, response.code());
        assertTrue(response.total() >= 0);
    }
}
