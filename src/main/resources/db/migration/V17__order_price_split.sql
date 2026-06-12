ALTER TABLE orders RENAME COLUMN total_price TO original_total_price;

ALTER TABLE orders
    ADD COLUMN discount_amount  NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN delivery_fee     NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN final_price      NUMERIC(19, 2) NOT NULL DEFAULT 0;

UPDATE orders SET final_price = original_total_price;
