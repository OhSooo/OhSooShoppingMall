-- =========================================================
-- V10__seed_local_users_10_with_unique_email.sql
--
-- 목적:
-- 1) LOCAL email 유니크 보장(부분 인덱스)
-- 2) 로컬 테스트용 사용자 10명(users + auth_identities) 시드
--    - 멱등(idempotent): 이미 있으면 users/auth_identities 업데이트 + 복구
--
-- 전제:
-- - V5 이후 스키마 기준(users.email 없음)
-- - auth_identities(provider,email,password_hash)
-- =========================================================

-- 1) LOCAL email 유니크 보장 (LOCAL에서만, email NOT NULL인 경우)
--    (소셜은 email NULL일 수 있으니 영향 최소)
CREATE UNIQUE INDEX IF NOT EXISTS uk_auth_local_email
    ON auth_identities (email)
    WHERE provider = 'LOCAL' AND email IS NOT NULL;

DO $$
DECLARE
now_ts timestamptz := now();

  -- 공통 로직용
  v_user_id bigint;
BEGIN

  -- =======================================================
  -- Seed #1
  -- =======================================================
  IF EXISTS (SELECT 1 FROM auth_identities WHERE provider='LOCAL' AND email='minji.kim@test.com') THEN
SELECT user_id INTO v_user_id
FROM auth_identities
WHERE provider='LOCAL' AND email='minji.kim@test.com';

UPDATE users
SET name='김민지', birth=DATE '1999-02-14', gender='FEMALE',
    phone='010-2010-0001', address='서울특별시 마포구', role='GENERAL',
    onboarded=true, is_deleted=false, deleted_at=NULL, updated_at=now_ts
WHERE user_id=v_user_id;

UPDATE auth_identities
SET password_hash='$2b$10$XxgZvZ7uYd0wWas9NWgS.OBLXYRpqcvx.pJN8hfXnTQX.2NlhD3V.',
    is_deleted=false, deleted_at=NULL, updated_at=now_ts
WHERE provider='LOCAL' AND email='minji.kim@test.com';
ELSE
    WITH u AS (
      INSERT INTO users (name,birth,gender,phone,address,role,onboarded,created_at,updated_at,is_deleted,deleted_at)
      VALUES ('김민지', DATE '1999-02-14', 'FEMALE', '010-2010-0001', '서울특별시 마포구', 'GENERAL', true, now_ts, now_ts, false, NULL)
      RETURNING user_id
    )
    INSERT INTO auth_identities (user_id,provider,provider_user_id,password_hash,email,created_at,updated_at,is_deleted,deleted_at)
    VALUES ((SELECT user_id FROM u), 'LOCAL', NULL,
            '$2b$10$XxgZvZ7uYd0wWas9NWgS.OBLXYRpqcvx.pJN8hfXnTQX.2NlhD3V.',
            'minji.kim@test.com', now_ts, now_ts, false, NULL);
END IF;

  -- =======================================================
  -- Seed #2
  -- =======================================================
  IF EXISTS (SELECT 1 FROM auth_identities WHERE provider='LOCAL' AND email='jihoon.lee@test.com') THEN
SELECT user_id INTO v_user_id
FROM auth_identities
WHERE provider='LOCAL' AND email='jihoon.lee@test.com';

UPDATE users
SET name='이지훈', birth=DATE '1997-09-03', gender='MALE',
    phone='010-2010-0002', address='서울특별시 강남구', role='GENERAL',
    onboarded=true, is_deleted=false, deleted_at=NULL, updated_at=now_ts
WHERE user_id=v_user_id;

UPDATE auth_identities
SET password_hash='$2b$10$xePLP1gRkbwlG0qQGky3sOtPhU6xbiIouQG6IyYsZt4wzX1CxOOQq',
    is_deleted=false, deleted_at=NULL, updated_at=now_ts
