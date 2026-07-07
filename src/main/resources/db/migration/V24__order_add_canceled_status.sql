-- order status 체크 제약에 CANCELED 추가
-- Java OrderStatus enum에는 있고 OrderService.confirmCancelOrderItem()이 실제로 사용하나 DB 제약에 누락되어 있어
-- 주문 전체 취소 확정 시 500 발생
ALTER TABLE orders DROP CONSTRAINT chk_order_status;

ALTER TABLE orders
    ADD CONSTRAINT chk_order_status
        CHECK (status IN ('CREATED', 'PAID', 'CANCELED', 'PARTIALLY_CANCELED', 'PARTIALLY_REFUNDED', 'COMPLETED'));
