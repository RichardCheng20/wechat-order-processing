# 蔬菜批发小程序后端

Java 21 + Spring Boot 3 + Sa-Token + MyBatis-Plus + Flyway + RabbitMQ（可选）

## 前置条件

- JDK 21
- Maven 3.9+
- MySQL 8 + Redis + RabbitMQ（推荐 Docker，见下文）

## 快速启动

### 1. 启动基础设施

```bash
cd backend/docker
docker compose up -d
```

| 服务 | 端口 | 账号 |
|------|------|------|
| MySQL | 3306 | `vwholesale` / `vwholesale`，库名 `vwholesale` |
| Redis | 6379 | 无密码 |
| RabbitMQ | 5672 / 15672 | `vwholesale` / `vwholesale` |

管理台：http://localhost:15672

暂不启用消息队列时：

```bash
export MQ_ENABLED=false
```

### 2. 启动后端

必须在 **`backend/`** 目录执行（项目根目录会报 `No plugin found for prefix 'spring-boot'`）：

```bash
cd backend
mvn spring-boot:run
```

- API 文档：http://localhost:8080/doc.html
- 健康检查：http://localhost:8080/api/health

修改 Java 代码或 Flyway 迁移后需**重启**后端。8080 被占用时先结束旧进程。

JDK 未找到时：

```bash
source backend/scripts/dev-env.sh
cd backend && mvn spring-boot:run
```

### 3. 本地微信 / 太阳码（可选）

复制并编辑（勿提交 Git）：

```text
src/main/resources/application-dev-local.example.yml
  → application-dev-local.yml
```

填入 `app-secret`；仅开发者工具联调时设 `env-version: develop`。  
`application-dev.yml` 会自动 import 该文件。

未配置微信时使用 **dev-login** 联调（见 [docs/testing.md](../docs/testing.md)）。

## 常用命令

| 步骤 | 命令 |
|------|------|
| 基础设施 | `cd backend/docker && docker compose up -d` |
| 后端 | `cd backend && mvn spring-boot:run` |
| API 冒烟（仓库根） | `../scripts/smoke-api.sh` 或于项目根 `./scripts/smoke-api.sh` |
| 老板 API 详细冒烟 | `BASE_URL=http://localhost:8080 ./scripts/smoke-test-boss-apis.sh` |

## API 前缀

| 前缀 | 说明 |
|------|------|
| `/api/auth` | 微信登录、dev-login、退出 |
| `/api/customer` | 客户端：商品、下单、绑定、注册 |
| `/api/boss` | 老板端：订单、商品、客户、采购、收款、报表等 |
| `/api/worker` | 工人端：拣单任务 |
| `/api/config` | 公共配置 |
| `/api/health` | 健康检查 |

完整接口以 Knife4j（`/doc.html`）或各 `*Controller.java` 为准。

## 目录说明

```text
backend/
  docker/              MySQL + Redis + RabbitMQ
  scripts/             联调 SQL、smoke-test-boss-apis.sh
  src/main/java/com/vwholesale/
    auth/              登录鉴权、WechatClient
    user/              用户实体
    common/            配置、异常、MerchantContext、Health
    customer/          客户 CRUD、VIP 码、注册审核
    product/           商品、分类、库存、报价单
    order/             订单、拣单、录价、统计
    dispatch/          派单
    payment/           销售收款、采购付款、文件上传
    procurement/       采购任务
    supplier/          供应商
    worker/            人员管理
    merchant/          档口资料、档口下单码、数据平台密码
    mq/                RabbitMQ 消费者
  src/main/resources/
    application.yml              默认 profile: dev
    application-dev.yml            本地连接配置
    application-dev-local.example.yml  私密配置模板
    db/migration/                  Flyway 迁移（V1…）
```

## 消息队列（RabbitMQ）

| 队列 | 用途 |
|------|------|
| `vwholesale.queue.wechat.notify` | 微信订阅消息（失败重试） |
| `vwholesale.queue.stats.refresh` | 老板端统计缓存刷新 |
| `vwholesale.queue.dlq` | 死信队列 |

配置：

- `app.mq.enabled`：默认 `true`；`false` 时同步处理
- `spring.rabbitmq.*`：见 `application-dev.yml`

## 测试与数据脚本

本地清数据、重置库存、Flyway 修复等见 **[docs/testing.md](../docs/testing.md)**。

## 前端联调

```bash
cd frontend
npm install --legacy-peer-deps
npm run dev:mp-weixin
```

微信开发者工具打开 `frontend/dist/dev/mp-weixin`。真机调试在登录页配置局域网后端 IP。
