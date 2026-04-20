-- Menu categories
CREATE TABLE menu_categories (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id     UUID         NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    name          VARCHAR(100) NOT NULL,
    display_order INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_menu_categories_vendor_id ON menu_categories(vendor_id);

-- Menu variants (purchasable unit of a menu item)
CREATE TABLE menu_variants (
    id            UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_item_id  UUID           NOT NULL REFERENCES menu_items(id) ON DELETE CASCADE,
    label         VARCHAR(100),
    price         DECIMAL(10, 2) NOT NULL CHECK (price > 0),
    is_available  BOOLEAN        NOT NULL DEFAULT true,
    display_order INTEGER        NOT NULL DEFAULT 0,
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_menu_variants_menu_item_id ON menu_variants(menu_item_id);

-- Extend menu_items
ALTER TABLE menu_items
    ADD COLUMN category_id   UUID    REFERENCES menu_categories(id) ON DELETE SET NULL,
    ADD COLUMN description   TEXT,
    ADD COLUMN is_veg        BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN display_order INTEGER NOT NULL DEFAULT 0;

-- Backfill: create one unlabelled variant per existing item (price + availability from item)
INSERT INTO menu_variants (id, menu_item_id, label, price, is_available, display_order)
SELECT gen_random_uuid(), id, NULL, price, is_available, 0
FROM menu_items;

-- Extend order_items to capture which variant was ordered
ALTER TABLE order_items
    ADD COLUMN variant_id    UUID         REFERENCES menu_variants(id),
    ADD COLUMN variant_label VARCHAR(100);
