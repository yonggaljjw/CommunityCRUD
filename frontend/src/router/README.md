# router

URL과 feature 화면을 연결합니다. 새 페이지를 추가할 때는 먼저 해당 feature에 View를 만든 뒤 `index.js`의 `routes`에 경로를 등록합니다.

## 기본 개념

### SPA Router

SPA에서는 사용자가 링크를 눌렀을 때 매번 서버에서 새로운 HTML 문서를 받기보다 JavaScript가 현재 URL을 보고 보여 줄 Vue 컴포넌트를 바꿉니다.

Vue Router가 이 역할을 담당합니다.

```text
/posts/10
   ↓ router
PostView.vue
```

### `createRouter`

라우터 객체를 만들고 `routes` 배열에 URL과 컴포넌트의 대응 관계를 정의합니다.

```js
{ path: "/posts/:id", component: PostView }
```

`:id`는 동적인 경로 파라미터입니다.

### `RouterView`

현재 URL에 해당하는 컴포넌트를 실제로 렌더링하는 자리입니다. `App.vue`의 `<RouterView />` 안에 `BoardView`, `PostView`, `ChatView` 등이 교체되어 표시됩니다.

### `RouterLink`

내부 페이지 이동에는 `<RouterLink>`를 사용합니다. 브라우저 전체 새로고침 없이 라우터가 화면을 전환합니다.

### History Mode

`createWebHistory()`를 사용하면 주소가 `/#/posts/1`이 아니라 `/posts/1`처럼 일반적인 URL로 보입니다. 대신 운영 서버(Nginx)는 존재하지 않는 프런트 URL 요청도 `index.html`로 돌려줘야 Vue Router가 처리할 수 있습니다.

### 404 경로

`/:pathMatch(.*)*`는 앞에서 매칭되지 않은 모든 주소를 잡아 프런트용 "페이지를 찾을 수 없습니다" 화면을 보여 줍니다.
