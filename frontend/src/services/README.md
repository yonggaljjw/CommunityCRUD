# services

외부 시스템과 통신하는 코드를 둡니다. `api.js`는 `fetch` 공통 설정, JSON 변환, HTTP 오류 처리를 한곳에서 수행합니다.

## 기본 개념

### Service Layer

프런트엔드에서 `services`는 화면 그 자체가 아니라 서버/API 같은 외부 시스템과 통신하는 책임을 모으는 곳입니다.

각 Vue 컴포넌트마다 다음 코드를 반복하지 않도록 합니다.

- 공통 URL `/api`
- JSON 직렬화
- 공통 헤더
- 쿠키 전송 설정
- HTTP 오류 처리

### `fetch`

브라우저 기본 HTTP 요청 API입니다. `fetch()` 자체는 404나 500 응답에서 자동으로 예외를 던지지 않으므로 `response.ok`를 직접 검사해야 합니다.

### `async / await`

네트워크 요청은 결과가 즉시 나오지 않습니다. `async/await`는 Promise 기반 비동기 코드를 동기 코드처럼 읽기 쉽게 작성하도록 돕습니다.

```js
const posts = await api("/posts");
```

### JSON 직렬화

JavaScript 객체를 HTTP 요청 본문으로 보낼 때 `JSON.stringify()`로 문자열로 변환합니다. 응답은 `response.json()`으로 다시 JavaScript 객체로 변환합니다.

### HTTP Status Code

`api.js`는 실패 시 status를 Error 객체에 보존합니다. 덕분에 호출한 화면에서 상태별로 처리할 수 있습니다.

- `400`: 잘못된 요청
- `403`: 권한/비밀번호 문제
- `404`: 대상 없음
- `409`: 충돌
- `500`: 서버 내부 오류

### 204 No Content

DELETE 성공처럼 응답 본문이 없는 경우 `204`를 사용할 수 있습니다. 이때 `response.json()`을 호출하면 실패하므로 `api.js`는 `null`을 반환합니다.

## 흐름

```text
Vue 화면
  ↓ api(path, options)
api.js
  ↓ fetch
Spring Boot /api/...
  ↓ JSON 응답
api.js
  ↓ 성공 데이터 또는 Error
Vue 화면
```
