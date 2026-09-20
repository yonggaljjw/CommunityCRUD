import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
// 개발 중에도 /api, /ws를 같은 주소로 요청합니다. 쿠키와 WebSocket 연결을 함께 프록시합니다.
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      "/api": { target: "http://localhost:8080" },
      "/ws": { target: "ws://localhost:8080", ws: true },
    },
  },
});
