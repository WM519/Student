package com.leave.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点
 */
@Data
public class MenuNode {
    private Long id;
    private Long parentId;
    private String menuName;
    private String menuType;
    private String path;
    private String component;
    private String icon;
    private Integer sortOrder;
    private Integer visible;
    private List<MenuNode> children = new ArrayList<>();
}
