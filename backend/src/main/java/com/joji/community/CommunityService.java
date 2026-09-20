package com.joji.community;

import static com.joji.community.ApiModels.*;

import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommunityService {
  private final CommunityRepository repo;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public CommunityService(CommunityRepository repo) {
    this.repo = repo;
  }

  private String encode(String password) {
    // BCrypt의 입력 한계는 글자 수가 아니라 UTF-8 72바이트입니다.
    if (password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.");
    return encoder.encode(password);
  }

  private void verify(String raw, String hash) {
    if (raw == null
        || raw.getBytes(StandardCharsets.UTF_8).length > 72
        || !encoder.matches(raw, hash))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "비밀번호가 올바르지 않습니다.");
  }

  @Transactional
  public Post create(PostInput input, HttpSession session) {
    AnonymousSession.limit(session, "post", 3000);
    long id = repo.createPost(input, AnonymousSession.tag(session), encode(input.password()));
    return repo.post(id, AnonymousSession.actor(session));
  }

  @Transactional
  public Post edit(long id, PostInput input, HttpSession session) {
    AnonymousSession.limit(session, "password", 1000);
    verify(input.password(), repo.postPassword(id));
    repo.updatePost(id, input);
    return repo.post(id, AnonymousSession.actor(session));
  }

  @Transactional
  public void delete(long id, String password, HttpSession session) {
    AnonymousSession.limit(session, "password", 1000);
    verify(password, repo.postPassword(id));
    repo.deletePost(id);
  }

  @Transactional
  public Post like(long id, HttpSession session) {
    AnonymousSession.limit(session, "like", 500);
    repo.like(id, AnonymousSession.actor(session));
    return repo.post(id, AnonymousSession.actor(session));
  }

  @Transactional
  public Comment comment(long id, CommentInput input, HttpSession session) {
    AnonymousSession.limit(session, "comment", 3000);
    repo.post(id, AnonymousSession.actor(session));
    return repo.createComment(id, input, AnonymousSession.tag(session), encode(input.password()));
  }

  @Transactional
  public void deleteComment(long postId, long id, String password, HttpSession session) {
    AnonymousSession.limit(session, "password", 1000);
    verify(password, repo.commentPassword(id, postId));
    repo.deleteComment(postId, id);
  }

  @Transactional
  public Room createRoom(RoomInput input, HttpSession session) {
    AnonymousSession.limit(session, "room", 10000);
    String password = input.password();
    if (password != null && !password.isBlank() && password.length() < 4)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "방 비밀번호는 4자 이상이어야 합니다.");
    Room room =
        repo.createRoom(input, password == null || password.isBlank() ? null : encode(password));
    session.setAttribute("room:" + room.id(), true);
    return room;
  }

  public void join(long id, String password, HttpSession session) {
    AnonymousSession.actor(session);
    if (Boolean.TRUE.equals(session.getAttribute("room:" + id))) {
      repo.room(id);
      return;
    }
    String hash = repo.roomPassword(id);
    if (hash != null) {
      AnonymousSession.limit(session, "roomPassword", 1000);
      verify(password, hash);
    }
    session.setAttribute("room:" + id, true);
  }

  public void requireRoom(long id, HttpSession session) {
    String hash = repo.roomPassword(id);
    if (hash != null && !Boolean.TRUE.equals(session.getAttribute("room:" + id)))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "비밀번호를 입력하고 입장해 주세요.");
  }
}
