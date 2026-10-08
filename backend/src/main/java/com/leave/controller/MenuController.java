package com.leave.controller;

import com.leave.common.Result;
import com.leave.common.constant.RoleCodes;
import com.leave.entity.Menu;
import com.leave.security.SecurityUtil;
import com.leave.service.MenuService;
import com.leave.vo.MenuNode;
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
 * 菜单管理接口（仅系统管理员）
 */
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/tree")
    public Result<List<MenuNode>> tree() {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        return Result.ok(menuService.buildTree(menuService.listAll()));
    }

    @GetMapping
    public Result<List<Menu>> list() {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        return Result.ok(menuService.listAll());
    }

    @PostMapping
    public Result<Void> save(@RequestBody Menu menu) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        menuService.save(menu);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Menu menu) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        menu.setId(id);
        menuService.update(menu);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        SecurityUtil.requireRole(RoleCodes.SUPER_ADMIN);
        menuService.delete(id);
        return Result.ok();
    }
}
