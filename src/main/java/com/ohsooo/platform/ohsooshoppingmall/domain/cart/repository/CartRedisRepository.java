package com.ohsooo.platform.ohsooshoppingmall.domain.cart.repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 장바구니 저장소. Redis Hash 하나 = 유저 한 명의 장바구니.
 *
 * 키: cart:{userId}
 * field: itemVariantId, value: quantity
 */
@Repository
@RequiredArgsConstructor
public class CartRedisRepository {

  private static final String KEY_PREFIX = "cart:";

  private final StringRedisTemplate redisTemplate;

  private HashOperations<String, String, String> hashOps() {
    return redisTemplate.opsForHash();
  }

  private String key(Long userId) {
    return KEY_PREFIX + userId;
  }

  /**
   * 유저 장바구니 전체 조회. (itemVariantId -> quantity)
   */
  public Map<Long, Integer> findAll(Long userId) {
    Map<Object, Object> raw = redisTemplate.opsForHash().entries(key(userId));
    Map<Long, Integer> result = new LinkedHashMap<>();
    for (Map.Entry<Object, Object> entry : raw.entrySet()) {
      result.put(Long.valueOf((String) entry.getKey()), Integer.valueOf((String) entry.getValue()));
    }
    return result;
  }

  public Optional<Integer> findQuantity(Long userId, Long itemVariantId) {
    String value = hashOps().get(key(userId), String.valueOf(itemVariantId));
    return value == null ? Optional.empty() : Optional.of(Integer.valueOf(value));
  }

  public boolean exists(Long userId, Long itemVariantId) {
    return hashOps().hasKey(key(userId), String.valueOf(itemVariantId));
  }

  public boolean isEmpty(Long userId) {
    return Boolean.FALSE.equals(redisTemplate.hasKey(key(userId)));
  }

  /**
   * 담겨있으면 수량 증가, 없으면 신규 추가. 원자적 연산이라 동시 요청에도 안전.
   * 증가 후의 최종 수량을 반환한다.
   */
  public int increaseQuantity(Long userId, Long itemVariantId, int amount) {
    Long result = hashOps().increment(key(userId), String.valueOf(itemVariantId), amount);
    return result.intValue();
  }

  public void setQuantity(Long userId, Long itemVariantId, int quantity) {
    hashOps().put(key(userId), String.valueOf(itemVariantId), String.valueOf(quantity));
  }

  public void removeItem(Long userId, Long itemVariantId) {
    hashOps().delete(key(userId), String.valueOf(itemVariantId));
  }

  public void removeItems(Long userId, Collection<Long> itemVariantIds) {
    if (itemVariantIds == null || itemVariantIds.isEmpty()) return;
    Object[] fields = itemVariantIds.stream().map(String::valueOf).toArray();
    hashOps().delete(key(userId), fields);
  }

  public void clear(Long userId) {
    redisTemplate.delete(key(userId));
  }
}
