import { createApp } from "vue";
import { createRouter, createWebHistory } from "vue-router";
import App from "./App.vue";
import BoardView from "./views/BoardView.vue";
import PostView from "./views/PostView.vue";
import WriteView from "./views/WriteView.vue";
import ChatView from "./views/ChatView.vue";
import "./style.css";

// URL이 화면을 결정합니다. 새로고침/뒤로 가기를 지원하고 Nginx가 하위 경로도 index.html로 연결합니다.
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", component: BoardView },
    { path: "/galleries/:id", component: BoardView },
    { path: "/posts/:id", component: PostView },
    { path: "/write", component: WriteView },
    { path: "/posts/:id/edit", component: WriteView },
    { path: "/chat/:id?", component: ChatView },
    {
      path: "/:pathMatch(.*)*",
      component: {
        template:
          '<div class="empty"><h2>페이지를 찾을 수 없습니다.</h2><a href="/">홈으로 돌아가기</a></div>',
      },
    },
  ],
  scrollBehavior(to, from, saved) {
    return saved || { top: 0 };
  },
});
createApp(App).use(router).mount("#app");
