# 获奖系统 SAIMS

高校学生竞赛获奖信息管理系统（Student Award Information Management System）。
覆盖 **获奖申报 → 审核 → 查重/OCR 智能预检 → 统计 → 竞赛组队** 的完整业务闭环。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.5.6 + MyBatis + MySQL 8 + Hutool + Apache POI（Excel 导入导出） |
| 前端 | Vue 3 + Vite + Element Plus + ECharts + Axios |
| 鉴权 | JWT（`Authorization: Bearer <token>`） |
| 智能预检 | 感知哈希（pHash）查重 + 百度智能云 OCR 文字识别（可选） |
| 运行环境 | JDK 21、Node.js 20+、MySQL 8 |

## 目录结构

```
system/
├─ springboot-backend/       后端服务（Spring Boot，端口 9998）
│  ├─ src/main/java/com/example/
│  │  ├─ certificate/        证书查重、OCR 预检、风险规则
│  │  ├─ team/               竞赛组队与匹配推荐
│  │  ├─ controller/ service/ mapper/ entity/
│  │  └─ SpringbootBackendApplication.java   主启动类
│  └─ src/main/resources/
│     ├─ application.yml     配置（数据库/端口/JWT/OCR）
│     └─ mapper/*.xml        MyBatis 映射
├─ vue-frontend/             前端工程（Vue 3 + Vite）
│  └─ src/views/             各业务页面（AdminReview、TeamHub 等）
├─ sql/                      数据库脚本
├─ docs/                     说明文档
├─ start_backend.sample.ps1  后端启动脚本模板
└─ USAGE_GUIDE.md            功能使用说明
```

## 快速开始

### 1. 准备数据库

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS awardsystem DEFAULT CHARSET utf8mb4;"
mysql -u root -p awardsystem < sql/schema.sql           # 1. 全部 40 张表结构
mysql -u root -p awardsystem < sql/sample-data.sql      # 2. 字典数据（竞赛目录/技能树/勋章/风险规则）
mysql -u root -p awardsystem < sql/demo-accounts.sql    # 3. 演示账号（虚构数据）
```

数据库名固定为 `awardsystem`（后端 `application.yml` 中已写死连接串）。
`sql/` 目录下的其他 `*.sql` 为历史迁移脚本，仅当从旧版本库升级时才需要执行，全新导入无需理会。

### 2. 配置环境变量

后端**不把密码写在代码里**，启动前必须提供以下变量（缺任何一个都会启动失败）：

| 变量 | 必填 | 说明 |
| --- | --- | --- |
| `SAIMS_DB_USERNAME` | 是 | MySQL 账号 |
| `SAIMS_DB_PASSWORD` | 是 | MySQL 密码 |
| `SAIMS_JWT_SECRET` | 是 | JWT 签名密钥，任意 32 位以上随机串 |
| `SAIMS_OCR_API_KEY` / `SAIMS_OCR_SECRET_KEY` | 否 | 百度 OCR 密钥，留空则证书预检自动转人工审核 |

Windows PowerShell 下最省事的方式：

```powershell
Copy-Item start_backend.sample.ps1 start_backend.ps1   # 复制模板
notepad start_backend.ps1                              # 填自己的账号密码
```

### 3. 启动后端

```bash
cd springboot-backend
mvn -DskipTests package                # 首次需打包
java -jar target/springboot-backend-0.0.1-SNAPSHOT.jar
```

或用 IDE 直接运行 `com.example.SpringbootBackendApplication`。
启动成功后服务监听 `http://localhost:9998`。

### 4. 启动前端

```bash
cd vue-frontend
npm install
npm run dev
```

前端默认 `http://localhost:5173`（若端口被占用，Vite 会自动顺延）。
前后端联调地址已在前端代码中固定为 `http://localhost:9998`，无需额外配置代理。

## 演示账号

演示数据自带三个账号（密码为明文演示值，仅供本地联调，请勿用于生产）：

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `202600010001` | `202600010001` | 学生 |
| `202600010002` | `202600010002` | 学生 |
| `admin` | `admin123` | 管理员 |

## 注意

- 本仓库**不包含**运行时的学生上传材料（`uploads/`）、构建产物（`node_modules/`、`target/`）与本机密钥文件，这些已在 `.gitignore` 中排除。
- 请勿把 `start_backend.ps1`、`.env`、数据库密码、JWT 密钥提交到仓库。
- 附件默认保存在后端运行目录的 `uploads/applications/` 下，需保证该目录可写。
