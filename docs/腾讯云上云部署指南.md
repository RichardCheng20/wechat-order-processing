# 腾讯云上云部署指南

版本：v1.0  
日期：2026-06-17  
适用项目：蔬菜批发微信小程序（`frontend/` + `backend/`）

本文说明在**腾讯云**上需要购买哪些资源、推荐规格，以及从 0 到上线的部署步骤。

---

## 1. 上云架构概览

```text
微信小程序（uni-app）
    │  HTTPS
    ▼
域名 api.xxx.com（已备案 + SSL）
    │
    ▼
Nginx（443）── 反代 /api、/uploads
    │
    ▼
Spring Boot 后端（8080，内网）
    │
    ├── MySQL 8（业务库 vwholesale）
    ├── Redis（Sa-Token 会话 / 统计缓存）
    ├── RabbitMQ（微信通知、统计刷新异步队列）
    └── 文件：本地磁盘 uploads/ 或 腾讯云 COS
```

**说明**

- 小程序**必须**使用 **HTTPS 域名**，并在微信公众平台配置合法域名。
- 域名若解析到**中国大陆**服务器，需完成 **ICP 备案**。
- 前端小程序代码在微信平台上传审核，**不需要**单独买「小程序服务器」；需要的是 **API 后端 + 域名**。

---

## 2. 需要购买的腾讯云产品

### 2.1 必选

| 产品 | 控制台名称 | 用途 | 本项目说明 |
|------|------------|------|------------|
| 云服务器 CVM | 云服务器 | 跑 Nginx、Java 后端、Docker（MySQL/Redis/RabbitMQ） | 首版可单机 All-in-One |
| 域名 | 域名注册 | `api.你的域名.com` | 仅 API 子域名即可，也可同域反代 |
| SSL 证书 | SSL 证书 | HTTPS | 腾讯云免费 DV 证书即可 |
| ICP 备案 | 备案 | 大陆服务器 + 域名合法接入 | 购买 CVM 后在同一账号提交备案 |

### 2.2 强烈建议

| 产品 | 用途 | 说明 |
|------|------|------|
| 对象存储 COS | 商品图、收款凭证、对账单图片 | 比 CVM 本地盘更可靠；后端 `/uploads` 可逐步迁到 COS |
| 云数据库 TencentDB for MySQL | 业务库 | 自动备份、主从可选；比 Docker 自管 MySQL 更省心 |
| 云数据库 TencentDB for Redis | 缓存 / Token | 小规格即可 |

### 2.3 可选（规模上来后再买）

| 产品 | 用途 |
|------|------|
| 负载均衡 CLB | 多台 CVM 时做高可用 |
| 云监控 + 告警 | CPU、磁盘、端口、进程监控 |
| 内容分发 CDN | COS 静态资源加速（图片多时再开） |
| 容器服务 TKE | 已 Docker 化且要多实例时再考虑 |

### 2.4 不需要单独购买

| 项目 | 原因 |
|------|------|
| 微信小程序「服务器」 | 小程序运行在微信客户端，只需配置后端 API 域名 |
| uni-app / HBuilderX | 本地或 CI 构建，构建产物上传微信后台 |
| 独立 RabbitMQ 云服务 | 腾讯云无标准托管 RabbitMQ，**在 CVM 上用 Docker 跑即可**（与本地 `backend/docker/docker-compose.yml` 一致） |

---

## 3. 推荐配置（按阶段）

### 3.1 内测 / 单店上线（最省）

适合：1 家批发档口、136 级商品量、日订单几十～几百单。

| 资源 | 推荐规格 |
|------|----------|
| CVM | **2 核 4 GB**，系统盘 **50 GB SSD**，带宽 **3～5 Mbps**，地域选离用户近的（如华南广州） |
| 系统 | **OpenCloudOS 8** 或 **Ubuntu 22.04 LTS** |
| MySQL / Redis / RabbitMQ | 与本地相同，**Docker Compose 部署在同一台 CVM** |
| 域名 + SSL | 1 个二级域名 + 免费证书 |
| COS | 标准存储，按量（图片不多时每月几元级） |

