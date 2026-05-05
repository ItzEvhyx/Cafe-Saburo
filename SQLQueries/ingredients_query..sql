-- ============================================================
--  SQLQueries/ingredients_query.sql
--  T-SQL (Microsoft SQL Server) — CAFE_SABURO
--
--  FIX: DELETE FROM ingredients was blocked by FK constraint
--  "fk_inv_ingredient" on dbo.Inventory.
--
--  Root cause:
--    DELETE tried to remove rows that dbo.Inventory.ingredient_id
--    still references → FK violation → INSERT then hit duplicate PK.
--
--  Solution: Replace DELETE + INSERT with MERGE (upsert).
--    • Existing rows  → UPDATE ingredient_name + price only.
--    • Missing rows   → INSERT as 'active'.
--    • dbo.Inventory FK references are never touched.
-- ============================================================

-- ------------------------------------------------------------
--  1.  CREATE TABLE (skipped if already exists)
-- ------------------------------------------------------------
IF NOT EXISTS (
    SELECT 1 FROM sys.tables
    WHERE name = 'Ingredients' AND schema_id = SCHEMA_ID('dbo')
)
BEGIN
    CREATE TABLE dbo.Ingredients (
        ingredient_id   VARCHAR(10)    NOT NULL,
        ingredient_name VARCHAR(255)   NOT NULL,
        price           DECIMAL(10,2)  NOT NULL CONSTRAINT df_ingredients_price  DEFAULT 0.00,
        status          VARCHAR(10)    NOT NULL CONSTRAINT df_ingredients_status DEFAULT 'active',

        CONSTRAINT pk_ingredients        PRIMARY KEY (ingredient_id),
        CONSTRAINT chk_ingredient_status CHECK (status IN ('active', 'archived'))
    );
    PRINT 'Table dbo.Ingredients created.';
END
ELSE
BEGIN
    PRINT 'Table dbo.Ingredients already exists — skipping CREATE.';
END;
GO

