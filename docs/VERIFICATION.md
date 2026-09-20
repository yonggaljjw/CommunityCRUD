# 검증 기록

검증일: 2026-09-05

## 통과한 검증

| 항목 | 결과 |
|---|---|
| Vue 프로덕션 빌드 (`npm run build`) | 통과 |
| Spring Boot Java 17 컴파일 | 통과 |
| Flyway V1·V2 마이그레이션 | H2 MySQL 호환 모드에서 통과 |
| 서버 통합 테스트 | 6개 통과, 실패 0, 오류 0 |
| HTTP 서버와 Java WebSocket 클라이언트 간 실제 연결 | 통과 |
| Compose·Spring 설정 YAML 구문 해석 | 통과 |
| .env 생성 스크립트 | 임의 비밀번호 생성, 기존 파일 보존 확인 |
| 프런트 의존성 잠금 파일 | package.json과 일치 |

## 통합 테스트 목록

- `uniqueLikesAndSafeSearch`
- `privateRoomHistoryRequiresPassword`
- `webSocketRejectsPrivateRoomAndForeignOrigin`
- `realWebSocketBroadcastIsolationValidationAndPersistence`
- `validationOriginAndRateLimit`
- `postCrudPasswordAndCascade`

테스트는 `@SpringBootTest`로 실제 내장 Tomcat 서버를 띄웁니다. REST는 MockMvc와 Java HttpClient를 사용하고, 실시간 통신은 서로 다른 쿠키 저장소를 가진 Java WebSocket 클라이언트로 검증합니다. 데이터 접근·SQL·비밀번호 해싱은 가짜 구현으로 바꾸지 않았습니다.

Mockito의 JVM 동적 attach가 필요하지 않도록 테스트 전용 `mock-maker-subclass` 설정을 포함했습니다. DB 자동 생성 값 중 ID만 명시적으로 요청하여 드라이버별 반환 컬럼 차이를 처리합니다.

## 검증하지 않은 영역

- 이 작업 환경에는 Docker 실행 엔진이 없어 **Docker 이미지 빌드·Compose 기동·MySQL 8.4 컨테이너·Nginx 프록시 전체 경로는 실행 검증하지 못했습니다.**
- H2의 MySQL 호환 모드는 실제 MySQL과 완전히 같지 않습니다. MySQL의 문자 정렬, 실행 계획, 잠금 경합은 실제 DB에서 확인해야 합니다.
- 브라우저 자동화와 화면 스크린샷 검수는 수행하지 않았습니다. 반응형 CSS와 모달 키보드 처리는 구현되어 있지만 실제 기기별 확인이 필요합니다.
- 대규모 동시 접속 부하, 장애 복구, 다중 서버 메시지 전달, 운영 보안 점검은 수행하지 않았습니다.

## 사용자 환경에서 마지막 확인

README의 빠른 실행과 10단계 확인 절차를 진행하세요. 특히 일반 창·시크릿 창 사이의 실시간 대화, 비밀번호방, 재시작 후 DB 보존을 확인하면 전체 배포 경로를 검증할 수 있습니다.
