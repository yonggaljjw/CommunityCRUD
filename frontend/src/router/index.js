import { createRouter, createWebHistory } from "vue-router";
import BoardView from "../features/board/BoardView.vue";
import PostView from "../features/board/PostView.vue";
import WriteView from "../features/board/WriteView.vue";
import ChatView from "../features/chat/ChatView.vue";

/**
 * URL과 화면 컴포넌트의 대응표입니다.
 * 라우터를 main.js와 분리해 '앱 시작'과 '화면 경로 설정'의 책임을 나눕니다.
 */
export const router = createRouter({
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
  // 뒤로 가기는 이전 스크롤 위치를 복구하고, 새 화면 이동은 맨 위부터 보여 줍니다.
  scrollBehavior(to, from, savedPosition) {
    return savedPosition || { top: 0 };
  },
});
