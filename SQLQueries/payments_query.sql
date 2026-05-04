-- payments_query.sql — Cafe Saburo POS: Payments table, triggers, stored procedures, and seed data
-- Dialect: T-SQL (SQL Server) | Run this AFTER orders_query.sql
-- Amount stored as plain DECIMAL; currency symbol applied in Java (payments_contents.java) to avoid JavaFX encoding issues


-- Audit log: records every archive, restore, and soft-delete event on the Payments table
IF OBJECT_ID('dbo.PaymentAuditLog', 'U') IS NOT NULL
    DROP TABLE dbo.PaymentAuditLog;
GO

CREATE TABLE dbo.PaymentAuditLog (
    log_id       INT          NOT NULL IDENTITY(1,1) PRIMARY KEY,
    payment_id   VARCHAR(10)  NOT NULL,
    old_status   VARCHAR(10)  NULL,
    new_status   VARCHAR(10)  NULL,
    changed_by   VARCHAR(50)  NULL DEFAULT SYSTEM_USER,
    changed_at   DATETIME     NOT NULL DEFAULT GETDATE(),
    action_type  VARCHAR(10)  NOT NULL   -- 'ARCHIVE' | 'RESTORE' | 'DELETE'
);
GO


-- fn_FormatAmount: returns a DECIMAL formatted as "X,XXX.XX" for consistent display across analytics procs
IF OBJECT_ID('dbo.fn_FormatAmount', 'FN') IS NOT NULL
    DROP FUNCTION dbo.fn_FormatAmount;
GO

CREATE FUNCTION dbo.fn_FormatAmount (@amount DECIMAL(8,2))
RETURNS VARCHAR(20)
AS
BEGIN
    RETURN FORMAT(@amount, 'N2');
END;
GO

-- fn_VolumeLabel: maps a transaction count to a human-readable tier (High ≥10, Medium ≥5, Low <5)
IF OBJECT_ID('dbo.fn_VolumeLabel', 'FN') IS NOT NULL
    DROP FUNCTION dbo.fn_VolumeLabel;
GO

CREATE FUNCTION dbo.fn_VolumeLabel (@count INT)
RETURNS VARCHAR(10)
AS
BEGIN
    RETURN CASE
        WHEN @count >= 10 THEN 'High'
        WHEN @count >= 5  THEN 'Medium'
        ELSE                   'Low'
    END;
END;
GO


-- fn_ActivePayments: inline TVF returning non-deleted active payments; pass @method = NULL to include all methods
IF OBJECT_ID('dbo.fn_ActivePayments', 'IF') IS NOT NULL
    DROP FUNCTION dbo.fn_ActivePayments;
GO

CREATE FUNCTION dbo.fn_ActivePayments (@method VARCHAR(20) = NULL)
RETURNS TABLE
AS
RETURN (
    SELECT
        payment_id,
        order_id,
        payment_method,
        amount,
        payment_date
    FROM dbo.Payments
    WHERE is_deleted = 0
      AND status     = 'active'
      AND (@method IS NULL OR payment_method = @method)
);
GO


-- Payments: core transaction table linked to Orders; supports soft-delete and archive lifecycle
IF OBJECT_ID('dbo.Payments', 'U') IS NOT NULL
    DROP TABLE dbo.Payments;
GO

CREATE TABLE dbo.Payments (
    payment_id      VARCHAR(10)    NOT NULL PRIMARY KEY,
    order_id        VARCHAR(10)    NOT NULL,
    payment_method  VARCHAR(20)    NOT NULL,
    amount          DECIMAL(8, 2)  NOT NULL,
    payment_date    DATE           NOT NULL,
    is_deleted      BIT            NOT NULL DEFAULT 0,
    deleted_at      DATETIME       NULL,
    status          VARCHAR(10)    NOT NULL DEFAULT 'active',

    CONSTRAINT fk_payment_order  FOREIGN KEY (order_id) REFERENCES dbo.Orders (order_id),
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('Cash', 'GCash', 'Card')),
    CONSTRAINT chk_payment_status CHECK (status IN ('active', 'archived'))
);
GO


