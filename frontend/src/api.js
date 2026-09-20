// HTTP 통신을 한곳에 모으면 오류 처리와 헤더 설정을 화면마다 반복하지 않아도 됩니다.
export async function api(path, options = {}) {
  const response = await fetch(`/api${path}`, {
    ...options,
    credentials: "same-origin",
    headers: {
      "Content-Type": "application/json",
      "X-Requested-With": "JOJI",
      ...options.headers,
    },
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    const error = new Error(
      body.message || `요청에 실패했습니다. (${response.status})`,
    );
    error.status = response.status;
    throw error;
  }
  return response.status === 204 ? null : response.json();
}
export function dateTime(value) {
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}
export function shortDate(value) {
  const date = new Date(value);
  return date.toLocaleDateString("ko-KR", { month: "2-digit", day: "2-digit" });
}
