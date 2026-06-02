-- V12__orders_shipping_and_payment_tables.sql
-- 목적:
-- 1) orders 테이블에 배송(스냅샷) 컬럼 추가 (엔티티 Order 기준)
-- 2) order_item_history(단수) -> order_item_histories(복수) 테이블명 정합성 맞추기
-- 3) users 테이블에 shipping_postcode / shipping_address_detail 컬럼 추가 + 기존 시드 유저 값 채우기
-- 4) payment 도메인 테이블(payments, payment_events, refunds, refund_item) 생성
--
-- 전제:
-- - V1~V11까지 적용된 상태
-- - V12는 아직 적용 전

-- NOTE:
-- Flyway는 기본적으로 마이그레이션을 트랜잭션으로 실행할 수 있으므로
-- BEGIN/COMMIT를 SQL 파일에 직접 두지 않는 것을 권장한다.

-- =========================================================
-- 1) orders 배송 컬럼 추가
-- =========================================================

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS receiver_name VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS receiver_phone VARCHAR(50)  NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS shipping_address VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS shipping_postcode VARCHAR(20),
    ADD COLUMN IF NOT EXISTS shipping_address_detail VARCHAR(255),
    ADD COLUMN IF NOT EXISTS shipping_request_note VARCHAR(255);

COMMENT ON COLUMN orders.receiver_name IS '받는 사람 이름(주문 스냅샷)';
COMMENT ON COLUMN orders.receiver_phone IS '받는 사람 전화번호(주문 스냅샷)';
COMMENT ON COLUMN orders.shipping_address IS '배송 주소(주문 스냅샷)';
COMMENT ON COLUMN orders.shipping_postcode IS '우편번호(주문 스냅샷)';
COMMENT ON COLUMN orders.shipping_address_detail IS '상세주소(주문 스냅샷)';
COMMENT ON COLUMN orders.shipping_request_note IS '배송 요청사항(주문 스냅샷)';


-- =========================================================
-- 2) order_item_history -> order_item_histories rename
-- =========================================================

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = 'public'
      AND table_name = 'order_item_history'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = 'public'
      AND table_name = 'order_item_histories'
  )
  THEN
ALTER TABLE order_item_history RENAME TO order_item_histories;
END IF;
END $$;

COMMENT ON TABLE order_item_histories IS '주문 상품 상태 변경 이력';

-- changed_at 보정(안전망)
DO $$
DECLARE
v_data_type text;
BEGIN
SELECT data_type
INTO v_data_type
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'order_item_histories'
  AND column_name = 'changed_at';

