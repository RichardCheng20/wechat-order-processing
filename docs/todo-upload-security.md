# TODO：上传文件商户隔离与访问鉴权

> 状态：**待做**（多商户正式上线前完成）  
> 背景：当前 `/uploads/**` 静态公开、文件未按商户分目录，知道 URL 即可访问。

---

## 现状问题

- 上传路径：`uploads/{uuid}.png`，全商户混放
- 访问方式：`GET /uploads/**`，无登录、无 `merchant_id` 校验
- 小程序 `<image>` 无法方便携带 `Authorization` 头

业务 API 已按 `merchant_id` 隔离，**静态文件通道仍公开**。

---

## 目标

- 多商户磁盘隔离
- 猜 URL 无法访问
- 客户不能看别家对账单/凭证
- 兼容小程序 `<image>` / `downloadFile`

---

## 阶段一：目录按商户隔离

- [ ] `FileUploadService` 改为写入 `uploads/m{merchantId}/{type}/`
- [ ] `type` 区分：`products` | `statements` | `vouchers`
- [ ] DB 存相对路径，如 `/uploads/m1/statements/xxx.png`
- [ ] 上传入口区分类型（商品图 / 对账单 / 收款凭证）

**目录示例：**

```text
uploads/
  m1/products/136-土豆.jpg
  m1/statements/a1b2c3....png
  m1/vouchers/d4e5f6....png
  m2/...
```

> 仅做本阶段仍无法防 URL 泄露，须配合阶段二。

---

## 阶段二：关闭静态映射 + 签名下载 API（核心）

- [ ] 移除 `WebMvcConfig` 中 `/uploads/**` 的 `ResourceHandler` 与 exclude 规则
- [ ] 新增 `FileDownloadController`：`GET /api/files/**`
- [ ] 新增 `SignedUrlHelper`：HMAC 生成/校验 `sign` + `exp`（建议有效期 15～60 分钟）
- [ ] 新增 `FileAccessService`：校验签名 + 资源归属
- [ ] 配置项：`app.upload.sign-secret`（生产环境独立密钥）
- [ ] Nginx 只反代 `/api/`，不再直出 `/uploads/`
- [ ] 各 VO 返回**带签名的完整 URL**（前端改动最小）

**签名 URL 示例：**

```text
https://api.52laicai.cn/api/files/m1/statements/xxx.png?exp=1719234567&sign=...
```

### 归属校验规则

| 资源类型 | 允许访问 |
|----------|----------|
| `products/` | 同商户老板；同商户客户（商品图） |
| `statements/` | 同商户老板；**仅该订单所属客户** |
| `vouchers/` | **仅同商户老板** |

---

## 阶段三：兼容与迁移

- [ ] 旧路径 fallback：`/uploads/products/`、`/uploads/{uuid}.png`
- [ ] 脚本或 Flyway：批量更新 DB 中 `image_url`、`statement_image_url`、`voucher_urls`
- [ ] 可选：物理文件搬迁至 `m1/`（单店 `merchant_id=1`）

---

## 阶段四（可选）：COS 私有桶

- [ ] 上传改 COS，key 仍用 `m{id}/{type}/...`
- [ ] 鉴权通过后生成 COS 预签名 URL
- [ ] 逻辑与阶段二一致，仅替换存储层

---

## 实施优先级

| 时机 | 内容 |
|------|------|
| 单店内测 | 可暂缓 |
| **多商户上线前** | **必做阶段二 + 阶段三** |
| 图片量增大后 | 阶段四（COS） |

---

## 涉及模块（预估）

| 层 | 模块 |
|----|------|
| 后端 | `FileUploadService`、`FileDownloadController`（新）、`SignedUrlHelper`（新）、`FileAccessService`（新）、`WebMvcConfig`、各 VO URL 组装 |
| 配置 | `application-prod.yml` → `app.upload.sign-secret` |
| 前端 | 若后端返回完整签名 URL → 基本不改；否则改 `resolveMediaUrl` |
| 部署 | Nginx 去掉 `/uploads/` 直出；`deploy-prod.sh` 常规发布不受影响 |

---

## 相关文档

- [腾讯云上线分步指南](./腾讯云上线分步操作指南.md)
- [腾讯云上云部署指南](./腾讯云上云部署指南.md) — COS 规划

---

## 已完成（相关）

- [x] `deploy-prod.sh` 仅同步 `uploads/products/`，不再全量 `--delete` 覆盖
- [x] `scripts/clean-upload-temp.sh` 清理根目录临时测试图
