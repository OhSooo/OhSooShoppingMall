-- =========================================================
-- V8__fix_users_sequence.sql
--
-- 목적:
-- - users.user_id 시퀀스 정합성 보정 (PK 중복 방지)
-- =========================================================

SELECT setval(
               pg_get_serial_sequence('users', 'user_id'),
               (SELECT COALESCE(MAX(user_id), 1) FROM users),
               true
       );
