-- ============================================================
--  SQLQueries/suppliers_setup.sql
--  Cafe Saburo POS — Suppliers Table Setup  (v3)
--  Dialect: T-SQL (SQL Server)
--
--  !! RUN THIS FILE ONCE TO SET UP YOUR DATABASE !!
--  Run AFTER inventory_setup.sql, BEFORE purchases_setup.sql.
--
--  Changes from v2:
--    • Added dbo.vw_Suppliers VIEW that re-exposes the
--      computed `ingredients` column (STRING_AGG join).
--      Java code that does  SELECT ... ingredients ...
--      FROM dbo.vw_Suppliers  will work without changes.
--
--  Soft delete  : is_deleted = 1
--  Archive      : [status] = 'archived'
-- ============================================================

SET QUOTED_IDENTIFIER ON;
GO

-- ============================================================
--  DROP OBJECTS (safe re-run — dependents first)
-- ============================================================

IF OBJECT_ID('dbo.vw_Suppliers',          'V') IS NOT NULL DROP VIEW  dbo.vw_Suppliers;
IF OBJECT_ID('dbo.Supplier_Ingredients',  'U') IS NOT NULL DROP TABLE dbo.Supplier_Ingredients;
IF OBJECT_ID('dbo.Suppliers',             'U') IS NOT NULL DROP TABLE dbo.Suppliers;

GO

-- ============================================================
--  CREATE dbo.Suppliers
-- ============================================================

CREATE TABLE dbo.Suppliers (
    supplier_id    VARCHAR(10)   NOT NULL PRIMARY KEY,
    supplier_name  VARCHAR(100)  NOT NULL,
    contact_info   VARCHAR(100)  NOT NULL,
    address        VARCHAR(200)  NOT NULL,
    is_deleted     BIT           NOT NULL DEFAULT 0,
    [status]       VARCHAR(10)   NOT NULL DEFAULT 'active',
    CONSTRAINT chk_sup_status CHECK ([status] IN ('active', 'archived'))
);

GO

-- ============================================================
--  CREATE dbo.Supplier_Ingredients  (junction table)
--
--  Each row = "this supplier provides this inventory item".
--  PK is composite (supplier_id + inventory_id).
-- ============================================================

CREATE TABLE dbo.Supplier_Ingredients (
    supplier_id   VARCHAR(10)  NOT NULL,
    inventory_id  VARCHAR(10)  NOT NULL,
    CONSTRAINT pk_sup_ing
        PRIMARY KEY (supplier_id, inventory_id),
    CONSTRAINT fk_supIng_supplier
        FOREIGN KEY (supplier_id)  REFERENCES dbo.Suppliers (supplier_id),
    CONSTRAINT fk_supIng_inventory
        FOREIGN KEY (inventory_id) REFERENCES dbo.Inventory (inventory_id)
);

GO

-- ============================================================
--  CREATE dbo.vw_Suppliers
--
--  Exposes the same 5-column shape the Java layer expects:
--    supplier_id | supplier_name | ingredients | contact_info | address
--
--  Your Java fetchSuppliers() should query THIS VIEW instead
--  of the base dbo.Suppliers table so it always gets the
--  computed ingredients string without any code changes.
-- ============================================================

CREATE VIEW dbo.vw_Suppliers
AS
SELECT
    s.supplier_id,
    s.supplier_name,
    ISNULL(
        STRING_AGG(i.ingredient, ', ')
        WITHIN GROUP (ORDER BY i.ingredient ASC),
        '—'
    )                       AS ingredients,
    s.contact_info,
    s.address,
    s.[status],
    s.is_deleted
FROM       dbo.Suppliers            s
LEFT JOIN  dbo.Supplier_Ingredients si ON si.supplier_id  = s.supplier_id
LEFT JOIN  dbo.Inventory            i  ON i.inventory_id  = si.inventory_id
                                       AND i.is_deleted   = 0
GROUP BY
    s.supplier_id,
    s.supplier_name,
    s.contact_info,
    s.address,
    s.[status],
    s.is_deleted;

GO

-- ============================================================
--  SEED DATA — 15 Suppliers
-- ============================================================

INSERT INTO dbo.Suppliers (supplier_id, supplier_name, contact_info, address) VALUES
('SUP-0001', 'Benguet Brew Supply',           '+63 912 111 2233', '45 Session Road, Baguio City, Benguet'),
('SUP-0002', 'Magnolia Dairy Distributors',   '+63 923 222 3344', '12 Dairy Lane, Quezon City, Metro Manila'),
('SUP-0003', 'SweetBase Co.',                 '+63 934 333 4455', '78 Rizal Ave., Caloocan City, Metro Manila'),
('SUP-0004', 'Aroma Flavor House',            '+63 945 444 5566', '33 Industrial Blvd., Pasig City, Metro Manila'),
('SUP-0005', 'Niche Botanicals Inc.',         '+63 956 555 6677', '9 Botanical Ave., Marikina City, Metro Manila'),
('SUP-0006', 'Continental Spreads PH',        '+63 967 666 7788', '22 Confection St., Mandaluyong City, Metro Manila'),
('SUP-0007', 'Tropical Puree Solutions',      '+63 978 777 8899', '5 Orchard Road, Davao City, Davao del Sur'),
('SUP-0008', 'Zen Matcha Trading',            '+63 989 888 9900', '17 Green Hill St., Cebu City, Cebu'),
('SUP-0009', 'Fruity Refresh Distributors',   '+63 990 999 0011', '88 Citrus Drive, Cagayan de Oro, Misamis Oriental'),
('SUP-0010', 'BlendPro Supplies',             '+63 912 100 2222', '60 Blend St., Las Piñas City, Metro Manila'),
('SUP-0011', 'Crumbz & Toppings Co.',         '+63 923 200 3333', '14 Baker Lane, Antipolo City, Rizal'),
('SUP-0012', 'GreenSip Alternatives',         '+63 934 300 4444', '3 Eco Ave., Taguig City, Metro Manila'),
('SUP-0013', 'AquaPure Logistics',            '+63 945 400 5555', '101 Purified Blvd., Valenzuela City, Metro Manila'),
('SUP-0014', 'CafeSupply Express',            '+63 956 500 6666', '55 Express Road, Makati City, Metro Manila'),
('SUP-0015', 'Natures Pantry PH',             '+63 967 600 7777', '27 Harvest St., Santa Rosa City, Laguna');

