import { ref, watch, onBeforeUnmount } from "vue";
import { api } from "../api";
import { state, notify } from "../state";

export function useChat(room) {
  const messages = ref([]),
    status = ref("connecting"),
    online = ref(0),
    needsPassword = ref(false),
    error = ref(""),
    hasMore = ref(false),
    pending = ref(false),
    acknowledged = ref(null);
  let socket,
    retryTimer,
    heartbeat,
    acknowledgementTimer,
    disposed = false,
    generation = 0,
    retry = 0,
    pendingId = null;
  // id가 같은 메시지는 한 번만 보입니다. 최초 이력과 실시간 수신이 겹치는 상황도 처리합니다.
  function merge(rows) {
    messages.value = [
      ...new Map([...messages.value, ...rows].map((m) => [m.id, m])).values(),
    ].sort((a, b) => a.id - b.id);
  }
  function closeSocket() {
    clearTimeout(retryTimer);
    clearInterval(heartbeat);
    clearTimeout(acknowledgementTimer);
    pending.value = false;
    if (socket) {
      socket.onclose = null;
      socket.onmessage = null;
      socket.close();
      socket = null;
    }
  }
  async function connect(password = "") {
    const current = ++generation;
    closeSocket();
    messages.value = [];
    status.value = "connecting";
    error.value = "";
    needsPassword.value = false;
    const id = room.value?.id;
    if (!id) return;
    try {
      const identity = await api("/session");
      if (disposed || current !== generation) return;
      state.tag = identity.tag;
      await api(`/rooms/${id}/join`, { method: "POST", body: { password } });
      if (disposed || current !== generation) return;
      const ws = new WebSocket(
        `${location.protocol === "https:" ? "wss:" : "ws:"}//${location.host}/ws/chat?roomId=${id}`,
      );
      socket = ws;
      ws.onopen = () => {
        if (current !== generation) return;
        status.value = "connected";
        retry = 0;
        heartbeat = setInterval(() => {
          if (ws.readyState === WebSocket.OPEN)
            ws.send(JSON.stringify({ type: "ping" }));
        }, 25000);
      };
      ws.onmessage = (event) => {
        if (current !== generation) return;
        let data;
        try {
          data = JSON.parse(event.data);
        } catch {
          return;
        }
        if (data.type === "history") {
          merge(data.messages);
          hasMore.value = data.messages.length === 100;
        }
        if (data.type === "message") {
          merge([data.message]);
          // 메시지 표시와 전송 확인을 분리합니다. 같은 세션의 다른 탭 메시지를 내 전송 성공으로 오인하지 않습니다.
        }
        if (data.type === "ack" && data.clientId === pendingId) {
          pending.value = false;
          clearTimeout(acknowledgementTimer);
          acknowledged.value = data.clientId;
        }
        if (data.type === "presence") online.value = data.online;
        if (data.type === "error") {
          pending.value = false;
          clearTimeout(acknowledgementTimer);
          error.value = data.message;
          notify(data.message);
        }
      };
      ws.onerror = () => {
        error.value = "연결 상태를 확인하고 있습니다.";
      };
      ws.onclose = () => {
        if (disposed || current !== generation) return;
        status.value = "disconnected";
        online.value = 0;
        pending.value = false;
        clearInterval(heartbeat);
        // 재전송은 하지 않습니다. 메시지 중복 등록을 막고 재접속 후 DB 이력으로 상태를 복구합니다.
        retryTimer = setTimeout(
          () => connect(),
          Math.min(1000 * 2 ** retry++, 15000),
        );
      };
    } catch (e) {
      if (disposed || current !== generation) return;
      status.value = "disconnected";
      error.value = e.message;
      if (e.status === 403) {
        needsPassword.value = true;
        return;
      }
      if (e.status !== 404)
        retryTimer = setTimeout(
          () => connect(),
          Math.min(1000 * 2 ** retry++, 15000),
        );
    }
  }
  function send(content, nickname) {
    if (socket?.readyState !== WebSocket.OPEN || pending.value) return false;
    error.value = "";
    pending.value = true;
    pendingId = Array.from(crypto.getRandomValues(new Uint8Array(16)), (x) =>
      x.toString(16).padStart(2, "0"),
    ).join("");
    socket.send(
      JSON.stringify({
        type: "message",
        clientId: pendingId,
        content: content.trim(),
        nickname: nickname.trim(),
      }),
    );
    acknowledgementTimer = setTimeout(() => {
      pending.value = false;
      error.value =
        "전송 확인이 지연됩니다. 최근 대화를 확인한 뒤 다시 시도해 주세요.";
    }, 8000);
    return true;
  }
  async function older() {
    const id = room.value.id,
      current = generation;
    try {
      const rows = await api(
        `/rooms/${id}/messages?before=${messages.value[0]?.id || Number.MAX_SAFE_INTEGER}`,
      );
      if (current !== generation) return;
      merge(rows);
      hasMore.value = rows.length === 100;
    } catch (e) {
      notify(e.message);
    }
  }
  watch(
    () => room.value?.id,
    () => {
      messages.value = [];
      online.value = 0;
      hasMore.value = false;
      retry = 0;
      connect();
    },
    { immediate: true },
  );
  onBeforeUnmount(() => {
    disposed = true;
    generation++;
    closeSocket();
  });
  return {
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
  };
}
