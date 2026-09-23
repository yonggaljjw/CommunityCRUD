package com.joji.community.board;

import static com.joji.community.board.BoardModels.*;

import com.joji.community.common.AnonymousSession;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** 게시판 HTTP API의 진입점입니다. 요청 검증 후 Repository/Service에 역할을 위임합니다. */
@RestController
@RequestMapping("/api")
public class BoardController {
  private final BoardRepository repository;
  private final BoardService service;

  public BoardController(BoardRepository repository, BoardService service) {
    this.repository = repository;
    this.service = service;
  }

  @GetMapping("/galleries")
  public List<Gallery> galleries() {
    return repository.galleries();
  }

  @GetMapping("/posts")
  public Page<Post> posts(
      @RequestParam(required = false) Long galleryId,
      @RequestParam(defaultValue = "") String category,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "new") String sort,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "15") int size) {
    // 목록 API는 직접 들어오는 쿼리 파라미터 범위를 Controller에서 제한합니다.
    if (page < 1
        || page > 100000
        || size < 1
        || size > 50
        || q.length() > 100
        || !Set.of("new", "hot", "best").contains(sort)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검색 조건을 확인해 주세요.");
    }
    return repository.posts(galleryId, category, q, sort, page, size);
  }

  @GetMapping("/posts/{id}")
  public Post post(@PathVariable long id, HttpSession session) {
    repository.increaseView(id);
    return repository.post(id, AnonymousSession.actor(session));
  }

  @PostMapping("/posts")
  @ResponseStatus(HttpStatus.CREATED)
  public Post create(@Valid @RequestBody PostInput post, HttpSession session) {
    return service.create(post, session);
  }

  @PutMapping("/posts/{id}")
  public Post edit(@PathVariable long id, @Valid @RequestBody PostInput post, HttpSession session) {
    return service.edit(id, post, session);
  }

  @DeleteMapping("/posts/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @PathVariable long id, @Valid @RequestBody PasswordInput password, HttpSession session) {
    service.delete(id, password.password(), session);
  }

  @PostMapping("/posts/{id}/like")
  public Post like(@PathVariable long id, HttpSession session) {
    return service.like(id, session);
  }

  @GetMapping("/posts/{id}/comments")
  public List<Comment> comments(
      @PathVariable long id, @RequestParam(defaultValue = "0") long after) {
    repository.post(id, "");
    return repository.comments(id, after);
  }

  @PostMapping("/posts/{id}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  public Comment comment(
      @PathVariable long id, @Valid @RequestBody CommentInput comment, HttpSession session) {
    return service.comment(id, comment, session);
  }

  @DeleteMapping("/posts/{postId}/comments/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteComment(
      @PathVariable long postId,
      @PathVariable long id,
      @Valid @RequestBody PasswordInput password,
      HttpSession session) {
    service.deleteComment(postId, id, password.password(), session);
  }
}
