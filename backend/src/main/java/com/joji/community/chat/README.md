# chat 기능

채팅방과 실시간 채팅에 필요한 코드를 한곳에 모았습니다. HTTP와 WebSocket이라는 처리 방식은 다르지만 모두 같은 채팅 기능이므로 현재 규모에서는 별도 하위 패키지로 쪼개지 않습니다.

## 파일 역할

- `ChatController.java`: 방 목록·생성·입장·과거 메시지 조회 API
- `ChatService.java`: 방 입장 및 비밀번호 검증 등 업무 규칙
- `ChatRepository.java`: 방·메시지 DB 접근
- `ChatModels.java`: 요청·응답 및 WebSocket 메시지 자료형
- `ChatHandler.java`: WebSocket 연결·수신·저장·방송

추천 읽기 순서: `ChatController → ChatService → ChatHandler → ChatRepository`.

## 기본 개념

### HTTP와 WebSocket의 차이

일반 HTTP는 보통 **요청 → 응답** 한 번으로 끝납니다.

```text
브라우저 → GET /api/rooms → 서버 응답 → 연결 종료
```

채팅은 상대방이 새 메시지를 보낼 때마다 화면에 즉시 전달해야 하므로 서버와 연결을 계속 유지하는 WebSocket이 잘 맞습니다.

```text
브라우저 ⇄ WebSocket 연결 유지 ⇄ 서버
          메시지 수신 가능
```

이 프로젝트에서는 방 생성·입장·과거 메시지 조회는 HTTP로 처리하고, 실시간 메시지는 WebSocket으로 처리합니다.

### WebSocket handshake

WebSocket도 처음에는 HTTP 요청으로 연결을 시작합니다. 이 최초 연결 과정을 handshake라고 합니다. `WebSocketConfig`에서 `/ws/chat` 연결을 등록하고 비밀방 입장 권한을 확인한 뒤 실제 `ChatHandler`로 연결을 넘깁니다.

### WebSocket Session과 HttpSession

이 둘은 이름은 비슷하지만 역할이 다릅니다.

- `HttpSession`: 같은 브라우저 사용자를 서버가 식별하기 위한 HTTP 세션
- `WebSocketSession`: 현재 열려 있는 하나의 WebSocket 연결

이 프로젝트는 handshake 때 `HttpSession`과 `roomId`를 WebSocket 세션 속성으로 전달합니다.

### 실시간 방송(broadcast)

사용자가 메시지를 보내면 서버는 메시지를 DB에 먼저 저장하고 같은 방에 연결된 WebSocket들에게 전송합니다. 이를 broadcast라고 합니다.

```text
사용자 A
  ↓ 메시지
ChatHandler
  ↓ 저장
ChatRepository → DB
  ↓
같은 방 A/B/C의 WebSocket에 방송
```

### ConcurrentHashMap

`ChatHandler`는 현재 접속 중인 WebSocket을 메모리의 `ConcurrentHashMap`으로 관리합니다. 여러 요청 스레드가 동시에 접근할 수 있으므로 일반 `HashMap`보다 동시성 환경에 적합합니다.

현재 방식은 서버 한 대에서는 간단하지만 서버를 여러 대 띄우면 각 서버의 메모리가 서로 다르므로 Redis Pub/Sub 같은 별도 메시지 브로커가 필요할 수 있습니다.

### heartbeat와 ack

프런트 `useChat.js`는 주기적으로 `ping`을 보내 연결이 살아 있는지 확인하고, 메시지를 전송한 뒤 서버의 `ack`를 기다립니다.

- heartbeat: 연결이 정상인지 주기적으로 확인
- ack(acknowledgement): 서버가 특정 요청을 정상 처리했음을 알려 주는 응답

이 구조 덕분에 단순히 `send()`를 호출한 것과 실제 서버 저장 성공을 구분할 수 있습니다.
