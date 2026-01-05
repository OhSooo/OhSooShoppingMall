CREATE TABLE stores (
                        store_id BIGSERIAL PRIMARY KEY,
                        owner_id BIGINT NOT NULL,
                        name VARCHAR(255) NOT NULL,
                        description VARCHAR(255),
                        created_at TIMESTAMP NOT NULL DEFAULT now()
);
