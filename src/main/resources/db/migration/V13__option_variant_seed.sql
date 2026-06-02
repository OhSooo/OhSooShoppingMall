-- item 1: 프리미엄 강아지 사료 5kg (용량 옵션)
INSERT INTO options (item_id, type, value) VALUES
                                               (1, 'SIZE', '5KG'),
                                               (1, 'SIZE', '10KG');

-- item 2: 저알러지 강아지 사료 3kg
INSERT INTO options (item_id, type, value) VALUES
                                               (2, 'SIZE', '3KG'),
                                               (2, 'SIZE', '6KG');

-- item 7: 고양이 헤어볼 사료
INSERT INTO options (item_id, type, value) VALUES
                                               (7, 'SIZE', '2KG'),
                                               (7, 'SIZE', '5KG');

-- item 11: 벤토나이트 고양이 모래 10L
INSERT INTO options (item_id, type, value) VALUES
                                               (11, 'SIZE', '10L'),
                                               (11, 'SIZE', '20L');



INSERT INTO item_variants (item_id, sku, price, quantity, status) VALUES
                                              (1, 'DOG-FOOD-5KG', 45000.00, 40, 'ACTIVE'),
                                              (1, 'DOG-FOOD-10KG', 79000.00, 25, 'ACTIVE'),

                                              (2, 'DOG-FOOD-3KG', 38000.00, 30, 'ACTIVE'),
                                              (2, 'DOG-FOOD-6KG', 69000.00, 20, 'ACTIVE'),

                                              (7, 'CAT-FOOD-2KG', 42000.00, 45, 'ACTIVE'),
                                              (7, 'CAT-FOOD-5KG', 75000.00, 30, 'ACTIVE'),

                                              (11, 'CAT-SAND-10L', 22000.00, 60, 'ACTIVE'),
                                              (11, 'CAT-SAND-20L', 39000.00, 40, 'ACTIVE');




-- item 1
INSERT INTO item_variant_options (item_variant_id, option_id)
VALUES
    (
        (SELECT item_variant_id FROM item_variants WHERE sku = 'DOG-FOOD-5KG'),
        (SELECT option_id FROM options WHERE item_id = 1 AND type = 'SIZE' AND value = '5KG')
    ),
    (
        (SELECT item_variant_id FROM item_variants WHERE sku = 'DOG-FOOD-10KG'),
        (SELECT option_id FROM options WHERE item_id = 1 AND type = 'SIZE' AND value = '10KG')
    );

-- item 2
INSERT INTO item_variant_options (item_variant_id, option_id)
VALUES
    (
        (SELECT item_variant_id FROM item_variants WHERE sku = 'DOG-FOOD-3KG'),
        (SELECT option_id FROM options WHERE item_id = 2 AND type = 'SIZE' AND value = '3KG')
    ),
    (
        (SELECT item_variant_id FROM item_variants WHERE sku = 'DOG-FOOD-6KG'),
        (SELECT option_id FROM options WHERE item_id = 2 AND type = 'SIZE' AND value = '6KG')
    );

-- item 7
INSERT INTO item_variant_options (item_variant_id, option_id)
VALUES
    (
        (SELECT item_variant_id FROM item_variants WHERE sku = 'CAT-FOOD-2KG'),
        (SELECT option_id FROM options WHERE item_id = 7 AND type = 'SIZE' AND value = '2KG')
    ),
    (
        (SELECT item_variant_id FROM item_variants WHERE sku = 'CAT-FOOD-5KG'),
        (SELECT option_id FROM options WHERE item_id = 7 AND type = 'SIZE' AND value = '5KG')
    );

-- item 11
INSERT INTO item_variant_options (item_variant_id, option_id)
VALUES
    (
        (SELECT item_variant_id FROM item_variants WHERE sku = 'CAT-SAND-10L'),
        (SELECT option_id FROM options WHERE item_id = 11 AND type = 'SIZE' AND value = '10L')
    ),
    (
        (SELECT item_variant_id FROM item_variants WHERE sku = 'CAT-SAND-20L'),
        (SELECT option_id FROM options WHERE item_id = 11 AND type = 'SIZE' AND value = '20L')
    );
