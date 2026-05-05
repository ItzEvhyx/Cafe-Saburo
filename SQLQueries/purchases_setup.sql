-- purchases_setup.sql — Cafe Saburo POS: Purchases table definition and seed data
-- Dialect: T-SQL (SQL Server)
-- Run AFTER ingredients_setup.sql, inventory_setup.sql, and suppliers_setup.sql.
--
-- Line-item model: one row = one supplier + one inventory item.
-- Group by purchase_group_id for multi-item orders.

-- ── STEP 1: Drop Purchases (safe re-run) ─────────────────────────────────────────────────────────
IF OBJECT_ID('dbo.Purchases', 'U') IS NOT NULL DROP TABLE dbo.Purchases;
GO

-- ── STEP 2: Create Purchases ──────────────────────────────────────────────────────────────────────
CREATE TABLE dbo.Purchases (
    purchase_id       VARCHAR(10)     NOT NULL,
    purchase_group_id VARCHAR(10)     NOT NULL,
    supplier_id       VARCHAR(10)     NOT NULL,
    inventory_id      VARCHAR(10)     NOT NULL,
    quantity_ordered  DECIMAL(10, 2)  NOT NULL DEFAULT 1,
    order_date        DATE            NOT NULL,
    is_deleted        BIT             NOT NULL DEFAULT 0,
    [status]          VARCHAR(15)     NOT NULL DEFAULT 'Pending',

    CONSTRAINT pk_purchases            PRIMARY KEY (purchase_id),
    CONSTRAINT fk_purchases_supplier   FOREIGN KEY (supplier_id)
        REFERENCES dbo.Suppliers  (supplier_id),
    CONSTRAINT fk_purchases_inventory  FOREIGN KEY (inventory_id)
        REFERENCES dbo.Inventory  (inventory_id),
    CONSTRAINT chk_pur_status          CHECK ([status] IN (
        'Pending', 'Approved', 'On its way', 'Delivered', 'Cancelled', 'archived'
    ))
);
GO

-- ── STEP 3: Seed 40 purchase line items across 15 order groups ───────────────────────────────────
INSERT INTO dbo.Purchases
    (purchase_id, purchase_group_id, supplier_id, inventory_id, quantity_ordered, order_date, [status])
VALUES
-- PG-001
('PUR-0001', 'PG-001', 'SUP-0001', 'INV-0001', 10.00, '2025-01-05', 'Delivered'),

-- PG-002
('PUR-0002', 'PG-002', 'SUP-0002', 'INV-0002', 20.00, '2025-01-10', 'Delivered'),
('PUR-0003', 'PG-002', 'SUP-0002', 'INV-0009', 15.00, '2025-01-10', 'Delivered'),
('PUR-0004', 'PG-002', 'SUP-0002', 'INV-0049',  8.00, '2025-01-10', 'Delivered'),

-- PG-003
('PUR-0005', 'PG-003', 'SUP-0003', 'INV-0005',  6.00, '2025-01-15', 'Delivered'),
('PUR-0006', 'PG-003', 'SUP-0003', 'INV-0006',  6.00, '2025-01-15', 'Delivered'),
('PUR-0007', 'PG-003', 'SUP-0003', 'INV-0012',  4.00, '2025-01-15', 'Delivered'),

-- PG-004
('PUR-0008', 'PG-004', 'SUP-0004', 'INV-0011',  3.00, '2025-01-20', 'Delivered'),
('PUR-0009', 'PG-004', 'SUP-0004', 'INV-0013',  3.00, '2025-01-20', 'Delivered'),
('PUR-0010', 'PG-004', 'SUP-0004', 'INV-0016',  3.00, '2025-01-20', 'Delivered'),
('PUR-0011', 'PG-004', 'SUP-0004', 'INV-0018',  2.00, '2025-01-20', 'Delivered'),

-- PG-005
('PUR-0012', 'PG-005', 'SUP-0008', 'INV-0032',  2.00, '2025-02-01', 'Delivered'),
('PUR-0013', 'PG-005', 'SUP-0008', 'INV-0033',  1.50, '2025-02-01', 'Delivered'),
('PUR-0014', 'PG-005', 'SUP-0008', 'INV-0034', 80.00, '2025-02-01', 'Delivered'),
('PUR-0015', 'PG-005', 'SUP-0008', 'INV-0035', 80.00, '2025-02-01', 'Delivered'),

