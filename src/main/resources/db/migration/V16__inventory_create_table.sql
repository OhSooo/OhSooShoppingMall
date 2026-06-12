-- inventories 테이블 생성
CREATE TABLE inventories (
    inventory_id BIGSERIAL PRIMARY KEY,
    item_variant_id BIGINT NOT NULL UNIQUE,
    quantity INT NOT NULL DEFAULT 0,
    CONSTRAINT chk_inventory_quantity CHECK (quantity >= 0),
    CONSTRAINT fk_inventory_item_variant
        FOREIGN KEY (item_variant_id)
            REFERENCES item_variants(item_variant_id)
);

COMMENT ON TABLE inventories IS '상품 판매 단위별 재고';
COMMENT ON COLUMN inventories.item_variant_id IS '상품 판매 단위 ID (1:1)';
COMMENT ON COLUMN inventories.quantity IS '재고 수량';

CREATE INDEX idx_inventories_item_variant_id ON inventories(item_variant_id);

-- 기존 item_variants.quantity 데이터를 inventories로 이전
INSERT INTO inventories (item_variant_id, quantity)
SELECT item_variant_id, quantity FROM item_variants;

-- item_variants에서 quantity 컬럼 제거
ALTER TABLE item_variants DROP COLUMN quantity;
