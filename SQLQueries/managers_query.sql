-- ══════════════════════════════════════════════════════
--  Managers Table
--  Holds all authorized store manager accounts.
--  Passwords are stored as SHA-256 hashes (hex-encoded, uppercase).
--  This table is backend-only and is NOT exposed in any UI.
-- ══════════════════════════════════════════════════════

-- ══════════════════════════════════════════════════════
--  STEP 1 — Fix any rows that have the old incorrect hash.
--  SHA-256("Admin@12345") correct = 6F2CB9DD8F4B65E24E1C3F3FA5BC57982349237F11ABCEACD45BBCB74D621C25
--  The previously seeded hash (665A6DA9...) was WRONG — update it.
-- ══════════════════════════════════════════════════════

UPDATE dbo.Managers
SET    password = '6F2CB9DD8F4B65E24E1C3F3FA5BC57982349237F11ABCEACD45BBCB74D621C25'
WHERE  password = '665A6DA9F18B4CFBBA29F1040AC62BBBC63F6FD52C9B3AE55E05DC0E28CDBA17';

-- ══════════════════════════════════════════════════════
--  STEP 2 — Insert only managers that don't exist yet.
--  Safe to re-run; will not duplicate or error on existing rows.
-- ══════════════════════════════════════════════════════

INSERT INTO dbo.Managers (manager_id, manager_name, password)
SELECT 'MGR-001', 'Yhvhan Suba', '6F2CB9DD8F4B65E24E1C3F3FA5BC57982349237F11ABCEACD45BBCB74D621C25'
WHERE NOT EXISTS (SELECT 1 FROM dbo.Managers WHERE manager_id = 'MGR-001');

INSERT INTO dbo.Managers (manager_id, manager_name, password)
SELECT 'MGR-002', 'Louis Alzona', '6F2CB9DD8F4B65E24E1C3F3FA5BC57982349237F11ABCEACD45BBCB74D621C25'
WHERE NOT EXISTS (SELECT 1 FROM dbo.Managers WHERE manager_id = 'MGR-002');

INSERT INTO dbo.Managers (manager_id, manager_name, password)
SELECT 'MGR-003', 'Armando Tampos', '6F2CB9DD8F4B65E24E1C3F3FA5BC57982349237F11ABCEACD45BBCB74D621C25'
WHERE NOT EXISTS (SELECT 1 FROM dbo.Managers WHERE manager_id = 'MGR-003');