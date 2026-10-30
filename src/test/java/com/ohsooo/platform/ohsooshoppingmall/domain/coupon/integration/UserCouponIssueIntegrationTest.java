package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.integration;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.hibernate.exception.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 실제 PostgreSQL을 사용하는 쿠폰 발급 통합 테스트.
 *
 * <p>테스트 전용 DB에서만 실행한다. 환경변수 COUPON_IT_DB_URL이 없으면 건너뛴다.
 * 예) COUPON_IT_DB_URL=jdbc:postgresql://localhost:5432/ohsoo_shoppingmall_coupon_it
 * 계정은 application.yml의 datasource 설정을 그대로 사용한다.
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
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "COUPON_IT_DB_URL", matches = ".+")
class UserCouponIssueIntegrationTest {

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

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserCouponService userCouponService;

  @Autowired
  private CouponRepository couponRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JwtProvider jwtProvider;

  private final ExecutorService executor = Executors.newFixedThreadPool(10);

  private Long userId;
  private Long couponId;

  @BeforeEach
  void setUp() {
    userId = jdbcTemplate.queryForObject(
        "SELECT user_id FROM users WHERE is_deleted = false ORDER BY user_id LIMIT 1", Long.class);
    OffsetDateTime now = OffsetDateTime.now();
    Coupon coupon = Coupon.create(
        "통합테스트쿠폰", DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(1000), null, null,
        now.minusDays(1), now.plusDays(1), now.minusDays(1), now.plusDays(1));
    couponId = couponRepository.save(coupon).getCouponId();
  }

  @AfterEach
  void tearDown() {
    executor.shutdownNow();
    jdbcTemplate.update("DELETE FROM user_coupons WHERE coupon_id = ?", couponId);
    jdbcTemplate.update("DELETE FROM coupons WHERE coupon_id = ?", couponId);
  }

  @Test
  void save_시점의_INSERT에서_UNIQUE_위반이_발생하고_uq_user_coupon으로_식별된다() throws Exception {
    Throwable thrown = issueWhileConflictingRowIsUncommitted(() -> {
      try {
        userCouponService.issueCoupon(userId, couponId);
        return null;
      } catch (Throwable t) {
        return t;
      }
    });

    // 커밋 시점에 터졌다면 try-catch 밖이라 DataIntegrityViolationException이 그대로 나온다.
    // BusinessException이라는 것은 save() 호출 안에서 위반이 발생해 잡혔다는 뜻이다.
    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode()).isEqualTo(CouponErrorCode.COUPON_ALREADY_ISSUED);
    assertThat(thrown.getCause()).isInstanceOf(DataIntegrityViolationException.class);

    PSQLException psqlException = findCause(thrown, PSQLException.class);
    assertThat((Throwable) psqlException).isNotNull();
    assertThat(psqlException.getSQLState()).isEqualTo("23505");
    assertThat(psqlException.getServerErrorMessage()).isNotNull();
    assertThat(psqlException.getServerErrorMessage().getConstraint()).isEqualTo("uq_user_coupon");
    System.out.println("[IT] server message = " + psqlException.getServerErrorMessage().getMessage()
        + ", hibernate constraintName = "
        + findCause(thrown, ConstraintViolationException.class).getConstraintName());

