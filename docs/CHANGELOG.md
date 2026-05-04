# 更新日志 (Changelog)

## 项目概述

Smart Factory MES System - 智能工厂制造执行系统

- **创建日期**: 2026-04-04
- **技术栈**: Java (Spring Cloud) + .NET 8 + Python + Vue 3 + WPF
- **架构**: 微服务架构，支持2000+设备并发连接

---

## v1.0.26 (2026-05-04)

### 新增功能

#### 1. MES AI预测功能增强

根据MES AI功能开发提示词，实现以下功能：

##### 文件1: schemas/prediction.py
- QualityPredictionRequest - 质量预测请求模型
- QualityPredictionResponse - 质量预测响应模型
- BatchPredictionRequest - 批量预测请求模型
- BatchPredictionResponse - 批量预测响应模型
- ModelInfoResponse - 模型信息响应
- DeviceFaultPredictionRequest - 设备故障预测请求
- DeviceFaultPredictionResponse - 设备故障预测响应
- ProcessParamRecommendationRequest - 工艺参数推荐请求
- ProcessParamRecommendationResponse - 工艺参数推荐响应
- AnomalyDetectionRequest - 异常检测请求
- AnomalyDetectionResponse - 异常检测响应

##### 文件2: services/quality_predictor.py
- 特征工程: 数值归一化、编码、特征组合
- 模型加载: 加载ONNX或Pickle模型
- 推理: 返回预测结果和置信度
- 错误处理: 降级处理和缓存

##### 文件3: router/prediction.py
- POST /api/v1/predict/quality - 质量预测
- POST /api/v1/predict/batch - 批量预测
- GET /api/v1/predict/model/info - 模型信息
- POST /api/v1/predict/device/fault - 设备故障预测
- POST /api/v1/predict/process/recommend - 工艺参数推荐
- POST /api/v1/predict/anomaly - 异常检测

##### 文件4: models/train.py
- 数据加载: 支持CSV和模拟数据
- 特征工程: 特征组合、归一化
- LightGBM训练: 二分类模型
- 模型保存: PKL和ONNX格式

##### 文件5: tests/test_prediction.py
- 18个测试用例全部通过
- 请求/响应模型测试
- 端点测试
- 推理准确性测试

### 文件更新

1. mes-ai-service/README.md - 更新API文档
2. mes-ai-service/requirements.txt - 添加pytest、joblib、onnx依赖
3. mes-ai-service/Dockerfile - 添加models和tests目录

### 问题修复

#### 1. 逻辑删除问题修复
- 禁用@TableLogic，改用物理删除
- 修复删除后无法创建相同编码数据的问题
- 删除多余文档

---

## v1.0.25 (2026-05-02)

### 新增功能

#### 1. WPF设备模拟器
- 创建WPF桌面应用替代旧的Node.js模拟器
- 支持设备创建/更新/删除操作
- 支持实时数据模拟推送
- 支持亮色/暗色主题切换
- 现代化UI设计

#### 2. 设备管理API
- POST /api/dashboard/device - 创建设备
- PUT /api/dashboard/device - 更新设备
- DELETE /api/dashboard/device/{deviceCode} - 删除设备
- DELETE /api/dashboard/devices/all - 清空所有设备

#### 3. 前端优化
- 设备列表每5秒自动刷新
- 搜索支持设备ID和名称
- 运行时长根据最后心跳真实计算
- 修复设备数据显示

#### 4. 代码清理
- 删除旧的mes-device-simulator (Node.js版本)
- 统一使用WPF设备模拟器

---

## v1.0.24 (2026-04-13)

### 新增功能

#### 1. 权限管理系统
- 创建角色管理模块 (RoleController.java)
- 创建菜单管理模块 (MenuController.java)
- 创建用户管理模块 (UserController.java)
- 创建权限管理前端页面 (RoleView.vue, UserView.vue)

#### 2. 数据库表结构
- sys_role - 角色表
- sys_permission - 权限表
- sys_menu - 菜单表
- sys_role_permission - 角色权限关联表
- 新增V4迁移脚本 (V4__permission_enhance.sql)

#### 3. 前端权限控制
- 创建权限store (permission.ts)
- 创建权限指令 (v-permission)
- 动态菜单加载
- 角色管理CRUD

#### 4. 登录认证修复
- 修复BCrypt密码验证问题
- 支持明文密码和BCrypt双模式验证

### 文件更新

1. 后端 (mes-auth)
   - AuthApplication.java - 添加Mapper扫描
   - RoleController.java - 角色CRUD + 权限分配
   - MenuController.java - 菜单查询
   - UserController.java - 用户管理
   - AuthService.java - 密码验证修复
   - application.yml - 添加mapper扫描配置

2. 公共模块 (mes-common)
   - Role.java - 添加sort字段
   - Menu.java - 添加children字段
   - Permission.java - 添加children字段
   - RolePermission.java - 新增实体
   - RoleMapper.java - 新增
   - PermissionMapper.java - 新增
   - MenuMapper.java - 新增
   - RolePermissionMapper.java - 新增

3. 前端 (mes-frontend)
   - router/index.ts - 添加role路由
   - main.ts - 添加权限指令
   - vite.config.ts - 添加auth代理
   - stores/permission.ts - 权限store
   - views/role/RoleView.vue - 角色管理页面
   - views/user/UserView.vue - 用户管理页面
   - views/layout/MainLayout.vue - 动态菜单
   - directives/permission.ts - 权限指令

4. SQL脚本
   - sql/init.sql - 更新用户密码
   - sql/V4__permission_enhance.sql - 权限增强迁移
   - sql/fix_password.sql - 密码修复脚本

---

## v1.0.23 (2026-04-08)

### 新增功能

#### 1. 个人中心和系统设置
- 创建个人中心页面 (ProfileView.vue)
- 创建系统设置页面 (SettingsView.vue)
- 添加真实数据库字段支持

#### 2. 真实用户数据集成
- 添加后端API: PUT /auth/profile
- 添加后端API: PUT /auth/password
- 更新User实体添加新字段

---

## v1.0.22 (2026-04-06)

### 新增功能

#### 1. UI美化优化
- 优化全局样式 (global.scss)
- 统一设计系统 - 按钮、卡片、输入框
- 优化MainLayout布局和样式
- 修复下拉框样式问题
- 添加主题切换功能

#### 2. 数据库更新
- V2迁移: 添加删除功能字段

---

## v1.0.0 (2026-04-04)

### 新增功能

#### 1. 项目结构初始化
- 创建完整的微服务架构项目
- 7个Java微服务模块 (mes-common, mes-gateway, mes-auth, mes-workorder, mes-process, mes-quality, mes-dashboard)
- .NET 8 设备网关 (mes-device-gateway)
- Python AI 服务 (mes-ai-service)
- Vue 3 前端 (mes-frontend)

#### 2. 数据库初始化
- 创建 sql/init.sql 初始化脚本
- 包含所有业务表: users, work_orders, work_reports, process_templates, quality_records, traceability_records, production_stats
- 包含示例测试数据

#### 3. 容器化支持
- 为所有Java服务创建 Dockerfile