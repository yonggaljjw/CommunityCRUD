import { createApp } from "vue";
import App from "./App.vue";
import { router } from "./router/index.js";
import "./style.css";

// Vue 앱을 생성하고 라우터를 연결한 뒤 index.html의 #app에 마운트합니다.
createApp(App).use(router).mount("#app");
