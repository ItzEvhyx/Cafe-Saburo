-- customers_query.sql — Cafe Saburo POS: Customer Queries
-- Dialect: T-SQL (SQL Server) | Run customers_setup.sql FIRST before executing these
-- Soft delete: is_deleted = 1 → hidden from all normal views | Archive: status = 'archived' → accessible via Archived tab


-- Returns all active customers with their latest order ID and accumulated loyalty points
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


-- Returns active customers who have placed at least one non-deleted order, with their total order count and loyalty points
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


-- Returns active customers who have never placed an order
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


-- Returns active customers ranked by spend with Gold/Silver/Bronze loyalty tiers (Gold ≥5 orders, Silver ≥3)
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


-- Returns all archived customers with their accumulated loyalty points
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


-- Soft-deletes a customer and all their orders; replace 'CUST-0003' with the target customer_id
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


-- Archives a customer and their active orders; replace 'CUST-0003' with the target customer_id
UPDATE dbo.Orders
SET    status = 'archived'
WHERE  customer_id = 'CUST-0003'
  AND  is_deleted  = 0;

UPDATE dbo.Customers
SET    status = 'archived'
WHERE  customer_id = 'CUST-0003';
GO


-- Restores an archived customer and their orders back to active; replace 'CUST-0003' with the target customer_id
UPDATE dbo.Orders
SET    status = 'active'
WHERE  customer_id = 'CUST-0003'
  AND  is_deleted  = 0;

UPDATE dbo.Customers
SET    status = 'active'
WHERE  customer_id = 'CUST-0003';
GO