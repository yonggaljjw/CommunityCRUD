# JOJI COMMINITY

첨부된 게시판·채팅방·게시글 상세 HTML 시안을 바탕으로 만든 **Vue 3 + Spring Boot + MySQL 익명 커뮤니티 학습 프로젝트**입니다. 요청한 표기인 `JOJI COMMINITY`를 화면 제목으로 사용했습니다.

짙은 남색 메뉴, 밝은 파랑 배경, 오렌지 강조색, 갤러리별 게시판, 채팅 라운지 구성을 반영했습니다. 원본의 하드코딩된 숫자와 클릭 시 메시지만 추가하던 동작을 실제 API·DB·WebSocket 동작으로 교체했습니다.

### 첨부 시안 반영 위치

| 첨부 | 반영한 부분 |
|---|---|
| 붙여넣은 텍스트 (1) | 공통 헤더·메뉴, 갤러리 게시글 표, 하단 익명 채팅 |
| 붙여넣은 텍스트 (2) | 채팅방 목록, 방 개설 모달, 실시간 대화 화면 |
| 붙여넣은 텍스트 (3) | 게시글 본문, 추천 영역, 댓글 입력과 목록 |

원본의 Tailwind CDN 및 직접 DOM 조작 스크립트를 Vue 컴포넌트와 공통 CSS로 재구성했습니다. 화면에서 사용하는 아이콘은 설치된 패키지에 포함되며 외부 폰트/CDN 호출에 의존하지 않습니다.

## 1. 가장 빠른 실행

준비물: **Docker Desktop 또는 Docker Engine + Compose v2**. 소스 폴더를 압축 해제한 후 `compose.yaml`이 있는 디렉터리에서 실행합니다. Docker가 Linux 컨테이너 모드로 실행되어 있어야 합니다.

```bash
# Python이 있는 경우: 임의 비밀번호를 생성하며 .env를 준비합니다.
python scripts/init_env.py

# 최초 실행은 이미지/라이브러리 다운로드와 빌드 때문에 시간이 걸립니다.
docker compose up --build -d

# 세 서비스가 실행되었는지 확인합니다.
docker compose ps
```

**브라우저 접속: http://localhost:8088**

Windows에서 `python` 명령이 없다면 `py scripts/init_env.py`를 시도하세요. Python 없이도 아래 방법으로 설정할 수 있습니다.

```powershell
# Windows PowerShell: Python 대신 이 방법을 사용해도 됩니다.
Copy-Item .env.example .env
notepad .env
```

