-- ingredients_setup.sql — Cafe Saburo POS: Ingredients master table
-- Dialect: T-SQL (SQL Server)
-- Run this FIRST — before inventory_setup.sql and suppliers_setup.sql.
--
-- This is the master ingredient registry. ingredient_id is the PK that
-- dbo.Inventory and dbo.Supplier_Ingredients reference as a FK.
--
-- ING-0001 → Agave Syrup ... ING-0050 → White Chocolate Syrup (alphabetical)
-- ING-NNNN maps 1-to-1 with INV-NNNN in dbo.Inventory (same alphabetical order).

-- ── STEP 1: Drop all dependent tables first (child → parent order) ───────────────────────────────
IF OBJECT_ID('dbo.Purchases',            'U') IS NOT NULL DROP TABLE dbo.Purchases;
GO
IF OBJECT_ID('dbo.Supplier_Ingredients', 'U') IS NOT NULL DROP TABLE dbo.Supplier_Ingredients;
GO
IF OBJECT_ID('dbo.Suppliers',            'U') IS NOT NULL DROP TABLE dbo.Suppliers;
GO
IF OBJECT_ID('dbo.Inventory',            'U') IS NOT NULL DROP TABLE dbo.Inventory;
GO
IF OBJECT_ID('dbo.Ingredients',          'U') IS NOT NULL DROP TABLE dbo.Ingredients;
GO

-- ── STEP 2: Create Ingredients master table ───────────────────────────────────────────────────────
CREATE TABLE dbo.Ingredients (
    ingredient_id   VARCHAR(10)     NOT NULL,
    ingredient_name VARCHAR(100)    NOT NULL,
    price           DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    is_deleted      BIT             NOT NULL DEFAULT 0,
    [status]        VARCHAR(10)     NOT NULL DEFAULT 'active',

    CONSTRAINT pk_ingredients        PRIMARY KEY (ingredient_id),
    CONSTRAINT uq_ingredient_name    UNIQUE (ingredient_name),
    CONSTRAINT chk_ingredient_status CHECK ([status] IN ('active', 'archived'))
);
GO

-- ── STEP 3: Seed 50 ingredients in alphabetical order ────────────────────────────────────────────
-- Prices default to 0.00 — update to actual purchase prices per unit after seeding.
INSERT INTO dbo.Ingredients (ingredient_id, ingredient_name, price, [status]) VALUES

-- A
('ING-0001', 'Agave Syrup',             0.00, 'active'),

-- B
('ING-0002', 'Banana Puree',            0.00, 'active'),
('ING-0003', 'Biscoff Crumbs',          0.00, 'active'),
('ING-0004', 'Biscoff Spread',          0.00, 'active'),
('ING-0005', 'Blueberry Puree',         0.00, 'active'),
('ING-0006', 'Brown Sugar Syrup',       0.00, 'active'),
('ING-0007', 'Butterscotch Syrup',      0.00, 'active'),

-- C
('ING-0008', 'Caramel Syrup',           0.00, 'active'),
('ING-0009', 'Cheesecake Base',         0.00, 'active'),
('ING-0010', 'Chocolate Syrup',         0.00, 'active'),
('ING-0011', 'Condensed Milk',          0.00, 'active'),
('ING-0012', 'Cream Cheese',            0.00, 'active'),
('ING-0013', 'Creamer',                 0.00, 'active'),

-- D
('ING-0014', 'Dark Chocolate Syrup',    0.00, 'active'),

-- E
('ING-0015', 'Espresso Beans',          0.00, 'active'),
('ING-0016', 'Extra Espresso Shot',     0.00, 'active'),

-- F
('ING-0017', 'Frappe Base Powder',      0.00, 'active'),
('ING-0018', 'Fresh Milk',              0.00, 'active'),
('ING-0019', 'Fruit Tea Bags',          0.00, 'active'),
('ING-0020', 'Fruit Tea Concentrate',   0.00, 'active'),

-- G
('ING-0021', 'Green Apple Syrup',       0.00, 'active'),
('ING-0022', 'Green Tea Bags',          0.00, 'active'),

-- H
('ING-0023', 'Hazelnut Syrup',          0.00, 'active'),
('ING-0024', 'Hojicha Powder',          0.00, 'active'),
('ING-0025', 'Honey',                   0.00, 'active'),

-- I
('ING-0026', 'Ice',                     0.00, 'active'),
('ING-0027', 'Ice Cream Base',          0.00, 'active'),
('ING-0028', 'Irish Cream Syrup',       0.00, 'active'),

-- K
('ING-0029', 'Kiwi Syrup',              0.00, 'active'),

-- L
('ING-0030', 'Lavender Syrup',          0.00, 'active'),

-- M
('ING-0031', 'Mango Puree',             0.00, 'active'),
('ING-0032', 'Matcha Powder',           0.00, 'active'),
('ING-0033', 'Mixed Berry Syrup',       0.00, 'active'),
('ING-0034', 'Mocha Sauce',             0.00, 'active'),

-- O
('ING-0035', 'Oat Milk',               0.00, 'active'),
('ING-0036', 'Oreo Crumbs',             0.00, 'active'),

-- P
('ING-0037', 'Passion Fruit Syrup',     0.00, 'active'),
('ING-0038', 'Pecan Syrup',             0.00, 'active'),
('ING-0039', 'Pistachio Paste',         0.00, 'active'),
('ING-0040', 'Pistachio Syrup',         0.00, 'active'),
('ING-0041', 'Pomegranate Syrup',       0.00, 'active'),
('ING-0042', 'Pumpkin Spice Powder',    0.00, 'active'),

-- S
('ING-0043', 'Sakura Syrup',            0.00, 'active'),
('ING-0044', 'Strawberry Puree',        0.00, 'active'),
('ING-0045', 'Sugar Syrup',             0.00, 'active'),

-- T
('ING-0046', 'Tiramisu Flavoring',      0.00, 'active'),

-- V
('ING-0047', 'Vanilla Syrup',           0.00, 'active'),

-- W
('ING-0048', 'Water',                   0.00, 'active'),
('ING-0049', 'Whipped Cream',           0.00, 'active'),
('ING-0050', 'White Chocolate Syrup',   0.00, 'active');
GO

-- ── VERIFY: Expected result: 50 ───────────────────────────────────────────────────────────────────
SELECT COUNT(*) AS seeded_rows FROM dbo.Ingredients WHERE is_deleted = 0 AND [status] = 'active';
GO