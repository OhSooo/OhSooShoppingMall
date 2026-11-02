package com.ohsooo.platform.ohsooshoppingmall.domain.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.Coupon;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.DiscountType;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.exception.CouponErrorCode;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.repository.CouponRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.service.UserCouponService;
import com.ohsooo.platform.ohsooshoppingmall.global.exception.BusinessException;
import com.ohsooo.platform.ohsooshoppingmall.global.jwt.JwtProvider;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 동일 사용자가 동일 쿠폰으로 POST /orders를 동시에 보낼 때의 정합성을 실제 PostgreSQL에서 검증한다.
 *
 * <p>다른 쿠폰 통합 테스트와 같이 환경변수 COUPON_IT_DB_URL이 있을 때만 테스트 전용 DB에서 실행한다.
 * 요청은 테스트 트랜잭션 없이 MockMvc(보안 필터 → 컨트롤러 → 예외 핸들러)로 스레드마다 병렬 실행되므로,
 * 각 요청은 OrderService.createOrder()의 @Transactional 프록시에서 자기 트랜잭션을 연다.
 * DIRECT 주문을 사용해 Redis(장바구니) 없이 검증한다.
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
    "spring.jpa.properties.hibernate.session_factory.statement_inspector="
        + "com.ohsooo.platform.ohsooshoppingmall.domain.order.integration.OrderCouponConcurrencyIntegrationTest$SqlCapture",
    "logging.level.org.springframework.security=INFO"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "COUPON_IT_DB_URL", matches = ".+")
class OrderCouponConcurrencyIntegrationTest {

  private static final String MAIN_DB_NAME = "ohsoo_shoppingmall";
  private static final int REQUESTS = 10;
  private static final String ALREADY_USED_CODE = CouponErrorCode.USER_COUPON_ALREADY_USED.getCode();

  @DynamicPropertySource
  static void testDatabase(DynamicPropertyRegistry registry) {
    String url = System.getenv("COUPON_IT_DB_URL");
    // 운영(로컬 기본) DB에서 실수로 실행되는 것을 막는다.
    if (url.replaceAll("\\?.*$", "").endsWith("/" + MAIN_DB_NAME)) {
      throw new IllegalStateException("통합 테스트는 기본 DB(" + MAIN_DB_NAME + ")에서 실행할 수 없습니다: " + url);
    }
    registry.add("spring.datasource.url", () -> url);
  }

  /** 실행된 SQL을 그대로 수집한다. (Hibernate가 클래스 이름으로 생성) */
  public static class SqlCapture implements StatementInspector {

    static final List<String> SQL = new CopyOnWriteArrayList<>();

    @Override
    public String inspect(String sql) {
      SQL.add(sql);
      return sql;
    }
  }

  private record Outcome(int status, String errorCode, Long orderId) {

    boolean succeeded() {
      return status == 200 && orderId != null;
    }
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private UserCouponService userCouponService;
  @Autowired private CouponRepository couponRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private JwtProvider jwtProvider;
  @Autowired private PlatformTransactionManager transactionManager;

  @Value("${spring.datasource.username}") private String dbUsername;
  @Value("${spring.datasource.password}") private String dbPassword;

  private final ExecutorService executor = Executors.newFixedThreadPool(REQUESTS);
  private final List<Long> couponIds = new ArrayList<>();

  private Long userId;
  private Long variantId;
  private BigDecimal unitPrice;
  private int originalStock;
  private String originalVariantStatus;
  private long maxOrderIdBefore;
  private int peakLockWaiters;

  @BeforeEach
  void setUp() {
    userId = jdbcTemplate.queryForObject(
        "SELECT user_id FROM users WHERE is_deleted = false ORDER BY user_id LIMIT 1", Long.class);
    Map<String, Object> variant = jdbcTemplate.queryForMap("""
        SELECT v.item_variant_id, v.price, v.status, i.quantity
          FROM item_variants v JOIN inventories i ON i.item_variant_id = v.item_variant_id
         WHERE v.status = 'ACTIVE' AND i.quantity >= 20 AND v.price > 1000
         ORDER BY v.item_variant_id LIMIT 1
        """);
    variantId = ((Number) variant.get("item_variant_id")).longValue();
    unitPrice = (BigDecimal) variant.get("price");
    originalStock = ((Number) variant.get("quantity")).intValue();
    originalVariantStatus = (String) variant.get("status");
    maxOrderIdBefore = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(order_id), 0) FROM orders", Long.class);
    SqlCapture.SQL.clear();
  }