macOS/Linux에서는 `cp .env.example .env` 후 편집합니다. `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `DEMO_POST_PASSWORD`의 예시 값을 서로 다른 임의 문자열로 교체합니다. `.env`는 절대로 공유하거나 커밋하지 마세요.

> Docker 이미지 안에서 Vue 빌드와 Java 빌드·테스트가 수행됩니다. Docker로만 실행하면 PC에 Java·Maven·Node.js를 따로 설치할 필요가 없습니다. 최초 다운로드에는 인터넷 연결이 필요합니다.

## 2. 실행하고 직접 확인할 것

1. 홈에서 `[예시]` 게시글을 열고 갤러리를 이동해 봅니다.
2. `글쓰기`에서 닉네임·비밀번호를 정해 새 글을 등록합니다.
3. 작성 비밀번호로 수정·삭제를 시도합니다. 잘못된 비밀번호는 거절됩니다.
4. 댓글을 작성하고, 추천 버튼을 두 번 눌러 추천·취소를 확인합니다.
5. `실시간 채팅방`에서 새 방을 만들고 입장합니다.
6. **일반 창과 시크릿 창**으로 같은 방에 접속한 뒤 메시지를 주고받습니다. 서로 다른 익명 식별코드가 표시됩니다.
7. 한 창을 다른 방으로 옮깁니다. 그 방에는 이전 방의 새 메시지가 나타나지 않아야 합니다.
8. 비밀번호방을 만들고 시크릿 창에서 입장 비밀번호를 확인합니다.
9. 새로고침 후 최근 채팅이 복구되는지 확인합니다.
10. `docker compose restart` 후 작성한 글과 대화가 남아 있는지 확인합니다.

같은 브라우저의 일반 탭들은 쿠키를 공유하므로 같은 익명 사용자입니다. 방의 인원 수도 중복 탭을 제외한 세션 기준입니다. 새 브라우저 프로필·시크릿 창은 다른 사용자입니다.

## 3. 구현 범위

| 영역 | 구현한 동작 |
|---|---|
| 갤러리 | 자유, 프로그래밍, 취업·커리어, 게임, 유머, 일상 |
| 게시판 | 목록, 페이지 이동, 말머리 필터, 제목+내용 검색, 추천순 정렬, 추천 30개 이상 개념글 |
| 게시글 | 작성·조회·수정·삭제, 비밀번호 확인, 조회수 |
| 댓글 | 작성·삭제, 비밀번호 확인, 100개 단위 더 불러오기 |
| 추천 | 세션당 1개, 다시 누르면 취소, DB 복합 기본키로 중복 방지 |
| 채팅방 | 생성, 목록·이름/카테고리 검색, 공개방·비밀번호방 |
| 채팅 | 실제 WebSocket 송수신, 방별 격리, DB 저장, 최근 100개 복구, 이전 대화 조회 |
| 연결 관리 | 실제 참여 인원, 25초 heartbeat, 지수 간격 재접속, ID 기준 수신 중복 제거 |
| 기본 보호 | 서버 입력 검증, BCrypt, SQL 파라미터 바인딩, Origin 확인, 요청 헤더 확인, 세션 전송 간격 |
| 화면 | PC·태블릿·모바일 대응, 키보드 모달 조작, 로딩·빈 화면·오류 표시 |

원본 시안의 이미지 첨부, 이모티콘 스토어, 음성 댓글, 대댓글, 투표, 공지 관리, 신고·운영자 차단, 임시방 만료, 회원 계정은 이번 구현 범위에 포함하지 않았습니다. 작동하지 않는 기능을 되는 것처럼 표시하지 않도록 관련 버튼과 가상 통계를 제거했습니다. `인기글`은 전체 기간 추천순이고, 시간별 실시간 순위가 아닙니다.

## 4. 디렉터리 구조와 역할

이번 버전은 **README를 만들기 위해 폴더를 쪼개지 않고, 실제로 관리에 도움이 되는 기능 단위만 폴더로 분리**했습니다. 작은 기능 안에서는 Controller/Service/Repository/DTO를 별도 하위 폴더로 다시 나누지 않고 같은 기능 폴더에 둡니다. 파일 수가 크게 늘어날 때만 그때 하위 패키지를 추가하면 됩니다.

```text
CommunityCRUD/
├─ README.md                         # 프로젝트 전체 설명과 실행 방법
├─ compose.yaml                     # MySQL → Spring Boot → Nginx 실행
├─ compose.dev.yaml                 # 로컬 프런트 개발용 추가 설정
├─ .env.example                     # 공유 가능한 환경 변수 예시
├─ scripts/
│  ├─ README.md                     # 환경 초기화 스크립트 설명
│  └─ init_env.py                   # 임의 비밀번호로 .env 생성
├─ docs/
│  ├─ README.md                     # 문서 안내
│  ├─ API.md                        # HTTP / WebSocket 계약
│  ├─ LEARNING_GUIDE.md             # 기능 흐름을 따라가는 학습 가이드
│  └─ VERIFICATION.md               # 검증 범위와 결과
├─ frontend/
│  ├─ README.md                     # 프런트 전체 구조 설명
│  └─ src/
│     ├─ App.vue                    # 공통 레이아웃을 조립하는 최상위 화면
│     ├─ main.js                    # Vue 앱 시작점
│     ├─ style.css                  # 전역 스타일
│     ├─ router/
│     │  └─ index.js                # URL ↔ 화면 연결
│     ├─ stores/
│     │  └─ appState.js             # 갤러리/방/닉네임/토스트 공통 상태
│     ├─ services/
│     │  └─ api.js                  # fetch 공통 처리와 HTTP 오류 처리
│     ├─ utils/
│     │  └─ date.js                 # 날짜 표시 함수
│     ├─ components/
│     │  ├─ Modal.vue               # 여러 화면에서 재사용하는 모달
│     │  └─ layout/
│     │     ├─ SiteHeader.vue       # 브랜드·검색·상단 메뉴
│     │     ├─ LeftSidebar.vue      # 갤러리·채팅방 바로가기
│     │     ├─ RightSidebar.vue     # 실시간 라운지·글쓰기 보조 영역
│     │     └─ SiteFooter.vue       # 공통 푸터
│     └─ features/
│        ├─ board/
│        │  ├─ BoardView.vue        # 목록·검색·정렬·페이지 이동
│        │  ├─ PostView.vue         # 상세·추천·댓글·삭제
│        │  └─ WriteView.vue        # 작성·수정 공용 폼
│        └─ chat/
│           ├─ ChatView.vue         # 방 검색·생성·선택
│           ├─ ChatPanel.vue        # 메시지 목록·입력 UI
│           └─ useChat.js           # WebSocket·재접속·ack·과거 이력
└─ backend/
   ├─ README.md                     # 백엔드 전체 구조 설명
   └─ src/main/java/com/joji/community/
      ├─ CommunityApplication.java  # Spring Boot 시작점
      ├─ board/
      │  ├─ BoardController.java    # 게시판 HTTP API
      │  ├─ BoardService.java       # 게시판 업무 규칙·트랜잭션
      │  ├─ BoardRepository.java    # 게시판 SQL·DB 접근
      │  └─ BoardModels.java        # 게시판 요청·응답 DTO
      ├─ chat/
      │  ├─ ChatController.java     # 채팅방 HTTP API
      │  ├─ ChatService.java        # 채팅방 업무 규칙
      │  ├─ ChatRepository.java     # 채팅 SQL·DB 접근
      │  ├─ ChatModels.java         # 채팅 요청·응답 DTO
      │  └─ ChatHandler.java        # WebSocket 메시지 처리
      ├─ common/
      │  ├─ SessionController.java  # 익명 세션 확인 API
      │  ├─ AnonymousSession.java   # 익명 사용자 세션 관리
      │  ├─ PasswordManager.java    # 비밀번호 해시·검증
      │  ├─ JdbcInsertHelper.java   # INSERT 후 PK 조회 공통 기능
      │  ├─ RequestGuard.java       # 요청 헤더·간격 보호
      │  └─ ApiExceptionHandler.java# 공통 예외 응답
      ├─ config/
      │  └─ WebSocketConfig.java    # WebSocket 경로·Handshake 설정
      └─ bootstrap/
         └─ DemoData.java           # 예시 데이터 초기화
