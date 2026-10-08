package com.leave.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学院
 */
@Data
@TableName("t_college")
public class College {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 学院名称 */
    private String collegeName;
    /** 学院编码 */
    private String collegeCode;
    /** 院领导（教师 ID） */
    private Long leaderTeacherId;
    /** 备注 */
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
