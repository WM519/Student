package com.leave.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师
 */
@Data
@TableName("t_teacher")
public class Teacher {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 工号 */
    private String teacherNo;
    /** 姓名 */
    private String name;
    /** 性别 M/F */
    private String gender;
    private String phone;
    private String email;
    /** 所属学院 ID */
    private Long collegeId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
