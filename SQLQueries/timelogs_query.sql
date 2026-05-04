-- timelogs_query.sql  |  Cafe Saburo POS — TimeLogs Table Setup + Seed  |  T-SQL (SQL Server)
-- Creates the TimeLogs table and seeds it with one log per employee.
-- Run AFTER employees_query.sql.

-- ── CREATE TABLE: TimeLogs with soft delete and clock-in/out tracking ────────────────────────────
-- time_out = NULL means the employee is currently clocked in.
IF NOT EXISTS (
    SELECT 1 FROM sys.tables WHERE name = 'TimeLogs' AND type = 'U'
)
BEGIN
    CREATE TABLE dbo.TimeLogs (
        log_id        VARCHAR(15)  NOT NULL PRIMARY KEY,
        employee_id   VARCHAR(10)  NOT NULL,
        employee_name VARCHAR(100) NOT NULL,   -- denormalized for fast display without a join
        time_in       DATETIME     NOT NULL,
        time_out      DATETIME         NULL,   -- NULL = currently clocked in
        status        VARCHAR(10)  NOT NULL DEFAULT 'active',  -- active | archived
        is_deleted    BIT          NOT NULL DEFAULT 0
    );
END;

-- ── FK: Add foreign key to Employees outside the IF block so it always resolves on re-runs ──────
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE name = 'FK_TimeLogs_Employees'
      AND parent_object_id = OBJECT_ID('dbo.TimeLogs')
)
BEGIN
    ALTER TABLE dbo.TimeLogs
        ADD CONSTRAINT FK_TimeLogs_Employees
            FOREIGN KEY (employee_id) REFERENCES dbo.Employees (employee_id);
END;

-- ── SEED: One completed log per employee; Carlo Mendoza left clocked in (time_out = NULL) ────────
MERGE dbo.TimeLogs AS target
USING (VALUES
    ('LOG-0001', 'EMP-001', 'Maria Santos',   '2025-04-18 08:00:00', '2025-04-18 17:00:00', 'active', 0),
    ('LOG-0002', 'EMP-002', 'Juan Reyes',     '2025-04-18 09:00:00', '2025-04-18 18:00:00', 'active', 0),
    ('LOG-0003', 'EMP-003', 'Ana Villanueva', '2025-04-18 07:30:00', '2025-04-18 16:30:00', 'active', 0),
    ('LOG-0004', 'EMP-004', 'Carlo Mendoza',  '2025-04-18 10:00:00',                   NULL, 'active', 0)
) AS source (log_id, employee_id, employee_name, time_in, time_out, status, is_deleted)
ON target.log_id = source.log_id
WHEN NOT MATCHED THEN
    INSERT (log_id, employee_id, employee_name, time_in, time_out, status, is_deleted)
    VALUES (source.log_id, source.employee_id, source.employee_name,
            source.time_in, source.time_out, source.status, source.is_deleted);