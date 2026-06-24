# 蔬菜批发微信小程序

uni-app 前端 + Java Spring Boot 后端，替代纸单流程。

## 项目结构

```text
docs/                  产品与技术文档
backend/               Java 后端（Spring Boot + Sa-Token + Flyway）
frontend/              uni-app 前端（Vue 3 + TS + uView Plus）
vegetable-wholesale-app/ 已废弃（uni-app x 空壳）
```

## 本地环境要求

| 工具 | 用途 | 当前状态 |
|------|------|----------|
| JDK 21 | 后端 | 已安装（需配置 JAVA_HOME） |
| Maven 3.9+ | 后端构建 | 已安装 |
| MySQL + Redis | 数据库/缓存 | 已通过 Homebrew 安装，需确认 MySQL 正常启动 |
| Node.js 18+ | 前端 | 已有 |
| 微信开发者工具 | 小程序预览 | 需安装 |
| HBuilderX | 可选，运行前端 | 可选 |

## 快速开始

### 1. 基础设施

**方式 A：Homebrew（当前机器）**

```bash
brew services start mysql redis
mysql -u root -e "CREATE DATABASE IF NOT EXISTS vwholesale; CREATE USER IF NOT EXISTS 'vwholesale'@'localhost' IDENTIFIED BY 'vwholesale'; GRANT ALL PRIVILEGES ON vwholesale.* TO 'vwholesale'@'localhost';"
```

**方式 B：Docker**

```bash
cd backend/docker && docker compose up -d
```

### 2. 后端

`~/.zshrc` 已配置 JDK 21，新开终端后直接：

```bash
cd backend
mvn spring-boot:run
```

若 Java 未找到，可临时执行 `source backend/scripts/dev-env.sh`。

文档：http://localhost:8080/doc.html

### 3. 前端

```bash
cd frontend
npm install --legacy-peer-deps
npm run dev:mp-weixin
```

用微信开发者工具打开 `frontend/dist/dev/mp-weixin`。

### 4. H5 内测（浏览器，无需小程序审核）

先确保后端已启动（`http://127.0.0.1:8080`），再开前端：

```bash
cd frontend
npm run dev:h5
```

浏览器打开 **http://localhost:5173**（或终端里显示的地址）。

- H5 无法使用微信登录，登录页用 **「老板登录 / 客户A…」** 快捷账号进入
- 本地 dev 默认走 Vite 代理：`/api` → `127.0.0.1:8080`，一般不用填后端地址
- 若代理不通，可在登录页填 `http://127.0.0.1:8080` 后重新登录

**局域网给别人试用**：同一 Wi-Fi 下用手机浏览器访问 `http://<你电脑IP>:5173`（需本机防火墙放行 5173）。

**部署到服务器**（后续）：

```bash
cd frontend
# 构建前在 .env.production 设置 VITE_API_BASE_URL=https://你的API域名
npm run build:h5
# 将 dist/build/h5 目录交给 Nginx；推荐同域反代 /api 到后端
```

## Dev 测试账号（登录页一键切换）

| 角色 | openid | 显示名 |
|------|--------|--------|
| 老板 | `dev-owner-001` | 老板 |
| 客户 A/B/C | `dev-customer-001` / `002` / `003` | 微信用户1/2/3 |

**主流程**：客户下单 → 老板「确认已交货」→ 录价 → 推送给客户。订单详情可「修改状态」用于纠错回退。

## 文档

- [PRD](docs/vegetable-wholesale-miniapp-prd.md)
- [技术架构](docs/technical-architecture.md)
- [开工清单](docs/pre-development-checklist.md)
