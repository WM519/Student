package com.leave.controller;

import com.leave.common.PageResult;
import com.leave.common.Result;
import com.leave.common.constant.RoleCodes;
import com.leave.dto.PageQuery;
import com.leave.dto.StudentSaveRequest;
import com.leave.security.SecurityUtil;
import com.leave.service.StudentService;
import com.leave.vo.StudentRowVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生管理接口（仅系统管理员）
 */
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    public Result<PageResult<StudentRowVO>> page(PageQuery query,
                                                 @RequestParam(required = false) Long classId) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        return Result.ok(studentService.page(query, classId));
    }

    @PostMapping
    public Result<Void> save(@Valid @RequestBody StudentSaveRequest request) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        studentService.save(request);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody StudentSaveRequest request) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        studentService.update(id, request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        studentService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status/{status}")
    public Result<Void> toggleStatus(@PathVariable Long id, @PathVariable Integer status) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        studentService.toggleStatus(id, status);
        return Result.ok();
    }

    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        studentService.resetPassword(id);
        return Result.ok();
    }
}