**预估费用（参考，以腾讯云官网为准）**：CVM 约 100～300 元/月 + 域名约 50～80 元/年 + COS/流量按量。

### 3.2 稳定生产（推荐）

| 资源 | 推荐规格 |
|------|----------|
| CVM | **4 核 8 GB**，系统盘 50 GB + **数据盘 100 GB**（放 Docker 数据与 uploads） |
| TencentDB MySQL | **1 核 2 GB**，20 GB 存储，自动备份 7 天 |
| TencentDB Redis | **256 MB～1 GB** 标准版 |
| RabbitMQ | 仍在 CVM Docker 内 |
| COS | 私有读写 + 后端 SDK 或预签名 URL（后续改造） |

### 3.3 安全组（防火墙）开放端口

| 端口 | 来源 | 说明 |
|------|------|------|
| 22 | 你的办公 IP | SSH，**不要对 0.0.0.0/0 长期开放** |
| 80 | 0.0.0.0/0 | HTTP，仅用于跳转 HTTPS |
| 443 | 0.0.0.0/0 | HTTPS，小程序访问入口 |
| 8080 | **不对外开放** | 仅本机 Nginx 反代 |
| 3306 / 6379 / 5672 | **不对外开放** | 数据库与 MQ 仅内网或 127.0.0.1 |

---

## 4. 部署前准备清单

- [ ] 腾讯云账号已实名
- [ ] 已注册域名（建议 `.com` / `.cn`，备案用）
- [ ] 微信小程序 AppID、AppSecret（`manifest.json` / 后端 `app.wechat`）
- [ ] 小程序主体：正式经营建议 **个体工商户**（个人主体部分能力受限）
- [ ] 本地能成功运行：`docker compose up` + `mvn spring-boot:run` + 小程序联调
- [ ] 生产环境变量清单（见第 7 节）

---

## 5. 部署步骤

### 5.1 购买并初始化 CVM

1. 控制台 → **云服务器 CVM** → 新建实例，按 §3 选规格。
2. 登录方式：SSH 密钥（推荐）或密码。
3. 绑定**弹性公网 IP**（若未自动分配）。
4. 配置**安全组**（§3.3）。

```bash
# 登录服务器后
sudo yum update -y    # OpenCloudOS/CentOS
# 或 sudo apt update && sudo apt upgrade -y   # Ubuntu

# 安装 Docker（示例：Ubuntu）
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# 安装 Docker Compose 插件
sudo apt install -y docker-compose-plugin

# 安装 Java 21（后端运行）
sudo apt install -y openjdk-21-jre-headless

# 安装 Nginx
sudo apt install -y nginx
```

### 5.2 启动基础设施（MySQL + Redis + RabbitMQ）

**方式 A：与本地一致，Docker Compose（内测推荐）**

```bash
# 在服务器上克隆项目或仅上传 backend/docker
cd /opt/vwholesale
git clone <你的仓库> .
cd backend/docker

# 生产务必修改默认密码！编辑 docker-compose.yml 中 MYSQL/RabbitMQ 密码
docker compose up -d

# 确认容器运行
docker compose ps
```

**方式 B：使用 TencentDB MySQL + Redis**

- 在控制台创建实例，记下**内网地址**、端口、账号密码。
- 安全组允许 **CVM 内网 IP** 访问数据库端口。
- **不再**在 Docker 中启动 mysql/redis 服务，仅保留 RabbitMQ 容器（或整机只跑应用 + MQ）。

初始化数据库（若空库）：

```bash
mysql -h <MySQL地址> -u vwholesale -p -e "CREATE DATABASE IF NOT EXISTS vwholesale CHARACTER SET utf8mb4;"
```

Flyway 会在后端**首次启动**时自动执行 `db/migration` 迁移。

### 5.3 构建并部署后端

