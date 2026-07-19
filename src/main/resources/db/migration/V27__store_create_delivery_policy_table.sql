-- =========================
-- Store Delivery Policies
-- =========================
CREATE TABLE store_delivery_policies (
    delivery_policy_id      BIGSERIAL      PRIMARY KEY,
    store_id                BIGINT         NOT NULL,
    base_delivery_fee       NUMERIC(19,2)  NOT NULL DEFAULT 0.00,
    free_delivery_threshold NUMERIC(19,2),
    is_active               BOOLEAN        NOT NULL DEFAULT true,
    created_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT fk_store_delivery_policy_store
        FOREIGN KEY (store_id) REFERENCES stores(store_id),
    CONSTRAINT uq_store_delivery_policy_store_id
        UNIQUE (store_id)
);

COMMENT ON TABLE store_delivery_policies IS '스토어 배송정책';

COMMENT ON COLUMN store_delivery_policies.delivery_policy_id IS '배송정책 PK';
COMMENT ON COLUMN store_delivery_policies.store_id IS '스토어 ID';
COMMENT ON COLUMN store_delivery_policies.base_delivery_fee IS '기본 배송비';
COMMENT ON COLUMN store_delivery_policies.free_delivery_threshold IS '무료배송 기준 금액';
COMMENT ON COLUMN store_delivery_policies.is_active IS '활성 여부';
COMMENT ON COLUMN store_delivery_policies.created_at IS '생성 시각';
COMMENT ON COLUMN store_delivery_policies.updated_at IS '수정 시각';