WHERE provider='LOCAL' AND email='jihoon.lee@test.com';
ELSE
    WITH u AS (
      INSERT INTO users (name,birth,gender,phone,address,role,onboarded,created_at,updated_at,is_deleted,deleted_at)
      VALUES ('이지훈', DATE '1997-09-03', 'MALE', '010-2010-0002', '서울특별시 강남구', 'GENERAL', true, now_ts, now_ts, false, NULL)
      RETURNING user_id
    )
    INSERT INTO auth_identities (user_id,provider,provider_user_id,password_hash,email,created_at,updated_at,is_deleted,deleted_at)
    VALUES ((SELECT user_id FROM u), 'LOCAL', NULL,
            '$2b$10$xePLP1gRkbwlG0qQGky3sOtPhU6xbiIouQG6IyYsZt4wzX1CxOOQq',
            'jihoon.lee@test.com', now_ts, now_ts, false, NULL);
END IF;

  -- =======================================================
  -- Seed #3
  -- =======================================================
  IF EXISTS (SELECT 1 FROM auth_identities WHERE provider='LOCAL' AND email='seoyeon.park@test.com') THEN
SELECT user_id INTO v_user_id
FROM auth_identities
WHERE provider='LOCAL' AND email='seoyeon.park@test.com';

UPDATE users
SET name='박서연', birth=DATE '2000-01-22', gender='FEMALE',
    phone='010-2010-0003', address='인천광역시 연수구', role='GENERAL',
    onboarded=true, is_deleted=false, deleted_at=NULL, updated_at=now_ts
WHERE user_id=v_user_id;

UPDATE auth_identities
SET password_hash='$2b$10$u0X4sYlC0kPuJqX7yK8u/egkWh5FqkE0eSxOAfXQvY4q0uO5wSPf2',
    is_deleted=false, deleted_at=NULL, updated_at=now_ts
WHERE provider='LOCAL' AND email='seoyeon.park@test.com';
ELSE
    WITH u AS (
      INSERT INTO users (name,birth,gender,phone,address,role,onboarded,created_at,updated_at,is_deleted,deleted_at)
      VALUES ('박서연', DATE '2000-01-22', 'FEMALE', '010-2010-0003', '인천광역시 연수구', 'GENERAL', true, now_ts, now_ts, false, NULL)
      RETURNING user_id
    )
    INSERT INTO auth_identities (user_id,provider,provider_user_id,password_hash,email,created_at,updated_at,is_deleted,deleted_at)
    VALUES ((SELECT user_id FROM u), 'LOCAL', NULL,
            '$2b$10$u0X4sYlC0kPuJqX7yK8u/egkWh5FqkE0eSxOAfXQvY4q0uO5wSPf2',
            'seoyeon.park@test.com', now_ts, now_ts, false, NULL);
END IF;

  -- =======================================================
  -- Seed #4
  -- =======================================================
  IF EXISTS (SELECT 1 FROM auth_identities WHERE provider='LOCAL' AND email='junseo.choi@test.com') THEN
SELECT user_id INTO v_user_id
FROM auth_identities
WHERE provider='LOCAL' AND email='junseo.choi@test.com';

UPDATE users
SET name='최준서', birth=DATE '1998-06-30', gender='MALE',
    phone='010-2010-0004', address='부산광역시 해운대구', role='GENERAL',
    onboarded=true, is_deleted=false, deleted_at=NULL, updated_at=now_ts
WHERE user_id=v_user_id;

UPDATE auth_identities
SET password_hash='$2b$10$S6m7T1RhgG7iNfCEB6T8qO0OHd13b8v1rQFyrpAoeWcsc94PkEJjC',
    is_deleted=false, deleted_at=NULL, updated_at=now_ts
