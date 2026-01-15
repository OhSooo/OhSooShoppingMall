-- V1__init_schema.sql
-- 쇼핑몰 서비스 초기 스키마
-- 사용자, 상품, 주문, 리뷰 도메인 정의

-- =========================
-- Users
-- =========================
CREATE TABLE users (
                       user_id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       name VARCHAR(100) NOT NULL,
                       birth DATE NOT NULL,
                       gender VARCHAR(10) NOT NULL,
                       phone VARCHAR(20) NOT NULL,
                       address VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL DEFAULT 'GENERAL',
                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at TIMESTAMPTZ,
                       is_deleted BOOLEAN NOT NULL DEFAULT false,
                       deleted_at TIMESTAMPTZ,
                       CONSTRAINT chk_user_role CHECK (role IN ('GENERAL', 'OWNER', 'ADMIN')),
                       CONSTRAINT chk_gender
                           CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'))
);

COMMENT ON TABLE users IS '서비스 사용자 정보';

COMMENT ON COLUMN users.user_id IS '사용자 PK';
COMMENT ON COLUMN users.email IS '로그인 이메일';
COMMENT ON COLUMN users.name IS '사용자 이름';
COMMENT ON COLUMN users.birth IS '생년월일';
COMMENT ON COLUMN users.gender IS '성별';
COMMENT ON COLUMN users.phone IS '전화번호';
COMMENT ON COLUMN users.address IS '주소';
COMMENT ON COLUMN users.role IS '사용자 역할(GENERAL, OWNER, ADMIN)';
COMMENT ON COLUMN users.created_at IS '생성 시각';
COMMENT ON COLUMN users.updated_at IS '수정 시각';
COMMENT ON COLUMN users.is_deleted IS '논리 삭제 여부';
COMMENT ON COLUMN users.deleted_at IS '삭제 시각';

-- =========================
-- Categories
-- =========================
CREATE TABLE categories (
                            category_id BIGSERIAL PRIMARY KEY,
                            parent_id BIGINT,
                            name VARCHAR(100) NOT NULL,
                            depth INT NOT NULL,
                            display_order INT NOT NULL,
                            is_active BOOLEAN NOT NULL DEFAULT true,
                            CONSTRAINT fk_category_parent
                                FOREIGN KEY (parent_id)
                                    REFERENCES categories(category_id)
                                    ON DELETE SET NULL
);

COMMENT ON TABLE categories IS '상품 카테고리';

COMMENT ON COLUMN categories.parent_id IS '부모 카테고리 ID';
COMMENT ON COLUMN categories.depth IS '카테고리 깊이';
COMMENT ON COLUMN categories.display_order IS '노출 순서';
COMMENT ON COLUMN categories.is_active IS '사용 여부';

-- =========================
-- Stores
-- =========================
CREATE TABLE stores (
                        store_id BIGSERIAL PRIMARY KEY,
                        owner_id BIGINT NOT NULL,
                        name VARCHAR(255) NOT NULL,
                        description VARCHAR(255),
                        status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                        created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        updated_at TIMESTAMPTZ,
                        CONSTRAINT chk_store_status
                            CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED')),
                        CONSTRAINT fk_store_owner
                            FOREIGN KEY (owner_id) REFERENCES users(user_id)
);

COMMENT ON TABLE stores IS '스토어 정보';

COMMENT ON COLUMN stores.owner_id IS '스토어 소유자(user_id)';
COMMENT ON COLUMN stores.status IS '스토어 상태';

-- =========================
-- Items
-- =========================
CREATE TABLE items (
                       item_id BIGSERIAL PRIMARY KEY,
                       store_id BIGINT NOT NULL,
                       category_id BIGINT NOT NULL,
                       name VARCHAR(255) NOT NULL,
                       status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                       base_price INT NOT NULL DEFAULT 0,
                       rating FLOAT NOT NULL DEFAULT 0.0,
                       review_count INT NOT NULL DEFAULT 0,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at TIMESTAMPTZ,
                       is_deleted BOOLEAN NOT NULL DEFAULT false,
                       deleted_at TIMESTAMPTZ,
                       CONSTRAINT chk_item_status
                           CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
                       CONSTRAINT fk_item_store
                           FOREIGN KEY (store_id) REFERENCES stores(store_id),
                       CONSTRAINT fk_item_category
                           FOREIGN KEY (category_id) REFERENCES categories(category_id)
);

COMMENT ON TABLE items IS '상품 기본 정보';

COMMENT ON COLUMN items.store_id IS '소속 스토어 ID';
COMMENT ON COLUMN items.category_id IS '카테고리 ID';
COMMENT ON COLUMN items.base_price IS '기본 가격';
COMMENT ON COLUMN items.rating IS '평균 평점';
COMMENT ON COLUMN items.review_count IS '리뷰 수';

