package com.leave.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leave.common.BusinessException;
import com.leave.entity.Menu;
import com.leave.mapper.MenuMapper;
import com.leave.mapper.RoleMenuMapper;
import com.leave.vo.MenuNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单服务（含菜单树构建）
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;

    /** 全部菜单（菜单管理页面用） */
    public List<Menu> listAll() {
        return menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                .orderByAsc(Menu::getSortOrder).orderByAsc(Menu::getId));
    }

    public List<MenuNode> buildTree(List<Menu> menus) {
        Map<Long, MenuNode> map = new LinkedHashMap<>();
        for (Menu m : menus) {
            MenuNode node = new MenuNode();
            node.setId(m.getId());
            node.setParentId(m.getParentId());
            node.setMenuName(m.getMenuName());
            node.setMenuType(m.getMenuType());
            node.setPath(m.getPath());
            node.setComponent(m.getComponent());
            node.setIcon(m.getIcon());
            node.setSortOrder(m.getSortOrder());
            node.setVisible(m.getVisible());
            map.put(m.getId(), node);
        }
        List<MenuNode> roots = new ArrayList<>();
        for (MenuNode node : map.values()) {
            if (node.getParentId() != null && node.getParentId() != 0 && map.containsKey(node.getParentId())) {
                map.get(node.getParentId()).getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    public void save(Menu menu) {
        validate(menu);
        menuMapper.insert(menu);
    }

    public void update(Menu menu) {
        if (menu.getId() == null) {
            throw new BusinessException("菜单 ID 不能为空");
        }
        validate(menu);
        menuMapper.updateById(menu);
    }

    public void delete(Long id) {
        Long children = menuMapper.selectCount(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getParentId, id));
        if (children != null && children > 0) {
            throw new BusinessException("请先删除该目录下的子菜单");
        }
        Long bind = roleMenuMapper.selectCount(new LambdaQueryWrapper<com.leave.entity.RoleMenu>()
                .eq(com.leave.entity.RoleMenu::getMenuId, id));
        if (bind != null && bind > 0) {
            throw new BusinessException("该菜单已分配给角色，无法删除");
        }
        menuMapper.deleteById(id);
    }

    private void validate(Menu menu) {
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getParentId() != 0 && menuMapper.selectById(menu.getParentId()) == null) {
            throw new BusinessException("父菜单不存在");
        }
        if (menu.getVisible() == null) {
            menu.setVisible(1);
        }
        if (menu.getSortOrder() == null) {
            menu.setSortOrder(0);
        }
    }
}
