-- =========================================================
-- V33__auth_seed_local_admin_user.sql
--
-- 목적:
-- - 로컬 테스트용 ADMIN 계정 시드 (기존 시드에 ADMIN 계정이 없어 ADMIN 전용 API 테스트 불가)
--   admin@test.com / Test1234! (V11과 같은 bcrypt 해시)
-- - 멱등(idempotent): 이미 같은 LOCAL 이메일 계정이 있으면 ADMIN 권한/비밀번호만 맞추고 복구
--
-- 주의:
-- - 메인 마이그레이션 폴더라 운영 환경에도 적용됨. 배포 전 시드 분리 여부 결정 필요
-- =========================================================

DO $$
DECLARE
  now_ts timestamptz := now();
  v_user_id bigint;
BEGIN
  IF EXISTS (SELECT 1 FROM auth_identities WHERE provider = 'LOCAL' AND email = 'admin@test.com') THEN
    SELECT user_id INTO v_user_id
    FROM auth_identities
    WHERE provider = 'LOCAL' AND email = 'admin@test.com';

    UPDATE users
    SET role = 'ADMIN', onboarded = true, is_deleted = false, deleted_at = NULL, updated_at = now_ts
    WHERE user_id = v_user_id;

    UPDATE auth_identities
    SET password_hash = '$2a$10$5ezwRF81cPHoGw.bbHCNpOX3gW98e038m/0QEEUxKCLfBeFUznyGa',
        is_deleted = false, deleted_at = NULL, updated_at = now_ts
    WHERE provider = 'LOCAL' AND email = 'admin@test.com';
  ELSE
    WITH u AS (
      INSERT INTO users (name, birth, gender, phone, address, role, onboarded, created_at, updated_at, is_deleted, deleted_at)
      VALUES ('관리자', DATE '1990-01-01', 'OTHER', '010-0000-0000', '서울특별시 중구', 'ADMIN', true, now_ts, now_ts, false, NULL)
      RETURNING user_id
    )
    INSERT INTO auth_identities (user_id, provider, provider_user_id, password_hash, email, created_at, updated_at, is_deleted, deleted_at)
    VALUES ((SELECT user_id FROM u), 'LOCAL', NULL,
            '$2a$10$5ezwRF81cPHoGw.bbHCNpOX3gW98e038m/0QEEUxKCLfBeFUznyGa',
            'admin@test.com', now_ts, now_ts, false, NULL);
  END IF;
END $$;
