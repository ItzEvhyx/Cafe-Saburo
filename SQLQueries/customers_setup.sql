-- ============================================================
--  SQLQueries/customers_setup.sql
--  Cafe Saburo POS — Customers + Orders Table Setup
--  Dialect: T-SQL (SQL Server)
--
--  !! RUN THIS FILE ONCE TO SET UP YOUR DATABASE !!
--  Run this BEFORE customers_query.sql or orders_query.sql.
--
--  Rules enforced:
--    • Every customer must have at least one order.
--    • Every order grants +10 loyalty points to the customer.
--    • Soft delete  : is_deleted = 1, deleted_at = timestamp (row hidden, not removed)
--    • Archive      : status = 'archived'  (moved out of active view, still accessible)
-- ============================================================


-- ============================================================
--  DROP (safe re-run — Orders first because of FK)
-- ============================================================

IF OBJECT_ID('dbo.Orders',    'U') IS NOT NULL DROP TABLE dbo.Orders;
IF OBJECT_ID('dbo.Customers', 'U') IS NOT NULL DROP TABLE dbo.Customers;

GO

-- ============================================================
--  CREATE TABLES
-- ============================================================

CREATE TABLE dbo.Customers (
    customer_id     VARCHAR(10)   NOT NULL PRIMARY KEY,
    customer_name   VARCHAR(100)  NOT NULL,
    email           VARCHAR(100),
    phone           VARCHAR(20),
    loyalty_points  INT           NOT NULL DEFAULT 0,
    -- Soft delete
    is_deleted      BIT           NOT NULL DEFAULT 0,
    deleted_at      DATETIME      NULL,
    -- Archive
    status          VARCHAR(10)   NOT NULL DEFAULT 'active',
    CONSTRAINT chk_customer_status CHECK (status IN ('active', 'archived'))
);

CREATE TABLE dbo.Orders (
    order_id      VARCHAR(10)   NOT NULL PRIMARY KEY,
    customer_id   VARCHAR(10)   NOT NULL,
    order_status  VARCHAR(20)   NOT NULL,
    payment_type  VARCHAR(20)   NOT NULL,
    order_date    DATE          NOT NULL,
    total_amount  DECIMAL(8,2)  NOT NULL,
    -- Soft delete
    is_deleted    BIT           NOT NULL DEFAULT 0,
    deleted_at    DATETIME      NULL,
    -- Archive
    status        VARCHAR(10)   NOT NULL DEFAULT 'active',
    CONSTRAINT fk_customer
        FOREIGN KEY (customer_id) REFERENCES dbo.Customers(customer_id),
    CONSTRAINT chk_order_status
        CHECK (order_status IN ('Pending', 'Preparing', 'Completed', 'Cancelled')),
    CONSTRAINT chk_payment
        CHECK (payment_type IN ('Cash', 'GCash', 'Card')),
    CONSTRAINT chk_order_archive_status
        CHECK (status IN ('active', 'archived'))
);

GO

-- ============================================================
--  SEED DATA — 20 Customers (Batch 2: CUST-0021 to CUST-0040)
--  loyalty_points starts at 0; updated after orders are inserted.
--  All seeded records default to: is_deleted = 0, status = 'active'
-- ============================================================

INSERT INTO dbo.Customers (customer_id, customer_name, email, phone) VALUES
    ('CUST-0021', 'Andrea Pascual',    'andrea.p@email.com',     '09171234568'),
    ('CUST-0022', 'Bernard Ocampo',    'bernard.o@email.com',    '09281234568'),
    ('CUST-0023', 'Carla Dizon',       'carla.d@email.com',      '09391234568'),
    ('CUST-0024', 'Danilo Reyes',      'danilo.r@email.com',     '09171112244'),
    ('CUST-0025', 'Ester Magno',       'ester.m@email.com',      '09282223355'),
    ('CUST-0026', 'Fernando Alcala',   'fernando.a@email.com',   '09393334466'),
    ('CUST-0027', 'Gloria Santos',     'gloria.s@email.com',     '09174445577'),
    ('CUST-0028', 'Harold Bautista',   'harold.b@email.com',     '09285556688'),
    ('CUST-0029', 'Irma Fernandez',    'irma.f@email.com',       '09396667799'),
    ('CUST-0030', 'Joel Villanueva',   'joel.v@email.com',       '09177778800'),
    ('CUST-0031', 'Kristine Lim',      'kristine.l@email.com',   '09288889911'),
    ('CUST-0032', 'Leonardo Cruz',     'leonardo.c@email.com',   '09399990022'),
    ('CUST-0033', 'Marisol Torres',    'marisol.t@email.com',    '09170001133'),
    ('CUST-0034', 'Nathaniel Gomez',   'nathaniel.g@email.com',  '09281112244'),
    ('CUST-0035', 'Ophelia Ramos',     'ophelia.r@email.com',    '09392223355'),
    ('CUST-0036', 'Patricio De Leon',  'patricio.dl@email.com',  '09173334466'),
    ('CUST-0037', 'Rowena Aquino',     'rowena.a@email.com',     '09284445577'),
    ('CUST-0038', 'Salvador Navarro',  'salvador.n@email.com',   '09395556688'),
    ('CUST-0039', 'Theresa Castro',    'theresa.c@email.com',    '09176667799'),
    ('CUST-0040', 'Ulysses Mendoza',   'ulysses.m@email.com',    '09287778800');

