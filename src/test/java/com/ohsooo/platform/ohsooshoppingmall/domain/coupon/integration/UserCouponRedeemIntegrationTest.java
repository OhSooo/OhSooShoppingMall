package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.Coupon;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.DiscountType;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.exception.CouponErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.repository.CouponRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.service.UserCouponService;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 실제 PostgreSQL을 사용하는 쿠폰 사용(redeem)/복구(releaseByOrder) 통합 테스트.
 *
 * <p>UserCouponIssueIntegrationTest와 같이 환경변수 COUPON_IT_DB_URL이 있을 때만, 테스트 전용 DB에서 실행한다.
 * redeem()은 주문 생성 트랜잭션 참여(MANDATORY)를 전제로 하므로, TransactionTemplate으로 호출자 트랜잭션을 흉내 낸다.
 * user_coupons.used_order_id에는 FK가 없어 실제 주문 행 없이 임의의 주문 ID로 검증한다.
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
class UserCouponRedeemIntegrationTest {

  private static final String MAIN_DB_NAME = "ohsoo_shoppingmall";
  private static final BigDecimal ORDER_AMOUNT = BigDecimal.valueOf(10000);
  private static final BigDecimal DISCOUNT = BigDecimal.valueOf(1000);

  @DynamicPropertySource
  static void testDatabase(DynamicPropertyRegistry registry) {
    String url = System.getenv("COUPON_IT_DB_URL");
    // 운영(로컬 기본) DB에서 실수로 실행되는 것을 막는다.
    if (url.replaceAll("\\?.*$", "").endsWith("/" + MAIN_DB_NAME)) {
      throw new IllegalStateException("통합 테스트는 기본 DB(" + MAIN_DB_NAME + ")에서 실행할 수 없습니다: " + url);
    }
    registry.add("spring.datasource.url", () -> url);
  }

  @Autowired
  private UserCouponService userCouponService;

  @Autowired
  private CouponRepository couponRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private PlatformTransactionManager transactionManager;

  private final ExecutorService executor = Executors.newFixedThreadPool(10);

  private TransactionTemplate tx;
  private Long userId;
  private Long couponId;
  private Long userCouponId;

  @BeforeEach
  void setUp() {
    tx = new TransactionTemplate(transactionManager);
    userId = jdbcTemplate.queryForObject(
        "SELECT user_id FROM users WHERE is_deleted = false ORDER BY user_id LIMIT 1", Long.class);
    OffsetDateTime now = OffsetDateTime.now();
    Coupon coupon = Coupon.create(
        "사용통합테스트쿠폰", DiscountType.FIXED_AMOUNT, DISCOUNT, BigDecimal.valueOf(5000), null,
        now.minusDays(1), now.plusDays(1), now.minusDays(1), now.plusDays(1));
    couponId = couponRepository.save(coupon).getCouponId();
    userCouponId = userCouponService.issueCoupon(userId, couponId).getUserCouponId();
  }

  @AfterEach
  void tearDown() {
    executor.shutdownNow();
    jdbcTemplate.update("DELETE FROM user_coupons WHERE coupon_id = ?", couponId);
    jdbcTemplate.update("DELETE FROM coupons WHERE coupon_id = ?", couponId);
  }

  @Test
  void 동시에_redeem하면_정확히_한건만_성공하고_나머지는_이미_사용됨으로_거부된다() throws Exception {
    int threads = 10;
    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Object>> futures = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      long orderId = 900_000L + i;
      futures.add(executor.submit(() -> {
        ready.countDown();
        start.await();
        try {
          tx.executeWithoutResult(s ->
              userCouponService.redeem(userId, userCouponId, orderId, ORDER_AMOUNT, DISCOUNT));
          return orderId;
        } catch (Throwable t) {
          return t;
        }
      }));
    }
    ready.await(5, TimeUnit.SECONDS);
    start.countDown();

    List<Long> succeededOrderIds = new ArrayList<>();
    int alreadyUsed = 0;
    List<String> results = new ArrayList<>();
    for (Future<Object> f : futures) {
      Object r = f.get(15, TimeUnit.SECONDS);
      if (r instanceof Long orderId) {
        succeededOrderIds.add(orderId);
        results.add("OK:" + orderId);
      } else if (r instanceof BusinessException be
          && be.getErrorCode() == CouponErrorCode.USER_COUPON_ALREADY_USED) {
        alreadyUsed++;
        results.add("ALREADY_USED");
      } else {
        results.add("UNEXPECTED:" + r);
      }
    }
    System.out.println("[IT] concurrent redeem results = " + results);

