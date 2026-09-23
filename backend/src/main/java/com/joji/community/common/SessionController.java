package com.joji.community.common;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 프런트가 현재 익명 식별 태그를 확인할 때 사용하는 작은 공통 API입니다. */
@RestController
@RequestMapping("/api")
public class SessionController {
  @GetMapping("/session")
  public Map<String, String> session(HttpSession session) {
    return Map.of("tag", AnonymousSession.tag(session));
  }
}
