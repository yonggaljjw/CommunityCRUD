package com.joji.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 애플리케이션의 시작점입니다. 하위 패키지의 Controller, Service 등을 자동 등록합니다.
@SpringBootApplication
public class CommunityApplication {
  public static void main(String[] args) {
    SpringApplication.run(CommunityApplication.class, args);
  }
}
