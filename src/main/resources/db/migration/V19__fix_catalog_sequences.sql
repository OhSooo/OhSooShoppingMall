-- =========================================================
-- V19__fix_catalog_sequences.sql
--
-- 목적:
-- - V2(categories), V4(stores, items) 에서 PK를 명시 지정하여 INSERT한 탓에
--   시퀀스가 1로 고정된 상태임. JPA의 nextval 호출 시 PK 충돌 방지를 위해
--   각 시퀀스를 현재 MAX(id) 값으로 보정한다.
-- =========================================================

SELECT setval(
    pg_get_serial_sequence('categories', 'category_id'),
    (SELECT COALESCE(MAX(category_id), 1) FROM categories),
    true
);

SELECT setval(
    pg_get_serial_sequence('stores', 'store_id'),
    (SELECT COALESCE(MAX(store_id), 1) FROM stores),
    true
);

SELECT setval(
    pg_get_serial_sequence('items', 'item_id'),
    (SELECT COALESCE(MAX(item_id), 1) FROM items),
    true
);
