-- Root Categories (depth = 0)
INSERT INTO categories (category_id, parent_id, name, depth, display_order, is_active)
VALUES
    (1, NULL, '강아지', 0, 1, true),
    (2, NULL, '고양이', 0, 2, true),
    (3, NULL, '기타', 0, 3, true);

-- Dog Categories (depth = 1)
INSERT INTO categories (category_id, parent_id, name, depth, display_order, is_active)
VALUES
    (4, 1, '사료', 1, 1, true),
    (5, 1, '간식', 1, 2, true),
    (6, 1, '장난감', 1, 3, true),
    (7, 1, '의류', 1, 4, true);

-- Cat Categories (depth = 1)
INSERT INTO categories (category_id, parent_id, name, depth, display_order, is_active)
VALUES
    (8, 2, '사료', 1, 1, true),
    (9, 2, '간식', 1, 2, true),
    (10, 2, '모래', 1, 3, true),
    (11, 2, '장난감', 1, 4, true);

-- Etc Categories (depth = 1)
INSERT INTO categories (category_id, parent_id, name, depth, display_order, is_active)
VALUES
    (12, 3, '위생용품', 1, 1, true),
    (13, 3, '이동용품', 1, 2, true);
