package com.joji.community.common;

import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 로그인 기능이 없는 익명 커뮤니티에서 브라우저 세션을 사용자처럼 식별하는 도우미입니다.
 *
 * <p>actor는 내부 중복 방지용 UUID이고, tag는 화면에 보여 주는 짧은 식별자입니다.
 */
public final class AnonymousSession {
  private AnonymousSession() {}

  /** 세션에 익명 사용자 UUID가 없으면 생성하고, 항상 같은 값을 반환합니다. */
  public static String actor(HttpSession session) {
    // 같은 세션에 동시에 요청이 들어와 UUID가 두 번 생성되지 않도록 세션 자체를 잠급니다.
    synchronized (session) {
      if (session.getAttribute("actor") == null) {
        session.setAttribute("actor", UUID.randomUUID().toString());
      }
      return (String) session.getAttribute("actor");
    }
  }

  /** 화면에는 UUID 전체 대신 앞 8자리만 노출합니다. */
  public static String tag(HttpSession session) {
    return actor(session).substring(0, 8);
  }

  /**
   * 같은 세션에서 특정 행동을 너무 빠르게 반복하지 못하게 하는 간단한 rate limit입니다.
   * 운영 규모가 커지면 Redis 같은 공유 저장소 기반 제한으로 교체할 수 있습니다.
   */
  public static void limit(HttpSession session, String action, long milliseconds) {
    synchronized (session) {
      long now = System.currentTimeMillis();
      Long last = (Long) session.getAttribute("rate:" + action);
      if (last != null && now - last < milliseconds) {
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해 주세요.");
      }
      session.setAttribute("rate:" + action, now);
    }
  }
}
