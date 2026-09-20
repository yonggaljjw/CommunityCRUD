<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  Search,
  MessageCircle,
  PenLine,
  Hash,
  ArrowUpRight,
  LockKeyhole,
  Users,
  Menu,
  X,
  Flame,
  LayoutGrid,
  Code2,
  BriefcaseBusiness,
  Gamepad2,
  Coffee,
  Smile,
  Radio,
} from "lucide-vue-next";
import { state, bootstrap } from "./state";
const route = useRoute(),
  router = useRouter(),
  search = ref(""),
  mobileMenu = ref(false);
const icons = [Coffee, Code2, BriefcaseBusiness, Gamepad2, Smile, Hash];
const isChat = computed(() => route.path.startsWith("/chat"));
const totalPosts = computed(() =>
  state.galleries.reduce((sum, g) => sum + g.postCount, 0),
);
function searchPosts() {
  router.push({ path: "/", query: { q: search.value.trim() } });
}
onMounted(bootstrap);
</script>
<template>
  <header class="site-header">
    <div class="top-line">
      <div class="shell top-inner">
        <span><b>JOJI</b> 오늘도, 하고 싶은 이야기.</span
        ><span class="top-note">가입 없이 만나는 익명 커뮤니티</span>
      </div>
    </div>
    <div class="shell brand-row">
      <RouterLink class="brand" to="/"
        ><span class="brand-mark">J.</span
        ><span>JOJI <strong>COMMINITY</strong></span></RouterLink
      >
      <form class="global-search" @submit.prevent="searchPosts">
        <Search :size="19" /><input
          v-model="search"
          aria-label="게시글 통합 검색"
          maxlength="100"
          placeholder="어떤 이야기를 찾고 있나요?"
        /><button type="submit">검색</button>
      </form>
      <div class="identity">
        <span class="avatar">익</span>
        <div>
          <b>{{ state.nickname }}</b
          ><span
            >익명 활동 중 <small v-if="state.tag">#{{ state.tag }}</small></span
          >
        </div>
      </div>
      <button
        class="icon-button mobile-menu"
        aria-label="갤러리 메뉴"
        @click="mobileMenu = !mobileMenu"
      >
        <Menu />
      </button>
    </div>
    <nav class="main-nav">
      <div class="shell nav-inner">
        <RouterLink
          to="/"
          :class="{ active: route.path === '/' && !route.query.sort }"
          >갤러리 홈</RouterLink
        ><RouterLink
          to="/?sort=hot"
          :class="{ active: route.query.sort === 'hot' }"
          ><Flame :size="16" /> 인기글</RouterLink
        ><RouterLink
          to="/?sort=best"
          :class="{ active: route.query.sort === 'best' }"
          >개념글 모음</RouterLink
        ><RouterLink to="/chat" :class="{ active: isChat }"
          >실시간 채팅방 <span class="live-tag">LIVE</span></RouterLink
        ><span class="nav-end">YOUR EVERYDAY COMMUNITY</span>
      </div>
    </nav>
  </header>
  <div v-if="!state.ready" class="shell connection-state">
    <h2>{{ state.error || "이야기를 불러오고 있어요." }}</h2>
    <button v-if="state.error" class="primary" @click="bootstrap">
      다시 연결
    </button>
  </div>
  <div v-else class="shell app-layout" :class="{ 'chat-layout': isChat }">
    <aside class="left-sidebar" :class="{ 'show-mobile': mobileMenu }">
      <div class="side-heading">
        GALLERIES
        <button
          class="icon-button mobile-menu"
          @click="mobileMenu = false"
          aria-label="메뉴 닫기"
        >
          <X :size="18" />
        </button>
      </div>
      <RouterLink
        to="/"
        class="side-link"
        :class="{ selected: route.path === '/' }"
        @click="mobileMenu = false"
        ><LayoutGrid :size="18" /> 전체 갤러리
        <span>{{ totalPosts }}</span></RouterLink
      >
      <RouterLink
        v-for="(g, i) in state.galleries"
        :key="g.id"
        :to="`/galleries/${g.id}`"
        class="side-link"
        :class="{
          selected:
            route.params.id == g.id && route.path.startsWith('/galleries'),
        }"
        @click="mobileMenu = false"
        ><component :is="icons[i % icons.length]" :size="18" /> {{ g.name }}
        <span>{{ g.postCount }}</span></RouterLink
      >
      <div class="side-divider"></div>
      <div class="side-heading">LIVE LOUNGE <Radio :size="15" /></div>
      <RouterLink
        v-for="r in state.rooms.slice(0, 4)"
        :key="r.id"
        :to="`/chat/${r.id}`"
        class="side-room"
        @click="mobileMenu = false"
        ><span># {{ r.name }}</span
        ><LockKeyhole v-if="r.locked" :size="13"
      /></RouterLink>
      <div class="side-note">
        <MessageCircle :size="24" /><b>같은 관심사, 새로운 대화</b>
        <p>글로 남기고, 채팅으로 이어가세요.</p>
        <RouterLink to="/chat"
          >채팅방 둘러보기 <ArrowUpRight :size="15"
        /></RouterLink>
      </div>
    </aside>
    <main id="main-content"><RouterView :key="route.path" /></main>
    <aside v-if="!isChat" class="right-sidebar">
      <section class="panel lounge-card">
        <div class="panel-heading">
          <h2><Radio :size="18" /> 실시간 라운지</h2>
          <span class="live-label">LIVE</span>
        </div>
        <p class="muted">지금, 함께 이야기해요.</p>
        <RouterLink
          v-for="r in state.rooms.slice(0, 4)"
          :key="r.id"
          :to="`/chat/${r.id}`"
          class="room-link"
          ><span class="room-symbol">#</span>
          <div>
            <b>{{ r.name }}</b
            ><span
              >{{ r.category }} <LockKeyhole v-if="r.locked" :size="12"
            /></span>
          </div>
          <span class="online-number"
            ><Users :size="13" />{{ r.online }}</span
          ></RouterLink
        ><RouterLink to="/chat" class="room-footer"
          >모든 채팅방 보기 <ArrowUpRight :size="15"
        /></RouterLink>
      </section>
      <section class="community-note">
        <span class="eyebrow">JOJI COMMUNITY NOTE</span>
        <h3>익명이니까,<br />더 편하게. 더 존중하며.</h3>
        <p>개인정보를 올리지 말고,<br />서로의 이야기를 존중해 주세요.</p>
        <div class="note-line"></div>
        <span>당신의 이야기를 기다립니다.</span>
      </section>
      <RouterLink to="/write" class="write-callout"
        ><PenLine :size="18" /> 첫 문장부터 시작해요 <ArrowUpRight :size="16"
      /></RouterLink>
    </aside>
  </div>
  <footer class="shell site-footer">
    <b>JOJI COMMINITY</b><span>이야기가 모이고, 대화가 이어지는 곳.</span
    ><span>© {{ new Date().getFullYear() }} JOJI</span>
  </footer>
  <div v-if="state.toast" class="toast" role="status">{{ state.toast }}</div>
</template>