-- PG-006
('PUR-0016', 'PG-006', 'SUP-0007', 'INV-0028',  4.00, '2025-02-08', 'Delivered'),
('PUR-0017', 'PG-006', 'SUP-0007', 'INV-0029',  4.00, '2025-02-08', 'Delivered'),
('PUR-0018', 'PG-006', 'SUP-0007', 'INV-0041',  3.00, '2025-02-08', 'Delivered'),

-- PG-007
('PUR-0019', 'PG-007', 'SUP-0006', 'INV-0019',  4.00, '2025-02-14', 'Delivered'),
('PUR-0020', 'PG-007', 'SUP-0006', 'INV-0020',  3.00, '2025-02-14', 'Delivered'),
('PUR-0021', 'PG-007', 'SUP-0006', 'INV-0027',  4.00, '2025-02-14', 'Delivered'),

-- PG-008
('PUR-0022', 'PG-008', 'SUP-0010', 'INV-0042',  4.00, '2025-02-20', 'Approved'),
('PUR-0023', 'PG-008', 'SUP-0010', 'INV-0043',  4.00, '2025-02-20', 'Approved'),
('PUR-0024', 'PG-008', 'SUP-0010', 'INV-0045',  2.00, '2025-02-20', 'Approved'),

-- PG-009
('PUR-0025', 'PG-009', 'SUP-0013', 'INV-0003', 30.00, '2025-03-01', 'Delivered'),
('PUR-0026', 'PG-009', 'SUP-0013', 'INV-0004', 80.00, '2025-03-01', 'Delivered'),

-- PG-010
('PUR-0027', 'PG-010', 'SUP-0014', 'INV-0009', 10.00, '2025-03-05', 'Delivered'),
('PUR-0028', 'PG-010', 'SUP-0014', 'INV-0014',  5.00, '2025-03-05', 'Delivered'),
('PUR-0029', 'PG-010', 'SUP-0014', 'INV-0050',200.00, '2025-03-05', 'Delivered'),

-- PG-011
('PUR-0030', 'PG-011', 'SUP-0005', 'INV-0021',  1.50, '2025-03-10', 'Pending'),
('PUR-0031', 'PG-011', 'SUP-0005', 'INV-0024',  2.00, '2025-03-10', 'Pending'),
('PUR-0032', 'PG-011', 'SUP-0005', 'INV-0025',  2.00, '2025-03-10', 'Pending'),

-- PG-012
('PUR-0033', 'PG-012', 'SUP-0009', 'INV-0037',  3.00, '2025-03-15', 'Approved'),
('PUR-0034', 'PG-012', 'SUP-0009', 'INV-0038',  3.00, '2025-03-15', 'Approved'),
('PUR-0035', 'PG-012', 'SUP-0009', 'INV-0039',  2.00, '2025-03-15', 'Approved'),
('PUR-0036', 'PG-012', 'SUP-0009', 'INV-0041',  2.00, '2025-03-15', 'Approved'),

-- PG-013
('PUR-0037', 'PG-013', 'SUP-0012', 'INV-0047',  8.00, '2025-03-20', 'Delivered'),

-- PG-014
('PUR-0038', 'PG-014', 'SUP-0011', 'INV-0023',  1.00, '2025-03-25', 'Approved'),
('PUR-0039', 'PG-014', 'SUP-0011', 'INV-0044',  2.00, '2025-03-25', 'Approved'),

-- PG-015
('PUR-0040', 'PG-015', 'SUP-0015', 'INV-0007',  3.00, '2025-04-01', 'Pending'),
('PUR-0041', 'PG-015', 'SUP-0015', 'INV-0023',  1.00, '2025-04-01', 'Pending'),
('PUR-0042', 'PG-015', 'SUP-0015', 'INV-0046',  2.00, '2025-04-01', 'Pending');
GO

-- ── VERIFY ────────────────────────────────────────────────────────────────────────────────────────
-- Expected total: 42
SELECT COUNT(*) AS total_purchase_lines FROM dbo.Purchases WHERE is_deleted = 0;
GO

-- Status distribution
SELECT [status], COUNT(*) AS cnt
FROM dbo.Purchases
WHERE is_deleted = 0
GROUP BY [status]
ORDER BY [status];
GO