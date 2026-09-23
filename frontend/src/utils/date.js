// 화면 여러 곳에서 사용하는 날짜 표시 형식을 한곳에 모아 둡니다.
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
