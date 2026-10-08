package com.leave.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录用户（管理员/教师/学生统一账号）
 */
@Data
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    /** BCrypt 加密后的密码 */
    private String password;
    private String realName;
    /** 用户类型：ADMIN/TEACHER/STUDENT */
    private String userType;
    /** 关联教师/学生表 ID（管理员为空） */
    private Long refId;
    /** 1 启用 0 停用 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
