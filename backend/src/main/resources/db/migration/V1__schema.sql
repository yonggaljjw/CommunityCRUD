-- Flyway가 최초 실행 때 한 번 적용합니다. 변경은 V2, V3 파일로 추가하세요.
-- 비밀번호는 원문이 아니라 BCrypt 해시만 저장합니다. IP 주소는 저장·공개하지 않습니다.
CREATE TABLE galleries (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 slug VARCHAR(40) NOT NULL UNIQUE,
 name VARCHAR(60) NOT NULL,
 description VARCHAR(255) NOT NULL
);
CREATE TABLE posts (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 gallery_id BIGINT NOT NULL,
 category VARCHAR(20) NOT NULL,
 title VARCHAR(150) NOT NULL,
 content TEXT NOT NULL,
 nickname VARCHAR(20) NOT NULL,
 author_tag VARCHAR(12) NOT NULL,
 password_hash VARCHAR(100) NOT NULL,
 views BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY (gallery_id) REFERENCES galleries(id),
 INDEX idx_posts_gallery_created (gallery_id, created_at, id)
);
CREATE TABLE comments (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 post_id BIGINT NOT NULL,
 nickname VARCHAR(20) NOT NULL,
 author_tag VARCHAR(12) NOT NULL,
 content VARCHAR(1000) NOT NULL,
 password_hash VARCHAR(100) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
 INDEX idx_comments_post (post_id, id)
);
CREATE TABLE post_likes (
 post_id BIGINT NOT NULL,
 actor_id VARCHAR(36) NOT NULL,
 PRIMARY KEY (post_id, actor_id),
 FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);
CREATE TABLE chat_rooms (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 name VARCHAR(60) NOT NULL,
 category VARCHAR(30) NOT NULL,
 description VARCHAR(200) NOT NULL,
 password_hash VARCHAR(100),
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE TABLE chat_messages (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 room_id BIGINT NOT NULL,
 nickname VARCHAR(20) NOT NULL,
 author_tag VARCHAR(12) NOT NULL,
 content VARCHAR(500) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY (room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
 INDEX idx_messages_room (room_id, id)
);
INSERT INTO galleries (slug,name,description) VALUES
 ('free','자유','별일 없는 하루부터 하고 싶은 이야기까지.'),
 ('programming','프로그래밍','막힌 코드, 새로 배운 기술, 개발자의 일상.'),
 ('career','취업·커리어','서류부터 면접까지, 함께 준비하는 취업 이야기.'),
 ('games','게임','오늘의 플레이와 e스포츠 이야기.'),
 ('humor','유머','잠깐 쉬어 가는 웃긴 이야기.'),
 ('daily','일상','취미, 책상, 점심 메뉴. 소소한 하루의 기록.');
INSERT INTO chat_rooms (name,category,description) VALUES
 ('전체 익명 광장','자유','가입 없이 편하게 이야기하는 모두의 광장'),
 ('개발자 라운지','개발 / IT','막힌 코드부터 개발자 일상까지'),
 ('취업 준비 같이해요','취업 / 커리어','자소서와 면접 준비를 함께 이야기해요'),
 ('게임 · e스포츠 토크','게임','경기 보면서 실시간으로 이야기해요');