-- =========================
-- Options
-- =========================
CREATE TABLE options (
                         option_id BIGSERIAL PRIMARY KEY,
                         item_id BIGINT NOT NULL,
                         type VARCHAR(20) NOT NULL,
                         value VARCHAR(100) NOT NULL,
                         CONSTRAINT chk_option_type
                             CHECK (type IN ('SIZE', 'COLOR')),
                         CONSTRAINT fk_option_item
                             FOREIGN KEY (item_id) REFERENCES items(item_id)
);

COMMENT ON TABLE options IS '상품 옵션 정의';

COMMENT ON COLUMN options.type IS '옵션 타입(SIZE, COLOR)';
COMMENT ON COLUMN options.value IS '옵션 값';

-- =========================
-- Item_variants
-- =========================
CREATE TABLE item_variants (
                               item_variant_id BIGSERIAL PRIMARY KEY,
                               item_id BIGINT NOT NULL,
                               sku VARCHAR(100) NOT NULL UNIQUE,
                               price INT NOT NULL DEFAULT 0,
                               quantity INT NOT NULL DEFAULT 0,
                               status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                               CONSTRAINT chk_variant_status
                                   CHECK (status IN ('ACTIVE', 'OUT_OF_STOCK', 'DISABLED')),
                               CONSTRAINT fk_variant_item
                                   FOREIGN KEY (item_id) REFERENCES items(item_id)
);

COMMENT ON TABLE item_variants IS '상품 옵션 조합(판매 단위)';

COMMENT ON COLUMN item_variants.sku IS '상품 SKU';
COMMENT ON COLUMN item_variants.quantity IS '재고 수량';
COMMENT ON COLUMN item_variants.status IS '판매 상태';

-- =========================
-- Carts
-- =========================
CREATE TABLE carts (
                       cart_id BIGSERIAL PRIMARY KEY,
                       user_id BIGINT NOT NULL,
                       CONSTRAINT fk_cart_user
                           FOREIGN KEY (user_id) REFERENCES users(user_id),
                       CONSTRAINT uq_cart_user UNIQUE (user_id)
);

COMMENT ON TABLE carts IS '사용자 장바구니';

COMMENT ON COLUMN carts.user_id IS '장바구니 소유 사용자';


-- =========================
-- Orders
-- =========================
CREATE TABLE orders (
                        order_id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        total_price INT NOT NULL DEFAULT 0,
                        status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
                        created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                        CONSTRAINT chk_order_status
                            CHECK (
                                status IN (
                                           'CREATED', 'PAID',
                                           'PARTIALLY_CANCELED',
                                           'PARTIALLY_REFUNDED',
                                           'COMPLETED'
                                    )
                                ),
                        CONSTRAINT fk_order_user
                            FOREIGN KEY (user_id) REFERENCES users(user_id)
);

COMMENT ON TABLE orders IS '주문 정보';

COMMENT ON COLUMN orders.status IS '주문 상태';
COMMENT ON COLUMN orders.total_price IS '주문 총액';


-- =========================
-- Order_items
-- =========================
CREATE TABLE order_items (
                             order_item_id BIGSERIAL PRIMARY KEY,
                             order_id BIGINT NOT NULL,
                             item_variant_id BIGINT NOT NULL,
                             quantity INT NOT NULL DEFAULT 1,
                             price_at_purchase INT NOT NULL DEFAULT 0,
                             status VARCHAR(30) NOT NULL DEFAULT 'ORDERED',
                             CONSTRAINT chk_order_item_status
                                 CHECK (
                                     status
                                         IN (
                                             'ORDERED', 'SHIPPED', 'DELIVERED',
                                             'CANCEL_REQUESTED', 'CANCELED',
                                             'REFUND_REQUESTED', 'REFUNDED'
                                         )
                                     ),
                             CONSTRAINT fk_order_item_order
                                 FOREIGN KEY (order_id) REFERENCES orders(order_id),
                             CONSTRAINT fk_order_item_variant
                                 FOREIGN KEY (item_variant_id) REFERENCES item_variants(item_variant_id)
);

COMMENT ON TABLE order_items IS '주문 상품';

COMMENT ON COLUMN order_items.price_at_purchase IS '구매 당시 가격';
COMMENT ON COLUMN order_items.status IS '주문 상품 상태';

