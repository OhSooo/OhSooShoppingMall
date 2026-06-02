-- =========================================================
-- V11__update_seed_local_password_hash.sql
--
-- 목적:
-- - V10에서 넣은 LOCAL 시드 계정들의 password_hash를
--   로그인 가능한 bcrypt 해시로 일괄 교체
-- - owner1/owner2도 V6에서 password_hash가 NULL이라 로그인 불가 → 같이 교체
--
-- 적용 대상:
-- - provider='LOCAL'
-- - email이 @test.com 으로 끝나는 계정들(시드 계정 범위)
--
-- 비밀번호(평문):
-- - Test1234!
--
-- bcrypt hash:
-- - $2a$10$5ezwRF81cPHoGw.bbHCNpOX3gW98e038m/0QEEUxKCLfBeFUznyGa
-- =========================================================

UPDATE auth_identities
SET password_hash = '$2a$10$5ezwRF81cPHoGw.bbHCNpOX3gW98e038m/0QEEUxKCLfBeFUznyGa',
    updated_at = now()
WHERE provider = 'LOCAL'
  AND email IS NOT NULL
  AND email LIKE '%@test.com';
