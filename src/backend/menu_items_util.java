package backend;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class menu_items_util {

    // ══════════════════════════════════════════════════════
    //  DIAGNOSTIC  — call this once on startup to confirm
    //  the DB is reachable and the table has rows.
    //  Output goes to System.out so you see it in your IDE
    //  console the moment the app launches.
    // ══════════════════════════════════════════════════════
    public static void runStartupDiagnostic(Connection conn) {
        if (conn == null) {
            System.err.println("[DIAGNOSTIC] conn is NULL — check your DB connection code.");
            return;
        }
        try {
            System.out.println("[DIAGNOSTIC] Connection valid: " + !conn.isClosed());
            System.out.println("[DIAGNOSTIC] Catalog : " + conn.getCatalog());
            System.out.println("[DIAGNOSTIC] Schema  : " + conn.getSchema());
        } catch (SQLException e) {
            System.err.println("[DIAGNOSTIC] Could not read catalog/schema: " + e.getMessage());
        }

        // Count total rows so we know the table exists and has data
        String countSql = "SELECT COUNT(*) AS total, " +
                          "SUM(CASE WHEN is_archived = 0 THEN 1 ELSE 0 END) AS active, " +
                          "SUM(CASE WHEN is_archived = 1 THEN 1 ELSE 0 END) AS archived " +
                          "FROM dbo.menu_items";
        try (PreparedStatement ps = conn.prepareStatement(countSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int total    = rs.getInt("total");
                int active   = rs.getInt("active");
                int archived = rs.getInt("archived");
                System.out.println("[DIAGNOSTIC] dbo.menu_items row count → total=" + total
                        + "  active=" + active + "  archived=" + archived);
                if (total == 0) {
                    System.err.println("[DIAGNOSTIC] *** TABLE IS EMPTY ***  " +
                            "Run menu_items_setup.sql in SSMS to seed data.");
                }
            }
        } catch (SQLException e) {
            System.err.println("[DIAGNOSTIC] Could not query dbo.menu_items: " + e.getMessage());
            System.err.println("[DIAGNOSTIC] Check that the table exists in the correct DB/schema.");
            e.printStackTrace();
        }

        // Show every distinct sizes value — catches spacing/casing differences
        String sizesSql = "SELECT DISTINCT sizes, COUNT(*) AS row_count " +
                          "FROM dbo.menu_items GROUP BY sizes ORDER BY sizes";
        try (PreparedStatement ps = conn.prepareStatement(sizesSql);
             ResultSet rs = ps.executeQuery()) {
            System.out.println("[DIAGNOSTIC] Distinct sizes values in DB:");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.println("  '" + rs.getString("sizes") + "' → " + rs.getInt("row_count") + " rows");
            }
            if (!any) System.out.println("  (none — table is empty)");
        } catch (SQLException e) {
            System.err.println("[DIAGNOSTIC] sizes query failed: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    //  FETCH  (populates cachedRows in menu_items_contents)
    //  Returns: [item_id, item_name, sizes, display_price]
    //  display_price: "100 / 105"  (Small/Large)
    //              or "140"        (One Size)
    // ══════════════════════════════════════════════════════
    public static List<String[]> fetchMenuItems(Connection conn, String tab) {
        List<String[]> rows = new ArrayList<>();
        if (conn == null) {
            System.err.println("[menu_items_util] fetchMenuItems — conn is null");
            return rows;
        }

        int isArchived = tab.equals("archived") ? 1 : 0;

        String sql =
            "SELECT item_id, item_name, sizes, " +
            "CASE " +
            "  WHEN LOWER(LTRIM(RTRIM(sizes))) = 'small, large' " +
            "    THEN CAST(CAST(ISNULL(price_small, 0) AS INT) AS VARCHAR)" +
            "       + ' / ' " +
            "       + CAST(CAST(ISNULL(price_large, 0) AS INT) AS VARCHAR) " +
            "  ELSE CAST(CAST(ISNULL(price, 0) AS INT) AS VARCHAR) " +
            "END AS display_price " +
            "FROM dbo.menu_items " +
            "WHERE is_archived = ? " +
            "ORDER BY " +
            "  CASE category " +
            "    WHEN 'Espresso'         THEN 1 " +
            "    WHEN 'Specialty Coffee' THEN 2 " +
            "    WHEN 'Iced Coffee'      THEN 3 " +
            "    WHEN 'Frappe'           THEN 4 " +
            "    WHEN 'Matcha Series'    THEN 5 " +
            "    WHEN 'Smoothie'         THEN 6 " +
            "    WHEN 'Refresher'        THEN 7 " +
            "    WHEN 'Add On'           THEN 8 " +
            "    ELSE                         9 " +
            "  END, item_name ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, isArchived);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String displayPrice = rs.getString("display_price");
                    rows.add(new String[]{
                        rs.getString("item_id"),
                        rs.getString("item_name"),
                        rs.getString("sizes"),
                        displayPrice != null ? displayPrice : "0"
                    });
                }
            }
            System.out.println("[menu_items_util] fetchMenuItems(tab=" + tab + ") → " + rows.size() + " rows");
        } catch (SQLException e) {
            System.err.println("[menu_items_util] fetchMenuItems error: " + e.getMessage());
            e.printStackTrace();
        }
        return rows;
    }

    // ══════════════════════════════════════════════════════
    //  SEARCH  (client-side filter)
    // ══════════════════════════════════════════════════════
    public static List<String[]> getFilteredRows(List<String[]> rows, String query) {
        if (query == null || query.isBlank()) return rows;
        String q = query.toLowerCase();
        List<String[]> result = new ArrayList<>();
        for (String[] r : rows) {
            for (String cell : r) {
                if (cell != null && cell.toLowerCase().contains(q)) {
                    result.add(r);
                    break;
                }
            }
        }
        return result;
    }

    // ══════════════════════════════════════════════════════
    //  INSERT
    // ══════════════════════════════════════════════════════
    public static String insertMenuItem(Connection conn,
                                        String itemName,
                                        String category,
                                        String sizes,
                                        Double priceSmall,
                                        Double priceLarge,
                                        Double price) {
        if (conn == null) return null;

        String nextId = getNextItemId(conn);
        if (nextId == null) return null;

        String sql =
            "INSERT INTO dbo.menu_items " +
            "(item_id, item_name, category, sizes, price_small, price_large, price, is_add_on, is_signature) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nextId);
            ps.setString(2, itemName);
            ps.setString(3, category);
            ps.setString(4, sizes);
            if (priceSmall != null) ps.setDouble(5, priceSmall); else ps.setNull(5, java.sql.Types.DECIMAL);
            if (priceLarge != null) ps.setDouble(6, priceLarge); else ps.setNull(6, java.sql.Types.DECIMAL);
            if (price     != null) ps.setDouble(7, price);      else ps.setNull(7, java.sql.Types.DECIMAL);
            ps.executeUpdate();
            return nextId;
        } catch (SQLException e) {
            System.err.println("[menu_items_util] insertMenuItem error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ══════════════════════════════════════════════════════
    //  UPDATE  (full update — name + sizes + prices)
    // ══════════════════════════════════════════════════════
    public static boolean updateMenuItem(Connection conn,
                                          String itemId,
                                          String itemName,
                                          String sizes,
                                          Double priceSmall,
                                          Double priceLarge,
                                          Double price) {
        if (conn == null) return false;

        String sql =
            "UPDATE dbo.menu_items " +
            "SET item_name = ?, sizes = ?, price_small = ?, price_large = ?, price = ? " +
            "WHERE item_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, itemName);
            ps.setString(2, sizes);
            if (priceSmall != null) ps.setDouble(3, priceSmall); else ps.setNull(3, java.sql.Types.DECIMAL);
            if (priceLarge != null) ps.setDouble(4, priceLarge); else ps.setNull(4, java.sql.Types.DECIMAL);
            if (price      != null) ps.setDouble(5, price);      else ps.setNull(5, java.sql.Types.DECIMAL);
            ps.setString(6, itemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[menu_items_util] updateMenuItem error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ── Convenience overload used by inline editable cells ──
    public static boolean updateMenuItemFromRow(Connection conn, String[] row) {
        if (conn == null || row == null || row.length < 4) return false;
        String itemId   = row[0];
        String itemName = row[1];
        String sizes    = row[2];
        String disprice = row[3] != null ? row[3].replace("₱", "").trim() : "";

        if (disprice.isEmpty()) return false;

        Double priceSmall = null, priceLarge = null, price = null;
        try {
            String sizesNorm = sizes != null ? sizes.toLowerCase().trim() : "";
            if (sizesNorm.equals("small, large") && disprice.contains("/")) {
                String[] parts = disprice.split("/");
                priceSmall = Double.parseDouble(parts[0].trim());
                priceLarge = Double.parseDouble(parts[1].trim());
            } else {
                price = Double.parseDouble(disprice.trim());
            }
        } catch (NumberFormatException e) {
            System.err.println("[menu_items_util] updateMenuItemFromRow parse error for value '"
                + disprice + "': " + e.getMessage());
            return false;
        }
        return updateMenuItem(conn, itemId, itemName, sizes, priceSmall, priceLarge, price);
    }

    // ══════════════════════════════════════════════════════
    //  ARCHIVE / RESTORE SELECTED
    // ══════════════════════════════════════════════════════
    public static void archiveSelected(Connection conn, Set<String> ids) {
        if (conn == null || ids == null || ids.isEmpty()) return;
        String sql = "UPDATE dbo.menu_items SET is_archived = 1 WHERE item_id IN ("
                   + buildPlaceholders(ids.size()) + ") AND is_archived = 0";
        runBulkIdUpdate(conn, sql, ids, "[menu_items_util] archiveSelected error");
    }

    public static void restoreSelected(Connection conn, Set<String> ids) {
        if (conn == null || ids == null || ids.isEmpty()) return;
        String sql = "UPDATE dbo.menu_items SET is_archived = 0 WHERE item_id IN ("
                   + buildPlaceholders(ids.size()) + ") AND is_archived = 1";
        runBulkIdUpdate(conn, sql, ids, "[menu_items_util] restoreSelected error");
    }

    // ══════════════════════════════════════════════════════
    //  ARCHIVE ALL / RESTORE ALL
    // ══════════════════════════════════════════════════════
    public static void archiveAll(Connection conn) {
        if (conn == null) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE dbo.menu_items SET is_archived = 1 WHERE is_archived = 0")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[menu_items_util] archiveAll error: " + e.getMessage());
        }
    }

    public static void restoreAll(Connection conn) {
        if (conn == null) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE dbo.menu_items SET is_archived = 0 WHERE is_archived = 1")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[menu_items_util] restoreAll error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    //  HARD DELETE ALL  (by tab)
    // ══════════════════════════════════════════════════════
    public static void hardDeleteAll(Connection conn, String tab) {
        if (conn == null) return;
        int isArchived = tab.equals("archived") ? 1 : 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM dbo.menu_items WHERE is_archived = ?")) {
            ps.setInt(1, isArchived);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[menu_items_util] hardDeleteAll error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    //  EXPORT CSV
    // ══════════════════════════════════════════════════════
    public static void exportCsv(List<String[]> cachedRows, String tab, Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Menu Items CSV");
        fc.setInitialFileName("menu_items_" + tab + ".csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        java.io.File file = fc.showSaveDialog(stage);
        if (file == null) return;

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("Item ID,Item Name,Sizes,Price");
            for (String[] row : cachedRows) {
                pw.println(
                    escapeCsv(row[0]) + "," +
                    escapeCsv(row[1]) + "," +
                    escapeCsv(row[2]) + "," +
                    escapeCsv(row[3])
                );
            }
        } catch (IOException e) {
            System.err.println("[menu_items_util] exportCsv error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════
    private static String getNextItemId(Connection conn) {
        String sql =
            "SELECT 'MI' + RIGHT('000' + CAST(" +
            "  COALESCE(MAX(CAST(SUBSTRING(item_id, 3, LEN(item_id)) AS INT)), 0) + 1" +
            "  AS VARCHAR), 3) AS next_item_id " +
            "FROM dbo.menu_items";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString("next_item_id");
        } catch (SQLException e) {
            System.err.println("[menu_items_util] getNextItemId error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private static String buildPlaceholders(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        return sb.toString();
    }

    private static void runBulkIdUpdate(Connection conn, String sql, Set<String> ids, String errLabel) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            for (String id : ids) ps.setString(i++, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println(errLabel + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}