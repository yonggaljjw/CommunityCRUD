<script setup>
// 채팅 홈 화면: 방 목록 검색·방 생성·선택된 채팅방 표시를 담당합니다.
import { computed, onMounted, onBeforeUnmount, ref, reactive } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  Plus,
  Search,
  LockKeyhole,
  Users,
  MessageCircle,
  ArrowRight,
} from "lucide-vue-next";
import { api } from "../../services/api";
import { state, refreshRooms, notify } from "../../stores/appState";
import Modal from "../../components/Modal.vue";
import ChatPanel from "./ChatPanel.vue";
const route = useRoute(),
  router = useRouter(),
  search = ref(""),
  creating = ref(false),
  busy = ref(false),
  error = ref("");
const room = computed(() =>
  state.rooms.find((r) => r.id === Number(route.params.id)),
);
const filtered = computed(() =>
  state.rooms.filter((r) => `${r.name} ${r.category}`.includes(search.value)),
);
const form = reactive({
  name: "",
  category: "자유",
  description: "",
  password: "",
});
let timer;
// 채팅방 접속자 수가 바뀔 수 있으므로 방 목록은 10초마다 가볍게 새로고침합니다.
onMounted(() => {
  refreshRooms().catch(() => {});
  timer = setInterval(() => refreshRooms().catch(() => {}), 10000);
});
onBeforeUnmount(() => clearInterval(timer));
// 새 방을 만든 뒤 전역 방 목록을 갱신하고 방 상세 URL로 이동합니다.
async function create() {
  busy.value = true;
  error.value = "";
  try {
    const r = await api("/rooms", { method: "POST", body: form });
    await refreshRooms();
    creating.value = false;
    Object.assign(form, {
      name: "",
      category: "자유",
      description: "",
      password: "",
    });
    notify("채팅방을 만들었습니다.");
    router.push(`/chat/${r.id}`);
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <div class="breadcrumb">
    커뮤니티 <span>/</span><RouterLink to="/chat">실시간 채팅방</RouterLink
    ><template v-if="room"><span>/</span>{{ room.name }}</template>
  </div>
  <div class="page-heading">
    <div>
      <span class="eyebrow">REAL-TIME LOUNGE</span>
      <h1>이야기는 지금부터<span class="heading-dot">.</span></h1>
      <p>관심 있는 방에 들어가, 익명으로 대화를 시작하세요.</p>
    </div>
    <button
      class="primary"
      @click="
        creating = true;
        error = '';
      "
    >
      <Plus :size="17" />채팅방 만들기
    </button>
  </div>
  <div class="chat-workspace">
    <aside class="panel room-directory">
      <div class="directory-heading">
        <b>채팅방</b><span>{{ state.rooms.length }}</span>
      </div>
      <label class="room-search"
        ><Search :size="16" /><input
          v-model="search"
          aria-label="채팅방 검색"
          placeholder="채팅방 검색"
      /></label>
      <div v-if="!filtered.length" class="empty-small muted">
        검색 결과가 없습니다.
      </div>
      <RouterLink
        v-for="r in filtered"
        :key="r.id"
        :to="`/chat/${r.id}`"
        class="directory-room"
        :class="{ selected: r.id === room?.id }"
        ><div>
          <b># {{ r.name }}</b
          ><LockKeyhole v-if="r.locked" :size="14" />
        </div>
        <span
          >{{ r.category
          }}<small><Users :size="12" />{{ r.online }}</small></span
        ></RouterLink
      ><button
        class="create-room-link"
        @click="
          creating = true;
          error = '';
        "
      >
        <Plus :size="15" /> 새로운 채팅방 만들기
      </button>
    </aside>
    <ChatPanel v-if="room" :room="room" />
    <section v-else class="panel room-landing">
      <MessageCircle :size="45" /><span class="eyebrow"
        >FIND YOUR CONVERSATION</span
      >
      <h2>
        {{
          route.params.id
            ? "채팅방을 찾을 수 없어요."
            : "어디에서 이야기할까요?"
        }}
      </h2>
      <p>채팅방을 선택하거나<br />새로운 관심사로 직접 방을 만들어 보세요.</p>
      <div class="room-grid">
        <RouterLink
          v-for="r in state.rooms.slice(0, 4)"
          :key="r.id"
          :to="`/chat/${r.id}`"
          ><span class="category-badge">{{ r.category }}</span>
          <h3>{{ r.name }}</h3>
          <p>{{ r.description }}</p>
          <span>참여하기 <ArrowRight :size="15" /></span
        ></RouterLink>
      </div>
    </section>
  </div>
  <Modal v-if="creating" title="새 익명 채팅방 만들기" @close="creating = false"
    ><form @submit.prevent="create">
      <label
        >채팅방 이름<input
          v-model="form.name"
          maxlength="60"
          required
          placeholder="어떤 이야기를 나눌까요?" /></label
      ><label
        >카테고리<select v-model="form.category">
          <option
            v-for="c in ['자유', '개발 / IT', '취업 / 커리어', '게임']"
            :key="c"
          >
            {{ c }}
          </option>
        </select></label
      ><label
        >방 소개<textarea
          v-model="form.description"
          maxlength="200"
          rows="2"
          placeholder="방에 대한 짧은 설명"
        /></label
      ><label
        >입장 비밀번호 <span class="muted">(선택)</span
        ><input
          v-model="form.password"
          type="password"
          minlength="4"
          maxlength="60"
          autocomplete="new-password"
          placeholder="비워 두면 누구나 입장할 수 있어요"
      /></label>
      <p class="help">비밀번호를 설정하면 입장할 때 확인합니다.</p>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <div class="form-actions">
        <button type="button" @click="creating = false">취소</button
        ><button class="primary" :disabled="busy">
          {{ busy ? "만드는 중…" : "채팅방 만들기" }}
        </button>
      </div>
    </form></Modal
  >
</template>
