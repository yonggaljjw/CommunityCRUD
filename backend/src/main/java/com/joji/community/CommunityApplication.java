package com.joji.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 애플리케이션의 시작점입니다.
 * 실제 기능 코드는 board/chat/common/config 하위 패키지에 기능별로 나뉘어 있습니다.
 */
@SpringBootApplication
public class CommunityApplication {
  public static void main(String[] args) {
    SpringApplication.run(CommunityApplication.class, args);
  }
}
