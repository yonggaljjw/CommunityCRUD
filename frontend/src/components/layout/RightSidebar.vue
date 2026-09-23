<script setup>
import {
  ArrowUpRight,
  LockKeyhole,
  PenLine,
  Radio,
  Users,
} from "lucide-vue-next";
import { state } from "../../stores/appState";

// 게시판 화면의 우측 보조 영역입니다. 채팅 화면에서는 App.vue가 이 컴포넌트를 숨깁니다.
</script>

<template>
  <aside class="right-sidebar">
    <section class="panel lounge-card">
      <div class="panel-heading">
        <h2><Radio :size="18" /> 실시간 라운지</h2>
        <span class="live-label">LIVE</span>
      </div>
      <p class="muted">지금, 함께 이야기해요.</p>

      <RouterLink
        v-for="room in state.rooms.slice(0, 4)"
        :key="room.id"
        :to="`/chat/${room.id}`"
        class="room-link"
      >
        <span class="room-symbol">#</span>
        <div>
          <b>{{ room.name }}</b>
          <span>
            {{ room.category }}
            <LockKeyhole v-if="room.locked" :size="12" />
          </span>
        </div>
        <span class="online-number"><Users :size="13" />{{ room.online }}</span>
      </RouterLink>

      <RouterLink to="/chat" class="room-footer">
        모든 채팅방 보기 <ArrowUpRight :size="15" />
      </RouterLink>
    </section>

    <section class="community-note">
      <span class="eyebrow">JOJI COMMUNITY NOTE</span>
      <h3>익명이니까,<br />더 편하게. 더 존중하며.</h3>
      <p>개인정보를 올리지 말고,<br />서로의 이야기를 존중해 주세요.</p>
      <div class="note-line"></div>
      <span>당신의 이야기를 기다립니다.</span>
    </section>

    <RouterLink to="/write" class="write-callout">
      <PenLine :size="18" /> 첫 문장부터 시작해요 <ArrowUpRight :size="16" />
    </RouterLink>
  </aside>
</template>
