# 一体化民宿管理系统

## 项目简介

这是一个基于Spring Boot的一体化民宿管理系统，包含宾客官网和员工管理端两个部分。系统实现了房间管理、预订管理、支付处理、统计报表等核心功能，并预留了OTA平台、公安部门、支付网关等外部接口（模拟实现）。

**重要更新**：系统已实现端口分离，宾客端和管理端完全独立运行。

## 技术栈

- **后端**: Spring Boot 3.5.6, Spring Security, JPA, MySQL, JWT
- **前端**: HTML, CSS, JavaScript (原生)
- **数据库**: MySQL 8.0+

## 端口说明

- **8080端口**: 宾客端应用（HotelSystemApplication）- 宾客访问的官网
- **8081端口**: 管理端应用（AdminApplication）- 员工使用的管理后台

## 功能特性

### 宾客端功能（8080端口）
- **用户注册与登录**：支持姓名/邮箱登录，密码找回（密保问题）
- **房间浏览和搜索**：多维度搜索（日期、房型、价格排序）
- **在线预订**：实时预订，支持保证金支付
- **订单管理**：查看订单、取消订单（自动退款）
- **在线支付**：模拟支付流程（保证金、退款）
- **个人中心**：编辑个人信息、修改密码、管理密保问题

### 管理端功能（8081端口）
- **员工登录**：支持多种角色（管理员、经理、前台、房务）
- **预订管理**：查看所有预订、办理入住/退房、订单详情
- **房间管理**：房间增删改查、价格管理
- **房态管理**：前台和房务员工可更新房间状态（清洁中、维修中等）
- **宾客管理**：查看宾客信息、管理宾客偏好
- **POS消费管理**：记录和管理宾客在店消费（餐饮、服务等）
- **智能库存管理**：
  - 物资入库/出库管理
  - 移动加权平均成本核算
  - 库存预警（低库存提醒）
  - 库存交易记录查询
- **任务管理**：自动生成清洁任务，任务分配和跟踪
- **经营分析报表**：
  - 今日统计（入住、退房、收入、房态）
  - 日期范围统计
  - **完整经营分析**（收入分析、成本分析、利润分析）
    - 收入侧：客房收入、POS消费收入、平均房价、入住率、渠道来源占比
    - 成本侧：物料消耗成本、清洁运维成本、平台佣金
    - 利润侧：毛利润、净利润、利润率
- **宾客偏好管理**：
  - 手动添加/编辑宾客偏好
  - 从历史订单自动提取偏好
  - 基于偏好的房型推荐
- **规则设置**：
  - 保证金退还策略（24小时前、24小时内、当天取消退款比例）
  - 节假日折扣策略（折扣率、节假日日期）
  - 默认保证金比例设置
- **员工管理**：创建员工账户、分配角色权限

### 外部接口（模拟实现）
- OTA平台集成接口
- 公安部门接口（身份证验证、住宿登记上报）
- 支付网关接口

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+

### 数据库配置

1. 创建数据库：
```sql
CREATE DATABASE hotel_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 修改配置文件 `HotelSystem/src/main/resources/application.yml` 和 `application-admin.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hotel_system?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8
    username: root
    password: your_password  # 修改为你的数据库密码
