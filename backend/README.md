# 蔬菜批发小程序后端

Java 21 + Spring Boot 3 + Sa-Token + MyBatis-Plus + Flyway

## 前置条件

- JDK 21
- Maven 3.9+
- Docker Desktop（用于 MySQL / Redis）

## 快速启动

### 1. 启动基础设施（MySQL + Redis + RabbitMQ）

```bash
cd backend/docker
docker compose up -d
```

RabbitMQ 管理台：http://localhost:15672（账号/密码 `vwholesale` / `vwholesale`）

若暂不启用消息队列，可设置环境变量 `MQ_ENABLED=false`，系统将回退为同步处理。

### 2. 启动后端

`~/.zshrc` 已配置 JDK 21 时，新开终端后：

```bash
cd backend
mvn spring-boot:run
```

若提示找不到 Java：

```bash
source backend/scripts/dev-env.sh
cd backend
mvn spring-boot:run
```

- API 文档：http://localhost:8080/doc.html
- 健康检查：http://localhost:8080/api/health

**注意：**

- 必须在 `backend/` 目录执行，项目根目录运行会报 `No plugin found for prefix 'spring-boot'`
- 8080 已被占用说明旧进程还在，先结束再启动
- 修改 Java 代码或 Flyway 迁移后需重启后端

### 3. 启动前端（小程序）

首次安装依赖：

```bash
cd frontend
npm install --legacy-peer-deps
```

开发编译：

```bash
cd frontend
npm run dev:mp-weixin
```

用微信开发者工具打开目录：`frontend/dist/dev/mp-weixin`

- 模拟器默认连 `http://127.0.0.1:8080`
- 真机调试在登录页配置局域网后端 IP

### 4. 配置环境变量（可选）

微信正式登录需配置：

```bash
export WECHAT_APP_ID=你的AppID
export WECHAT_APP_SECRET=你的AppSecret
```

或在 `application-dev.yml` / 环境变量中配置。

主管理员白名单（openid）在 `application.yml` 的 `app.admin.openid-whitelist` 中配置。

本地开发未配置微信时，可使用 **开发登录接口**：

```bash
curl -X POST http://localhost:8080/api/auth/dev-login \
  -H 'Content-Type: application/json' \
  -d '{"openid":"dev-owner-001","nickname":"老板","role":"OWNER_ADMIN"}'
```

## 常用命令速查

| 步骤 | 命令 |
|------|------|
| 基础设施 | `cd backend/docker && docker compose up -d` |
| 后端 | `cd backend && mvn spring-boot:run` |
| **API 冒烟测试** | `./scripts/smoke-api.sh`（需后端已启动） |
| 前端（首次） | `cd frontend && npm install --legacy-peer-deps` |
| 前端（开发） | `cd frontend && npm run dev:mp-weixin` |
| 前端（构建） | `cd frontend && npm run build:mp-weixin` |

## 目录说明

```text
backend/
  scripts/       本地测试/数据脚本（见 docs/testing.md）
  src/main/java/com/vwholesale/
    auth/          登录鉴权
    user/          用户
    common/        公共配置、响应、异常
  src/main/resources/db/migration/   Flyway 迁移脚本
  docker/          MySQL + Redis + RabbitMQ
```

## 消息队列（RabbitMQ）

订单相关异步任务通过 RabbitMQ 处理，队列与交换机均开启**持久化**：

| 队列 | 用途 |
|------|------|
| `vwholesale.queue.wechat.notify` | 客户提醒老板（微信订阅消息，失败重试 3 次） |
| `vwholesale.queue.stats.refresh` | 订单/收款事件后刷新老板端统计缓存 |
| `vwholesale.queue.dlq` | 死信队列 |

配置项（`application.yml`）：

- `app.mq.enabled`：是否启用 MQ（默认 `true`，设为 `false` 则同步处理）
- `spring.rabbitmq.*`：连接地址与账号（见 `application-dev.yml`）

## 测试与数据脚本

本地联调、清空订单、重置库存、Flyway 修复等，见 **[docs/testing.md](../docs/testing.md)**。
