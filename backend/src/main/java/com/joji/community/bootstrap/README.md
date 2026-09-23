# bootstrap

애플리케이션 시작 시 한 번 수행할 초기화 코드를 둡니다. `DemoData`는 설정이 켜진 경우에만 학습용 예시 게시글을 최초 1회 생성합니다.

## 기본 개념

### Bootstrap / Seed Data

애플리케이션이 처음 실행될 때 필요한 기본 데이터나 개발용 샘플 데이터를 넣는 작업을 흔히 bootstrap 또는 seed라고 부릅니다.

이 프로젝트의 실제 테이블 생성은 별도 DB 초기화 SQL이 담당하고, `DemoData`는 화면과 기능을 바로 확인할 수 있도록 **선택적인 학습용 게시글**을 추가합니다.

### `ApplicationRunner`

`DemoData`는 Spring Boot의 `ApplicationRunner`를 구현합니다. Spring Boot가 애플리케이션 컨텍스트를 준비한 뒤 `run()`을 호출하므로 서버 시작 직후 수행할 작업을 작성할 수 있습니다.

```text
Spring Boot 시작
  ↓
Bean 생성 및 설정
  ↓
ApplicationRunner.run()
  ↓
웹 요청 처리 시작
```

### 멱등성(Idempotency)

초기화 코드가 서버를 재시작할 때마다 같은 데이터를 계속 넣으면 중복이 생깁니다. 그래서 `app_settings`에 `demo_seeded` 여부를 기록해 이미 실행한 경우 다시 넣지 않습니다.

같은 초기화 작업을 여러 번 실행해도 결과가 불필요하게 달라지지 않도록 만드는 것이 중요한 포인트입니다.

### 환경 변수로 기능 켜기

`APP_SEED_DEMO`와 `DEMO_POST_PASSWORD`는 환경 설정에서 주입됩니다. 운영 환경에서 불필요한 샘플 데이터를 만들지 않고 필요할 때만 활성화하기 위한 구조입니다.

## 주의

학습용 seed 데이터와 DB 스키마 마이그레이션은 목적이 다릅니다. 테이블 구조 변경은 migration 도구나 초기 SQL에서 관리하고, 이 폴더는 애플리케이션 시작 후 필요한 데이터 준비에 사용합니다.
