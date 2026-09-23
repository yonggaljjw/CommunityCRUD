# features/board

갤러리 게시판 기능의 화면을 한 폴더에서 관리합니다.

## 파일 역할

- `BoardView.vue`: 목록, 검색, 정렬, 페이지 이동
- `PostView.vue`: 게시글 상세, 추천, 댓글, 삭제
- `WriteView.vue`: 게시글 작성·수정 공용 폼

현재 View가 3개뿐이므로 별도의 `views/` 폴더는 만들지 않았습니다. 게시판 관련 파일 수가 크게 늘어날 때 구조를 다시 나누면 됩니다.

## 기본 개념

### `ref`

`ref()`는 Vue에서 변경되는 값을 반응형으로 관리합니다.

예를 들어 게시글 목록의 로딩 여부가 `false → true → false`로 바뀌면 이를 사용하는 화면도 자동으로 갱신됩니다.

```js
const loading = ref(false);
```

### `computed`

기존 상태를 이용해 새로운 값을 계산할 때 사용합니다. 원본 상태가 바뀌면 계산 결과도 자동으로 다시 계산됩니다.

예를 들어 현재 URL의 `galleryId`와 전역 갤러리 목록을 이용해 현재 갤러리를 찾는 값에 사용할 수 있습니다.

### `watch`

특정 값이 변경되는 순간 비동기 조회 같은 동작을 실행할 때 사용합니다. `BoardView`에서는 URL의 갤러리·검색어·정렬 방식이 바뀌면 게시글 목록을 다시 조회합니다.

### Route Params와 Query

URL에는 두 종류의 값을 자주 사용합니다.

```text
/galleries/3        → params.id = 3
/?sort=hot&q=vue    → query.sort, query.q
```

- params: URL 경로 자체의 일부
- query: `?key=value` 형태의 검색/정렬 조건

Vue Router의 `useRoute()`로 현재 값을 읽고 `useRouter()`로 다른 주소로 이동합니다.

### CRUD

게시판은 CRUD 학습에 좋은 예입니다.

- Create: 글/댓글 작성
- Read: 목록·상세 조회
- Update: 글 수정
- Delete: 글/댓글 삭제

화면이 직접 DB를 만지는 것이 아니라 `api.js → Spring Controller → Service/Repository → DB` 순서로 처리됩니다.

### 비동기 요청과 경쟁 상태

사용자가 검색 조건을 빠르게 바꾸면 이전 요청보다 새 요청이 먼저 끝날 수도 있습니다. `BoardView`의 `sequence` 값은 늦게 도착한 과거 응답이 최신 화면을 덮어쓰는 것을 막기 위한 간단한 방법입니다.

## 목록 조회 흐름

```text
BoardView
  ↓ api("/posts?... ")
services/api.js
  ↓ HTTP GET
Spring BoardController
  ↓
BoardRepository
  ↓
MySQL
  ↓ JSON
BoardView의 result 갱신
  ↓
Vue가 화면 자동 렌더링
```

프런트와 백엔드의 연결을 공부할 때 가장 먼저 따라가 보기 좋은 흐름입니다.
