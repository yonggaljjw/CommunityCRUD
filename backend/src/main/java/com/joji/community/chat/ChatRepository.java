package com.joji.community.chat;

import static com.joji.community.chat.ChatModels.*;

import com.joji.community.common.JdbcInsertHelper;
import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 채팅방·채팅 메시지 SQL만 담당하는 Repository입니다. */
@Repository
public class ChatRepository {
  private final JdbcTemplate db;
  private final JdbcInsertHelper insertHelper;

  public ChatRepository(JdbcTemplate db, JdbcInsertHelper insertHelper) {
    this.db = db;
    this.insertHelper = insertHelper;
  }

  private final RowMapper<Room> roomMapper =
      (r, n) ->
          new Room(
              r.getLong("id"),
              r.getString("name"),
              r.getString("category"),
              r.getString("description"),
              r.getString("password_hash") != null,
              0);

  private final RowMapper<ChatMessage> messageMapper =
      (r, n) ->
          new ChatMessage(
              r.getLong("id"),
              r.getLong("room_id"),
              r.getString("nickname"),
              r.getString("author_tag"),
              r.getString("content"),
              r.getTimestamp("created_at").toInstant());

  public List<Room> rooms() {
    return db.query("SELECT * FROM chat_rooms ORDER BY id", roomMapper);
  }

  public Room room(long id) {
    return db.query("SELECT * FROM chat_rooms WHERE id=?", roomMapper, id).stream()
        .findFirst()
        .orElseThrow(ChatRepository::notFound);
  }

  public String roomPassword(long id) {
    List<String> rows =
        db.query("SELECT password_hash FROM chat_rooms WHERE id=?", (r, n) -> r.getString(1), id);
    if (rows.isEmpty()) {
      throw notFound();
    }
    return rows.get(0);
  }

  public Room createRoom(RoomInput room, String hash) {
    long id =
        insertHelper.insert(
            "INSERT INTO chat_rooms(name,category,description,password_hash) VALUES(?,?,?,?)",
            room.name().trim(),
            room.category(),
            room.description().trim(),
            hash);
    return room(id);
  }

  /** 최근 메시지부터 100개를 조회한 뒤 화면 표시 순서에 맞춰 오래된 것부터 정렬합니다. */
  public List<ChatMessage> messages(long roomId, long before) {
    List<ChatMessage> rows =
        db.query(
            "SELECT * FROM chat_messages WHERE room_id=? AND id<? ORDER BY id DESC LIMIT 100",
            messageMapper,
            roomId,
            before);
    Collections.reverse(rows);
    return rows;
  }

  @Transactional
  public ChatMessage saveMessage(long roomId, MessageInput message, String tag) {
    long id =
        insertHelper.insert(
            "INSERT INTO chat_messages(room_id,nickname,author_tag,content) VALUES(?,?,?,?)",
            roomId,
            message.nickname().trim(),
            tag,
            message.content().trim());
    return db.queryForObject("SELECT * FROM chat_messages WHERE id=?", messageMapper, id);
  }

  private static ResponseStatusException notFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다.");
  }
}
