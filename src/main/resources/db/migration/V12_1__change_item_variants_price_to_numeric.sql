ALTER TABLE item_variants
ALTER COLUMN price TYPE NUMERIC(12,2) USING price::numeric;