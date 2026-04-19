-- ============================================================
--  SQLQueries/payments_query.sql
--  Cafe Saburo POS — Payments Table
--  Dialect: T-SQL (SQL Server)
--
--  Run this AFTER orders_query.sql has been executed.
--  Amount is stored and returned as plain DECIMAL — the ₱
--  symbol is applied in Java (payments_contents.java) to
--  avoid encoding issues with JavaFX label rendering.
-- ============================================================


-- ============================================================
--  STEP 1 — Drop & recreate Payments table
-- ============================================================

IF OBJECT_ID('dbo.Payments', 'U') IS NOT NULL
    DROP TABLE dbo.Payments;

GO

CREATE TABLE dbo.Payments (
    payment_id      VARCHAR(10)    NOT NULL PRIMARY KEY,
    order_id        VARCHAR(10)    NOT NULL,
    payment_method  VARCHAR(20)    NOT NULL,
    amount          DECIMAL(8, 2)  NOT NULL,   -- plain number, no symbol
    payment_date    DATE           NOT NULL,
    is_deleted      BIT            NOT NULL DEFAULT 0,
    deleted_at      DATETIME       NULL,
    status          VARCHAR(10)    NOT NULL DEFAULT 'active',

    CONSTRAINT fk_payment_order
        FOREIGN KEY (order_id) REFERENCES dbo.Orders (order_id),
    CONSTRAINT chk_payment_method
        CHECK (payment_method IN ('Cash', 'GCash', 'Card')),
    CONSTRAINT chk_payment_status
        CHECK (status IN ('active', 'archived'))
);

GO

-- ============================================================
--  STEP 2 — Safety check: make sure Orders has rows
-- ============================================================

IF NOT EXISTS (SELECT 1 FROM dbo.Orders WHERE is_deleted = 0)
BEGIN
    RAISERROR(
        'dbo.Orders is empty. Run orders_query.sql first, then re-run this script.',
        16, 1
    );
    RETURN;
END

GO

-- ============================================================
--  STEP 3 — Seed one payment per existing order.
--  Reads order_id, payment_type, total_amount, order_date
--  directly from dbo.Orders — no hardcoded IDs.
-- ============================================================

INSERT INTO dbo.Payments
    (payment_id, order_id, payment_method, amount, payment_date)
SELECT
    'PAY-' + RIGHT('00000' + CAST(
        ROW_NUMBER() OVER (ORDER BY order_date ASC, order_id ASC)
    AS VARCHAR), 5),
    order_id,
    payment_type,
    total_amount,
    order_date
FROM dbo.Orders
WHERE is_deleted = 0;

GO

-- ============================================================
--  VERIFY — confirm all rows inserted cleanly
-- ============================================================

SELECT
    p.payment_id,
    p.order_id,
    p.payment_method,
    p.amount,                        -- plain DECIMAL, Java adds ₱
    p.payment_date,
    p.status
FROM dbo.Payments AS p
ORDER BY p.payment_date ASC, p.payment_id ASC;

GO

-- ============================================================
--  QUERY 1 — Active payments (what payments_contents loads)
--  Returns amount as plain DECIMAL — Java formats it as ₱X,XXX.XX
-- ============================================================

SELECT
    payment_id,
    order_id,
    payment_method,
    amount,
    payment_date
FROM dbo.Payments
WHERE is_deleted = 0
  AND status     = 'active'
ORDER BY payment_date DESC;

GO

-- ============================================================
--  QUERY 2 — Revenue summary by payment method (aggregate-ready)
-- ============================================================

SELECT
    payment_method,
    COUNT(payment_id)          AS num_transactions,
    SUM(amount)                AS total_revenue,
    ROUND(AVG(amount), 2)      AS avg_transaction,
    MIN(amount)                AS min_transaction,
    MAX(amount)                AS max_transaction
FROM dbo.Payments
WHERE is_deleted = 0
GROUP BY payment_method
ORDER BY total_revenue DESC;

GO

-- ============================================================
--  QUERY 3 — Total revenue across all payment methods
-- ============================================================

SELECT
    COUNT(payment_id)          AS total_transactions,
    SUM(amount)                AS grand_total_revenue,
    ROUND(AVG(amount), 2)      AS overall_avg,
    MIN(amount)                AS lowest_transaction,
    MAX(amount)                AS highest_transaction
FROM dbo.Payments
WHERE is_deleted = 0;

GO

-- ============================================================
--  QUERY 4 — JOIN with Orders + Customers (full receipt view)
-- ============================================================

SELECT
    p.payment_id,
    p.order_id,
    c.customer_name,
    o.order_status,
    p.payment_method,
    p.amount,
    p.payment_date
FROM dbo.Payments AS p
INNER JOIN dbo.Orders    AS o ON p.order_id    = o.order_id
INNER JOIN dbo.Customers AS c ON o.customer_id = c.customer_id
WHERE p.is_deleted = 0
  AND p.status     = 'active'
ORDER BY p.payment_date DESC;

GO

-- ============================================================
--  OPTIONAL OPERATIONS (uncomment to use)
-- ============================================================

-- Soft-delete a payment:
-- UPDATE dbo.Payments SET is_deleted = 1, deleted_at = GETDATE()
-- WHERE payment_id = 'PAY-00001';

-- Archive payments for completed orders:
-- UPDATE dbo.Payments SET status = 'archived'
-- WHERE is_deleted = 0
--   AND order_id IN (
--       SELECT order_id FROM dbo.Orders
--       WHERE order_status = 'Completed' AND is_deleted = 0
--   );

-- Restore archived payments:
-- UPDATE dbo.Payments SET status = 'active'
-- WHERE status = 'archived' AND is_deleted = 0;

-- Hard delete soft-deleted rows (admin only):
-- DELETE FROM dbo.Payments WHERE is_deleted = 1;