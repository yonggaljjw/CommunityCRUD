# features/chat

실시간 채팅 기능을 한 폴더에서 관리합니다.

## 파일 역할

- `ChatView.vue`: 채팅방 검색·생성·선택 화면
- `ChatPanel.vue`: 메시지 목록과 입력 UI
- `useChat.js`: WebSocket 연결, heartbeat, 재접속, ack, 과거 메시지 조회

`views/components/composables`처럼 각각 한 파일을 위한 중간 폴더는 만들지 않았습니다. 기능이 커져 파일 탐색이 불편해질 때만 분리하세요.

## 기본 개념

### Composable

Vue Composition API에서 여러 컴포넌트가 사용할 수 있도록 **상태와 로직을 함수로 분리한 것**을 흔히 composable이라고 부릅니다.

이 프로젝트의 `useChat(room)`은 WebSocket 처리 코드를 화면 템플릿에서 분리합니다.

```text
ChatPanel.vue
  ↓ useChat(room)
useChat.js
  ├─ 연결
  ├─ 수신
  ├─ 전송
  ├─ 재접속
  └─ 과거 메시지 조회
```

### WebSocket 상태

`new WebSocket()`으로 연결한 뒤 주로 다음 이벤트를 처리합니다.

- `onopen`: 연결 성공
- `onmessage`: 서버 메시지 수신
- `onerror`: 통신 오류
- `onclose`: 연결 종료

일반 HTTP처럼 요청할 때마다 연결을 새로 만드는 것이 아니라 연결을 유지한 상태로 양방향 통신합니다.

### heartbeat

중간 네트워크 장비나 서버가 끊긴 연결을 알아차리지 못하는 경우가 있습니다. 그래서 일정 시간마다 `ping` 메시지를 보내 연결이 살아 있는지 확인합니다.

### 재접속과 Exponential Backoff

연결이 끊겼다고 즉시 무한 재시도하면 서버와 네트워크에 부담을 줄 수 있습니다. `useChat.js`는 재시도 간격을 점차 늘리고 최대 15초로 제한합니다.

```text
1초 → 2초 → 4초 → 8초 → 15초 ...
```

이 방식을 exponential backoff라고 합니다.

### ACK

클라이언트가 메시지를 `send()`했다고 해서 서버가 DB 저장까지 성공했다는 뜻은 아닙니다. 그래서 각 전송에 `clientId`를 붙이고 서버가 처리 후 같은 ID의 `ack`를 보내도록 해 전송 완료를 확인합니다.

### 메시지 중복 제거

과거 메시지 조회 결과와 실시간 메시지가 겹치거나 재접속 과정에서 같은 메시지가 다시 들어올 수 있습니다. `merge()`는 메시지 ID를 기준으로 중복을 제거한 뒤 순서대로 정렬합니다.

### 컴포넌트 정리(clean-up)

화면을 떠났는데 WebSocket·타이머가 계속 살아 있으면 메모리 누수나 중복 이벤트가 생길 수 있습니다. `onBeforeUnmount()`에서 소켓과 타이머를 닫는 이유입니다.

## 실시간 메시지 흐름

```text
ChatPanel
  ↓ send()
useChat.js
  ↓ WebSocket
Spring ChatHandler
  ↓ DB 저장
ChatRepository
  ↓
ChatHandler가 같은 방에 broadcast
  ↓ WebSocket
useChat.js onmessage
  ↓
messages ref 변경
  ↓
ChatPanel 자동 갱신
```
