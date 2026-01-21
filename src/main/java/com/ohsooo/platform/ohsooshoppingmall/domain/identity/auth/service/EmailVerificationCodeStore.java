package com.ohsooo.platform.ohsooshoppingmall.domain.identity.auth.service;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerificationCodeStore {

  private final StringRedisTemplate redis;

  private static final String KEY_PREFIX = "email_verify:";

  public void save(String email, String code, Duration ttl) {
    redis.opsForValue().set(KEY_PREFIX + email, code, ttl);
  }

  public Optional<String> find(String email) {
    return Optional.ofNullable(redis.opsForValue().get(KEY_PREFIX + email));
  }

  public void delete(String email) {
    redis.delete(KEY_PREFIX + email);
  }

  public boolean verify(String email, String code) {
    String saved = redis.opsForValue().get(KEY_PREFIX + email);
    return saved != null && saved.equals(code);
  }
}
