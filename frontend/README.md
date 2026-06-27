# 蔬菜批发小程序前端

标准 uni-app + Vue 3 + TypeScript + Pinia + uView Plus

支持：**微信小程序**（主）、**H5**（内测/Dev 登录）

## 开发

```bash
cd frontend
npm install --legacy-peer-deps
npm run dev:mp-weixin    # 微信小程序
npm run dev:h5           # 浏览器 H5 → http://localhost:5173
```

编译产物：

- 小程序开发：`dist/dev/mp-weixin` → **微信开发者工具打开此目录**
- 小程序生产：`dist/build/mp-weixin` → 上传发布用

## 构建与发布

**上传微信务必用生产构建**，不要用 `dist/dev`：

```bash
npm run build:mp-weixin
```

H5 生产：

```bash
# 构建前在 .env.production 设置 VITE_API_BASE_URL
npm run build:h5
npm run preview:h5   # 本地预览 dist/build/h5
```

生产包：JS 压缩、分包（主包启动页 + vendor，公共模块在 `packages/common` 分包）。

## 配置

| 项 | 位置 |
|----|------|
| API 基址 | `src/shared/utils/config.ts`（H5 开发通常走 Vite 代理，留空即可） |
| 代理目标 | 环境变量 `VITE_API_PROXY_TARGET`，默认 `http://127.0.0.1:8080` |
| 小程序 AppID | `src/manifest.json` → `mp-weixin.appid` |

路径别名：`@common` → `src/shared`（见 `vite.config.ts`）

## 目录

```text
frontend/src/
  pages/              页面分包
    launch/           扫码入口（scene / m / r 参数）
    login/            登录 + Dev 快捷账号
    customer/         客户端：下单、购物车、订单、VIP 绑定、注册
    boss/             老板端：订单、商品、客户、采购、报表、档口码等
    worker/           工人端：拣单任务
  components/         公共组件（TabBar、OrderFlowBar、AppIcon 等）
  shared/
    api/              接口封装（request、customer、order…）
    stores/           Pinia（user、cart、salesOrder…）
    utils/            配置、租户入口、订单流转、打印等
  styles/             全局样式片段
  static/             静态资源
```

## 三端入口

登录后按角色跳转：

| 角色 | 首页 |
|------|------|
| 老板 / 档口老板 / 档口经理 | `/pages/boss/orders/index` |
| 客户 | `/pages/customer/home/index` |
| 配送员 | `/pages/worker/tasks/index` |

扫码入口统一经 `pages/launch/index`（解析 `m=` 档口、`r=` 注册 token 等，见 `shared/utils/tenant.ts`）。

## H5 与小程序差异

- H5 无微信登录 → 登录页 Dev 按钮（`dev-owner-001` 等）
- H5 开发：`/api` 代理到本机 8080
- 太阳码、订阅消息等仅小程序环境完整可用

## 类型检查

```bash
npm run type-check
```

## HBuilderX

也可使用 HBuilderX 打开 `frontend/` 运行到微信开发者工具。
