package com.leave.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级
 */
@Data
@TableName("t_class")
public class Clazz {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 班级名称 */
    private String className;
    /** 班级编码 */
    private String classCode;
    /** 所属学院 ID */
    private Long collegeId;
    /** 班主任教师 ID */
    private Long headTeacherId;
    /** 辅导员教师 ID */
    private Long counselorTeacherId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
