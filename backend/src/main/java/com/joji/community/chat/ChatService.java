package com.joji.community.chat;

import static com.joji.community.chat.ChatModels.*;

import com.joji.community.common.PasswordManager;
import com.joji.community.common.AnonymousSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 채팅방 생성·입장 권한을 관리합니다. WebSocket 연결 전에도 이 규칙을 그대로 사용합니다. */
@Service
public class ChatService {
  private final ChatRepository repository;
  private final PasswordManager passwords;

  public ChatService(ChatRepository repository, PasswordManager passwords) {
    this.repository = repository;
    this.passwords = passwords;
  }

  @Transactional
  public Room createRoom(RoomInput input, HttpSession session) {
    AnonymousSession.limit(session, "room", 10000);
    String password = input.password();

    if (password != null && !password.isBlank() && password.length() < 4) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "방 비밀번호는 4자 이상이어야 합니다.");
    }

    String hash = password == null || password.isBlank() ? null : passwords.encode(password);
    Room room = repository.createRoom(input, hash);
    // 방을 만든 세션은 추가 비밀번호 입력 없이 바로 입장할 수 있습니다.
    session.setAttribute("room:" + room.id(), true);
    return room;
  }

  public void join(long id, String password, HttpSession session) {
    AnonymousSession.actor(session);

    if (Boolean.TRUE.equals(session.getAttribute("room:" + id))) {
      repository.room(id);
      return;
    }

    String hash = repository.roomPassword(id);
    if (hash != null) {
      AnonymousSession.limit(session, "roomPassword", 1000);
      passwords.verify(password, hash);
    }
    session.setAttribute("room:" + id, true);
  }

  /** 비밀방은 HTTP에서 입장 인증을 마친 세션만 메시지/소켓에 접근할 수 있습니다. */
  public void requireRoom(long id, HttpSession session) {
    String hash = repository.roomPassword(id);
    if (hash != null && !Boolean.TRUE.equals(session.getAttribute("room:" + id))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "비밀번호를 입력하고 입장해 주세요.");
    }
  }
}
