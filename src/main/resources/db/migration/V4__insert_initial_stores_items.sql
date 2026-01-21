INSERT INTO stores
(store_id, owner_id, name, description, status, created_at, updated_at)
VALUES
-- 강아지 용품 스토어 (owner: user_id = 1)
(1,
 1,
 '강아지 용품 스토어',
 '강아지를 위한 사료, 간식, 장난감 전문 스토어',
 'ACTIVE',
 now(),
 now()
),

-- 고양이 용품 스토어 (owner: user_id = 2)
(2,
 2,
 '고양이 용품 스토어',
 '고양이를 위한 사료, 간식, 모래 전문 스토어',
 'ACTIVE',
 now(),
 now()
);



-- =========================
-- ITEM SEED
-- store_id 가정:
-- 1 = 강아지 스토어
-- 2 = 고양이 스토어
-- =========================

INSERT INTO items
(item_id, store_id, category_id, name, status, base_price, rating, review_count, created_at, updated_at, is_deleted, deleted_at)
VALUES
-- 강아지 > 사료
(1, 1, 4, '프리미엄 강아지 사료 5kg', 'ACTIVE', 45000, 4.5, 120, now(), now(), false, NULL),
(2, 1, 4, '저알러지 강아지 사료 3kg', 'ACTIVE', 38000, 4.3, 85,  now(), now(), false, NULL),

-- 강아지 > 간식
(3, 1, 5, '닭가슴살 트릿',        'ACTIVE', 12000, 4.7, 210, now(), now(), false, NULL),
(4, 1, 5, '소고기 육포 간식',      'ACTIVE', 15000, 4.6, 180, now(), now(), false, NULL),

-- 강아지 > 장난감
(5, 1, 6, '고무 치발기 장난감',     'ACTIVE',  9000, 4.2, 60,  now(), now(), false, NULL),
(6, 1, 6, '노즈워크 장난감',        'ACTIVE', 17000, 4.8, 140, now(), now(), false, NULL),

-- 고양이 > 사료
(7, 2, 8, '고양이 헤어볼 사료',     'ACTIVE', 42000, 4.6, 155, now(), now(), false, NULL),
(8, 2, 8, '그레인프리 고양이 사료', 'ACTIVE', 48000, 4.7, 98,  now(), now(), false, NULL),

-- 고양이 > 간식
(9,  2, 9, '참치 큐브 간식',        'ACTIVE', 11000, 4.5, 170, now(), now(), false, NULL),
(10, 2, 9, '연어 스틱 간식',        'ACTIVE', 13000, 4.6, 140, now(), now(), false, NULL),

-- 고양이 > 모래
(11, 2, 10, '벤토나이트 고양이 모래 10L', 'ACTIVE', 22000, 4.4, 200, now(), now(), false, NULL),
(12, 2, 10, '두부 고양이 모래',           'ACTIVE', 25000, 4.5, 165, now(), now(), false, NULL),

-- 기타 > 위생용품
(13, 1, 12, '반려동물 전용 샴푸',           'ACTIVE', 18000, 4.3, 75,  now(), now(), false, NULL),
(14, 2, 12, '강아지/고양이 겸용 탈취제',    'ACTIVE', 16000, 4.4, 90,  now(), now(), false, NULL);
