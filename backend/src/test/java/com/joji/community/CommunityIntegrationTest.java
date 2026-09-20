package com.joji.community;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// SQL 검증은 H2의 MySQL 호환 모드를 사용합니다. MySQL 컨테이너 테스트와 동일하다고 간주하지 않습니다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate db;
  @LocalServerPort int port;
  MockHttpSession session;

  @BeforeEach
  void setup() {
    session = new MockHttpSession();
  }

  String postBody(String title) {
    return """
{"galleryId":2,"category":"정보","title":"%s","content":"한글 본문 <script>alert(1)</script>","nickname":"익명","password":"test1234"}
"""
        .formatted(title);
  }

  long createPost() throws Exception {
    var result =
        mvc.perform(
                post("/api/posts")
                    .session(session)
                    .header("X-Requested-With", "JOJI")
                    .contentType("application/json")
                    .content(postBody("테스트 게시글")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andReturn();
    return json.readTree(result.getResponse().getContentAsString()).path("id").asLong();
  }

  @Test
  void postCrudPasswordAndCascade() throws Exception {
    long id = createPost();
    String stored =
        db.queryForObject("SELECT password_hash FROM posts WHERE id=?", String.class, id);
    assertNotEquals("test1234", stored);
    assertTrue(stored.startsWith("$2"));
    mvc.perform(
            put("/api/posts/" + id)
                .session(session)
                .header("X-Requested-With", "JOJI")
                .contentType("application/json")
                .content(postBody("수정").replace("test1234", "wrong")))
        .andExpect(status().isForbidden());
    session.removeAttribute("rate:password");
    mvc.perform(
            put("/api/posts/" + id)
                .session(session)
                .header("X-Requested-With", "JOJI")
                .contentType("application/json")
                .content(postBody("수정 성공")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("수정 성공"));
    mvc.perform(
            post("/api/posts/" + id + "/comments")
                .session(session)
                .header("X-Requested-With", "JOJI")
                .contentType("application/json")
                .content("{\"nickname\":\"댓글\",\"content\":\"안녕\",\"password\":\"test1234\"}"))
        .andExpect(status().isCreated());
    session.removeAttribute("rate:password");
    mvc.perform(
            delete("/api/posts/" + id)
                .session(session)
                .header("X-Requested-With", "JOJI")
                .contentType("application/json")
                .content("{\"password\":\"test1234\"}"))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/posts/" + id)).andExpect(status().isNotFound());
    assertEquals(
        0L, db.queryForObject("SELECT COUNT(*) FROM comments WHERE post_id=?", Long.class, id));
  }

  @Test
  void validationOriginAndRateLimit() throws Exception {
    mvc.perform(post("/api/posts").contentType("application/json").content(postBody("거부")))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/posts")
                .header("X-Requested-With", "JOJI")
                .header("Origin", "https://evil.example")
                .contentType("application/json")
                .content(postBody("거부")))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/posts")
                .session(session)
                .header("X-Requested-With", "JOJI")
                .contentType("application/json")
                .content(postBody("")))
        .andExpect(status().isBadRequest());
    createPost();
    mvc.perform(
            post("/api/posts")
                .session(session)
                .header("X-Requested-With", "JOJI")
                .contentType("application/json")
                .content(postBody("도배")))
        .andExpect(status().isTooManyRequests());
    mvc.perform(get("/api/posts?page=-1")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/posts?size=10000")).andExpect(status().isBadRequest());
  }

  @Test
  void uniqueLikesAndSafeSearch() throws Exception {
    long id = createPost();
    mvc.perform(
            post("/api/posts/" + id + "/like").session(session).header("X-Requested-With", "JOJI"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.likes").value(1))
        .andExpect(jsonPath("$.liked").value(true));
    session.removeAttribute("rate:like");
    mvc.perform(
            post("/api/posts/" + id + "/like").session(session).header("X-Requested-With", "JOJI"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.likes").value(0));
    mvc.perform(get("/api/posts").param("q", "' OR 1=1 --"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0));
    mvc.perform(get("/api/posts").param("q", "테스트").param("galleryId", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].galleryId").value(2));
  }

  @Test
  void privateRoomHistoryRequiresPassword() throws Exception {
    var result =
        mvc.perform(
                post("/api/rooms")
                    .session(session)
                    .header("X-Requested-With", "JOJI")
                    .contentType("application/json")
                    .content(
                        "{\"name\":\"비밀방\",\"category\":\"자유\",\"description\":\"테스트\",\"password\":\"secret123\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    long id = json.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    MockHttpSession stranger = new MockHttpSession();
    mvc.perform(get("/api/rooms/" + id + "/messages").session(stranger))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/rooms/" + id + "/join")
                .session(stranger)
                .header("X-Requested-With", "JOJI")
                .contentType("application/json")
                .content("{\"password\":\"wrong\"}"))
        .andExpect(status().isForbidden());
    stranger.removeAttribute("rate:roomPassword");
    mvc.perform(
            post("/api/rooms/" + id + "/join")
                .session(stranger)
                .header("X-Requested-With", "JOJI")
                .contentType("application/json")
                .content("{\"password\":\"secret123\"}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/rooms/" + id + "/messages").session(stranger)).andExpect(status().isOk());
  }

  @Test
  void realWebSocketBroadcastIsolationValidationAndPersistence() throws Exception {
    Client a = new Client(), b = new Client(), other = new Client();
    try {
      a.connect(1);
      b.connect(1);
      other.connect(2);
      a.listener.await("history");
      b.listener.await("history");
      other.listener.await("history");
      a.send("안녕하세요 한글 채팅");
      JsonNode received = b.listener.await("message");
      assertEquals("안녕하세요 한글 채팅", received.path("message").path("content").asText());
      assertEquals(1, received.path("message").path("roomId").asInt());
      a.listener.await("ack");
      assertEquals(0, other.listener.messageCount.get(), "다른 방으로 메시지가 새면 안 됩니다.");
      a.send("즉시 도배");
      assertEquals("error", a.listener.await("error").path("type").asText());
      b.send("x".repeat(501));
      b.listener.await("error");
      String saved = b.request("GET", "/api/rooms/1/messages", null).body();
      assertTrue(saved.contains("안녕하세요 한글 채팅"));
    } finally {
      a.close();
      b.close();
      other.close();
    }
  }

  @Test
  void webSocketRejectsPrivateRoomAndForeignOrigin() throws Exception {
    Client owner = new Client(), stranger = new Client();
    try {
      owner.request("GET", "/api/session", null);
      stranger.request("GET", "/api/session", null);
      var response =
          owner.request(
              "POST",
              "/api/rooms",
              "{\"name\":\"소켓"
                  + " 비밀방\",\"category\":\"자유\",\"description\":\"\",\"password\":\"secret123\"}");
      long id = json.readTree(response.body()).path("id").asLong();
      assertThrows(CompletionException.class, () -> stranger.open(id, "http://localhost:8088"));
      assertThrows(CompletionException.class, () -> stranger.open(1, "https://evil.example"));
      assertEquals(
          200,
          stranger
              .request("POST", "/api/rooms/" + id + "/join", "{\"password\":\"secret123\"}")
              .statusCode());
      stranger.open(id, "http://localhost:8088");
      stranger.listener.await("history");
    } finally {
      owner.close();
      stranger.close();
    }
  }

  class Client {
    final HttpClient client =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    final Listener listener = new Listener();
    WebSocket socket;

    HttpResponse<String> request(String method, String path, String body) throws Exception {
      var b =
          HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
              .timeout(Duration.ofSeconds(8))
              .header("X-Requested-With", "JOJI")
              .header("Content-Type", "application/json");
      return client.send(
          b.method(
                  method,
                  body == null
                      ? HttpRequest.BodyPublishers.noBody()
                      : HttpRequest.BodyPublishers.ofString(body))
              .build(),
          HttpResponse.BodyHandlers.ofString());
    }

    void connect(long id) throws Exception {
      request("GET", "/api/session", null);
      open(id, "http://localhost:8088");
    }

    void open(long id, String origin) {
      socket =
          client
              .newWebSocketBuilder()
              .header("Origin", origin)
              .connectTimeout(Duration.ofSeconds(5))
              .buildAsync(URI.create("ws://localhost:" + port + "/ws/chat?roomId=" + id), listener)
              .join();
    }

    void send(String content) throws Exception {
      socket
          .sendText(
              json.writeValueAsString(
                  Map.of(
                      "type",
                      "message",
                      "clientId",
                      UUID.randomUUID().toString(),
                      "nickname",
                      "테스터",
                      "content",
                      content)),
              true)
          .join();
    }

    void close() {
      if (socket != null) socket.abort();
    }
  }

  class Listener implements WebSocket.Listener {
    final BlockingQueue<JsonNode> queue = new LinkedBlockingQueue<>();
    final StringBuilder buffer = new StringBuilder();
    final AtomicInteger messageCount = new AtomicInteger();

    @Override
    public void onOpen(WebSocket socket) {
      socket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
      buffer.append(data);
      if (last) {
        try {
          JsonNode n = json.readTree(buffer.toString());
          queue.add(n);
          if (n.path("type").asText().equals("message")) messageCount.incrementAndGet();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        buffer.setLength(0);
      }
      socket.request(1);
      return null;
    }

    JsonNode await(String type) throws Exception {
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
      while (System.nanoTime() < deadline) {
        JsonNode n = queue.poll(200, TimeUnit.MILLISECONDS);
        if (n != null && n.path("type").asText().equals(type)) return n;
      }
      fail("WebSocket 이벤트 수신 실패: " + type);
      return null;
    }
  }
}
