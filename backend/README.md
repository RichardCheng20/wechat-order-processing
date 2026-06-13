# 蔬菜批发小程序后端

Java 21 + Spring Boot 3 + Sa-Token + MyBatis-Plus + Flyway

## 前置条件

- JDK 21
- Maven 3.9+
- Docker Desktop（用于 MySQL / Redis）

## 快速启动

### 1. 启动基础设施

```bash
cd backend/docker
docker compose up -d
```

### 2. 配置环境变量（可选）

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

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

- API 文档：http://localhost:8080/doc.html
- 健康检查：http://localhost:8080/api/health

## 目录说明

```text
backend/
  src/main/java/com/vwholesale/
    auth/          登录鉴权
    user/          用户
    common/        公共配置、响应、异常
  src/main/resources/db/migration/   Flyway 迁移脚本
  docker/          MySQL + Redis
```
