-- =========================================================
-- V15__normalize_money_columns_and_seed_precision.sql
--
-- 목적:
-- 1) "돈" 컬럼들을 numeric(19,2)로 통일(확장 포함)
--    - items.base_price
--    - item_variants.price
--    - orders.total_price
--    - order_items.price_at_purchase
--    - payments.amount / refunds.amount / refund_item.amount (V13_1에서 했지만 최종 정리)
--
-- 2) seed/기존 데이터의 소수점 자릿수 정규화(2자리로)
--    - 혹시 3자리 이상 들어온 경우 round(..., 2)
--
-- 3) payment_events.created_at 기본값 now() 안전망 (코드에서도 넣지만 DB도 안전하게)
--
-- 주의:
-- - Flyway 트랜잭션 환경에서 실행 가능
-- - 모두 멱등/재실행 안전하게 작성
-- =========================================================

-- ---------------------------------------------------------
-- 0) helper: 컬럼 타입을 numeric(19,2)로 정규화하는 패턴
-- ---------------------------------------------------------

-- 1) items.base_price
DO $$
DECLARE v_type text;
BEGIN
SELECT data_type INTO v_type
FROM information_schema.columns
WHERE table_schema='public' AND table_name='items' AND column_name='base_price';

IF v_type IS NOT NULL THEN
    EXECUTE '
      ALTER TABLE items
        ALTER COLUMN base_price TYPE numeric(19,2)
        USING base_price::numeric(19,2)
    ';
EXECUTE 'ALTER TABLE items ALTER COLUMN base_price SET DEFAULT 0.00';
END IF;
END $$;

-- 2) item_variants.price
DO $$
DECLARE v_type text;
BEGIN
SELECT data_type INTO v_type
FROM information_schema.columns
WHERE table_schema='public' AND table_name='item_variants' AND column_name='price';

IF v_type IS NOT NULL THEN
    EXECUTE '
      ALTER TABLE item_variants
        ALTER COLUMN price TYPE numeric(19,2)
        USING price::numeric(19,2)
    ';
EXECUTE 'ALTER TABLE item_variants ALTER COLUMN price SET DEFAULT 0.00';
END IF;
END $$;

-- 3) orders.total_price
DO $$
DECLARE v_type text;
BEGIN
SELECT data_type INTO v_type
FROM information_schema.columns
WHERE table_schema='public' AND table_name='orders' AND column_name='total_price';

IF v_type IS NOT NULL THEN
    EXECUTE '
      ALTER TABLE orders
        ALTER COLUMN total_price TYPE numeric(19,2)
        USING total_price::numeric(19,2)
    ';
EXECUTE 'ALTER TABLE orders ALTER COLUMN total_price SET DEFAULT 0.00';
END IF;
END $$;

-- 4) order_items.price_at_purchase
DO $$
DECLARE v_type text;
BEGIN
SELECT data_type INTO v_type
FROM information_schema.columns
WHERE table_schema='public' AND table_name='order_items' AND column_name='price_at_purchase';

IF v_type IS NOT NULL THEN
    EXECUTE '
      ALTER TABLE order_items
        ALTER COLUMN price_at_purchase TYPE numeric(19,2)
        USING price_at_purchase::numeric(19,2)
    ';
EXECUTE 'ALTER TABLE order_items ALTER COLUMN price_at_purchase SET DEFAULT 0.00';
END IF;
END $$;

-- 5) payments.amount (최종 정리)
DO $$
DECLARE v_type text;
BEGIN
SELECT data_type INTO v_type
FROM information_schema.columns
WHERE table_schema='public' AND table_name='payments' AND column_name='amount';

IF v_type IS NOT NULL THEN
    EXECUTE '
      ALTER TABLE payments
        ALTER COLUMN amount TYPE numeric(19,2)
        USING amount::numeric(19,2)
    ';
EXECUTE 'ALTER TABLE payments ALTER COLUMN amount SET DEFAULT 0.00';
END IF;
END $$;

-- 6) refunds.amount (최종 정리)
DO $$
DECLARE v_type text;
BEGIN
SELECT data_type INTO v_type
FROM information_schema.columns
WHERE table_schema='public' AND table_name='refunds' AND column_name='amount';

IF v_type IS NOT NULL THEN
    EXECUTE '
      ALTER TABLE refunds
        ALTER COLUMN amount TYPE numeric(19,2)
        USING amount::numeric(19,2)
    ';
EXECUTE 'ALTER TABLE refunds ALTER COLUMN amount SET DEFAULT 0.00';
END IF;
END $$;

-- 7) refund_item.amount (최종 정리)
DO $$
DECLARE v_type text;
BEGIN
SELECT data_type INTO v_type
FROM information_schema.columns
WHERE table_schema='public' AND table_name='refund_item' AND column_name='amount';

IF v_type IS NOT NULL THEN
    EXECUTE '
      ALTER TABLE refund_item
        ALTER COLUMN amount TYPE numeric(19,2)
        USING amount::numeric(19,2)
    ';
EXECUTE 'ALTER TABLE refund_item ALTER COLUMN amount SET DEFAULT 0.00';
END IF;
END $$;

-- ---------------------------------------------------------
-- 2) seed/기존 데이터 자릿수 정규화 (혹시 2자리 초과 대비)
-- ---------------------------------------------------------
UPDATE items
SET base_price = round(base_price, 2)
WHERE base_price IS NOT NULL;

UPDATE item_variants
SET price = round(price, 2)
WHERE price IS NOT NULL;

UPDATE orders
SET total_price = round(total_price, 2)
WHERE total_price IS NOT NULL;

UPDATE order_items
SET price_at_purchase = round(price_at_purchase, 2)
WHERE price_at_purchase IS NOT NULL;

UPDATE payments
SET amount = round(amount, 2)
WHERE amount IS NOT NULL;

UPDATE refunds
SET amount = round(amount, 2)
WHERE amount IS NOT NULL;

UPDATE refund_item
SET amount = round(amount, 2)
WHERE amount IS NOT NULL;

-- ---------------------------------------------------------
-- 3) payment_events.created_at 기본값 안전망
--    (코드에서 넣지만, 혹시 INSERT에서 누락될 때 대비)
-- ---------------------------------------------------------
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema='public'
      AND table_name='payment_events'
      AND column_name='created_at'
  ) THEN
    EXECUTE 'ALTER TABLE payment_events ALTER COLUMN created_at SET DEFAULT now()';
END IF;
END $$;