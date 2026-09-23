package com.joji.community.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** 채팅방과 WebSocket 메시지에서 사용하는 DTO 모음입니다. */
public final class ChatModels {
  private ChatModels() {}

  public record Room(
      long id, String name, String category, String description, boolean locked, int online) {}

  public record ChatMessage(
      long id,
      long roomId,
      String nickname,
      String authorTag,
      String content,
      Instant createdAt) {}

  public record RoomInput(
      @NotBlank @Size(max = 60) String name,
      @NotBlank @Pattern(regexp = "자유|개발 / IT|취업 / 커리어|게임") String category,
      @Size(max = 200) @NotNull String description,
      @Size(max = 60) String password) {}

  public record PasswordInput(@Size(max = 60) String password) {}

  public record MessageInput(
      @NotBlank @Size(max = 20) String nickname,
      @NotBlank @Size(max = 500) String content) {}
}