-- trg_SetPaymentDate: defaults payment_date to today on INSERT when the caller omits it
CREATE OR ALTER TRIGGER dbo.trg_SetPaymentDate
ON dbo.Payments
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE p
    SET    p.payment_date = CAST(GETDATE() AS DATE)
    FROM   dbo.Payments AS p
    INNER JOIN inserted AS i ON p.payment_id = i.payment_id
    WHERE  i.payment_date IS NULL;
END;
GO

-- trg_PreventDeleteActive: blocks hard-deletes on active rows; callers must archive the row first
CREATE OR ALTER TRIGGER dbo.trg_PreventDeleteActive
ON dbo.Payments
INSTEAD OF DELETE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1 FROM deleted WHERE status = 'active' AND is_deleted = 0
    )
    BEGIN
        RAISERROR('Cannot hard-delete active payments. Archive them first.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;

    DELETE FROM dbo.Payments
    WHERE payment_id IN (SELECT payment_id FROM deleted);
END;
GO

-- trg_LogArchiveEvent: inserts one audit row whenever a payment's status column changes
CREATE OR ALTER TRIGGER dbo.trg_LogArchiveEvent
ON dbo.Payments
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.PaymentAuditLog (payment_id, old_status, new_status, action_type)
    SELECT
        d.payment_id,
        d.status,
        i.status,
        CASE
            WHEN i.status = 'archived' THEN 'ARCHIVE'
            WHEN i.status = 'active'   THEN 'RESTORE'
            ELSE                            'UPDATE'
        END
    FROM   deleted  AS d
    INNER JOIN inserted AS i ON d.payment_id = i.payment_id
    WHERE  d.status <> i.status;
END;
GO

-- trg_LogSoftDelete: inserts a DELETE audit row when is_deleted flips from 0 to 1
CREATE OR ALTER TRIGGER dbo.trg_LogSoftDelete
ON dbo.Payments
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.PaymentAuditLog (payment_id, old_status, new_status, action_type)
    SELECT d.payment_id, d.status, NULL, 'DELETE'
    FROM   deleted  AS d
    INNER JOIN inserted AS i ON d.payment_id = i.payment_id
    WHERE  d.is_deleted = 0 AND i.is_deleted = 1;
END;
GO


-- usp_GetPayments: returns active or archived payments with optional search filter on payment_id or order_id
CREATE OR ALTER PROCEDURE dbo.usp_GetPayments
    @tab    VARCHAR(10),
    @search NVARCHAR(100) = NULL
AS
BEGIN
    SET NOCOUNT ON;

    SELECT payment_id, order_id, payment_method, amount
    FROM dbo.Payments
    WHERE is_deleted = 0
      AND status     = @tab
      AND (@search IS NULL
           OR payment_id LIKE '%' + @search + '%'
           OR order_id   LIKE '%' + @search + '%')
    ORDER BY payment_date DESC;
END;
GO

-- usp_ArchivePayments: bulk-archives a comma-separated list of payment IDs in a single UPDATE
CREATE OR ALTER PROCEDURE dbo.usp_ArchivePayments
    @ids NVARCHAR(MAX)
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE dbo.Payments
    SET    status = 'archived'
    WHERE  is_deleted = 0
      AND  payment_id IN (SELECT TRIM(value) FROM STRING_SPLIT(@ids, ','));
END;
GO

-- usp_RestorePayments: bulk-restores a comma-separated list of archived payment IDs back to active
CREATE OR ALTER PROCEDURE dbo.usp_RestorePayments
    @ids NVARCHAR(MAX)
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE dbo.Payments
    SET    status = 'active'
    WHERE  is_deleted = 0
      AND  payment_id IN (SELECT TRIM(value) FROM STRING_SPLIT(@ids, ','));
END;
GO

-- usp_HardDeleteAll: permanently removes all non-soft-deleted rows for a given status tab (intended for 'archived' only)
CREATE OR ALTER PROCEDURE dbo.usp_HardDeleteAll
    @tab VARCHAR(10)
AS
BEGIN
    SET NOCOUNT ON;

    DELETE FROM dbo.Payments
    WHERE is_deleted = 0
      AND status     = @tab;
END;
GO


-- Analytics procs below each return two result sets: (1) detail rows, (2) a single summary row

-- usp_PaymentSum: lists all active payments with a grand-total summary row
CREATE OR ALTER PROCEDURE dbo.usp_PaymentSum
AS
BEGIN
    SET NOCOUNT ON;

    SELECT payment_id, order_id, payment_method, dbo.fn_FormatAmount(amount) AS amount
    FROM dbo.fn_ActivePayments(NULL)
    ORDER BY payment_date DESC;

    SELECT 'TOTAL' AS label, dbo.fn_FormatAmount(SUM(amount)) AS grand_total
    FROM dbo.fn_ActivePayments(NULL);
END;
GO

-- usp_PaymentAvg: lists all active payments with an overall average-amount summary row
CREATE OR ALTER PROCEDURE dbo.usp_PaymentAvg
AS
BEGIN
    SET NOCOUNT ON;

    SELECT payment_id, order_id, payment_method, dbo.fn_FormatAmount(amount) AS amount
    FROM dbo.fn_ActivePayments(NULL)
    ORDER BY payment_date DESC;

    SELECT 'AVERAGE' AS label, dbo.fn_FormatAmount(ROUND(AVG(amount), 2)) AS avg_amount
    FROM dbo.fn_ActivePayments(NULL);
END;
GO

-- usp_PaymentCount: groups active payments by method with a volume label; summary row shows total transaction count
CREATE OR ALTER PROCEDURE dbo.usp_PaymentCount
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @total INT;
    SELECT @total = COUNT(payment_id) FROM dbo.fn_ActivePayments(NULL);

    SELECT
        payment_method                        AS [Payment Method],
        COUNT(payment_id)                     AS [Transactions],
        dbo.fn_VolumeLabel(COUNT(payment_id)) AS [Volume]
    FROM dbo.fn_ActivePayments(NULL)
    GROUP BY payment_method
    HAVING COUNT(payment_id) >= 1
    ORDER BY COUNT(payment_id) DESC;

    SELECT 'TOTAL' AS label, @total AS total_transactions;
END;
GO

-- usp_PaymentHighest: top 10 highest-value active payments joined to customer name; summary row shows the maximum amount
CREATE OR ALTER PROCEDURE dbo.usp_PaymentHighest
AS
BEGIN
    SET NOCOUNT ON;

    SELECT TOP 10
        p.payment_id,
        c.customer_name               AS [Customer],
        p.payment_method,
        dbo.fn_FormatAmount(p.amount) AS amount
    FROM dbo.fn_ActivePayments(NULL) AS p
    INNER JOIN dbo.Orders    AS o ON p.order_id    = o.order_id
    INNER JOIN dbo.Customers AS c ON o.customer_id = c.customer_id
    WHERE o.is_deleted = 0
    ORDER BY p.amount DESC;

    SELECT 'HIGHEST' AS label, dbo.fn_FormatAmount(MAX(p.amount)) AS highest_amount
    FROM dbo.fn_ActivePayments(NULL) AS p
    INNER JOIN dbo.Orders AS o ON p.order_id = o.order_id
    WHERE o.is_deleted = 0;
END;
GO

-- usp_PaymentLowest: top 10 lowest-value active payments joined to customer name; summary row shows the minimum amount
CREATE OR ALTER PROCEDURE dbo.usp_PaymentLowest
AS
BEGIN
    SET NOCOUNT ON;

    SELECT TOP 10
        p.payment_id,
        c.customer_name               AS [Customer],
        p.payment_method,
        dbo.fn_FormatAmount(p.amount) AS amount
    FROM dbo.fn_ActivePayments(NULL) AS p
    INNER JOIN dbo.Orders    AS o ON p.order_id    = o.order_id
    INNER JOIN dbo.Customers AS c ON o.customer_id = c.customer_id
    WHERE o.is_deleted = 0
    ORDER BY p.amount ASC;

    SELECT 'LOWEST' AS label, dbo.fn_FormatAmount(MIN(p.amount)) AS lowest_amount
    FROM dbo.fn_ActivePayments(NULL) AS p
    INNER JOIN dbo.Orders AS o ON p.order_id = o.order_id
    WHERE o.is_deleted = 0;
END;
GO

-- usp_ByMethod: revenue breakdown per payment method across all non-deleted statuses; summary row shows grand totals
CREATE OR ALTER PROCEDURE dbo.usp_ByMethod
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @grandTotal DECIMAL(18,2);
    DECLARE @grandCount INT;

    SELECT @grandTotal = SUM(amount), @grandCount = COUNT(payment_id)
    FROM dbo.Payments
    WHERE is_deleted = 0;

    SELECT
        payment_method                            AS [Method],
        COUNT(payment_id)                         AS [Transactions],
        dbo.fn_FormatAmount(SUM(amount))          AS [Total],
        dbo.fn_FormatAmount(ROUND(AVG(amount),2)) AS [Average],
        dbo.fn_FormatAmount(MIN(amount))          AS [Lowest],
        dbo.fn_FormatAmount(MAX(amount))          AS [Highest]
    FROM dbo.Payments
    WHERE is_deleted = 0
    GROUP BY payment_method
    HAVING COUNT(payment_id) >= 1
    ORDER BY SUM(amount) DESC;

    SELECT 'TOTAL' AS label, @grandCount AS total_transactions, dbo.fn_FormatAmount(@grandTotal) AS grand_total;
END;
GO

-- usp_ByCustomer: lists customers whose total spend exceeds the overall per-customer average; summary row shows the grand total
CREATE OR ALTER PROCEDURE dbo.usp_ByCustomer
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        c.customer_name                    AS [Customer],
        COUNT(p.payment_id)                AS [Payments],
        dbo.fn_FormatAmount(SUM(p.amount)) AS [Total Spent]
    FROM dbo.fn_ActivePayments(NULL) AS p
    INNER JOIN dbo.Orders    AS o ON p.order_id    = o.order_id
    INNER JOIN dbo.Customers AS c ON o.customer_id = c.customer_id
    WHERE o.is_deleted = 0
    GROUP BY c.customer_name
    HAVING SUM(p.amount) > (
        SELECT AVG(sub_total)
        FROM (
            SELECT SUM(p2.amount) AS sub_total
            FROM   dbo.Payments  AS p2
            INNER JOIN dbo.Orders AS o2 ON p2.order_id = o2.order_id
            WHERE  p2.is_deleted = 0
            GROUP  BY o2.customer_id
        ) AS sub
    )
    ORDER BY SUM(p.amount) DESC;

    SELECT 'TOTAL' AS label,
        dbo.fn_FormatAmount(ISNULL((
            SELECT SUM(p3.amount)
            FROM   dbo.fn_ActivePayments(NULL) AS p3
            INNER JOIN dbo.Orders AS o3 ON p3.order_id = o3.order_id
            WHERE  o3.is_deleted = 0
        ), 0)) AS grand_total;
END;
GO

-- usp_Daily: groups active payments by date showing per-day transaction count, total, and average; summary row shows overall totals
CREATE OR ALTER PROCEDURE dbo.usp_Daily
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @grandTotal DECIMAL(18,2);
    DECLARE @grandCount INT;

    SELECT @grandTotal = SUM(p.amount), @grandCount = COUNT(p.payment_id)
    FROM dbo.fn_ActivePayments(NULL) AS p
    INNER JOIN dbo.Orders AS o ON p.order_id = o.order_id
    WHERE o.is_deleted = 0;

    SELECT
        CONVERT(VARCHAR(10), p.payment_date, 23)    AS [Date],
        COUNT(p.payment_id)                         AS [Transactions],
        dbo.fn_FormatAmount(SUM(p.amount))          AS [Daily Total],
        dbo.fn_FormatAmount(ROUND(AVG(p.amount),2)) AS [Daily Avg]
    FROM dbo.fn_ActivePayments(NULL) AS p
    INNER JOIN dbo.Orders AS o ON p.order_id = o.order_id
    WHERE o.is_deleted = 0
    GROUP BY p.payment_date
    ORDER BY p.payment_date DESC;

    SELECT 'TOTAL' AS label, @grandCount AS total_transactions, dbo.fn_FormatAmount(@grandTotal) AS grand_total;
END;
GO

-- usp_AboveAvg: returns active payments that exceed the overall average; summary row shows the sum of those above-average payments
CREATE OR ALTER PROCEDURE dbo.usp_AboveAvg
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @avg DECIMAL(8,2);
    SELECT @avg = AVG(amount) FROM dbo.fn_ActivePayments(NULL);

    SELECT
        p.payment_id,
        c.customer_name                  AS [Customer],
        p.payment_method,
        dbo.fn_FormatAmount(p.amount)    AS amount
    FROM dbo.fn_ActivePayments(NULL) AS p
    INNER JOIN dbo.Orders    AS o ON p.order_id    = o.order_id
    INNER JOIN dbo.Customers AS c ON o.customer_id = c.customer_id
    WHERE o.is_deleted = 0 AND p.amount > @avg
    ORDER BY p.amount DESC;

    SELECT 'TOTAL' AS label,
        dbo.fn_FormatAmount(ISNULL((
            SELECT SUM(p2.amount)
            FROM   dbo.fn_ActivePayments(NULL) AS p2
            INNER JOIN dbo.Orders AS o2 ON p2.order_id = o2.order_id
            WHERE  o2.is_deleted = 0 AND p2.amount > @avg
        ), 0)) AS total_above_avg;
END;
GO

-- usp_NoPayments: lists active customers with no linked non-deleted payment records; summary row shows the count
CREATE OR ALTER PROCEDURE dbo.usp_NoPayments
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @count INT;

    SELECT @count = COUNT(*)
    FROM dbo.Customers
    WHERE is_deleted = 0
      AND (customer_id NOT IN (
               SELECT o.customer_id
               FROM   dbo.Orders    AS o
               INNER JOIN dbo.Payments AS p ON o.order_id = p.order_id
               WHERE  o.is_deleted = 0 AND p.is_deleted = 0
           )
           OR customer_name IS NULL);

    SELECT
        customer_id                          AS [Customer ID],
        ISNULL(customer_name,  '(none)')     AS [Name],
        ISNULL(customer_email, '-')          AS [Email]
    FROM dbo.Customers
    WHERE is_deleted = 0
      AND (customer_id NOT IN (
               SELECT o.customer_id
               FROM   dbo.Orders    AS o
               INNER JOIN dbo.Payments AS p ON o.order_id = p.order_id
               WHERE  o.is_deleted = 0 AND p.is_deleted = 0
           )
           OR customer_name IS NULL)
    ORDER BY customer_name ASC;

    SELECT 'COUNT' AS label, @count AS customer_count;
END;
GO


-- Guard: abort if Orders is empty, since payment seed data depends on existing order rows
IF NOT EXISTS (SELECT 1 FROM dbo.Orders WHERE is_deleted = 0)
BEGIN
    RAISERROR('dbo.Orders is empty. Run orders_query.sql first, then re-run this script.', 16, 1);
    RETURN;
END;
GO


-- Seed payments: auto-generate one payment per existing order using ROW_NUMBER for sequential PAY- IDs
INSERT INTO dbo.Payments (payment_id, order_id, payment_method, amount, payment_date)
SELECT
    'PAY-' + RIGHT('00000' + CAST(ROW_NUMBER() OVER (ORDER BY order_date ASC, order_id ASC) AS VARCHAR), 5),
    order_id,
    payment_type,
    total_amount,
    order_date
FROM dbo.Orders
WHERE is_deleted = 0;
GO


-- Verification: confirm seeded rows, then spot-check key procs and functions
SELECT p.payment_id, p.order_id, p.payment_method, p.amount, p.payment_date, p.status
FROM dbo.Payments AS p
ORDER BY p.payment_date ASC, p.payment_id ASC;
GO

EXEC dbo.usp_GetPayments @tab = 'active';
GO
EXEC dbo.usp_PaymentSum;
GO
EXEC dbo.usp_ByMethod;
GO

-- Scalar function smoke test: expects "1,234.50", "High", "Medium", "Low"
SELECT
    dbo.fn_FormatAmount(1234.5) AS formatted,
    dbo.fn_VolumeLabel(12)      AS vol_high,
    dbo.fn_VolumeLabel(6)       AS vol_medium,
    dbo.fn_VolumeLabel(2)       AS vol_low;
GO

SELECT * FROM dbo.fn_ActivePayments(NULL);
GO
SELECT * FROM dbo.fn_ActivePayments('Cash');
GO

SELECT * FROM dbo.PaymentAuditLog ORDER BY changed_at DESC;
GO