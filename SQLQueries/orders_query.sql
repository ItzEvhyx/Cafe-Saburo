-- ============================================================
--  SQLQueries/orders_query.sql
--  Cafe Saburo POS — Orders Table
--  Dialect: T-SQL (SQL Server)
--
--  Soft delete : is_deleted = 1, deleted_at = timestamp
--                Row is hidden from UI but kept in DB.
--  Archive     : status = 'archived'
--                Row moves to Archived tab, off Active view.
-- ============================================================


-- ============================================================
--  DROP TABLES (safe re-run)
-- ============================================================

IF OBJECT_ID('dbo.Orders',    'U') IS NOT NULL DROP TABLE dbo.Orders;
IF OBJECT_ID('dbo.Customers', 'U') IS NOT NULL DROP TABLE dbo.Customers;

GO

-- ============================================================
--  CREATE TABLES
-- ============================================================

CREATE TABLE Customers (
    customer_id     VARCHAR(10)   NOT NULL PRIMARY KEY,
    customer_name   VARCHAR(100)  NOT NULL,
    email           VARCHAR(100),
    phone           VARCHAR(20),
    loyalty_points  INT           NOT NULL DEFAULT 0,
    is_deleted      BIT           NOT NULL DEFAULT 0,
    deleted_at      DATETIME      NULL,
    status          VARCHAR(10)   NOT NULL DEFAULT 'active',
    CONSTRAINT chk_customer_status CHECK (status IN ('active', 'archived'))
);

CREATE TABLE Orders (
    order_id      VARCHAR(10)   NOT NULL PRIMARY KEY,
    customer_id   VARCHAR(10)   NOT NULL,
    order_status  VARCHAR(20)   NOT NULL,
    payment_type  VARCHAR(20)   NOT NULL,
    order_date    DATE          NOT NULL,
    total_amount  DECIMAL(8,2)  NOT NULL,
    is_deleted    BIT           NOT NULL DEFAULT 0,
    deleted_at    DATETIME      NULL,
    status        VARCHAR(10)   NOT NULL DEFAULT 'active',
    CONSTRAINT fk_customer
        FOREIGN KEY (customer_id) REFERENCES Customers(customer_id),
    CONSTRAINT chk_order_status
        CHECK (order_status IN ('Pending', 'Preparing', 'Completed', 'Cancelled')),
    CONSTRAINT chk_payment
        CHECK (payment_type IN ('Cash', 'GCash', 'Card')),
    CONSTRAINT chk_order_archive_status
        CHECK (status IN ('active', 'archived'))
);

GO

-- ============================================================
--  SEED DATA — Customers
-- ============================================================

INSERT INTO Customers (customer_id, customer_name, email, phone) VALUES
    ('CUST-0001', 'Ana Reyes',        'ana.reyes@email.com',     '09171234567'),
    ('CUST-0002', 'Ben Santos',       'ben.santos@email.com',    '09281234567'),
    ('CUST-0003', 'Clara Mendoza',    'clara.m@email.com',       '09391234567'),
    ('CUST-0004', 'Diego Cruz',       'diego.cruz@email.com',    '09171112233'),
    ('CUST-0005', 'Elena Villanueva', 'elena.v@email.com',       '09282223344'),
    ('CUST-0006', 'Felix Ramos',      'felix.r@email.com',       '09393334455'),
    ('CUST-0007', 'Grace Lim',        'grace.lim@email.com',     '09174445566'),
    ('CUST-0008', 'Hector Navarro',   'hector.n@email.com',      '09285556677');


-- ============================================================
--  SEED DATA — Orders (20 entries)
-- ============================================================

