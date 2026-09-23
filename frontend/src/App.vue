<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import SiteHeader from "./components/layout/SiteHeader.vue";
import LeftSidebar from "./components/layout/LeftSidebar.vue";
import RightSidebar from "./components/layout/RightSidebar.vue";
import SiteFooter from "./components/layout/SiteFooter.vue";
import { bootstrap, state } from "./stores/appState";

/**
 * App.vue는 이제 '페이지 조립'만 담당합니다.
 * 실제 헤더/사이드바 기능은 components/layout, 게시판/채팅 기능은 features 아래에 있습니다.
 */
const route = useRoute();
const mobileMenu = ref(false);
const isChat = computed(() => route.path.startsWith("/chat"));

onMounted(bootstrap);
</script>

<template>
  <SiteHeader @toggle-menu="mobileMenu = !mobileMenu" />

  <div v-if="!state.ready" class="shell connection-state">
    <h2>{{ state.error || "이야기를 불러오고 있어요." }}</h2>
    <button v-if="state.error" class="primary" @click="bootstrap">
      다시 연결
    </button>
  </div>

  <div v-else class="shell app-layout" :class="{ 'chat-layout': isChat }">
    <LeftSidebar :show-mobile="mobileMenu" @close="mobileMenu = false" />
    <main id="main-content"><RouterView :key="route.path" /></main>
    <RightSidebar v-if="!isChat" />
  </div>

  <SiteFooter />
  <div v-if="state.toast" class="toast" role="status">{{ state.toast }}</div>
</template>
