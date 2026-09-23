<script setup>
// 하나의 채팅방 UI: 메시지 목록·입력창·비밀번호 입장 화면을 표시합니다.
import { computed, ref, watch, nextTick } from "vue";
import {
  Radio,
  Send,
  LockKeyhole,
  RefreshCw,
  ArrowUpRight,
} from "lucide-vue-next";
import { useChat } from "./useChat";
import { state, rememberNickname, notify } from "../../stores/appState";
const props = defineProps({ room: Object, compact: Boolean });
const room = computed(() => props.room),
  {
    messages,
    status,
    online,
    needsPassword,
    error,
    hasMore,
    pending,
    acknowledged,
    connect,
    send,
    older,
  } = useChat(room);
const draft = ref(""),
  password = ref(""),
  autoScroll = ref(true),
  stream = ref(null),
  cooldownUntil = ref(0),
  lastSent = ref("");
const statusText = computed(
  () =>
    ({ connected: "연결됨", connecting: "연결 중", disconnected: "연결 끊김" })[
      status.value
    ],
);
// 새 메시지가 왔을 때 사용자가 이미 아래쪽을 보고 있다면 자동으로 맨 아래까지 스크롤합니다.
watch(
  () => messages.value.at(-1)?.id,
  async () => {
    if (autoScroll.value) {
      await nextTick();
      if (stream.value) stream.value.scrollTop = stream.value.scrollHeight;
    }
  },
);
// 성공 확인 전까지 입력 내용을 남깁니다. 서버가 거절한 메시지를 사용자가 잃지 않게 합니다.
watch(acknowledged, () => {
  if (draft.value === lastSent.value) draft.value = "";
});
function submit() {
  if (!draft.value.trim() || !state.nickname.trim()) return;
  if (Date.now() < cooldownUntil.value) {
    notify("메시지는 3초 간격으로 보낼 수 있습니다.");
    return;
  }
  lastSent.value = draft.value;
  if (send(draft.value, state.nickname)) {
    rememberNickname();
    cooldownUntil.value = Date.now() + 3000;
  }
}
function keydown(e) {
  if (e.key === "Enter" && !e.shiftKey && !e.isComposing) {
    e.preventDefault();
    submit();
  }
}
async function loadOlder() {
  const previous = stream.value?.scrollHeight || 0;
  await older();
  await nextTick();
  if (stream.value)
    stream.value.scrollTop = stream.value.scrollHeight - previous;
}
</script>
<template>
  <section class="panel chat-panel" :class="{ compact }">
    <header class="chat-header">
      <div>
        <Radio :size="18" />
        <h2>{{ room.name }}</h2>
        <span class="live-tag">LIVE</span>
      </div>
      <RouterLink
        v-if="compact"
        :to="`/chat/${room.id}`"
        aria-label="채팅방 크게 열기"
        ><ArrowUpRight :size="18" /></RouterLink
      ><span v-else class="chat-online">{{ online }}명 참여 중</span>
    </header>
    <div class="chat-status">
      <span
        :class="['status-dot', { connected: status === 'connected' }]"
      ></span
      >{{ statusText }}<span v-if="compact">· {{ online }}명</span
      ><label><input v-model="autoScroll" type="checkbox" />자동 스크롤</label>
    </div>
    <form
      v-if="needsPassword"
      class="room-password"
      @submit.prevent="connect(password)"
    >
      <LockKeyhole :size="28" />
      <h3>비밀번호가 있는 채팅방입니다.</h3>
      <label
        >입장 비밀번호<input
          v-model="password"
          type="password"
          required
          maxlength="60"
          autocomplete="off"
      /></label>
      <p v-if="error" class="error">{{ error }}</p>
      <button class="primary">입장하기</button>
    </form>
    <template v-else
      ><div
        ref="stream"
        class="chat-stream"
        role="log"
        aria-label="채팅 메시지"
        aria-live="polite"
      >
        <div class="chat-welcome">
          <span>JOJI LOUNGE</span>
          <p>
            대화에 오신 것을 환영합니다.<br />개인정보를 공유하지 말고 서로
            존중해 주세요.
          </p>
        </div>
        <button v-if="hasMore" class="older-button" @click="loadOlder">
          이전 대화 불러오기
        </button>
        <p
          v-if="!messages.length && status === 'connected'"
          class="empty-small muted"
        >
          아직 대화가 없어요. 먼저 인사해 볼까요?
        </p>
        <div
          v-for="m in messages"
          :key="m.id"
          class="chat-message"
          :class="{ mine: m.authorTag === state.tag }"
        >
          <span class="chat-avatar">{{ m.nickname.slice(0, 1) }}</span>
          <div class="message-body">
            <div class="message-meta">
              <b>{{ m.nickname }}</b
              ><small>#{{ m.authorTag }}</small
              ><span v-if="m.authorTag === state.tag" class="me-tag">나</span
              ><time>{{
                new Date(m.createdAt).toLocaleTimeString("ko-KR", {
                  hour: "2-digit",
                  minute: "2-digit",
                })
              }}</time>
            </div>
            <p>{{ m.content }}</p>
          </div>
        </div>
      </div>
      <p v-if="error" class="chat-error" role="alert">
        {{ error }}
        <button
          v-if="status === 'disconnected'"
          class="text-button"
          @click="connect()"
        >
          <RefreshCw :size="14" />재연결
        </button>
      </p>
      <form class="chat-compose" @submit.prevent="submit">
        <div class="nickname-row">
          <label
            >닉네임<input
              v-model="state.nickname"
              maxlength="20"
              required
              @change="rememberNickname" /></label
          ><span>#{{ state.tag }}</span
          ><span class="compose-hint">3초 간격 · {{ draft.length }}/500</span>
        </div>
        <div class="chat-input-row">
          <textarea
            v-model="draft"
            maxlength="500"
            required
            :rows="compact ? 1 : 2"
            aria-label="채팅 메시지"
            placeholder="지금 하고 싶은 이야기를 남겨 보세요."
            @keydown="keydown"
          /><button
            class="primary"
            :disabled="status !== 'connected' || pending || !draft.trim()"
          >
            <Send :size="17" /><span>{{ pending ? "전송 중" : "전송" }}</span>
          </button>
        </div>
        <p v-if="!compact" class="help">Enter 전송 · Shift + Enter 줄바꿈</p>
      </form></template
    >
  </section>
</template>
