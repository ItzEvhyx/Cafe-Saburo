-- ============================================================
--  SQLQueries/inventory_setup.sql
--  Cafe Saburo POS — Inventory Table Setup
--  Dialect: T-SQL (SQL Server)
--
--  !! RUN THIS FILE ONCE TO SET UP YOUR DATABASE !!
--  Run this BEFORE inventory_query.sql.
--
--  Rules enforced:
--    • Soft delete  : is_deleted = 1 (row hidden, not removed)
--    • Archive      : status = 'archived' (moved out of active view)
-- ============================================================


-- ============================================================
--  DROP TABLE (safe re-run)
-- ============================================================

IF OBJECT_ID('dbo.Inventory', 'U') IS NOT NULL DROP TABLE dbo.Inventory;

GO

-- ============================================================
--  CREATE TABLE
-- ============================================================

CREATE TABLE dbo.Inventory (
    inventory_id   VARCHAR(10)     NOT NULL PRIMARY KEY,
    ingredient     VARCHAR(100)    NOT NULL,
    quantity       DECIMAL(10, 2)  NOT NULL DEFAULT 0,
    unit           VARCHAR(10)     NOT NULL,
    reorder_level  INT             NOT NULL DEFAULT 0,
    is_deleted     BIT             NOT NULL DEFAULT 0,
    status         VARCHAR(10)     NOT NULL DEFAULT 'active',
    CONSTRAINT chk_inv_status CHECK (status IN ('active', 'archived')),
    CONSTRAINT chk_inv_unit   CHECK (unit   IN ('ml', 'l', 'g', 'kg', 'pcs'))
);

GO

-- ============================================================
--  SEED DATA — Inventory (50 ingredients)
-- ============================================================

INSERT INTO dbo.Inventory (inventory_id, ingredient, quantity, unit, reorder_level) VALUES

-- ── Base & Essentials ───────────────────────────────────────
('INV-0001', 'Espresso Beans',           20.00, 'kg',  5),
('INV-0002', 'Fresh Milk',               30.00, 'l',   8),
('INV-0003', 'Ice',                      50.00, 'kg',  15),
('INV-0004', 'Water',                    100.00,'l',   20),

-- ── Sweeteners & General Add-ons ────────────────────────────
('INV-0005', 'Sugar Syrup',              8.00,  'l',   2),
('INV-0006', 'Brown Sugar Syrup',        6.00,  'l',   2),
('INV-0007', 'Honey',                    4.00,  'l',   1),
('INV-0008', 'Agave Syrup',              3.00,  'l',   1),
('INV-0009', 'Whipped Cream',            10.00, 'pcs', 3),
('INV-0010', 'Chocolate Syrup',          5.00,  'l',   2),
('INV-0011', 'Caramel Syrup',            5.00,  'l',   2),
('INV-0012', 'Vanilla Syrup',            5.00,  'l',   2),

-- ── Coffee Flavor Syrups ────────────────────────────────────
('INV-0013', 'Hazelnut Syrup',           4.00,  'l',   1),
('INV-0014', 'Irish Cream Syrup',        3.00,  'l',   1),
('INV-0015', 'Butterscotch Syrup',       3.00,  'l',   1),
('INV-0016', 'White Chocolate Syrup',    4.00,  'l',   1),
('INV-0017', 'Dark Chocolate Syrup',     4.00,  'l',   1),
('INV-0018', 'Mocha Sauce',              3.00,  'l',   1),

-- ── Specialty Ingredients ───────────────────────────────────
('INV-0019', 'Biscoff Spread',           5.00,  'kg',  2),
('INV-0020', 'Biscoff Crumbs',           3.00,  'kg',  1),
('INV-0021', 'Pistachio Syrup',          2.00,  'l',   1),
('INV-0022', 'Pistachio Paste',          2.00,  'kg',  1),
('INV-0023', 'Pumpkin Spice Powder',     1.00,  'kg',  1),
('INV-0024', 'Lavender Syrup',           2.00,  'l',   1),
('INV-0025', 'Sakura Syrup',             2.00,  'l',   1),
('INV-0026', 'Tiramisu Flavoring',       1.50,  'l',   1),
('INV-0027', 'Cream Cheese',             5.00,  'kg',  2),
('INV-0028', 'Banana Puree',             4.00,  'kg',  2),
('INV-0029', 'Strawberry Puree',         4.00,  'kg',  2),
('INV-0030', 'Blueberry Puree',          3.00,  'kg',  1),
('INV-0031', 'Mango Puree',              4.00,  'kg',  2),

-- ── Matcha & Tea-Based ──────────────────────────────────────
('INV-0032', 'Matcha Powder',            3.00,  'kg',  1),
('INV-0033', 'Hojicha Powder',           2.00,  'kg',  1),
('INV-0034', 'Green Tea Bags',           100.00,'pcs', 20),
('INV-0035', 'Fruit Tea Bags',           100.00,'pcs', 20),
('INV-0036', 'Fruit Tea Concentrate',    5.00,  'l',   2),

-- ── Refresher / Fruit Syrups ────────────────────────────────
('INV-0037', 'Kiwi Syrup',               3.00,  'l',   1),
('INV-0038', 'Green Apple Syrup',        3.00,  'l',   1),
('INV-0039', 'Passion Fruit Syrup',      3.00,  'l',   1),
('INV-0040', 'Pomegranate Syrup',        3.00,  'l',   1),
('INV-0041', 'Mixed Berry Syrup',        3.00,  'l',   1),

-- ── Frappe / Smoothie Ingredients ───────────────────────────
('INV-0042', 'Frappe Base Powder',       5.00,  'kg',  2),
('INV-0043', 'Ice Cream Base',           5.00,  'kg',  2),
('INV-0044', 'Oreo Crumbs',              3.00,  'kg',  1),
('INV-0045', 'Cheesecake Base',          3.00,  'kg',  1),
('INV-0046', 'Pecan Syrup',              2.00,  'l',   1),

-- ── Milk Alternatives ───────────────────────────────────────
('INV-0047', 'Oat Milk',                 10.00, 'l',   3),

-- ── Creamers & Condensed ────────────────────────────────────
('INV-0048', 'Creamer',                  8.00,  'kg',  2),
('INV-0049', 'Condensed Milk',           6.00,  'kg',  2),

-- ── Consumables ─────────────────────────────────────────────
('INV-0050', 'Extra Espresso Shot',      500.00,'pcs', 50);

GO

-- ============================================================
--  VERIFY — confirm 50 active rows were inserted
-- ============================================================

SELECT COUNT(*) AS seeded_rows
FROM dbo.Inventory
WHERE is_deleted = 0 AND status = 'active';
-- Expected: 50

GO