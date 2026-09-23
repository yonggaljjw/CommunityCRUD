# board 기능

게시판 도메인에 필요한 파일을 한곳에 모았습니다. 작은 프로젝트에서 Controller/Service/Repository/DTO를 각각 한 파일짜리 폴더로 다시 나누지 않아도 역할은 클래스 이름으로 충분히 구분됩니다.

## 파일 역할

- `BoardController.java`: 게시판 HTTP 요청과 응답
- `BoardService.java`: 비밀번호 확인, 수정·삭제 등 업무 규칙과 트랜잭션
- `BoardRepository.java`: 게시글·댓글·추천 SQL과 DB 접근
- `BoardModels.java`: API 요청·응답 DTO

추천 읽기 순서: `BoardController → BoardService → BoardRepository`, 필요한 요청/응답 구조는 중간중간 `BoardModels`를 확인하세요.

## 기본 개념

### Controller

Controller는 브라우저가 보낸 HTTP 요청이 가장 먼저 도착하는 곳입니다.

```java
@GetMapping("/posts/{id}")
@PostMapping("/posts")
@DeleteMapping("/posts/{id}")
```

이런 애너테이션으로 HTTP 메서드와 URL을 Java 메서드에 연결합니다. Controller에서는 요청 파라미터를 읽고 기본적인 입력 검증을 한 뒤 실제 업무 처리를 Service나 Repository에 맡깁니다.

### Service

Service는 **업무 규칙**을 담당합니다. 예를 들어 게시글을 수정할 때는 단순히 `UPDATE` SQL만 실행하는 것이 아니라 작성 비밀번호가 맞는지 확인하고, 필요한 여러 DB 작업을 하나의 트랜잭션으로 처리해야 합니다.

즉 Repository가 "DB에서 무엇을 읽고 쓸 것인가"를 담당한다면 Service는 "어떤 조건에서 그 작업을 허용할 것인가"를 담당한다고 생각하면 쉽습니다.

### Repository

Repository는 DB 접근을 담당합니다. 이 프로젝트는 JPA가 아니라 `JdbcTemplate`을 사용하므로 SQL을 직접 작성합니다.

```text
Repository
  ↓ JdbcTemplate
SQL 실행
  ↓
MySQL
```

SQL의 `?` 자리에 값을 전달하는 Prepared Statement 방식을 사용하면 사용자 입력을 SQL 문자열에 직접 이어 붙이는 것보다 SQL Injection을 방지하기 쉽습니다.

### DTO와 Java record

`BoardModels`의 `Post`, `Comment`, `PostInput` 등은 DB Entity라기보다 API 데이터를 전달하기 위한 DTO입니다.

```java
public record PasswordInput(String password) {}
```

`record`는 생성자, 접근자, `equals`, `hashCode` 같은 반복 코드를 Java가 자동으로 만들어 주어 단순 데이터 전달 객체에 잘 어울립니다.

### Bean Validation

`@NotBlank`, `@Size`, `@Min`, `@Pattern`은 요청값 검증 규칙입니다. Controller에서 `@Valid`와 함께 사용하면 잘못된 데이터가 Service/Repository까지 내려가기 전에 차단할 수 있습니다.

### 페이지네이션

게시글을 한 번에 전부 가져오지 않고 일정 개수씩 나누어 가져오는 것을 페이지네이션이라고 합니다. 이 프로젝트는 `LIMIT`과 `OFFSET`을 이용해 페이지 단위로 게시글을 조회합니다.

## 게시글 작성 흐름

```text
WriteView.vue
  ↓ POST /api/posts
BoardController.create()
  ↓
BoardService.create()
  ↓ 비밀번호 BCrypt 해시
BoardRepository
  ↓ INSERT
MySQL
  ↓
생성된 Post JSON 반환
```

이 흐름을 따라가면 프런트 입력이 DB에 저장되기까지의 전체 CRUD 구조를 이해하기 좋습니다.
