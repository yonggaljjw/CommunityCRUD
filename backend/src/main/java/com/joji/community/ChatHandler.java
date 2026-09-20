package com.joji.community;

import static com.joji.community.ApiModels.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Validator;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatHandler extends TextWebSocketHandler {
  private final CommunityRepository repo;
  private final ObjectMapper json;
  private final Validator validator;
  // 현재 구현은 Spring Boot 1개 인스턴스 기준입니다. 서버 확장 시 Redis Pub/Sub 등으로 교체합니다.
  private final Map<Long, Map<String, WebSocketSession>> rooms = new ConcurrentHashMap<>();

  public ChatHandler(CommunityRepository repo, ObjectMapper json, Validator validator) {
    this.repo = repo;
    this.json = json;
    this.validator = validator;
  }

  public int online(long id) {
    // 같은 브라우저의 여러 탭은 한 명으로 셉니다. 값은 실제 열린 소켓의 세션 기준입니다.
    return (int)
        rooms.getOrDefault(id, Map.of()).values().stream()
            .filter(WebSocketSession::isOpen)
            .map(s -> s.getAttributes().get("session"))
            .distinct()
            .count();
  }

  private long room(WebSocketSession s) {
    return (Long) s.getAttributes().get("roomId");
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession s) throws Exception {
    s.setTextMessageSizeLimit(8192);
    rooms.computeIfAbsent(room(s), k -> new ConcurrentHashMap<>()).put(s.getId(), s);
    // 등록 뒤 이력을 보냅니다. 중간에 새 메시지가 섞여도 프런트가 message.id로 중복 제거합니다.
    send(s, Map.of("type", "history", "messages", repo.messages(room(s), Long.MAX_VALUE)));
    presence(room(s));
  }

  @Override
  protected void handleTextMessage(WebSocketSession s, TextMessage message) throws Exception {
    try {
      var tree = json.readTree(message.getPayload());
      HttpSession session = (HttpSession) s.getAttributes().get("session");
      // 세션 만료 여부를 주기적으로 확인하고 프록시 유휴 연결도 유지합니다.
      session.getCreationTime();
      if ("ping".equals(tree.path("type").asText())) {
        send(s, Map.of("type", "pong"));
        return;
      }
      if (!"message".equals(tree.path("type").asText())) throw new IllegalArgumentException();
      MessageInput input =
          new MessageInput(tree.path("nickname").asText(), tree.path("content").asText());
      String clientId = tree.path("clientId").asText();
      if (clientId.length() > 64) throw new IllegalArgumentException();
      if (!validator.validate(input).isEmpty()) {
        send(s, Map.of("type", "error", "message", "닉네임은 20자, 메시지는 1~500자로 입력해 주세요."));
        return;
      }
      AnonymousSession.limit(session, "chat", 3000);
      ChatMessage saved = repo.message(room(s), input, AnonymousSession.tag(session));
      // 저장에 성공한 메시지만 같은 방에 방송합니다. 클라이언트가 보낸 roomId는 사용하지 않습니다.
      broadcast(room(s), Map.of("type", "message", "message", saved));
      send(s, Map.of("type", "ack", "clientId", clientId, "messageId", saved.id()));
    } catch (ResponseStatusException e) {
      send(s, Map.of("type", "error", "message", Objects.requireNonNull(e.getReason())));
    } catch (IllegalStateException e) {
      s.close(CloseStatus.POLICY_VIOLATION);
    } catch (Exception e) {
      send(s, Map.of("type", "error", "message", "메시지를 처리하지 못했습니다. 입력값과 연결을 확인해 주세요."));
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession s, CloseStatus status) {
    remove(s);
  }

  @Override
  public void handleTransportError(WebSocketSession s, Throwable error) throws Exception {
    remove(s);
    if (s.isOpen()) s.close(CloseStatus.SERVER_ERROR);
  }

  private void remove(WebSocketSession s) {
    Map<String, WebSocketSession> members = rooms.get(room(s));
    if (members != null) members.remove(s.getId());
    presence(room(s));
  }

  private void presence(long id) {
    broadcast(id, Map.of("type", "presence", "online", online(id)));
  }

  private void broadcast(long id, Object payload) {
    rooms
        .getOrDefault(id, Map.of())
        .values()
        .forEach(
            s -> {
              try {
                send(s, payload);
              } catch (Exception e) {
                try {
                  s.close();
                } catch (Exception ignored) {
                }
              }
            });
  }

  private void send(WebSocketSession s, Object payload) throws Exception {
    // 표준 WebSocketSession은 동시 send를 지원하지 않으므로 소켓별로 쓰기를 직렬화합니다.
    synchronized (s) {
      if (s.isOpen()) s.sendMessage(new TextMessage(json.writeValueAsString(payload)));
    }
  }
}
