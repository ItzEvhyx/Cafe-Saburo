-- menu_items_setup.sql — Cafe Saburo POS: Menu Items table definition and seed data
-- Dialect: T-SQL (SQL Server) | Run this BEFORE menu_items_query.sql


-- Drops the table if it exists to allow a clean re-run
IF OBJECT_ID('dbo.menu_items', 'U') IS NOT NULL
    DROP TABLE dbo.menu_items;

-- menu_items: stores all drink menu entries with size-based or flat pricing, archive state, and signature flags
CREATE TABLE dbo.menu_items (
    item_id       VARCHAR(10)     NOT NULL PRIMARY KEY,
    item_name     VARCHAR(100)    NOT NULL,
    category      VARCHAR(50)     NOT NULL,
    sizes         VARCHAR(50)     NOT NULL,
    price_small   DECIMAL(10, 2)  NULL,
    price_large   DECIMAL(10, 2)  NULL,
    price         DECIMAL(10, 2)  NULL,
    is_add_on     BIT             NOT NULL DEFAULT 0,
    is_archived   BIT             NOT NULL DEFAULT 0,
    is_signature  BIT             NOT NULL DEFAULT 0,
    created_at    DATETIME        NOT NULL DEFAULT GETDATE()
);

-- Indexes to speed up filtering by category, add-on flag, and archive state
CREATE INDEX idx_menu_items_category    ON dbo.menu_items (category);
CREATE INDEX idx_menu_items_is_add_on   ON dbo.menu_items (is_add_on);
CREATE INDEX idx_menu_items_is_archived ON dbo.menu_items (is_archived);


-- Espresso: Small = Hot price, Large = Cold price
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI001', 'Americano',  'Espresso', 'Small, Large', 100.00, 105.00, NULL, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI002', 'Cafe Latte', 'Espresso', 'Small, Large', 120.00, 125.00, NULL, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI003', 'Cafe Mocha', 'Espresso', 'One Size',     NULL,   NULL,   140.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI004', 'Cappuccino', 'Espresso', 'Small, Large', 120.00, 125.00, NULL, 0, 0);

-- Specialty Coffee: One Size
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI005', 'Banoffee',                'Specialty Coffee', 'One Size', NULL, NULL, 190.00, 0, 1);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI006', 'Biscoff Cream Latte',     'Specialty Coffee', 'One Size', NULL, NULL, 195.00, 0, 1);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI007', 'Biscoff Cold Foam Latte', 'Specialty Coffee', 'One Size', NULL, NULL, 195.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI008', 'Creme Brulee Latte',      'Specialty Coffee', 'One Size', NULL, NULL, 170.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI009', 'Lavender Latte',          'Specialty Coffee', 'One Size', NULL, NULL, 170.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI010', 'Pistachio Latte',         'Specialty Coffee', 'One Size', NULL, NULL, 190.00, 0, 1);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI011', 'Pumpkin Spice Latte',     'Specialty Coffee', 'One Size', NULL, NULL, 170.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI012', 'Sakura Cloud Latte',      'Specialty Coffee', 'One Size', NULL, NULL, 170.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI013', 'Smores Latte',            'Specialty Coffee', 'One Size', NULL, NULL, 170.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI014', 'Tiramisu Latte',          'Specialty Coffee', 'One Size', NULL, NULL, 170.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI015', 'Signature Drink',         'Specialty Coffee', 'One Size', NULL, NULL, 200.00, 0, 1);

-- Iced Coffee: One Size
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI016', 'Butterscotch Latte',  'Iced Coffee', 'One Size', NULL, NULL, 150.00, 0, 1);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI017', 'Caramel Latte',       'Iced Coffee', 'One Size', NULL, NULL, 150.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI018', 'Caramel Macchiato',   'Iced Coffee', 'One Size', NULL, NULL, 150.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI019', 'Hazelnut Latte',      'Iced Coffee', 'One Size', NULL, NULL, 150.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI020', 'Irish Cream Latte',   'Iced Coffee', 'One Size', NULL, NULL, 150.00, 0, 1);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI021', 'Mocha Latte',         'Iced Coffee', 'One Size', NULL, NULL, 150.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI022', 'Spanish Latte',       'Iced Coffee', 'One Size', NULL, NULL, 150.00, 0, 1);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI023', 'Vanilla Latte',       'Iced Coffee', 'One Size', NULL, NULL, 150.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI024', 'White Mocha Latte',   'Iced Coffee', 'One Size', NULL, NULL, 150.00, 0, 0);

