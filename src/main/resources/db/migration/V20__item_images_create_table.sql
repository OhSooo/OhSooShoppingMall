-- =========================
-- Item_images
-- =========================
CREATE TABLE item_images (
    item_image_id BIGSERIAL PRIMARY KEY,
    item_id       BIGINT        NOT NULL,
    image_url     VARCHAR(2048) NOT NULL,
    display_order INT           NOT NULL DEFAULT 0,
    is_primary    BOOLEAN       NOT NULL DEFAULT false,
    alt_text      VARCHAR(255),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_item_image_item
        FOREIGN KEY (item_id) REFERENCES items(item_id)
);

COMMENT ON TABLE item_images IS '상품 이미지 메타데이터';

COMMENT ON COLUMN item_images.item_image_id IS '이미지 PK';
COMMENT ON COLUMN item_images.item_id IS '상품 ID';
COMMENT ON COLUMN item_images.image_url IS '이미지 URL';
COMMENT ON COLUMN item_images.display_order IS '노출 순서';
COMMENT ON COLUMN item_images.is_primary IS '대표 이미지 여부';
COMMENT ON COLUMN item_images.alt_text IS '대체 텍스트';
COMMENT ON COLUMN item_images.created_at IS '생성 시각';
COMMENT ON COLUMN item_images.updated_at IS '수정 시각';

CREATE INDEX idx_item_images_item_id ON item_images(item_id);

-- 상품당 대표 이미지는 하나만 허용
CREATE UNIQUE INDEX uq_item_images_primary
    ON item_images(item_id) WHERE is_primary = true;
