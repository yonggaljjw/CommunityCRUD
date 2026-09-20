package com.joji.community;

import static com.joji.community.ApiModels.*;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class CommunityController {
  private final CommunityRepository repo;
  private final CommunityService service;
  private final ChatHandler chat;

  public CommunityController(CommunityRepository repo, CommunityService service, ChatHandler chat) {
    this.repo = repo;
    this.service = service;
    this.chat = chat;
  }

  @GetMapping("/session")
  public Map<String, String> session(HttpSession s) {
    return Map.of("tag", AnonymousSession.tag(s));
  }

  @GetMapping("/galleries")
  public List<Gallery> galleries() {
    return repo.galleries();
  }

  @GetMapping("/posts")
  public Page<Post> posts(
      @RequestParam(required = false) Long galleryId,
      @RequestParam(defaultValue = "") String category,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "new") String sort,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "15") int size) {
    if (page < 1
        || page > 100000
        || size < 1
        || size > 50
        || q.length() > 100
        || !Set.of("new", "hot", "best").contains(sort))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검색 조건을 확인해 주세요.");
    return repo.posts(galleryId, category, q, sort, page, size);
  }

  @GetMapping("/posts/{id}")
  public Post post(@PathVariable long id, HttpSession s) {
    repo.view(id);
    return repo.post(id, AnonymousSession.actor(s));
  }

  @PostMapping("/posts")
  @ResponseStatus(HttpStatus.CREATED)
  public Post create(@Valid @RequestBody PostInput p, HttpSession s) {
    return service.create(p, s);
  }

  @PutMapping("/posts/{id}")
  public Post edit(@PathVariable long id, @Valid @RequestBody PostInput p, HttpSession s) {
    return service.edit(id, p, s);
  }

  @DeleteMapping("/posts/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long id, @Valid @RequestBody PasswordInput p, HttpSession s) {
    service.delete(id, p.password(), s);
  }

  @PostMapping("/posts/{id}/like")
  public Post like(@PathVariable long id, HttpSession s) {
    return service.like(id, s);
  }

  @GetMapping("/posts/{id}/comments")
  public List<Comment> comments(
      @PathVariable long id, @RequestParam(defaultValue = "0") long after) {
    repo.post(id, "");
    return repo.comments(id, after);
  }

  @PostMapping("/posts/{id}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  public Comment comment(@PathVariable long id, @Valid @RequestBody CommentInput c, HttpSession s) {
    return service.comment(id, c, s);
  }

  @DeleteMapping("/posts/{postId}/comments/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteComment(
      @PathVariable long postId,
      @PathVariable long id,
      @Valid @RequestBody PasswordInput p,
      HttpSession s) {
    service.deleteComment(postId, id, p.password(), s);
  }

  @GetMapping("/rooms")
  public List<Room> rooms() {
    return repo.rooms().stream()
        .map(
            r ->
                new Room(
                    r.id(),
                    r.name(),
                    r.category(),
                    r.description(),
                    r.locked(),
                    chat.online(r.id())))
        .toList();
  }

  @PostMapping("/rooms")
  @ResponseStatus(HttpStatus.CREATED)
  public Room room(@Valid @RequestBody RoomInput r, HttpSession s) {
    return service.createRoom(r, s);
  }

  @PostMapping("/rooms/{id}/join")
  public Map<String, Boolean> join(
      @PathVariable long id, @Valid @RequestBody PasswordInput p, HttpSession s) {
    service.join(id, p.password(), s);
    return Map.of("joined", true);
  }

  @GetMapping("/rooms/{id}/messages")
  public List<ChatMessage> messages(
      @PathVariable long id,
      @RequestParam(defaultValue = "9223372036854775807") long before,
      HttpSession s) {
    service.requireRoom(id, s);
    return repo.messages(id, before);
  }
}
