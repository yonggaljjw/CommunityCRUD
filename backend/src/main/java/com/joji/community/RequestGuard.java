package com.joji.community;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestGuard extends OncePerRequestFilter {
  private final Set<String> origins;

  public RequestGuard(@Value("${app.allowed-origins}") String origins) {
    this.origins = Set.of(origins.split(","));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    // 쓰기 API는 동일 출처의 JS 요청만 허용합니다. CORS를 열지 않고 사용자 지정 헤더도 확인합니다.
    if (req.getRequestURI().startsWith("/api/")
        && !Set.of("GET", "HEAD", "OPTIONS").contains(req.getMethod())) {
      String origin = req.getHeader("Origin");
      if (!"JOJI".equals(req.getHeader("X-Requested-With"))
          || (origin != null && !origins.contains(origin))) {
        res.setStatus(403);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"message\":\"허용되지 않은 요청입니다.\"}");
        return;
      }
    }
    chain.doFilter(req, res);
  }
}
