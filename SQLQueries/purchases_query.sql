-- ============================================================
--  SQLQueries/purchases_query.sql
--  Cafe Saburo POS — Purchases CRUD Queries
--  Dialect: T-SQL (SQL Server)
--
--  Run AFTER purchases_setup.sql.
--
--  Sections:
--    1. READ   — fetch active / archived purchases (with joins)
--    2. INSERT — create a new purchase line item
--    3. UPDATE — edit fields / change status
--    4. ARCHIVE / RESTORE
--    5. SOFT DELETE
--    6. EXPORT helper — all columns flat
-- ============================================================

SET QUOTED_IDENTIFIER ON;
GO

-- ============================================================
--  1.  READ — used by fetchPurchases(tab) in Java
--      Returns columns in the order the Java code expects:
--        purchase_id | supplier_id | inventory_id | ingredient | order_date | status
--
--      NOTE: ingredient is resolved via JOIN with dbo.Inventory.
--            No schema change to dbo.Purchases is required.
-- ============================================================

-- Active purchases (Pending, Approved, On its way, Delivered, Cancelled)
SELECT
    p.purchase_id,
    p.supplier_id,
    p.inventory_id,
    ISNULL(i.ingredient, '—') AS ingredient,
    CONVERT(VARCHAR(10), p.order_date, 120) AS order_date,
    p.[status]
FROM dbo.Purchases p
LEFT JOIN dbo.Inventory i ON i.inventory_id = p.inventory_id AND i.is_deleted = 0
WHERE p.is_deleted = 0
  AND p.[status] <> 'archived'
ORDER BY p.order_date DESC, p.purchase_id ASC;

GO

-- Archived purchases
SELECT
    p.purchase_id,
    p.supplier_id,
    p.inventory_id,
    ISNULL(i.ingredient, '—') AS ingredient,
    CONVERT(VARCHAR(10), p.order_date, 120) AS order_date,
    p.[status]
FROM dbo.Purchases p
LEFT JOIN dbo.Inventory i ON i.inventory_id = p.inventory_id AND i.is_deleted = 0
WHERE p.is_deleted = 0
  AND p.[status] = 'archived'
ORDER BY p.order_date DESC, p.purchase_id ASC;

GO

-- ── Rich join view (for reporting / future UI expansion) ────
SELECT
    p.purchase_id,
    p.purchase_group_id,
    p.supplier_id,
    s.supplier_name,
    p.inventory_id,
    i.ingredient,
    p.quantity_ordered,
    i.unit,
    CONVERT(VARCHAR(10), p.order_date, 120) AS order_date,
    p.[status]
FROM  dbo.Purchases  p
JOIN  dbo.Suppliers  s ON s.supplier_id  = p.supplier_id
JOIN  dbo.Inventory  i ON i.inventory_id = p.inventory_id
WHERE p.is_deleted = 0
ORDER BY p.order_date DESC, p.purchase_group_id, p.purchase_id;

GO

-- ============================================================
--  2.  INSERT — generate next purchase_id, then insert
-- ============================================================

-- Step 1: get next numeric suffix
SELECT
    MAX(CAST(SUBSTRING(purchase_id, 5, LEN(purchase_id)) AS INT)) AS max_num
FROM dbo.Purchases
WHERE is_deleted = 0;
-- Java: newId = String.format("PUR-%04d", maxNum + 1)

-- Step 2: get next group_id suffix
SELECT
    MAX(CAST(SUBSTRING(purchase_group_id, 4, LEN(purchase_group_id)) AS INT)) AS max_grp
FROM dbo.Purchases
WHERE is_deleted = 0;
-- Java: newGroupId = String.format("PG-%03d", maxGrp + 1)

-- Step 3: insert the new row
-- Parameters: purchase_id, purchase_group_id, supplier_id,
--             inventory_id, quantity_ordered, order_date, status
-- ingredient is NOT stored here — it is resolved at read time via JOIN.
INSERT INTO dbo.Purchases
    (purchase_id, purchase_group_id, supplier_id, inventory_id,
     quantity_ordered, order_date, [status])
VALUES
    (?, ?, ?, ?, ?, ?, ?);

GO

-- ============================================================
--  3.  UPDATE — status-only update (the only editable field)
--      Valid values: 'Pending' | 'Approved' | 'On its way'
--                    | 'Delivered' | 'Cancelled'
-- ============================================================

UPDATE dbo.Purchases
SET [status] = ?
WHERE purchase_id = ?
  AND is_deleted  = 0;

GO

-- ============================================================
--  4.  ARCHIVE / RESTORE
-- ============================================================

-- Archive a single purchase
UPDATE dbo.Purchases
SET [status] = 'archived'
WHERE purchase_id = ?
  AND is_deleted  = 0;

GO

-- Restore a single purchase to Pending (safe default)
UPDATE dbo.Purchases
SET [status] = 'Pending'
WHERE purchase_id = ?
  AND is_deleted  = 0;

GO

-- Archive ALL active purchases
UPDATE dbo.Purchases
SET [status] = 'archived'
WHERE is_deleted = 0
  AND [status] <> 'archived';

GO

-- Restore ALL archived purchases
UPDATE dbo.Purchases
SET [status] = 'Pending'
WHERE is_deleted = 0
  AND [status]   = 'archived';

GO

-- ============================================================
--  5.  SOFT DELETE
-- ============================================================

-- Delete all visible rows in current tab
UPDATE dbo.Purchases
SET is_deleted = 1
WHERE is_deleted = 0
  AND [status]   = ?;

GO

-- Delete a single purchase
UPDATE dbo.Purchases
SET is_deleted = 1
WHERE purchase_id = ?;

GO

-- ============================================================
--  6.  EXPORT HELPER
-- ============================================================

SELECT
    p.purchase_id,
    p.purchase_group_id,
    p.supplier_id,
    s.supplier_name,
    p.inventory_id,
    i.ingredient,
    p.quantity_ordered,
    i.unit,
    CONVERT(VARCHAR(10), p.order_date, 120) AS order_date,
    p.[status]
FROM  dbo.Purchases  p
JOIN  dbo.Suppliers  s ON s.supplier_id  = p.supplier_id
JOIN  dbo.Inventory  i ON i.inventory_id = p.inventory_id
WHERE p.is_deleted = 0
ORDER BY p.order_date DESC, p.purchase_group_id, p.purchase_id;

GO

-- ============================================================
--  7.  HELPER — load suppliers for Make an Order modal dropdown
-- ============================================================

SELECT supplier_id, supplier_name
FROM dbo.Suppliers
WHERE is_deleted = 0 AND [status] = 'active'
ORDER BY supplier_name ASC;

GO

-- ============================================================
--  8.  HELPER — load inventory items for a given supplier
--      (used to populate the item dropdown after supplier is chosen)
--      Parameter: supplier_id
-- ============================================================

SELECT
    si.inventory_id,
    i.ingredient,
    i.unit
FROM  dbo.Supplier_Ingredients si
JOIN  dbo.Inventory             i  ON i.inventory_id = si.inventory_id
                                   AND i.is_deleted  = 0
WHERE si.supplier_id = ?
ORDER BY i.ingredient ASC;

GO