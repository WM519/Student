@echo off
rem 学生请假系统后端启动脚本（本机 Maven 3.9.16）
cd /d "%~dp0"
if not defined JAVA_HOME set "JAVA_HOME=D:\porgramming language\Idea\JDK"
if not defined DB_USERNAME set DB_USERNAME=root
if not defined DB_PASSWORD set /p DB_PASSWORD=请输入 MySQL 密码: 
set "MVN=C:\Users\pc\.m2\wrapper\dists\apache-maven-3.9.16\0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0\bin\mvn.cmd"
if not exist "%MVN%" set "MVN=mvn"
call "%MVN%" -f pom.xml spring-boot:run