    assertThat(succeededOrderIds).hasSize(1);
    assertThat(alreadyUsed).isEqualTo(threads - 1);

    // DB에는 성공한 한 건의 주문 이력만 남아야 한다 (나중 요청이 덮어쓰지 않음)
    Map<String, Object> row = userCouponRow();
    assertThat(row.get("status")).isEqualTo("USED");
    assertThat(((Number) row.get("used_order_id")).longValue()).isEqualTo(succeededOrderIds.get(0));
    assertThat((BigDecimal) row.get("discount_applied_amount")).isEqualByComparingTo(DISCOUNT);
    assertThat((BigDecimal) row.get("used_order_amount")).isEqualByComparingTo(ORDER_AMOUNT);
    assertThat(row.get("used_at")).isNotNull();
  }

  @Test
  void 호출자_트랜잭션이_롤백되면_쿠폰은_사용되지_않은_상태로_남는다() {
    // 주문 생성이 쿠폰 사용 이후 단계에서 실패하는 상황
    assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
      userCouponService.redeem(userId, userCouponId, 900_100L, ORDER_AMOUNT, DISCOUNT);
      throw new IllegalStateException("주문 저장 실패 가정");
    })).isInstanceOf(IllegalStateException.class);

    Map<String, Object> row = userCouponRow();
    assertThat(row.get("status")).isEqualTo("AVAILABLE");
    assertThat(row.get("used_order_id")).isNull();
  }

  @Test
  void 트랜잭션_없이_redeem을_호출하면_거부된다() {
    assertThatThrownBy(() ->
        userCouponService.redeem(userId, userCouponId, 900_200L, ORDER_AMOUNT, DISCOUNT))
        .isInstanceOf(IllegalTransactionStateException.class);

    assertThat(userCouponRow().get("status")).isEqualTo("AVAILABLE");
  }

  @Test
  void 복구하면_사용이력이_초기화되고_원래_사용조건으로_다시_사용할_수_있다() {
    long orderId = 900_300L;
    tx.executeWithoutResult(s ->
        userCouponService.redeem(userId, userCouponId, orderId, ORDER_AMOUNT, DISCOUNT));

    Integer released = tx.execute(s -> userCouponService.releaseByOrder(orderId));

    assertThat(released).isEqualTo(1);
    Map<String, Object> row = userCouponRow();
    assertThat(row.get("status")).isEqualTo("AVAILABLE");
    assertThat(row.get("used_order_id")).isNull();
    assertThat(row.get("used_order_amount")).isNull();
    assertThat(row.get("discount_applied_amount")).isNull();
    assertThat(row.get("used_at")).isNull();

    // 최소 주문금액(5000) 조건은 그대로 유지된다
    assertThatThrownBy(() -> userCouponService.calculateDiscount(userId, userCouponId, BigDecimal.valueOf(4000)))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET);
    assertThat(userCouponService.calculateDiscount(userId, userCouponId, ORDER_AMOUNT))
        .isEqualByComparingTo(DISCOUNT);

    tx.executeWithoutResult(s ->
        userCouponService.redeem(userId, userCouponId, 900_301L, ORDER_AMOUNT, DISCOUNT));
    assertThat(((Number) userCouponRow().get("used_order_id")).longValue()).isEqualTo(900_301L);
  }

  @Test
  void 같은_주문으로_중복_복구를_요청하면_한번만_복구된다() throws Exception {
    long orderId = 900_400L;
    tx.executeWithoutResult(s ->
        userCouponService.redeem(userId, userCouponId, orderId, ORDER_AMOUNT, DISCOUNT));

    int threads = 5;
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Integer>> futures = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      futures.add(executor.submit(() -> {
        start.await();
        return tx.execute(s -> userCouponService.releaseByOrder(orderId));
      }));
    }
    start.countDown();

    int total = 0;
    for (Future<Integer> f : futures) {
      total += f.get(15, TimeUnit.SECONDS);
    }

    assertThat(total).isEqualTo(1);
    assertThat(userCouponRow().get("status")).isEqualTo("AVAILABLE");
  }

  private Map<String, Object> userCouponRow() {
    return jdbcTemplate.queryForMap(
        "SELECT status, used_order_id, used_order_amount, discount_applied_amount, used_at "
            + "FROM user_coupons WHERE user_coupon_id = ?", userCouponId);
  }
}
