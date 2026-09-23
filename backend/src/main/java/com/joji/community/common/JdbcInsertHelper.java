package com.joji.community.common;

import java.sql.PreparedStatement;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

/** INSERT 후 AUTO_INCREMENT로 생성된 id를 돌려받는 JDBC 공통 코드입니다. */
@Component
public class JdbcInsertHelper {
  private final JdbcTemplate db;

  public JdbcInsertHelper(JdbcTemplate db) {
    this.db = db;
  }

  public long insert(String sql, Object... params) {
    KeyHolder key = new GeneratedKeyHolder();
    db.update(
        connection -> {
          // 생성 키 중 id 컬럼만 요청해 DB 드라이버별 반환 차이를 줄입니다.
          PreparedStatement statement = connection.prepareStatement(sql, new String[] {"id"});
          for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
          }
          return statement;
        },
        key);
    return Objects.requireNonNull(key.getKey()).longValue();
  }
}
