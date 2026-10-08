package com.leave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leave.entity.Role;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RoleMapper extends BaseMapper<Role> {

    @Select("SELECT r.id,r.role_name,r.role_code,r.description,r.status " +
            "FROM t_role r JOIN t_user_role ur ON ur.role_id=r.id " +
            "WHERE ur.user_id=#{userId} AND r.status=1 ORDER BY r.id")
    List<Role> selectRolesByUserId(@Param("userId") Long userId);

    @Select("SELECT r.role_code FROM t_role r JOIN t_user_role ur ON ur.role_id=r.id " +
            "WHERE ur.user_id=#{userId} AND r.status=1 ORDER BY r.id")
    List<String> selectCodesByUserId(@Param("userId") Long userId);
}
