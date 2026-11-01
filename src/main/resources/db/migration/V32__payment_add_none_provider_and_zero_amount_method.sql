-- PG를 거치지 않는 0원 결제(쿠폰 전액 할인 등)를 기록하기 위한 provider/method 값 추가
-- provider=NONE: PG 미경유 / method=ZERO_AMOUNT: 결제할 금액 없음
ALTER TABLE payments DROP CONSTRAINT chk_payment_provider;

ALTER TABLE payments
    ADD CONSTRAINT chk_payment_provider
        CHECK (provider IN ('TOSS', 'KAKAOPAY', 'NICE', 'NONE'));

ALTER TABLE payments DROP CONSTRAINT chk_payment_method;

ALTER TABLE payments
    ADD CONSTRAINT chk_payment_method
        CHECK (method IN ('CARD', 'TRANSFER', 'VIRTUAL_ACCOUNT', 'MOBILE', 'EASY_PAY', 'ZERO_AMOUNT'));
