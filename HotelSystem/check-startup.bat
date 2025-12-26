@echo off
echo ========================================
echo 酒店管理系统启动检查脚本
echo ========================================
echo.

echo [1] 检查 Java 版本...
java -version
if %errorlevel% neq 0 (
    echo 错误: 未找到 Java，请安装 Java 17 或更高版本
    pause
    exit /b 1
)
echo.

echo [2] 检查 Maven 版本...
mvn -version
if %errorlevel% neq 0 (
    echo 错误: 未找到 Maven
    pause
    exit /b 1
)
echo.

echo [3] 检查端口占用...
echo 检查 8080 端口...
netstat -ano | findstr :8080
if %errorlevel% equ 0 (
    echo 警告: 8080 端口已被占用
) else (
    echo 8080 端口可用
)
echo.

echo 检查 8081 端口...
netstat -ano | findstr :8081
if %errorlevel% equ 0 (
    echo 警告: 8081 端口已被占用
) else (
    echo 8081 端口可用
)
echo.

echo [4] 清理并编译项目...
call mvn clean compile
if %errorlevel% neq 0 (
    echo 错误: 编译失败
    pause
    exit /b 1
)
echo.

echo [5] 检查数据库配置...
echo 请确认以下配置正确：
echo - MySQL 服务已启动
echo - 数据库 hotel_system 已创建
echo - 用户名: root
echo - 密码: 请检查 application.yml 和 application-admin.yml
echo.

echo ========================================
echo 检查完成！
echo ========================================
echo.
echo 如果所有检查都通过，可以尝试启动：
echo   宾客端: mvn spring-boot:run -Dspring-boot.run.main-class=com.hotelsystem.HotelSystemApplication
echo   管理端: mvn spring-boot:run -Dspring-boot.run.main-class=com.hotelsystem.AdminApplication
echo.
pause

