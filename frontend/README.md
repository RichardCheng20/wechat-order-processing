# 蔬菜批发小程序前端

标准 uni-app + Vue 3 + TypeScript + Pinia + uView Plus

## 开发

```bash
cd frontend
npm install --legacy-peer-deps
npm run dev:mp-weixin
```

编译产物在 `frontend/dist/dev/mp-weixin`，用**微信开发者工具**打开该目录。

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
