package com.joji.community;

import static com.joji.community.ApiModels.*;

import java.sql.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.*;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class CommunityRepository {
  private final JdbcTemplate db;

  public CommunityRepository(JdbcTemplate db) {
    this.db = db;
  }

  // 모든 사용자 입력은 ? 바인딩으로 전달합니다. 문자열 SQL 연결은 고정된 조건절에만 사용합니다.
  private static final String POST_SELECT =
      """
      SELECT p.*, g.name AS gallery_name,
      (SELECT COUNT(*) FROM comments c WHERE c.post_id=p.id) AS comment_count,
      (SELECT COUNT(*) FROM post_likes l WHERE l.post_id=p.id) AS likes
      FROM posts p JOIN galleries g ON g.id=p.gallery_id
      """;
  private final RowMapper<Post> postMapper =
      (r, n) ->
          new Post(
              r.getLong("id"),
              r.getLong("gallery_id"),
              r.getString("gallery_name"),
              r.getString("category"),
              r.getString("title"),
              r.getString("content"),
              r.getString("nickname"),
              r.getString("author_tag"),
              r.getLong("views"),
              r.getLong("likes"),
              r.getLong("comment_count"),
              r.getTimestamp("created_at").toInstant(),
              false);
  private final RowMapper<Comment> commentMapper =
      (r, n) ->
          new Comment(
              r.getLong("id"),
              r.getLong("post_id"),
              r.getString("nickname"),
              r.getString("author_tag"),
              r.getString("content"),
              r.getTimestamp("created_at").toInstant());
  private final RowMapper<ChatMessage> messageMapper =
      (r, n) ->
          new ChatMessage(
              r.getLong("id"),
              r.getLong("room_id"),
              r.getString("nickname"),
              r.getString("author_tag"),
              r.getString("content"),
              r.getTimestamp("created_at").toInstant());

  public List<Gallery> galleries() {
    return db.query(
        "SELECT g.*, (SELECT COUNT(*) FROM posts p WHERE p.gallery_id=g.id) AS cnt FROM galleries g"
            + " ORDER BY g.id",
        (r, n) ->
            new Gallery(
                r.getLong("id"),
                r.getString("slug"),
                r.getString("name"),
                r.getString("description"),
                r.getLong("cnt")));
  }

  public Page<Post> posts(
      Long galleryId, String category, String query, String sort, int page, int size) {
    List<Object> params = new ArrayList<>();
    String where = " WHERE 1=1";
    if (galleryId != null) {
      where += " AND p.gallery_id=?";
      params.add(galleryId);
    }
    if (!category.isBlank()) {
      where += " AND p.category=?";
      params.add(category);
    }
    if (!query.isBlank()) {
      // LOCATE는 %와 _를 와일드카드로 취급하지 않아 사용자가 입력한 문자열 그대로 검색됩니다.
      where += " AND (LOCATE(?,p.title)>0 OR LOCATE(?,p.content)>0)";
      params.add(query);
      params.add(query);
    }
    if (sort.equals("best"))
      where += " AND (SELECT COUNT(*) FROM post_likes l WHERE l.post_id=p.id)>=30";
    long total =
        db.queryForObject("SELECT COUNT(*) FROM posts p" + where, Long.class, params.toArray());
    String order =
        sort.equals("hot") || sort.equals("best")
            ? " ORDER BY likes DESC,p.id DESC"
            : " ORDER BY p.id DESC";
    params.add(size);
    params.add((page - 1) * size);
    return new Page<>(
        db.query(POST_SELECT + where + order + " LIMIT ? OFFSET ?", postMapper, params.toArray()),
        total,
        page,
        size);
  }

  public Post post(long id, String actor) {
    Post p =
        db.query(POST_SELECT + " WHERE p.id=?", postMapper, id).stream()
            .findFirst()
            .orElseThrow(CommunityRepository::notFound);
    boolean liked =
        db.queryForObject(
                "SELECT COUNT(*) FROM post_likes WHERE post_id=? AND actor_id=?",
                Long.class,
                id,
                actor)
            > 0;
    return new Post(
        p.id(),
        p.galleryId(),
        p.galleryName(),
        p.category(),
        p.title(),
        p.content(),
        p.nickname(),
        p.authorTag(),
        p.views(),
        p.likes(),
        p.commentCount(),
        p.createdAt(),
        liked);
  }

  public long createPost(PostInput p, String tag, String hash) {
    return insert(
        "INSERT INTO posts(gallery_id,category,title,content,nickname,author_tag,password_hash)"
            + " VALUES(?,?,?,?,?,?,?)",
        p.galleryId(),
        p.category(),
        p.title().trim(),
        p.content().trim(),
        p.nickname().trim(),
        tag,
        hash);
  }

  public void updatePost(long id, PostInput p) {
    db.update(
        "UPDATE posts SET"
            + " gallery_id=?,category=?,title=?,content=?,nickname=?,updated_at=CURRENT_TIMESTAMP"
            + " WHERE id=?",
        p.galleryId(),
        p.category(),
        p.title().trim(),
        p.content().trim(),
        p.nickname().trim(),
        id);
  }

  public void view(long id) {
    db.update("UPDATE posts SET views=views+1 WHERE id=?", id);
  }

  public void deletePost(long id) {
    db.update("DELETE FROM posts WHERE id=?", id);
  }

  public String postPassword(long id) {
    return hash("SELECT password_hash FROM posts WHERE id=?", id);
  }

  public String commentPassword(long id, long postId) {
    return hash("SELECT password_hash FROM comments WHERE id=? AND post_id=?", id, postId);
  }

  private String hash(String sql, Object... args) {
    return db.query(sql, (r, n) -> r.getString(1), args).stream()
        .findFirst()
        .orElseThrow(CommunityRepository::notFound);
  }

  public void like(long id, String actor) {
    // 존재하는 게시글 행을 잠그면 동시에 들어오는 추천·취소도 순서대로 처리됩니다.
    if (db.queryForList("SELECT id FROM posts WHERE id=? FOR UPDATE", Long.class, id).isEmpty())
      throw notFound();
    if (db.update("DELETE FROM post_likes WHERE post_id=? AND actor_id=?", id, actor) == 0)
      db.update("INSERT INTO post_likes(post_id,actor_id) VALUES(?,?)", id, actor);
  }

  public List<Comment> comments(long postId, long after) {
    return db.query(
        "SELECT * FROM comments WHERE post_id=? AND id>? ORDER BY id LIMIT 100",
        commentMapper,
        postId,
        after);
  }

  public Comment createComment(long postId, CommentInput c, String tag, String hash) {
    long id =
        insert(
            "INSERT INTO comments(post_id,nickname,author_tag,content,password_hash)"
                + " VALUES(?,?,?,?,?)",
            postId,
            c.nickname().trim(),
            tag,
            c.content().trim(),
            hash);
    return db.queryForObject("SELECT * FROM comments WHERE id=?", commentMapper, id);
  }

  public void deleteComment(long postId, long id) {
    db.update("DELETE FROM comments WHERE post_id=? AND id=?", postId, id);
  }

  public List<Room> rooms() {
    return db.query(
        "SELECT * FROM chat_rooms ORDER BY id",
        (r, n) ->
            new Room(
                r.getLong("id"),
                r.getString("name"),
                r.getString("category"),
                r.getString("description"),
                r.getString("password_hash") != null,
                0));
  }

  public Room room(long id) {
    return rooms().stream()
        .filter(r -> r.id() == id)
        .findFirst()
        .orElseThrow(CommunityRepository::notFound);
  }

  public String roomPassword(long id) {
    List<String> rows =
        db.query("SELECT password_hash FROM chat_rooms WHERE id=?", (r, n) -> r.getString(1), id);
    if (rows.isEmpty()) throw notFound();
    return rows.get(0);
  }

  public Room createRoom(RoomInput r, String hash) {
    long id =
        insert(
            "INSERT INTO chat_rooms(name,category,description,password_hash) VALUES(?,?,?,?)",
            r.name().trim(),
            r.category(),
            r.description().trim(),
            hash);
    return room(id);
  }

  public List<ChatMessage> messages(long roomId, long before) {
    // 과거 방향 커서 페이지. 최근 100건부터 표시하고 필요할 때 더 불러옵니다.
    List<ChatMessage> rows =
        db.query(
            "SELECT * FROM chat_messages WHERE room_id=? AND id<? ORDER BY id DESC LIMIT 100",
            messageMapper,
            roomId,
            before);
    Collections.reverse(rows);
    return rows;
  }

  @org.springframework.transaction.annotation.Transactional
  public ChatMessage message(long roomId, MessageInput m, String tag) {
    long id =
        insert(
            "INSERT INTO chat_messages(room_id,nickname,author_tag,content) VALUES(?,?,?,?)",
            roomId,
            m.nickname().trim(),
            tag,
            m.content().trim());
    return db.queryForObject("SELECT * FROM chat_messages WHERE id=?", messageMapper, id);
  }

  private long insert(String sql, Object... params) {
    KeyHolder key = new GeneratedKeyHolder();
    db.update(
        connection -> {
          // ID 컬럼만 요청합니다. DB 드라이버가 생성 시각까지 반환하는 경우도 구분합니다.
          PreparedStatement statement =
              connection.prepareStatement(sql, new String[] {"id"});
          for (int i = 0; i < params.length; i++) statement.setObject(i + 1, params[i]);
          return statement;
        },
        key);
    return Objects.requireNonNull(key.getKey()).longValue();
  }

  public static ResponseStatusException notFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다.");
  }
}
