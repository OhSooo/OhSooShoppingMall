-- =========================================================
-- V6__seed_auth_identities.sql
--
-- 목적:
-- - 로컬 테스트용 LOCAL 인증 identity seed
-- =========================================================

INSERT INTO auth_identities (user_id, provider, provider_user_id, password_hash, email, created_at, updated_at, is_deleted, deleted_at)
VALUES
    (1, 'LOCAL', NULL, NULL, 'owner1@test.com', now(), now(), false, NULL),
    (2, 'LOCAL', NULL, NULL, 'owner2@test.com', now(), now(), false, NULL);
