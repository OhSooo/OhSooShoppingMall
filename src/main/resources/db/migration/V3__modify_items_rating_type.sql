-- =========================================================
-- V3__change_items_rating_to_numeric.sql
--
-- 목적:
-- items 테이블의 rating 컬럼 타입을 DOUBLE PRECISION(float8)에서
-- NUMERIC(2,1)로 변경한다
--
-- 배경:
-- rating 값은 평점(예: 4.5, 3.0 등)으로 소수점 정밀도가 중요하며
-- 부동소수점(Double)은 오차 가능성이 있어 BigDecimal + NUMERIC 타입이 적합함
--
-- 변경 내용:
-- - 기존 타입: DOUBLE PRECISION (float8)
-- - 변경 타입: NUMERIC(2,1)
--
-- 주의 사항:
-- - 기존 rating 값이 NUMERIC(2,1) 범위를 초과하지 않아야 함
-- - USING 절을 통해 기존 데이터를 안전하게 캐스팅함
--
-- 관련 Entity:
-- Item.rating (BigDecimal)
-- @Column(precision = 2, scale = 1)
-- =========================================================

ALTER TABLE items
ALTER COLUMN rating TYPE numeric(2,1)
USING rating::numeric(2,1);
