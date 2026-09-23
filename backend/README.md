# Backend 안내

Spring Boot 3 + Java 17 백엔드입니다. 현재 규모에서는 계층별 폴더를 과도하게 만들지 않고 **기능 단위 패키지**를 우선합니다.

## 디렉터리 역할

- `board/`: 게시판의 Controller, Service, Repository, DTO를 한곳에 둡니다.
- `chat/`: 채팅방 HTTP 처리와 WebSocket 처리 코드를 한곳에 둡니다.
- `common/`: 게시판과 채팅에서 같이 쓰는 세션·보안·JDBC·예외 처리 코드입니다.
- `config/`: Spring 설정입니다.
- `bootstrap/`: 애플리케이션 시작 시 실행하는 초기화 코드입니다.

기능을 읽을 때는 `Controller → Service → Repository → DB` 순서가 가장 이해하기 쉽습니다. 파일이 많아져 탐색이 불편해질 때만 그 기능 폴더 안을 다시 세분화하세요.

## 먼저 알아둘 백엔드 기본 개념

### Spring Boot

Spring 기반 웹 애플리케이션을 비교적 적은 설정으로 실행할 수 있게 해주는 프레임워크입니다. `CommunityApplication.java`의 `main()`에서 Spring Boot가 시작되고, `@Component`, `@Service`, `@Repository`, `@RestController` 등이 붙은 클래스를 찾아 객체로 관리합니다.

### IoC / DI

Spring이 필요한 객체를 직접 생성하고 연결해 주는 방식을 **IoC(Inversion of Control)**라고 하며, 필요한 객체를 생성자 등을 통해 전달받는 것을 **DI(Dependency Injection)**라고 합니다.

예를 들어 `BoardController`가 `new BoardService()`를 직접 만들지 않고 생성자로 `BoardService`를 받습니다. 덕분에 객체 생성 책임과 실제 기능 구현을 분리할 수 있습니다.

### Controller → Service → Repository

이 프로젝트에서 가장 자주 보게 되는 흐름입니다.

```text
브라우저
  ↓ HTTP 요청
Controller   요청/응답, 입력 형식 확인
  ↓
Service      업무 규칙, 권한, 트랜잭션
  ↓
Repository   SQL 실행, DB 접근
  ↓
MySQL
```

모든 요청이 반드시 세 계층을 전부 거쳐야 하는 것은 아닙니다. 단순 조회처럼 별도 업무 규칙이 적으면 Controller가 Repository를 바로 호출하기도 합니다.

### REST API와 JSON

프런트엔드는 `/api/posts`, `/api/rooms` 같은 주소로 HTTP 요청을 보내고 백엔드는 대부분 JSON으로 응답합니다.

- `GET`: 조회
- `POST`: 생성
- `PUT`: 수정
- `DELETE`: 삭제

### DTO

DTO(Data Transfer Object)는 API 요청·응답으로 데이터를 전달하기 위한 자료형입니다. 이 프로젝트는 Java `record`를 사용해 단순 데이터 객체를 간결하게 표현합니다.

### 트랜잭션

여러 DB 작업을 하나의 논리적인 작업으로 묶는 개념입니다. `@Transactional`이 붙은 작업에서 중간에 예외가 발생하면 변경 내용을 되돌릴 수 있어 데이터 일관성을 지키는 데 도움이 됩니다.

## 추천 학습 순서

1. `CommunityApplication.java`에서 서버 시작점 확인
2. `board/`에서 일반적인 HTTP CRUD 흐름 학습
3. `common/`에서 세션·비밀번호·예외 처리 학습
4. `chat/`에서 HTTP와 WebSocket의 차이 학습
5. `config/`에서 WebSocket 연결 설정 확인
6. `bootstrap/`에서 서버 시작 직후 실행되는 코드 확인
