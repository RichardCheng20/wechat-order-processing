import { defineConfig } from "vite";
import uni from "@dcloudio/vite-plugin-uni";
import path from "path";

const API_TARGET = process.env.VITE_API_PROXY_TARGET || "http://127.0.0.1:8080";

export default defineConfig({
  resolve: {
    alias: {
      "@common": path.resolve(__dirname, "src/shared"),
    },
  },
  plugins: [uni()],
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@import "${path.resolve(__dirname, "node_modules/uview-plus/theme.scss")}";`,
      },
    },
  },
  server: {
    host: true,
    port: 5173,
    proxy: {
      "/api": {
        target: API_TARGET,
        changeOrigin: true,
      },
      "/uploads": {
        target: API_TARGET,
        changeOrigin: true,
      },
    },
  },
});