    assertThat(countUserCoupons()).isEqualTo(1);
  }

  @Test
  void DB_UNIQUE_위반으로_실패한_발급_요청은_409와_COUPON_4091을_반환한다() throws Exception {
    MvcResult result = issueWhileConflictingRowIsUncommitted(
        () -> mockMvc.perform(issueRequest()).andReturn());

    assertThat(result.getResponse().getStatus()).isEqualTo(409);
    assertThat(errorCode(result)).isEqualTo("COUPON_4091");
    assertThat(countUserCoupons()).isEqualTo(1);
  }

  @Test
  void 순차적인_중복_발급_요청은_두번째가_409로_거부된다() throws Exception {
    MvcResult first = mockMvc.perform(issueRequest()).andReturn();
    MvcResult second = mockMvc.perform(issueRequest()).andReturn();

    assertThat(first.getResponse().getStatus()).isEqualTo(200);
    assertThat(second.getResponse().getStatus()).isEqualTo(409);
    assertThat(errorCode(second)).isEqualTo("COUPON_4091");
    assertThat(countUserCoupons()).isEqualTo(1);
  }

  @Test
  void 동시_발급_요청은_한건만_성공하고_나머지는_409를_반환한다() throws Exception {
    int threads = 10;
    CountDownLatch ready = new CountDownLatch(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<MvcResult>> futures = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      futures.add(executor.submit(() -> {
        ready.countDown();
        start.await();
        return mockMvc.perform(issueRequest()).andReturn();
      }));
    }
    ready.await(5, TimeUnit.SECONDS);
    start.countDown();

    int ok = 0;
    int conflict = 0;
    List<String> responses = new ArrayList<>();
    for (Future<MvcResult> f : futures) {
      MvcResult r = f.get(10, TimeUnit.SECONDS);
      int status = r.getResponse().getStatus();
      responses.add(status == 200 ? "200" : status + ":" + errorCode(r));
      if (status == 200) {
        ok++;
      } else if (status == 409 && "COUPON_4091".equals(errorCode(r))) {
        conflict++;
      }
    }
    System.out.println("[IT] concurrent responses = " + responses
        + ", rows = " + countUserCoupons());

    assertThat(ok).isEqualTo(1);
    assertThat(conflict).isEqualTo(threads - 1);
    assertThat(countUserCoupons()).isEqualTo(1);
  }

  /**
   * 동시 발급 경합을 결정적으로 재현한다.
   * 1) 별도 커넥션에서 같은 (user, coupon) 행을 INSERT하고 커밋하지 않는다.
   * 2) 발급 요청은 exists 검사(MVCC로 미커밋 행이 안 보임)를 통과하고, INSERT에서 UNIQUE 인덱스 락을 기다린다.
   * 3) 발급 요청이 실제로 락 대기 중인 것을 확인한 뒤 커밋하면, 대기하던 INSERT가 23505로 실패한다.
   */
  private <T> T issueWhileConflictingRowIsUncommitted(Callable<T> issueCall) throws Exception {
    try (Connection blocker = dataSource.getConnection()) {
      blocker.setAutoCommit(false);
      try (PreparedStatement ps = blocker.prepareStatement(
          "INSERT INTO user_coupons (user_id, coupon_id, status, issued_at) VALUES (?, ?, 'AVAILABLE', now())")) {
        ps.setLong(1, userId);
        ps.setLong(2, couponId);
        ps.executeUpdate();
      }

      Future<T> future = executor.submit(issueCall);
      awaitInsertBlockedOnLock();
      blocker.commit();
      return future.get(10, TimeUnit.SECONDS);
    }
  }

  private void awaitInsertBlockedOnLock() throws InterruptedException {
    long deadline = System.currentTimeMillis() + 5_000;
    while (System.currentTimeMillis() < deadline) {
      Integer waiting = jdbcTemplate.queryForObject("""
          SELECT count(*) FROM pg_stat_activity
           WHERE datname = current_database()
             AND wait_event_type = 'Lock'
             AND query ILIKE 'insert into user_coupons%'
          """, Integer.class);
      if (waiting != null && waiting > 0) {
        return;
      }
      Thread.sleep(50);
    }
    throw new AssertionError("발급 요청의 INSERT가 UNIQUE 락 대기 상태에 도달하지 않았습니다.");
  }

  private org.springframework.test.web.servlet.RequestBuilder issueRequest() {
    String token = jwtProvider.createAccessToken(userId, Map.of("role", "GENERAL"));
    return post("/coupons/{couponId}/issue", couponId)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
  }

  private String errorCode(MvcResult result) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), "$.errorCode");
  }

  private int countUserCoupons() {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM user_coupons WHERE user_id = ? AND coupon_id = ?", Integer.class, userId, couponId);
    return count == null ? 0 : count;
  }

  private static <T extends Throwable> T findCause(Throwable t, Class<T> type) {
    for (Throwable c = t; c != null; c = c.getCause()) {
      if (type.isInstance(c)) {
        return type.cast(c);
      }
    }
    return null;
  }
}
