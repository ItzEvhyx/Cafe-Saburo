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
    //  DIAGNOSTIC
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

        String countSql = "SELECT COUNT(*) AS total, " +
                          "SUM(CASE WHEN is_archived = 0 THEN 1 ELSE 0 END) AS active, " +
                          "SUM(CASE WHEN is_archived = 1 THEN 1 ELSE 0 END) AS archived " +
                          "FROM dbo.menu_items";
        try (PreparedStatement ps = conn.prepareStatement(countSql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                System.out.println("[DIAGNOSTIC] dbo.menu_items row count → total=" + rs.getInt("total")
                        + "  active=" + rs.getInt("active") + "  archived=" + rs.getInt("archived"));
            }
        } catch (SQLException e) {
            System.err.println("[DIAGNOSTIC] Could not query dbo.menu_items: " + e.getMessage());
            e.printStackTrace();
        }

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
    //  FETCH — ONE ROW PER SIZE/PRICE (3NF compliant)
    //
    //  Returns String[5] per row:
    //    [0] item_id
    //    [1] item_name
    //    [2] size_label   — "Small", "Large", or "One Size"
    //    [3] price        — price for that specific size as a plain integer string
    //    [4] sizes_raw    — original sizes value from DB (used for edits/logic)
    //
    //  A "Small, Large" item produces TWO rows (Small row, then Large row).
    //  A "One Size" item produces ONE row.
    // ══════════════════════════════════════════════════════
    public static List<String[]> fetchMenuItems(Connection conn, String tab) {
        List<String[]> rows = new ArrayList<>();
        if (conn == null) {
            System.err.println("[menu_items_util] fetchMenuItems — conn is null");
            return rows;
        }

        int isArchived = tab.equals("archived") ? 1 : 0;

        String sql =
            "SELECT item_id, item_name, sizes, price_small, price_large, price " +
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
                    String itemId   = rs.getString("item_id");
                    String itemName = rs.getString("item_name");
                    String sizes    = rs.getString("sizes");
                    String sizesNorm = sizes != null ? sizes.toLowerCase().trim() : "";

                    if (sizesNorm.equals("small, large")) {
                        // Two atomic rows — one per size
                        int priceSmall = (int) Math.round(rs.getDouble("price_small"));
                        int priceLarge = (int) Math.round(rs.getDouble("price_large"));
                        rows.add(new String[]{ itemId, itemName, "Small", String.valueOf(priceSmall), sizes });
                        rows.add(new String[]{ itemId, itemName, "Large", String.valueOf(priceLarge), sizes });
                    } else {
                        // One atomic row
                        int price = (int) Math.round(rs.getDouble("price"));
                        String sizeLabel = (sizes != null && !sizes.isBlank()) ? sizes : "One Size";
                        rows.add(new String[]{ itemId, itemName, sizeLabel, String.valueOf(price), sizes });
                    }
                }
            }
            System.out.println("[menu_items_util] fetchMenuItems(tab=" + tab + ") → " + rows.size() + " size-rows");
        } catch (SQLException e) {
            System.err.println("[menu_items_util] fetchMenuItems error: " + e.getMessage());
            e.printStackTrace();
        }
        return rows;
    }

    // ══════════════════════════════════════════════════════
    //  SEARCH (client-side filter on 3NF rows)
    //  Matches on item_id, item_name, size_label, or price
    // ══════════════════════════════════════════════════════
    public static List<String[]> getFilteredRows(List<String[]> rows, String query) {
        if (query == null || query.isBlank()) return rows;
        String q = query.toLowerCase();
        List<String[]> result = new ArrayList<>();
        for (String[] r : rows) {
            // Check item_id [0], item_name [1], size_label [2], price [3]
            if ((r[0] != null && r[0].toLowerCase().contains(q)) ||
                (r[1] != null && r[1].toLowerCase().contains(q)) ||
                (r[2] != null && r[2].toLowerCase().contains(q)) ||
                (r[3] != null && r[3].toLowerCase().contains(q))) {
                result.add(r);
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
    //  UPDATE (full update — name + sizes + prices)
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

    // ══════════════════════════════════════════════════════
    //  UPDATE FROM 3NF ROW
    //  row: [0]=item_id, [1]=item_name, [2]=size_label, [3]=price, [4]=sizes_raw
    //
    //  Because each row is now atomic, we must reconstruct the full DB record.
    //  For Small/Large items we look up the sibling row to get both prices.
    // ══════════════════════════════════════════════════════
    public static boolean updateMenuItemFromRow(Connection conn, String[] row, List<String[]> allCachedRows) {
        if (conn == null || row == null || row.length < 5) return false;

        String itemId    = row[0];
        String itemName  = row[1];
        String sizeLabel = row[2];
        String priceStr  = row[3] != null ? row[3].replace("₱", "").trim() : "";
        String sizesRaw  = row[4];

        if (priceStr.isEmpty()) return false;

        Double priceSmall = null, priceLarge = null, price = null;
        try {
            String sizesNorm = sizesRaw != null ? sizesRaw.toLowerCase().trim() : "";
            if (sizesNorm.equals("small, large")) {
                // Find sibling rows for same item_id to get both prices
                double currentPrice = Double.parseDouble(priceStr);
                double siblingPrice = currentPrice; // fallback
                for (String[] r : allCachedRows) {
                    if (r[0].equals(itemId) && !r[2].equals(sizeLabel)) {
                        try { siblingPrice = Double.parseDouble(r[3]); } catch (NumberFormatException ignored) {}
                        break;
                    }
                }
                if (sizeLabel.equalsIgnoreCase("Small")) {
                    priceSmall = currentPrice;
                    priceLarge = siblingPrice;
                } else {
                    priceLarge = currentPrice;
                    priceSmall = siblingPrice;
                }
            } else {
                price = Double.parseDouble(priceStr);
            }
        } catch (NumberFormatException e) {
            System.err.println("[menu_items_util] updateMenuItemFromRow parse error: " + e.getMessage());
            return false;
        }

        return updateMenuItem(conn, itemId, itemName, sizesRaw, priceSmall, priceLarge, price);
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
    //  HARD DELETE ALL (by tab)
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
    //  Exports one row per DB record (not per size-row) for readability
    // ══════════════════════════════════════════════════════
    public static void exportCsv(List<String[]> cachedRows, String tab, Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Menu Items CSV");
        fc.setInitialFileName("menu_items_" + tab + ".csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        java.io.File file = fc.showSaveDialog(stage);
        if (file == null) return;

        // De-duplicate: only export first size-row per item_id
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("Item ID,Item Name,Size,Price");
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