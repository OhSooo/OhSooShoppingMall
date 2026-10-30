-- =========================
-- Coupon 1차 기능: 쿠폰 정책 / 사용자 발급 쿠폰 테이블
-- =========================

CREATE TABLE coupons (
    coupon_id             BIGSERIAL PRIMARY KEY,
    name                  VARCHAR(255)   NOT NULL,
    discount_type         VARCHAR(30)    NOT NULL,
    discount_value        NUMERIC(19, 2) NOT NULL,
    min_order_amount      NUMERIC(19, 2),
    max_discount_amount   NUMERIC(19, 2),
    issue_start_at        TIMESTAMPTZ    NOT NULL,
    issue_end_at          TIMESTAMPTZ    NOT NULL,
    use_start_at          TIMESTAMPTZ    NOT NULL,
    use_end_at            TIMESTAMPTZ    NOT NULL,
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT chk_coupons_discount_type CHECK (discount_type IN ('FIXED_AMOUNT', 'PERCENTAGE')),
    CONSTRAINT chk_coupons_discount_value CHECK (discount_value > 0),
    CONSTRAINT chk_coupons_min_order_amount CHECK (min_order_amount IS NULL OR min_order_amount >= 0),
    CONSTRAINT chk_coupons_max_discount_amount CHECK (max_discount_amount IS NULL OR max_discount_amount > 0),
    CONSTRAINT chk_coupons_issue_period CHECK (issue_start_at < issue_end_at),
    CONSTRAINT chk_coupons_use_period CHECK (use_start_at < use_end_at)
);

CREATE TABLE user_coupons (
    user_coupon_id          BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT         NOT NULL REFERENCES users(user_id),
    coupon_id                BIGINT         NOT NULL REFERENCES coupons(coupon_id),
    status                   VARCHAR(20)    NOT NULL,
    issued_at                TIMESTAMPTZ    NOT NULL,
    used_at                  TIMESTAMPTZ,
    used_order_amount        NUMERIC(19, 2),
    discount_applied_amount  NUMERIC(19, 2),
    used_order_id            BIGINT,
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT uq_user_coupon UNIQUE (user_id, coupon_id),
    CONSTRAINT chk_user_coupons_status CHECK (status IN ('AVAILABLE', 'USED', 'EXPIRED'))
);

CREATE INDEX idx_user_coupons_user_status ON user_coupons(user_id, status);
CREATE INDEX idx_coupons_issue_period ON coupons(issue_start_at, issue_end_at);
