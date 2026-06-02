-- V14__align_money_columns_to_numeric.sql
-- 목적:
-- - BigDecimal(JPA) <-> NUMERIC(PostgreSQL) 정합성 맞추기
-- - price / base_price / total_price / price_at_purchase 등 "돈" 관련 컬럼 타입을 numeric(12,2)로 통일
--
-- 배경:
-- - 기존 V1 스키마는 INT 기반으로 가격을 저장했으나,
--   JPA 엔티티에서 BigDecimal을 사용하면 Hibernate validate에서 타입 불일치로 부팅이 실패함.
--
-- 주의:
-- - 기존 값이 정수여도 numeric(12,2)로 안전하게 변환됨.
-- - USING 절로 캐스팅 수행.

-- =========================================================
-- 1) items.base_price : INT -> numeric(12,2)
-- =========================================================
ALTER TABLE items
ALTER COLUMN base_price TYPE numeric(12,2)
  USING base_price::numeric(12,2);

-- 기본값도 numeric으로 정합성 맞춤
ALTER TABLE items
    ALTER COLUMN base_price SET DEFAULT 0.00;

COMMENT ON COLUMN items.base_price IS '기본 가격';


-- =========================================================
-- 2) item_variants.price : INT -> numeric(12,2)
-- =========================================================
ALTER TABLE item_variants
ALTER COLUMN price TYPE numeric(12,2)
  USING price::numeric(12,2);

ALTER TABLE item_variants
    ALTER COLUMN price SET DEFAULT 0.00;

COMMENT ON COLUMN item_variants.price IS '판매 단위 가격';


-- =========================================================
-- 3) orders.total_price : INT -> numeric(12,2)
-- =========================================================
ALTER TABLE orders
ALTER COLUMN total_price TYPE numeric(12,2)
  USING total_price::numeric(12,2);

ALTER TABLE orders
    ALTER COLUMN total_price SET DEFAULT 0.00;

COMMENT ON COLUMN orders.total_price IS '주문 총액';


-- =========================================================
-- 4) order_items.price_at_purchase : INT -> numeric(12,2)
-- =========================================================
ALTER TABLE order_items
ALTER COLUMN price_at_purchase TYPE numeric(12,2)
  USING price_at_purchase::numeric(12,2);

ALTER TABLE order_items
    ALTER COLUMN price_at_purchase SET DEFAULT 0.00;

COMMENT ON COLUMN order_items.price_at_purchase IS '구매 당시 가격';


-- =========================================================
-- 5) (선택) payments/refunds 금액 컬럼 처리
-- ---------------------------------------------------------
-- 현재 payments.amount / refunds.amount / refund_item.amount 는 INT로 되어있음.
-- "원화는 정수" 정책이면 그대로 둬도 OK.
-- 만약 결제 금액도 BigDecimal로 가려면 아래 주석을 풀어 numeric으로 맞춰라.
-- =========================================================

/*
ALTER TABLE payments
  ALTER COLUMN amount TYPE numeric(12,2)
  USING amount::numeric(12,2);

ALTER TABLE payments
  ALTER COLUMN amount SET DEFAULT 0.00;

ALTER TABLE refunds
  ALTER COLUMN amount TYPE numeric(12,2)
  USING amount::numeric(12,2);

ALTER TABLE refund_item
  ALTER COLUMN amount TYPE numeric(12,2)
  USING amount::numeric(12,2);
*/
