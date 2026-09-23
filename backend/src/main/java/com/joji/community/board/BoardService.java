package com.joji.community.board;

import static com.joji.community.board.BoardModels.*;

import com.joji.community.common.PasswordManager;
import com.joji.community.common.AnonymousSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 게시글·댓글의 쓰기 규칙과 트랜잭션 경계를 담당합니다. */
@Service
public class BoardService {
  private final BoardRepository repository;
  private final PasswordManager passwords;

  public BoardService(BoardRepository repository, PasswordManager passwords) {
    this.repository = repository;
    this.passwords = passwords;
  }

  @Transactional
  public Post create(PostInput input, HttpSession session) {
    AnonymousSession.limit(session, "post", 3000);
    long id =
        repository.createPost(
            input, AnonymousSession.tag(session), passwords.encode(input.password()));
    return repository.post(id, AnonymousSession.actor(session));
  }

  @Transactional
  public Post edit(long id, PostInput input, HttpSession session) {
    AnonymousSession.limit(session, "password", 1000);
    passwords.verify(input.password(), repository.postPassword(id));
    repository.updatePost(id, input);
    return repository.post(id, AnonymousSession.actor(session));
  }

  @Transactional
  public void delete(long id, String password, HttpSession session) {
    AnonymousSession.limit(session, "password", 1000);
    passwords.verify(password, repository.postPassword(id));
    repository.deletePost(id);
  }

  @Transactional
  public Post like(long id, HttpSession session) {
    AnonymousSession.limit(session, "like", 500);
    String actor = AnonymousSession.actor(session);
    repository.toggleLike(id, actor);
    return repository.post(id, actor);
  }

  @Transactional
  public Comment comment(long id, CommentInput input, HttpSession session) {
    AnonymousSession.limit(session, "comment", 3000);
    // 없는 게시글에 댓글을 저장하지 않도록 부모 글 존재 여부를 먼저 확인합니다.
    repository.post(id, AnonymousSession.actor(session));
    return repository.createComment(
        id, input, AnonymousSession.tag(session), passwords.encode(input.password()));
  }

  @Transactional
  public void deleteComment(long postId, long id, String password, HttpSession session) {
    AnonymousSession.limit(session, "password", 1000);
    passwords.verify(password, repository.commentPassword(id, postId));
    repository.deleteComment(postId, id);
  }
}
