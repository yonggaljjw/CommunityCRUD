package com.joji.community.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 쓰기 API가 허용된 화면에서 보낸 요청인지 기본적으로 확인하는 HTTP 필터입니다. */
@Component
public class RequestGuard extends OncePerRequestFilter {
  private final Set<String> origins;

  public RequestGuard(@Value("${app.allowed-origins}") String origins) {
    // 환경 변수에 공백이 포함되어도 비교가 실패하지 않도록 trim 합니다.
    this.origins = Arrays.stream(origins.split(",")).map(String::trim).collect(Collectors.toSet());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    boolean isWriteApi =
        req.getRequestURI().startsWith("/api/")
            && !Set.of("GET", "HEAD", "OPTIONS").contains(req.getMethod());

    if (isWriteApi) {
      String origin = req.getHeader("Origin");
      boolean requestedByApp = "JOJI".equals(req.getHeader("X-Requested-With"));
      boolean allowedOrigin = origin == null || origins.contains(origin);

      if (!requestedByApp || !allowedOrigin) {
        res.setStatus(403);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"message\":\"허용되지 않은 요청입니다.\"}");
        return;
      }
    }

    chain.doFilter(req, res);
  }
}