在**本地或 CI** 构建：

```bash
cd backend
mvn -DskipTests clean package
# 产物：target/vegetable-wholesale-backend-*.jar
```

上传到服务器：

```bash
scp target/vegetable-wholesale-backend-*.jar user@<CVM_IP>:/opt/vwholesale/app.jar
```

创建生产配置 `/opt/vwholesale/application-prod.yml`（勿提交 Git）：

```yaml
spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/vwholesale?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: vwholesale
    password: <强密码>
  data:
    redis:
      host: 127.0.0.1
      port: 6379
  rabbitmq:
    host: 127.0.0.1
    port: 5672
    username: vwholesale
    password: <强密码>

app:
  mq:
    enabled: true
  wechat:
    app-id: <小程序AppID>
    app-secret: <小程序AppSecret>
  admin:
    openid-whitelist:
      - <老板微信openid>
  upload:
    dir: /opt/vwholesale/uploads

logging:
  level:
    com.vwholesale: info
```

使用 systemd 守护进程：

```bash
sudo tee /etc/systemd/system/vwholesale.service <<'EOF'
[Unit]
Description=Vegetable Wholesale Backend
After=network.target docker.service

[Service]
User=root
WorkingDirectory=/opt/vwholesale
ExecStart=/usr/bin/java -jar /opt/vwholesale/app.jar --spring.profiles.active=prod --spring.config.additional-location=/opt/vwholesale/application-prod.yml
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable vwholesale
sudo systemctl start vwholesale
sudo systemctl status vwholesale
```

验证：

```bash
curl -s http://127.0.0.1:8080/api/health
```

### 5.4 配置 Nginx + HTTPS

1. 控制台申请**免费 SSL 证书**，下载 Nginx 格式，放到 `/etc/nginx/ssl/`。
2. 域名 **A 记录** 解析到 CVM 公网 IP。
3. 备案审核通过后再对外提供 80/443 服务（未备案域名可能被拦截）。

```nginx
# /etc/nginx/sites-available/vwholesale
server {
    listen 80;
    server_name api.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.example.com;

    ssl_certificate     /etc/nginx/ssl/api.example.com.crt;
    ssl_certificate_key /etc/nginx/ssl/api.example.com.key;

    client_max_body_size 20m;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /uploads/ {
        proxy_pass http://127.0.0.1:8080;
    }

    # 可选：关闭公网访问 Swagger
    location /doc.html { return 404; }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/vwholesale /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

### 5.5 对象存储 COS（建议）

1. 控制台 → **对象存储 COS** → 创建存储桶（地域与 CVM 一致，**私有读写**）。
2. 创建子账号 / CAM 密钥，仅授予该桶读写权限。
3. 短期可继续用 CVM 本地 `uploads/`；中期将 `FileUploadService` 改为上传 COS，返回 CDN 或 COS 域名 URL。

当前项目图片路径为 `/uploads/...`，经 Nginx 反代即可被小程序 `downloadFile` / `<image>` 访问；需在小程序后台配置 **downloadFile 合法域名**（与 request 相同或 COS 独立域名）。

### 5.6 构建并发布小程序

```bash
cd frontend

# 生产 API 地址（与 Nginx 域名一致）
echo 'VITE_API_BASE_URL=https://api.example.com' > .env.production

npm install --legacy-peer-deps
npm run build:mp-weixin
```

1. 微信开发者工具打开 `frontend/dist/build/mp-weixin`。
2. 上传代码 → 微信公众平台 → 版本管理 → 提交审核 → 发布。

### 5.7 微信公众平台配置

登录 [微信公众平台](https://mp.weixin.qq.com/) → 开发 → 开发管理 → 开发设置：

| 配置项 | 填写示例 |
|--------|----------|
| request 合法域名 | `https://api.example.com` |
| uploadFile 合法域名 | `https://api.example.com`（或 COS 域名） |
| downloadFile 合法域名 | `https://api.example.com`（商品图片） |

