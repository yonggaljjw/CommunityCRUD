# stores

화면 여러 곳에서 공유하는 앱 상태입니다. 현재 작은 프로젝트라 Vue `reactive`만 사용하며, 갤러리/채팅방/닉네임/토스트/초기 로딩 상태를 관리합니다.

## 기본 개념

### 상태(State)

화면에 표시되거나 여러 로직이 참고하는 현재 데이터를 상태라고 합니다.

예:

```text
현재 닉네임
갤러리 목록
채팅방 목록
토스트 메시지
서버 초기 연결 완료 여부
```

### Local State와 Global State

한 화면에서만 필요한 값은 해당 컴포넌트의 `ref`로 관리하는 편이 단순합니다. 여러 화면/컴포넌트가 같이 써야 하는 값은 공통 store에서 관리하면 중복을 줄일 수 있습니다.

```text
BoardView만 필요 → local ref
Header + Chat + Board 모두 필요 → shared state
```

### `reactive`

객체의 여러 속성을 반응형 상태로 만들 수 있습니다. `state.nickname`이 바뀌면 이 값을 사용하는 Header 등의 화면도 자동으로 변경됩니다.

### Pinia를 사용하지 않은 이유

Pinia는 Vue에서 많이 사용하는 정식 상태관리 라이브러리지만, 현재 프로젝트의 공통 상태가 작기 때문에 `reactive` 하나로도 충분합니다.

상태가 많아지고 기능별 store, getter, action, 개발 도구 추적 등이 필요해질 때 Pinia로 옮기는 것을 고려하면 됩니다.

### bootstrap

앱 시작 시 필요한 여러 API를 `Promise.all()`로 동시에 호출합니다.

```text
/session   ┐
/galleries ├─ 동시에 요청 → 모두 완료 → state.ready = true
/rooms     ┘
```

순차적으로 하나씩 기다리는 것보다 서로 의존하지 않는 요청을 병렬로 보내 초기 로딩 시간을 줄일 수 있습니다.

### localStorage

닉네임은 브라우저 `localStorage`에 저장해 새로고침 후에도 유지합니다. 서버 세션과 달리 브라우저 로컬에 저장되는 값이므로 비밀번호 같은 민감한 정보는 넣지 않습니다.
