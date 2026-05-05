-- promotions_query.sql  SQL Server (T-SQL) edition
-- All prepared-statement queries for promotions_contents.java
-- Placeholder style kept as ? for JDBC documentation; actual runtime params are bound by Java.


-- ============================================================
-- TABLE DEFINITION  (run once)
-- ============================================================
IF OBJECT_ID('promotions', 'U') IS NULL
BEGIN
    CREATE TABLE promotions (
        promo_id      VARCHAR(10)  NOT NULL PRIMARY KEY,
        promo_name    VARCHAR(120) NOT NULL,
        discount_type VARCHAR(80)  NOT NULL,
        start_date    DATE         NOT NULL,
        end_date      DATE         NOT NULL,
        status        VARCHAR(10)  NOT NULL DEFAULT 'active',
        created_at    DATETIME     NOT NULL DEFAULT GETDATE()
    );
END;
GO


-- ============================================================
-- SAMPLE DATA  (25 promotions)
-- ============================================================
INSERT INTO promotions (promo_id, promo_name, discount_type, start_date, end_date, status) VALUES
('PRO-0001', 'Summer Splash Sale',           'Percentage (15%)',         '2025-06-01', '2025-06-30', 'active'),
('PRO-0002', 'Mid-Year Mega Deals',          'Percentage (20%)',         '2025-07-01', '2025-07-15', 'active'),
('PRO-0003', 'Back to School Bonanza',       'Fixed (500 Off)',          '2025-08-01', '2025-08-31', 'active'),
('PRO-0004', 'Ber Month Kickoff',            'BOGO',                     '2025-09-01', '2025-09-10', 'active'),
('PRO-0005', 'Holiday Early Bird',           'Percentage (25%)',         '2025-10-15', '2025-11-01', 'active'),
('PRO-0006', 'November Payday Treat',        'Fixed (500 Off)',          '2025-11-15', '2025-11-16', 'active'),
('PRO-0007', 'Christmas Countdown',          'Percentage (30%)',         '2025-12-01', '2025-12-25', 'active'),
('PRO-0008', 'New Year New Savings',         'Percentage (10%)',         '2026-01-01', '2026-01-07', 'active'),
('PRO-0009', 'Valentine''s Day Special',     'Fixed (150 Off)',          '2026-02-10', '2026-02-14', 'active'),
('PRO-0010', 'Anniversary Grand Sale',       'BOGO + Free Shipping',     '2026-03-01', '2026-03-31', 'active'),
('PRO-0011', 'Spring Refresh Promo',         'Percentage (12%)',         '2026-04-01', '2026-04-15', 'active'),
('PRO-0012', 'Labor Day Weekend Deal',       'Fixed (300 Off)',          '2026-05-01', '2026-05-04', 'active'),
('PRO-0013', 'Mothers Day Bundle',           'BOGO',                     '2026-05-08', '2026-05-11', 'active'),
('PRO-0014', 'Mid-Season Flash Sale',        'Percentage (18%)',         '2026-05-20', '2026-05-22', 'active'),
('PRO-0015', 'Fathers Day Treat',            'Fixed (200 Off)',          '2026-06-12', '2026-06-16', 'active'),
('PRO-0016', 'Independence Day Blowout',     'Percentage (22%)',         '2026-06-10', '2026-06-12', 'active'),
('PRO-0017', 'Back-to-Work Promo',           'Fixed (100 Off)',          '2026-01-08', '2026-01-15', 'archived'),
('PRO-0018', 'Rainy Season Deals',           'Percentage (8%)',          '2025-07-16', '2025-07-31', 'archived'),
('PRO-0019', 'Clearance Weekend',            'Percentage (35%)',         '2025-09-27', '2025-09-28', 'archived'),
('PRO-0020', 'All Saints Flash Sale',        'Fixed (250 Off)',          '2025-11-01', '2025-11-02', 'archived'),
('PRO-0021', 'Year-End Blowout',             'Percentage (40%)',         '2025-12-26', '2025-12-31', 'archived'),
('PRO-0022', 'Chinese New Year Special',     'BOGO',                     '2026-01-28', '2026-02-01', 'archived'),
('PRO-0023', 'Super Brand Day',              'Percentage (50%)',         '2026-02-28', '2026-02-28', 'archived'),
('PRO-0024', 'Holy Week Getaway Sale',       'Fixed (400 Off)',          '2026-04-01', '2026-04-05', 'archived'),
('PRO-0025', 'Earth Day Eco Promo',          'Percentage (5%)',          '2026-04-22', '2026-04-22', 'archived');
GO


