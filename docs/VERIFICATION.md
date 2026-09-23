# 검증 기록

## 2026-09-23 기능별 리팩토링 검증

이번 작업에서는 과도하게 세분화했던 계층 폴더를 다시 기능 폴더로 평탄화했기 때문에 **경로 깨짐과 문법 오류**를 우선 확인했습니다.

| 항목 | 결과 |
|---|---|
| 프런트 상대 import가 실제 파일을 가리키는지 검사 | 통과 |
| 모든 `.js`의 `node --check` | 통과 |
| 모든 Vue 파일의 `<script setup>` 추출 후 `node --check` | 통과 |
| 프로젝트 내부 Java import 경로 검사 | 통과 |
| `javac` 출력에서 파싱 계열 문법 오류 패턴 검사 | 발견 없음 |
| 관리 목적으로 유지한 기능/공통 폴더의 `README.md` 존재 여부 | 통과 |
| 오래된 클래스명·프런트 경로가 문서에 남았는지 검색 | 발견 없음 |

### 이번 환경에서 전체 빌드를 다시 실행하지 못한 이유

- Maven 실행 파일이 설치되어 있지 않아 `mvn test`를 실행할 수 없었습니다.
- `npm ci`는 외부 npm 레지스트리에서 의존성을 내려받는 중 실행 환경의 네트워크/시간 제한으로 완료되지 않았습니다.
- Docker 실행 엔진이 없어 Dockerfile/Compose를 이용한 빌드로 대신 검증할 수도 없었습니다.

따라서 이번 리팩토링본은 **프런트 상대 import, JS/Vue 문법, Java 내부 import 경로와 파싱 수준 검증은 완료했지만 최종 `mvn test`와 `npm run build`는 사용자 환경에서 한 번 더 실행하는 것이 필요합니다.**

권장 확인 명령:

```bash
cd backend
mvn test

cd ../frontend
npm ci
npm run build
```

또는 프로젝트 루트에서:

```bash
docker compose up --build -d
docker compose ps
```

---

## 2026-09-05 기존 기능 구현 당시 검증 기록

리팩토링 전 동일 기능 버전에서는 아래 검증이 완료되어 있었습니다. 이번 구조 변경 이후에는 위 2026-09-23 항목처럼 전체 빌드를 다시 실행하지 못했으므로, 아래 결과를 현재 구조의 재검증 결과로 오해하면 안 됩니다.

| 항목 | 당시 결과 |
|---|---|
| Vue 프로덕션 빌드 (`npm run build`) | 통과 |
| Spring Boot Java 17 컴파일 | 통과 |
| Flyway V1·V2 마이그레이션 | H2 MySQL 호환 모드에서 통과 |
| 서버 통합 테스트 | 6개 통과, 실패 0, 오류 0 |
| HTTP 서버와 Java WebSocket 클라이언트 간 실제 연결 | 통과 |
| Compose·Spring 설정 YAML 구문 해석 | 통과 |
| .env 생성 스크립트 | 임의 비밀번호 생성, 기존 파일 보존 확인 |
| 프런트 의존성 잠금 파일 | package.json과 일치 |

통합 테스트 이름은 다음과 같습니다: `uniqueLikesAndSafeSearch`, `privateRoomHistoryRequiresPassword`, `webSocketRejectsPrivateRoomAndForeignOrigin`, `realWebSocketBroadcastIsolationValidationAndPersistence`, `validationOriginAndRateLimit`, `postCrudPasswordAndCascade`.

기존 테스트는 `@SpringBootTest`로 실제 내장 Tomcat 서버를 띄우고, REST는 MockMvc/Java HttpClient, 실시간 통신은 Java WebSocket 클라이언트로 확인했습니다. H2 MySQL 호환 모드는 실제 MySQL과 완전히 같지 않으므로 문자 정렬, 실행 계획, 잠금 경합은 실제 MySQL에서도 확인해야 합니다.
