-- V9__add_users_onboarded.sql
ALTER TABLE users
    ADD COLUMN onboarded BOOLEAN NOT NULL DEFAULT false;

-- 기존 로컬/시드 유저들은 프로필이 다 채워져 있으니 true로 처리하고 싶으면:
UPDATE users
SET onboarded = true
WHERE name IS NOT NULL
  AND birth IS NOT NULL
  AND gender IS NOT NULL
  AND phone IS NOT NULL
  AND address IS NOT NULL;
