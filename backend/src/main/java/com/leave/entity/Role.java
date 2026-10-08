package com.leave.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色
 */
@Data
@TableName("t_role")
public class Role {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roleName;
    /** 角色编码，如 super_admin */
    private String roleCode;
    private String description;
    /** 1 启用 0 停用 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
