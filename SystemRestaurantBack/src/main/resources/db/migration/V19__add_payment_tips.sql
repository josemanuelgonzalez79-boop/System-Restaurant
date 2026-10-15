-- Tips are collected with a payment, but never reduce the order balance or increase sales.
ALTER TABLE order_payments
    ADD COLUMN tip_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN tip_beneficiary_user_id BIGINT REFERENCES app_users (id),
    ADD COLUMN tip_beneficiary_name VARCHAR(120),
    ADD CONSTRAINT ck_payment_tip_nonnegative CHECK (tip_amount >= 0),
    ADD CONSTRAINT ck_payment_tip_beneficiary CHECK (
        (tip_amount = 0 AND tip_beneficiary_user_id IS NULL AND tip_beneficiary_name IS NULL)
        OR (tip_amount > 0 AND tip_beneficiary_user_id IS NOT NULL
            AND tip_beneficiary_name IS NOT NULL AND LENGTH(TRIM(tip_beneficiary_name)) > 0)
    );

-- V7 required electronic tendered_amount = amount. Electronic tips need the
-- full charge; cash must cover both amounts before change can be returned.
ALTER TABLE order_payments DROP CONSTRAINT ck_order_payments_non_cash_tendered;
ALTER TABLE order_payments ADD CONSTRAINT ck_order_payments_tip_tendered
    CHECK (
        (method = 'CASH' AND tendered_amount >= amount + tip_amount)
        OR (method <> 'CASH' AND tendered_amount = amount + tip_amount)
    );

CREATE INDEX idx_payment_tip_beneficiary
    ON order_payments (tip_beneficiary_user_id, received_at)
    WHERE tip_amount > 0;