GO

-- ============================================================
--  SEED DATA — 32 Orders (Batch 2: ORD-0033 to ORD-0064)
--  Every customer (CUST-0021 to CUST-0040) has at least one order.
--  Several customers have multiple orders for varied loyalty points.
--  All seeded records default to: is_deleted = 0, status = 'active'
-- ============================================================

INSERT INTO dbo.Orders (order_id, customer_id, order_status, payment_type, order_date, total_amount) VALUES
    -- Primary orders — one per customer
    ('ORD-0033', 'CUST-0021', 'Completed',  'Cash',  '2025-03-01', 155.00),
    ('ORD-0034', 'CUST-0022', 'Completed',  'GCash', '2025-03-02', 200.00),
    ('ORD-0035', 'CUST-0023', 'Cancelled',  'Card',  '2025-03-03', 175.00),
    ('ORD-0036', 'CUST-0024', 'Completed',  'Cash',  '2025-03-04', 130.00),
    ('ORD-0037', 'CUST-0025', 'Preparing',  'GCash', '2025-03-05', 210.00),
    ('ORD-0038', 'CUST-0026', 'Pending',    'Cash',  '2025-03-06', 105.00),
    ('ORD-0039', 'CUST-0027', 'Completed',  'Card',  '2025-03-07', 185.00),
    ('ORD-0040', 'CUST-0028', 'Cancelled',  'GCash', '2025-03-08', 140.00),
    ('ORD-0041', 'CUST-0029', 'Completed',  'Cash',  '2025-03-09', 160.00),
    ('ORD-0042', 'CUST-0030', 'Completed',  'Card',  '2025-03-10', 195.00),
    ('ORD-0043', 'CUST-0031', 'Preparing',  'GCash', '2025-03-11', 170.00),
    ('ORD-0044', 'CUST-0032', 'Completed',  'Cash',  '2025-03-12', 145.00),
    ('ORD-0045', 'CUST-0033', 'Pending',    'Card',  '2025-03-13', 115.00),
    ('ORD-0046', 'CUST-0034', 'Completed',  'GCash', '2025-03-14', 200.00),
    ('ORD-0047', 'CUST-0035', 'Completed',  'Cash',  '2025-03-15', 175.00),
    ('ORD-0048', 'CUST-0036', 'Cancelled',  'Card',  '2025-03-16', 135.00),
    ('ORD-0049', 'CUST-0037', 'Completed',  'GCash', '2025-03-17', 155.00),
    ('ORD-0050', 'CUST-0038', 'Completed',  'Cash',  '2025-03-18', 190.00),
    ('ORD-0051', 'CUST-0039', 'Preparing',  'Card',  '2025-03-19', 165.00),
    ('ORD-0052', 'CUST-0040', 'Completed',  'GCash', '2025-03-20', 185.00),
    -- Additional orders for varied loyalty point totals
    ('ORD-0053', 'CUST-0021', 'Completed',  'GCash', '2025-03-21', 190.00),
    ('ORD-0054', 'CUST-0021', 'Completed',  'Cash',  '2025-03-22', 145.00),
    ('ORD-0055', 'CUST-0022', 'Completed',  'Card',  '2025-03-23', 180.00),
    ('ORD-0056', 'CUST-0022', 'Pending',    'GCash', '2025-03-24', 120.00),
    ('ORD-0057', 'CUST-0024', 'Completed',  'Cash',  '2025-03-25', 200.00),
    ('ORD-0058', 'CUST-0027', 'Completed',  'Card',  '2025-03-26', 175.00),
    ('ORD-0059', 'CUST-0027', 'Completed',  'GCash', '2025-03-27', 155.00),
    ('ORD-0060', 'CUST-0030', 'Completed',  'Cash',  '2025-03-28', 160.00),
    ('ORD-0061', 'CUST-0034', 'Completed',  'GCash', '2025-03-29', 195.00),
    ('ORD-0062', 'CUST-0034', 'Completed',  'Card',  '2025-03-30', 140.00),
    ('ORD-0063', 'CUST-0038', 'Completed',  'Cash',  '2025-03-31', 175.00),
    ('ORD-0064', 'CUST-0040', 'Completed',  'GCash', '2025-04-01', 200.00);

GO

-- ============================================================
--  UPDATE LOYALTY POINTS
--  Every order = +10 points (only non-deleted orders count).
-- ============================================================

UPDATE dbo.Customers
SET loyalty_points = (
    SELECT COUNT(*) * 10
    FROM dbo.Orders
    WHERE dbo.Orders.customer_id = dbo.Customers.customer_id
      AND dbo.Orders.is_deleted  = 0
);

GO