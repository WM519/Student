package com.leave;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 学生请假管理系统启动类
 */
@SpringBootApplication
@MapperScan("com.leave.mapper")
public class LeaveApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaveApplication.class, args);
    }
}
