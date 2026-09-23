import { reactive } from "vue";
import { api } from "../services/api";

/**
 * 로그인 없는 작은 프로젝트라 Pinia 대신 Vue reactive로 공통 상태를 관리합니다.
 * 규모가 커지면 이 파일의 역할을 Pinia store로 옮길 수 있습니다.
 */
function savedNickname() {
  try {
    return localStorage.getItem("joji-nickname") || "ㅇㅇ";
  } catch {
    return "ㅇㅇ";
  }
}

export const state = reactive({
  galleries: [],
  rooms: [],
  tag: "",
  nickname: savedNickname(),
  toast: "",
  ready: false,
  error: "",
});

let toastTimer;

/** 짧은 안내 메시지를 화면 하단 toast로 보여 줍니다. */
export function notify(message) {
  state.toast = message;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (state.toast = ""), 4500);
}

/** 개인정보가 아닌 닉네임만 브라우저에 기억합니다. 비밀번호는 저장하지 않습니다. */
export function rememberNickname() {
  try {
    localStorage.setItem("joji-nickname", state.nickname);
  } catch {
    // 저장이 차단된 브라우저에서도 앱 자체는 계속 사용할 수 있습니다.
  }
}

export async function refreshRooms() {
  state.rooms = await api("/rooms");
}

/** 앱 최초 진입 시 공통으로 필요한 세션·갤러리·채팅방 정보를 병렬로 불러옵니다. */
export async function bootstrap() {
  state.error = "";
  try {
    const [session, galleries, rooms] = await Promise.all([
      api("/session"),
      api("/galleries"),
      api("/rooms"),
    ]);
    state.tag = session.tag;
    state.galleries = galleries;
    state.rooms = rooms;
    state.ready = true;
  } catch {
    state.ready = false;
    state.error =
      "서버에 연결하지 못했습니다. 실행 상태를 확인한 뒤 다시 시도해 주세요.";
  }
}
