-- ══════════════════════════════════════════════════════════════
--  employees_query.sql
--  Creates the Employees table and seeds it with 4 sample rows.
--  Run this BEFORE timelogs_query.sql.
-- ══════════════════════════════════════════════════════════════

-- ── Table definition ─────────────────────────────────────────
IF NOT EXISTS (
    SELECT 1 FROM sys.tables WHERE name = 'Employees' AND type = 'U'
)
BEGIN
    CREATE TABLE dbo.Employees (
        employee_id        VARCHAR(10)  NOT NULL PRIMARY KEY,
        employee_name      VARCHAR(100) NOT NULL,
        age                INT              NULL,                    -- optional age field
        role               VARCHAR(50)  NOT NULL,
        employment_status  VARCHAR(20)  NOT NULL DEFAULT 'Active',  -- Active | Inactive | On Leave | Terminated
        status             VARCHAR(10)  NOT NULL DEFAULT 'active',  -- active | archived  (for soft archive)
        is_deleted         BIT          NOT NULL DEFAULT 0,
        created_at         DATETIME     NOT NULL DEFAULT GETDATE()
    );
END;

-- ── Add age column if table already exists without it ────────
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.Employees') AND name = 'age'
)
BEGIN
    ALTER TABLE dbo.Employees ADD age INT NULL;
END;

-- ── Seed data — 4 employees (no Manager role) ────────────────
-- Wrapped in dynamic SQL so column resolution happens at runtime,
-- AFTER the ALTER TABLE above has already added the 'age' column.
EXEC sp_executesql N'
MERGE dbo.Employees AS target
USING (VALUES
    (''EMP-001'', ''Maria Santos'',   28, ''Barista'',  ''Active'',   ''active'', 0),
    (''EMP-002'', ''Juan Reyes'',     24, ''Barista'',  ''Active'',   ''active'', 0),
    (''EMP-003'', ''Ana Villanueva'', 22, ''Cashier'',  ''On Leave'', ''active'', 0),
    (''EMP-004'', ''Carlo Mendoza'',  26, ''Barista'',  ''Active'',   ''active'', 0)
) AS source (employee_id, employee_name, age, role, employment_status, status, is_deleted)
ON target.employee_id = source.employee_id
WHEN NOT MATCHED THEN
    INSERT (employee_id, employee_name, age, role, employment_status, status, is_deleted)
    VALUES (source.employee_id, source.employee_name, source.age, source.role,
            source.employment_status, source.status, source.is_deleted);
';