  @AfterEach
  void tearDown() {
    executor.shutdownNow();
    List<Long> orderIds = jdbcTemplate.queryForList(
        "SELECT order_id FROM orders WHERE order_id > ?", Long.class, maxOrderIdBefore);
    for (Long orderId : orderIds) {
      jdbcTemplate.update("DELETE FROM payments WHERE order_id = ?", orderId);
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

  @Test
  void 같은_쿠폰으로_유료_주문을_동시에_10건_요청하면_한건만_성공하고_나머지는_중복사용으로_롤백된다() throws Exception {
    Long userCouponId = issueFixedCoupon(BigDecimal.valueOf(1000));

    List<Outcome> outcomes = requestConcurrently(repeat(orderBody(userCouponId)));
    report("유료 주문", outcomes, userCouponId);

    assertExactlyOneWinnerAndNineDuplicates(outcomes);
    Long winnerOrderId = winner(outcomes).orderId();

    // 성공한 주문 1건만 남고, 실패한 요청의 주문/주문상품은 롤백된다
    assertThat(createdOrderIds()).containsExactly(winnerOrderId);
    assertThat(countOrderItems()).isEqualTo(1);
    assertThat(countPayments()).isZero();
    assertThat(stock()).isEqualTo(originalStock);

    Map<String, Object> order = jdbcTemplate.queryForMap(
        "SELECT status, discount_amount, final_price, user_coupon_id FROM orders WHERE order_id = ?", winnerOrderId);
    assertThat(order.get("status")).isEqualTo("CREATED");
    assertThat((BigDecimal) order.get("discount_amount")).isEqualByComparingTo("1000");
    assertThat((BigDecimal) order.get("final_price")).isEqualByComparingTo(unitPrice.subtract(BigDecimal.valueOf(1000)));
    assertThat(((Number) order.get("user_coupon_id")).longValue()).isEqualTo(userCouponId);

    Map<String, Object> coupon = userCouponRow(userCouponId);
    assertThat(coupon.get("status")).isEqualTo("USED");
    assertThat(((Number) coupon.get("used_order_id")).longValue()).isEqualTo(winnerOrderId);
    assertThat((BigDecimal) coupon.get("discount_applied_amount")).isEqualByComparingTo("1000");
    assertThat((BigDecimal) coupon.get("used_order_amount")).isEqualByComparingTo(unitPrice);
    assertThat(coupon.get("used_at")).isNotNull();
  }

  @Test
  void 같은_쿠폰으로_0원_주문을_동시에_10건_요청하면_결제행과_재고차감도_한건만_반영된다() throws Exception {
    Long userCouponId = issueFixedCoupon(unitPrice);

    List<Outcome> outcomes = requestConcurrently(repeat(orderBody(userCouponId)));
    report("0원 주문", outcomes, userCouponId);

    assertExactlyOneWinnerAndNineDuplicates(outcomes);
    Long winnerOrderId = winner(outcomes).orderId();

    assertThat(createdOrderIds()).containsExactly(winnerOrderId);
    assertThat(countOrderItems()).isEqualTo(1);
    assertThat(countPayments()).isEqualTo(1);
    assertThat(stock()).isEqualTo(originalStock - 1);

    Map<String, Object> coupon = userCouponRow(userCouponId);
    assertThat(coupon.get("status")).isEqualTo("USED");
    assertThat(((Number) coupon.get("used_order_id")).longValue()).isEqualTo(winnerOrderId);
  }

  /** 대조군: 요청 형식/재고가 문제였다면 쿠폰이 서로 달라도 실패해야 한다. */
  @Test
  void 서로_다른_쿠폰으로_동시에_10건_요청하면_모두_성공한다() throws Exception {
    List<Long> userCouponIds = new ArrayList<>();
    List<String> bodies = new ArrayList<>();
    for (int i = 0; i < REQUESTS; i++) {
      Long userCouponId = issueFixedCoupon(BigDecimal.valueOf(1000));
      userCouponIds.add(userCouponId);
      bodies.add(orderBody(userCouponId));
    }

    List<Outcome> outcomes = requestConcurrently(bodies);
    System.out.println("[IT] 대조군 outcomes = " + outcomes);

    assertThat(outcomes).allMatch(Outcome::succeeded);
    assertThat(createdOrderIds()).hasSize(REQUESTS);
    for (Long userCouponId : userCouponIds) {
      assertThat(userCouponRow(userCouponId).get("status")).isEqualTo("USED");
    }
  }

  /**
   * createOrder()와 같은 순서(calculateDiscount → redeem)를 타이밍 의존 없이 재현한다.
   * 락 없이 쿠폰을 읽은 뒤, 락을 얻기 전에 다른 주문이 먼저 커밋한 경우에도 redeem은 거부되어야 한다.
   */
  @Test
  void 락_없이_읽은_뒤_다른_주문이_먼저_커밋하면_redeem은_중복사용으로_거부된다() {
    Long userCouponId = issueFixedCoupon(BigDecimal.valueOf(1000));
    BigDecimal orderAmount = BigDecimal.valueOf(10000);
    BigDecimal discount = BigDecimal.valueOf(1000);
    long firstOrderId = 900_001L;
    long secondOrderId = 900_002L;
    TransactionTemplate tx = new TransactionTemplate(transactionManager);

    assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
      userCouponService.calculateDiscount(userId, userCouponId, orderAmount);
      commitRedeemInAnotherTransaction(tx, userCouponId, firstOrderId, orderAmount, discount);
      userCouponService.redeem(userId, userCouponId, secondOrderId, orderAmount, discount);
    }))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(CouponErrorCode.USER_COUPON_ALREADY_USED);

