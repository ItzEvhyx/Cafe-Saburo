-- ============================================================
--  SQLQueries/purchases_setup.sql
--  Cafe Saburo POS — Purchases Table Setup
--  Dialect: T-SQL (SQL Server)
--
--  !! RUN THIS FILE ONCE TO SET UP YOUR DATABASE !!
--  Run AFTER inventory_setup.sql and suppliers_setup.sql.
--
--  Design:
--    • Each purchase row = one supplier ordering one inventory
--      item (line-item model).  Group by purchase_group_id to
--      reconstruct a multi-item "order".
--    • supplier_id  → FK to dbo.Suppliers
--    • inventory_id → FK to dbo.Inventory
--    • ingredient   → denormalized from dbo.Inventory via JOIN
--                     (no extra column needed — resolved at query time)
--    • Soft delete  : is_deleted = 1
--    • Status flow  : 'Pending' → 'Approved' → 'On its way'
--                     → 'Delivered'  or  'Cancelled'
-- ============================================================

SET QUOTED_IDENTIFIER ON;
GO

-- ============================================================
--  TRUNCATE + RESET (safe re-run without dropping FKs)
-- ============================================================

IF OBJECT_ID('dbo.Purchases', 'U') IS NOT NULL
BEGIN
    -- Remove FK constraints temporarily so TRUNCATE works
    ALTER TABLE dbo.Purchases NOCHECK CONSTRAINT ALL;
    TRUNCATE TABLE dbo.Purchases;
    ALTER TABLE dbo.Purchases CHECK CONSTRAINT ALL;
END;

GO

-- ============================================================
--  DROP + RECREATE TABLE (full reset path)
-- ============================================================

IF OBJECT_ID('dbo.Purchases', 'U') IS NOT NULL DROP TABLE dbo.Purchases;

GO

CREATE TABLE dbo.Purchases (
    purchase_id       VARCHAR(10)     NOT NULL PRIMARY KEY,
    purchase_group_id VARCHAR(10)     NOT NULL,
    supplier_id       VARCHAR(10)     NOT NULL,
    inventory_id      VARCHAR(10)     NOT NULL,
    quantity_ordered  DECIMAL(10, 2)  NOT NULL DEFAULT 1,
    order_date        DATE            NOT NULL,
    is_deleted        BIT             NOT NULL DEFAULT 0,
    [status]          VARCHAR(15)     NOT NULL DEFAULT 'Pending',

    CONSTRAINT fk_purchases_supplier  FOREIGN KEY (supplier_id)
        REFERENCES dbo.Suppliers (supplier_id),

    CONSTRAINT fk_purchases_inventory FOREIGN KEY (inventory_id)
        REFERENCES dbo.Inventory (inventory_id),

    CONSTRAINT chk_pur_status CHECK ([status] IN (
        'Pending', 'Approved', 'On its way', 'Delivered', 'Cancelled', 'archived'
    ))
);

GO

-- ============================================================
--  SEED DATA — 40 purchase line items across 15 order groups
-- ============================================================

INSERT INTO dbo.Purchases
    (purchase_id, purchase_group_id, supplier_id, inventory_id, quantity_ordered, order_date, [status])
VALUES

-- ── Order PG-001 : Benguet Brew Supply — Espresso Beans ────
('PUR-0001', 'PG-001', 'SUP-0001', 'INV-0001', 10.00, '2025-01-05', 'Delivered'),

-- ── Order PG-002 : Magnolia Dairy Distributors ──────────────
('PUR-0002', 'PG-002', 'SUP-0002', 'INV-0002', 20.00, '2025-01-10', 'Delivered'),
('PUR-0003', 'PG-002', 'SUP-0002', 'INV-0009', 15.00, '2025-01-10', 'Delivered'),
('PUR-0004', 'PG-002', 'SUP-0002', 'INV-0049', 8.00,  '2025-01-10', 'Delivered'),

-- ── Order PG-003 : SweetBase Co. ────────────────────────────
('PUR-0005', 'PG-003', 'SUP-0003', 'INV-0005', 6.00,  '2025-01-15', 'Delivered'),
('PUR-0006', 'PG-003', 'SUP-0003', 'INV-0006', 6.00,  '2025-01-15', 'Delivered'),
('PUR-0007', 'PG-003', 'SUP-0003', 'INV-0012', 4.00,  '2025-01-15', 'Delivered'),

-- ── Order PG-004 : Aroma Flavor House ───────────────────────
('PUR-0008', 'PG-004', 'SUP-0004', 'INV-0013', 3.00,  '2025-01-20', 'Delivered'),
('PUR-0009', 'PG-004', 'SUP-0004', 'INV-0016', 3.00,  '2025-01-20', 'Delivered'),
('PUR-0010', 'PG-004', 'SUP-0004', 'INV-0018', 2.00,  '2025-01-20', 'Delivered'),

