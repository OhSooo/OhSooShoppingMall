-- =========================
-- Store Banners
-- =========================
CREATE TABLE store_banners (
    store_banner_id BIGSERIAL PRIMARY KEY,
    store_id        BIGINT        NOT NULL,
    image_url       VARCHAR(2048) NOT NULL,
    link_url        VARCHAR(2048),
    title           VARCHAR(255),
    sort_order      INT           NOT NULL DEFAULT 0,
    is_active       BOOLEAN       NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_store_banner_store
        FOREIGN KEY (store_id) REFERENCES stores(store_id)
);

COMMENT ON TABLE store_banners IS '스토어 배너';

COMMENT ON COLUMN store_banners.store_banner_id IS '배너 PK';
COMMENT ON COLUMN store_banners.store_id IS '스토어 ID';
COMMENT ON COLUMN store_banners.image_url IS '배너 이미지 URL';
COMMENT ON COLUMN store_banners.link_url IS '배너 클릭 시 이동 URL';
COMMENT ON COLUMN store_banners.title IS '배너 제목';
COMMENT ON COLUMN store_banners.sort_order IS '정렬 순서';
COMMENT ON COLUMN store_banners.is_active IS '활성 여부';
COMMENT ON COLUMN store_banners.created_at IS '생성 시각';
COMMENT ON COLUMN store_banners.updated_at IS '수정 시각';

CREATE INDEX idx_store_banners_store_id ON store_banners(store_id);
