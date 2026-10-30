package com.ohsooo.platform.ohsooshoppingmall.domain.coupon.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.request.UserCouponSearchRequestDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.UserCouponPageResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.dto.response.UserCouponResponseDto;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.Coupon;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.DiscountType;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.UserCoupon;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.entity.UserCouponStatus;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.mapper.CouponMapper;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.repository.CouponRepository;
import com.ohsooo.platform.ohsooshoppingmall.domain.coupon.service.UserCouponService;
import com.ohsooo.platform.ohsooshoppingmall.global.response.BaseResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import org.hibernate.SessionFactory;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 실제 PostgreSQL을 사용하는 내 쿠폰 목록(페이지) 조회 통합 테스트.
 *
 * <p>다른 쿠폰 통합 테스트와 같이 환경변수 COUPON_IT_DB_URL이 있을 때만 테스트 전용 DB에서 실행한다.
 * 정렬/필터/사용자 격리 검증을 위해 테스트가 직접 만든 사용자·쿠폰·발급 행만 사용하고, 끝나면 그 행만 지운다.
 * SQL 실행 횟수는 Hibernate Statistics로, 실제 SQL은 StatementInspector로 수집한다.
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
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "spring.jpa.properties.hibernate.session_factory.statement_inspector="
        + "com.ohsooo.platform.ohsooshoppingmall.domain.coupon.integration.UserCouponListIntegrationTest$SqlCapture",
    "logging.level.org.springframework.security=INFO",
    "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN"
})
@EnabledIfEnvironmentVariable(named = "COUPON_IT_DB_URL", matches = ".+")
class UserCouponListIntegrationTest {

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

  /** 실행된 SQL을 그대로 수집한다. (Hibernate가 클래스 이름으로 생성) */
  public static class SqlCapture implements StatementInspector {

    static final List<String> SQL = new CopyOnWriteArrayList<>();

    @Override
    public String inspect(String sql) {
      SQL.add(sql);
      return sql;
    }
  }

  @Autowired
  private UserCouponService userCouponService;

  @Autowired
  private CouponRepository couponRepository;

  @Autowired
  private CouponMapper couponMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private ObjectMapper objectMapper;

  @PersistenceContext
  private EntityManager entityManager;

  private Statistics statistics;
  private OffsetDateTime now;
  private final List<Long> createdUserIds = new ArrayList<>();
  private final List<Long> createdCouponIds = new ArrayList<>();

  @BeforeEach
  void setUp() {
    statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    now = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
  }

  @AfterEach
  void tearDown() {
    // 이 테스트가 만든 행만 정리한다.
    for (Long couponId : createdCouponIds) {
      jdbcTemplate.update("DELETE FROM user_coupons WHERE coupon_id = ?", couponId);
    }
    for (Long userId : createdUserIds) {
      jdbcTemplate.update("DELETE FROM user_coupons WHERE user_id = ?", userId);
    }
    for (Long couponId : createdCouponIds) {
      jdbcTemplate.update("DELETE FROM coupons WHERE coupon_id = ?", couponId);
    }
    for (Long userId : createdUserIds) {
      jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", userId);
    }
  }

  // ---------- 데이터 준비 ----------

  private Long newUser() {
    Long userId = jdbcTemplate.queryForObject("INSERT INTO users DEFAULT VALUES RETURNING user_id", Long.class);
    createdUserIds.add(userId);
    return userId;
  }

  private Long newCoupon(String name, OffsetDateTime useStartAt, OffsetDateTime useEndAt) {
    Coupon coupon = Coupon.create(
        name, DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(1000), BigDecimal.valueOf(5000), null,
        now.minusDays(30), now.plusDays(30), useStartAt, useEndAt);
    Long couponId = couponRepository.save(coupon).getCouponId();
    createdCouponIds.add(couponId);
    return couponId;
  }

  private Long usableCoupon(String name) {
    return newCoupon(name, now.minusDays(1), now.plusDays(1));
  }

  private Long notYetUsableCoupon(String name) {
    return newCoupon(name, now.plusDays(1), now.plusDays(2));
  }

