-- suppliers_query.sql  |  Cafe Saburo POS — Suppliers CRUD Queries  |  T-SQL (SQL Server)
-- All READ queries target dbo.vw_Suppliers (not the base table) — the view handles STRING_AGG.
-- Java fetchSuppliers() receives: supplier_id | supplier_name | ingredients | contact_info | address
-- Run AFTER suppliers_setup.sql.

SET QUOTED_IDENTIFIER ON;
GO

-- ── 1a. READ: Active suppliers from the view (ingredients already aggregated) ──────────────────
SELECT
    supplier_id,
    supplier_name,
    ingredients,
    contact_info,
    address
FROM  dbo.vw_Suppliers
WHERE is_deleted = 0
  AND [status]   = 'active'
ORDER BY supplier_name ASC;
GO

-- ── 1b. READ: Archived suppliers only ───────────────────────────────────────────────────────────
SELECT
    supplier_id,
    supplier_name,
    ingredients,
    contact_info,
    address
FROM  dbo.vw_Suppliers
WHERE is_deleted = 0
  AND [status]   = 'archived'
ORDER BY supplier_name ASC;
GO

-- ── 2a. INSERT (Step 1): Get current max suffix to generate the next supplier_id ────────────────
-- Java: newId = String.format("SUP-%04d", maxNum + 1)
SELECT
    MAX(CAST(SUBSTRING(supplier_id, 5, LEN(supplier_id)) AS INT)) AS max_num
FROM dbo.Suppliers
WHERE is_deleted = 0;
GO

-- ── 2b. INSERT (Step 2): Insert the supplier header row ─────────────────────────────────────────
-- Params: supplier_id, supplier_name, contact_info, address
INSERT INTO dbo.Suppliers (supplier_id, supplier_name, contact_info, address)
VALUES (?, ?, ?, ?);

-- ── 2c. INSERT (Step 3): Link each selected inventory item in the junction table (repeat per item)
-- Params: supplier_id, inventory_id
INSERT INTO dbo.Supplier_Ingredients (supplier_id, inventory_id)
VALUES (?, ?);
GO

-- ── 3. UPDATE: Edit supplier header fields only; junction rows handled separately in section 4 ──
-- Params: supplier_name, contact_info, address, supplier_id
UPDATE dbo.Suppliers
SET
    supplier_name = ?,
    contact_info  = ?,
    address       = ?
WHERE supplier_id = ?
  AND is_deleted  = 0;
GO

-- ── 4a. MANAGE INGREDIENTS: Add a single ingredient link to the junction table ─────────────────
INSERT INTO dbo.Supplier_Ingredients (supplier_id, inventory_id)
VALUES (?, ?);
GO

-- ── 4b. MANAGE INGREDIENTS: Remove a single ingredient link from the junction table ────────────
DELETE FROM dbo.Supplier_Ingredients
WHERE supplier_id  = ?
  AND inventory_id = ?;
GO

-- ── 4c. MANAGE INGREDIENTS: Replace all ingredient links in one operation (delete then re-insert)
-- Step 1: wipe existing links; Step 2: re-insert each inventory_id from the updated list
DELETE FROM dbo.Supplier_Ingredients WHERE supplier_id = ?;

INSERT INTO dbo.Supplier_Ingredients (supplier_id, inventory_id)
VALUES (?, ?);
GO

-- ── 4d. MANAGE INGREDIENTS: Fetch current ingredient links to pre-populate the edit modal ───────
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

-- ── 5a. ARCHIVE: Move a single supplier to archived status; junction rows preserved for restore ─
UPDATE dbo.Suppliers
SET [status] = 'archived'
WHERE supplier_id = ?
  AND is_deleted  = 0;
GO

-- ── 5b. RESTORE: Restore a single archived supplier back to active ───────────────────────────────
UPDATE dbo.Suppliers
SET [status] = 'active'
WHERE supplier_id = ?
  AND is_deleted  = 0;
GO

-- ── 5c. ARCHIVE ALL: Archive every non-deleted active supplier in bulk ───────────────────────────
UPDATE dbo.Suppliers
SET [status] = 'archived'
WHERE is_deleted = 0
  AND [status]   = 'active';
GO

-- ── 5d. RESTORE ALL: Restore every archived supplier back to active in bulk ─────────────────────
UPDATE dbo.Suppliers
SET [status] = 'active'
WHERE is_deleted = 0
  AND [status]   = 'archived';
GO

-- ── 6a. SOFT DELETE: Remove all visible rows matching the current tab's status ─────────────────
-- Param: status (the tab currently open in the UI)
UPDATE dbo.Suppliers
SET is_deleted = 1
WHERE is_deleted = 0
  AND [status]   = ?;
GO

-- ── 6b. SOFT DELETE: Remove a single supplier; junction rows left intact (unreachable via view) ─
UPDATE dbo.Suppliers
SET is_deleted = 1
WHERE supplier_id = ?;
GO

-- ── 7. EXPORT: Full flat row including status for CSV export ─────────────────────────────────────
SELECT
    supplier_id,
    supplier_name,
    ingredients,
    contact_info,
    address,
    [status]
FROM  dbo.vw_Suppliers
WHERE is_deleted = 0
ORDER BY supplier_name ASC;
GO