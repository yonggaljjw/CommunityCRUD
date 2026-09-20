package com.joji.community;

import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class AnonymousSession {
  private AnonymousSession() {}

  // 로그인 대신 서버가 임의 식별자를 발급합니다. 클라이언트가 다른 사용자의 ID를 지정할 수 없습니다.
  public static String actor(HttpSession session) {
    synchronized (session) {
      if (session.getAttribute("actor") == null)
        session.setAttribute("actor", UUID.randomUUID().toString());
      return (String) session.getAttribute("actor");
    }
  }

  public static String tag(HttpSession session) {
    return actor(session).substring(0, 8);
  }

  // 여러 탭/소켓이 동시에 전송해도 동일한 세션의 대기 시간을 우회하지 못하게 동기화합니다.
  public static void limit(HttpSession session, String action, long milliseconds) {
    synchronized (session) {
      long now = System.currentTimeMillis();
      Long last = (Long) session.getAttribute("rate:" + action);
      if (last != null && now - last < milliseconds)
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해 주세요.");
      session.setAttribute("rate:" + action, now);
    }
  }
}