INSERT INTO Orders (order_id, customer_id, order_status, payment_type, order_date, total_amount) VALUES
    ('ORD-0001', 'CUST-0001', 'Completed',  'Cash',  '2025-01-10', 150.00),
    ('ORD-0002', 'CUST-0002', 'Completed',  'GCash', '2025-01-11', 195.00),
    ('ORD-0003', 'CUST-0003', 'Cancelled',  'Card',  '2025-01-12', 170.00),
    ('ORD-0004', 'CUST-0004', 'Completed',  'Cash',  '2025-01-13', 120.00),
    ('ORD-0005', 'CUST-0005', 'Preparing',  'GCash', '2025-01-14', 200.00),
    ('ORD-0006', 'CUST-0001', 'Completed',  'GCash', '2025-01-15', 190.00),
    ('ORD-0007', 'CUST-0006', 'Pending',    'Cash',  '2025-01-16', 100.00),
    ('ORD-0008', 'CUST-0002', 'Completed',  'Card',  '2025-01-17', 160.00),
    ('ORD-0009', 'CUST-0007', 'Cancelled',  'GCash', '2025-01-18', 125.00),
    ('ORD-0010', 'CUST-0003', 'Completed',  'Cash',  '2025-01-19', 150.00),
    ('ORD-0011', 'CUST-0008', 'Preparing',  'Card',  '2025-01-20', 180.00),
    ('ORD-0012', 'CUST-0004', 'Completed',  'GCash', '2025-01-21', 195.00),
    ('ORD-0013', 'CUST-0005', 'Completed',  'Cash',  '2025-01-22', 170.00),
    ('ORD-0014', 'CUST-0006', 'Cancelled',  'Card',  '2025-01-23', 140.00),
    ('ORD-0015', 'CUST-0007', 'Completed',  'GCash', '2025-01-24', 200.00),
    ('ORD-0016', 'CUST-0001', 'Pending',    'Cash',  '2025-01-25', 120.00),
    ('ORD-0017', 'CUST-0008', 'Completed',  'GCash', '2025-01-26', 150.00),
    ('ORD-0018', 'CUST-0002', 'Preparing',  'Cash',  '2025-01-27', 190.00),
    ('ORD-0019', 'CUST-0003', 'Completed',  'Card',  '2025-01-28', 160.00),
    ('ORD-0020', 'CUST-0004', 'Completed',  'GCash', '2025-01-29', 175.00);

GO

-- ============================================================
--  QUERY 1 — Active, non-deleted orders (default view)
-- ============================================================

SELECT
    order_id,
    customer_id,
    order_status,
    payment_type,
    order_date,
    total_amount
FROM Orders
WHERE is_deleted = 0
  AND status     = 'active'
ORDER BY order_date DESC;


-- ============================================================
--  QUERY 2 — WHERE + LOGICAL OPERATORS (AND / OR / NOT)
--  Active, non-deleted orders that are Pending or Preparing,
--  paid by GCash or Card.
-- ============================================================

SELECT
    order_id,
    customer_id,
    order_status,
    payment_type,
    total_amount
FROM Orders
WHERE is_deleted = 0
  AND status     = 'active'
  AND (order_status = 'Pending'   OR order_status = 'Preparing')
  AND (payment_type = 'GCash'     OR payment_type = 'Card')
  AND NOT order_status = 'Cancelled';


-- ============================================================
--  QUERY 3 — GROUP BY + HAVING + COUNT + SUM
--  Active customers with more than 2 non-deleted orders.
-- ============================================================

SELECT
    customer_id,
    COUNT(order_id)   AS total_orders,
    SUM(total_amount) AS total_spent
FROM Orders
WHERE is_deleted = 0
GROUP BY customer_id
HAVING COUNT(order_id) > 2
ORDER BY total_spent DESC;


-- ============================================================
--  QUERY 4 — JOIN + WHERE + ORDER BY
--  Completed, active, non-deleted orders with customer names.
-- ============================================================

SELECT
    o.order_id,
    c.customer_name,
    o.order_status,
    o.payment_type,
    o.order_date,
    o.total_amount
FROM Orders AS o
INNER JOIN Customers AS c ON o.customer_id = c.customer_id
WHERE o.is_deleted = 0
  AND o.status     = 'active'
  AND o.order_status = 'Completed'
ORDER BY o.order_date ASC;


-- ============================================================
--  QUERY 5 — JOIN + GROUP BY + AVG + HAVING
--  Customers (non-deleted) whose avg order value exceeds ₱150.
-- ============================================================

SELECT
    c.customer_name,
    COUNT(o.order_id)              AS num_orders,
    ROUND(AVG(o.total_amount), 2)  AS avg_order_value,
    SUM(o.total_amount)            AS lifetime_spend
FROM Orders AS o
INNER JOIN Customers AS c ON o.customer_id = c.customer_id
WHERE o.is_deleted = 0
  AND c.is_deleted = 0
GROUP BY c.customer_name
HAVING AVG(o.total_amount) > 150.00
ORDER BY avg_order_value DESC;


-- ============================================================
--  QUERY 6 — CASE EXPRESSION + JOIN
--  Spend tier + activity flag for active, non-deleted orders.
-- ============================================================

