CREATE INDEX IF NOT EXISTS idx_order_payments_received_status
    ON order_payments (received_at, status, order_id);

CREATE INDEX IF NOT EXISTS idx_order_payments_voided
    ON order_payments (voided_at, order_id)
    WHERE status = 'VOIDED';

CREATE INDEX IF NOT EXISTS idx_restaurant_orders_branch_closed_status
    ON restaurant_orders (branch_id, closed_at, status)
    WHERE closed_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_cash_register_sessions_branch_closed
    ON cash_register_sessions (branch_id, closed_at)
    WHERE closed_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_cash_register_movements_voided
    ON cash_register_movements (voided_at, session_id)
    WHERE status = 'VOIDED';
