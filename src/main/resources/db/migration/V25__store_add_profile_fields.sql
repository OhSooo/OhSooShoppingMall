-- =========================
-- Store 프로필 필드 추가
-- =========================

-- description 컬럼 타입을 TEXT로 확장
ALTER TABLE stores ALTER COLUMN description TYPE TEXT;

-- notice 컬럼 추가
ALTER TABLE stores ADD COLUMN notice TEXT;

-- main_image_url 컬럼 추가
ALTER TABLE stores ADD COLUMN main_image_url VARCHAR(500);

-- operation_status 컬럼 추가 (VARCHAR + CHECK 방식, 기존 status와 동일 패턴)
ALTER TABLE stores ADD COLUMN operation_status VARCHAR(20) NOT NULL DEFAULT 'OPEN';
ALTER TABLE stores ADD CONSTRAINT chk_store_operation_status
    CHECK (operation_status IN ('OPEN', 'CLOSED', 'PAUSED'));

COMMENT ON COLUMN stores.notice IS '스토어 공지';
COMMENT ON COLUMN stores.main_image_url IS '스토어 대표 이미지 URL';
COMMENT ON COLUMN stores.operation_status IS '스토어 영업 상태 (OPEN, CLOSED, PAUSED)';