```

### 启动方式

#### 方式一：分别启动（推荐）

1. **启动宾客端（8080端口）**：
```bash
cd HotelSystem
mvn spring-boot:run -Dspring-boot.run.main-class=com.hotelsystem.HotelSystemApplication
```

2. **启动管理端（8081端口）**（新开一个终端）：
```bash
cd HotelSystem
mvn spring-boot:run -Dspring-boot.run.main-class=com.hotelsystem.AdminApplication
```

#### 方式二：使用IDE启动

1. 在IDE中运行 `HotelSystemApplication` - 启动宾客端（8080）
2. 在IDE中运行 `AdminApplication` - 启动管理端（8081）

#### 方式三：打包后启动

1. 打包项目：
```bash
mvn clean package
```

2. 启动宾客端：
```bash
java -jar target/HotelSystem-0.0.1-SNAPSHOT.jar --spring.config.name=application
```

3. 启动管理端（新开终端）：
```bash
java -jar target/HotelSystem-0.0.1-SNAPSHOT.jar --spring.config.name=application-admin
```

### 访问地址

- **宾客端**: http://localhost:8080/guest/login.html
- **管理端**: http://localhost:8081/admin/login.html

### 默认账号

系统启动时会自动创建管理员账号：
- 用户名: `admin`
- 密码: `admin123`
- 角色: `ADMIN`

## 问题修复说明

### 1. 管理员登录问题
- ✅ 已修复：AuthController现在返回统一的ApiResponse格式
- ✅ 已修复：登录接口使用username字段（不是email）

### 2. API返回格式问题
- ✅ 已修复：所有API统一返回ApiResponse格式
- ✅ 已修复：前端API客户端增加了JSON解析错误处理

### 3. 端口分离
- ✅ 已实现：宾客端运行在8080端口
- ✅ 已实现：管理端运行在8081端口
- ✅ 已实现：管理端使用独立的API客户端（api-admin.js）

### 4. 功能完善
- ✅ 已添加：宾客管理页面
- ✅ 已添加：员工管理页面
- ✅ 已修复：预订创建时自动从token获取guestId
- ✅ 已添加：房态管理页面（供前台和房务员工使用）
- ✅ 已添加：规则设置模块（保证金退还策略、节假日折扣策略）
- ✅ 已修复：取消订单功能（自动退款）
- ✅ 已修复：管理端预订和宾客页面数据加载问题

### 5. 核心业务功能实现
- ✅ **智能库存管理（FR-09）**：
  - 物资入库/出库管理
  - 移动加权平均成本核算
  - 库存预警功能
  - 库存交易记录查询
  
- ✅ **POS消费管理**：
  - 消费记录创建、更新、删除
  - 按预订查询消费记录
  - 支持多种消费分类（餐饮、服务、商品等）

- ✅ **自动化房务（FR-04）**：
  - 退房时自动创建清洁任务
  - 房态更新为"已清洁"时自动扣减标准耗材
  - 房间标准耗材配置管理

- ✅ **完整经营分析（FR-08）**：
  - 收入分析：客房收入、POS消费收入、平均房价、入住率、渠道来源占比
  - 成本分析：物料消耗成本、清洁运维成本、平台佣金
  - 利润分析：毛利润、净利润、利润率计算
  - 支持日期范围查询

- ✅ **宾客偏好管理（FR-10）**：
  - 手动添加/编辑宾客偏好（房型偏好、楼层偏好、设施偏好等）
  - 从历史订单自动提取偏好
  - 基于偏好的房型推荐功能
  - 偏好频率统计和排序

- ✅ **任务管理**：
  - 自动生成清洁任务（退房时）
  - 任务分配和状态跟踪
  - 支持多种任务类型（清洁、维修、入住、退房等）

- ✅ **消息通知系统**：
  - 通知实体和基础服务已实现
  - 支持多种通知类型（预订确认、库存预警、任务分配等）

### 6. 最新功能完善（根据测试反馈）

#### 管理端新增功能

- ✅ **角色化工作台（NFR-12）**：
  - 前台工作台：显示待办理入住、待办理退房、待处理订单
  - 房务工作台：显示待清洁房间、清洁任务、维修中房间、库存预警
  - 经理/管理员工作台：显示经营数据概览、异常提醒、库存预警
  - 实时任务聚合和提醒
  - 根据角色自动显示相关待办任务

- ✅ **批量操作功能**：
  - 批量确认预订（`POST /reservations/batch/confirm`）
  - 批量取消预订（`POST /reservations/batch/cancel`）
  - 支持选择多个订单进行批量处理
  - 批量操作结果反馈（成功数、失败数、错误详情）

- ✅ **支付记录查询**：
  - 管理端支付记录查询（`GET /payments/admin`）
    - 支持按支付类型筛选（PAYMENT/REFUND）
    - 支持按支付状态筛选（PENDING/SUCCESS/FAILED）
    - 支持按日期范围筛选
  - 宾客端支付记录查询（`GET /payments/me`）
    - 查看自己的所有支付记录
  - 按预订查询支付记录（`GET /payments/reservation/{id}`）

- ✅ **订单详情增强**：
  - 订单详情接口（`GET /reservations/{id}/details`）
    - 包含订单基本信息
    - 订单条款（取消政策、保证金说明、入住须知）
    - 订单操作历史记录
    - 支付记录列表
  - 支持宾客端和管理端查看

- ✅ **审计日志查看**：
  - 操作日志查询接口（`GET /api/logs`）
    - 支持分页查询
    - 支持按用户名、操作类型、时间范围筛选
    - 最近日志查询（`GET /api/logs/recent`）
  - 管理端日志查看页面（`/admin/logs.html`）

#### 宾客端新增功能

- ✅ **支付记录查询**：
  - 我的支付记录页面
  - 查看所有支付和退款记录
  - 支付详情展示（金额、状态、时间）

- ✅ **订单详情完善**：
  - 订单详情查看（包含订单条款、操作历史）
  - 支付记录关联显示

## 项目结构

```
HotelSystem/
├── src/
│   ├── main/
│   │   ├── java/com/hotelsystem/
│   │   │   ├── controller/          # 控制器层
│   │   │   │   ├── external/        # 外部接口控制器
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── RoomController.java
│   │   │   │   ├── ReservationController.java
│   │   │   │   ├── GuestController.java
│   │   │   │   ├── GuestPreferenceController.java
│   │   │   │   ├── FrontDeskController.java
│   │   │   │   ├── InventoryController.java
│   │   │   │   ├── PosConsumptionController.java
│   │   │   │   ├── StatisticsController.java
│   │   │   │   ├── SystemSettingController.java
│   │   │   │   └── UserController.java
│   │   │   ├── service/            # 服务层
│   │   │   │   ├── external/     # 外部接口服务
│   │   │   │   ├── ReservationService.java
│   │   │   │   ├── RoomService.java
│   │   │   │   ├── GuestService.java
│   │   │   │   ├── GuestPreferenceService.java
│   │   │   │   ├── InventoryService.java
│   │   │   │   ├── PosConsumptionService.java
│   │   │   │   ├── StatisticsService.java
│   │   │   │   ├── TaskService.java
│   │   │   │   ├── NotificationService.java
│   │   │   │   └── RoomStatusUpdateService.java
│   │   │   ├── repository/         # 数据访问层
│   │   │   ├── entity/             # 实体类
│   │   │   │   ├── Room.java
│   │   │   │   ├── Reservation.java
│   │   │   │   ├── Guest.java
│   │   │   │   ├── GuestPreference.java
│   │   │   │   ├── Inventory.java
│   │   │   │   ├── InventoryTransaction.java
│   │   │   │   ├── PosConsumption.java
│   │   │   │   ├── Task.java
│   │   │   │   ├── Notification.java
│   │   │   │   ├── SystemSetting.java
│   │   │   │   └── RoomStandardConsumption.java
│   │   │   ├── dto/                # 数据传输对象
│   │   │   ├── security/           # 安全配置
│   │   │   ├── config/             # 配置类
│   │   │   └── audit/              # 审计日志
│   │   └── resources/
│   │       ├── static/             # 静态资源（前端页面）
│   │       │   ├── guest/          # 宾客端页面
│   │       │   │   ├── login.html
│   │       │   │   ├── register.html
│   │       │   │   ├── rooms.html
│   │       │   │   └── profile.html
│   │       │   ├── admin/          # 管理端页面
│   │       │   │   ├── login.html
│   │       │   │   ├── dashboard.html
│   │       │   │   ├── reservations.html
│   │       │   │   ├── rooms.html
│   │       │   │   ├── room-status.html
│   │       │   │   ├── guests.html
│   │       │   │   ├── statistics.html
│   │       │   │   ├── settings.html
│   │       │   │   └── js/
│   │       │   │       ├── api-admin.js
│   │       │   │       └── permissions.js
│   │       │   ├── css/            # 样式文件
│   │       │   └── js/             # 宾客端API客户端
│   │       ├── application.yml      # 宾客端配置（8080端口）
│   │       └── application-admin.yml # 管理端配置（8081端口）
│   └── test/                       # 测试文件
├── HotelSystemApplication.java     # 宾客端启动类
├── AdminApplication.java           # 管理端启动类
└── pom.xml                         # Maven配置
```

## API接口

### 认证接口
- `POST /auth/login` - 登录（支持username和password）
- `POST /guests` - 宾客注册

### 房间接口
- `GET /rooms` - 获取所有房间（宾客端匿名可访问）
- `GET /rooms/{id}` - 获取房间详情
- `POST /rooms` - 创建房间（需管理员权限）
- `PUT /rooms/{id}` - 更新房间（需管理员权限）
- `DELETE /rooms/{id}` - 删除房间（需管理员权限）

### 预订接口
- `GET /reservations` - 获取所有预订（需管理员权限）
- `GET /reservations/me` - 获取我的预订（宾客）
- `GET /reservations/{id}` - 获取预订详情（支持宾客和管理端）
- `GET /reservations/{id}/details` - 获取订单详情（包含条款、历史、支付记录）
- `POST /reservations` - 创建预订（宾客端自动从token获取guestId）
- `POST /reservations/{id}/cancel` - 取消预订
- `POST /reservations/batch/confirm` - 批量确认预订（管理端）
- `POST /reservations/batch/cancel` - 批量取消预订（管理端）

### 前台操作接口
- `POST /frontdesk/checkin/{reservationId}` - 办理入住（需前台权限）
- `POST /frontdesk/checkout/{reservationId}` - 办理退房（需前台权限）

### 工作台接口
- `GET /api/dashboard/workspace` - 获取角色化工作台数据（根据角色返回不同的待办任务）

### 统计接口
- `GET /api/statistics/today` - 今日统计（需管理员权限）
- `GET /api/statistics/date-range` - 日期范围统计（需管理员权限）
- `GET /api/statistics/room-types` - 房型统计（需管理员权限）
- `GET /api/statistics/business-analysis` - 完整经营分析（收入、成本、利润）（需管理员权限）

### 审计日志接口
- `GET /api/logs` - 获取操作日志列表（分页，支持筛选）
- `GET /api/logs/recent` - 获取最近的日志

### 支付接口
- `POST /payments/create` - 创建支付交易
- `POST /payments/callback` - 支付回调
- `GET /payments/reservation/{reservationId}` - 获取预订的支付记录
- `GET /payments/me` - 获取我的支付记录（宾客端）
- `GET /payments/admin` - 管理端查询所有支付记录（支持按类型、状态、日期筛选）

### 库存管理接口
- `GET /inventory` - 获取所有库存物品（需房务/管理员权限）
- `GET /inventory/{id}` - 获取库存详情
- `POST /inventory` - 创建库存物品（需管理员权限）
- `PUT /inventory/{id}` - 更新库存物品（需管理员权限）
- `POST /inventory/{id}/stock-in` - 入库操作（需管理员权限）
- `POST /inventory/{id}/stock-out` - 出库操作（需房务/管理员权限）
- `GET /inventory/low-stock` - 获取低库存物品（需房务/管理员权限）
- `GET /inventory/{id}/transactions` - 获取库存交易记录（需管理员权限）

### POS消费接口
- `GET /pos/reservation/{reservationId}` - 获取预订的消费记录（需前台/管理员权限）
- `POST /pos` - 创建消费记录（需前台/管理员权限）
- `PUT /pos/{id}` - 更新消费记录（需前台/管理员权限）
- `DELETE /pos/{id}` - 删除消费记录（需前台/管理员权限）

### 宾客偏好接口
- `GET /guests/{guestId}/preferences` - 获取宾客偏好列表（需前台/管理员权限）
- `POST /guests/{guestId}/preferences` - 创建偏好记录（需前台/管理员权限）
- `PUT /guests/{guestId}/preferences/{id}` - 更新偏好记录（需前台/管理员权限）
- `DELETE /guests/{guestId}/preferences/{id}` - 删除偏好记录（需前台/管理员权限）
- `POST /guests/{guestId}/preferences/extract` - 从历史订单提取偏好（需前台/管理员权限）
- `GET /guests/{guestId}/preferences/recommendations` - 获取推荐房型（需前台/管理员权限）

### 规则设置接口
- `GET /settings` - 获取所有系统设置（需管理员权限）
- `GET /settings/{key}` - 获取指定设置（需管理员权限）
- `POST /settings` - 保存系统设置（需管理员权限）
- `PUT /settings/{key}` - 更新系统设置（需管理员权限）
- `DELETE /settings/{id}` - 删除系统设置（需管理员权限）

### 房间状态接口
- `PUT /rooms/{id}/status` - 更新房间状态（需前台/房务/管理员权限）
  - 支持状态：AVAILABLE（空闲）、OCCUPIED（已入住）、RESERVED（已预订）、CLEANING（清洁中）、MAINTENANCE（维修中）
  - 当状态更新为AVAILABLE时，自动触发标准耗材扣减

### 外部接口（模拟）
- `POST /api/external/ota/sync-reservation` - 同步预订到OTA平台
- `POST /api/external/security/verify-id-card` - 验证身份证
- `POST /api/external/payment/create-order` - 创建支付订单

## 角色权限

- **ADMIN**: 管理员，拥有所有权限
  - 所有管理功能
  - 员工管理
  - 规则设置
  - 完整经营分析报表

- **MANAGER**: 经理，可以管理预订、房间、查看报表
  - 预订管理、房间管理
  - 宾客管理、宾客偏好管理
  - POS消费管理
  - 库存管理
  - 经营分析报表
  - 规则设置

- **RECEPTIONIST**: 前台，可以办理入住/退房、查看预订
  - 预订查看和管理
  - 办理入住/退房
  - 房态管理
  - POS消费记录
  - 宾客偏好查看和编辑

- **HOUSEKEEPING**: 房务，可以管理房态和库存
  - 房态管理（更新房间状态）
  - 库存查看和出库操作
  - 任务查看和处理

- **GUEST**: 宾客，可以浏览房间、预订、查看自己的订单
  - 房间浏览和搜索
  - 在线预订
  - 订单管理（查看、取消）
  - 个人中心管理

## 核心业务逻辑

### 预订流程
1. 宾客浏览房间并选择入住/离店日期
2. 创建预订（状态：待支付）
3. 支付保证金（模拟支付）
4. 预订确认（状态：已确认，房态：已预订）
5. 办理入住（状态：已入住，房态：已入住）
6. 办理退房（状态：已退房，房态：清洁中，自动创建清洁任务）

### 自动化房务流程
1. 退房时自动创建清洁任务
2. 房务员工完成清洁，更新房态为"已清洁"
3. 系统自动扣减房间标准耗材（如果已配置）
4. 质检完成后，更新房态为"空闲"，房间可再次预订

### 库存管理流程
1. 创建库存物品（设置安全库存阈值）
2. 采购入库（使用移动加权平均法更新成本）
3. 房务出库（清洁时自动出库，或手动申领）
4. 库存低于安全阈值时系统预警

### 经营分析
- **收入统计**：从预订和POS消费记录中统计
- **成本统计**：从库存出库交易中统计物料成本
- **利润计算**：收入 - 成本 = 利润，自动计算利润率

### 宾客偏好管理
- **自动提取**：系统从宾客历史订单中自动提取房型偏好
- **手动管理**：前台员工可以手动添加/编辑宾客偏好
- **智能推荐**：办理入住时，系统基于偏好推荐匹配房型

### 角色权限说明
- **ADMIN**: 管理员，拥有所有权限
- **MANAGER**: 经理，可以管理预订、房间、查看报表
- **RECEPTIONIST**: 前台，可以办理入住/退房、查看预订
- **HOUSEKEEPING**: 房务，可以查看房间状态、更新房态
- **GUEST**: 宾客，可以浏览房间、预订、查看自己的订单

## 注意事项

1. **端口分离**: 两个应用必须同时运行才能完整使用系统功能。

2. **外部接口**: 所有外部接口（OTA平台、公安部门、支付网关）都是模拟实现，不会真实调用外部API。

3. **支付流程**: 支付功能为模拟实现，实际使用时需要集成真实的支付网关。

4. **安全**: 生产环境请修改JWT密钥和数据库密码，并启用HTTPS。

5. **数据初始化**: 系统启动时会自动创建管理员账号，但不会初始化房间数据，需要手动添加。

6. **库存配置**: 首次使用库存管理功能时，需要先创建库存物品，然后配置房间标准耗材。

7. **规则设置**: 建议在使用前先配置保证金退还策略和节假日折扣策略。

## 开发说明

### 添加新功能

1. 在 `entity` 包中创建实体类
2. 在 `repository` 包中创建Repository接口
3. 在 `service` 包中创建Service类
4. 在 `controller` 包中创建Controller类
5. 如需前端页面，在 `static` 目录下创建HTML文件

### 数据库迁移

系统使用JPA自动创建表结构（`ddl-auto: update`），首次运行时会自动创建所有表。

## 许可证

本项目仅供学习和参考使用。
