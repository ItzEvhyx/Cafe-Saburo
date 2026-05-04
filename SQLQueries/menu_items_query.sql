-- menu_items_query.sql — Cafe Saburo POS: Menu Items Queries (used by menu_items_util.java)
-- Dialect: T-SQL (SQL Server) | Run menu_items_setup.sql first
-- REFERENCE / DEV USE ONLY: Run each block individually, never the entire file at once
-- Placeholders: Java uses ? for JDBC PreparedStatement params; concrete example values are used here for SSMS testing
-- display_price logic: ISNULL() prevents NULL from collapsing the CASE result; LOWER(LTRIM(RTRIM())) handles casing/spacing variants in the sizes column


-- Diagnostic: shows raw sizes values alongside the normalized form and computed display_price — run this first to catch spacing/casing issues before running fetch queries
SELECT
    item_id,
    item_name,
    sizes,
    price_small,
    price_large,
    price,
    is_archived,
    LOWER(LTRIM(RTRIM(sizes))) AS sizes_normalised,
    CASE
        WHEN LOWER(LTRIM(RTRIM(sizes))) = 'small, large'
            THEN CAST(CAST(ISNULL(price_small, 0) AS INT) AS VARCHAR)
               + ' / '
               + CAST(CAST(ISNULL(price_large, 0) AS INT) AS VARCHAR)
        ELSE
            CAST(CAST(ISNULL(price, 0) AS INT) AS VARCHAR)
    END AS display_price
FROM dbo.menu_items
ORDER BY item_id;


-- Fetches active menu items for table display, ordered by category then name; maps to cachedRows: [item_id, item_name, sizes, display_price]
SELECT
    item_id,
    item_name,
    sizes,
    CASE
        WHEN LOWER(LTRIM(RTRIM(sizes))) = 'small, large'
            THEN CAST(CAST(ISNULL(price_small, 0) AS INT) AS VARCHAR)
               + ' / '
               + CAST(CAST(ISNULL(price_large, 0) AS INT) AS VARCHAR)
        ELSE
            CAST(CAST(ISNULL(price, 0) AS INT) AS VARCHAR)
    END AS display_price
FROM dbo.menu_items
WHERE is_archived = 0
ORDER BY
    CASE category
        WHEN 'Espresso'         THEN 1
        WHEN 'Specialty Coffee' THEN 2
        WHEN 'Iced Coffee'      THEN 3
        WHEN 'Frappe'           THEN 4
        WHEN 'Matcha Series'    THEN 5
        WHEN 'Smoothie'         THEN 6
        WHEN 'Refresher'        THEN 7
        WHEN 'Add On'           THEN 8
        ELSE                         9
    END,
    item_name ASC;


-- Fetches archived menu items for the Archived tab, same display_price logic and sort order as active fetch
SELECT
    item_id,
    item_name,
    sizes,
    CASE
        WHEN LOWER(LTRIM(RTRIM(sizes))) = 'small, large'
            THEN CAST(CAST(ISNULL(price_small, 0) AS INT) AS VARCHAR)
               + ' / '
               + CAST(CAST(ISNULL(price_large, 0) AS INT) AS VARCHAR)
        ELSE
            CAST(CAST(ISNULL(price, 0) AS INT) AS VARCHAR)
    END AS display_price
FROM dbo.menu_items
WHERE is_archived = 1
ORDER BY
    CASE category
        WHEN 'Espresso'         THEN 1
        WHEN 'Specialty Coffee' THEN 2
        WHEN 'Iced Coffee'      THEN 3
        WHEN 'Frappe'           THEN 4
        WHEN 'Matcha Series'    THEN 5
        WHEN 'Smoothie'         THEN 6
        WHEN 'Refresher'        THEN 7
        WHEN 'Add On'           THEN 8
        ELSE                         9
    END,
    item_name ASC;


-- Searches active items across item_id, item_name, sizes, and category; replace '%caramel%' with the actual search term
SELECT
    item_id,
    item_name,
    sizes,
    CASE
        WHEN LOWER(LTRIM(RTRIM(sizes))) = 'small, large'
            THEN CAST(CAST(ISNULL(price_small, 0) AS INT) AS VARCHAR)
               + ' / '
               + CAST(CAST(ISNULL(price_large, 0) AS INT) AS VARCHAR)
        ELSE
            CAST(CAST(ISNULL(price, 0) AS INT) AS VARCHAR)
    END AS display_price
FROM dbo.menu_items
WHERE is_archived = 0
  AND (
        item_id   LIKE '%caramel%'
     OR item_name LIKE '%caramel%'
     OR sizes     LIKE '%caramel%'
     OR category  LIKE '%caramel%'
  )
ORDER BY item_name ASC;


-- Searches archived items using the same multi-column LIKE logic; replace '%caramel%' with the actual search term
SELECT
    item_id,
    item_name,
    sizes,
    CASE
        WHEN LOWER(LTRIM(RTRIM(sizes))) = 'small, large'
            THEN CAST(CAST(ISNULL(price_small, 0) AS INT) AS VARCHAR)
               + ' / '
               + CAST(CAST(ISNULL(price_large, 0) AS INT) AS VARCHAR)
        ELSE
            CAST(CAST(ISNULL(price, 0) AS INT) AS VARCHAR)
    END AS display_price
FROM dbo.menu_items
WHERE is_archived = 1
  AND (
        item_id   LIKE '%caramel%'
     OR item_name LIKE '%caramel%'
     OR sizes     LIKE '%caramel%'
     OR category  LIKE '%caramel%'
  )
