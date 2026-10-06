# SAIMS 获奖信息管理系统 使用说明书 (v2.10)

欢迎使用 SAIMS (Student Award Information Management System)。本系统是专门为高校竞赛获奖信息采集、审核与统计而设计的现代化 Web 平台。

---

## 🚀 快速启动指南

### 1. 数据库环境搭建 (MySQL)
*   **创建数据库**: 在 MySQL 中创建一个名为 `awardsystem` 的数据库。
*   **导入脚本**: 运行项目根目录下 `/sql/awardsystem.sql` 文件，完成表结构与基础数据的初始化。
*   **连接配置**: 
    - 数据库配置文件位于：`springboot-backend/src/main/resources/application.yml`
    - 账号密码通过**环境变量**注入，不写在代码里：`SAIMS_DB_USERNAME` / `SAIMS_DB_PASSWORD`

### 2. 后端启动 (Spring Boot)
*   **运行环境**: JDK 21。
*   **启动方式**:
    - 使用 IDE (如 IntelliJ IDEA) 打开 `springboot-backend` 文件夹。
    - 运行主启动类 `com.example.SpringbootBackendApplication`。
    - 或直接运行根目录 `start_backend.ps1`（首次请复制 `start_backend.sample.ps1` 并填写自己的配置）。
    - 服务默认端口：`9998`。
*   **文件上传**: 申请附件默认保存至项目根目录下的 `/uploads/` 文件夹。

### 3. 前端启动 (Vue 3 + Vite)
*   **运行环境**: Node.js v20 及以上版本。
*   **依赖安装**: 进入 `vue-frontend` 目录，运行 `npm install`。
*   **开发模式**: 运行 `npm run dev`。
*   **生产部署**: 运行 `npm run build` 生成静态文件。

---

## 🧩 各板块功能详解

### 🛒 学生端流程

#### 1. 竞赛获奖申请
*   **入口**: `StudentAward.vue`
*   **功能**: 学生填写竞赛名称、等级、证书附件等信息并提交。
*   **特点**: 表单式布局，支持多附件极速上传。

#### 2. 我的申请记录
*   **入口**: `ApplicationRecord.vue`
*   **功能**: 追踪申请进度（待审核、已通过、已拒绝、已打回）。
*   **优化**: 
    - **1320px 宽屏**: 提供更广阔的信息展示区域。
    - **两行换行**: 针对超长竞赛名称，系统会自动切换为两行显示，移除省略号，确保信息一目了然。

---

### 🛡️ 管理员端流程

#### 1. 审核管理
*   **入口**: `AdminReview.vue`
*   **功能**: 管理员批量评审学生申请。
*   **特点**: 
    - **1400px 巨幕配置**: 专门针对庞大的操作组（查看、通过、打回、拒绝）进行的黄金宽度微调。
    - **像素级对齐**: 解决了高频操作场景下的列重叠问题。

#### 2. 数据统计看板
*   **入口**: `AdminStatistics.vue`
*   **功能**: 图表化展示学生获奖分布与申请成功率，提供决策支持。

#### 3. 竞赛与学生管理
*   **入口**: `CompetitionManage.vue` / `Student.vue`
*   **功能**: 维护系统的基础元数据。
*   **优化**: 学生管理页面同步了 1320px 宽度标准，解决了“学院/专业”长文本被截断的视觉 Bug。

---

## 🛠️ 维护与后续修改建议

### 布局规范 (CSS)
本系统采用了一套基于 `.content-container` 的响应式分级宽度标准，后续新增页面建议遵循：
*   **标准页面**: `max-width: 1200px`
*   **重度数据页 (管理页)**: `max-width: 1320px` (配合 `el-table` 自适应)
*   **操作密集页 (审核页)**: `max-width: 1400px`

### 前后端交互
*   **接口代理**: 生产环境部署时，需在前端 `vite.config.js` 或 Nginx 中配置针对 `9998` 端口的代理，以解决跨域问题。
*   **状态码**: 系统统一了申请状态枚举，修改 `applicationStatus` 字段时请确保前后端对照表同步更新。

---

> [!NOTE]
> 当前版本 v2.10 (全修改完整版) 已针对全站 UI 做了深度打磨，代码库结构清晰，注释完备，适合作为二次开发的基座。
