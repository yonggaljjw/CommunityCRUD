package com.joji.community.board;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** 게시판 기능에서 HTTP 요청·응답에 사용하는 DTO 모음입니다. */
public final class BoardModels {
  private BoardModels() {}

  // record를 사용하면 단순 데이터 전달 객체의 getter/생성자 코드를 줄일 수 있습니다.
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

  public record Page<T>(List<T> items, long total, int page, int size) {}

  /** 게시글 작성·수정 요청. Bean Validation으로 길이와 허용 카테고리를 먼저 검사합니다. */
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

  public record PasswordInput(@Size(max = 60) String password) {}
}
