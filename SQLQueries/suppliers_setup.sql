-- suppliers_setup.sql — Cafe Saburo POS: Suppliers + Supplier_Ingredients setup and seed data
-- Dialect: T-SQL (SQL Server)
-- Run AFTER ingredients_setup.sql and inventory_setup.sql, BEFORE purchases_setup.sql.
--
-- Supplier_Ingredients junction table links suppliers to ingredients via ingredient_id (FK → dbo.Ingredients).
-- It also carries inventory_id (FK → dbo.Inventory) so purchases can resolve both the
-- supplier and the specific stock record in one join.
--
-- The DROP block below is safe to re-run standalone.

-- ── STEP 1: Drop child tables first ───────────────────────────────────────────────────────────────
IF OBJECT_ID('dbo.Purchases',            'U') IS NOT NULL DROP TABLE dbo.Purchases;
GO
IF OBJECT_ID('dbo.Supplier_Ingredients', 'U') IS NOT NULL DROP TABLE dbo.Supplier_Ingredients;
GO
IF OBJECT_ID('dbo.Suppliers',            'U') IS NOT NULL DROP TABLE dbo.Suppliers;
GO

-- ── STEP 2: Create Suppliers ──────────────────────────────────────────────────────────────────────
CREATE TABLE dbo.Suppliers (
    supplier_id    VARCHAR(10)     NOT NULL,
    supplier_name  VARCHAR(100)    NOT NULL,
    contact_info   VARCHAR(100)    NOT NULL DEFAULT '',
    address        VARCHAR(200)    NOT NULL DEFAULT '',
    is_deleted     BIT             NOT NULL DEFAULT 0,
    [status]       VARCHAR(10)     NOT NULL DEFAULT 'active',

    CONSTRAINT pk_suppliers    PRIMARY KEY (supplier_id),
    CONSTRAINT chk_sup_status  CHECK ([status] IN ('active', 'archived'))
);
GO

-- ── STEP 3: Create Supplier_Ingredients junction table ────────────────────────────────────────────
-- ingredient_id → dbo.Ingredients (the master ingredient record, carries the name & price)
-- inventory_id  → dbo.Inventory   (the stock record, carries quantity, unit, reorder level)
-- Both FKs together let us answer: "which supplier supplies which ingredient, and where is it stocked?"
CREATE TABLE dbo.Supplier_Ingredients (
    supplier_id    VARCHAR(10)  NOT NULL,
    ingredient_id  VARCHAR(10)  NOT NULL,
    inventory_id   VARCHAR(10)  NOT NULL,

    CONSTRAINT pk_sup_ing       PRIMARY KEY (supplier_id, ingredient_id),
    CONSTRAINT fk_si_supplier   FOREIGN KEY (supplier_id)   REFERENCES dbo.Suppliers   (supplier_id),
    CONSTRAINT fk_si_ingredient FOREIGN KEY (ingredient_id) REFERENCES dbo.Ingredients (ingredient_id),
    CONSTRAINT fk_si_inventory  FOREIGN KEY (inventory_id)  REFERENCES dbo.Inventory   (inventory_id)
);
GO

-- ── STEP 4: Seed 15 suppliers ─────────────────────────────────────────────────────────────────────
INSERT INTO dbo.Suppliers (supplier_id, supplier_name, contact_info, address) VALUES
('SUP-0001', 'Agave & Co.',              '+63 912 001 0001', '1 Agave Lane, Makati City'),
('SUP-0002', 'Baker''s Best',            '+63 912 002 0002', '2 Baker St., BGC, Taguig'),
('SUP-0003', 'Berry Bliss Farms',        '+63 912 003 0003', '3 Berry Rd., Quezon City'),
('SUP-0004', 'Café Essentials PH',       '+63 912 004 0004', '4 Brew Ave., Pasig City'),
('SUP-0005', 'Eastern Spice Traders',    '+63 912 005 0005', '5 Spice St., Binondo, Manila'),
('SUP-0006', 'Fruity Finds',             '+63 912 006 0006', '6 Fruit Blvd., Mandaluyong'),
('SUP-0007', 'Global Syrup House',       '+63 912 007 0007', '7 Syrup Cir., Ortigas, Pasig'),
('SUP-0008', 'Green Leaf Organics',      '+63 912 008 0008', '8 Leaf St., Marikina City'),
('SUP-0009', 'Island Flavors Co.',       '+63 912 009 0009', '9 Tropics Dr., Paranaque'),
('SUP-0010', 'Metro Powder Supplies',    '+63 912 010 0010', '10 Powder Rd., Caloocan City'),
('SUP-0011', 'Nectar & Bloom',           '+63 912 011 0011', '11 Bloom St., Las Pinas City'),
('SUP-0012', 'Premium Extract Co.',      '+63 912 012 0012', '12 Extract Ave., Muntinlupa'),
('SUP-0013', 'Sweet Crunch Distributors','+63 912 013 0013', '13 Crunch Blvd., Valenzuela'),
('SUP-0014', 'The Chocolate Source',     '+63 912 014 0014', '14 Choco Lane, San Juan City'),
('SUP-0015', 'Vanilla & Friends',        '+63 912 015 0015', '15 Vanilla Rd., Pasay City');
GO

