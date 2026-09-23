package com.joji.community.bootstrap;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** APP_SEED_DEMO=true일 때 학습용 예시 게시글을 최초 1회 넣습니다. */
@Component
public class DemoData implements ApplicationRunner {
  private final JdbcTemplate db;
  private final boolean enabled;
  private final String password;

  public DemoData(
      JdbcTemplate db,
      @Value("${APP_SEED_DEMO:false}") boolean enabled,
      @Value("${DEMO_POST_PASSWORD:}") String password) {
    this.db = db;
    this.enabled = enabled;
    this.password = password;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!enabled
        || db.queryForObject(
                "SELECT COUNT(*) FROM app_settings WHERE setting_key='demo_seeded'", Long.class)
            > 0) return;
    if (password.length() < 4 || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new IllegalArgumentException(
          "학습 예시를 사용하려면 .env의 DEMO_POST_PASSWORD를 4자 이상, UTF-8 72바이트 이하로 설정하세요.");
    String hash = new BCryptPasswordEncoder().encode(password);
    String[][] examples = {
      {
        "1",
        "일반",
        "JOJI COMMINITY에 오신 것을 환영합니다",
        "여기는 익명으로 이야기를 나누는 공간입니다.\n\n"
            + "갤러리에서 글을 쓰고 댓글로 대화해 보세요. 실시간 채팅방을 만들거나 이미 열린 방에 참여할 수도 있습니다.\n\n"
            + "이 글은 학습을 위해 생성된 예시 글입니다. 모든 예시 글의 수정·삭제 비밀번호는 .env의 DEMO_POST_PASSWORD입니다."
      },
      {
        "2",
        "정보",
        "Vue에서 Spring Boot까지, 글 하나가 저장되는 과정",
        "[학습용 예시 글]\n\n"
            + "1. Vue의 WriteView에서 제목과 내용을 입력합니다.\n"
            + "2. api.js가 POST /api/posts로 JSON을 전송합니다.\n"
            + "3. Controller에서 입력값을 검증합니다.\n"
            + "4. Service에서 비밀번호를 BCrypt로 해싱합니다.\n"
            + "5. Repository의 INSERT가 MySQL에 저장합니다.\n"
            + "6. 반환된 글 번호로 상세 화면으로 이동합니다.\n\n"
            + "소스 코드를 이 순서대로 따라 읽어 보세요."
      },
      {
        "3",
        "잡담",
        "자소서 쓰다가 잠깐 쉬어 가는 사람?",
        "[학습용 예시 글]\n\n같이 준비하는 사람들과 취업 준비 채팅방에서 이야기해 보세요.\n회사의 채용 일정 등 실제 정보는 원문 공고를 확인해 주세요."
      },
      {
        "2",
        "질문",
        "WebSocket이랑 일반 HTTP 요청은 뭐가 다른가요?",
        "[학습용 예시 글]\n\n"
            + "게시글 목록은 HTTP 요청에 한 번 응답하면 끝납니다.\n"
            + "WebSocket은 연결을 유지해서 서버가 새 메시지를 즉시 보낼 수 있습니다.\n\n"
            + "이 프로젝트에서는 같은 방에 연결된 소켓에게만 저장된 메시지를 방송합니다."
      },
      {
        "4",
        "잡담",
        "오늘 게임 같이 할 사람들 여기로",
        "[학습용 예시 글]\n\n관심사가 같은 사람끼리 새로운 채팅방을 만들어 보세요. 방 이름과 카테고리를 지정하고, 필요하면 입장 비밀번호도 설정할 수 있습니다."
      },
      {
        "6",
        "후기",
        "책상 위를 정리하니까 코딩할 맛이 나네요",
        "[학습용 예시 글]\n\n작업 공간 이야기나 취미 이야기를 자유롭게 남겨 주세요. 이 예시는 실제 사용자의 후기가 아니라 화면 확인용 데이터입니다."
      },
      {"5", "유머", "버그 하나 잡았는데 두 개가 더 나타났다", "[학습용 예시 글]\n\n개발자의 평범한 하루.\n그래서 변경 후에는 테스트를 실행해 봅니다."},
      {
        "2",
        "정보",
        "Docker Compose로 세 서비스를 함께 실행하기",
        "[학습용 예시 글]\n\n"
            + "루트에서 docker compose up --build -d를 실행하면 MySQL → Spring Boot → Nginx 순으로 준비됩니다.\n\n"
            + "자세한 설명은 README.md와 docs/LEARNING_GUIDE.md를 확인하세요."
      }
    };
    for (String[] e : examples)
      db.update(
          "INSERT INTO posts(gallery_id,category,title,content,nickname,author_tag,password_hash)"
              + " VALUES(?,?,?,?,?,?,?)",
          Long.parseLong(e[0]),
          e[1],
          "[예시] " + e[2],
          e[3],
          "예시작성자",
          "demo",
          hash);
    db.update("INSERT INTO app_settings(setting_key,setting_value) VALUES('demo_seeded','true')");
  }
}
