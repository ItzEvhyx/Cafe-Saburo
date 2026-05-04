SELECT DISTINCT sizes, COUNT(*) AS row_count FROM dbo.menu_items GROUP BY sizes;
