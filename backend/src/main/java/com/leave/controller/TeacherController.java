package com.leave.controller;

import com.leave.common.PageResult;
import com.leave.common.Result;
import com.leave.common.constant.RoleCodes;
import com.leave.dto.PageQuery;
import com.leave.dto.TeacherSaveRequest;
import com.leave.security.SecurityUtil;
import com.leave.service.TeacherService;
import com.leave.vo.TeacherOptionVO;
import com.leave.vo.TeacherRowVO;
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

import java.util.List;

/**
 * 教师管理接口（仅系统管理员）
 */
@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping
    public Result<PageResult<TeacherRowVO>> page(PageQuery query,
                                                 @RequestParam(required = false) Long collegeId) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        return Result.ok(teacherService.page(query, collegeId));
    }

    @GetMapping("/options")
    public Result<List<TeacherOptionVO>> options(@RequestParam(required = false) String roleCode,
                                                 @RequestParam(required = false) Long collegeId) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        return Result.ok(teacherService.options(roleCode, collegeId));
    }

    @PostMapping
    public Result<Void> save(@Valid @RequestBody TeacherSaveRequest request) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        teacherService.save(request);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TeacherSaveRequest request) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        teacherService.update(id, request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        teacherService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status/{status}")
    public Result<Void> toggleStatus(@PathVariable Long id, @PathVariable Integer status) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        teacherService.toggleStatus(id, status);
        return Result.ok();
    }

    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        teacherService.resetPassword(id);
        return Result.ok();
    }
}
