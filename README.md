<<<<<<< HEAD
# 学生请假管理系统

前后端分离的课程/演示项目：**Vue3 + Vite + Element Plus + Pinia + Axios**（前端）、**Spring Boot 3 + MyBatis-Plus + Apache Shiro(JWT) + MySQL 8**（后端）。

## 项目结构

```text
D:\Date\student
├─ backend/            Spring Boot 后端
├─ frontend/           Vue3 前端
├─ sql/init.sql        建库建表 + 完整演示数据
└─ README.md
```

## 功能模块

- 登录认证：JWT 无状态 Token、Shiro Realm（认证/授权）、CORS 跨域配置
- 系统管理：学院管理、班级管理、菜单管理（动态路由）、角色管理（角色-菜单分配）
- 人员管理：教师管理、学生管理（自动创建登录账号与角色，可重置密码/启停用）
- 请假业务：学生发起请假、我的请假、撤销申请、销假
- 审批中心：班主任审批 → 辅导员审批 → 院领导审批（按请假天数分层）
- 请假总览（管理员）与首页仪表盘统计

## 审批规则

| 请假天数 | 审批链 |
| --- | --- |
| ≤ 3 天 | 班主任 |
| 4 ~ 7 天 | 班主任 → 辅导员 |
| > 7 天 | 班主任 → 辅导员 → 院领导 |

任一环节驳回即终止；已通过且请假开始后，学生可办理**销假**；未进入审批（待班主任）的申请可**撤销**。

## 快速开始

### 0. 环境要求

- JDK 21、Maven 3.9+
- Node.js 18+（推荐 20+）
- MySQL 8.x（本机服务名 MySQL80）

### 1. 初始化数据库

```bash
mysql -u root -p < sql/init.sql
```

脚本会重建 `student_leave` 库。若 root 有密码，执行时会提示输入。

### 2. 启动后端（端口 8081）

数据库密码通过环境变量 `DB_PASSWORD` 注入（无密码则为空）：

```bash
cd backend
set DB_PASSWORD=你的数据库密码      # Windows CMD
# 或 PowerShell: $env:DB_PASSWORD="你的数据库密码"
mvn spring-boot:run
```

或在 IDEA 中直接打开 `backend/pom.xml` 运行 `LeaveApplication`，并在启动前配置 `DB_PASSWORD` 环境变量。

> 没有 Maven 时，可使用本机缓存路径：`C:\Users\pc\.m2\wrapper\dists\apache-maven-3.9.16\...\bin\mvn.cmd`

### 3. 启动前端（端口 5174）

```bash
cd frontend
npm install
npm run dev
```

浏览器访问 http://localhost:5174 （Vite 已代理 `/api` 到 8081）。
若本机 5173 被其他项目占用也没关系，本系统固定使用 5174。

## 演示账号

| 账号 | 密码 | 角色 | 说明 |
| --- | --- | --- | --- |
| admin | admin123 | 系统管理员 | 系统/人员/总览管理 |
| head01 | 123456 | 班主任 | 计科2401 班主任（第一环节） |
| coun01 | 123456 | 辅导员 | 计科/软工 辅导员（第二环节） |
| leader01 | 123456 | 院领导 | 计算机学院院领导（第三环节） |
| stu01 | 123456 | 学生 | 计科2401 张小明，已有可销假记录 |
| head03 / coun02 / leader02 / stu02... | 123456 | 各自角色 | 其他演示账号 |

## 配置说明

- 后端配置：`backend/src/main/resources/application.yml`（端口 8081、数据源、JWT 密钥与有效期）
- 前端代理：`frontend/vite.config.js`（`/api` → `http://localhost:8081`）
- 密码使用 BCrypt 存储；新建教师/学生初始密码统一 `123456`

  