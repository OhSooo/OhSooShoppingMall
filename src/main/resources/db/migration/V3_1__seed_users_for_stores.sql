-- =========================================================
-- V3_1__seed_users_for_stores.sql
--
-- 목적:
-- - V4에서 stores.owner_id = 1,2 를 사용하므로
--   FK 오류 방지를 위해 users 1,2를 선행 생성한다.
--
-- 주의:
-- - 현재 V1 스키마 기준 users.email은 NOT NULL이므로 임시 email을 넣는다.
-- - 이후 V5에서 users.email은 제거되고 auth_identities가 도입된다.
-- =========================================================

INSERT INTO users (user_id, email, name, birth, gender, phone, address, role, created_at, updated_at, is_deleted, deleted_at)
VALUES
    (1, 'owner1@test.com', 'Owner One', '1990-01-01', 'FEMALE', '010-0000-0001', 'Seoul', 'OWNER', now(), now(), false, NULL),
    (2, 'owner2@test.com', 'Owner Two', '1990-01-02', 'MALE',   '010-0000-0002', 'Seoul', 'OWNER', now(), now(), false, NULL);
