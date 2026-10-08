package com.leave.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 名称-数值（图表/统计项）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NameValueVO {
    private String name;
    private Long value;
}
