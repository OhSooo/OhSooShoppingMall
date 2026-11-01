-- 주문에 적용된 쿠폰 이력 컬럼 추가
-- user_coupons.used_order_id는 "현재 사용 중인 주문"(쿠폰 복구 시 null로 초기화)이고,
-- orders.user_coupon_id는 "이 주문에 과거 어떤 쿠폰이 적용됐는지"를 취소/환불 이후에도 보존한다.
-- 쿠폰을 적용하지 않은 주문과 기존 주문은 null.
ALTER TABLE orders
    ADD COLUMN user_coupon_id BIGINT;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_user_coupon
        FOREIGN KEY (user_coupon_id) REFERENCES user_coupons(user_coupon_id);

CREATE INDEX idx_orders_user_coupon_id ON orders(user_coupon_id);

COMMENT ON COLUMN orders.user_coupon_id IS '주문에 적용된 사용자 쿠폰 ID(FK, 미적용 시 null). 쿠폰 복구 후에도 유지';
