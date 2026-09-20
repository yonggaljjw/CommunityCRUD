package com.joji.community;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.*;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
  private final ChatHandler handler;
  private final CommunityService service;
  private final String[] origins;

  public WebSocketConfig(
      ChatHandler handler,
      CommunityService service,
      @Value("${app.allowed-origins}") String origins) {
    this.handler = handler;
    this.service = service;
    this.origins = origins.split(",");
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry
        .addHandler(handler, "/ws/chat")
        .setAllowedOrigins(origins)
        .addInterceptors(
            new HandshakeInterceptor() {
              @Override
              public boolean beforeHandshake(
                  ServerHttpRequest request,
                  ServerHttpResponse response,
                  WebSocketHandler h,
                  Map<String, Object> attrs) {
                try {
                  var session =
                      ((ServletServerHttpRequest) request).getServletRequest().getSession(false);
                  if (session == null || session.getAttribute("actor") == null) {
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return false;
                  }
                  long roomId =
                      Long.parseLong(
                          UriComponentsBuilder.fromUri(request.getURI())
                              .build()
                              .getQueryParams()
                              .getFirst("roomId"));
                  // 클라이언트의 방 ID를 신뢰하지 않습니다. HTTP에서 승인된 비밀방만 소켓 연결을 허용합니다.
                  service.requireRoom(roomId, session);
                  attrs.put("roomId", roomId);
                  attrs.put("session", session);
                  return true;
                } catch (Exception e) {
                  response.setStatusCode(HttpStatus.FORBIDDEN);
                  return false;
                }
              }

              @Override
              public void afterHandshake(
                  ServerHttpRequest r, ServerHttpResponse s, WebSocketHandler h, Exception e) {}
            });
  }
}
