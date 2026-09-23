<script setup>
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Flame, Menu, Search } from "lucide-vue-next";
import { state } from "../../stores/appState";

// 상단 브랜드/검색/메인 메뉴만 담당합니다. 모바일 사이드바 열기는 부모(App.vue)에 요청합니다.
const emit = defineEmits(["toggle-menu"]);
const route = useRoute();
const router = useRouter();
const search = ref("");
const isChat = computed(() => route.path.startsWith("/chat"));

function searchPosts() {
  router.push({ path: "/", query: { q: search.value.trim() || undefined } });
}
</script>

<template>
  <header class="site-header">
    <div class="top-line">
      <div class="shell top-inner">
        <span><b>JOJI</b> 오늘도, 하고 싶은 이야기.</span>
        <span class="top-note">가입 없이 만나는 익명 커뮤니티</span>
      </div>
    </div>

    <div class="shell brand-row">
      <RouterLink class="brand" to="/">
        <span class="brand-mark">J.</span>
        <span>JOJI <strong>COMMINITY</strong></span>
      </RouterLink>

      <form class="global-search" @submit.prevent="searchPosts">
        <Search :size="19" />
        <input
          v-model="search"
          aria-label="게시글 통합 검색"
          maxlength="100"
          placeholder="어떤 이야기를 찾고 있나요?"
        />
        <button type="submit">검색</button>
      </form>

      <div class="identity">
        <span class="avatar">익</span>
        <div>
          <b>{{ state.nickname }}</b>
          <span>
            익명 활동 중 <small v-if="state.tag">#{{ state.tag }}</small>
          </span>
        </div>
      </div>

      <button
        class="icon-button mobile-menu"
        aria-label="갤러리 메뉴"
        @click="emit('toggle-menu')"
      >
        <Menu />
      </button>
    </div>

    <nav class="main-nav">
      <div class="shell nav-inner">
        <RouterLink
          to="/"
          :class="{ active: route.path === '/' && !route.query.sort }"
        >
          갤러리 홈
        </RouterLink>
        <RouterLink
          to="/?sort=hot"
          :class="{ active: route.query.sort === 'hot' }"
        >
          <Flame :size="16" /> 인기글
        </RouterLink>
        <RouterLink
          to="/?sort=best"
          :class="{ active: route.query.sort === 'best' }"
        >
          개념글 모음
        </RouterLink>
        <RouterLink to="/chat" :class="{ active: isChat }">
          실시간 채팅방 <span class="live-tag">LIVE</span>
        </RouterLink>
        <span class="nav-end">YOUR EVERYDAY COMMUNITY</span>
      </div>
    </nav>
  </header>
</template>
