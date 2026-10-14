-- Plus block 3: keep a per-preparation-item stock ledger and audit cancellations.
-- Existing tickets are left untouched; only new dispatches consume stock.
ALTER TABLE order_items ADD COLUMN cancelled_at TIMESTAMPTZ;

ALTER TABLE preparation_items
    ADD COLUMN cancel_reason VARCHAR(180),
    ADD COLUMN cancelled_by_user_id BIGINT REFERENCES app_users (id),
    ADD COLUMN cancelled_by_username VARCHAR(80);

ALTER TABLE inventory_movements
    DROP CONSTRAINT ck_inventory_movement_type;
ALTER TABLE inventory_movements
    ADD CONSTRAINT ck_inventory_movement_type CHECK (
        movement_type IN ('PURCHASE', 'USE', 'WASTE', 'ADJUSTMENT',
                          'ORDER_CONSUMPTION', 'ORDER_REVERSAL')
    );

ALTER TABLE inventory_movements
    ADD COLUMN preparation_item_id BIGINT REFERENCES preparation_items (id),
    ADD COLUMN reverses_movement_id BIGINT REFERENCES inventory_movements (id);

ALTER TABLE inventory_movements
    ADD CONSTRAINT ck_inventory_movement_source CHECK (
        (movement_type IN ('ORDER_CONSUMPTION', 'ORDER_REVERSAL')
            AND preparation_item_id IS NOT NULL)
        OR (movement_type NOT IN ('ORDER_CONSUMPTION', 'ORDER_REVERSAL')
            AND preparation_item_id IS NULL AND reverses_movement_id IS NULL)
    ),
    ADD CONSTRAINT ck_inventory_movement_reversal CHECK (
        (movement_type = 'ORDER_CONSUMPTION'
            AND quantity_delta < 0 AND reverses_movement_id IS NULL)
        OR (movement_type = 'ORDER_REVERSAL'
            AND quantity_delta > 0 AND reverses_movement_id IS NOT NULL)
        OR movement_type NOT IN ('ORDER_CONSUMPTION', 'ORDER_REVERSAL')
    );

CREATE UNIQUE INDEX uk_inventory_order_item_ingredient_type
    ON inventory_movements (preparation_item_id, ingredient_id, movement_type)
    WHERE preparation_item_id IS NOT NULL;
CREATE UNIQUE INDEX uk_inventory_reversed_movement
    ON inventory_movements (reverses_movement_id)
    WHERE reverses_movement_id IS NOT NULL;
CREATE INDEX idx_inventory_order_item
    ON inventory_movements (preparation_item_id)
    WHERE preparation_item_id IS NOT NULL;
