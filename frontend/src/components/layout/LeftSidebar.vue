<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import {
  ArrowUpRight,
  BriefcaseBusiness,
  Code2,
  Coffee,
  Gamepad2,
  Hash,
  LayoutGrid,
  LockKeyhole,
  MessageCircle,
  Radio,
  Smile,
  X,
} from "lucide-vue-next";
import { state } from "../../stores/appState";

// 갤러리/채팅방 바로가기 전용 사이드바입니다.
defineProps({
  showMobile: { type: Boolean, default: false },
});
const emit = defineEmits(["close"]);
const route = useRoute();
const icons = [Coffee, Code2, BriefcaseBusiness, Gamepad2, Smile, Hash];
const totalPosts = computed(() =>
  state.galleries.reduce((sum, gallery) => sum + gallery.postCount, 0),
);
</script>

<template>
  <aside class="left-sidebar" :class="{ 'show-mobile': showMobile }">
    <div class="side-heading">
      GALLERIES
      <button
        class="icon-button mobile-menu"
        aria-label="메뉴 닫기"
        @click="emit('close')"
      >
        <X :size="18" />
      </button>
    </div>

    <RouterLink
      to="/"
      class="side-link"
      :class="{ selected: route.path === '/' }"
      @click="emit('close')"
    >
      <LayoutGrid :size="18" /> 전체 갤러리
      <span>{{ totalPosts }}</span>
    </RouterLink>

    <RouterLink
      v-for="(gallery, index) in state.galleries"
      :key="gallery.id"
      :to="`/galleries/${gallery.id}`"
      class="side-link"
      :class="{
        selected:
          route.params.id == gallery.id && route.path.startsWith('/galleries'),
      }"
      @click="emit('close')"
    >
      <component :is="icons[index % icons.length]" :size="18" />
      {{ gallery.name }}
      <span>{{ gallery.postCount }}</span>
    </RouterLink>

    <div class="side-divider"></div>
    <div class="side-heading">LIVE LOUNGE <Radio :size="15" /></div>

    <RouterLink
      v-for="room in state.rooms.slice(0, 4)"
      :key="room.id"
      :to="`/chat/${room.id}`"
      class="side-room"
      @click="emit('close')"
    >
      <span># {{ room.name }}</span>
      <LockKeyhole v-if="room.locked" :size="13" />
    </RouterLink>

    <div class="side-note">
      <MessageCircle :size="24" />
      <b>같은 관심사, 새로운 대화</b>
      <p>글로 남기고, 채팅으로 이어가세요.</p>
      <RouterLink to="/chat">
        채팅방 둘러보기 <ArrowUpRight :size="15" />
      </RouterLink>
    </div>
  </aside>
</template>
