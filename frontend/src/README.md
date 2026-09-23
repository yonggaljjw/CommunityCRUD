# src 구조

Vue 애플리케이션의 실제 소스 코드가 있는 폴더입니다.

## 구조

- `components/`: 여러 화면에서 공유하는 UI
- `features/board/`: 게시판 기능
- `features/chat/`: 채팅 기능
- `services/`: HTTP 같은 외부 통신
- `stores/`: 앱 전역 상태
- `router/`: URL과 화면 연결
- `utils/`: 날짜 포맷 같은 순수 도우미 함수
- `App.vue`: 공통 레이아웃 조립
- `main.js`: Vue 앱 시작
- `style.css`: 전역 스타일

## 기본 개념: Vue 앱이 시작되는 순서

브라우저가 `index.html`을 읽으면 Vite가 `main.js`를 진입점으로 실행합니다.

```text
index.html
  ↓
main.js
  ↓ createApp(App)
App.vue
  ↓
RouterView
  ↓
BoardView / PostView / ChatView ...
```

`main.js`에서는 Vue 앱을 생성하고 Router를 등록한 뒤 HTML의 `#app` 요소에 mount합니다.

### mount란?

Vue가 실제 DOM의 특정 위치를 맡아 화면을 렌더링하기 시작하는 것입니다.

### App.vue의 역할

`App.vue`는 전체 화면 골격을 담당합니다. 헤더·사이드바·푸터를 배치하고 실제 페이지 내용은 `<RouterView>`에 맡깁니다.

기능 로직을 `App.vue`에 다시 몰아넣기보다 해당 `features` 또는 공통 폴더에 두는 것이 현재 구조의 핵심입니다.

### 관심사의 분리

소스를 폴더별로 나누는 이유는 파일 수를 늘리기 위해서가 아니라 서로 다른 책임을 분리하기 위해서입니다.

```text
화면 표현     → components / features
서버 통신     → services
공통 상태     → stores
URL 이동      → router
순수 계산     → utils
```

어떤 코드를 어디에 둘지 헷갈리면 "이 코드가 가장 책임지는 대상이 무엇인가?"를 기준으로 판단하면 좋습니다.
