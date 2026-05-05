-- inventory_setup.sql — Cafe Saburo POS: Inventory table definition and seed data
-- Dialect: T-SQL (SQL Server)
-- Run AFTER ingredients_setup.sql, BEFORE suppliers_setup.sql.
--
-- ingredient_id is a FK → dbo.Ingredients (ingredient_id).
-- INV-NNNN maps 1-to-1 with ING-NNNN (same alphabetical order):
--   INV-0001 ↔ ING-0001 (Agave Syrup) ... INV-0050 ↔ ING-0050 (White Chocolate Syrup)
--
-- NOTE: ingredients_setup.sql already dropped Inventory and all dependent
-- tables. The DROP block below is kept so this file is also safe to run standalone.

-- ── STEP 1: Drop child tables that reference Inventory ────────────────────────────────────────────
IF OBJECT_ID('dbo.Purchases',            'U') IS NOT NULL DROP TABLE dbo.Purchases;
GO
IF OBJECT_ID('dbo.Supplier_Ingredients', 'U') IS NOT NULL DROP TABLE dbo.Supplier_Ingredients;
GO
IF OBJECT_ID('dbo.Suppliers',            'U') IS NOT NULL DROP TABLE dbo.Suppliers;
GO
IF OBJECT_ID('dbo.Inventory',            'U') IS NOT NULL DROP TABLE dbo.Inventory;
GO

-- ── STEP 2: Create Inventory ──────────────────────────────────────────────────────────────────────
-- ingredient_id is a FK to dbo.Ingredients — the ingredient name lives there, not here.
CREATE TABLE dbo.Inventory (
    inventory_id   VARCHAR(10)     NOT NULL,
    ingredient_id  VARCHAR(10)     NOT NULL,
    quantity       DECIMAL(10, 2)  NOT NULL DEFAULT 0,
    unit           VARCHAR(10)     NOT NULL,
    reorder_level  INT             NOT NULL DEFAULT 0,
    is_deleted     BIT             NOT NULL DEFAULT 0,
    [status]       VARCHAR(10)     NOT NULL DEFAULT 'active',

    CONSTRAINT pk_inventory      PRIMARY KEY (inventory_id),
    CONSTRAINT fk_inv_ingredient FOREIGN KEY (ingredient_id)
        REFERENCES dbo.Ingredients (ingredient_id),
    CONSTRAINT chk_inv_status    CHECK ([status] IN ('active', 'archived')),
    CONSTRAINT chk_inv_unit      CHECK (unit IN ('ml', 'l', 'g', 'kg', 'pcs'))
);
GO

-- ── STEP 3: Seed 50 inventory rows — one per ingredient, alphabetical order ──────────────────────
-- INV-NNNN ↔ ING-NNNN (same number = same ingredient)
INSERT INTO dbo.Inventory (inventory_id, ingredient_id, quantity, unit, reorder_level) VALUES

-- A
('INV-0001', 'ING-0001',  3.00,   'l',    1),   -- Agave Syrup

-- B
('INV-0002', 'ING-0002',  4.00,   'kg',   2),   -- Banana Puree
('INV-0003', 'ING-0003',  3.00,   'kg',   1),   -- Biscoff Crumbs
('INV-0004', 'ING-0004',  5.00,   'kg',   2),   -- Biscoff Spread
('INV-0005', 'ING-0005',  3.00,   'kg',   1),   -- Blueberry Puree
('INV-0006', 'ING-0006',  6.00,   'l',    2),   -- Brown Sugar Syrup
('INV-0007', 'ING-0007',  3.00,   'l',    1),   -- Butterscotch Syrup

-- C
('INV-0008', 'ING-0008',  5.00,   'l',    2),   -- Caramel Syrup
('INV-0009', 'ING-0009',  3.00,   'kg',   1),   -- Cheesecake Base
('INV-0010', 'ING-0010',  5.00,   'l',    2),   -- Chocolate Syrup
('INV-0011', 'ING-0011',  6.00,   'kg',   2),   -- Condensed Milk
('INV-0012', 'ING-0012',  5.00,   'kg',   2),   -- Cream Cheese
('INV-0013', 'ING-0013',  8.00,   'kg',   2),   -- Creamer

-- D
('INV-0014', 'ING-0014',  4.00,   'l',    1),   -- Dark Chocolate Syrup

