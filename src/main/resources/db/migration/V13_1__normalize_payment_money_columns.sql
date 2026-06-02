-- =========================================================
-- V13_1__normalize_payment_money_columns.sql
--
-- 목적:
-- - Payment 도메인 금액 컬럼을 BigDecimal(JPA) <-> NUMERIC(PostgreSQL) 로 정합성 맞추기
-- - payments.amount / refunds.amount / refund_item.amount : INT -> numeric(19,2)
-- - 기본값도 0.00 으로 통일
--
-- 배경:
-- - Payment 엔티티는 BigDecimal(precision=19, scale=2) 사용
-- - DB가 INT면 Hibernate validate에서 부팅 실패 가능
-- =========================================================

-- 1) payments.amount
DO $$
DECLARE v_type text;
BEGIN
SELECT data_type
INTO v_type
FROM information_schema.columns
WHERE table_schema='public'
  AND table_name='payments'
  AND column_name='amount';

IF v_type IS NOT NULL AND v_type <> 'numeric' THEN
    EXECUTE '
      ALTER TABLE payments
        ALTER COLUMN amount TYPE numeric(19,2)
        USING amount::numeric(19,2)
    ';
  ELSIF v_type = 'numeric' THEN
    -- 이미 numeric이라도 precision/scale 정합성 맞추기(확장/정규화)
    EXECUTE '
      ALTER TABLE payments
        ALTER COLUMN amount TYPE numeric(19,2)
        USING amount::numeric(19,2)
    ';
END IF;
END $$;

ALTER TABLE payments
    ALTER COLUMN amount SET DEFAULT 0.00;

-- 2) refunds.amount
DO $$
DECLARE v_type text;
BEGIN
SELECT data_type
INTO v_type
FROM information_schema.columns
WHERE table_schema='public'
  AND table_name='refunds'
  AND column_name='amount';

IF v_type IS NOT NULL AND v_type <> 'numeric' THEN
    EXECUTE '
      ALTER TABLE refunds
        ALTER COLUMN amount TYPE numeric(19,2)
        USING amount::numeric(19,2)
    ';
  ELSIF v_type = 'numeric' THEN
    EXECUTE '
      ALTER TABLE refunds
        ALTER COLUMN amount TYPE numeric(19,2)
        USING amount::numeric(19,2)
    ';
END IF;
END $$;

ALTER TABLE refunds
    ALTER COLUMN amount SET DEFAULT 0.00;

-- 3) refund_item.amount
DO $$
DECLARE v_type text;
BEGIN
SELECT data_type
INTO v_type
FROM information_schema.columns
WHERE table_schema='public'
  AND table_name='refund_item'
  AND column_name='amount';

IF v_type IS NOT NULL AND v_type <> 'numeric' THEN
    EXECUTE '
      ALTER TABLE refund_item
        ALTER COLUMN amount TYPE numeric(19,2)
        USING amount::numeric(19,2)
    ';
  ELSIF v_type = 'numeric' THEN
    EXECUTE '
      ALTER TABLE refund_item
        ALTER COLUMN amount TYPE numeric(19,2)
        USING amount::numeric(19,2)
    ';
END IF;
END $$;

ALTER TABLE refund_item
    ALTER COLUMN amount SET DEFAULT 0.00;