# Frontend 안내

Vue 3 + Vite 프런트엔드입니다. 화면 파일도 **기능 단위로 먼저 묶고**, 한두 파일을 위한 중간 폴더는 만들지 않습니다.

## 디렉터리 역할

- `src/features/board/`: 게시판 목록·상세·작성 화면
- `src/features/chat/`: 채팅 화면·패널·WebSocket composable
- `src/components/`: 여러 기능에서 재사용하는 UI
- `src/services/`: 서버 통신
- `src/stores/`: 공통 상태
- `src/router/`: URL과 화면 연결
- `src/utils/`: 순수 도우미 함수

`main.js → App.vue → router → 각 feature` 순서로 읽으면 전체 흐름을 잡기 쉽습니다.

## 먼저 알아둘 프런트엔드 기본 개념

### Vue 3

Vue는 데이터를 화면에 반영하고 사용자 이벤트에 따라 UI를 갱신하는 프런트엔드 프레임워크입니다. 이 프로젝트는 Vue 3의 **Composition API**와 `<script setup>` 문법을 사용합니다.

### SFC(Single File Component)

`.vue` 파일 하나 안에 컴포넌트의 JavaScript와 HTML 템플릿을 함께 작성하는 방식을 SFC라고 합니다.

```vue
<script setup>
// 상태와 로직
</script>

<template>
  <!-- 화면 -->
</template>
```

전역 CSS는 `style.css`에서 관리합니다.

### 반응성(Reactivity)

Vue는 상태가 바뀌면 그 값을 사용하는 화면을 자동으로 다시 렌더링합니다.

- `ref()`: 하나의 값이나 객체를 반응형으로 관리
- `reactive()`: 객체 자체를 반응형으로 관리
- `computed()`: 다른 상태를 이용해 계산된 값 생성
- `watch()`: 특정 값의 변경을 감지해 작업 실행

### 컴포넌트

화면을 재사용 가능한 작은 UI 단위로 나눈 것입니다. `App.vue` 하나에 모든 코드를 넣는 대신 Header, Sidebar, BoardView, ChatPanel 등으로 책임을 분리합니다.

### SPA와 Router

이 프로젝트는 페이지를 이동할 때 서버에서 HTML 전체를 다시 받기보다 Vue Router가 화면 컴포넌트를 교체하는 SPA(Single Page Application) 구조입니다.

### API 통신

Vue는 `fetch()`를 이용해 Spring Boot의 `/api/...`에 요청합니다. 공통 HTTP 처리는 `services/api.js`에 모아 각 화면에서 반복하지 않도록 했습니다.

## 추천 학습 순서

1. `src/main.js`: Vue 앱이 어떻게 시작되는지
2. `src/App.vue`: 공통 레이아웃과 `<RouterView>`
3. `src/router/`: URL과 화면 연결
4. `src/stores/`: 공통 상태
5. `src/services/`: 백엔드 API 호출
6. `src/features/board/`: 일반적인 CRUD 화면
7. `src/features/chat/`: WebSocket 실시간 기능
8. `src/components/`: 재사용 UI와 props/emit/slot