-- ============================================================
-- READ
-- ============================================================

-- [1] All active promotions, newest first
SELECT promo_id,
       promo_name,
       discount_type,
       CAST(start_date AS VARCHAR(10)) AS start_date,
       CAST(end_date   AS VARCHAR(10)) AS end_date
FROM   promotions
WHERE  status = 'active'
ORDER  BY created_at DESC;

-- [2] All archived promotions, newest first
SELECT promo_id,
       promo_name,
       discount_type,
       CAST(start_date AS VARCHAR(10)) AS start_date,
       CAST(end_date   AS VARCHAR(10)) AS end_date
FROM   promotions
WHERE  status = 'archived'
ORDER  BY created_at DESC;

-- [3] Keyword search across promo_id, promo_name, and discount_type within a given tab
--     Bind: status, '%keyword%' x3
SELECT promo_id,
       promo_name,
       discount_type,
       CAST(start_date AS VARCHAR(10)) AS start_date,
       CAST(end_date   AS VARCHAR(10)) AS end_date
FROM   promotions
WHERE  status = ?
  AND  (LOWER(promo_id)      LIKE LOWER(?)
    OR  LOWER(promo_name)    LIKE LOWER(?)
    OR  LOWER(discount_type) LIKE LOWER(?))
ORDER  BY created_at DESC;

-- [4] Derive the next sequential promo_id  (T-SQL — no LPAD; uses RIGHT + REPLICATE)
--     Used in promotions_util.generateNextPromoId()
SELECT COALESCE(
           'PRO-' + RIGHT(
               REPLICATE('0', 4) + CAST(MAX(CAST(SUBSTRING(promo_id, 5, 4) AS INT)) + 1 AS VARCHAR(4)),
               4),
           'PRO-0001') AS next_id
FROM   promotions;


-- ============================================================
-- CREATE
-- ============================================================

-- [5] Insert a new promotion (status defaults to 'active')
--     Bind: promo_id, promo_name, discount_type, start_date, end_date
INSERT INTO promotions (promo_id, promo_name, discount_type, start_date, end_date, status)
VALUES (?, ?, ?, ?, ?, 'active');


-- ============================================================
-- UPDATE
-- ============================================================

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

-- [10] Archive a single promotion by ID
--      Bind: promo_id
UPDATE promotions
SET    status = 'archived'
WHERE  promo_id = ?;

-- [11] Restore a single promotion to active
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


-- ============================================================
-- DELETE
-- ============================================================

-- [14] Hard-delete a single promotion by ID
--      Bind: promo_id
DELETE FROM promotions
WHERE  promo_id = ?;

-- [15] Hard-delete all promotions in the given tab
--      Bind: status ('active' | 'archived')
DELETE FROM promotions
WHERE  status = ?;


-- ============================================================
-- EXPORT
-- ============================================================

-- [16] Full row dump for CSV export of the current tab
--      Bind: status
SELECT promo_id,
       promo_name,
       discount_type,
       CAST(start_date AS VARCHAR(10)) AS start_date,
       CAST(end_date   AS VARCHAR(10)) AS end_date,
       status
FROM   promotions
WHERE  status = ?
ORDER  BY created_at DESC;