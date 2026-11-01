-- =========================================================
-- V34__coupon_seed_local_coupons.sql
--
-- 목적:
-- - 로컬 테스트용 쿠폰 시드 (쿠폰 적용 주문 / 0원 주문 / 만료 쿠폰 시나리오)
-- - 쿠폰 ID는 지정하지 않고, 같은 이름의 쿠폰이 없을 때만 INSERT (로컬에 수동으로 넣은 쿠폰과 충돌 방지)
-- - 기간은 마이그레이션 실행 시점과 무관하도록 고정 날짜 사용
--
-- 시드 쿠폰:
-- 1) 정액 5,000원 (최소 주문 30,000원)  — 기본 할인 / 최소 금액 미달
-- 2) 정률 10% (최대 할인 10,000원)       — 정률 계산 / 할인 상한
-- 3) 정액 10,000,000원                  — 할인 후 0원 주문
-- 4) 기간 만료 쿠폰 + minji.kim@test.com에게 발급된 상태 — 만료 쿠폰 사용 거부
--    (발급 기간이 지나 API로는 발급 불가하므로 user_coupons까지 시드)
--
-- 주의:
-- - 메인 마이그레이션 폴더라 운영 환경에도 적용됨. 배포 전 시드 분리 여부 결정 필요
-- =========================================================

INSERT INTO coupons (name, discount_type, discount_value, min_order_amount, max_discount_amount,
                     issue_start_at, issue_end_at, use_start_at, use_end_at)
SELECT s.name, s.discount_type, s.discount_value, s.min_order_amount, s.max_discount_amount,
       s.issue_start_at, s.issue_end_at, s.use_start_at, s.use_end_at
FROM (VALUES
  ('[시드] 정액 5,000원 (3만원 이상)', 'FIXED_AMOUNT', 5000::numeric, 30000::numeric, NULL::numeric,
   TIMESTAMPTZ '2026-01-01 00:00:00+09', TIMESTAMPTZ '2099-12-31 23:59:59+09',
   TIMESTAMPTZ '2026-01-01 00:00:00+09', TIMESTAMPTZ '2099-12-31 23:59:59+09'),
  ('[시드] 정률 10% (최대 1만원)', 'PERCENTAGE', 10::numeric, NULL::numeric, 10000::numeric,
   TIMESTAMPTZ '2026-01-01 00:00:00+09', TIMESTAMPTZ '2099-12-31 23:59:59+09',
   TIMESTAMPTZ '2026-01-01 00:00:00+09', TIMESTAMPTZ '2099-12-31 23:59:59+09'),
  ('[시드] 전액 할인 (0원 주문용)', 'FIXED_AMOUNT', 10000000::numeric, NULL::numeric, NULL::numeric,
   TIMESTAMPTZ '2026-01-01 00:00:00+09', TIMESTAMPTZ '2099-12-31 23:59:59+09',
   TIMESTAMPTZ '2026-01-01 00:00:00+09', TIMESTAMPTZ '2099-12-31 23:59:59+09'),
  ('[시드] 기간 만료 쿠폰', 'FIXED_AMOUNT', 3000::numeric, NULL::numeric, NULL::numeric,
   TIMESTAMPTZ '2026-01-01 00:00:00+09', TIMESTAMPTZ '2026-01-31 23:59:59+09',
   TIMESTAMPTZ '2026-01-01 00:00:00+09', TIMESTAMPTZ '2026-02-28 23:59:59+09')
) AS s(name, discount_type, discount_value, min_order_amount, max_discount_amount,
       issue_start_at, issue_end_at, use_start_at, use_end_at)
WHERE NOT EXISTS (SELECT 1 FROM coupons c WHERE c.name = s.name);

-- 만료 쿠폰을 minji.kim@test.com에게 발급된 상태로 시드 (계정이 없거나 이미 발급돼 있으면 건너뜀)
-- status는 AVAILABLE로 저장 — EXPIRED는 저장하지 않고 조회 시 사용 종료일 기준으로 계산됨
INSERT INTO user_coupons (user_id, coupon_id, status, issued_at)
SELECT a.user_id, c.coupon_id, 'AVAILABLE', TIMESTAMPTZ '2026-01-15 00:00:00+09'
FROM auth_identities a
JOIN coupons c ON c.name = '[시드] 기간 만료 쿠폰'
WHERE a.provider = 'LOCAL'
  AND a.email = 'minji.kim@test.com'
  AND NOT EXISTS (
    SELECT 1 FROM user_coupons uc WHERE uc.user_id = a.user_id AND uc.coupon_id = c.coupon_id
  );
