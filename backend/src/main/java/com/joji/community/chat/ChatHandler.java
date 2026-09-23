package com.joji.community.chat;

import static com.joji.community.chat.ChatModels.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joji.community.common.AnonymousSession;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Validator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** WebSocket으로 들어온 채팅을 DB에 저장하고 같은 방의 접속자에게 방송합니다. */
@Component
public class ChatHandler extends TextWebSocketHandler {
  private final ChatRepository repository;
  private final ObjectMapper json;
  private final Validator validator;

  // 단일 Spring Boot 인스턴스용 메모리 접속자 목록입니다. 다중 서버에서는 Redis Pub/Sub 등이 필요합니다.
  private final Map<Long, Map<String, WebSocketSession>> rooms = new ConcurrentHashMap<>();

  public ChatHandler(ChatRepository repository, ObjectMapper json, Validator validator) {
    this.repository = repository;
    this.json = json;
    this.validator = validator;
  }

  /** 같은 브라우저 탭 여러 개는 HttpSession이 같으므로 한 명으로 계산합니다. */
  public int online(long roomId) {
    return (int)
        rooms.getOrDefault(roomId, Map.of()).values().stream()
            .filter(WebSocketSession::isOpen)
            .map(session -> session.getAttributes().get("session"))
            .distinct()
            .count();
  }

  private long roomId(WebSocketSession session) {
    return (Long) session.getAttributes().get("roomId");
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    session.setTextMessageSizeLimit(8192);
    rooms.computeIfAbsent(roomId(session), key -> new ConcurrentHashMap<>())
        .put(session.getId(), session);

    // 소켓 등록 후 최근 이력을 보냅니다. 프런트는 message.id 기준으로 중복을 제거합니다.
    send(
        session,
        Map.of(
            "type",
            "history",
            "messages",
            repository.messages(roomId(session), Long.MAX_VALUE)));
    broadcastPresence(roomId(session));
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    try {
      var tree = json.readTree(message.getPayload());
      HttpSession httpSession = (HttpSession) session.getAttributes().get("session");

      // 세션이 만료되었으면 getCreationTime()에서 예외가 발생하여 연결을 정리합니다.
      httpSession.getCreationTime();
      if ("ping".equals(tree.path("type").asText())) {
        send(session, Map.of("type", "pong"));
        return;
      }
      if (!"message".equals(tree.path("type").asText())) {
        throw new IllegalArgumentException();
      }

      MessageInput input =
          new MessageInput(tree.path("nickname").asText(), tree.path("content").asText());
      String clientId = tree.path("clientId").asText();
      if (clientId.length() > 64) {
        throw new IllegalArgumentException();
      }
      if (!validator.validate(input).isEmpty()) {
        send(session, Map.of("type", "error", "message", "닉네임은 20자, 메시지는 1~500자로 입력해 주세요."));
        return;
      }

      AnonymousSession.limit(httpSession, "chat", 3000);
      ChatMessage saved =
          repository.saveMessage(roomId(session), input, AnonymousSession.tag(httpSession));

      // 저장에 성공한 메시지만 방송하여 새로고침 후 DB 이력과 화면이 일치하게 합니다.
      broadcast(roomId(session), Map.of("type", "message", "message", saved));
      send(session, Map.of("type", "ack", "clientId", clientId, "messageId", saved.id()));
    } catch (ResponseStatusException e) {
      send(session, Map.of("type", "error", "message", Objects.requireNonNull(e.getReason())));
    } catch (IllegalStateException e) {
      session.close(CloseStatus.POLICY_VIOLATION);
    } catch (Exception e) {
      send(
          session,
          Map.of(
              "type",
              "error",
              "message",
              "메시지를 처리하지 못했습니다. 입력값과 연결을 확인해 주세요."));
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    remove(session);
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable error) throws Exception {
    remove(session);
    if (session.isOpen()) {
      session.close(CloseStatus.SERVER_ERROR);
    }
  }

  private void remove(WebSocketSession session) {
    Map<String, WebSocketSession> members = rooms.get(roomId(session));
    if (members != null) {
      members.remove(session.getId());
      if (members.isEmpty()) {
        rooms.remove(roomId(session), members);
      }
    }
    broadcastPresence(roomId(session));
  }

  private void broadcastPresence(long id) {
    broadcast(id, Map.of("type", "presence", "online", online(id)));
  }

  private void broadcast(long id, Object payload) {
    rooms.getOrDefault(id, Map.of()).values().forEach(session -> safeSend(session, payload));
  }

  private void safeSend(WebSocketSession session, Object payload) {
    try {
      send(session, payload);
    } catch (Exception e) {
      try {
        session.close();
      } catch (Exception ignored) {
        // 이미 끊긴 소켓을 다시 닫는 중 발생한 예외는 무시합니다.
      }
    }
  }

  private void send(WebSocketSession session, Object payload) throws Exception {
    // 같은 소켓에 여러 스레드가 동시에 sendMessage 하지 않도록 세션을 잠급니다.
    synchronized (session) {
      if (session.isOpen()) {
        session.sendMessage(new TextMessage(json.writeValueAsString(payload)));
      }
    }
  }
}