WHERE provider='LOCAL' AND email='junseo.choi@test.com';
ELSE
    WITH u AS (
      INSERT INTO users (name,birth,gender,phone,address,role,onboarded,created_at,updated_at,is_deleted,deleted_at)
      VALUES ('최준서', DATE '1998-06-30', 'MALE', '010-2010-0004', '부산광역시 해운대구', 'GENERAL', true, now_ts, now_ts, false, NULL)
      RETURNING user_id
    )
    INSERT INTO auth_identities (user_id,provider,provider_user_id,password_hash,email,created_at,updated_at,is_deleted,deleted_at)
    VALUES ((SELECT user_id FROM u), 'LOCAL', NULL,
            '$2b$10$S6m7T1RhgG7iNfCEB6T8qO0OHd13b8v1rQFyrpAoeWcsc94PkEJjC',
            'junseo.choi@test.com', now_ts, now_ts, false, NULL);
END IF;

  -- =======================================================
  -- Seed #5
  -- =======================================================
  IF EXISTS (SELECT 1 FROM auth_identities WHERE provider='LOCAL' AND email='hayeon.jung@test.com') THEN
SELECT user_id INTO v_user_id
FROM auth_identities
WHERE provider='LOCAL' AND email='hayeon.jung@test.com';

UPDATE users
SET name='정하연', birth=DATE '2001-11-08', gender='FEMALE',
    phone='010-2010-0005', address='대구광역시 수성구', role='GENERAL',
    onboarded=true, is_deleted=false, deleted_at=NULL, updated_at=now_ts
WHERE user_id=v_user_id;

UPDATE auth_identities
SET password_hash='$2b$10$gmfCGwUAhB5qj07w5G2wTOr4.6kzANXKcB0bK1Jv.Hj1wz7R/3xq6',
    is_deleted=false, deleted_at=NULL, updated_at=now_ts
WHERE provider='LOCAL' AND email='hayeon.jung@test.com';
ELSE
    WITH u AS (
      INSERT INTO users (name,birth,gender,phone,address,role,onboarded,created_at,updated_at,is_deleted,deleted_at)
      VALUES ('정하연', DATE '2001-11-08', 'FEMALE', '010-2010-0005', '대구광역시 수성구', 'GENERAL', true, now_ts, now_ts, false, NULL)
      RETURNING user_id
    )
    INSERT INTO auth_identities (user_id,provider,provider_user_id,password_hash,email,created_at,updated_at,is_deleted,deleted_at)
    VALUES ((SELECT user_id FROM u), 'LOCAL', NULL,
            '$2b$10$gmfCGwUAhB5qj07w5G2wTOr4.6kzANXKcB0bK1Jv.Hj1wz7R/3xq6',
            'hayeon.jung@test.com', now_ts, now_ts, false, NULL);
END IF;

  -- =======================================================
  -- Seed #6
  -- =======================================================
  IF EXISTS (SELECT 1 FROM auth_identities WHERE provider='LOCAL' AND email='yuna.han@test.com') THEN
SELECT user_id INTO v_user_id
FROM auth_identities
WHERE provider='LOCAL' AND email='yuna.han@test.com';

UPDATE users
SET name='한유나', birth=DATE '1996-04-19', gender='FEMALE',
    phone='010-2010-0006', address='대전광역시 서구', role='GENERAL',
    onboarded=true, is_deleted=false, deleted_at=NULL, updated_at=now_ts
WHERE user_id=v_user_id;

UPDATE auth_identities
SET password_hash='$2b$10$Pq0xUSUdy3nC4fCkYvXH4u8ZXOj3YgXW5cG8qzXn7H8mGdMLL6ahq',
    is_deleted=false, deleted_at=NULL, updated_at=now_ts
WHERE provider='LOCAL' AND email='yuna.han@test.com';
ELSE
    WITH u AS (
      INSERT INTO users (name,birth,gender,phone,address,role,onboarded,created_at,updated_at,is_deleted,deleted_at)
      VALUES ('한유나', DATE '1996-04-19', 'FEMALE', '010-2010-0006', '대전광역시 서구', 'GENERAL', true, now_ts, now_ts, false, NULL)
      RETURNING user_id
    )
    INSERT INTO auth_identities (user_id,provider,provider_user_id,password_hash,email,created_at,updated_at,is_deleted,deleted_at)
    VALUES ((SELECT user_id FROM u), 'LOCAL', NULL,
            '$2b$10$Pq0xUSUdy3nC4fCkYvXH4u8ZXOj3YgXW5cG8qzXn7H8mGdMLL6ahq',
            'yuna.han@test.com', now_ts, now_ts, false, NULL);