ORDER BY item_name ASC;


-- Inserts a One Size item; pass NULL for price_small and price_large, set price to the flat amount
INSERT INTO dbo.menu_items
    (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature)
VALUES
    ('MI055', 'Sample Drink', 'Specialty Coffee', 'One Size', NULL, NULL, 175.00, 0, 0);

-- Inserts a Small/Large item; pass NULL for price, set price_small and price_large — note 'Small, Large' requires a space after the comma
INSERT INTO dbo.menu_items
    (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature)
VALUES
    ('MI056', 'Sample Espresso', 'Espresso', 'Small, Large', 110.00, 120.00, NULL, 0, 0);


-- Updates the item name only for a single active item; replace 'MI001' and 'New Name' as needed
UPDATE dbo.menu_items
SET item_name = 'New Name'
WHERE item_id = 'MI001'
  AND is_archived = 0;

-- Updates the flat price for a One Size item only; skips Small/Large rows to prevent accidental overwrites
UPDATE dbo.menu_items
SET price = 155.00
WHERE item_id = 'MI003'
  AND LOWER(LTRIM(RTRIM(sizes))) <> 'small, large'
  AND is_archived = 0;

-- Updates both size prices together for a Small/Large item
UPDATE dbo.menu_items
SET price_small = 115.00,
    price_large = 120.00
WHERE item_id = 'MI001'
  AND LOWER(LTRIM(RTRIM(sizes))) = 'small, large'
  AND is_archived = 0;

-- Full update from the Add/Edit modal; replaces all editable fields for the target item
UPDATE dbo.menu_items
SET item_name   = 'Updated Drink Name',
    sizes       = 'One Size',
    price_small = NULL,
    price_large = NULL,
    price       = 180.00
WHERE item_id = 'MI005';


-- Archives selected items by ID, skipping any already archived
UPDATE dbo.menu_items
SET is_archived = 1
WHERE item_id IN ('MI001', 'MI002', 'MI003')
  AND is_archived = 0;

-- Restores selected archived items back to active
UPDATE dbo.menu_items
SET is_archived = 0
WHERE item_id IN ('MI001', 'MI002', 'MI003')
  AND is_archived = 1;

-- ADMIN: Archives all currently active items at once
UPDATE dbo.menu_items
SET is_archived = 1
WHERE is_archived = 0;

-- ADMIN: Restores all archived items at once
UPDATE dbo.menu_items
SET is_archived = 0
WHERE is_archived = 1;


-- DANGER: Permanently deletes all active items — this cannot be undone
DELETE FROM dbo.menu_items WHERE is_archived = 0;

-- DANGER: Permanently deletes all archived items — this cannot be undone
DELETE FROM dbo.menu_items WHERE is_archived = 1;

-- Permanently deletes a single item by ID
DELETE FROM dbo.menu_items WHERE item_id = 'MI001';


-- Exports all columns for active items in display order; change is_archived = 1 to export the archived tab instead
SELECT
    item_id,
    item_name,
    category,
    sizes,
    price_small,
    price_large,
    price,
    is_add_on,
    is_signature,
    is_archived,
    created_at
FROM dbo.menu_items
WHERE is_archived = 0
ORDER BY
    CASE category
        WHEN 'Espresso'         THEN 1
        WHEN 'Specialty Coffee' THEN 2
        WHEN 'Iced Coffee'      THEN 3
        WHEN 'Frappe'           THEN 4
        WHEN 'Matcha Series'    THEN 5
        WHEN 'Smoothie'         THEN 6
        WHEN 'Refresher'        THEN 7
        WHEN 'Add On'           THEN 8
        ELSE                         9
    END,
    item_name ASC;


-- Computes the next available item_id by incrementing the highest existing numeric suffix
SELECT 'MI' + RIGHT('000' + CAST(
    COALESCE(MAX(CAST(SUBSTRING(item_id, 3, LEN(item_id)) AS INT)), 0) + 1
AS VARCHAR), 3) AS next_item_id
FROM dbo.menu_items;

-- Fetches all columns for a single item by ID
SELECT * FROM dbo.menu_items WHERE item_id = 'MI001';

-- Returns a summary count of active vs archived items
SELECT
    SUM(CASE WHEN is_archived = 0 THEN 1 ELSE 0 END) AS active_count,
    SUM(CASE WHEN is_archived = 1 THEN 1 ELSE 0 END) AS archived_count
FROM dbo.menu_items;

-- Returns all distinct categories in display order
SELECT DISTINCT category,
    CASE category
        WHEN 'Espresso'         THEN 1
        WHEN 'Specialty Coffee' THEN 2
        WHEN 'Iced Coffee'      THEN 3
        WHEN 'Frappe'           THEN 4
        WHEN 'Matcha Series'    THEN 5
        WHEN 'Smoothie'         THEN 6
        WHEN 'Refresher'        THEN 7
        WHEN 'Add On'           THEN 8
        ELSE                         9
    END AS sort_order
FROM dbo.menu_items
ORDER BY sort_order;

-- Checks all distinct sizes values and their row counts — if variants like 'Small,Large' (no space) appear here, fix the data or widen the CASE condition to match
SELECT DISTINCT sizes, COUNT(*) AS row_count
FROM dbo.menu_items
GROUP BY sizes
ORDER BY sizes;