注意：

- 不要带路径，不要带端口（必须 443）。
- 域名必须已备案且证书有效。
- 开发版可在开发者工具勾选「不校验合法域名」；**体验版/正式版必须配置**。

---

## 6. 环境变量一览

| 变量 | 说明 | 示例 |
|------|------|------|
| `WECHAT_APP_SECRET` | 小程序密钥 | 控制台获取 |
| `WECHAT_ORDER_NOTIFY_TEMPLATE_ID` | 订阅消息模板 ID | 可选 |
| `MQ_ENABLED` | 是否启用 RabbitMQ | `true` / `false` |
| `SPRING_PROFILES_ACTIVE` | Spring 环境 | `prod` |
| JDBC / Redis / RabbitMQ | 见 `application-prod.yml` | 生产强密码 |

**生产环境务必关闭** `dev-login` 对外暴露（需在后端增加 `prod`  profile 禁用 `/api/auth/dev-login`，若尚未实现请在上线前处理）。

---

## 7. 数据备份与运维

| 对象 | 建议 |
|------|------|
| MySQL | TencentDB 自动备份；自管则用 `mysqldump` + 定时任务上传 COS |
| Redis | 可重建；重要缓存丢失可接受 |
| uploads / COS | COS 版本控制或定期同步 |
| 应用日志 | `journalctl -u vwholesale -f`；可接入 CLS 日志服务 |
| RabbitMQ | 队列持久化已开启；定期备份 Docker volume |

**发布新版本后端**

```bash
# 本地打包上传覆盖 app.jar
sudo systemctl restart vwholesale
# Flyway 会自动执行新 migration
```

---

## 8. 常见问题

### Q1：端口 8080 已被占用？

同一台机器不要重复启动多个后端。用 `lsof -i :8080` 查进程，保留 systemd 管理的一个实例即可。

### Q2：小程序报「不在合法域名列表」？

检查 HTTPS 域名是否与后台配置一致；正式版必须备案域名。

### Q3：图片 /uploads 访问 404？

确认 `app.upload.dir` 目录存在且 Nginx 反代了 `/uploads/`；上传文件需持久化到数据盘或 COS。

### Q4：RabbitMQ 要不要单独买？

首版不需要。单机 Docker 足够；消息量极大或要多机部署时再评估。

### Q5：H5 内测还要买什么？

H5 静态页可部署在同一 Nginx（`npm run build:h5` → `dist/build/h5`），与 API 同域最省事，**不额外购买产品**。

---

## 9. 推荐采购顺序（实操）

1. **注册域名** → 提交 **ICP 备案**（与 CVM 同账号，约 1～3 周）
2. 购买 **CVM** + 配置安全组
3. 申请 **SSL 证书**
4. 部署 Docker（MySQL/Redis/RabbitMQ）+ 后端 JAR + Nginx
5. 解析域名、开启 HTTPS、冒烟测试 `https://api.example.com/api/health`
6. 配置 **微信小程序合法域名**
7. 构建 **mp-weixin** 上传审核
8. （建议）开通 **COS**，规划图片迁移

---

## 10. 相关项目文档

- [技术架构](./technical-architecture.md) — 部署与环境说明 §10
- [开工清单](./pre-development-checklist.md) — 域名、备案、HTTPS
- [后端 README](../backend/README.md) — 本地 Docker 与 MQ 说明
- [根目录 README](../README.md) — 本地快速开始

---

## 11. 后续可补充的工程项（非购买项）

上线前建议在代码库中补齐（当前仓库尚未包含）：

- `backend/Dockerfile` 或多阶段构建镜像
- `application-prod.yml` 模板（敏感项走环境变量）
- 生产禁用 `dev-login` 的配置开关
- COS 上传适配
- CI/CD（GitHub Actions / 腾讯云 CODING 构建 jar 并 SSH 发布）

如需，可在下一轮迭代中按本文档逐项落地脚本与配置文件。