END IF;

  -- =======================================================
  -- Seed #7
  -- =======================================================
  IF EXISTS (SELECT 1 FROM auth_identities WHERE provider='LOCAL' AND email='taehyun.kang@test.com') THEN
SELECT user_id INTO v_user_id
FROM auth_identities
WHERE provider='LOCAL' AND email='taehyun.kang@test.com';

UPDATE users
SET name='강태현', birth=DATE '1995-12-05', gender='MALE',
    phone='010-2010-0007', address='광주광역시 북구', role='GENERAL',
    onboarded=true, is_deleted=false, deleted_at=NULL, updated_at=now_ts
WHERE user_id=v_user_id;

UPDATE auth_identities
SET password_hash='$2b$10$HkB9CTv8qPW7eR6nVtHcFu8kKTtFvJ6b2dZx0Qq2C8y3p2i5hPqQG',
    is_deleted=false, deleted_at=NULL, updated_at=now_ts
WHERE provider='LOCAL' AND email='taehyun.kang@test.com';
ELSE
    WITH u AS (
      INSERT INTO users (name,birth,gender,phone,address,role,onboarded,created_at,updated_at,is_deleted,deleted_at)
      VALUES ('강태현', DATE '1995-12-05', 'MALE', '010-2010-0007', '광주광역시 북구', 'GENERAL', true, now_ts, now_ts, false, NULL)
      RETURNING user_id
    )
    INSERT INTO auth_identities (user_id,provider,provider_user_id,password_hash,email,created_at,updated_at,is_deleted,deleted_at)
    VALUES ((SELECT user_id FROM u), 'LOCAL', NULL,
            '$2b$10$HkB9CTv8qPW7eR6nVtHcFu8kKTtFvJ6b2dZx0Qq2C8y3p2i5hPqQG',
            'taehyun.kang@test.com', now_ts, now_ts, false, NULL);
END IF;

  -- =======================================================
  -- Seed #8
  -- =======================================================
  IF EXISTS (SELECT 1 FROM auth_identities WHERE provider='LOCAL' AND email='sumin.yoo@test.com') THEN
SELECT user_id INTO v_user_id
FROM auth_identities
WHERE provider='LOCAL' AND email='sumin.yoo@test.com';

UPDATE users
SET name='유수민', birth=DATE '2002-07-27', gender='FEMALE',
    phone='010-2010-0008', address='울산광역시 남구', role='GENERAL',
    onboarded=true, is_deleted=false, deleted_at=NULL, updated_at=now_ts
WHERE user_id=v_user_id;

UPDATE auth_identities
SET password_hash='$2b$10$kVYtX0r2g3JjHpt6nD0lIuCzQ3gqjz9mXlN4bYh1Qb4xK3pVdA2Q2',
    is_deleted=false, deleted_at=NULL, updated_at=now_ts
WHERE provider='LOCAL' AND email='sumin.yoo@test.com';
ELSE
    WITH u AS (
      INSERT INTO users (name,birth,gender,phone,address,role,onboarded,created_at,updated_at,is_deleted,deleted_at)
      VALUES ('유수민', DATE '2002-07-27', 'FEMALE', '010-2010-0008', '울산광역시 남구', 'GENERAL', true, now_ts, now_ts, false, NULL)
      RETURNING user_id
    )
    INSERT INTO auth_identities (user_id,provider,provider_user_id,password_hash,email,created_at,updated_at,is_deleted,deleted_at)
    VALUES ((SELECT user_id FROM u), 'LOCAL', NULL,
            '$2b$10$kVYtX0r2g3JjHpt6nD0lIuCzQ3gqjz9mXlN4bYh1Qb4xK3pVdA2Q2',
            'sumin.yoo@test.com', now_ts, now_ts, false, NULL);