-- ------------------------------------------------------------
--  2.  UPSERT via MERGE — safe with FK constraints
--      Matched   → UPDATE name + price (status preserved)
--      Unmatched → INSERT as 'active'
-- ------------------------------------------------------------
MERGE dbo.Ingredients AS target
USING (VALUES

    -- ── Base & Essentials ────────────────────────────────
    ('ING-0001', 'Espresso Beans',          8.00),   -- ~₱800/kg, ~100 shots
    ('ING-0002', 'Fresh Milk',             18.75),   -- ~₱75/L, 250 ml/drink
    ('ING-0003', 'Ice',                     5.00),   -- negligible, per cup
    ('ING-0004', 'Water',                   1.00),   -- purified, per drink

    -- ── Sweeteners & General Add-ons ────────────────────
    ('ING-0005', 'Sugar Syrup',             2.40),   -- ~₱120/L, 20 ml/pump
    ('ING-0006', 'Brown Sugar Syrup',       3.00),   -- slightly richer, 20 ml
    ('ING-0007', 'Honey',                   7.00),   -- ~₱350/500 g, 10 g
    ('ING-0008', 'Agave Syrup',            11.20),   -- imported, ~₱280/250 ml, 10 ml
    ('ING-0009', 'Whipped Cream',           6.00),   -- ~₱180/canister, 30 servings
    ('ING-0010', 'Chocolate Syrup',         5.87),   -- ~₱220/750 ml, 20 ml
    ('ING-0011', 'Caramel Syrup',           6.40),   -- ~₱240/750 ml, 20 ml
    ('ING-0012', 'Vanilla Syrup',           5.60),   -- ~₱210/750 ml, 20 ml

    -- ── Coffee Flavor Syrups ─────────────────────────────
    ('ING-0013', 'Hazelnut Syrup',          6.93),   -- ~₱260/750 ml, 20 ml
    ('ING-0014', 'Irish Cream Syrup',       7.47),   -- ~₱280/750 ml, 20 ml
    ('ING-0015', 'Butterscotch Syrup',      6.67),   -- ~₱250/750 ml, 20 ml
    ('ING-0016', 'White Chocolate Syrup',   8.00),   -- ~₱300/750 ml, 20 ml
    ('ING-0017', 'Dark Chocolate Syrup',    8.53),   -- ~₱320/750 ml, 20 ml
    ('ING-0018', 'Mocha Sauce',             9.33),   -- ~₱350/750 ml, 20 ml

    -- ── Specialty Ingredients ────────────────────────────
    ('ING-0019', 'Biscoff Spread',         15.00),   -- ~₱400/400 g, 15 g
    ('ING-0020', 'Biscoff Crumbs',          7.20),   -- ~₱180/200 g, 8 g
    ('ING-0021', 'Pistachio Syrup',        11.20),   -- imported, ~₱420/750 ml, 20 ml
    ('ING-0022', 'Pistachio Paste',        34.80),   -- ~₱580/250 g, 15 g
    ('ING-0023', 'Pumpkin Spice Powder',    6.60),   -- ~₱220/100 g, 3 g
    ('ING-0024', 'Lavender Syrup',          7.60),   -- ~₱380/750 ml, 15 ml
    ('ING-0025', 'Sakura Syrup',            9.00),   -- seasonal import, ~₱450/750 ml, 15 ml
    ('ING-0026', 'Tiramisu Flavoring',      9.00),   -- ~₱300/500 ml, 15 ml
    ('ING-0027', 'Cream Cheese',           14.40),   -- ~₱180/250 g, 20 g
    ('ING-0028', 'Banana Puree',            4.80),   -- ~₱120/kg, 40 g
    ('ING-0029', 'Strawberry Puree',        8.80),   -- ~₱220/kg, 40 g
    ('ING-0030', 'Blueberry Puree',        11.20),   -- ~₱280/kg, 40 g
    ('ING-0031', 'Mango Puree',             6.00),   -- local, ~₱150/kg, 40 g

    -- ── Matcha & Tea-Based ───────────────────────────────
    ('ING-0032', 'Matcha Powder',          40.00),   -- culinary grade, ~₱800/100 g, 5 g
    ('ING-0033', 'Hojicha Powder',         32.50),   -- ~₱650/100 g, 5 g
    ('ING-0034', 'Green Tea Bags',          2.40),   -- ~₱120/50-bag box
    ('ING-0035', 'Fruit Tea Bags',          3.60),   -- ~₱180/50-bag box
    ('ING-0036', 'Fruit Tea Concentrate',  10.50),   -- ~₱350/L, 30 ml

    -- ── Refresher / Fruit Syrups ─────────────────────────
    ('ING-0037', 'Kiwi Syrup',              6.93),   -- ~₱260/750 ml, 20 ml
    ('ING-0038', 'Green Apple Syrup',       6.67),   -- ~₱250/750 ml, 20 ml
    ('ING-0039', 'Passion Fruit Syrup',     7.73),   -- ~₱290/750 ml, 20 ml
    ('ING-0040', 'Pomegranate Syrup',       8.27),   -- ~₱310/750 ml, 20 ml
    ('ING-0041', 'Mixed Berry Syrup',       7.20),   -- ~₱270/750 ml, 20 ml

    -- ── Frappe / Smoothie Ingredients ───────────────────
    ('ING-0042', 'Frappe Base Powder',     14.40),   -- ~₱480/kg, 30 g
    ('ING-0043', 'Ice Cream Base',         21.00),   -- ~₱350/kg, 60 g
    ('ING-0044', 'Oreo Crumbs',             7.00),   -- ~₱140/200 g, 10 g
    ('ING-0045', 'Cheesecake Base',        10.40),   -- ~₱260/500 g, 20 g
    ('ING-0046', 'Pecan Syrup',            10.67),   -- specialty, ~₱400/750 ml, 20 ml

    -- ── Milk Alternatives ────────────────────────────────
    ('ING-0047', 'Oat Milk',               45.00),   -- ~₱180/L, 250 ml/drink

    -- ── Creamers & Condensed ─────────────────────────────
    ('ING-0048', 'Creamer',                 3.30),   -- ~₱220/kg powder, 15 g
    ('ING-0049', 'Condensed Milk',          6.54),   -- ~₱85/390 g can, 30 g

    -- ── Consumables ──────────────────────────────────────
    ('ING-0050', 'Extra Espresso Shot',     8.00)    -- same cost as base shot

) AS source (ingredient_id, ingredient_name, price)
ON target.ingredient_id = source.ingredient_id

WHEN MATCHED THEN
    UPDATE SET
        target.ingredient_name = source.ingredient_name,
        target.price           = source.price
        -- status intentionally excluded: archived rows stay archived

WHEN NOT MATCHED BY TARGET THEN
    INSERT (ingredient_id, ingredient_name, price, status)
    VALUES (source.ingredient_id, source.ingredient_name, source.price, 'active');

PRINT 'MERGE complete — 50 ingredient rows upserted.';
GO

-- ------------------------------------------------------------
--  3.  VERIFY
--      First run : 50 active, 0 archived
--      Re-run    : counts reflect current status of each row
-- ------------------------------------------------------------
SELECT
    status,
    COUNT(*) AS row_count
FROM dbo.Ingredients
GROUP BY status
ORDER BY status;
GO