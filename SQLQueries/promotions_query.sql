-- ============================================================
--  promotions_query.sql
--  All prepared-statement queries for promotions_contents.java
-- ============================================================


-- ── READ ───────────────────────────────────────────────────

-- [1] Fetch all active promotions (newest first)
SELECT promo_id,
       promo_name,
       discount_type,
       CAST(start_date AS VARCHAR) AS start_date,
       CAST(end_date   AS VARCHAR) AS end_date
FROM   promotions
WHERE  status = 'active'
ORDER  BY created_at DESC;

-- [2] Fetch all archived promotions (newest first)
SELECT promo_id,
       promo_name,
       discount_type,
       CAST(start_date AS VARCHAR) AS start_date,
       CAST(end_date   AS VARCHAR) AS end_date
FROM   promotions
WHERE  status = 'archived'
ORDER  BY created_at DESC;

-- [3] Search promotions by keyword (across id, name, discount_type)
--     Bind: ? = '%keyword%'  (x3)
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

-- [4] Generate next promo_id  (auto-increment style)
SELECT COALESCE(
           'PRO-' || LPAD(
               CAST(MAX(CAST(SUBSTRING(promo_id, 5) AS INTEGER)) + 1 AS VARCHAR),
               4, '0'),
           'PRO-0001')  AS next_id
FROM   promotions;


-- ── CREATE ─────────────────────────────────────────────────

-- [5] Insert a new promotion
--     Bind: promo_id, promo_name, discount_type, start_date, end_date
INSERT INTO promotions (promo_id, promo_name, discount_type, start_date, end_date, status)
VALUES (?, ?, ?, ?, ?, 'active');


-- ── UPDATE ─────────────────────────────────────────────────

-- [6] Update promotion name
--     Bind: promo_name, promo_id
UPDATE promotions
SET    promo_name = ?
WHERE  promo_id   = ?;

-- [7] Update discount type
--     Bind: discount_type, promo_id
UPDATE promotions
SET    discount_type = ?
WHERE  promo_id      = ?;

-- [8] Update start date
--     Bind: start_date (DATE), promo_id
UPDATE promotions
SET    start_date = ?
WHERE  promo_id   = ?;

-- [9] Update end date
--     Bind: end_date (DATE), promo_id
UPDATE promotions
SET    end_date = ?
WHERE  promo_id = ?;

-- [10] Archive selected promotions (by ID list — run once per ID)
--      Bind: promo_id
UPDATE promotions
SET    status = 'archived'
WHERE  promo_id = ?;

-- [11] Restore selected promotions to active (run once per ID)
--      Bind: promo_id
UPDATE promotions
SET    status = 'active'
WHERE  promo_id = ?;

-- [12] Archive ALL active promotions at once
UPDATE promotions
SET    status = 'archived'
WHERE  status = 'active';

-- [13] Restore ALL archived promotions at once
UPDATE promotions
SET    status = 'active'
WHERE  status = 'archived';


-- ── DELETE ─────────────────────────────────────────────────

-- [14] Hard-delete a single promotion
--      Bind: promo_id
DELETE FROM promotions
WHERE  promo_id = ?;

-- [15] Hard-delete all promotions in current tab (active or archived)
--      Bind: status ('active' | 'archived')
DELETE FROM promotions
WHERE  status = ?;


-- ── EXPORT CSV helper ──────────────────────────────────────

-- [16] Full dump for CSV export (current tab)
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