SELECT
    o.order_id,
    c.customer_name,
    o.payment_type,
    o.total_amount,
    CASE
        WHEN o.total_amount >= 190 THEN 'High'
        WHEN o.total_amount >= 150 THEN 'Medium'
        ELSE                            'Low'
    END AS spend_tier,
    CASE
        WHEN o.order_status IN ('Pending', 'Preparing') THEN 'Active'
        WHEN o.order_status = 'Completed'               THEN 'Done'
        ELSE                                                  'Inactive'
    END AS order_flag
FROM Orders AS o
INNER JOIN Customers AS c ON o.customer_id = c.customer_id
WHERE o.is_deleted = 0
  AND c.is_deleted = 0
ORDER BY o.total_amount DESC;


-- ============================================================
--  QUERY 7 — VIEW: active completed orders with spend tier
-- ============================================================

GO

CREATE OR ALTER VIEW vw_completed_orders AS
SELECT
    o.order_id,
    c.customer_name,
    c.email,
    o.payment_type,
    o.order_date,
    o.total_amount,
    CASE
        WHEN o.total_amount >= 190 THEN 'High'
        WHEN o.total_amount >= 150 THEN 'Medium'
        ELSE                            'Low'
    END AS spend_tier
FROM Orders AS o
INNER JOIN Customers AS c ON o.customer_id = c.customer_id
WHERE o.order_status = 'Completed'
  AND o.is_deleted   = 0
  AND o.status       = 'active';

GO

SELECT * FROM vw_completed_orders ORDER BY order_date DESC;


-- ============================================================
--  QUERY 8 — SUBQUERY in WHERE
--  Non-deleted orders above the overall average total.
-- ============================================================

SELECT
    order_id,
    customer_id,
    order_status,
    total_amount
FROM Orders
WHERE is_deleted   = 0
  AND total_amount > (
      SELECT AVG(total_amount)
      FROM Orders
      WHERE is_deleted = 0
  )
ORDER BY total_amount DESC;


-- ============================================================
--  QUERY 9 — SUBQUERY in FROM (derived table) + JOIN
--  Rank customers by lifetime spend (non-deleted orders only).
-- ============================================================

SELECT
    c.customer_name,
    spend_summary.total_orders,
    spend_summary.lifetime_spend
FROM (
    SELECT
        customer_id,
        COUNT(order_id)   AS total_orders,
        SUM(total_amount) AS lifetime_spend
    FROM Orders
    WHERE is_deleted = 0
    GROUP BY customer_id
) AS spend_summary
INNER JOIN Customers AS c ON spend_summary.customer_id = c.customer_id
WHERE c.is_deleted = 0
ORDER BY spend_summary.lifetime_spend DESC;


-- ============================================================
--  QUERY 10 — EXISTS: customers with at least one Cancelled order
-- ============================================================

SELECT
    c.customer_id,
    c.customer_name,
    c.email
FROM Customers AS c
WHERE c.is_deleted = 0
  AND EXISTS (
      SELECT 1
      FROM Orders AS o
      WHERE o.customer_id  = c.customer_id
        AND o.order_status = 'Cancelled'
        AND o.is_deleted   = 0
  );


-- ============================================================
--  QUERY 11 — Soft delete an order (instead of DELETE)
--  Marks an order as deleted without removing the row.
-- ============================================================

UPDATE Orders
SET    is_deleted = 1,
       deleted_at = GETDATE()
WHERE  order_id   = 'ORD-0003';  -- example order


-- ============================================================
--  QUERY 12 — Archive old orders (completed before a cutoff)
-- ============================================================

UPDATE Orders
SET    status = 'archived'
WHERE  order_status = 'Completed'
  AND  order_date   < '2025-01-20'
  AND  is_deleted   = 0;


-- ============================================================
--  QUERY 13 — View archived orders
-- ============================================================

SELECT
    o.order_id,
    c.customer_name,
    o.order_status,
    o.payment_type,
    o.order_date,
    o.total_amount
FROM Orders AS o
INNER JOIN Customers AS c ON o.customer_id = c.customer_id
WHERE o.status     = 'archived'
  AND o.is_deleted = 0
ORDER BY o.order_date DESC;


-- ============================================================
--  QUERY 14 — Restore an archived order back to active
-- ============================================================

UPDATE Orders
SET    status = 'active'
WHERE  order_id = 'ORD-0001';  -- example order


-- ============================================================
--  QUERY 15 — Permanently purge all soft-deleted rows (admin only)
-- ============================================================

DELETE FROM Orders    WHERE is_deleted = 1;
DELETE FROM Customers WHERE is_deleted = 1;