-- ── Order PG-005 : Zen Matcha Trading ───────────────────────
('PUR-0011', 'PG-005', 'SUP-0008', 'INV-0032', 2.00,  '2025-02-01', 'Delivered'),
('PUR-0012', 'PG-005', 'SUP-0008', 'INV-0033', 1.50,  '2025-02-01', 'Delivered'),
('PUR-0013', 'PG-005', 'SUP-0008', 'INV-0034', 80.00, '2025-02-01', 'Delivered'),
('PUR-0014', 'PG-005', 'SUP-0008', 'INV-0035', 80.00, '2025-02-01', 'Delivered'),

-- ── Order PG-006 : Tropical Puree Solutions ─────────────────
('PUR-0015', 'PG-006', 'SUP-0007', 'INV-0028', 4.00,  '2025-02-08', 'Delivered'),
('PUR-0016', 'PG-006', 'SUP-0007', 'INV-0029', 4.00,  '2025-02-08', 'Delivered'),
('PUR-0017', 'PG-006', 'SUP-0007', 'INV-0031', 3.00,  '2025-02-08', 'Delivered'),

-- ── Order PG-007 : Continental Spreads PH ───────────────────
('PUR-0018', 'PG-007', 'SUP-0006', 'INV-0019', 4.00,  '2025-02-14', 'Delivered'),
('PUR-0019', 'PG-007', 'SUP-0006', 'INV-0020', 3.00,  '2025-02-14', 'Delivered'),
('PUR-0020', 'PG-007', 'SUP-0006', 'INV-0027', 4.00,  '2025-02-14', 'Delivered'),

-- ── Order PG-008 : BlendPro Supplies ────────────────────────
('PUR-0021', 'PG-008', 'SUP-0010', 'INV-0042', 4.00,  '2025-02-20', 'Approved'),
('PUR-0022', 'PG-008', 'SUP-0010', 'INV-0043', 4.00,  '2025-02-20', 'Approved'),
('PUR-0023', 'PG-008', 'SUP-0010', 'INV-0045', 2.00,  '2025-02-20', 'Approved'),

-- ── Order PG-009 : AquaPure Logistics ───────────────────────
('PUR-0024', 'PG-009', 'SUP-0013', 'INV-0003', 30.00, '2025-03-01', 'Delivered'),
('PUR-0025', 'PG-009', 'SUP-0013', 'INV-0004', 80.00, '2025-03-01', 'Delivered'),

-- ── Order PG-010 : CafeSupply Express ───────────────────────
('PUR-0026', 'PG-010', 'SUP-0014', 'INV-0050', 200.00,'2025-03-05', 'Delivered'),
('PUR-0027', 'PG-010', 'SUP-0014', 'INV-0009', 10.00, '2025-03-05', 'Delivered'),

-- ── Order PG-011 : Niche Botanicals Inc. ────────────────────
('PUR-0028', 'PG-011', 'SUP-0005', 'INV-0024', 2.00,  '2025-03-10', 'Pending'),
('PUR-0029', 'PG-011', 'SUP-0005', 'INV-0025', 2.00,  '2025-03-10', 'Pending'),
('PUR-0030', 'PG-011', 'SUP-0005', 'INV-0021', 1.50,  '2025-03-10', 'Pending'),

-- ── Order PG-012 : Fruity Refresh Distributors ──────────────
('PUR-0031', 'PG-012', 'SUP-0009', 'INV-0037', 3.00,  '2025-03-15', 'Approved'),
('PUR-0032', 'PG-012', 'SUP-0009', 'INV-0038', 3.00,  '2025-03-15', 'Approved'),
('PUR-0033', 'PG-012', 'SUP-0009', 'INV-0039', 2.00,  '2025-03-15', 'Approved'),
('PUR-0034', 'PG-012', 'SUP-0009', 'INV-0041', 2.00,  '2025-03-15', 'Approved'),

-- ── Order PG-013 : GreenSip Alternatives ────────────────────
('PUR-0035', 'PG-013', 'SUP-0012', 'INV-0047', 8.00,  '2025-03-20', 'Delivered'),

-- ── Order PG-014 : Crumbz & Toppings Co. ────────────────────
('PUR-0036', 'PG-014', 'SUP-0011', 'INV-0044', 2.00,  '2025-03-25', 'Approved'),
('PUR-0037', 'PG-014', 'SUP-0011', 'INV-0023', 1.00,  '2025-03-25', 'Approved'),

-- ── Order PG-015 : Natures Pantry PH ────────────────────────
('PUR-0038', 'PG-015', 'SUP-0015', 'INV-0046', 2.00,  '2025-04-01', 'Pending'),
('PUR-0039', 'PG-015', 'SUP-0015', 'INV-0007', 3.00,  '2025-04-01', 'Pending'),
('PUR-0040', 'PG-015', 'SUP-0015', 'INV-0023', 1.00,  '2025-04-01', 'Pending');

GO

-- ============================================================
--  VERIFY
-- ============================================================

SELECT COUNT(*) AS total_purchase_lines FROM dbo.Purchases WHERE is_deleted = 0;
-- Expected: 40

SELECT [status], COUNT(*) AS cnt
FROM dbo.Purchases
WHERE is_deleted = 0
GROUP BY [status]
ORDER BY [status];
-- Expected: Approved=10, Delivered=22, Pending=8

GO