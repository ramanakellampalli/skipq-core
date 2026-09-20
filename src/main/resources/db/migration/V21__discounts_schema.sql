-- Vendor discounts: first-class discount entities with item-level application
CREATE TABLE discounts (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id  UUID         NOT NULL REFERENCES vendors(id),
    name       VARCHAR(100) NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    value      NUMERIC(10,2) NOT NULL,
    scope      VARCHAR(20)  NOT NULL DEFAULT 'ITEM',
    active     BOOLEAN      NOT NULL DEFAULT true,
    priority   INTEGER      NOT NULL DEFAULT 0,
    starts_at  TIMESTAMP,
    ends_at    TIMESTAMP,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at TIMESTAMP    NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP,

    CONSTRAINT discounts_type_valid     CHECK (type  IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT discounts_scope_valid    CHECK (scope IN ('ITEM', 'ORDER')),
    CONSTRAINT discounts_value_positive CHECK (value > 0),
    CONSTRAINT discounts_pct_max        CHECK (type != 'PERCENTAGE' OR value <= 100),
    CONSTRAINT discounts_date_range     CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at > starts_at)
);

-- Join table: which menu items a discount applies to
CREATE TABLE menu_item_discounts (
    menu_item_id UUID NOT NULL REFERENCES menu_items(id) ON DELETE CASCADE,
    discount_id  UUID NOT NULL REFERENCES discounts(id) ON DELETE CASCADE,
    PRIMARY KEY (menu_item_id, discount_id)
);

CREATE INDEX idx_discounts_vendor_id ON discounts(vendor_id);
CREATE INDEX idx_discounts_active    ON discounts(active, starts_at, ends_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_mid_menu_item_id    ON menu_item_discounts(menu_item_id);