```

### 백엔드 읽는 법

게시판은 같은 `board` 폴더 안에서 `BoardController → BoardService → BoardRepository → MySQL` 순서로 읽으면 됩니다. `BoardModels`는 각 계층에서 주고받는 요청·응답 자료형입니다. 채팅은 `ChatController → ChatService`로 방 입장 흐름을 본 뒤 `WebSocketConfig → ChatHandler → ChatRepository` 순서로 실시간 메시지 흐름을 보면 이해하기 쉽습니다.

이 프로젝트 규모에서는 `board/controller`, `board/service`, `board/dto`처럼 한 파일짜리 하위 폴더를 만드는 것보다 관련 파일을 `board`에 함께 두는 편이 찾기 쉽습니다. 나중에 파일 수가 충분히 많아졌을 때만 하위 패키지를 추가하는 것을 권장합니다.

### 프런트 읽는 법

`main.js → App.vue → router/index.js`로 앱의 큰 골격을 본 뒤, `features/board` 또는 `features/chat`으로 들어가면 됩니다. 채팅도 `views/components/composables`로 다시 한 단계씩 나누지 않고 관련 파일을 한 기능 폴더에 모았습니다. 공통 UI만 `components`, 서버 통신은 `services`, 공통 상태는 `stores`로 분리했습니다.

README는 **폴더를 만들기 위한 이유가 아니라, 관리상 필요해서 이미 존재하는 폴더를 설명하는 용도**로만 배치했습니다.

## 5. 예시 글과 개인정보

`.env`의 `APP_SEED_DEMO=true`이면 첫 실행에 **명확히 `[예시]`로 표시된 학습 게시글 8개**를 DB에 넣습니다. 접속자·조회수·추천·댓글 수를 가짜로 올리지 않습니다. 예시 글의 수정·삭제 비밀번호는 `DEMO_POST_PASSWORD`이며 DB에는 해시만 저장합니다.

예시를 원하지 않으면 **최초 실행 전에** `APP_SEED_DEMO=false`로 설정하세요. 나중에 false로 바꾸어도 이미 저장된 글은 삭제되지 않습니다. 예시 글을 삭제한 뒤 재시작해도 다시 생성하지 않도록 초기화 기록을 남깁니다.

닉네임은 원하는 대로 쓸 수 있고, 서버 발급 식별코드가 함께 표시됩니다. 실제 IP는 화면에 표시하거나 애플리케이션 테이블에 저장하지 않습니다. 익명은 로그인 없는 가명 활동이라는 뜻이며, 인프라 운영자가 추적할 수 없다는 보장은 아닙니다. Nginx의 일반 접근 로그는 비활성화했지만 오류 로그·외부 인프라 로그 정책은 별도로 관리해야 합니다.

## 6. 자주 쓰는 명령

```bash
# 로그 확인
docker compose logs -f backend

# 중지: DB 볼륨은 남습니다.
docker compose down

# 수정한 코드를 반영해 재빌드
docker compose up --build -d

# DB 접속: 프롬프트가 뜨면 .env의 MYSQL_PASSWORD를 입력합니다.
# MYSQL_USER 또는 MYSQL_DATABASE를 바꾸었다면 아래 인자도 바꾸세요.
docker compose exec mysql mysql -u joji_app -p joji_community
```

**`docker compose down -v`는 DB 볼륨까지 지우는 초기화 명령입니다. 기존 글·댓글·채팅을 유지하려면 사용하지 마세요.** MySQL의 초기 사용자 비밀번호는 처음 볼륨 생성 때 반영됩니다. 기존 볼륨이 있을 때 `.env` 비밀번호만 변경하면 접속 오류가 나므로 DB 내부에서 계정 비밀번호도 변경해야 합니다.

`docker compose config`는 치환된 비밀번호를 출력할 수 있으니 출력 결과를 그대로 공유하지 마세요.

## 7. 화면을 수정하며 공부하기

Node.js 22.12 이상을 준비한 후:

```bash
# DB와 백엔드를 띄우고 로컬 개발용 포트만 엽니다.
docker compose -f compose.yaml -f compose.dev.yaml up --build -d mysql backend

