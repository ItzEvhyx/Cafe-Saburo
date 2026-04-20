-- ============================================================
--  SQLQueries/suppliers_setup.sql
--  Cafe Saburo POS — Suppliers Table Setup
--  Dialect: T-SQL (SQL Server)
--
--  !! RUN THIS FILE ONCE TO SET UP YOUR DATABASE !!
--
--  Notes:
--    • ingredients column is a plain VARCHAR — it lists the
--      ingredient names this supplier provides, comma-separated.
--      It is NOT a FK to Inventory; suppliers may serve
--      overlapping ingredients from different sources.
--    • Soft delete  : is_deleted = 1 (row hidden, not removed)
--    • Archive      : [status] = 'archived'
-- ============================================================

SET QUOTED_IDENTIFIER ON;
GO

-- ============================================================
--  DROP TABLE (safe re-run)
-- ============================================================

IF OBJECT_ID('dbo.Suppliers', 'U') IS NOT NULL DROP TABLE dbo.Suppliers;

GO

-- ============================================================
--  CREATE TABLE
-- ============================================================

CREATE TABLE dbo.Suppliers (
    supplier_id    VARCHAR(10)   NOT NULL PRIMARY KEY,
    supplier_name  VARCHAR(100)  NOT NULL,
    ingredients    VARCHAR(500)  NOT NULL,
    contact_info   VARCHAR(100)  NOT NULL,
    address        VARCHAR(200)  NOT NULL,
    is_deleted     BIT           NOT NULL DEFAULT 0,
    [status]       VARCHAR(10)   NOT NULL DEFAULT 'active',
    CONSTRAINT chk_sup_status CHECK ([status] IN ('active', 'archived'))
);

GO

-- ============================================================
--  SEED DATA — 15 Suppliers
--  Ingredients reference items from inventory_setup.sql seed.
-- ============================================================

INSERT INTO dbo.Suppliers (supplier_id, supplier_name, ingredients, contact_info, address) VALUES

-- ── Coffee & Espresso ─────────────────────────────────────
('SUP-0001', 'Benguet Brew Supply',
 'Espresso Beans',
 '+63 912 111 2233',
 '45 Session Road, Baguio City, Benguet'),

-- ── Dairy & Milk ──────────────────────────────────────────
('SUP-0002', 'Magnolia Dairy Distributors',
 'Fresh Milk, Whipped Cream, Condensed Milk, Creamer',
 '+63 923 222 3344',
 '12 Dairy Lane, Quezon City, Metro Manila'),

-- ── Syrups — Core ─────────────────────────────────────────
('SUP-0003', 'SweetBase Co.',
 'Sugar Syrup, Brown Sugar Syrup, Vanilla Syrup, Caramel Syrup, Chocolate Syrup',
 '+63 934 333 4455',
 '78 Rizal Ave., Caloocan City, Metro Manila'),

-- ── Syrups — Coffee Flavors ───────────────────────────────
('SUP-0004', 'Aroma Flavor House',
 'Hazelnut Syrup, Irish Cream Syrup, Butterscotch Syrup, White Chocolate Syrup, Dark Chocolate Syrup, Mocha Sauce',
 '+63 945 444 5566',
 '33 Industrial Blvd., Pasig City, Metro Manila'),

-- ── Specialty & Seasonal Syrups ───────────────────────────
('SUP-0005', 'Niche Botanicals Inc.',
 'Lavender Syrup, Sakura Syrup, Pistachio Syrup, Honey, Agave Syrup',
 '+63 956 555 6677',
 '9 Botanical Ave., Marikina City, Metro Manila'),

-- ── Spreads & Specialty Solids ────────────────────────────
('SUP-0006', 'Continental Spreads PH',
 'Biscoff Spread, Biscoff Crumbs, Pistachio Paste, Cream Cheese, Tiramisu Flavoring',
 '+63 967 666 7788',
 '22 Confection St., Mandaluyong City, Metro Manila'),

-- ── Fruit Purees ──────────────────────────────────────────
('SUP-0007', 'Tropical Puree Solutions',
 'Banana Puree, Strawberry Puree, Blueberry Puree, Mango Puree',
 '+63 978 777 8899',
 '5 Orchard Road, Davao City, Davao del Sur'),

-- ── Matcha & Tea ──────────────────────────────────────────
('SUP-0008', 'Zen Matcha Trading',
 'Matcha Powder, Hojicha Powder, Green Tea Bags, Fruit Tea Bags, Fruit Tea Concentrate',
 '+63 989 888 9900',
 '17 Green Hill St., Cebu City, Cebu'),

-- ── Refresher Syrups ──────────────────────────────────────
('SUP-0009', 'Fruity Refresh Distributors',
 'Kiwi Syrup, Green Apple Syrup, Passion Fruit Syrup, Pomegranate Syrup, Mixed Berry Syrup',
 '+63 990 999 0011',
 '88 Citrus Drive, Cagayan de Oro, Misamis Oriental'),

-- ── Frappe & Smoothie Bases ───────────────────────────────
('SUP-0010', 'BlendPro Supplies',
 'Frappe Base Powder, Ice Cream Base, Cheesecake Base',
 '+63 912 100 2222',
 '60 Blend St., Las Piñas City, Metro Manila'),

-- ── Dry Toppings & Crumbs ─────────────────────────────────
('SUP-0011', 'Crumbz & Toppings Co.',
 'Oreo Crumbs, Biscoff Crumbs, Pumpkin Spice Powder',
 '+63 923 200 3333',
 '14 Baker Lane, Antipolo City, Rizal'),

-- ── Alternative Milks ─────────────────────────────────────
('SUP-0012', 'GreenSip Alternatives',
 'Oat Milk',
 '+63 934 300 4444',
 '3 Eco Ave., Taguig City, Metro Manila'),

-- ── Water & Ice ───────────────────────────────────────────
('SUP-0013', 'AquaPure Logistics',
 'Water, Ice',
 '+63 945 400 5555',
 '101 Purified Blvd., Valenzuela City, Metro Manila'),

-- ── Consumables ───────────────────────────────────────────
('SUP-0014', 'CafeSupply Express',
 'Extra Espresso Shot, Whipped Cream',
 '+63 956 500 6666',
 '55 Express Road, Makati City, Metro Manila'),

-- ── Pecan & Seasonal ──────────────────────────────────────
('SUP-0015', 'Natures Pantry PH',
 'Pecan Syrup, Honey, Pumpkin Spice Powder',
 '+63 967 600 7777',
 '27 Harvest St., Santa Rosa City, Laguna');

GO

-- ============================================================
--  VERIFY — confirm 15 active rows were inserted
-- ============================================================

SELECT COUNT(*) AS seeded_rows
FROM dbo.Suppliers
WHERE is_deleted = 0 AND [status] = 'active';
-- Expected: 15

GO