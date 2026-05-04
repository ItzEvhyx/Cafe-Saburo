-- promotions_query.sql — All prepared-statement queries for promotions_contents.java


-- READ

-- [1] All active promotions, newest first
SELECT promo_id,
       promo_name,
       discount_type,
       CAST(start_date AS VARCHAR) AS start_date,
       CAST(end_date   AS VARCHAR) AS end_date
FROM   promotions
WHERE  status = 'active'
ORDER  BY created_at DESC;

-- [2] All archived promotions, newest first
SELECT promo_id,
       promo_name,
       discount_type,
       CAST(start_date AS VARCHAR) AS start_date,
       CAST(end_date   AS VARCHAR) AS end_date
FROM   promotions
WHERE  status = 'archived'
ORDER  BY created_at DESC;

-- [3] Keyword search across promo_id, promo_name, and discount_type within a given tab
--     Bind: status, '%keyword%' x3
SELECT promo_id,
       promo_name,
       discount_type,
       CAST(start_date AS VARCHAR) AS start_date,
       CAST(end_date   AS VARCHAR) AS end_date
FROM   promotions
WHERE  status = ?
  AND  (LOWER(promo_id)      LIKE LOWER(?)
    OR  LOWER(promo_name)    LIKE LOWER(?)
    OR  LOWER(discount_type) LIKE LOWER(?))
ORDER  BY created_at DESC;

-- [4] Derive the next sequential promo_id by incrementing the current MAX numeric suffix
SELECT COALESCE(
           'PRO-' || LPAD(
               CAST(MAX(CAST(SUBSTRING(promo_id, 5) AS INTEGER)) + 1 AS VARCHAR),
               4, '0'),
           'PRO-0001') AS next_id
FROM   promotions;


-- CREATE

-- [5] Insert a new promotion with status defaulting to active
--     Bind: promo_id, promo_name, discount_type, start_date, end_date
INSERT INTO promotions (promo_id, promo_name, discount_type, start_date, end_date, status)
VALUES (?, ?, ?, ?, ?, 'active');


-- UPDATE

-- [6] Rename a promotion
--     Bind: promo_name, promo_id
UPDATE promotions
SET    promo_name = ?
WHERE  promo_id   = ?;

-- [7] Change the discount type
--     Bind: discount_type, promo_id
UPDATE promotions
SET    discount_type = ?
WHERE  promo_id      = ?;

-- [8] Change the start date
--     Bind: start_date (DATE), promo_id
UPDATE promotions
SET    start_date = ?
WHERE  promo_id   = ?;

-- [9] Change the end date
--     Bind: end_date (DATE), promo_id
UPDATE promotions
SET    end_date = ?
WHERE  promo_id = ?;

-- [10] Archive a single promotion by ID (run once per selected row)
--      Bind: promo_id
UPDATE promotions
SET    status = 'archived'
WHERE  promo_id = ?;

-- [11] Restore a single promotion to active (run once per selected row)
--      Bind: promo_id
UPDATE promotions
SET    status = 'active'
WHERE  promo_id = ?;

-- [12] Archive all active promotions in one pass
UPDATE promotions
SET    status = 'archived'
WHERE  status = 'active';

-- [13] Restore all archived promotions in one pass
UPDATE promotions
SET    status = 'active'
WHERE  status = 'archived';


-- DELETE

-- [14] Hard-delete a single promotion by ID
--      Bind: promo_id
DELETE FROM promotions
WHERE  promo_id = ?;

-- [15] Hard-delete all promotions in the given tab
--      Bind: status ('active' | 'archived')
DELETE FROM promotions
WHERE  status = ?;


-- EXPORT

-- [16] Full row dump for CSV export of the current tab
--      Bind: status
SELECT promo_id,
       promo_name,
       discount_type,
       CAST(start_date AS VARCHAR) AS start_date,
       CAST(end_date   AS VARCHAR) AS end_date,
       status
FROM   promotions
WHERE  status = ?
ORDER  BY created_at DESC;