-- E
('INV-0015', 'ING-0015',  20.00,  'kg',   5),   -- Espresso Beans
('INV-0016', 'ING-0016',  500.00, 'pcs',  50),  -- Extra Espresso Shot

-- F
('INV-0017', 'ING-0017',  5.00,   'kg',   2),   -- Frappe Base Powder
('INV-0018', 'ING-0018',  30.00,  'l',    8),   -- Fresh Milk
('INV-0019', 'ING-0019',  100.00, 'pcs',  20),  -- Fruit Tea Bags
('INV-0020', 'ING-0020',  5.00,   'l',    2),   -- Fruit Tea Concentrate

-- G
('INV-0021', 'ING-0021',  3.00,   'l',    1),   -- Green Apple Syrup
('INV-0022', 'ING-0022',  100.00, 'pcs',  20),  -- Green Tea Bags

-- H
('INV-0023', 'ING-0023',  4.00,   'l',    1),   -- Hazelnut Syrup
('INV-0024', 'ING-0024',  2.00,   'kg',   1),   -- Hojicha Powder
('INV-0025', 'ING-0025',  4.00,   'l',    1),   -- Honey

-- I
('INV-0026', 'ING-0026',  50.00,  'kg',   15),  -- Ice
('INV-0027', 'ING-0027',  5.00,   'kg',   2),   -- Ice Cream Base
('INV-0028', 'ING-0028',  3.00,   'l',    1),   -- Irish Cream Syrup

-- K
('INV-0029', 'ING-0029',  3.00,   'l',    1),   -- Kiwi Syrup

-- L
('INV-0030', 'ING-0030',  2.00,   'l',    1),   -- Lavender Syrup

-- M
('INV-0031', 'ING-0031',  4.00,   'kg',   2),   -- Mango Puree
('INV-0032', 'ING-0032',  3.00,   'kg',   1),   -- Matcha Powder
('INV-0033', 'ING-0033',  3.00,   'l',    1),   -- Mixed Berry Syrup
('INV-0034', 'ING-0034',  3.00,   'l',    1),   -- Mocha Sauce

-- O
('INV-0035', 'ING-0035',  10.00,  'l',    3),   -- Oat Milk
('INV-0036', 'ING-0036',  3.00,   'kg',   1),   -- Oreo Crumbs

-- P
('INV-0037', 'ING-0037',  3.00,   'l',    1),   -- Passion Fruit Syrup
('INV-0038', 'ING-0038',  2.00,   'l',    1),   -- Pecan Syrup
('INV-0039', 'ING-0039',  2.00,   'kg',   1),   -- Pistachio Paste
('INV-0040', 'ING-0040',  2.00,   'l',    1),   -- Pistachio Syrup
('INV-0041', 'ING-0041',  3.00,   'l',    1),   -- Pomegranate Syrup
('INV-0042', 'ING-0042',  1.00,   'kg',   1),   -- Pumpkin Spice Powder

-- S
('INV-0043', 'ING-0043',  2.00,   'l',    1),   -- Sakura Syrup
('INV-0044', 'ING-0044',  4.00,   'kg',   2),   -- Strawberry Puree
('INV-0045', 'ING-0045',  8.00,   'l',    2),   -- Sugar Syrup

-- T
('INV-0046', 'ING-0046',  1.50,   'l',    1),   -- Tiramisu Flavoring

-- V
('INV-0047', 'ING-0047',  5.00,   'l',    2),   -- Vanilla Syrup

-- W
('INV-0048', 'ING-0048',  100.00, 'l',    20),  -- Water
('INV-0049', 'ING-0049',  10.00,  'pcs',  3),   -- Whipped Cream
('INV-0050', 'ING-0050',  4.00,   'l',    1);   -- White Chocolate Syrup
GO

-- ── VERIFY: Expected result: 50 ───────────────────────────────────────────────────────────────────
SELECT COUNT(*) AS seeded_rows FROM dbo.Inventory WHERE is_deleted = 0 AND [status] = 'active';
GO

-- ── SPOT CHECK: Join to confirm FK linkage is correct ─────────────────────────────────────────────
SELECT TOP 5
    inv.inventory_id,
    ing.ingredient_id,
    ing.ingredient_name,
    inv.quantity,
    inv.unit
FROM dbo.Inventory inv
JOIN dbo.Ingredients ing ON ing.ingredient_id = inv.ingredient_id
ORDER BY inv.inventory_id;
GO