cd frontend
npm ci
npm run dev
```

접속: http://localhost:5173. `.vue`를 저장하면 변경이 반영됩니다. Vite가 `/api`와 `/ws`를 Spring Boot로 전달하므로 별도의 프런트 DB 비밀번호가 필요 없습니다.

Vue 코드 정리: `npm run format`. 배포 빌드: `npm run build`.

백엔드를 IDE에서 직접 실행하려면 `mysql`만 기동하고, Java 17+와 Maven 3.9+를 준비합니다. IDE Run Configuration에 `.env` 값을 환경 변수로 넣고 `DB_HOST=localhost`, `DB_PORT=3306`을 설정합니다. **Spring Boot/Maven은 루트 `.env` 파일을 자동으로 읽지 않습니다.** Docker Compose가 환경 변수로 주입해 주는 것과 다릅니다. 백엔드 컨테이너가 이미 8080 포트를 사용 중이면 먼저 중지하세요.

```bash
cd backend
mvn test
mvn spring-boot:run
```

`mvn test`는 H2 기반이라 실제 MySQL이나 `.env` 없이 실행할 수 있습니다. `spring-boot:run`은 DB 환경 변수가 필요합니다.

## 8. 실행이 안 될 때

- **8088 포트 충돌:** `.env`의 `HTTP_PORT`와 `APP_ORIGINS`의 주소를 함께 변경합니다.
- **403 / 채팅 연결 실패:** 접속한 주소가 `APP_ORIGINS`에 정확히 등록되어 있는지 확인합니다. `localhost`와 `127.0.0.1`, IP, 포트는 서로 다른 출처입니다. 쉼표 뒤에 공백을 넣지 않습니다.
- **메시지 전송 제한:** 채팅·게시글·댓글은 세션별 기본 대기 시간이 있습니다. 채팅은 3초를 기다려 주세요.
- **DB 접속 실패:** `docker compose logs mysql backend`에서 상태를 확인합니다. 비밀번호 변경과 기존 볼륨 여부를 확인하세요.
- **서버 재시작 후 비밀방 재입장 요구:** 현재 HTTP 세션은 서버 메모리에 있으므로 재시작하면 익명 식별자와 비밀방 허가가 초기화됩니다. DB 대화 기록은 유지됩니다.
- **다른 PC에서 접속:** 호스트의 웹 포트를 열고 해당 주소를 `APP_ORIGINS`에 추가합니다. 실제 인터넷 공개에는 HTTPS·운영 보호 정책을 먼저 적용해야 합니다.

## 9. 이 버전의 경계

학습과 소규모 단일 서버 실행을 위한 구현입니다. 여러 Spring Boot 서버로 확장할 때는 공유 세션과 Redis Pub/Sub 등 메시지 전달 구조가 필요합니다. 브라우저 세션을 새로 만들면 추천·도배 제한을 우회할 수 있어 현재 보호만으로 공개 대규모 익명 서비스를 운영하기에 충분하지 않습니다. 신고·관리자 도구, IP 기반 추가 제한, 보존·삭제 정책, 백업, 모니터링은 별도 구현 대상입니다.

검색은 MySQL `LOCATE` 기반의 문자열 검색입니다. 규모가 커지면 실제 실행 계획과 요구사항을 보고 FULLTEXT 또는 Elasticsearch를 추가하세요. 현재는 Elasticsearch 없이 실행됩니다. 조회수는 글 상세 API 호출마다 증가하며 순방문자 수가 아닙니다.

채팅은 저장 성공 후 방송합니다. 끊김 구간에는 실시간 이벤트가 누락될 수 있으나 재접속하면 최근 DB 기록을 불러옵니다. 전송 확인이 끊긴 경우 자동 재전송하지 않습니다. 대화 기록을 먼저 확인해 사용자의 수동 재전송에 따른 중복을 피하세요.

## 참고 공식 문서

- [Vue 가이드](https://vuejs.org/guide/introduction.html)
- [Vite 실행 환경 안내](https://vite.dev/guide/)
- [Spring WebSocket API와 Origin 설정](https://docs.spring.io/spring-framework/reference/web/websocket/server.html)
- [Docker Compose 환경 변수](https://docs.docker.com/compose/how-tos/environment-variables/variable-interpolation/)
