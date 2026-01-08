CREATE TABLE stores (
                        store_id BIGSERIAL PRIMARY KEY,
                        owner_id BIGINT NOT NULL,
                        name VARCHAR(255) NOT NULL,
                        description VARCHAR(255),
                        status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                        created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        CONSTRAINT chk_store_status
                            CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED'))
);