-- =========================
-- Order_item_history
-- =========================
CREATE TABLE order_item_history (
                                    order_item_history_id BIGSERIAL PRIMARY KEY,
                                    order_item_id BIGINT NOT NULL,
                                    previous_status VARCHAR(30) NOT NULL,
                                    new_status VARCHAR(30) NOT NULL,
                                    changed_by VARCHAR(20) NOT NULL,
                                    changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                                    CONSTRAINT chk_order_item_history_prev_status
                                        CHECK (
                                            previous_status IN (
                                                                'ORDERED', 'SHIPPED', 'DELIVERED',
                                                                'CANCEL_REQUESTED', 'CANCELED',
                                                                'REFUND_REQUESTED', 'REFUNDED'
                                                )
                                            ),

                                    CONSTRAINT chk_order_item_history_new_status
                                        CHECK (
                                            new_status IN (
                                                           'ORDERED', 'SHIPPED', 'DELIVERED',
                                                           'CANCEL_REQUESTED', 'CANCELED',
                                                           'REFUND_REQUESTED', 'REFUNDED'
                                                )
                                            ),

                                    CONSTRAINT chk_changed_by
                                        CHECK (changed_by IN ('GENERAL', 'OWNER', 'ADMIN')),

                                    CONSTRAINT fk_history_order_item
                                        FOREIGN KEY (order_item_id)
                                            REFERENCES order_items(order_item_id),

                                    CONSTRAINT chk_status_changed
                                        CHECK (previous_status <> new_status)
);

COMMENT ON TABLE order_item_history IS '주문 상품 상태 변경 이력';

COMMENT ON COLUMN order_item_history.previous_status IS '변경 전 상태';
COMMENT ON COLUMN order_item_history.new_status IS '변경 후 상태';
COMMENT ON COLUMN order_item_history.changed_by IS '변경 주체';


-- =========================
-- Reviews
-- =========================
CREATE TABLE reviews (
                         review_id BIGSERIAL PRIMARY KEY,
                         order_item_id BIGINT NOT NULL,
                         user_id BIGINT NOT NULL,
                         rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
                         title VARCHAR(255) NOT NULL,
                         contents TEXT,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                         updated_at TIMESTAMPTZ,
                         is_deleted BOOLEAN NOT NULL DEFAULT false,
                         deleted_at TIMESTAMPTZ,
                         CONSTRAINT fk_review_order_item
                             FOREIGN KEY (order_item_id)
                                 REFERENCES order_items(order_item_id),
                         CONSTRAINT fk_review_user
                             FOREIGN KEY (user_id)
                                 REFERENCES users(user_id)
);

COMMENT ON TABLE reviews IS '상품 리뷰';

COMMENT ON COLUMN reviews.rating IS '리뷰 평점(1~5)';

-- =========================
-- Review_media
-- =========================
CREATE TABLE review_media (
                              review_media_id BIGSERIAL PRIMARY KEY,
                              review_id BIGINT NOT NULL,
                              media_url VARCHAR(255) NOT NULL,
                              type VARCHAR(20) NOT NULL,
                              display_order INT NOT NULL DEFAULT 0,
                              CONSTRAINT chk_review_media_type
                                  CHECK (type IN ('PICTURE', 'VIDEO')),
                              CONSTRAINT fk_review_media_review
                                  FOREIGN KEY (review_id) REFERENCES reviews(review_id)
);

COMMENT ON TABLE review_media IS '리뷰 미디어';

COMMENT ON COLUMN review_media.media_url IS '미디어 URL';

-- =========================
-- Cart_items
-- =========================
CREATE TABLE cart_items (
                            cart_item_id BIGSERIAL PRIMARY KEY,
                            cart_id BIGINT NOT NULL,
                            item_variant_id BIGINT NOT NULL,
                            quantity INT NOT NULL DEFAULT 1,

                            CONSTRAINT fk_cart_item_cart
                                FOREIGN KEY (cart_id)
                                    REFERENCES carts(cart_id),

                            CONSTRAINT fk_cart_item_variant
                                FOREIGN KEY (item_variant_id)
                                    REFERENCES item_variants(item_variant_id),

                            CONSTRAINT uq_cart_item
                                UNIQUE (cart_id, item_variant_id)
);


COMMENT ON TABLE cart_items IS '장바구니 상품';

COMMENT ON COLUMN cart_items.quantity IS '담긴 수량';


-- =========================
-- Item_variant_options
-- =========================
CREATE TABLE item_variant_options (
                                      item_variant_option_id BIGSERIAL PRIMARY KEY,
                                      item_variant_id BIGINT NOT NULL,
                                      option_id BIGINT NOT NULL,
                                      CONSTRAINT fk_ivo_variant
                                          FOREIGN KEY (item_variant_id) REFERENCES item_variants(item_variant_id),
                                      CONSTRAINT fk_ivo_option
                                          FOREIGN KEY (option_id) REFERENCES options(option_id),
                                      CONSTRAINT uq_variant_option UNIQUE (item_variant_id, option_id)
);

-- =========================
-- fk 인덱스
-- =========================
CREATE INDEX idx_items_store_id ON items(store_id);
CREATE INDEX idx_items_category_id ON items(category_id);

CREATE INDEX idx_item_variants_item_id ON item_variants(item_id);

CREATE INDEX idx_options_item_id ON options(item_id);

CREATE INDEX idx_orders_user_id ON orders(user_id);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_item_variant_id ON order_items(item_variant_id);

CREATE INDEX idx_reviews_order_item_id ON reviews(order_item_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);

CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);
CREATE INDEX idx_cart_items_item_variant_id ON cart_items(item_variant_id);
