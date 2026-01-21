package com.ohsooo.platform.ohsooshoppingmall.global.mail;

public interface EmailSender {
  void sendTemporaryPassword(String toEmail, String tempPassword);
}
