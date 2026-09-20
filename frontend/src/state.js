import { reactive } from "vue";
import { api } from "./api";
// 닉네임만 로컬에 보관합니다. 비밀번호·DB 접속 정보·서버 세션 ID는 저장하지 않습니다.
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
export function notify(message) {
  state.toast = message;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (state.toast = ""), 4500);
}
export function rememberNickname() {
  try {
    localStorage.setItem("joji-nickname", state.nickname);
  } catch {
    /* 저장이 차단되어도 앱을 사용할 수 있습니다. */
  }
}
export async function refreshRooms() {
  state.rooms = await api("/rooms");
}
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
  } catch (e) {
    state.error =
      "서버에 연결하지 못했습니다. 실행 상태를 확인한 뒤 다시 시도해 주세요.";
  }
}