-- ── STEP 5: Seed Supplier_Ingredients junction rows ──────────────────────────────────────────────
-- Format: (supplier_id, ingredient_id, inventory_id)
-- ingredient_id = ING-NNNN, inventory_id = INV-NNNN (same number = same ingredient)
INSERT INTO dbo.Supplier_Ingredients (supplier_id, ingredient_id, inventory_id) VALUES

-- SUP-0001 Agave & Co. → Agave Syrup, Honey, Sugar Syrup
('SUP-0001', 'ING-0001', 'INV-0001'),   -- Agave Syrup
('SUP-0001', 'ING-0025', 'INV-0025'),   -- Honey
('SUP-0001', 'ING-0045', 'INV-0045'),   -- Sugar Syrup

-- SUP-0002 Baker's Best → Banana Puree, Cheesecake Base, Cream Cheese, Whipped Cream
('SUP-0002', 'ING-0002', 'INV-0002'),   -- Banana Puree
('SUP-0002', 'ING-0009', 'INV-0009'),   -- Cheesecake Base
('SUP-0002', 'ING-0012', 'INV-0012'),   -- Cream Cheese
('SUP-0002', 'ING-0049', 'INV-0049'),   -- Whipped Cream

-- SUP-0003 Berry Bliss Farms → Blueberry Puree, Brown Sugar Syrup, Cream Cheese, Strawberry Puree
('SUP-0003', 'ING-0005', 'INV-0005'),   -- Blueberry Puree
('SUP-0003', 'ING-0006', 'INV-0006'),   -- Brown Sugar Syrup
('SUP-0003', 'ING-0012', 'INV-0012'),   -- Cream Cheese
('SUP-0003', 'ING-0044', 'INV-0044'),   -- Strawberry Puree

-- SUP-0004 Café Essentials PH → Condensed Milk, Creamer, Extra Espresso Shot, Fresh Milk
('SUP-0004', 'ING-0011', 'INV-0011'),   -- Condensed Milk
('SUP-0004', 'ING-0013', 'INV-0013'),   -- Creamer
('SUP-0004', 'ING-0016', 'INV-0016'),   -- Extra Espresso Shot
('SUP-0004', 'ING-0018', 'INV-0018'),   -- Fresh Milk

-- SUP-0005 Eastern Spice Traders → Green Apple Syrup, Hojicha Powder, Honey
('SUP-0005', 'ING-0021', 'INV-0021'),   -- Green Apple Syrup
('SUP-0005', 'ING-0024', 'INV-0024'),   -- Hojicha Powder
('SUP-0005', 'ING-0025', 'INV-0025'),   -- Honey

-- SUP-0006 Fruity Finds → Fruit Tea Bags, Fruit Tea Concentrate, Ice Cream Base
('SUP-0006', 'ING-0019', 'INV-0019'),   -- Fruit Tea Bags
('SUP-0006', 'ING-0020', 'INV-0020'),   -- Fruit Tea Concentrate
('SUP-0006', 'ING-0027', 'INV-0027'),   -- Ice Cream Base

-- SUP-0007 Global Syrup House → Irish Cream Syrup, Kiwi Syrup, Pomegranate Syrup
('SUP-0007', 'ING-0028', 'INV-0028'),   -- Irish Cream Syrup
('SUP-0007', 'ING-0029', 'INV-0029'),   -- Kiwi Syrup
('SUP-0007', 'ING-0041', 'INV-0041'),   -- Pomegranate Syrup