  private Long expiredCoupon(String name) {
    return newCoupon(name, now.minusDays(3), now.minusDays(1));
  }

  /** 발급일시와 저장 상태를 직접 지정해 발급 행을 만든다. */
  private Long grant(Long userId, Long couponId, OffsetDateTime issuedAt, UserCouponStatus status) {
    boolean used = status == UserCouponStatus.USED;
    return jdbcTemplate.queryForObject(
        "INSERT INTO user_coupons (user_id, coupon_id, status, issued_at, used_at, used_order_amount, "
            + "discount_applied_amount, used_order_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING user_coupon_id",
        Long.class,
        userId, couponId, status.name(), issuedAt,
        used ? issuedAt.plusMinutes(1) : null,
        used ? BigDecimal.valueOf(10000) : null,
        used ? BigDecimal.valueOf(1000) : null,
        used ? 990_000L : null);
  }

  /** 서로 다른 발급일시로 count개를 발급하고, 기대 정렬 순서(발급일시 최신순)대로 ID를 반환한다. */
  private List<Long> grantUsableCoupons(Long userId, int count) {
    List<Long> ids = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      ids.add(grant(userId, usableCoupon("목록" + i), now.minusMinutes(count - i), UserCouponStatus.AVAILABLE));
    }
    Collections.reverse(ids);
    return ids;
  }

  private UserCouponPageResponseDto list(Long userId, int page, int size, UserCouponStatus status) {
    UserCouponSearchRequestDto condition = new UserCouponSearchRequestDto();
    condition.setPage(page);
    condition.setSize(size);
    condition.setStatus(status);
    return userCouponService.getMyCoupons(userId, condition);
  }

  private List<Long> ids(UserCouponPageResponseDto response) {
    return response.getCoupons().stream().map(UserCouponResponseDto::getUserCouponId).toList();
  }

  private List<Long> allPagesIds(Long userId, int size, UserCouponStatus status) {
    List<Long> all = new ArrayList<>();
    UserCouponPageResponseDto page;
    int pageNo = 0;
    do {
      page = list(userId, pageNo++, size, status);
      all.addAll(ids(page));
    } while (!page.getPageInfo().isLast());
    return all;
  }

  private long countStatements(Runnable action) {
    statistics.clear();
    action.run();
    return statistics.getPrepareStatementCount();
  }

  private <T> T captureSql(Supplier<T> action, List<String> out) {
    SqlCapture.SQL.clear();
    T result = action.get();
    out.addAll(SqlCapture.SQL);
    return result;
  }

  // 개선 전 조회 방식 재현: coupon fetch 없이 목록을 읽고 Mapper가 LAZY coupon에 접근한다.
  private List<UserCouponResponseDto> legacyGetMyCoupons(Long userId) {
    TransactionTemplate readOnlyTx = new TransactionTemplate(transactionManager);
    readOnlyTx.setReadOnly(true);
    return readOnlyTx.execute(s -> entityManager
        .createQuery("SELECT uc FROM UserCoupon uc WHERE uc.user.userId = :userId ORDER BY uc.issuedAt DESC",
            UserCoupon.class)
        .setParameter("userId", userId)
        .getResultList().stream()
        .map(uc -> couponMapper.toUserCouponResponseDto(uc, now))
        .toList());
  }

  // ---------- N+1 / 쿼리 ----------

  @Test
  void 개선전_조회는_건수만큼_쿠폰_SQL이_늘고_개선후에는_건수와_무관하게_일정하다() {
    List<String> report = new ArrayList<>();
    for (int n : new int[]{1, 10, 30}) {
      Long userId = newUser();
      grantUsableCoupons(userId, n);

      long before = countStatements(() -> assertThat(legacyGetMyCoupons(userId)).hasSize(n));
      // size(100) > n 이므로 첫 페이지에서 전체 건수가 확정되어 count 쿼리가 생략된다.
      long afterSinglePage = countStatements(() -> assertThat(list(userId, 0, 100, null).getCoupons()).hasSize(n));
      // size(5) < n 이면 목록 + count 2회.
      long afterPaged = countStatements(() -> list(userId, 0, 5, null));

      report.add("N=" + n + " before=" + before + " after(size=100)=" + afterSinglePage
          + " after(size=5)=" + afterPaged);
      assertThat(before).isEqualTo(1 + n);
      assertThat(afterSinglePage).isEqualTo(1);
      assertThat(afterPaged).isEqualTo(n > 5 ? 2 : 1);
    }
    System.out.println("[IT] my-coupons SQL statements " + report);
  }

  @Test
  void 목록조회는_coupon을_조인해_한번에_읽고_LIMIT_OFFSET으로_DB에서_페이지를_자른다() {
    Long userId = newUser();
    grantUsableCoupons(userId, 45);
    List<String> sql = new ArrayList<>();

    statistics.clear();
    UserCouponPageResponseDto page = captureSql(() -> list(userId, 1, 20, null), sql);

    assertThat(page.getCoupons()).hasSize(20);
    // 메모리 페이지네이션이었다면 45건 전체가 엔티티로 로딩된다.
    assertThat(statistics.getEntityStatistics(UserCoupon.class.getName()).getLoadCount()).isEqualTo(20);
    assertThat(statistics.getEntityStatistics(Coupon.class.getName()).getFetchCount()).isZero();
    assertThat(sql).hasSize(2);
    String select = sql.get(0).toLowerCase();
    assertThat(select).contains("join coupons").contains("offset").contains("fetch first");
    assertThat(select).contains("order by uc1_0.issued_at desc,uc1_0.user_coupon_id desc");
    assertThat(sql.get(1).toLowerCase()).startsWith("select count(");
    System.out.println("[IT] my-coupons page SQL = " + sql);
  }

  // ---------- 페이지네이션 ----------

  @Test
  void 쿠폰이_없으면_빈_목록과_0건_페이지정보를_반환한다() {
    Long userId = newUser();

    UserCouponPageResponseDto response = list(userId, 0, 20, null);

    assertThat(response.getCoupons()).isEmpty();
    assertThat(response.getPageInfo().getTotalElements()).isZero();
    assertThat(response.getPageInfo().getTotalPages()).isZero();
    assertThat(response.getPageInfo().isLast()).isTrue();
  }

  @Test
  void 쿠폰이_한건이면_한페이지로_반환한다() {
    Long userId = newUser();
    List<Long> expected = grantUsableCoupons(userId, 1);

    UserCouponPageResponseDto response = list(userId, 0, 20, null);

    assertThat(ids(response)).isEqualTo(expected);
    assertThat(response.getPageInfo().getTotalElements()).isEqualTo(1);
    assertThat(response.getPageInfo().getTotalPages()).isEqualTo(1);
    assertThat(response.getPageInfo().isLast()).isTrue();
  }

  @Test
  void 여러건은_첫_중간_마지막_범위밖_페이지로_정확히_나뉜다() {
    Long userId = newUser();
    List<Long> expected = grantUsableCoupons(userId, 45);

    UserCouponPageResponseDto first = list(userId, 0, 20, null);
    UserCouponPageResponseDto middle = list(userId, 1, 20, null);
    UserCouponPageResponseDto last = list(userId, 2, 20, null);
    UserCouponPageResponseDto outOfRange = list(userId, 3, 20, null);

    assertThat(ids(first)).isEqualTo(expected.subList(0, 20));
    assertThat(ids(middle)).isEqualTo(expected.subList(20, 40));
    assertThat(ids(last)).isEqualTo(expected.subList(40, 45));
    assertThat(first.getPageInfo().isLast()).isFalse();
    assertThat(middle.getPageInfo().isLast()).isFalse();
    assertThat(last.getPageInfo().isLast()).isTrue();
    assertThat(last.getCoupons()).hasSizeLessThan(20);
    for (UserCouponPageResponseDto page : List.of(first, middle, last, outOfRange)) {
      assertThat(page.getPageInfo().getTotalElements()).isEqualTo(45);
      assertThat(page.getPageInfo().getTotalPages()).isEqualTo(3);
      assertThat(page.getPageInfo().getSize()).isEqualTo(20);
    }
    assertThat(outOfRange.getCoupons()).isEmpty();
    assertThat(outOfRange.getPageInfo().getPage()).isEqualTo(3);
    assertThat(outOfRange.getPageInfo().isLast()).isTrue();
  }

  @Test
  void 허용되는_최대_page와_최대_size로_조회해도_offset이_넘치지_않고_빈_페이지를_반환한다() {
    Long userId = newUser();
    grantUsableCoupons(userId, 2);

    UserCouponPageResponseDto response = list(
        userId, UserCouponSearchRequestDto.MAX_PAGE, UserCouponSearchRequestDto.MAX_SIZE, null);

    assertThat(response.getCoupons()).isEmpty();
    assertThat(response.getPageInfo().getTotalElements()).isEqualTo(2);
    assertThat(response.getPageInfo().isLast()).isTrue();
  }

  @Test
  void 발급일시가_같으면_ID_내림차순으로_정렬되어_페이지간_중복과_누락이_없다() {
    Long userId = newUser();
    OffsetDateTime sameIssuedAt = now.minusHours(1);
    List<Long> granted = new ArrayList<>();
    for (int i = 0; i < 25; i++) {
      granted.add(grant(userId, usableCoupon("동시발급" + i), sameIssuedAt, UserCouponStatus.AVAILABLE));
    }
    // 더 최근 발급 1건은 맨 앞
    Long newest = grant(userId, usableCoupon("최신"), now.minusMinutes(1), UserCouponStatus.AVAILABLE);

    List<Long> expected = new ArrayList<>();
    expected.add(newest);
    expected.addAll(granted.stream().sorted((a, b) -> Long.compare(b, a)).toList());

    for (int size : new int[]{1, 7, 10}) {
      List<Long> all = allPagesIds(userId, size, null);
      assertThat(all).as("size=%d", size).isEqualTo(expected);
      assertThat(new HashSet<>(all)).hasSize(26);
    }
    // 같은 페이지를 반복 조회해도 결과가 같다
    assertThat(ids(list(userId, 1, 10, null))).isEqualTo(ids(list(userId, 1, 10, null)));
  }

  @Test
  void 다른_사용자의_쿠폰은_목록과_전체건수에_포함되지_않는다() {
    Long me = newUser();
    Long other = newUser();
    List<Long> mine = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      Long couponId = usableCoupon("공유" + i);
      mine.add(grant(me, couponId, now.minusMinutes(10 + i), UserCouponStatus.AVAILABLE));
      grant(other, couponId, now.minusMinutes(i), UserCouponStatus.AVAILABLE);
    }
    grant(other, expiredCoupon("타인만료"), now, UserCouponStatus.AVAILABLE);
    grant(other, usableCoupon("타인사용"), now, UserCouponStatus.USED);

    for (UserCouponStatus status : new UserCouponStatus[]{null, UserCouponStatus.AVAILABLE,
        UserCouponStatus.USED, UserCouponStatus.EXPIRED}) {
      UserCouponPageResponseDto response = list(me, 0, 100, status);
      assertThat(mine).containsAll(ids(response));
    }
    UserCouponPageResponseDto all = list(me, 0, 2, null);
    assertThat(all.getPageInfo().getTotalElements()).isEqualTo(3);
    assertThat(list(me, 0, 100, UserCouponStatus.EXPIRED).getPageInfo().getTotalElements()).isZero();
    assertThat(list(me, 0, 100, UserCouponStatus.USED).getPageInfo().getTotalElements()).isZero();
  }

  // ---------- 상태 / 필터 ----------

  @Test
  void 상태필터는_조회시점_상태로_DB에서_걸러지고_전체건수와_일치한다() {
    Long userId = newUser();
    Set<Long> usable = new HashSet<>();
    Set<Long> notYet = new HashSet<>();
    Set<Long> expired = new HashSet<>();
    Set<Long> used = new HashSet<>();
    int t = 0;
    for (int i = 0; i < 3; i++) {
      usable.add(grant(userId, usableCoupon("사용가능" + i), now.minusMinutes(t++), UserCouponStatus.AVAILABLE));
    }
    for (int i = 0; i < 2; i++) {
      notYet.add(grant(userId, notYetUsableCoupon("시작전" + i), now.minusMinutes(t++), UserCouponStatus.AVAILABLE));
    }
    for (int i = 0; i < 2; i++) {
      expired.add(grant(userId, expiredCoupon("만료" + i), now.minusMinutes(t++), UserCouponStatus.AVAILABLE));
    }
    used.add(grant(userId, usableCoupon("사용함"), now.minusMinutes(t++), UserCouponStatus.USED));
    // 사용 후 기간이 지난 쿠폰은 USED로 본다 (effectiveStatus와 동일)
    used.add(grant(userId, expiredCoupon("사용후만료"), now.minusMinutes(t++), UserCouponStatus.USED));

    UserCouponPageResponseDto available = list(userId, 0, 100, UserCouponStatus.AVAILABLE);
    Set<Long> expectedAvailable = new HashSet<>(usable);
    expectedAvailable.addAll(notYet);
    assertThat(ids(available)).containsExactlyInAnyOrderElementsOf(expectedAvailable);
    assertThat(available.getPageInfo().getTotalElements()).isEqualTo(5);
    for (UserCouponResponseDto dto : available.getCoupons()) {
      assertThat(dto.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
      // 사용 시작 전 쿠폰은 AVAILABLE이지만 지금 사용할 수는 없다
      assertThat(dto.isUsableNow()).isEqualTo(usable.contains(dto.getUserCouponId()));
    }

    UserCouponPageResponseDto expiredPage = list(userId, 0, 100, UserCouponStatus.EXPIRED);
    assertThat(ids(expiredPage)).containsExactlyInAnyOrderElementsOf(expired);
    assertThat(expiredPage.getPageInfo().getTotalElements()).isEqualTo(2);
    assertThat(expiredPage.getCoupons()).allSatisfy(dto -> {
      assertThat(dto.getStatus()).isEqualTo(UserCouponStatus.EXPIRED);
      assertThat(dto.isUsableNow()).isFalse();
    });

    UserCouponPageResponseDto usedPage = list(userId, 0, 100, UserCouponStatus.USED);
    assertThat(ids(usedPage)).containsExactlyInAnyOrderElementsOf(used);
    assertThat(usedPage.getPageInfo().getTotalElements()).isEqualTo(2);
    assertThat(usedPage.getCoupons()).allSatisfy(dto -> {
      assertThat(dto.getStatus()).isEqualTo(UserCouponStatus.USED);
      assertThat(dto.isUsableNow()).isFalse();
      assertThat(dto.getUsedAt()).isNotNull();
      assertThat(dto.getDiscountAppliedAmount()).isEqualByComparingTo("1000");
    });

    UserCouponPageResponseDto all = list(userId, 0, 100, null);
    assertThat(all.getPageInfo().getTotalElements()).isEqualTo(9);
    assertThat(available.getPageInfo().getTotalElements() + expiredPage.getPageInfo().getTotalElements()
        + usedPage.getPageInfo().getTotalElements()).isEqualTo(all.getPageInfo().getTotalElements());
  }

  @Test
  void 필터와_페이지네이션을_함께_쓰면_필터결과_기준으로_페이지가_나뉜다() {
    Long userId = newUser();
    List<Long> availableInOrder = new ArrayList<>();
    // 최신순으로 AVAILABLE / EXPIRED / USED가 섞이도록 발급
    for (int i = 0; i < 12; i++) {
      OffsetDateTime issuedAt = now.minusMinutes(i);
      switch (i % 3) {
        case 0 -> availableInOrder.add(grant(userId, usableCoupon("필터가능" + i), issuedAt, UserCouponStatus.AVAILABLE));
        case 1 -> grant(userId, expiredCoupon("필터만료" + i), issuedAt, UserCouponStatus.AVAILABLE);
        default -> grant(userId, usableCoupon("필터사용" + i), issuedAt, UserCouponStatus.USED);
      }
    }

    UserCouponPageResponseDto page0 = list(userId, 0, 3, UserCouponStatus.AVAILABLE);
    UserCouponPageResponseDto page1 = list(userId, 1, 3, UserCouponStatus.AVAILABLE);

    assertThat(ids(page0)).isEqualTo(availableInOrder.subList(0, 3));
    assertThat(ids(page1)).isEqualTo(availableInOrder.subList(3, 4));
    assertThat(page0.getPageInfo().getTotalElements()).isEqualTo(4);
    assertThat(page0.getPageInfo().getTotalPages()).isEqualTo(2);
    assertThat(page0.getPageInfo().isLast()).isFalse();
    assertThat(page1.getPageInfo().isLast()).isTrue();
    assertThat(allPagesIds(userId, 2, UserCouponStatus.EXPIRED)).hasSize(4).doesNotHaveDuplicates();
    assertThat(allPagesIds(userId, 5, UserCouponStatus.USED)).hasSize(4).doesNotHaveDuplicates();
  }

  // ---------- 기존 기능과의 연동 ----------

  @Test
  void 발급_사용_복구_결과가_목록의_상태와_사용이력에_반영된다() {
    Long userId = newUser();
    Long couponId = usableCoupon("연동쿠폰");

    Long userCouponId = userCouponService.issueCoupon(userId, couponId).getUserCouponId();
    UserCouponResponseDto issued = list(userId, 0, 20, null).getCoupons().get(0);
    assertThat(issued.getUserCouponId()).isEqualTo(userCouponId);
    assertThat(issued.getStatus()).isEqualTo(UserCouponStatus.AVAILABLE);
    assertThat(issued.isUsableNow()).isTrue();
    assertThat(issued.getCoupon().getCouponId()).isEqualTo(couponId);
    assertThat(issued.getCoupon().getName()).isEqualTo("연동쿠폰");
    assertThat(issued.getCoupon().getMinOrderAmount()).isEqualByComparingTo("5000");

    long orderId = 990_500L;
    userCouponService.useCoupon(userId, userCouponId, BigDecimal.valueOf(10000), orderId);
    UserCouponResponseDto usedDto = list(userId, 0, 20, UserCouponStatus.USED).getCoupons().get(0);
    assertThat(usedDto.getUserCouponId()).isEqualTo(userCouponId);
    assertThat(usedDto.getDiscountAppliedAmount()).isEqualByComparingTo("1000");
    assertThat(list(userId, 0, 20, UserCouponStatus.AVAILABLE).getCoupons()).isEmpty();

    new TransactionTemplate(transactionManager).executeWithoutResult(s -> userCouponService.releaseByOrder(orderId));
    UserCouponPageResponseDto restored = list(userId, 0, 20, UserCouponStatus.AVAILABLE);
    assertThat(ids(restored)).containsExactly(userCouponId);
    assertThat(restored.getCoupons().get(0).getUsedAt()).isNull();
    assertThat(restored.getCoupons().get(0).getDiscountAppliedAmount()).isNull();
  }

  @Test
  void 응답_JSON_예시를_출력한다() throws Exception {
    Long userId = newUser();
    grant(userId, usableCoupon("신규가입 1천원"), now.minusMinutes(3), UserCouponStatus.AVAILABLE);
    grant(userId, notYetUsableCoupon("주말 특가 1천원"), now.minusMinutes(2), UserCouponStatus.AVAILABLE);
    grant(userId, usableCoupon("첫구매 1천원"), now.minusMinutes(1), UserCouponStatus.USED);

    UserCouponPageResponseDto response = list(userId, 0, 2, null);
    String json = objectMapper.writerWithDefaultPrettyPrinter()
        .writeValueAsString(BaseResponse.success("내 쿠폰 목록 조회 성공", response));

    System.out.println("[IT] GET /coupons/my?page=0&size=2 response =\n" + json);
    assertThat(json).contains("\"coupons\"").contains("\"pageInfo\"").contains("\"usableNow\"");
  }
}