    assertThat(((Number) userCouponRow(userCouponId).get("used_order_id")).longValue()).isEqualTo(firstOrderId);
  }

  private void commitRedeemInAnotherTransaction(
      TransactionTemplate tx, Long userCouponId, long orderId, BigDecimal orderAmount, BigDecimal discount
  ) {
    try {
      executor.submit(() -> tx.executeWithoutResult(s ->
          userCouponService.redeem(userId, userCouponId, orderId, orderAmount, discount))
      ).get(15, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private List<String> repeat(String body) {
    List<String> bodies = new ArrayList<>();
    for (int i = 0; i < REQUESTS; i++) {
      bodies.add(body);
    }
    return bodies;
  }

  /** 모든 스레드를 출발선에 세운 뒤 동시에 POST /orders를 보내고, 그동안 DB의 락 대기 세션 수를 표본 조사한다. */
  private List<Outcome> requestConcurrently(List<String> bodies) throws Exception {
    String token = jwtProvider.createAccessToken(userId, Map.of("role", "GENERAL"));
    CountDownLatch ready = new CountDownLatch(bodies.size());
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Outcome>> futures = new ArrayList<>();
    for (String body : bodies) {
      futures.add(executor.submit(() -> {
        ready.countDown();
        start.await();
        return postOrder(token, body);
      }));
    }
    assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

    AtomicBoolean sampling = new AtomicBoolean(true);
    AtomicInteger peak = new AtomicInteger();
    Thread sampler = new Thread(() -> sampleLockWaiters(sampling, peak));
    sampler.start();
    start.countDown();

    List<Outcome> outcomes = new ArrayList<>();
    try {
      for (Future<Outcome> future : futures) {
        outcomes.add(future.get(60, TimeUnit.SECONDS));
      }
    } finally {
      sampling.set(false);
      sampler.join(5000);
    }
    peakLockWaiters = peak.get();
    return outcomes;
  }

  private Outcome postOrder(String token, String body) throws Exception {
    MvcResult result = mockMvc.perform(post("/orders")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andReturn();
    int status = result.getResponse().getStatus();
    String json = result.getResponse().getContentAsString();
    if (status == 200) {
      Number orderId = JsonPath.read(json, "$.data.orderId");
      return new Outcome(status, null, orderId.longValue());
    }
    return new Outcome(status, JsonPath.read(json, "$.errorCode"), null);
  }

  // Hikari 풀을 차지하면 요청 동시성이 달라지므로 별도 연결로 조사한다.
  private void sampleLockWaiters(AtomicBoolean sampling, AtomicInteger peak) {
    String sql = "SELECT count(*) FROM pg_stat_activity "
        + "WHERE datname = current_database() AND wait_event_type = 'Lock'";
    try (Connection connection = DriverManager.getConnection(System.getenv("COUPON_IT_DB_URL"), dbUsername, dbPassword);
        Statement statement = connection.createStatement()) {
      while (sampling.get()) {
        try (ResultSet rs = statement.executeQuery(sql)) {
          rs.next();
          peak.accumulateAndGet(rs.getInt(1), Math::max);
        }
      }
    } catch (SQLException e) {
      System.out.println("[IT] lock sampler failed: " + e.getMessage());
    }
  }

  private void assertExactlyOneWinnerAndNineDuplicates(List<Outcome> outcomes) {
    assertThat(outcomes.stream().filter(Outcome::succeeded).toList()).hasSize(1);
    // 실패 원인은 재고/요청 오류가 아니라 전부 쿠폰 중복 사용(409 COUPON_4092)이어야 한다
    assertThat(outcomes.stream().filter(o -> !o.succeeded()).toList())
        .hasSize(REQUESTS - 1)
        .allMatch(o -> o.status() == 409 && ALREADY_USED_CODE.equals(o.errorCode()));
  }

  private Outcome winner(List<Outcome> outcomes) {
    return outcomes.stream().filter(Outcome::succeeded).findFirst().orElseThrow();
  }

  private void report(String scenario, List<Outcome> outcomes, Long userCouponId) {
    List<String> lockSql = SqlCapture.SQL.stream()
        .filter(sql -> sql.contains("user_coupons") && sql.contains(" for "))
        .toList();
    System.out.println("[IT] " + scenario + " outcomes = " + outcomes);
    System.out.println("[IT] " + scenario + " 쿠폰 락 쿼리 실행 수 = " + lockSql.size()
        + ", 락 대기 세션 최대값 = " + peakLockWaiters);
    if (!lockSql.isEmpty()) {
      System.out.println("[IT] 쿠폰 락 SQL = " + lockSql.get(0));
    }
    System.out.println("[IT] " + scenario + " DB: orders=" + createdOrderIds() + ", order_items=" + countOrderItems()
        + ", payments=" + countPayments() + ", stock=" + stock() + "/" + originalStock
        + ", coupon=" + userCouponRow(userCouponId));
  }

  /** 정액 쿠폰을 만들어 사용자에게 발급하고 userCouponId를 반환한다. */
  private Long issueFixedCoupon(BigDecimal discountValue) {
    OffsetDateTime now = OffsetDateTime.now();
    Coupon coupon = Coupon.create(
        "주문동시성테스트쿠폰", DiscountType.FIXED_AMOUNT, discountValue, null, null,
        now.minusDays(1), now.plusDays(1), now.minusDays(1), now.plusDays(1));
    Long couponId = couponRepository.save(coupon).getCouponId();
    couponIds.add(couponId);
    return userCouponService.issueCoupon(userId, couponId).getUserCouponId();
  }

  private String orderBody(Long userCouponId) {
    return """
        {
          "source": "DIRECT",
          "items": [{ "itemVariantId": %d, "quantity": 1 }],
          "shipping": {
            "receiverName": "동시성테스트",
            "receiverPhone": "010-0000-0000",
            "shippingAddress": "서울시"
          },
          "userCouponId": %d
        }
        """.formatted(variantId, userCouponId);
  }

  private Map<String, Object> userCouponRow(Long userCouponId) {
    return jdbcTemplate.queryForMap(
        "SELECT status, used_order_id, used_order_amount, discount_applied_amount, used_at "
            + "FROM user_coupons WHERE user_coupon_id = ?", userCouponId);
  }

  private List<Long> createdOrderIds() {
    return jdbcTemplate.queryForList(
        "SELECT order_id FROM orders WHERE order_id > ? ORDER BY order_id", Long.class, maxOrderIdBefore);
  }

  private int countOrderItems() {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM order_items WHERE order_id > ?", Integer.class, maxOrderIdBefore);
  }

  private int countPayments() {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM payments WHERE order_id > ?", Integer.class, maxOrderIdBefore);
  }

  private int stock() {
    return jdbcTemplate.queryForObject(
        "SELECT quantity FROM inventories WHERE item_variant_id = ?", Integer.class, variantId);
  }
}