END IF;

  -- =======================================================
  -- Seed #9
  -- =======================================================
  IF EXISTS (SELECT 1 FROM auth_identities WHERE provider='LOCAL' AND email='donghyun.oh@test.com') THEN
SELECT user_id INTO v_user_id
FROM auth_identities
WHERE provider='LOCAL' AND email='donghyun.oh@test.com';

UPDATE users
SET name='오동현', birth=DATE '1994-03-12', gender='MALE',
    phone='010-2010-0009', address='경기도 성남시 분당구', role='GENERAL',
    onboarded=true, is_deleted=false, deleted_at=NULL, updated_at=now_ts
WHERE user_id=v_user_id;

UPDATE auth_identities
SET password_hash='$2b$10$3t3W2u5dUjVQ2A2n0xgN2u9Yf3DUPk4kQ4H4V3z3W2u5dUjVQ2A2n0',
    is_deleted=false, deleted_at=NULL, updated_at=now_ts
WHERE provider='LOCAL' AND email='donghyun.oh@test.com';
ELSE
    WITH u AS (
      INSERT INTO users (name,birth,gender,phone,address,role,onboarded,created_at,updated_at,is_deleted,deleted_at)
      VALUES ('오동현', DATE '1994-03-12', 'MALE', '010-2010-0009', '경기도 성남시 분당구', 'GENERAL', true, now_ts, now_ts, false, NULL)
      RETURNING user_id
    )
    INSERT INTO auth_identities (user_id,provider,provider_user_id,password_hash,email,created_at,updated_at,is_deleted,deleted_at)
    VALUES ((SELECT user_id FROM u), 'LOCAL', NULL,
            '$2b$10$3t3W2u5dUjVQ2A2n0xgN2u9Yf3DUPk4kQ4H4V3z3W2u5dUjVQ2A2n0',
            'donghyun.oh@test.com', now_ts, now_ts, false, NULL);
END IF;

  -- =======================================================
  -- Seed #10
  -- =======================================================
  IF EXISTS (SELECT 1 FROM auth_identities WHERE provider='LOCAL' AND email='jiwoo.song@test.com') THEN
SELECT user_id INTO v_user_id
FROM auth_identities
WHERE provider='LOCAL' AND email='jiwoo.song@test.com';

UPDATE users
SET name='송지우', birth=DATE '1999-10-01', gender='FEMALE',
    phone='010-2010-0010', address='제주특별자치도 제주시', role='GENERAL',
    onboarded=true, is_deleted=false, deleted_at=NULL, updated_at=now_ts
WHERE user_id=v_user_id;

UPDATE auth_identities
SET password_hash='$2b$10$2fVfYpB1wV8u1xgB7tQ0UO6o3MZJ8Y1x0g8yQ1Yb0x0c9eXw1mRZ.',
    is_deleted=false, deleted_at=NULL, updated_at=now_ts
WHERE provider='LOCAL' AND email='jiwoo.song@test.com';
ELSE
    WITH u AS (
      INSERT INTO users (name,birth,gender,phone,address,role,onboarded,created_at,updated_at,is_deleted,deleted_at)
      VALUES ('송지우', DATE '1999-10-01', 'FEMALE', '010-2010-0010', '제주특별자치도 제주시', 'GENERAL', true, now_ts, now_ts, false, NULL)
      RETURNING user_id
    )
    INSERT INTO auth_identities (user_id,provider,provider_user_id,password_hash,email,created_at,updated_at,is_deleted,deleted_at)
    VALUES ((SELECT user_id FROM u), 'LOCAL', NULL,
            '$2b$10$2fVfYpB1wV8u1xgB7tQ0UO6o3MZJ8Y1x0g8yQ1Yb0x0c9eXw1mRZ.',
            'jiwoo.song@test.com', now_ts, now_ts, false, NULL);
END IF;

END $$;
