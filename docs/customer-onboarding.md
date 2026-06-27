# 客户入驻与三种码

> 产品上下文见 [PRD v1.0 §5](./vegetable-wholesale-miniapp-prd.md#5-客户入驻)。  
> 老板端入口与 API 以当前代码为准。

## 概览

| 类型 | 老板入口 | 客户路径 | 用途 |
|------|----------|----------|------|
| **档口下单码** | 我的 → 档口下单码 | 扫太阳码 → 登录 → 直接选购 | 路人/临时客户，无需预先建档 |
| **VIP 专属码** | 客户管理 → 某客户 → VIP专属码 | 我的 → 成为 VIP 客户 → 输入 8 位码 | 已建档合作客户绑定微信 |
| **注册邀请** | 客户管理 → 邀请注册 | 扫太阳码 → 填资料 → 待审核 | 新客户申请档案，老板审核后开通 |

## 1. 档口长期下单码

- **API**：`GET /api/boss/merchant/order-qrcode`
- **微信 scene**：`m={merchantId}`（无过期）
- **页面**：`pages/boss/stall-qrcode/index`
- **客户行为**：扫码进入 `pages/launch/index`，保存档口 ID 后登录；未绑定档案时可临时下单（需填店铺名称）

本地联调（开发者工具编译模式）：

```text
pages/launch/index?m=1
```

## 2. VIP 专属码

- **API**：`POST /api/boss/customers/{id}/invite`（已有有效码则直接返回，不重复生成）
- **规则**：绑定前长期有效（`inviteExpiredAt` 为空）；绑定成功后码作废
- **客户 API**：`POST /api/customer/bind`，body `{ "inviteCode": "XXXXXXXX" }`
- **页面**：`pages/customer/bind/index`（导航标题：成为 VIP 客户）

## 3. 注册邀请（需审核）

- **API**：`GET/POST /api/boss/customers/register-invite`
- **微信 scene**：`m={merchantId},r={token}`（默认 7 天有效，可重新生成）
- **客户流程**：扫码 → `pages/customer/register/index` 提交 → 老板在「待审核」通过/拒绝
- **默认规则**：审核通过后「下单后自动确认」默认关闭；拒绝后允许再次提交

本地联调：

```text
pages/launch/index?m=1&r=TOKEN
```

## 微信太阳码配置

生成太阳码需配置微信 AppSecret。本地开发：

1. 复制 `backend/src/main/resources/application-dev-local.example.yml` → `application-dev-local.yml`（已 gitignore）
2. 填入真实 `app-secret`，未发正式版时设 `env-version: develop`
3. 重启后端

未配置时仍可用 dev-login 联调；太阳码接口会返回 `qrErrorHint` 说明原因。

## 真机调试

- 模拟器：API 默认 `http://127.0.0.1:8080`
- 真机：与开发机同一 Wi-Fi，在登录页填写电脑局域网 IP（如 `192.168.x.x:8080`）
- 扫太阳码：需小程序已发布体验版/开发版，或开发者工具预览；备案 HTTPS 上线后体验更稳定
