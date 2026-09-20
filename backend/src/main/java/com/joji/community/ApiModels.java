package com.joji.community;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

// API 전용 자료형(DTO). DB의 password_hash가 응답에 섞이지 않도록 응답 필드를 명시합니다.
public final class ApiModels {
  private ApiModels() {}

  public record Gallery(long id, String slug, String name, String description, long postCount) {}

  public record Post(
      long id,
      long galleryId,
      String galleryName,
      String category,
      String title,
      String content,
      String nickname,
      String authorTag,
      long views,
      long likes,
      long commentCount,
      Instant createdAt,
      boolean liked) {}

  public record Comment(
      long id,
      long postId,
      String nickname,
      String authorTag,
      String content,
      Instant createdAt) {}

  public record Room(
      long id,
      String name,
      String category,
      String description,
      boolean locked,
      int online) {}

  public record ChatMessage(
      long id,
      long roomId,
      String nickname,
      String authorTag,
      String content,
      Instant createdAt) {}

  public record Page<T>(List<T> items, long total, int page, int size) {}

  public record PostInput(
      @Min(1) long galleryId,
      @NotBlank @Pattern(regexp = "일반|질문|정보|유머|후기|잡담") String category,
      @NotBlank @Size(max = 150) String title,
      @NotBlank @Size(max = 20000) String content,
      @NotBlank @Size(max = 20) String nickname,
      @NotBlank @Size(min = 4, max = 60) String password) {}

  public record CommentInput(
      @NotBlank @Size(max = 20) String nickname,
      @NotBlank @Size(max = 1000) String content,
      @NotBlank @Size(min = 4, max = 60) String password) {}

  public record RoomInput(
      @NotBlank @Size(max = 60) String name,
      @NotBlank @Pattern(regexp = "자유|개발 / IT|취업 / 커리어|게임") String category,
      @Size(max = 200) @NotNull String description,
      @Size(max = 60) String password) {}

  public record PasswordInput(@Size(max = 60) String password) {}

  public record MessageInput(
      @NotBlank @Size(max = 20) String nickname, @NotBlank @Size(max = 500) String content) {}
}
