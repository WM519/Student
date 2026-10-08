package com.leave.controller;

import com.leave.common.PageResult;
import com.leave.common.Result;
import com.leave.common.constant.RoleCodes;
import com.leave.dto.PageQuery;
import com.leave.entity.College;
import com.leave.security.SecurityUtil;
import com.leave.service.CollegeService;
import com.leave.vo.CollegeRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学院管理接口（仅系统管理员）
 */
@RestController
@RequestMapping("/api/colleges")
@RequiredArgsConstructor
public class CollegeController {

    private final CollegeService collegeService;

    @GetMapping
    public Result<PageResult<CollegeRowVO>> page(PageQuery query) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        return Result.ok(collegeService.page(query));
    }

    @GetMapping("/all")
    public Result<List<College>> all() {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        return Result.ok(collegeService.listAll());
    }

    @PostMapping
    public Result<Void> save(@RequestBody College college) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        collegeService.save(college);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody College college) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        college.setId(id);
        collegeService.update(college);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        collegeService.delete(id);
        return Result.ok();
    }
}
