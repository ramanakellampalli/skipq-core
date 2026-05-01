-- Replace menu_categories FK with a plain category string on menu_items.
-- Categories are now hardcoded on the frontend; no DB table needed.

-- 1. Add new category string column (copy name from joined category where it exists)
ALTER TABLE menu_items ADD COLUMN category VARCHAR(100);

UPDATE menu_items mi
SET category = (
    SELECT mc.name
    FROM menu_categories mc
    WHERE mc.id = mi.category_id
)
WHERE mi.category_id IS NOT NULL;

-- 2. Drop category_id column (CASCADE drops the auto-named FK constraint with it)
ALTER TABLE menu_items DROP COLUMN category_id CASCADE;

-- 3. Drop menu_categories table (items have already been migrated above)
DROP TABLE IF EXISTS menu_categories;