-- Frappe: One Size
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI025', 'Coffee Caramel', 'Frappe', 'One Size', NULL, NULL, 160.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI026', 'Dark Mocha',     'Frappe', 'One Size', NULL, NULL, 160.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI027', 'Pecan Praline',  'Frappe', 'One Size', NULL, NULL, 160.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI028', 'White Mocha',    'Frappe', 'One Size', NULL, NULL, 160.00, 0, 0);

-- Matcha Series: One Size
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI029', 'Agave Matcha Latte',     'Matcha Series', 'One Size', NULL, NULL, 180.00, 0, 1);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI030', 'Banana Matcha',          'Matcha Series', 'One Size', NULL, NULL, 190.00, 0, 1);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI031', 'Blueberry Matcha',       'Matcha Series', 'One Size', NULL, NULL, 190.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI032', 'Dirty Matcha',           'Matcha Series', 'One Size', NULL, NULL, 190.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI033', 'Ichigo Matcha Latte',    'Matcha Series', 'One Size', NULL, NULL, 170.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI034', 'Lavender Matcha Latte',  'Matcha Series', 'One Size', NULL, NULL, 170.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI035', 'Mango Matcha',           'Matcha Series', 'One Size', NULL, NULL, 190.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI036', 'Matcha Latte',           'Matcha Series', 'One Size', NULL, NULL, 170.00, 0, 1);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI037', 'Matcha Pistachio Latte', 'Matcha Series', 'One Size', NULL, NULL, 200.00, 0, 1);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI038', 'Strawberry Matcha Latte','Matcha Series', 'One Size', NULL, NULL, 190.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI039', 'Oreo Matcha',            'Matcha Series', 'One Size', NULL, NULL, 190.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI040', 'Premium Hojicha',        'Matcha Series', 'One Size', NULL, NULL, 180.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI041', 'Kinako Hojicha',         'Matcha Series', 'One Size', NULL, NULL, 200.00, 0, 1);

-- Smoothie: One Size
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI042', 'Biscoff',               'Smoothie', 'One Size', NULL, NULL, 190.00, 0, 1);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI043', 'Blueberry Cheesecake',  'Smoothie', 'One Size', NULL, NULL, 170.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI044', 'Matcha',                'Smoothie', 'One Size', NULL, NULL, 180.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI045', 'Oreo Frappuccino',      'Smoothie', 'One Size', NULL, NULL, 170.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI046', 'Strawberry',            'Smoothie', 'One Size', NULL, NULL, 160.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI047', 'Strawberry Cheesecake', 'Smoothie', 'One Size', NULL, NULL, 170.00, 0, 0);

-- Refresher: One Size
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI048', 'Four Red Fruits Tea',   'Refresher', 'One Size', NULL, NULL, 125.00, 0, 1);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI049', 'Kiwi Green Apple Tea',  'Refresher', 'One Size', NULL, NULL, 125.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI050', 'Passion Fruit Tea',     'Refresher', 'One Size', NULL, NULL, 125.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI051', 'Pomegranate Lemon Tea', 'Refresher', 'One Size', NULL, NULL, 125.00, 0, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI052', 'Wild Berry Tea',        'Refresher', 'One Size', NULL, NULL, 125.00, 0, 0);

-- Add Ons
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI053', 'Espresso Shot', 'Add On', 'One Size', NULL, NULL, 30.00, 1, 0);
INSERT INTO dbo.menu_items (item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) VALUES
('MI054', 'Sub Oat',       'Add On', 'One Size', NULL, NULL, 30.00, 1, 0);