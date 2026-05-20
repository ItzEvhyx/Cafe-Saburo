-- suppliers_query.sql  |  Cafe Saburo POS — Suppliers CRUD Queries  |  T-SQL (SQL Server)
-- All READ queries target dbo.vw_Suppliers (not the base table) — the view handles STRING_AGG.
-- Java fetchSuppliers() receives: supplier_id | supplier_name | ingredient_name | contact_info | address | inventory_id
-- Run AFTER suppliers_setup.sql.

SET QUOTED_IDENTIFIER ON;
GO

-- ── 1a. READ: Active suppliers — one row per ingredient (1NF) ────────────────────────────────────
--  fetchSuppliers() uses this query (not the view) so ingredient_name is atomic per row.
--  The JOIN goes through si.ingredient_id (not inv.ingredient_id) because ingredient_id
--  is the authoritative FK stored in Supplier_Ingredients.
SELECT
    s.supplier_id,
    s.supplier_name,
    ISNULL(ing.ingredient_name, N'—') AS ingredient_name,
    s.contact_info,
    s.address,
    ISNULL(si.inventory_id, N'')      AS inventory_id
FROM      dbo.Suppliers            s
LEFT JOIN dbo.Supplier_Ingredients si  ON si.supplier_id   = s.supplier_id
LEFT JOIN dbo.Inventory            inv ON inv.inventory_id  = si.inventory_id
                                      AND inv.is_deleted   = 0
LEFT JOIN dbo.Ingredients          ing ON ing.ingredient_id = si.ingredient_id   -- FIX: was inv.ingredient_id
                                      AND ing.is_deleted   = 0
WHERE s.is_deleted = 0
  AND s.[status]   = 'active'
ORDER BY s.supplier_name ASC, ing.ingredient_name ASC;
GO

-- ── 1b. READ: Archived suppliers only ───────────────────────────────────────────────────────────
SELECT
    s.supplier_id,
    s.supplier_name,
    ISNULL(ing.ingredient_name, N'—') AS ingredient_name,
    s.contact_info,
    s.address,
    ISNULL(si.inventory_id, N'')      AS inventory_id
FROM      dbo.Suppliers            s
LEFT JOIN dbo.Supplier_Ingredients si  ON si.supplier_id   = s.supplier_id
LEFT JOIN dbo.Inventory            inv ON inv.inventory_id  = si.inventory_id
                                      AND inv.is_deleted   = 0
LEFT JOIN dbo.Ingredients          ing ON ing.ingredient_id = si.ingredient_id   -- FIX: was inv.ingredient_id
                                      AND ing.is_deleted   = 0
WHERE s.is_deleted = 0
  AND s.[status]   = 'archived'
ORDER BY s.supplier_name ASC, ing.ingredient_name ASC;
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
GO

-- ── 2c. INSERT (Step 3): Link each selected inventory item in the junction table ────────────────
--  FIX: ingredient_id MUST be included — it is part of the composite PK (supplier_id, ingredient_id)
--  and is the FK used by fetchSuppliers() to JOIN dbo.Ingredients for the ingredient name.
--  The Java layer resolves ingredient_id from the inventory list returned by fetchAllActive():
--    row[0] = inventory_id,  row[1] = ingredient_name,  row[2] = ingredient_id
--  Params: supplier_id, ingredient_id, inventory_id
INSERT INTO dbo.Supplier_Ingredients (supplier_id, ingredient_id, inventory_id)
VALUES (?, ?, ?);
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
--  FIX: include ingredient_id (composite PK column)
--  Params: supplier_id, ingredient_id, inventory_id
INSERT INTO dbo.Supplier_Ingredients (supplier_id, ingredient_id, inventory_id)
VALUES (?, ?, ?);
GO

-- ── 4b. MANAGE INGREDIENTS: Remove a single ingredient link from the junction table ────────────
DELETE FROM dbo.Supplier_Ingredients
WHERE supplier_id  = ?
  AND inventory_id = ?;
GO

-- ── 4c. MANAGE INGREDIENTS: Replace all ingredient links in one operation (delete then re-insert)
-- Step 1: wipe existing links
DELETE FROM dbo.Supplier_Ingredients WHERE supplier_id = ?;

-- Step 2: re-insert each pair — Params: supplier_id, ingredient_id, inventory_id
INSERT INTO dbo.Supplier_Ingredients (supplier_id, ingredient_id, inventory_id)
VALUES (?, ?, ?);
GO

-- ── 4d. MANAGE INGREDIENTS: Fetch current ingredient links to pre-populate the edit modal ───────
--  FIX: JOIN on si.ingredient_id (not i.ingredient_id from Inventory) — Inventory no longer
--  carries ingredient_id directly; it is stored in Supplier_Ingredients and Ingredients.
SELECT
    si.inventory_id,
    si.ingredient_id,
    ing.ingredient_name,
    inv.unit
FROM  dbo.Supplier_Ingredients si
JOIN  dbo.Inventory            inv ON inv.inventory_id  = si.inventory_id   AND inv.is_deleted = 0
JOIN  dbo.Ingredients          ing ON ing.ingredient_id = si.ingredient_id  AND ing.is_deleted = 0
WHERE si.supplier_id = ?
ORDER BY ing.ingredient_name ASC;
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