GO

-- ============================================================
--  SEED DATA — Supplier_Ingredients (junction rows)
-- ============================================================

INSERT INTO dbo.Supplier_Ingredients (supplier_id, inventory_id) VALUES

-- SUP-0001  Benguet Brew Supply → Espresso Beans
('SUP-0001', 'INV-0001'),

-- SUP-0002  Magnolia Dairy Distributors → Fresh Milk, Whipped Cream, Condensed Milk, Creamer
('SUP-0002', 'INV-0002'),
('SUP-0002', 'INV-0009'),
('SUP-0002', 'INV-0049'),
('SUP-0002', 'INV-0048'),

-- SUP-0003  SweetBase Co. → Sugar Syrup, Brown Sugar Syrup, Vanilla Syrup, Caramel Syrup, Chocolate Syrup
('SUP-0003', 'INV-0005'),
('SUP-0003', 'INV-0006'),
('SUP-0003', 'INV-0012'),
('SUP-0003', 'INV-0011'),
('SUP-0003', 'INV-0010'),

-- SUP-0004  Aroma Flavor House → Hazelnut, Irish Cream, Butterscotch, White Choc, Dark Choc, Mocha
('SUP-0004', 'INV-0013'),
('SUP-0004', 'INV-0014'),
('SUP-0004', 'INV-0015'),
('SUP-0004', 'INV-0016'),
('SUP-0004', 'INV-0017'),
('SUP-0004', 'INV-0018'),

-- SUP-0005  Niche Botanicals Inc. → Lavender, Sakura, Pistachio Syrup, Honey, Agave
('SUP-0005', 'INV-0024'),
('SUP-0005', 'INV-0025'),
('SUP-0005', 'INV-0021'),
('SUP-0005', 'INV-0007'),
('SUP-0005', 'INV-0008'),

-- SUP-0006  Continental Spreads PH → Biscoff Spread, Biscoff Crumbs, Pistachio Paste, Cream Cheese, Tiramisu
('SUP-0006', 'INV-0019'),
('SUP-0006', 'INV-0020'),
('SUP-0006', 'INV-0022'),
('SUP-0006', 'INV-0027'),
('SUP-0006', 'INV-0026'),

-- SUP-0007  Tropical Puree Solutions → Banana, Strawberry, Blueberry, Mango Puree
('SUP-0007', 'INV-0028'),
('SUP-0007', 'INV-0029'),
('SUP-0007', 'INV-0030'),
('SUP-0007', 'INV-0031'),

-- SUP-0008  Zen Matcha Trading → Matcha, Hojicha, Green Tea Bags, Fruit Tea Bags, Fruit Tea Concentrate
('SUP-0008', 'INV-0032'),
('SUP-0008', 'INV-0033'),
('SUP-0008', 'INV-0034'),
('SUP-0008', 'INV-0035'),
('SUP-0008', 'INV-0036'),

-- SUP-0009  Fruity Refresh Distributors → Kiwi, Green Apple, Passion Fruit, Pomegranate, Mixed Berry
('SUP-0009', 'INV-0037'),
('SUP-0009', 'INV-0038'),
('SUP-0009', 'INV-0039'),
('SUP-0009', 'INV-0040'),
('SUP-0009', 'INV-0041'),

-- SUP-0010  BlendPro Supplies → Frappe Base, Ice Cream Base, Cheesecake Base
('SUP-0010', 'INV-0042'),
('SUP-0010', 'INV-0043'),
('SUP-0010', 'INV-0045'),

-- SUP-0011  Crumbz & Toppings Co. → Oreo Crumbs, Biscoff Crumbs, Pumpkin Spice Powder
('SUP-0011', 'INV-0044'),
('SUP-0011', 'INV-0020'),
('SUP-0011', 'INV-0023'),

-- SUP-0012  GreenSip Alternatives → Oat Milk
('SUP-0012', 'INV-0047'),

-- SUP-0013  AquaPure Logistics → Water, Ice
('SUP-0013', 'INV-0004'),
('SUP-0013', 'INV-0003'),

-- SUP-0014  CafeSupply Express → Extra Espresso Shot, Whipped Cream
('SUP-0014', 'INV-0050'),
('SUP-0014', 'INV-0009'),

-- SUP-0015  Natures Pantry PH → Pecan Syrup, Honey, Pumpkin Spice Powder
('SUP-0015', 'INV-0046'),
('SUP-0015', 'INV-0007'),
('SUP-0015', 'INV-0023');

GO

-- ============================================================
--  VERIFY
-- ============================================================

-- 15 active suppliers
SELECT COUNT(*) AS supplier_rows
FROM dbo.Suppliers
WHERE is_deleted = 0 AND [status] = 'active';
-- Expected: 15

-- Junction row count
SELECT COUNT(*) AS junction_rows FROM dbo.Supplier_Ingredients;
-- Expected: 54

-- Verify the view works (same shape Java fetchSuppliers expects)
SELECT supplier_id, supplier_name, ingredients, contact_info, address
FROM dbo.vw_Suppliers
WHERE is_deleted = 0 AND [status] = 'active'
ORDER BY supplier_name ASC;

GO