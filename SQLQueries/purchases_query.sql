-- purchases_query.sql  |  Cafe Saburo POS — Purchases CRUD Queries  |  T-SQL (SQL Server)
-- Run AFTER purchases_setup.sql.

SET QUOTED_IDENTIFIER ON;
GO

-- ── 1. READ: Active purchases joined with Inventory to resolve ingredient name ──────────────────
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

-- ── 1b. READ: Archived purchases only ───────────────────────────────────────────────────────────
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

-- ── 1c. READ: Full join across Suppliers + Inventory for reporting and future UI use ────────────
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

-- ── 2a. INSERT (Step 1): Get the current max numeric suffix to generate the next purchase_id ────
-- Java: newId = String.format("PUR-%04d", maxNum + 1)
SELECT
    MAX(CAST(SUBSTRING(purchase_id, 5, LEN(purchase_id)) AS INT)) AS max_num
FROM dbo.Purchases
WHERE is_deleted = 0;
GO

-- ── 2b. INSERT (Step 2): Get the current max group suffix to generate the next purchase_group_id ─
-- Java: newGroupId = String.format("PG-%03d", maxGrp + 1)
SELECT
    MAX(CAST(SUBSTRING(purchase_group_id, 4, LEN(purchase_group_id)) AS INT)) AS max_grp
FROM dbo.Purchases
WHERE is_deleted = 0;
GO

-- ── 2c. INSERT (Step 3): Insert the new purchase row; ingredient is resolved at read time via JOIN
-- Params: purchase_id, purchase_group_id, supplier_id, inventory_id, quantity_ordered, order_date, status
INSERT INTO dbo.Purchases
    (purchase_id, purchase_group_id, supplier_id, inventory_id,
     quantity_ordered, order_date, [status])
VALUES
    (?, ?, ?, ?, ?, ?, ?);
GO

-- ── 3. UPDATE: Change the status of a single purchase (only editable field) ────────────────────
-- Valid statuses: 'Pending' | 'Approved' | 'On its way' | 'Delivered' | 'Cancelled'
-- Params: status, purchase_id
UPDATE dbo.Purchases
SET [status] = ?
WHERE purchase_id = ?
  AND is_deleted  = 0;
GO

-- ── 4a. ARCHIVE: Move a single purchase to archived status ──────────────────────────────────────
UPDATE dbo.Purchases
SET [status] = 'archived'
WHERE purchase_id = ?
  AND is_deleted  = 0;
GO

-- ── 4b. RESTORE: Restore a single archived purchase back to Pending ─────────────────────────────
UPDATE dbo.Purchases
SET [status] = 'Pending'
WHERE purchase_id = ?
  AND is_deleted  = 0;
GO

-- ── 4c. ARCHIVE ALL: Archive every non-deleted active purchase in bulk ──────────────────────────
UPDATE dbo.Purchases
SET [status] = 'archived'
WHERE is_deleted = 0
  AND [status] <> 'archived';
GO

-- ── 4d. RESTORE ALL: Restore every archived purchase back to Pending in bulk ───────────────────
UPDATE dbo.Purchases
SET [status] = 'Pending'
WHERE is_deleted = 0
  AND [status]   = 'archived';
GO

-- ── 5a. SOFT DELETE: Remove all visible rows matching the current tab's status ─────────────────
-- Param: status (the tab currently open in the UI)
UPDATE dbo.Purchases
SET is_deleted = 1
WHERE is_deleted = 0
  AND [status]   = ?;
GO

-- ── 5b. SOFT DELETE: Remove a single purchase by ID ────────────────────────────────────────────
UPDATE dbo.Purchases
SET is_deleted = 1
WHERE purchase_id = ?;
GO

-- ── 6. EXPORT: Full flat row with all columns for CSV/report export ─────────────────────────────
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

-- ── 7. HELPER: Load active suppliers for the Make an Order modal dropdown ──────────────────────
SELECT supplier_id, supplier_name
FROM dbo.Suppliers
WHERE is_deleted = 0 AND [status] = 'active'
ORDER BY supplier_name ASC;
GO

-- ── 8. HELPER: Load inventory items linked to a given supplier for the item dropdown ────────────
-- Param: supplier_id
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