package com.joji.community.chat;

import static com.joji.community.chat.ChatModels.*;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 채팅방 목록·생성·입장·과거 메시지 조회용 HTTP API입니다. */
@RestController
@RequestMapping("/api")
public class ChatController {
  private final ChatRepository repository;
  private final ChatService service;
  private final ChatHandler chatHandler;

  public ChatController(ChatRepository repository, ChatService service, ChatHandler chatHandler) {
    this.repository = repository;
    this.service = service;
    this.chatHandler = chatHandler;
  }

  @GetMapping("/rooms")
  public List<Room> rooms() {
    // DB의 방 정보에 현재 WebSocket 접속자 수를 합쳐 응답합니다.
    return repository.rooms().stream()
        .map(
            room ->
                new Room(
                    room.id(),
                    room.name(),
                    room.category(),
                    room.description(),
                    room.locked(),
                    chatHandler.online(room.id())))
        .toList();
  }

  @PostMapping("/rooms")
  @ResponseStatus(HttpStatus.CREATED)
  public Room room(@Valid @RequestBody RoomInput room, HttpSession session) {
    return service.createRoom(room, session);
  }

  @PostMapping("/rooms/{id}/join")
  public Map<String, Boolean> join(
      @PathVariable long id, @Valid @RequestBody PasswordInput password, HttpSession session) {
    service.join(id, password.password(), session);
    return Map.of("joined", true);
  }

  @GetMapping("/rooms/{id}/messages")
  public List<ChatMessage> messages(
      @PathVariable long id,
      @RequestParam(defaultValue = "9223372036854775807") long before,
      HttpSession session) {
    service.requireRoom(id, session);
    return repository.messages(id, before);
  }
}
