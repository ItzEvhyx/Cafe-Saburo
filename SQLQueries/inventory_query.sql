-- ============================================================
--  SQLQueries/inventory_query.sql
--  Cafe Saburo POS — Inventory Reference Queries
--  Dialect: T-SQL (SQL Server)
--
--  !! REFERENCE / DEV USE ONLY !!
--  These are individual queries for development, debugging,
--  and manual admin tasks. Run each block INDIVIDUALLY —
--  never execute this entire file at once.
--
--  Requires inventory_setup.sql to have been run first.
-- ============================================================


-- ============================================================
--  QUERY 1 — Active, non-deleted inventory (default view)
-- ============================================================

SELECT
    inventory_id,
    ingredient,
    quantity,
    unit,
    reorder_level
FROM dbo.Inventory
WHERE is_deleted = 0
  AND status     = 'active'
ORDER BY ingredient ASC;


-- ============================================================
--  QUERY 2 — Low-stock alert (quantity at or below reorder level)
-- ============================================================

SELECT
    inventory_id,
    ingredient,
    quantity,
    unit,
    reorder_level
FROM dbo.Inventory
WHERE is_deleted    = 0
  AND status        = 'active'
  AND quantity     <= reorder_level
ORDER BY quantity ASC;


-- ============================================================
--  QUERY 3 — Archived inventory items
-- ============================================================

SELECT
    inventory_id,
    ingredient,
    quantity,
    unit,
    reorder_level
FROM dbo.Inventory
WHERE is_deleted = 0
  AND status     = 'archived'
ORDER BY ingredient ASC;


-- ============================================================
--  QUERY 4 — Insert a new ingredient (example)
--  NOTE: The app handles this automatically via Add Ingredient modal.
--        Only run manually if needed.
-- ============================================================

-- INSERT INTO dbo.Inventory (inventory_id, ingredient, quantity, unit, reorder_level)
-- VALUES ('INV-0051', 'Rose Syrup', 2.00, 'l', 1);


-- ============================================================
--  QUERY 5 — Update quantity and reorder level (example)
--  NOTE: The app handles this automatically via Edit mode.
--        Only run manually if needed.
-- ============================================================

-- UPDATE dbo.Inventory
-- SET    quantity      = 25,
--        reorder_level = 8
-- WHERE  inventory_id  = 'INV-0001'
--   AND  is_deleted    = 0;


-- ============================================================
--  QUERY 6 — Archive selected items (example)
--  NOTE: The app handles this automatically via Archive mode.
--        Only run manually if needed.
-- ============================================================

-- UPDATE dbo.Inventory
-- SET    status = 'archived'
-- WHERE  inventory_id IN ('INV-0037', 'INV-0038')
--   AND  is_deleted   = 0;


-- ============================================================
--  QUERY 7 — Restore archived items back to active (example)
--  NOTE: The app handles this automatically via Archive mode.
--        Only run manually if needed.
-- ============================================================

-- UPDATE dbo.Inventory
-- SET    status = 'active'
-- WHERE  inventory_id IN ('INV-0037', 'INV-0038')
--   AND  is_deleted   = 0;


-- ============================================================
--  QUERY 8 — Soft delete all items in a given status (ADMIN)
--  !! DANGER: This hides ALL rows from the app. !!
--  Only run intentionally. Reverse with QUERY 9 below.
-- ============================================================

-- UPDATE dbo.Inventory
-- SET    is_deleted = 1
-- WHERE  is_deleted = 0
--   AND  status     = 'active';   -- change to 'archived' if needed


-- ============================================================
--  QUERY 9 — Restore all soft-deleted rows (ADMIN recovery)
--  Run this if you accidentally executed Query 8.
-- ============================================================

-- UPDATE dbo.Inventory
-- SET    is_deleted = 0
-- WHERE  is_deleted = 1;


-- ============================================================
--  QUERY 10 — COUNT by unit type
-- ============================================================

SELECT
    unit,
    COUNT(*) AS item_count
FROM dbo.Inventory
WHERE is_deleted = 0
GROUP BY unit
ORDER BY item_count DESC;


-- ============================================================
--  QUERY 11 — Full inventory report with stock status label
-- ============================================================

SELECT
    inventory_id,
    ingredient,
    quantity,
    unit,
    reorder_level,
    CASE
        WHEN quantity = 0              THEN 'Out of Stock'
        WHEN quantity <= reorder_level THEN 'Low Stock'
        ELSE                               'In Stock'
    END AS stock_status
FROM dbo.Inventory
WHERE is_deleted = 0
  AND status     = 'active'
ORDER BY stock_status ASC, ingredient ASC;