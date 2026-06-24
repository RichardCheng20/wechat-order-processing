# 腾讯云上线分步操作指南

> 适用：蔬菜批发微信小程序（轻量应用服务器 + 单店首版）  
> 服务器公网 IP：`119.29.184.221`（广州）  
> 详细架构与配置见 [tencent-cloud-deployment.md](./tencent-cloud-deployment.md)

---

## 总路线图

```text
第1步  注册域名 + DNS 解析          ← 域名已购，待完成 api 解析
第2步  提交 ICP 备案                 ← 当前可做（与第3步并行，约 1～3 周）
第3步  服务器装环境 + Docker + 后端   ← 备案期间可做
第4步  备案通过后：免费 SSL + Nginx HTTPS
第5步  微信公众平台合法域名 + 小程序发版
```

**当前进度（2026-06-17）**

- ✅ 主域名 `52laicai.cn` 已购买，实名审核已通过
- ✅ `api.52laicai.cn` A 记录 → `119.29.184.221`（已生效）
- ⬜ ICP 备案审核中
- ✅ 第 3 步：服务器环境 + Docker + 后端

**说明**

- **备案**和**装服务器**可以同时进行。
- **HTTPS** 和**微信合法域名**必须等 **ICP 备案通过**。
- 备案期间可在服务器上部署后端；详见 **[开发联调与生产部署指南](./dev-remote-testing.md)**（SSH 隧道、deploy-prod、本机/远程切换）。
- **SSL**：用腾讯云 [免费 DV 证书](https://cloud.tencent.com/document/product/400/6814)，不必搭购付费证书。
- **COS**：按需付费即可，首版图片可继续放服务器 `uploads/`。

---

## 已购资源核对

| 项 | 你的选择 |
|----|----------|
| 规格 | 2核4G6M |
| 系统盘 | 70GB SSD |
| 流量 | 600GB/月 |
| 地域 | 广州 |
| 镜像 | Ubuntu 22.04 LTS |
| 搭购 SSL | 不需要（用免费证书） |
| 搭购 COS 包年 | 不需要（按需） |
| 主域名 | **52laicai.cn**（实名已通过） |
| API 域名 | **api.52laicai.cn** |

---

## 第 1 步：注册域名 + 解析

### 1.1 注册主域名 ✅ 已完成

- 主域名：**52laicai.cn**
- 实名审核：**已通过**
- API 地址（最终）：**https://api.52laicai.cn**

### 1.2 添加 DNS 解析 ← 当前待做

轻量控制台 → **域名解析** → **添加域名解析**：

| 字段 | 填什么 |
|------|--------|
| 主机记录 | `api` |
| 记录类型 | **A** |
| 记录值 | `119.29.184.221` |
| TTL | 默认 |

效果：`api.52laicai.cn` → `119.29.184.221`

### 1.3 验证解析

在本机 Mac 终端：

```bash
ping api.52laicai.cn
# 或
nslookup api.52laicai.cn
```

应返回 `119.29.184.221`（DNS 生效可能需 5～30 分钟）。

### 第 1 步完成检查

- [x] 主域名 `52laicai.cn` 已购买并完成实名
- [x] `api` 的 A 记录已指向 `119.29.184.221`
- [x] `ping api.52laicai.cn` 解析正确

---

## 第 2 步：ICP 备案 ← 当前可做

1. 登录 [腾讯云备案控制台](https://console.cloud.tencent.com/beian)
2. 选择 **网站/域名**（不要选 APP）
3. 输入主域名 **`52laicai.cn`**，点击 **去备案**
4. 关联 **轻量应用服务器**（广州，`119.29.184.221`）
5. 按向导填写：主体信息（个人/个体户）、网站名称、核验照片等
6. 提交后等待管局审核（约 **1～3 周**）

**注意**

- 备案填 **主域名** `52laicai.cn`，子域名 `api.52laicai.cn` 随主域名一并生效，无需单独备案。
- 域名、服务器、备案需在**同一腾讯云账号**下最省事。
- 备案通过前，用域名对外访问可能被拦截；不影响在服务器上装环境。
- 小程序 **体验版/正式版** 必须备案 + HTTPS 合法域名。

---

## 第 3 步：服务器环境 + Docker + 后端

> 可与第 2 步备案**并行**进行。

### 3.1 SSH 登录（推荐密钥，长期方便）

**首次**：轻量控制台 → **终端连接 (SSH)**，用户名 `ubuntu`，密码见站内信。

**本机配置密钥（推荐）**

```bash
# 本机 Mac 生成密钥
ssh-keygen -t ed25519 -C "vwholesale-prod" -f ~/.ssh/vwholesale_lighthouse

# 登录服务器后写入公钥
mkdir -p ~/.ssh && chmod 700 ~/.ssh
echo "你的公钥内容" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

**本机 `~/.ssh/config`**

```sshconfig
Host vwholesale
    HostName 119.29.184.221
    User ubuntu
    IdentityFile ~/.ssh/vwholesale_lighthouse
```

之后：`ssh vwholesale`

### 3.2 防火墙（轻量控制台 → 防火墙）

| 端口 | 来源 | 说明 |
|------|------|------|
| 22 | 你的办公 IP | SSH |
| 80 | 0.0.0.0/0 | HTTP（跳转 HTTPS） |
| 443 | 0.0.0.0/0 | HTTPS，小程序入口 |
| 8080 / 3306 / 6379 / 5672 | **不开放** | 仅本机 |

### 3.3 安装基础软件

```bash
sudo apt update && sudo apt upgrade -y

# Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
# 重新登录 SSH 后生效

sudo apt install -y docker-compose-plugin

# Java 21
sudo apt install -y openjdk-21-jre-headless

# Nginx
sudo apt install -y nginx
```

### 3.4 启动 MySQL / Redis / RabbitMQ

```bash
sudo mkdir -p /opt/vwholesale
cd /opt/vwholesale

# 克隆仓库（或 scp 上传 backend/docker 目录）
git clone <你的仓库地址> .
cd backend/docker

# ⚠️ 生产务必修改 docker-compose.yml 中的默认密码！
docker compose up -d
docker compose ps
```

默认端口（仅本机）：MySQL 3306、Redis 6379、RabbitMQ 5672。

### 3.5 部署后端 JAR

**本地打包**

```bash
cd backend
mvn -DskipTests clean package
# 产物：target/vegetable-wholesale-backend-*.jar
```

**上传到服务器**

```bash
scp target/vegetable-wholesale-backend-*.jar vwholesale:/opt/vwholesale/app.jar
```

**创建生产配置** `/opt/vwholesale/application-prod.yml`（勿提交 Git）

```yaml
spring:
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

**systemd 守护进程**

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

**验证（服务器上）**

```bash
curl -s http://127.0.0.1:8080/api/health
```

### 3.6 上传商品图片（可选，与本地一致）

```bash
# 本机
scp -r backend/uploads vwholesale:/opt/vwholesale/
```

---

## 第 4 步：免费 SSL + Nginx HTTPS

> **必须等 ICP 备案通过后**再对外提供 443。

### 4.1 申请免费 SSL

1. [SSL 证书控制台](https://console.cloud.tencent.com/ssl) → **申请免费证书**
2. 绑定域名：`api.52laicai.cn`
3. 验证方式：DNS 自动验证（域名在腾讯云 DNS 最方便）
4. 签发后下载 **Nginx** 格式，上传到服务器 `/etc/nginx/ssl/`

### 4.2 Nginx 配置

```bash
sudo mkdir -p /etc/nginx/ssl
# 上传 api.52laicai.cn.crt 和 api.52laicai.cn.key 到 /etc/nginx/ssl/

sudo tee /etc/nginx/sites-available/vwholesale <<'EOF'
server {
    listen 80;
    server_name api.52laicai.cn;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.52laicai.cn;

    ssl_certificate     /etc/nginx/ssl/api.52laicai.cn.crt;
    ssl_certificate_key /etc/nginx/ssl/api.52laicai.cn.key;

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

    location /doc.html { return 404; }
}
EOF

sudo ln -sf /etc/nginx/sites-available/vwholesale /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

### 4.3 验证 HTTPS

```bash
curl -s https://api.52laicai.cn/api/health
```

---

## 第 5 步：微信小程序配置 + 发版

### 5.1 微信公众平台合法域名

登录 [微信公众平台](https://mp.weixin.qq.com/) → 开发 → 开发管理 → 开发设置：

| 配置项 | 填写 |
|--------|------|
| request 合法域名 | `https://api.52laicai.cn` |
| uploadFile 合法域名 | `https://api.52laicai.cn` |
| downloadFile 合法域名 | `https://api.52laicai.cn` |

- 不要带路径、不要带端口
- 域名必须已备案且证书有效

### 5.2 构建小程序

```bash
cd frontend

# 生产 API 地址
echo 'VITE_API_BASE_URL=https://api.52laicai.cn' > .env.production

npm install --legacy-peer-deps
npm run build:mp-weixin
```

1. 微信开发者工具打开 `frontend/dist/build/mp-weixin`
2. 上传代码 → 微信公众平台 → 版本管理 → 提交审核 → 发布

---

## 进度清单（可打印勾选）

```
阶段一 · 域名
[x] 注册主域名 52laicai.cn 并完成实名
[ ] api A 记录 → 119.29.184.221
[ ] ping api.52laicai.cn 解析正确

阶段二 · 备案
[ ] 提交 ICP 备案（填 52laicai.cn，选「网站/域名」）
[ ] 备案审核通过

阶段三 · 服务器（可与备案并行）
[ ] SSH 密钥登录配置
[ ] 防火墙 22/80/443
[ ] Docker + Java 21 + Nginx
[ ] docker compose up（MySQL/Redis/RabbitMQ）
[ ] application-prod.yml + systemd
[ ] curl http://127.0.0.1:8080/api/health 正常

阶段四 · HTTPS（备案后）
[ ] 免费 SSL 申请并部署
[ ] Nginx HTTPS 配置
[ ] curl https://api.52laicai.cn/api/health 正常

阶段五 · 小程序
[ ] 微信合法域名三项（api.52laicai.cn）
[ ] .env.production → https://api.52laicai.cn
[ ] build:mp-weixin 上传审核
```

---

## 常见问题速查

| 问题 | 处理 |
|------|------|
| 小程序「不在合法域名列表」 | 检查 HTTPS 域名与后台配置一致；正式版必须备案 |
| 8080 被占用 | `lsof -i :8080`，只保留 systemd 一个实例 |
| 图片 /uploads 404 | 确认 `/opt/vwholesale/uploads` 存在且 Nginx 反代了 `/uploads/` |
| 4GB 内存紧张 | MySQL buffer pool 512M～768M；Docker 日志设大小上限 |

---

## 相关文档

- [开发联调与生产部署指南](./dev-remote-testing.md) — SSH 隧道、deploy-prod、本机/远程切换
- [腾讯云上云部署指南（完整版）](./tencent-cloud-deployment.md)
- [TODO：上传文件鉴权](./todo-upload-security.md)
- [免费 SSL 申请流程](https://cloud.tencent.com/document/product/400/6814)
- [后端 README](../backend/README.md)

---

## 我的信息（填写备用）

| 项 | 内容 |
|----|------|
| 主域名 | 52laicai.cn |
| API 域名 | api.52laicai.cn |
| 备案号 | （待备案通过后填写） |
| 小程序 AppID | wxd056da1506f00ca1 |
| 服务器 IP | 119.29.184.221 |
| SSH 别名 | vwholesale |
