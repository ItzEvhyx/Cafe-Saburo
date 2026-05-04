-- orders_query.sql — Cafe Saburo POS: Orders and Customers tables with seed data and example queries
-- Dialect: T-SQL (SQL Server)
-- Soft delete: is_deleted = 1, deleted_at = timestamp (row hidden from UI, retained in DB)
-- Archive:     status = 'archived' (row moved to Archived tab, removed from Active view)


-- Drop existing tables to allow safe re-run
IF OBJECT_ID('dbo.Orders',    'U') IS NOT NULL DROP TABLE dbo.Orders;
IF OBJECT_ID('dbo.Customers', 'U') IS NOT NULL DROP TABLE dbo.Customers;
GO


-- Customers: stores customer profiles with loyalty points and soft-delete/archive lifecycle
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

-- Orders: stores transaction records linked to Customers with soft-delete and archive lifecycle
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


-- Seed 8 customers
INSERT INTO Customers (customer_id, customer_name, email, phone) VALUES
    ('CUST-0001', 'Ana Reyes',        'ana.reyes@email.com',     '09171234567'),
    ('CUST-0002', 'Ben Santos',       'ben.santos@email.com',    '09281234567'),
    ('CUST-0003', 'Clara Mendoza',    'clara.m@email.com',       '09391234567'),
    ('CUST-0004', 'Diego Cruz',       'diego.cruz@email.com',    '09171112233'),
    ('CUST-0005', 'Elena Villanueva', 'elena.v@email.com',       '09282223344'),
    ('CUST-0006', 'Felix Ramos',      'felix.r@email.com',       '09393334455'),
    ('CUST-0007', 'Grace Lim',        'grace.lim@email.com',     '09174445566'),
    ('CUST-0008', 'Hector Navarro',   'hector.n@email.com',      '09285556677');


-- Seed 20 orders spread across January 2025
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


-- Q1: Default active view — all non-deleted, active orders sorted newest first
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


-- Q2: In-progress orders paid by GCash or Card (excludes Cancelled via NOT)
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


-- Q3: Customers with more than 2 non-deleted orders, ranked by total spend
SELECT
    customer_id,
    COUNT(order_id)   AS total_orders,
    SUM(total_amount) AS total_spent
FROM Orders
WHERE is_deleted = 0
GROUP BY customer_id
HAVING COUNT(order_id) > 2
ORDER BY total_spent DESC;


-- Q4: Completed active orders joined with customer names, sorted oldest first
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


-- Q5: Customers whose average order value exceeds ₱150, ranked by average descending
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


-- Q6: Each order labeled with a spend tier (High/Medium/Low) and an activity flag (Active/Done/Inactive)
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


-- Q7: View — active completed orders enriched with customer contact info and spend tier
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


-- Q8: Non-deleted orders whose total exceeds the overall average (subquery in WHERE)
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


-- Q9: Customers ranked by lifetime spend using a derived table in FROM
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


-- Q10: Active customers who have at least one non-deleted Cancelled order (EXISTS subquery)
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


-- Q11: Soft-delete a single order by ID (marks deleted without removing the row)
UPDATE Orders
SET    is_deleted = 1,
       deleted_at = GETDATE()
WHERE  order_id   = 'ORD-0003';


-- Q12: Bulk-archive all completed orders placed before a cutoff date
UPDATE Orders
SET    status = 'archived'
WHERE  order_status = 'Completed'
  AND  order_date   < '2025-01-20'
  AND  is_deleted   = 0;


-- Q13: Retrieve all archived, non-deleted orders joined with customer names
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


-- Q14: Restore a single archived order back to active status
UPDATE Orders
SET    status = 'active'
WHERE  order_id = 'ORD-0001';


-- Q15: Permanently purge all soft-deleted rows from both tables (admin use only)
DELETE FROM Orders    WHERE is_deleted = 1;
DELETE FROM Customers WHERE is_deleted = 1;