package com.joji.community.common;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** 게시글·댓글·채팅방에서 공통으로 사용하는 BCrypt 비밀번호 처리기입니다. */
@Component
public class PasswordManager {
  private static final int BCRYPT_MAX_BYTES = 72;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  /** 평문 비밀번호를 검증한 뒤 BCrypt 해시로 변환합니다. */
  public String encode(String password) {
    validateLength(password);
    return encoder.encode(password);
  }

  /** 사용자가 입력한 평문과 DB의 BCrypt 해시가 같은지 확인합니다. */
  public void verify(String raw, String hash) {
    if (raw == null || byteLength(raw) > BCRYPT_MAX_BYTES || !encoder.matches(raw, hash)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "비밀번호가 올바르지 않습니다.");
    }
  }

  private void validateLength(String password) {
    // BCrypt의 제한은 '문자 수'가 아니라 UTF-8로 인코딩한 '바이트 수'입니다.
    if (password == null || byteLength(password) > BCRYPT_MAX_BYTES) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.");
    }
  }

  private int byteLength(String value) {
    return value.getBytes(StandardCharsets.UTF_8).length;
  }
}
