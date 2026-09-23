package com.joji.community.board;

import static com.joji.community.board.BoardModels.*;

import com.joji.community.common.JdbcInsertHelper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

/** 게시판 기능의 SQL을 한곳에 모은 Repository입니다. Controller는 SQL을 직접 알 필요가 없습니다. */
@Repository
public class BoardRepository {
  private final JdbcTemplate db;
  private final JdbcInsertHelper insertHelper;

  public BoardRepository(JdbcTemplate db, JdbcInsertHelper insertHelper) {
    this.db = db;
    this.insertHelper = insertHelper;
  }

  // 모든 사용자 값은 ? 파라미터로 전달합니다. 문자열 연결은 서버가 정한 고정 SQL 조각에만 사용합니다.
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

  public List<Gallery> galleries() {
    return db.query(
        "SELECT g.*, (SELECT COUNT(*) FROM posts p WHERE p.gallery_id=g.id) AS cnt "
            + "FROM galleries g ORDER BY g.id",
        (r, n) ->
            new Gallery(
                r.getLong("id"),
                r.getString("slug"),
                r.getString("name"),
                r.getString("description"),
                r.getLong("cnt")));
  }

  /** 게시글 목록은 조건을 조립한 뒤 LIMIT/OFFSET으로 페이지를 잘라 반환합니다. */
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
      // LOCATE는 %, _를 와일드카드로 보지 않아 검색어를 그대로 찾습니다.
      where += " AND (LOCATE(?,p.title)>0 OR LOCATE(?,p.content)>0)";
      params.add(query);
      params.add(query);
    }
    if (sort.equals("best")) {
      where += " AND (SELECT COUNT(*) FROM post_likes l WHERE l.post_id=p.id)>=30";
    }

    long total = db.queryForObject("SELECT COUNT(*) FROM posts p" + where, Long.class, params.toArray());
    String order =
        sort.equals("hot") || sort.equals("best")
            ? " ORDER BY likes DESC,p.id DESC"
            : " ORDER BY p.id DESC";

    params.add(size);
    params.add((page - 1) * size);
    List<Post> items =
        db.query(POST_SELECT + where + order + " LIMIT ? OFFSET ?", postMapper, params.toArray());
    return new Page<>(items, total, page, size);
  }

  /** 상세 글과 현재 익명 사용자의 추천 여부를 합쳐 반환합니다. */
  public Post post(long id, String actor) {
    Post post =
        db.query(POST_SELECT + " WHERE p.id=?", postMapper, id).stream()
            .findFirst()
            .orElseThrow(BoardRepository::notFound);

    boolean liked =
        db.queryForObject(
                "SELECT COUNT(*) FROM post_likes WHERE post_id=? AND actor_id=?",
                Long.class,
                id,
                actor)
            > 0;

    return new Post(
        post.id(),
        post.galleryId(),
        post.galleryName(),
        post.category(),
        post.title(),
        post.content(),
        post.nickname(),
        post.authorTag(),
        post.views(),
        post.likes(),
        post.commentCount(),
        post.createdAt(),
        liked);
  }

  public long createPost(PostInput post, String tag, String hash) {
    return insertHelper.insert(
        "INSERT INTO posts(gallery_id,category,title,content,nickname,author_tag,password_hash) "
            + "VALUES(?,?,?,?,?,?,?)",
        post.galleryId(),
        post.category(),
        post.title().trim(),
        post.content().trim(),
        post.nickname().trim(),
        tag,
        hash);
  }

  public void updatePost(long id, PostInput post) {
    db.update(
        "UPDATE posts SET gallery_id=?,category=?,title=?,content=?,nickname=?,"
            + "updated_at=CURRENT_TIMESTAMP WHERE id=?",
        post.galleryId(),
        post.category(),
        post.title().trim(),
        post.content().trim(),
        post.nickname().trim(),
        id);
  }

  public void increaseView(long id) {
    db.update("UPDATE posts SET views=views+1 WHERE id=?", id);
  }

  public void deletePost(long id) {
    db.update("DELETE FROM posts WHERE id=?", id);
  }

  public String postPassword(long id) {
    return requiredHash("SELECT password_hash FROM posts WHERE id=?", id);
  }

  public String commentPassword(long id, long postId) {
    return requiredHash("SELECT password_hash FROM comments WHERE id=? AND post_id=?", id, postId);
  }

  private String requiredHash(String sql, Object... args) {
    return db.query(sql, (r, n) -> r.getString(1), args).stream()
        .findFirst()
        .orElseThrow(BoardRepository::notFound);
  }

  /** 추천 행이 있으면 삭제하고, 없으면 추가하여 토글 동작을 만듭니다. */
  public void toggleLike(long id, String actor) {
    // 게시글 행을 먼저 잠가 동시 추천/취소가 순서대로 처리되도록 합니다.
    if (db.queryForList("SELECT id FROM posts WHERE id=? FOR UPDATE", Long.class, id).isEmpty()) {
      throw notFound();
    }
    if (db.update("DELETE FROM post_likes WHERE post_id=? AND actor_id=?", id, actor) == 0) {
      db.update("INSERT INTO post_likes(post_id,actor_id) VALUES(?,?)", id, actor);
    }
  }

  public List<Comment> comments(long postId, long after) {
    return db.query(
        "SELECT * FROM comments WHERE post_id=? AND id>? ORDER BY id LIMIT 100",
        commentMapper,
        postId,
        after);
  }

  public Comment createComment(long postId, CommentInput comment, String tag, String hash) {
    long id =
        insertHelper.insert(
            "INSERT INTO comments(post_id,nickname,author_tag,content,password_hash) VALUES(?,?,?,?,?)",
            postId,
            comment.nickname().trim(),
            tag,
            comment.content().trim(),
            hash);
    return db.queryForObject("SELECT * FROM comments WHERE id=?", commentMapper, id);
  }

  public void deleteComment(long postId, long id) {
    db.update("DELETE FROM comments WHERE post_id=? AND id=?", postId, id);
  }

  public static ResponseStatusException notFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다.");
  }
}
