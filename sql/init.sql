-- =====================================================================
-- 学生请假管理系统 数据库初始化脚本
-- 适用：MySQL 8.x
-- 说明：脚本会删除并重建 student_leave 库，请勿在正式数据上直接执行
-- =====================================================================
DROP DATABASE IF EXISTS student_leave;
CREATE DATABASE student_leave DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE student_leave;

-- ----------------------------
-- 1. 学院表
-- ----------------------------
DROP TABLE IF EXISTS t_college;
CREATE TABLE t_college (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    college_name      VARCHAR(100) NOT NULL COMMENT '学院名称',
    college_code      VARCHAR(50)  NOT NULL COMMENT '学院编码',
    leader_teacher_id BIGINT       DEFAULT NULL COMMENT '院领导（教师ID）',
    description       VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_college_code (college_code)
) ENGINE = InnoDB COMMENT = '学院';

-- ----------------------------
-- 2. 教师表
-- ----------------------------
DROP TABLE IF EXISTS t_teacher;
CREATE TABLE t_teacher (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    teacher_no  VARCHAR(50)  NOT NULL COMMENT '工号',
    name        VARCHAR(50)  NOT NULL COMMENT '姓名',
    gender      CHAR(1)      DEFAULT NULL COMMENT '性别 M/F',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    college_id  BIGINT       DEFAULT NULL COMMENT '所属学院ID',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_teacher_no (teacher_no),
    KEY idx_teacher_college (college_id)
) ENGINE = InnoDB COMMENT = '教师';

-- ----------------------------
-- 3. 班级表
-- ----------------------------
DROP TABLE IF EXISTS t_class;
CREATE TABLE t_class (
    id                    BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    class_name            VARCHAR(100) NOT NULL COMMENT '班级名称',
    class_code            VARCHAR(50) NOT NULL COMMENT '班级编码',
    college_id            BIGINT      NOT NULL COMMENT '所属学院ID',
    head_teacher_id       BIGINT      DEFAULT NULL COMMENT '班主任教师ID',
    counselor_teacher_id  BIGINT      DEFAULT NULL COMMENT '辅导员教师ID',
    create_time           DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time           DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_class_code (class_code),
    KEY idx_class_college (college_id)
) ENGINE = InnoDB COMMENT = '班级';

-- ----------------------------
-- 4. 学生表
-- ----------------------------
DROP TABLE IF EXISTS t_student;
CREATE TABLE t_student (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    student_no  VARCHAR(50)  NOT NULL COMMENT '学号',
    name        VARCHAR(50)  NOT NULL COMMENT '姓名',
    gender      CHAR(1)      DEFAULT NULL COMMENT '性别 M/F',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    class_id    BIGINT       DEFAULT NULL COMMENT '所属班级ID',
    enroll_year INT          DEFAULT NULL COMMENT '入学年份',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_no (student_no),
    KEY idx_student_class (class_id)
) ENGINE = InnoDB COMMENT = '学生';

-- ----------------------------
-- 5. 登录用户表（管理员/教师/学生统一账号）
-- ----------------------------
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50) NOT NULL COMMENT '登录用户名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
    real_name   VARCHAR(50) NOT NULL COMMENT '真实姓名',
    user_type   VARCHAR(20) NOT NULL COMMENT 'ADMIN/TEACHER/STUDENT',
    ref_id      BIGINT      DEFAULT NULL COMMENT '关联教师/学生ID',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_user_ref (user_type, ref_id)
) ENGINE = InnoDB COMMENT = '登录用户';

-- ----------------------------
-- 6. 角色表
-- ----------------------------
DROP TABLE IF EXISTS t_role;
CREATE TABLE t_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色名称',
    role_code   VARCHAR(50)  NOT NULL COMMENT '角色编码',
    description VARCHAR(200) DEFAULT NULL COMMENT '描述',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE = InnoDB COMMENT = '角色';

