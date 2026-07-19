-- =========================
-- Store Banner is_active 컬럼 제거
-- =========================
-- StoreBanner는 스토어 상세 페이지 노출용 홍보 이미지 관리 용도로,
-- 숨김/재노출 상태를 별도로 관리할 필요가 없어 삭제는 hard delete로 처리한다.

-- is_active를 포함하던 기존 인덱스 제거
DROP INDEX IF EXISTS idx_store_banners_store_active_sort;

ALTER TABLE store_banners
    DROP COLUMN is_active;

-- 배너 조회 조건 최적화 (storeId, sortOrder 기준)
CREATE INDEX idx_store_banners_store_sort
    ON store_banners(store_id, sort_order);
