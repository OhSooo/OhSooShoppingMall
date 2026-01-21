package com.ohsooo.platform.ohsooshoppingmall.global.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConsoleEmailSender implements EmailSender {

  @Override
  public void sendTemporaryPassword(String email, String tempPassword) {
    // 🔥 실제 메일 대신 로그로만 출력 (개발용)
    log.info("[DEV] Temporary password sent");
    log.info("email = {}", email);
    log.info("tempPassword = {}", tempPassword);
  }
}
