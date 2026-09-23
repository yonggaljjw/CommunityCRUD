# common 공통 기능

`board`와 `chat` 양쪽에서 함께 쓰는 작은 공통 기능을 모았습니다. 파일이 많지 않아 세션·보안·JDBC·웹 예외를 다시 여러 하위 폴더로 나누지 않았습니다.

## 파일 역할

- `AnonymousSession.java`: 익명 사용자 식별과 세션 관리
- `PasswordManager.java`: BCrypt 비밀번호 해시·검증
- `JdbcInsertHelper.java`: INSERT 후 생성 PK 조회
- `SessionController.java`: 현재 익명 세션 정보 API
- `RequestGuard.java`: 쓰기 요청의 헤더와 Origin 검사
- `ApiExceptionHandler.java`: 예외를 일관된 JSON 응답으로 변환

공통 파일이 크게 늘어나 서로 찾기 어려워질 때 `security`, `web` 같은 하위 패키지 분리를 고려하면 됩니다.

## 기본 개념

### 세션(Session)

HTTP 자체는 기본적으로 각 요청이 서로 독립적입니다. 서버가 같은 브라우저의 연속된 요청을 구분하기 위해 사용하는 대표적인 방법이 세션입니다.

이 프로젝트는 회원가입 없이 익명으로 동작하기 때문에 `HttpSession`에 익명 식별값과 비밀 채팅방 입장 여부 등을 저장합니다.

```text
브라우저 쿠키의 세션 ID
  ↓
서버 HttpSession
  ├─ actor
  └─ room:3 = true
```

### Hash와 BCrypt

비밀번호는 DB에 평문 그대로 저장하면 안 됩니다. `PasswordManager`는 BCrypt를 이용해 비밀번호를 단방향 해시로 저장합니다.

- 암호화: 키가 있으면 원문으로 복호화 가능
- 해시: 원문으로 되돌리는 용도가 아님

로그인/수정 시에는 입력받은 비밀번호를 저장된 BCrypt 해시와 비교합니다.

### Servlet Filter

`RequestGuard`는 `OncePerRequestFilter`를 상속합니다. Filter는 Controller에 요청이 도착하기 전에 공통 검사를 수행할 수 있는 계층입니다.

이 프로젝트에서는 쓰기 API에 `X-Requested-With` 헤더가 있는지, 허용된 Origin에서 온 요청인지 확인합니다.

### 전역 예외 처리

각 Controller마다 `try/catch`를 반복하지 않고 `@RestControllerAdvice`의 `ApiExceptionHandler`가 공통 예외를 처리합니다.

예를 들어 Validation 오류나 잘못된 요청을 프런트가 읽기 쉬운 다음 형태로 통일합니다.

```json
{ "message": "요청 형식이 올바르지 않습니다." }
```

### JDBC와 생성 PK

MySQL의 AUTO_INCREMENT 컬럼은 INSERT 시 DB가 새 ID를 생성합니다. `JdbcInsertHelper`는 `KeyHolder`를 사용해 INSERT 직후 생성된 ID를 다시 받아옵니다.

이 ID로 새로 생성된 게시글이나 채팅방을 다시 조회할 수 있습니다.

## 공부할 때 볼 포인트

`common`은 각각 독립된 기능처럼 보이지만 Controller/Service/Repository의 여러 코드에서 반복되는 문제를 한곳으로 모은 예입니다. "두 군데 이상에서 똑같은 코드가 반복되기 시작하는가?"를 공통화 판단 기준으로 보면 좋습니다.
