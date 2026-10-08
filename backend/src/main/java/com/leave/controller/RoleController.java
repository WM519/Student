package com.leave.controller;

import com.leave.common.PageResult;
import com.leave.common.Result;
import com.leave.common.constant.RoleCodes;
import com.leave.dto.PageQuery;
import com.leave.dto.RoleMenuRequest;
import com.leave.entity.Role;
import com.leave.security.SecurityUtil;
import com.leave.service.RoleService;
import jakarta.validation.Valid;
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
 * 角色管理接口（仅系统管理员）
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public Result<PageResult<Role>> page(PageQuery query) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        return Result.ok(roleService.page(query));
    }

    @GetMapping("/all")
    public Result<List<Role>> all() {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        return Result.ok(roleService.listAll());
    }

    @PostMapping
    public Result<Void> save(@RequestBody Role role) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        roleService.save(role);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Role role) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        role.setId(id);
        roleService.update(role);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        roleService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}/menus")
    public Result<List<Long>> roleMenus(@PathVariable Long id) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        return Result.ok(roleService.roleMenuIds(id));
    }

    @PutMapping("/{id}/menus")
    public Result<Void> assignMenus(@PathVariable Long id, @Valid @RequestBody RoleMenuRequest request) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        request.setRoleId(id);
        roleService.assignMenus(request);
        return Result.ok();
    }
}
