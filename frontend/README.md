# 蔬菜批发小程序前端

标准 uni-app + Vue 3 + TypeScript + Pinia + uView Plus

## 开发

```bash
cd frontend
npm install --legacy-peer-deps
npm run dev:mp-weixin
```

编译产物在 `frontend/dist/dev/mp-weixin`，用**微信开发者工具**打开该目录（日常开发调试）。

## 上传发布

**务必使用生产构建**，不要用 `dist/dev`：

```bash
cd frontend
npm run build:mp-weixin
```

用微信开发者工具打开 **`frontend/dist/build/mp-weixin`**，再点「上传」。

生产包已配置：JS 压缩、组件按需注入、分包（主包仅启动页 + vendor，公共模块在独立分包 `packages/common`）。

## 配置

编辑 `src/utils/config.ts`：

- `API_BASE_URL`：后端地址（本地默认 `http://localhost:8080`）
- 微信小程序 AppID：在 `src/manifest.json` 的 `mp-weixin.appid` 中填写

## 目录

```text
frontend/src/
  pages/         页面（login / customer / boss / worker）
  components/    公共组件
  stores/        Pinia 状态
  api/           接口封装
  utils/         工具与配置
```

## HBuilderX

也可使用 HBuilderX 打开 `frontend/` 目录运行到微信开发者工具。
