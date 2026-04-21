-- Price is now managed at the variant level; the legacy price column on
-- menu_items no longer needs a > 0 constraint and should allow NULL.
ALTER TABLE menu_items
    DROP CONSTRAINT IF EXISTS menu_items_price_check,
    ALTER COLUMN price DROP NOT NULL;
