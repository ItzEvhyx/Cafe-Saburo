-- inventory_query.sql — Cafe Saburo POS: Inventory Reference Queries
-- Dialect: T-SQL (SQL Server) | Requires inventory_setup.sql to have been run first
-- REFERENCE / DEV USE ONLY: Run each block individually, never the entire file at once


-- Returns all active, non-deleted inventory items sorted alphabetically
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


-- Returns active items at or below their reorder level, sorted by most critical first
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


-- Returns all archived (non-deleted) inventory items
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


-- Inserts a new ingredient; the app handles this via the Add Ingredient modal — only run manually if needed
-- INSERT INTO dbo.Inventory (inventory_id, ingredient, quantity, unit, reorder_level)
-- VALUES ('INV-0051', 'Rose Syrup', 2.00, 'l', 1);


-- Updates quantity and reorder level for a specific item; the app handles this via Edit mode — only run manually if needed
-- UPDATE dbo.Inventory
-- SET    quantity      = 25,
--        reorder_level = 8
-- WHERE  inventory_id  = 'INV-0001'
--   AND  is_deleted    = 0;


-- Archives specific items by ID; the app handles this via Archive mode — only run manually if needed
-- UPDATE dbo.Inventory
-- SET    status = 'archived'
-- WHERE  inventory_id IN ('INV-0037', 'INV-0038')
--   AND  is_deleted   = 0;


-- Restores specific archived items back to active; the app handles this via Archive mode — only run manually if needed
-- UPDATE dbo.Inventory
-- SET    status = 'active'
-- WHERE  inventory_id IN ('INV-0037', 'INV-0038')
--   AND  is_deleted   = 0;


-- DANGER: Soft-deletes ALL rows in a given status, hiding them from the app; reverse with the query below
-- UPDATE dbo.Inventory
-- SET    is_deleted = 1
-- WHERE  is_deleted = 0
--   AND  status     = 'active';   -- change to 'archived' if needed


-- ADMIN RECOVERY: Restores all soft-deleted rows; run this if the query above was executed accidentally
-- UPDATE dbo.Inventory
-- SET    is_deleted = 0
-- WHERE  is_deleted = 1;


-- Returns a count of active inventory items grouped by unit type
SELECT
    unit,
    COUNT(*) AS item_count
FROM dbo.Inventory
WHERE is_deleted = 0
GROUP BY unit
ORDER BY item_count DESC;


-- Returns all active inventory items with a computed stock status label (Out of Stock / Low Stock / In Stock)
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