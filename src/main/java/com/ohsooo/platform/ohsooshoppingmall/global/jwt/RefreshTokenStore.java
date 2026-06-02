package com.ohsooo.platform.ohsooshoppingmall.global.jwt;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenStore {

  private final StringRedisTemplate redis;

  private static final String KEY_PREFIX = "refresh:";

  public void save(Long userId, String refreshToken, Duration ttl) {
    redis.opsForValue().set(KEY_PREFIX + userId, refreshToken, ttl);
  }

  public Optional<String> find(Long userId) {
    return Optional.ofNullable(redis.opsForValue().get(KEY_PREFIX + userId));
  }

  public void delete(Long userId) {
    redis.delete(KEY_PREFIX + userId);
  }
}
