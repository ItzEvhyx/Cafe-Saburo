-- ============================================================
--  SQLQueries/customers_query.sql
--  Cafe Saburo POS — Customer Queries
--  Dialect: T-SQL (SQL Server)
--
--  !! Run customers_setup.sql FIRST before executing these !!
--
--  Soft delete : is_deleted = 1  → hidden from all normal views
--  Archive     : status = 'archived' → accessible via Archived tab
-- ============================================================


-- ============================================================
--  QUERY 1 — All active, non-deleted customers with loyalty pts
-- ============================================================

SELECT
    c.customer_id,
    c.customer_name,
    COALESCE(o.latest_order_id, 'N/A') AS latest_order_id,
    COALESCE(o.order_count * 10, 0)    AS loyalty_pts
FROM dbo.Customers AS c
LEFT JOIN (
    SELECT
        customer_id,
        COUNT(order_id) AS order_count,
        MAX(order_id)   AS latest_order_id
    FROM dbo.Orders
    WHERE is_deleted = 0
    GROUP BY customer_id
) AS o ON c.customer_id = o.customer_id
WHERE c.is_deleted = 0
  AND c.status     = 'active'
ORDER BY loyalty_pts DESC;

GO

-- ============================================================
--  QUERY 2 — Active customers (at least 1 non-deleted order)
-- ============================================================

SELECT
    c.customer_id,
    c.customer_name,
    COUNT(o.order_id)      AS total_orders,
    COUNT(o.order_id) * 10 AS loyalty_pts
FROM dbo.Customers AS c
INNER JOIN dbo.Orders AS o
    ON  c.customer_id = o.customer_id
    AND o.is_deleted  = 0
WHERE c.is_deleted = 0
  AND c.status     = 'active'
GROUP BY c.customer_id, c.customer_name
ORDER BY loyalty_pts DESC;

GO

-- ============================================================
--  QUERY 3 — Active customers with no orders yet
-- ============================================================

SELECT
    c.customer_id,
    c.customer_name,
    c.email
FROM dbo.Customers AS c
WHERE c.is_deleted = 0
  AND c.status     = 'active'
  AND NOT EXISTS (
      SELECT 1
      FROM dbo.Orders AS o
      WHERE o.customer_id = c.customer_id
        AND o.is_deleted  = 0
  );

GO

-- ============================================================
--  QUERY 4 — Top spenders with loyalty tier (active only)
-- ============================================================

SELECT
    c.customer_name,
    COUNT(o.order_id)       AS total_orders,
    COUNT(o.order_id) * 10  AS loyalty_pts,
    SUM(o.total_amount)     AS lifetime_spend,
    CASE
        WHEN COUNT(o.order_id) >= 5 THEN 'Gold'
        WHEN COUNT(o.order_id) >= 3 THEN 'Silver'
        ELSE                             'Bronze'
    END AS loyalty_tier
FROM dbo.Customers AS c
LEFT JOIN dbo.Orders AS o
    ON  c.customer_id = o.customer_id
    AND o.is_deleted  = 0
WHERE c.is_deleted = 0
  AND c.status     = 'active'
GROUP BY c.customer_id, c.customer_name
ORDER BY loyalty_pts DESC;

GO

-- ============================================================
--  QUERY 5 — Archived customers
-- ============================================================

SELECT
    c.customer_id,
    c.customer_name,
    c.email,
    COALESCE(o.order_count * 10, 0) AS loyalty_pts
FROM dbo.Customers AS c
LEFT JOIN (
    SELECT customer_id, COUNT(order_id) AS order_count
    FROM dbo.Orders
    WHERE is_deleted = 0
    GROUP BY customer_id
) AS o ON c.customer_id = o.customer_id
WHERE c.is_deleted = 0
  AND c.status     = 'archived'
ORDER BY c.customer_name;

GO

-- ============================================================
--  QUERY 6 — Soft-delete a customer
--  (also soft-deletes their orders to keep data consistent)
--  Replace 'CUST-0003' with the target customer_id.
-- ============================================================

UPDATE dbo.Orders
SET    is_deleted = 1,
       deleted_at = GETDATE()
WHERE  customer_id = 'CUST-0003'
  AND  is_deleted  = 0;

UPDATE dbo.Customers
SET    is_deleted = 1,
       deleted_at = GETDATE()
WHERE  customer_id = 'CUST-0003';

GO

-- ============================================================
--  QUERY 7 — Archive a customer (and their orders)
--  Replace 'CUST-0003' with the target customer_id.
-- ============================================================

UPDATE dbo.Orders
SET    status = 'archived'
WHERE  customer_id = 'CUST-0003'
  AND  is_deleted  = 0;

UPDATE dbo.Customers
SET    status = 'archived'
WHERE  customer_id = 'CUST-0003';

GO

-- ============================================================
--  QUERY 8 — Restore an archived customer back to active
--  Replace 'CUST-0003' with the target customer_id.
-- ============================================================

UPDATE dbo.Orders
SET    status = 'active'
WHERE  customer_id = 'CUST-0003'
  AND  is_deleted  = 0;

UPDATE dbo.Customers
SET    status = 'active'
WHERE  customer_id = 'CUST-0003';

GO