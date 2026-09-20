-- 예시 글을 삭제해도 재시작 때 다시 생성되지 않도록 최초 실행 여부를 별도로 기록합니다.
CREATE TABLE app_settings (
 setting_key VARCHAR(60) PRIMARY KEY,
 setting_value VARCHAR(255) NOT NULL
);
