-- =========================================================
-- V5__create_auth_identities_and_refactor_users.sql
--
-- 목적:
-- - 인증 정보 분리: users.email 제거
-- - auth_identities 테이블 생성
-- - users 프로필 컬럼 nullable 완화 (온보딩/마이페이지에서 입력)
-- =========================================================

-- 1) auth_identities 생성
CREATE TABLE auth_identities (
                                 auth_identities_id BIGSERIAL PRIMARY KEY,
                                 user_id BIGINT NOT NULL,
                                 provider VARCHAR(20) NOT NULL,
                                 provider_user_id VARCHAR(255),
                                 password_hash VARCHAR(255),
                                 email VARCHAR(255),
                                 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 is_deleted BOOLEAN NOT NULL DEFAULT false,
                                 deleted_at TIMESTAMPTZ,

                                 CONSTRAINT fk_auth_identity_user
                                     FOREIGN KEY (user_id) REFERENCES users(user_id),

                                 CONSTRAINT chk_auth_provider
                                     CHECK (provider IN ('LOCAL', 'GOOGLE', 'NAVER', 'KAKAO')),

                                 CONSTRAINT uk_auth_user_provider
                                     UNIQUE (user_id, provider),

                                 CONSTRAINT uk_auth_provider_user_id
                                     UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_auth_identities_user_id ON auth_identities(user_id);

-- 2) users.email 제거
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
ALTER TABLE users DROP COLUMN IF EXISTS email;

-- 3) users 프로필 컬럼 nullable 완화
ALTER TABLE users
    ALTER COLUMN name DROP NOT NULL,
ALTER COLUMN birth DROP NOT NULL,
    ALTER COLUMN gender DROP NOT NULL,
    ALTER COLUMN phone DROP NOT NULL,
    ALTER COLUMN address DROP NOT NULL;

-- 4) role 보장
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'GENERAL';
UPDATE users SET role = 'GENERAL' WHERE role IS NULL;
ALTER TABLE users ALTER COLUMN role SET NOT NULL;
