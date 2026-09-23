package com.joji.community.config;

import com.joji.community.chat.ChatService;
import com.joji.community.chat.ChatHandler;
import java.util.Arrays;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

/** /ws/chat 연결 경로와 WebSocket handshake 단계의 권한 확인을 설정합니다. */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
  private final ChatHandler handler;
  private final ChatService service;
  private final String[] origins;

  public WebSocketConfig(
      ChatHandler handler,
      ChatService service,
      @Value("${app.allowed-origins}") String origins) {
    this.handler = handler;
    this.service = service;
    this.origins = Arrays.stream(origins.split(",")).map(String::trim).toArray(String[]::new);
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry
        .addHandler(handler, "/ws/chat")
        .setAllowedOrigins(origins)
        .addInterceptors(new RoomHandshakeInterceptor(service));
  }

  /** HTTP 세션과 roomId를 WebSocket 세션 속성으로 넘겨 주는 handshake 전용 인터셉터입니다. */
  private static class RoomHandshakeInterceptor implements HandshakeInterceptor {
    private final ChatService service;

    RoomHandshakeInterceptor(ChatService service) {
      this.service = service;
    }

    @Override
    public boolean beforeHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler handler,
        Map<String, Object> attributes) {
      try {
        var servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        var session = servletRequest.getSession(false);
        if (session == null || session.getAttribute("actor") == null) {
          response.setStatusCode(HttpStatus.UNAUTHORIZED);
          return false;
        }

        String roomParam =
            UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("roomId");
        long roomId = Long.parseLong(roomParam);

        // 클라이언트의 roomId만 믿지 않고 서버의 비밀방 입장 상태를 다시 검사합니다.
        service.requireRoom(roomId, session);
        attributes.put("roomId", roomId);
        attributes.put("session", session);
        return true;
      } catch (Exception e) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return false;
      }
    }

    @Override
    public void afterHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler handler,
        Exception exception) {
      // handshake 이후에 별도로 처리할 작업은 없습니다.
    }
  }
}