-- ----------------------------
-- 7. 用户-角色关联表
-- ----------------------------
DROP TABLE IF EXISTS t_user_role;
CREATE TABLE t_user_role (
    id      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE = InnoDB COMMENT = '用户角色关联';

-- ----------------------------
-- 8. 菜单表
-- ----------------------------
DROP TABLE IF EXISTS t_menu;
CREATE TABLE t_menu (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单ID，0为根',
    menu_name   VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    menu_type   VARCHAR(10)  NOT NULL COMMENT 'DIR目录/MENU菜单',
    path        VARCHAR(200) DEFAULT NULL COMMENT '前端路由路径',
    component   VARCHAR(200) DEFAULT NULL COMMENT '前端组件路径（相对 src/views）',
    icon        VARCHAR(100) DEFAULT NULL COMMENT '图标名',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序',
    visible     TINYINT      NOT NULL DEFAULT 1 COMMENT '1显示 0隐藏',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_menu_parent (parent_id)
) ENGINE = InnoDB COMMENT = '菜单';

-- ----------------------------
-- 9. 角色-菜单关联表
-- ----------------------------
DROP TABLE IF EXISTS t_role_menu;
CREATE TABLE t_role_menu (
    id      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE = InnoDB COMMENT = '角色菜单关联';

-- ----------------------------
-- 10. 请假表
-- ----------------------------
DROP TABLE IF EXISTS t_leave;
CREATE TABLE t_leave (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    leave_no          VARCHAR(50)  NOT NULL COMMENT '请假单号',
    student_id        BIGINT       NOT NULL COMMENT '学生ID',
    leave_type        VARCHAR(20)  NOT NULL COMMENT '请假类型：事假/病假/其他',
    start_date        DATE         NOT NULL COMMENT '开始日期',
    end_date          DATE         NOT NULL COMMENT '结束日期',
    days              INT          NOT NULL COMMENT '请假天数',
    reason            VARCHAR(500) DEFAULT NULL COMMENT '请假事由',
    status            VARCHAR(32)  NOT NULL COMMENT '状态',
    current_level     TINYINT      DEFAULT NULL COMMENT '当前待审批环节 1/2/3',
    apply_time        DATETIME     DEFAULT NULL COMMENT '申请时间',
    actual_return_time DATETIME    DEFAULT NULL COMMENT '实际销假时间',
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_leave_no (leave_no),
    KEY idx_leave_student (student_id),
    KEY idx_leave_status (status)
) ENGINE = InnoDB COMMENT = '请假申请';

-- ----------------------------
-- 11. 请假审批流水表
-- ----------------------------
DROP TABLE IF EXISTS t_leave_audit;
CREATE TABLE t_leave_audit (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    leave_id        BIGINT       NOT NULL COMMENT '请假ID',
    level           TINYINT      DEFAULT NULL COMMENT '环节：0提交/1班主任/2辅导员/3院领导/4销假',
    operator_user_id BIGINT      DEFAULT NULL COMMENT '操作人用户ID',
    operator_name   VARCHAR(50)  DEFAULT NULL COMMENT '操作人姓名',
    action          VARCHAR(16)  NOT NULL COMMENT 'SUBMIT/APPROVE/REJECT/WITHDRAW/CANCEL',
    comment         VARCHAR(500) DEFAULT NULL COMMENT '意见',
    audit_time      DATETIME     DEFAULT NULL COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_audit_leave (leave_id)
) ENGINE = InnoDB COMMENT = '请假审批流水';

-- =====================================================================
-- 初始化数据
-- =====================================================================

-- 学院（院领导教师稍后回填）
INSERT INTO t_college (id, college_name, college_code, leader_teacher_id, description) VALUES
(1, '计算机学院', 'JSJ001', 3, '计算机科学与技术、软件工程等专业'),
(2, '外国语学院', 'WGY002', 6, '英语、日语等专业');

-- 教师
INSERT INTO t_teacher (id, teacher_no, name, gender, phone, email, college_id) VALUES
(1, 'T2020001', '张建国', 'M', '13800000001', 'zhangjg@school.edu.cn', 1),
(2, 'T2020002', '李慧',   'F', '13800000002', 'lihui@school.edu.cn', 1),
(3, 'T2020003', '王长明', 'M', '13800000003', 'wangcm@school.edu.cn', 1),
(4, 'T2020004', '赵敏',   'F', '13800000004', 'zhaomin@school.edu.cn', 2),
(5, 'T2020005', '陈芳',   'F', '13800000005', 'chenfang@school.edu.cn', 2),
(6, 'T2020006', '钱学文', 'M', '13800000006', 'qianxw@school.edu.cn', 2),
(7, 'T2020007', '周平',   'M', '13800000007', 'zhouping@school.edu.cn', 1);

-- 班级（1 班主任张建国/2 辅导员李慧/3 院领导王长明；班主任周平带软工2401）
INSERT INTO t_class (id, class_name, class_code, college_id, head_teacher_id, counselor_teacher_id) VALUES
(1, '计科2401', 'C2401', 1, 1, 2),
(2, '软工2401', 'SE2401', 1, 7, 2),
(3, '英语2401', 'E2401', 2, 4, 5);

-- 学生
INSERT INTO t_student (id, student_no, name, gender, phone, email, class_id, enroll_year) VALUES
(1, 'S2024001', '张小明', 'M', '13900000001', 'zhangxm@stu.edu.cn', 1, 2024),
(2, 'S2024002', '李大壮', 'M', '13900000002', 'lidz@stu.edu.cn', 2, 2024),
(3, 'S2024003', '周晓丽', 'F', '13900000003', 'zhouxl@stu.edu.cn', 1, 2024),
(4, 'S2024004', '刘婷婷', 'F', '13900000004', 'liutt@stu.edu.cn', 3, 2024);

-- 登录用户：admin/admin123，其余账号密码均为 123456
INSERT INTO t_user (id, username, password, real_name, user_type, ref_id, status) VALUES
(1,  'admin',    '$2a$10$JpgTJiEAti3/LZ5jtfMPx.0tJFVmP620WeQ4oKbvqOjaNOfXPgCse', '系统管理员', 'ADMIN',   NULL, 1),
(2,  'head01',   '$2a$10$LQgzFtKHxBvJbiwzLd2pxukE5ux.cvEr44w9IhQZH9SxHQWqzH4KS', '张建国', 'TEACHER', 1, 1),
(3,  'coun01',   '$2a$10$LQgzFtKHxBvJbiwzLd2pxukE5ux.cvEr44w9IhQZH9SxHQWqzH4KS', '李慧',   'TEACHER', 2, 1),
(4,  'leader01', '$2a$10$LQgzFtKHxBvJbiwzLd2pxukE5ux.cvEr44w9IhQZH9SxHQWqzH4KS', '王长明', 'TEACHER', 3, 1),
(5,  'head02',   '$2a$10$LQgzFtKHxBvJbiwzLd2pxukE5ux.cvEr44w9IhQZH9SxHQWqzH4KS', '赵敏',   'TEACHER', 4, 1),
(6,  'coun02',   '$2a$10$LQgzFtKHxBvJbiwzLd2pxukE5ux.cvEr44w9IhQZH9SxHQWqzH4KS', '陈芳',   'TEACHER', 5, 1),
(7,  'leader02', '$2a$10$LQgzFtKHxBvJbiwzLd2pxukE5ux.cvEr44w9IhQZH9SxHQWqzH4KS', '钱学文', 'TEACHER', 6, 1),
(8,  'head03',   '$2a$10$LQgzFtKHxBvJbiwzLd2pxukE5ux.cvEr44w9IhQZH9SxHQWqzH4KS', '周平',   'TEACHER', 7, 1),
(9,  'stu01',    '$2a$10$LQgzFtKHxBvJbiwzLd2pxukE5ux.cvEr44w9IhQZH9SxHQWqzH4KS', '张小明', 'STUDENT', 1, 1),
(10, 'stu02',    '$2a$10$LQgzFtKHxBvJbiwzLd2pxukE5ux.cvEr44w9IhQZH9SxHQWqzH4KS', '李大壮', 'STUDENT', 2, 1),
(11, 'stu03',    '$2a$10$LQgzFtKHxBvJbiwzLd2pxukE5ux.cvEr44w9IhQZH9SxHQWqzH4KS', '周晓丽', 'STUDENT', 3, 1),
(12, 'stu04',    '$2a$10$LQgzFtKHxBvJbiwzLd2pxukE5ux.cvEr44w9IhQZH9SxHQWqzH4KS', '刘婷婷', 'STUDENT', 4, 1);

-- 角色
INSERT INTO t_role (id, role_name, role_code, description) VALUES
(1, '系统管理员', 'super_admin',    '系统最高权限，负责基础数据与账号管理'),
(2, '班主任',     'head_teacher',   '审批所带班级学生的请假申请（第一环节）'),
(3, '辅导员',     'counselor',      '审批所负责班级学生的请假申请（第二环节）'),
(4, '院领导',     'college_leader', '审批本学院学生请假（第三环节）'),
(5, '学生',       'student',        '发起请假、撤销申请、销假');

-- 用户-角色
INSERT INTO t_user_role (id, user_id, role_id) VALUES
(1, 1, 1),
(2, 2, 2), (3, 3, 3), (4, 4, 4), (5, 5, 2), (6, 6, 3), (7, 7, 4), (8, 8, 2),
(9, 9, 5), (10, 10, 5), (11, 11, 5), (12, 12, 5);

-- 菜单（component 相对 frontend/src/views）
INSERT INTO t_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, visible) VALUES
(1, 0, '首页',       'MENU', '/dashboard',       'dashboard/Dashboard',     'Odometer',        1, 1),
(2, 0, '系统管理',   'DIR',  NULL,                NULL,                      'Setting',         2, 1),
(3, 2, '学院管理',   'MENU', '/system/college',  'system/CollegeManage',    'School',          1, 1),
(4, 2, '班级管理',   'MENU', '/system/class',    'system/ClassManage',      'Notebook',        2, 1),
(5, 2, '菜单管理',   'MENU', '/system/menu',     'system/MenuManage',       'Menu',            3, 1),
(6, 2, '角色管理',   'MENU', '/system/role',     'system/RoleManage',       'UserFilled',      4, 1),
(7, 0, '人员管理',   'DIR',  NULL,                NULL,                      'Avatar',          3, 1),
(8, 7, '教师管理',   'MENU', '/personnel/teacher','personnel/TeacherManage','UserFilled',      1, 1),
(9, 7, '学生管理',   'MENU', '/personnel/student','personnel/StudentManage','User',            2, 1),
(10, 0, '请假中心',  'DIR',  NULL,                NULL,                      'Tickets',         4, 1),
(11, 10, '发起请假', 'MENU', '/leave/apply',      'leave/LeaveApply',        'EditPen',         1, 1),
(12, 10, '我的请假', 'MENU', '/leave/my',         'leave/MyLeave',           'List',            2, 1),
(13, 0, '审批中心',  'DIR',  NULL,                NULL,                      'Stamp',           5, 1),
(14, 13, '班主任审批', 'MENU', '/approval/head',  'approval/ApprovalList',   'UserFilled',      1, 1),
(15, 13, '辅导员审批', 'MENU', '/approval/counselor','approval/ApprovalList','Avatar',         2, 1),
(16, 13, '院领导审批', 'MENU', '/approval/leader','approval/ApprovalList',   'OfficeBuilding',  3, 1),
(17, 0, '请假总览',  'MENU', '/leave/overview',   'leave/LeaveOverview',     'DataAnalysis',    6, 1);

-- 角色-菜单
INSERT INTO t_role_menu (id, role_id, menu_id) VALUES
-- 系统管理员：首页+系统管理+人员管理+请假总览
(1, 1, 1), (2, 1, 2), (3, 1, 3), (4, 1, 4), (5, 1, 5), (6, 1, 6),
(7, 1, 7), (8, 1, 8), (9, 1, 9), (10, 1, 17),
-- 班主任：首页+审批中心（班主任审批）
(11, 2, 1), (12, 2, 13), (13, 2, 14),
-- 辅导员：首页+审批中心（辅导员审批）
(14, 3, 1), (15, 3, 13), (16, 3, 15),
-- 院领导：首页+审批中心（院领导审批）
(17, 4, 1), (18, 4, 13), (19, 4, 16),
-- 学生：首页+请假中心
(20, 5, 1), (21, 5, 10), (22, 5, 11), (23, 5, 12);

-- 演示请假数据
-- 1) 张小明 2 天假已通过（开始日期已到，可演示“销假”）
INSERT INTO t_leave (id, leave_no, student_id, leave_type, start_date, end_date, days, reason, status, current_level, apply_time, actual_return_time) VALUES
(1, 'LV-DEMO-001', 1, '事假', DATE_SUB(CURDATE(), INTERVAL 4 DAY), DATE_SUB(CURDATE(), INTERVAL 3 DAY), 2, '家中有事需要回家处理', 'APPROVED', NULL, DATE_SUB(NOW(), INTERVAL 5 DAY), NULL);

INSERT INTO t_leave_audit (id, leave_id, level, operator_user_id, operator_name, action, comment, audit_time) VALUES
(1, 1, 0, 9,  '张小明', 'SUBMIT',  '提交请假申请', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, 1, 1, 2,  '张建国', 'APPROVE', '同意，注意安全', DATE_SUB(NOW(), INTERVAL 3 DAY));

-- 2) 李大壮 5 天假待班主任审批（可演示 班主任→辅导员 两级审批链）
INSERT INTO t_leave (id, leave_no, student_id, leave_type, start_date, end_date, days, reason, status, current_level, apply_time, actual_return_time) VALUES
(2, 'LV-DEMO-002', 2, '事假', DATE_ADD(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 5 DAY), 5, '参加实习面试，需要请假五天', 'PENDING_HEAD', 1, NOW(), NULL);

INSERT INTO t_leave_audit (id, leave_id, level, operator_user_id, operator_name, action, comment, audit_time) VALUES
(3, 2, 0, 10, '李大壮', 'SUBMIT', '提交请假申请', NOW());