-- SUP-0008 Green Leaf Organics → Matcha Powder, Mixed Berry Syrup, Mocha Sauce, Oat Milk
('SUP-0008', 'ING-0032', 'INV-0032'),   -- Matcha Powder
('SUP-0008', 'ING-0033', 'INV-0033'),   -- Mixed Berry Syrup
('SUP-0008', 'ING-0034', 'INV-0034'),   -- Mocha Sauce
('SUP-0008', 'ING-0035', 'INV-0035'),   -- Oat Milk

-- SUP-0009 Island Flavors Co. → Passion Fruit Syrup, Pecan Syrup, Pistachio Paste, Pomegranate Syrup
('SUP-0009', 'ING-0037', 'INV-0037'),   -- Passion Fruit Syrup
('SUP-0009', 'ING-0038', 'INV-0038'),   -- Pecan Syrup
('SUP-0009', 'ING-0039', 'INV-0039'),   -- Pistachio Paste
('SUP-0009', 'ING-0041', 'INV-0041'),   -- Pomegranate Syrup

-- SUP-0010 Metro Powder Supplies → Pumpkin Spice Powder, Sakura Syrup, Sugar Syrup
('SUP-0010', 'ING-0042', 'INV-0042'),   -- Pumpkin Spice Powder
('SUP-0010', 'ING-0043', 'INV-0043'),   -- Sakura Syrup
('SUP-0010', 'ING-0045', 'INV-0045'),   -- Sugar Syrup

-- SUP-0011 Nectar & Bloom → Hazelnut Syrup, Lavender Syrup, Strawberry Puree
('SUP-0011', 'ING-0023', 'INV-0023'),   -- Hazelnut Syrup
('SUP-0011', 'ING-0030', 'INV-0030'),   -- Lavender Syrup
('SUP-0011', 'ING-0044', 'INV-0044'),   -- Strawberry Puree

-- SUP-0012 Premium Extract Co. → Pistachio Syrup, Tiramisu Flavoring, Vanilla Syrup
('SUP-0012', 'ING-0040', 'INV-0040'),   -- Pistachio Syrup
('SUP-0012', 'ING-0046', 'INV-0046'),   -- Tiramisu Flavoring
('SUP-0012', 'ING-0047', 'INV-0047'),   -- Vanilla Syrup

-- SUP-0013 Sweet Crunch Distributors → Biscoff Crumbs, Biscoff Spread, Oreo Crumbs
('SUP-0013', 'ING-0003', 'INV-0003'),   -- Biscoff Crumbs
('SUP-0013', 'ING-0004', 'INV-0004'),   -- Biscoff Spread
('SUP-0013', 'ING-0036', 'INV-0036'),   -- Oreo Crumbs

-- SUP-0014 The Chocolate Source → Cheesecake Base, Dark Chocolate Syrup, White Chocolate Syrup
('SUP-0014', 'ING-0009', 'INV-0009'),   -- Cheesecake Base
('SUP-0014', 'ING-0014', 'INV-0014'),   -- Dark Chocolate Syrup
('SUP-0014', 'ING-0050', 'INV-0050'),   -- White Chocolate Syrup

-- SUP-0015 Vanilla & Friends → Butterscotch Syrup, Hazelnut Syrup, Tiramisu Flavoring
('SUP-0015', 'ING-0007', 'INV-0007'),   -- Butterscotch Syrup
('SUP-0015', 'ING-0023', 'INV-0023'),   -- Hazelnut Syrup
('SUP-0015', 'ING-0046', 'INV-0046');   -- Tiramisu Flavoring
GO

-- ── VERIFY ────────────────────────────────────────────────────────────────────────────────────────
-- Expected: 15 suppliers
SELECT COUNT(*) AS supplier_count FROM dbo.Suppliers WHERE is_deleted = 0;
GO

-- Expected: 44 junction rows
SELECT COUNT(*) AS junction_rows FROM dbo.Supplier_Ingredients;
GO

-- SPOT CHECK: Show supplier name + ingredient name + inventory_id for first 10 rows
SELECT TOP 10
    s.supplier_name,
    ing.ingredient_name,
    si.inventory_id
FROM dbo.Supplier_Ingredients si
JOIN dbo.Suppliers   s   ON s.supplier_id     = si.supplier_id
JOIN dbo.Ingredients ing ON ing.ingredient_id = si.ingredient_id
ORDER BY s.supplier_name, ing.ingredient_name;
GO