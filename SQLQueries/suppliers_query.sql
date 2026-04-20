  -- ============================================================
  --  SQLQueries/suppliers_query.sql
  --  Cafe Saburo POS — Suppliers Reference Queries
  --  Dialect: T-SQL (SQL Server)
  --
  --  !! REFERENCE / DEV USE ONLY !!
  --  Run each block INDIVIDUALLY — never execute this whole file.
  --  Requires suppliers_setup.sql to have been run first.
  -- ============================================================

  SET QUOTED_IDENTIFIER ON;
  GO

  -- ============================================================
  --  QUERY 1 — Active, non-deleted suppliers (default view)
  -- ============================================================

  SELECT
      supplier_id,
      supplier_name,
      ingredients,
      contact_info,
      address
  FROM dbo.Suppliers
  WHERE is_deleted = 0
    AND [status]   = 'active'
  ORDER BY supplier_name ASC;


  -- ============================================================
  --  QUERY 2 — Archived suppliers
  -- ============================================================

  SELECT
      supplier_id,
      supplier_name,
      ingredients,
      contact_info,
      address
  FROM dbo.Suppliers
  WHERE is_deleted = 0
    AND [status]   = 'archived'
  ORDER BY supplier_name ASC;


  -- ============================================================
  --  QUERY 3 — Search suppliers by ingredient keyword
  -- ============================================================

  SELECT
      supplier_id,
      supplier_name,
      ingredients,
      contact_info,
      address
  FROM dbo.Suppliers
  WHERE is_deleted  = 0
    AND [status]    = 'active'
    AND ingredients LIKE '%Matcha%'   -- replace keyword as needed
  ORDER BY supplier_name ASC;


  -- ============================================================
  --  QUERY 4 — Insert a new supplier (example)
  --  NOTE: The app handles this via Add Supplier modal.
  --        Only run manually if needed.
  -- ============================================================

  -- INSERT INTO dbo.Suppliers (supplier_id, supplier_name, ingredients, contact_info, address)
  -- VALUES ('SUP-0016', 'Sample Supplier', 'Espresso Beans, Oat Milk', '+63 900 000 0000', '1 Sample St., Manila');


  -- ============================================================
  --  QUERY 5 — Update supplier details (example)
  --  NOTE: The app handles this via Edit mode.
  --        Only run manually if needed.
  -- ============================================================

  -- UPDATE dbo.Suppliers
  -- SET    supplier_name = 'Updated Name',
  --        ingredients   = 'Espresso Beans',
  --        contact_info  = '+63 900 111 2222',
  --        address       = 'New Address, City'
  -- WHERE  supplier_id   = 'SUP-0001'
  --   AND  is_deleted    = 0;


  -- ============================================================
  --  QUERY 6 — Archive selected suppliers (example)
  --  NOTE: The app handles this via Archive mode.
  --        Only run manually if needed.
  -- ============================================================

  -- UPDATE dbo.Suppliers
  -- SET    [status] = 'archived'
  -- WHERE  supplier_id IN ('SUP-0014', 'SUP-0015')
  --   AND  is_deleted   = 0;


  -- ============================================================
  --  QUERY 7 — Restore archived suppliers (example)
  --  NOTE: The app handles this via Archive mode.
  --        Only run manually if needed.
  -- ============================================================

  -- UPDATE dbo.Suppliers
  -- SET    [status] = 'active'
  -- WHERE  supplier_id IN ('SUP-0014', 'SUP-0015')
  --   AND  is_deleted   = 0;


  -- ============================================================
  --  QUERY 8 — Soft delete all in a given status (ADMIN)
  --  !! DANGER: Hides ALL rows from the app. !!
  -- ============================================================

  -- UPDATE dbo.Suppliers
  -- SET    is_deleted = 1
  -- WHERE  is_deleted = 0
  --   AND  [status]   = 'active';   -- change to 'archived' if needed


  -- ============================================================
  --  QUERY 9 — Restore all soft-deleted rows (ADMIN recovery)
  --  Run if you accidentally ran Query 8.
  -- ============================================================

  -- UPDATE dbo.Suppliers
  -- SET    is_deleted = 0
  -- WHERE  is_deleted = 1;


  -- ============================================================
  --  QUERY 10 — Full supplier report
  -- ============================================================

  SELECT
      supplier_id,
      supplier_name,
      ingredients,
      contact_info,
      address,
      [status]
  FROM dbo.Suppliers
  WHERE is_deleted = 0
  ORDER BY [status] ASC, supplier_name ASC;