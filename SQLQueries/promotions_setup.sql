-- ============================================================
--  promotions_setup.sql
--  Creates the promotions table and seeds 10 sample records.
-- ============================================================

CREATE TABLE IF NOT EXISTS promotions (
    promo_id      VARCHAR(12)  PRIMARY KEY,
    promo_name    VARCHAR(120) NOT NULL,
    discount_type VARCHAR(80)  NOT NULL,
    start_date    DATE         NOT NULL,
    end_date      DATE         NOT NULL,
    status        VARCHAR(10)  NOT NULL DEFAULT 'active'   -- 'active' | 'archived'
        CHECK (status IN ('active', 'archived')),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── Indexes ────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_promotions_status     ON promotions (status);
CREATE INDEX IF NOT EXISTS idx_promotions_start_date ON promotions (start_date);
CREATE INDEX IF NOT EXISTS idx_promotions_end_date   ON promotions (end_date);

-- ── Sample data (10 rows) ──────────────────────────────────
INSERT INTO promotions (promo_id, promo_name, discount_type, start_date, end_date, status)
VALUES
    ('PRO-0001', 'Summer Splash Sale',          'Percentage (15%)',   '2025-06-01', '2025-06-30', 'active'),
    ('PRO-0002', 'Mid-Year Mega Deals',         'Percentage (20%)',   '2025-07-01', '2025-07-15', 'active'),
    ('PRO-0003', 'Back to School Bonanza',      'Fixed (₱200 Off)',   '2025-08-01', '2025-08-31', 'active'),
    ('PRO-0004', 'Ber Month Kickoff',            'BOGO',               '2025-09-01', '2025-09-10', 'active'),
    ('PRO-0005', 'Holiday Early Bird',          'Percentage (25%)',   '2025-10-15', '2025-11-01', 'active'),
    ('PRO-0006', 'November Payday Treat',       'Fixed (₱500 Off)',   '2025-11-15', '2025-11-16', 'active'),
    ('PRO-0007', 'Christmas Countdown',         'Percentage (30%)',   '2025-12-01', '2025-12-25', 'active'),
    ('PRO-0008', 'New Year New Savings',        'Percentage (10%)',   '2026-01-01', '2026-01-07', 'active'),
    ('PRO-0009', 'Valentine\'s Day Special',    'Fixed (₱150 Off)',   '2026-02-10', '2026-02-14', 'active'),
    ('PRO-0010', 'Anniversary Grand Sale',      'BOGO + Free Shipping','2026-03-01', '2026-03-31', 'active')
ON CONFLICT (promo_id) DO NOTHING;