IF v_data_type = 'timestamp without time zone' THEN
    EXECUTE 'ALTER TABLE order_item_histories
             ALTER COLUMN changed_at TYPE timestamptz
             USING changed_at AT TIME ZONE ''UTC''';
END IF;
END $$;

ALTER TABLE order_item_histories
    ALTER COLUMN changed_at SET DEFAULT now();


-- =========================================================
-- 3) users: shipping_postcode / shipping_address_detail 추가 + 시드값 채우기
-- =========================================================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS shipping_postcode VARCHAR(20),
    ADD COLUMN IF NOT EXISTS shipping_address_detail VARCHAR(255);

COMMENT ON COLUMN users.shipping_postcode IS '우편번호(기본 배송지)';
COMMENT ON COLUMN users.shipping_address_detail IS '상세주소(기본 배송지)';

-- ---------------------------------------------------------
-- 3-1) 기존 시드 유저들 값 채우기
-- - 원래 address만 있던 시드들을 대상으로, 테스트용 우편번호/상세주소를 넣는다.
-- - 이미 값이 들어있으면 덮어쓰지 않음 (NULL일 때만 채움)
-- ---------------------------------------------------------

-- owner1/owner2 (V3_1, V6에서 기반이 된 users 1,2)
UPDATE users
SET shipping_postcode = COALESCE(shipping_postcode, '00000'),
    shipping_address_detail = COALESCE(shipping_address_detail, '상세주소 미입력')
WHERE user_id IN (1, 2);

-- V10에서 만든 로컬 시드 10명: address 값 기반으로 “보기 좋은” 테스트 데이터 세팅
UPDATE users
SET shipping_postcode = CASE
                            WHEN address ILIKE '%마포구%' THEN '04000'
                            WHEN address ILIKE '%강남구%' THEN '06000'
                            WHEN address ILIKE '%연수구%' THEN '22000'
                            WHEN address ILIKE '%해운대구%' THEN '48000'
                            WHEN address ILIKE '%수성구%' THEN '42000'
                            WHEN address ILIKE '%서구%'   THEN '35000'   -- 대전 서구
                            WHEN address ILIKE '%북구%'   THEN '61000'   -- 광주 북구
                            WHEN address ILIKE '%남구%'   THEN '44000'   -- 울산 남구
                            WHEN address ILIKE '%분당구%' THEN '13500'
                            WHEN address ILIKE '%제주시%' THEN '63000'
                            ELSE COALESCE(shipping_postcode, '99999')
    END,
    shipping_address_detail = COALESCE(shipping_address_detail, '상세주소 101동 1001호')
WHERE shipping_postcode IS NULL
  AND shipping_address_detail IS NULL
  AND address IS NOT NULL;


-- =========================================================
-- 4) Payment 도메인 테이블 생성
--    - Entity 기준이 최신이므로 Entity 기준으로 작성
--    - Payment는 BaseTimeEntity 상속 -> created_at/updated_at/is_deleted/deleted_at 포함 필수
-- =========================================================

-- ---------------------------------------------------------
-- 4-1) payments
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS payments (
                                        payment_id BIGSERIAL PRIMARY KEY,
                                        order_id BIGINT NOT NULL,
                                        status VARCHAR(30) NOT NULL,
    amount INT NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'KRW',
    method VARCHAR(30) NOT NULL,
    provider VARCHAR(30) NOT NULL,

    pg_payment_key VARCHAR(255) UNIQUE,
    pg_transaction_id VARCHAR(255) UNIQUE,

    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    approved_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    fail_reason VARCHAR(255),

    -- BaseTimeEntity (필수)
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT fk_payments_order
    FOREIGN KEY (order_id) REFERENCES orders(order_id),

    CONSTRAINT chk_payment_status
    CHECK (status IN ('READY','AUTHORIZED','CAPTURED','FAILED','CANCELED','REFUNDED')),

    CONSTRAINT chk_payment_method
    CHECK (method IN ('CARD','TRANSFER','VIRTUAL_ACCOUNT','MOBILE','EASY_PAY')),

    CONSTRAINT chk_payment_provider
    CHECK (provider IN ('TOSS','KAKAOPAY','NICE'))
    );

COMMENT ON TABLE payments IS '결제(주문에 대한 결제 시도)';
COMMENT ON COLUMN payments.order_id IS '주문 PK(FK)';
COMMENT ON COLUMN payments.status IS '결제 상태(READY/AUTHORIZED/CAPTURED/FAILED/CANCELED/REFUNDED)';
COMMENT ON COLUMN payments.amount IS '결제 금액';
COMMENT ON COLUMN payments.currency IS '통화 코드';
COMMENT ON COLUMN payments.method IS '결제 수단';
COMMENT ON COLUMN payments.provider IS 'PG사';
COMMENT ON COLUMN payments.pg_payment_key IS 'PG 결제키';
COMMENT ON COLUMN payments.pg_transaction_id IS 'PG 트랜잭션키';
COMMENT ON COLUMN payments.requested_at IS '결제 요청 시각';
COMMENT ON COLUMN payments.approved_at IS 'PG 승인 시각';
COMMENT ON COLUMN payments.failed_at IS '결제 실패 시각';
COMMENT ON COLUMN payments.fail_reason IS '실패 사유';

CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_pg_payment_key ON payments(pg_payment_key);
CREATE INDEX IF NOT EXISTS idx_payments_pg_transaction_id ON payments(pg_transaction_id);

-- ---------------------------------------------------------
-- 4-2) payment_events
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS payment_events (
                                              payment_event_id BIGSERIAL PRIMARY KEY,
                                              payment_id BIGINT NOT NULL,
                                              event_type VARCHAR(50) NOT NULL,
    payload_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_payment_events_payment
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id) ON DELETE CASCADE,

    CONSTRAINT chk_payment_event_type
    CHECK (event_type IN ('WEBHOOK_APPROVED','WEBHOOK_FAILED','CLIENT_CONFIRM_REQUEST'))
    );

COMMENT ON TABLE payment_events IS '결제 관련 이벤트 로그(PG payload 원문 저장)';
COMMENT ON COLUMN payment_events.payment_id IS '결제 PK(FK)';
COMMENT ON COLUMN payment_events.event_type IS '이벤트 타입';
COMMENT ON COLUMN payment_events.payload_json IS '원문 JSON';
COMMENT ON COLUMN payment_events.created_at IS '생성 시각';

CREATE INDEX IF NOT EXISTS idx_payment_events_payment_id ON payment_events(payment_id);

-- ---------------------------------------------------------
-- 4-3) refunds
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS refunds (
                                       refund_id BIGSERIAL PRIMARY KEY,
                                       payment_id BIGINT NOT NULL,
                                       amount INT NOT NULL,
                                       status VARCHAR(30) NOT NULL,
    reason VARCHAR(255),
    refunded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_refunds_payment
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id),

    CONSTRAINT chk_refund_status
    CHECK (status IN ('REQUESTED','SUCCEEDED','FAILED'))
    );

COMMENT ON TABLE refunds IS '환불(결제에 대한 환불 요청)';
COMMENT ON COLUMN refunds.payment_id IS '결제 PK(FK)';
COMMENT ON COLUMN refunds.amount IS '환불 금액';
COMMENT ON COLUMN refunds.status IS '환불 상태(REQUESTED/SUCCEEDED/FAILED)';
COMMENT ON COLUMN refunds.reason IS '환불 사유';
COMMENT ON COLUMN refunds.refunded_at IS '환불 완료 시각';
COMMENT ON COLUMN refunds.created_at IS '생성 시각';

CREATE INDEX IF NOT EXISTS idx_refunds_payment_id ON refunds(payment_id);

-- ---------------------------------------------------------
-- 4-4) refund_item  (Entity 기준: refund_item)
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS refund_item (
                                           refunditem_id BIGSERIAL PRIMARY KEY,
                                           refund_id BIGINT NOT NULL,
                                           order_item_id BIGINT NOT NULL,
                                           amount INT NOT NULL,
                                           quantity INT NOT NULL,

                                           CONSTRAINT fk_refund_item_refund
                                           FOREIGN KEY (refund_id) REFERENCES refunds(refund_id) ON DELETE CASCADE,

    CONSTRAINT fk_refund_item_order_item
    FOREIGN KEY (order_item_id) REFERENCES order_items(order_item_id)
    );

COMMENT ON TABLE refund_item IS '환불 품목(주문상품 단위 환불)';
COMMENT ON COLUMN refund_item.refund_id IS '환불 PK(FK)';
COMMENT ON COLUMN refund_item.order_item_id IS '주문상품 PK(FK)';
COMMENT ON COLUMN refund_item.amount IS '환불 금액(해당 품목 기준)';
COMMENT ON COLUMN refund_item.quantity IS '환불 수량';

CREATE INDEX IF NOT EXISTS idx_refund_item_refund_id ON refund_item(refund_id);
CREATE INDEX IF NOT EXISTS idx_refund_item_order_item_id ON refund_item(order_item_id);
