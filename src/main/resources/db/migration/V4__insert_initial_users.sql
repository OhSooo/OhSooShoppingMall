INSERT INTO users
(user_id, email, name, birth, gender, phone, address, role, created_at, updated_at, is_deleted, deleted_at)
VALUES
-- 스토어 OWNER (강아지 스토어)
(1,
 'dog_owner@shop.com',
 '강아지사장',
 '1990-05-10',
 'MALE',
 '010-1111-1111',
 '서울시 강남구',
 'OWNER',
 now(),
 now(),
 false,
 NULL
),

-- 스토어 OWNER (고양이 스토어)
(2,
 'cat_owner@shop.com',
 '고양이사장',
 '1992-08-22',
 'FEMALE',
 '010-2222-2222',
 '서울시 마포구',
 'OWNER',
 now(),
 now(),
 false,
 NULL
);
