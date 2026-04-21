-- ============================================================
--  SQLQueries/suppliers_query.sql
--  Cafe Saburo POS — Suppliers CRUD Queries  (v3)
--  Dialect: T-SQL (SQL Server)
--
--  Run AFTER suppliers_setup.sql (v3).
--
--  Key change from v2:
--    All READ queries now target dbo.vw_Suppliers instead of
--    the base table.  The view already computes the
--    `ingredients` column via STRING_AGG, so Java receives
--    the same 5-column result set it always expected:
--      supplier_id | supplier_name | ingredients | contact_info | address
--
--  !! JAVA CHANGE REQUIRED !!
--    In suppliers_contents.java, change the FROM clause of
--    your fetchSuppliers SELECT from  dbo.Suppliers  to
--    dbo.vw_Suppliers  (or use the queries below verbatim).
--
--  Sections:
--    1. READ   — fetch active / archived suppliers
--    2. INSERT — add supplier + junction rows
--    3. UPDATE — edit supplier fields
--    4. MANAGE INGREDIENTS — add / remove junction rows
--    5. ARCHIVE / RESTORE
--    6. SOFT DELETE
--    7. EXPORT helper
-- ============================================================

SET QUOTED_IDENTIFIER ON;
GO

-- ============================================================
--  1.  READ — used by fetchSuppliers(tab) in Java
--
--      Returns columns in the order the Java code expects:
--        supplier_id | supplier_name | ingredients | contact_info | address
--
--      Queries dbo.vw_Suppliers — the view handles the join
--      and STRING_AGG so this stays a simple SELECT.
-- ============================================================

-- Active suppliers
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

-- Archived suppliers
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

-- ============================================================
--  2.  INSERT a new supplier
--      Step 1: generate next ID (same pattern as Java)
-- ============================================================

-- Step 1: get current max suffix
SELECT
    MAX(CAST(SUBSTRING(supplier_id, 5, LEN(supplier_id)) AS INT)) AS max_num
FROM dbo.Suppliers
WHERE is_deleted = 0;
-- Java: newId = String.format("SUP-%04d", maxNum + 1)

-- Step 2: insert the supplier header row
-- Parameters: supplier_id, supplier_name, contact_info, address
INSERT INTO dbo.Suppliers (supplier_id, supplier_name, contact_info, address)
VALUES (?, ?, ?, ?);

-- Step 3: link each selected inventory item in the junction table
-- Repeat for each inventory_id the user picks in the modal
INSERT INTO dbo.Supplier_Ingredients (supplier_id, inventory_id)
VALUES (?, ?);

GO

-- ============================================================
--  3.  UPDATE supplier header fields
--      (Does NOT touch junction rows — see section 4)
-- ============================================================

-- Parameters: supplier_name, contact_info, address, supplier_id
UPDATE dbo.Suppliers
SET
    supplier_name = ?,
    contact_info  = ?,
    address       = ?
WHERE supplier_id = ?
  AND is_deleted  = 0;

GO

-- ============================================================
--  4.  MANAGE INGREDIENTS (junction table)
--
--  Add a single ingredient link
-- ============================================================

INSERT INTO dbo.Supplier_Ingredients (supplier_id, inventory_id)
VALUES (?, ?);

GO

-- Remove a single ingredient link
DELETE FROM dbo.Supplier_Ingredients
WHERE supplier_id  = ?
  AND inventory_id = ?;

GO

-- Replace ALL ingredient links for a supplier in one operation
-- (delete old ones, re-insert from the new list)
DELETE FROM dbo.Supplier_Ingredients WHERE supplier_id = ?;

-- Then re-insert each inventory_id from the updated list:
INSERT INTO dbo.Supplier_Ingredients (supplier_id, inventory_id)
VALUES (?, ?);

GO

-- Fetch current ingredient links for a single supplier
-- (useful for pre-populating the edit modal)
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

-- ============================================================
--  5.  ARCHIVE / RESTORE
--      Junction rows are left intact — they come back when
--      the supplier is restored.
-- ============================================================

-- Archive one supplier
UPDATE dbo.Suppliers
SET [status] = 'archived'
WHERE supplier_id = ?
  AND is_deleted  = 0;

GO

-- Restore one supplier
UPDATE dbo.Suppliers
SET [status] = 'active'
WHERE supplier_id = ?
  AND is_deleted  = 0;

GO

-- Archive ALL active suppliers
UPDATE dbo.Suppliers
SET [status] = 'archived'
WHERE is_deleted = 0
  AND [status]   = 'active';

GO

-- Restore ALL archived suppliers
UPDATE dbo.Suppliers
SET [status] = 'active'
WHERE is_deleted = 0
  AND [status]   = 'archived';

GO

-- ============================================================
--  6.  SOFT DELETE
-- ============================================================

-- Soft-delete all visible rows in current tab (Java passes tab as ?)
UPDATE dbo.Suppliers
SET is_deleted = 1
WHERE is_deleted = 0
  AND [status]   = ?;

GO

-- Soft-delete a single supplier
-- Junction rows in Supplier_Ingredients are NOT deleted —
-- they become unreachable via the LEFT JOIN filter but are
-- preserved in case the row is ever hard-restored manually.
UPDATE dbo.Suppliers
SET is_deleted = 1
WHERE supplier_id = ?;

GO

-- ============================================================
--  7.  EXPORT HELPER — full flat row for CSV
-- ============================================================

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