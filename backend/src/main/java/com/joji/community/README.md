# Java 패키지 구조

이 패키지는 **기능별로 찾기 쉽게 만드는 것**을 우선합니다. README를 넣기 위해 추가 패키지를 만들지 않습니다.

## 현재 구조

- `board/`: 갤러리, 게시글, 댓글, 추천
- `chat/`: 채팅방, 과거 메시지, WebSocket 실시간 채팅
- `common/`: 세션, 비밀번호, 예외 처리, JDBC 공통 기능
- `config/`: WebSocket 등 애플리케이션 설정
- `bootstrap/`: 학습용 초기 데이터 주입
- `CommunityApplication.java`: Spring Boot 시작점

현재처럼 각 기능의 파일 수가 적을 때는 `board/controller`, `board/service`, `board/dto` 같은 하위 폴더를 만들지 않고 `board`에 함께 둡니다. 기능이 커졌을 때 필요한 계층만 분리하면 됩니다.

## Java 패키지란?

Java의 `package`는 서로 관련된 클래스를 묶는 이름 공간입니다. 파일 시스템의 폴더 구조와 Java 패키지 이름을 맞춰 두면 클래스 위치를 찾기 쉽고 같은 이름의 클래스 충돌도 피할 수 있습니다.

```java
package com.joji.community.board;
```

위 코드는 해당 클래스가 `com.joji.community.board` 패키지에 속한다는 의미입니다.

## Spring의 컴포넌트 스캔

`@SpringBootApplication`이 붙은 `CommunityApplication`이 `com.joji.community`에 있으므로 Spring은 기본적으로 그 아래 패키지를 탐색합니다.

따라서 다음처럼 하위 패키지에 있는 Spring 객체들이 자동으로 등록됩니다.

- `@RestController`
- `@Service`
- `@Repository`
- `@Component`
- `@Configuration`

이 때문에 기능별 패키지를 `com.joji.community` 아래에 두는 구조가 자연스럽습니다.

## 기능 기준 패키지와 계층 기준 패키지

두 가지 방식이 흔히 사용됩니다.

```text
계층 기준
controller/
service/
repository/
```

```text
기능 기준
board/
chat/
```

이 프로젝트는 규모가 작고 게시판/채팅이라는 기능 경계가 명확해 **기능 기준 구조**를 사용합니다. 하나의 기능을 수정할 때 관련 파일을 가까이서 찾을 수 있다는 장점이 있습니다.
