-- =========================
-- Store 추가 제약조건 및 인덱스 보완
-- =========================

-- main_image_url 길이 확장
ALTER TABLE stores
ALTER COLUMN main_image_url TYPE VARCHAR(2048);

-- 배너 정렬 순서 음수 방지
ALTER TABLE store_banners
    ADD CONSTRAINT chk_store_banners_sort_order
        CHECK (sort_order >= 0);

-- 배송비 음수 방지
ALTER TABLE store_delivery_policies
    ADD CONSTRAINT chk_store_delivery_base_fee
        CHECK (base_delivery_fee >= 0);

ALTER TABLE store_delivery_policies
    ADD CONSTRAINT chk_store_delivery_free_threshold
        CHECK (free_delivery_threshold IS NULL OR free_delivery_threshold >= 0);

-- 배너 조회 조건 최적화
CREATE INDEX idx_store_banners_store_active_sort
    ON store_banners(store_id, is_active, sort_order);