package com.ohsooo.platform.ohsooshoppingmall.domain.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.Coupon;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.DiscountType;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.repository.CouponRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.service.UserCouponService;
import com.ohsooo.platform.ohsooshoppingmall.domain.inventory.exception.InventoryErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderCreateRequestDto.OrderSource;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderCreateRequestDto.ShippingInfo;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderItemCancelRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.request.OrderItemCreateRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.dto.response.OrderCreateResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.order.service.OrderService;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 실제 PostgreSQL에서 주문 생성 ↔ 쿠폰 사용의 트랜잭션 정합성과 0원 주문 결제 완료를 검증한다. (#68)
 *
 * <p>쿠폰 통합 테스트와 같이 환경변수 COUPON_IT_DB_URL이 있을 때만 테스트 전용 DB에서 실행한다.
 * DIRECT 주문을 사용해 Redis(장바구니) 없이 검증하며, 테스트가 만든 주문/결제/쿠폰과 바꾼 재고는 매번 정리한다.
 */
@SpringBootTest(properties = {
    "MAIL_USERNAME=test", "MAIL_PASSWORD=test", "MAIL_FROM=test@test.com",
    "GOOGLE_CLIENT_ID=test", "GOOGLE_CLIENT_SECRET=test",
    "NAVER_CLIENT_ID=test", "NAVER_CLIENT_SECRET=test",
    "KAKAO_CLIENT_ID=test", "KAKAO_CLIENT_SECRET=test",
    "OAUTH_REDIRECT_URI=http://localhost/",
    "TOSS_SECRET_KEY=test_sk",
    // base64("test-secret-for-integration-tests-1234567890")
    "JWT_SECRET=dGVzdC1zZWNyZXQtZm9yLWludGVncmF0aW9uLXRlc3RzLTEyMzQ1Njc4OTA=",
    "spring.jpa.show-sql=false",
    "logging.level.org.springframework.security=INFO"
})
@EnabledIfEnvironmentVariable(named = "COUPON_IT_DB_URL", matches = ".+")
class OrderCouponIntegrationTest {

  private static final String MAIN_DB_NAME = "ohsoo_shoppingmall";

  @DynamicPropertySource
  static void testDatabase(DynamicPropertyRegistry registry) {
    String url = System.getenv("COUPON_IT_DB_URL");
    // 운영(로컬 기본) DB에서 실수로 실행되는 것을 막는다.
    if (url.replaceAll("\\?.*$", "").endsWith("/" + MAIN_DB_NAME)) {
      throw new IllegalStateException("통합 테스트는 기본 DB(" + MAIN_DB_NAME + ")에서 실행할 수 없습니다: " + url);
    }
    registry.add("spring.datasource.url", () -> url);
  }

  @Autowired private OrderService orderService;
  @Autowired private UserCouponService userCouponService;
  @Autowired private CouponRepository couponRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private final List<Long> couponIds = new ArrayList<>();

  private Long userId;
  private Long variantId;
  private BigDecimal unitPrice;
  private int originalStock;
  private String originalVariantStatus;
  private long maxOrderIdBefore;

  @BeforeEach
  void setUp() {
    userId = jdbcTemplate.queryForObject(
        "SELECT user_id FROM users WHERE is_deleted = false ORDER BY user_id LIMIT 1", Long.class);
    Map<String, Object> variant = jdbcTemplate.queryForMap("""
        SELECT v.item_variant_id, v.price, v.status, i.quantity
          FROM item_variants v JOIN inventories i ON i.item_variant_id = v.item_variant_id
         WHERE v.status = 'ACTIVE' AND i.quantity >= 10
         ORDER BY v.item_variant_id LIMIT 1
        """);
    variantId = ((Number) variant.get("item_variant_id")).longValue();
    unitPrice = (BigDecimal) variant.get("price");
    originalStock = ((Number) variant.get("quantity")).intValue();
    originalVariantStatus = (String) variant.get("status");
    maxOrderIdBefore = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(order_id), 0) FROM orders", Long.class);
  }

  @AfterEach
  void tearDown() {
    List<Long> orderIds = jdbcTemplate.queryForList(
        "SELECT order_id FROM orders WHERE order_id > ?", Long.class, maxOrderIdBefore);
    for (Long orderId : orderIds) {
      jdbcTemplate.update("DELETE FROM payments WHERE order_id = ?", orderId);
      jdbcTemplate.update(
          "DELETE FROM order_item_histories WHERE order_item_id IN (SELECT order_item_id FROM order_items WHERE order_id = ?)",
          orderId);
      jdbcTemplate.update("DELETE FROM order_items WHERE order_id = ?", orderId);
      jdbcTemplate.update("DELETE FROM orders WHERE order_id = ?", orderId);
    }
    for (Long couponId : couponIds) {
      jdbcTemplate.update("DELETE FROM user_coupons WHERE coupon_id = ?", couponId);
      jdbcTemplate.update("DELETE FROM coupons WHERE coupon_id = ?", couponId);
    }
    jdbcTemplate.update("UPDATE inventories SET quantity = ? WHERE item_variant_id = ?", originalStock, variantId);
    jdbcTemplate.update("UPDATE item_variants SET status = ? WHERE item_variant_id = ?", originalVariantStatus, variantId);
  }

  /** 정액 쿠폰을 만들어 사용자에게 발급하고 userCouponId를 반환한다. */
  private Long issueFixedCoupon(BigDecimal discountValue) {
    OffsetDateTime now = OffsetDateTime.now();
    Coupon coupon = Coupon.create(
        "주문연동통합테스트쿠폰", DiscountType.FIXED_AMOUNT, discountValue, null, null,
        now.minusDays(1), now.plusDays(1), now.minusDays(1), now.plusDays(1));
    Long couponId = couponRepository.save(coupon).getCouponId();
    couponIds.add(couponId);
    return userCouponService.issueCoupon(userId, couponId).getUserCouponId();
  }

  private OrderCreateRequestDto directOrder(int quantity, Long userCouponId) {
    return new OrderCreateRequestDto(OrderSource.DIRECT, null,
        List.of(new OrderItemCreateRequestDto(variantId, quantity)), null,
        new ShippingInfo("통합테스트", "010-0000-0000", "서울시", null, null, null),
        userCouponId);
  }

  private Map<String, Object> userCouponRow(Long userCouponId) {
    return jdbcTemplate.queryForMap(
        "SELECT status, used_order_id FROM user_coupons WHERE user_coupon_id = ?", userCouponId);
  }

  private int createdOrderCount() {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM orders WHERE order_id > ?", Integer.class, maxOrderIdBefore);
  }

  private int stock() {
    return jdbcTemplate.queryForObject(
        "SELECT quantity FROM inventories WHERE item_variant_id = ?", Integer.class, variantId);
  }

  @Test
  void 쿠폰을_적용하면_할인_금액과_적용_쿠폰이_저장되고_쿠폰은_해당_주문으로_USED된다() {
    Long userCouponId = issueFixedCoupon(BigDecimal.valueOf(1000));

    OrderCreateResponseDto res = orderService.createOrder(userId, directOrder(1, userCouponId));

    Map<String, Object> order = jdbcTemplate.queryForMap(
        "SELECT status, discount_amount, final_price, user_coupon_id FROM orders WHERE order_id = ?",
        res.getOrderId());
    assertThat(order.get("status")).isEqualTo("CREATED");
    assertThat((BigDecimal) order.get("discount_amount")).isEqualByComparingTo("1000");
    assertThat((BigDecimal) order.get("final_price")).isEqualByComparingTo(unitPrice.subtract(BigDecimal.valueOf(1000)));
    assertThat(((Number) order.get("user_coupon_id")).longValue()).isEqualTo(userCouponId);

    Map<String, Object> coupon = userCouponRow(userCouponId);
    assertThat(coupon.get("status")).isEqualTo("USED");
    assertThat(((Number) coupon.get("used_order_id")).longValue()).isEqualTo(res.getOrderId());

    // 유료 주문은 결제 전이므로 결제 행/재고 변화 없음
    assertThat(jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM payments WHERE order_id = ?", Integer.class, res.getOrderId())).isZero();
    assertThat(stock()).isEqualTo(originalStock);
  }

  @Test
  void 최종_0원_주문은_PG_없이_결제행이_생성되고_PAID와_재고차감까지_한번에_반영된다() {
    Long userCouponId = issueFixedCoupon(unitPrice.multiply(BigDecimal.valueOf(2)));

    OrderCreateResponseDto res = orderService.createOrder(userId, directOrder(2, userCouponId));

    assertThat(res.getStatus()).isEqualTo("PAID");
    assertThat(res.getFinalPrice()).isEqualByComparingTo("0");
    assertThat(jdbcTemplate.queryForObject(
        "SELECT status FROM orders WHERE order_id = ?", String.class, res.getOrderId())).isEqualTo("PAID");

    Map<String, Object> payment = jdbcTemplate.queryForMap(
        "SELECT provider, method, status, amount, approved_at FROM payments WHERE order_id = ?", res.getOrderId());
    assertThat(payment.get("provider")).isEqualTo("NONE");
    assertThat(payment.get("method")).isEqualTo("ZERO_AMOUNT");
    assertThat(payment.get("status")).isEqualTo("CAPTURED");
    assertThat((BigDecimal) payment.get("amount")).isEqualByComparingTo("0");
    assertThat(payment.get("approved_at")).isNotNull();

    assertThat(stock()).isEqualTo(originalStock - 2);
    assertThat(userCouponRow(userCouponId).get("status")).isEqualTo("USED");
  }

  @Test
  void 쿠폰_사용_이후_단계에서_실패하면_주문_결제_쿠폰이_모두_롤백된다() {
    // 0원 주문에서 재고 차감(쿠폰 redeem 이후 마지막 단계)이 실패하도록 재고를 0으로 만든다
    jdbcTemplate.update("UPDATE inventories SET quantity = 0 WHERE item_variant_id = ?", variantId);
    Long userCouponId = issueFixedCoupon(unitPrice);

    assertThatThrownBy(() -> orderService.createOrder(userId, directOrder(1, userCouponId)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(InventoryErrorCode.INSUFFICIENT_STOCK);

    assertThat(createdOrderCount()).isZero();
    Map<String, Object> coupon = userCouponRow(userCouponId);
    assertThat(coupon.get("status")).isEqualTo("AVAILABLE");
    assertThat(coupon.get("used_order_id")).isNull();
  }

  @Test
  void 결제전_쿠폰주문을_전체취소하면_쿠폰이_복구되고_금액은_0_재고는_그대로다() {
    Long userCouponId = issueFixedCoupon(BigDecimal.valueOf(1000));
    OrderCreateResponseDto res = orderService.createOrder(userId, directOrder(1, userCouponId));
    Long orderItemId = res.getItems().get(0).getOrderItemId();

    orderService.requestCancelOrderItem(userId, orderItemId, new OrderItemCancelRequestDto("단순 변심"));
    orderService.confirmCancelOrderItem(userId, orderItemId);

    Map<String, Object> order = jdbcTemplate.queryForMap(
        "SELECT status, discount_amount, final_price, user_coupon_id FROM orders WHERE order_id = ?",
        res.getOrderId());
    assertThat(order.get("status")).isEqualTo("CANCELED");
    assertThat((BigDecimal) order.get("discount_amount")).isEqualByComparingTo("0");
    assertThat((BigDecimal) order.get("final_price")).isEqualByComparingTo("0");
    // 적용 이력은 쿠폰 복구 후에도 유지
    assertThat(((Number) order.get("user_coupon_id")).longValue()).isEqualTo(userCouponId);

    Map<String, Object> coupon = userCouponRow(userCouponId);
    assertThat(coupon.get("status")).isEqualTo("AVAILABLE");
    assertThat(coupon.get("used_order_id")).isNull();

    // 결제 전 주문이라 재고는 차감된 적이 없으므로 취소해도 그대로
    assertThat(stock()).isEqualTo(originalStock);
  }
}
