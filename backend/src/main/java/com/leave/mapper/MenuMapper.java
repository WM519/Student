package com.leave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leave.entity.Menu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MenuMapper extends BaseMapper<Menu> {

    /** 查询某用户可见的全部菜单（按角色过滤） */
    @Select("SELECT DISTINCT m.id,m.parent_id,m.menu_name,m.menu_type,m.path,m.component,m.icon,m.sort_order,m.visible,m.create_time,m.update_time " +
            "FROM t_menu m JOIN t_role_menu rm ON rm.menu_id=m.id " +
            "JOIN t_user_role ur ON ur.role_id=rm.role_id " +
            "WHERE ur.user_id=#{userId} AND m.visible=1 ORDER BY m.sort_order")
    List<Menu> selectMenusByUserId(@Param("userId") Long userId);
}
