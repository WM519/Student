package com.leave.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生
 */
@Data
@TableName("t_student")
public class Student {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 学号 */
    private String studentNo;
    /** 姓名 */
    private String name;
    /** 性别 M/F */
    private String gender;
    private String phone;
    private String email;
    /** 所属班级 ID */
    private Long classId;
    /** 入学年份 */
    private Integer enrollYear;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
