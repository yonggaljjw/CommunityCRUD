/**
 * 백엔드 HTTP 통신 공통 함수입니다.
 * 화면 컴포넌트가 fetch의 헤더·JSON 파싱·에러 형식을 매번 반복하지 않도록 한곳에서 처리합니다.
 */
export async function api(path, options = {}) {
  const response = await fetch(`/api${path}`, {
    ...options,
    credentials: "same-origin",
    headers: {
      "Content-Type": "application/json",
      // 서버 RequestGuard가 우리 프런트에서 보낸 쓰기 요청인지 확인할 때 사용합니다.
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
    // 호출한 화면에서 403/404처럼 상태별 처리가 필요할 수 있어 status를 보존합니다.
    error.status = response.status;
    throw error;
  }

  // DELETE처럼 204 No Content인 응답에는 JSON 본문이 없으므로 null을 반환합니다.
  return response.status === 204 ? null : response.json();
}
