package com.leave.controller;

import com.leave.common.PageResult;
import com.leave.common.Result;
import com.leave.common.constant.RoleCodes;
import com.leave.dto.PageQuery;
import com.leave.entity.Clazz;
import com.leave.security.SecurityUtil;
import com.leave.service.ClassService;
import com.leave.vo.ClassRowVO;
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
 * 班级管理接口（仅系统管理员）
 */
@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @GetMapping
    public Result<PageResult<ClassRowVO>> page(PageQuery query,
                                               @RequestParam(required = false) Long collegeId) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        return Result.ok(classService.page(query, collegeId));
    }

    @GetMapping("/all")
    public Result<List<Clazz>> all(@RequestParam(required = false) Long collegeId) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        return Result.ok(classService.listAll(collegeId));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Clazz clazz) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        classService.save(clazz);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Clazz clazz) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        clazz.setId(id);
        classService.update(clazz);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        classService.delete(id);
        return Result.ok();
    }
}
