-- payment status 체크 제약에 CONFIRMING 추가
-- Java PaymentStatus enum에는 있으나 DB 제약에 누락되어 결제 confirm 시 500 발생
ALTER TABLE payments DROP CONSTRAINT chk_payment_status;

ALTER TABLE payments
    ADD CONSTRAINT chk_payment_status
        CHECK (status IN ('READY', 'CONFIRMING', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'CANCELED', 'REFUNDED'));
