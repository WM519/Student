package com.leave.dto;

import lombok.Data;

/**
 * 基础分页参数
 */
@Data
public class PageQuery {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    /** 通用关键字（名称/编号搜索） */
    private String keyword;

    public Integer getPageNum() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    public Integer getPageSize() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
