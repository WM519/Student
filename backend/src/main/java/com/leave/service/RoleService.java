package com.leave.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leave.common.BusinessException;
import com.leave.common.PageResult;
import com.leave.dto.PageQuery;
import com.leave.dto.RoleMenuRequest;
import com.leave.entity.Role;
import com.leave.entity.RoleMenu;
import com.leave.entity.UserRole;
import com.leave.mapper.RoleMapper;
import com.leave.mapper.RoleMenuMapper;
import com.leave.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 角色服务
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final UserRoleMapper userRoleMapper;

    public PageResult<Role> page(PageQuery query) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(Role::getRoleName, query.getKeyword())
                    .or().like(Role::getRoleCode, query.getKeyword());
        }
        wrapper.orderByAsc(Role::getId);
        IPage<Role> page = roleMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    /** 全部启用角色（下拉选择） */
    public List<Role> listAll() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .eq(Role::getStatus, 1).orderByAsc(Role::getId));
    }

    public void save(Role role) {
        checkCodeUnique(role.getRoleCode(), null);
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        roleMapper.insert(role);
    }

    public void update(Role role) {
        if (role.getId() == null) {
            throw new BusinessException("角色 ID 不能为空");
        }
        checkCodeUnique(role.getRoleCode(), role.getId());
        roleMapper.updateById(role);
    }

    @Transactional
    public void delete(Long id) {
        Long users = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, id));
        if (users != null && users > 0) {
            throw new BusinessException("该角色已分配给用户，无法删除");
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, id));
        roleMapper.deleteById(id);
    }

    /** 查询角色已分配菜单 ID */
    public List<Long> roleMenuIds(Long roleId) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenu>()
                        .eq(RoleMenu::getRoleId, roleId))
                .stream().map(RoleMenu::getMenuId).toList();
    }

    @Transactional
    public void assignMenus(RoleMenuRequest request) {
        if (roleMapper.selectById(request.getRoleId()) == null) {
            throw new BusinessException("角色不存在");
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, request.getRoleId()));
        if (request.getMenuIds() != null) {
            for (Long menuId : request.getMenuIds().stream().distinct().toList()) {
                RoleMenu rm = new RoleMenu();
                rm.setRoleId(request.getRoleId());
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            }
        }
    }

    private void checkCodeUnique(String code, Long excludeId) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException("角色编码不能为空");
        }
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, code);
        if (excludeId != null) {
            wrapper.ne(Role::getId, excludeId);
        }
        if (roleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("角色编码已存在");
        }
    }
}
