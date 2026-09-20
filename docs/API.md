# API 계약

웹 서버 기준 `/api` 아래에 있습니다. 요청과 응답은 UTF-8 JSON입니다. 익명 세션 쿠키는 서버가 발급하며 HttpOnly입니다. 상태를 바꾸는 요청에는 `X-Requested-With: JOJI` 헤더가 필요합니다. 브라우저의 Origin은 `.env`의 허용 주소와 일치해야 합니다.

## 게시판

| 메서드 | 경로 | 내용 |
|---|---|---|
| GET | `/api/session` | 세션 생성 또는 조회, `{ "tag": "식별코드" }` |
| GET | `/api/galleries` | 갤러리와 글 수 |
| GET | `/api/posts` | 글 목록: `galleryId`, `category`, `q`, `sort`, `page`, `size` |
| POST | `/api/posts` | 새 글 작성 |
| GET | `/api/posts/{id}` | 상세·조회수 증가·내 추천 여부 |
| PUT | `/api/posts/{id}` | 작성 비밀번호를 확인하고 수정 |
| DELETE | `/api/posts/{id}` | `{ "password": "작성 비밀번호" }` |
| POST | `/api/posts/{id}/like` | 세션 기준 추천/취소 토글 |
| GET | `/api/posts/{id}/comments?after=0` | 댓글 100개, 마지막 ID를 after로 전달해 다음 페이지 |
| POST | `/api/posts/{id}/comments` | 댓글 등록 |
| DELETE | `/api/posts/{postId}/comments/{id}` | 댓글 작성 비밀번호로 삭제 |

`sort`: `new`(최신), `hot`(추천순), `best`(추천 30개 이상·추천순). page는 1부터, size는 1~50입니다. q는 100자까지 제목·본문 부분 문자열 검색을 합니다.

글 작성·수정 요청:

```json
{
  "galleryId": 2,
  "category": "질문",
  "title": "WebSocket 연결 질문",
  "content": "질문 내용입니다.",
  "nickname": "ㅇㅇ",
  "password": "직접 정한 비밀번호"
}
```

category는 `일반/질문/정보/유머/후기/잡담` 중 하나입니다. 제목 150자, 본문 20,000자, 닉네임 20자, 비밀번호 4~60자 및 UTF-8 72바이트 이내입니다. 수정 요청의 비밀번호는 기존 비밀번호 확인용이며 비밀번호 변경 기능은 없습니다.

댓글 입력은 `nickname`, `content`(1~1,000자), `password`입니다.

글 목록 응답:

```json
{
  "items": [],
  "total": 0,
  "page": 1,
  "size": 15
}
```

## 채팅방

| 메서드 | 경로 | 내용 |
|---|---|---|
| GET | `/api/rooms` | 방 목록, locked, 현재 실제 접속 수 online |
| POST | `/api/rooms` | 새 방 생성 |
| POST | `/api/rooms/{id}/join` | `{ "password": "..." }`, 공개방은 빈 문자열 |
| GET | `/api/rooms/{id}/messages` | 최근 100개, 시간순 반환 |
| GET | `/api/rooms/{id}/messages?before={id}` | 주어진 메시지 ID 이전 100개 |

방 생성 입력:

```json
{
  "name": "면접 준비방",
  "category": "취업 / 커리어",
  "description": "면접 준비 이야기를 나눠요",
  "password": ""
}
```

방 이름 60자, 설명 200자까지입니다. category는 `자유`, `개발 / IT`, `취업 / 커리어`, `게임` 중 하나입니다. 비밀번호는 선택이며 설정하면 4자 이상이어야 합니다.

비밀방의 대화 이력 API와 WebSocket 모두 서버의 세션 입장 허가를 확인합니다. 방 목록에는 비밀번호 해시가 포함되지 않습니다. 허가는 세션이 끝날 때까지 유지됩니다.

## WebSocket 메시지

접속 순서: `/api/session` → `/api/rooms/{id}/join` → `ws://localhost:8088/ws/chat?roomId={id}`. HTTPS 환경에서는 `wss://`입니다.

클라이언트 → 서버:

```json
{"type":"message","clientId":"임의 요청 식별자","nickname":"ㅇㅇ","content":"안녕하세요"}
```

서버 → 같은 방 전체:

```json
{
  "type":"message",
  "message":{
    "id":1,"roomId":1,"nickname":"ㅇㅇ","authorTag":"abcdefgh",
    "content":"안녕하세요","createdAt":"2026-09-05T00:00:00Z"
  }
}
```

서버 → 보낸 소켓: `{"type":"ack","clientId":"임의 요청 식별자","messageId":1}`.

그 외 서버 이벤트:

- `history`: `{ "type": "history", "messages": [...] }`
- `presence`: `{ "type": "presence", "online": 2 }`
- `error`: `{ "type": "error", "message": "이유" }`
- `pong`: `{ "type": "pong" }` — 클라이언트의 `{ "type": "ping" }`에 응답

채팅 내용은 1~500자, 닉네임은 1~20자, clientId는 최대 64자입니다. 닉네임/본문의 공백만 입력은 거절합니다. 메시지는 같은 세션의 모든 채팅방·탭 합산으로 3초 간격입니다. JSON 패킷은 8KiB 이하입니다.

`clientId`는 전송 확인을 위한 상관 식별자입니다. 서버의 영구적인 중복 전송 방지 키는 아닙니다. 자동 재전송을 하지 않으며, 저장 성공 후 ack가 유실된 경우 이력을 먼저 확인해야 합니다.

## 오류

HTTP 오류는 기본적으로 `{ "message": "한글 오류 설명" }`으로 응답합니다. Nginx 차단이나 예상하지 못한 서버 오류는 형식이 다를 수 있어 프런트가 상태 코드 기반 오류 처리도 합니다.

- `400`: 길이·형식·검색 조건 오류
- `403`: 비밀번호 불일치, 비밀방 허가 없음, 허용되지 않은 요청
- `404`: 없는 글·방
- `409`: 데이터 동시 변경 또는 외래키 충돌
- `429`: 너무 빠른 요청

세션별 제한: 글 3초, 댓글 3초, 채팅 3초, 방 생성 10초, 추천 0.5초, 비밀번호 시도 1초. Nginx에는 IP별 일반 API 요청과 소켓 연결 요청의 추가 제한이 있습니다. WebSocket 연결 이후의 메시지 도배는 서버 세션 제한이 담당합니다.
