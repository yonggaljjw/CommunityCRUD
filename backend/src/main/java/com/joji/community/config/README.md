# config

Spring 설정 클래스를 둡니다. 현재 `WebSocketConfig`가 `/ws/chat` 경로, 허용 Origin, 비밀방 handshake 권한 검사를 설정합니다.

## 기본 개념

### `@Configuration`

`@Configuration`은 이 클래스가 일반 업무 로직이 아니라 **Spring 애플리케이션 설정을 정의하는 클래스**임을 나타냅니다.

### `@EnableWebSocket`

Spring에서 직접 WebSocket Handler를 등록할 수 있게 WebSocket 기능을 활성화합니다.

### Handler 등록

```text
/ws/chat
   ↓
WebSocketConfig
   ↓
ChatHandler
```

`registerWebSocketHandlers()`에서 특정 URL과 실제 처리 객체를 연결합니다.

### Origin

Origin은 브라우저에서 요청을 시작한 출처를 의미하며 보통 `프로토콜 + 호스트 + 포트`로 구성됩니다.

예:

```text
http://localhost:8080
```

WebSocket도 브라우저에서 연결되므로 허용할 Origin을 제한하는 것은 기본적인 보안 장치 중 하나입니다.

### HandshakeInterceptor

WebSocket 연결이 완전히 열리기 전에 실행되는 인터셉터입니다. 이 프로젝트는 다음을 확인합니다.

1. 기존 HTTP 세션이 있는지
2. 익명 사용자 식별값이 있는지
3. 요청한 `roomId`가 유효한지
4. 비밀방이라면 이미 입장 인증을 했는지

검사를 통과한 뒤 `roomId`와 `HttpSession`을 WebSocket 속성에 저장해 `ChatHandler`에서 다시 사용할 수 있게 합니다.

## 왜 chat 폴더가 아니라 config에 있나?

실시간 채팅이라는 기능과 관련되어 있지만 이 클래스의 핵심 책임은 메시지를 처리하는 것이 아니라 **Spring에 WebSocket 연결 방법을 등록하는 것**입니다. 따라서 `chat`이 아닌 `config`에 둡니다.
