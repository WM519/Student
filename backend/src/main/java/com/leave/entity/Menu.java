package com.leave.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜单
 */
@Data
@TableName("t_menu")
public class Menu {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 父菜单 ID，0 为根 */
    private Long parentId;
    private String menuName;
    /** DIR 目录 / MENU 菜单 */
    private String menuType;
    /** 前端路由路径 */
    private String path;
    /** 前端组件路径（相对 src/views） */
    private String component;
    /** Element Plus 图标名 */
    private String icon;
    /** 排序号 */
    private Integer sortOrder;
    /** 1 显示 0 隐藏 */
    private Integer visible;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
