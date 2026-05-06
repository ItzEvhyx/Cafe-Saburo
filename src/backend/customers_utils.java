package backend;

import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.layout.Pane;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class customers_utils {

    // ══════════════════════════════════════════════════════
    //  FONT LOADING
    // ══════════════════════════════════════════════════════
    private static boolean fontsLoaded = false;

    public static void loadFonts() {
        if (fontsLoaded) return;
        String[] variants = {
            "Aleo-Black","Aleo-BlackItalic","Aleo-Bold","Aleo-BoldItalic",
            "Aleo-ExtraBold","Aleo-ExtraBoldItalic","Aleo-ExtraLight","Aleo-ExtraLightItalic",
            "Aleo-Italic","Aleo-Light","Aleo-LightItalic","Aleo-Medium","Aleo-MediumItalic",
            "Aleo-Regular","Aleo-SemiBold","Aleo-SemiBoldItalic","Aleo-Thin","Aleo-ThinItalic"
        };
        for (String v : variants) Font.loadFont("file:assets/fonts/" + v + ".ttf", 12);
        fontsLoaded = true;
    }

    // ══════════════════════════════════════════════════════
    //  DB FETCH
    //  Returns rows: [customerId, customerName, latestOrderId, loyaltyPts]
    // ══════════════════════════════════════════════════════
    public static List<String[]> fetchCustomers(Connection conn, String tab) {
        List<String[]> rows = new ArrayList<>();
        if (conn == null) return rows;
        try { if (conn.isClosed()) return rows; } catch (Exception e) { return rows; }

        String sql =
            "SELECT " +
            "    c.customer_id, " +
            "    c.customer_name, " +
            "    COALESCE(o.latest_order_id, 'N/A') AS latest_order_id, " +
            "    COALESCE(o.order_count * 10, 0)    AS loyalty_pts " +
            "FROM dbo.Customers AS c " +
            "LEFT JOIN ( " +
            "    SELECT customer_id, COUNT(order_id) AS order_count, MAX(order_id) AS latest_order_id " +
            "    FROM dbo.Orders WHERE is_deleted = 0 GROUP BY customer_id " +
            ") AS o ON c.customer_id = o.customer_id " +
            "WHERE c.is_deleted = 0 AND c.status = ? " +
            "ORDER BY loyalty_pts DESC, c.customer_name ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tab);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("customer_id")     != null ? rs.getString("customer_id")     : "—",
                    rs.getString("customer_name")   != null ? rs.getString("customer_name")   : "—",
                    rs.getString("latest_order_id") != null ? rs.getString("latest_order_id") : "N/A",
                    String.valueOf(rs.getInt("loyalty_pts"))
                });
            }
            rs.close();
        } catch (Exception e) { e.printStackTrace(); }
        return rows;
    }

    // ══════════════════════════════════════════════════════
    //  ROW FILTERING
    //  Searches by customer name (index 1) or order ID (index 2)
    // ══════════════════════════════════════════════════════
    public static List<String[]> getFilteredRows(List<String[]> cachedRows, String searchQuery) {
        if (searchQuery == null || searchQuery.isBlank()) return cachedRows;
        String q = searchQuery.trim().toLowerCase();
        List<String[]> filtered = new ArrayList<>();
        for (String[] row : cachedRows) {
            if (row[1].toLowerCase().contains(q) || row[2].toLowerCase().contains(q))
                filtered.add(row);
        }
        return filtered;
    }

    // ══════════════════════════════════════════════════════
    //  LIVE-UPDATE: PREPEND OR UPDATE CUSTOMER
    //  Mutates cachedRows in-place; caller should rebuildTable() after.
    // ══════════════════════════════════════════════════════
    public static void prependOrUpdateCustomer(
            List<String[]> cachedRows,
            String customerId, String customerName, String orderId) {

        boolean found = false;
        for (String[] row : cachedRows) {
            if (row[0].equals(customerId)) {
                row[2] = orderId;
                int pts = 0;
                try { pts = Integer.parseInt(row[3]); } catch (NumberFormatException ignored) {}
                row[3] = String.valueOf(pts + 10);
                found = true;
                break;
            }
        }

        if (!found) {
            cachedRows.add(0, new String[]{ customerId, customerName, orderId, "10" });
        }

        sortByLoyaltyThenName(cachedRows);
    }

    // ══════════════════════════════════════════════════════
    //  SORT HELPER
    // ══════════════════════════════════════════════════════
    public static void sortByLoyaltyThenName(List<String[]> rows) {
        rows.sort((a, b) -> {
            int ptsA = 0, ptsB = 0;
            try { ptsA = Integer.parseInt(a[3]); } catch (NumberFormatException ignored) {}
            try { ptsB = Integer.parseInt(b[3]); } catch (NumberFormatException ignored) {}
            if (ptsB != ptsA) return Integer.compare(ptsB, ptsA);
            return a[1].compareToIgnoreCase(b[1]);
        });
    }

    // ══════════════════════════════════════════════════════
    //  DB OPERATIONS
    // ══════════════════════════════════════════════════════
    public static void archiveSelected(Connection conn, Set<String> ids) {
        if (conn == null || ids.isEmpty()) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Orders SET status = 'archived' WHERE customer_id = ? AND is_deleted = 0")) {
                ps.setString(1, id); ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Customers SET status = 'archived' WHERE customer_id = ?")) {
                ps.setString(1, id); ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public static void restoreSelected(Connection conn, Set<String> ids) {
        if (conn == null || ids.isEmpty()) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Orders SET status = 'active' WHERE customer_id = ? AND is_deleted = 0")) {
                ps.setString(1, id); ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Customers SET status = 'active' WHERE customer_id = ?")) {
                ps.setString(1, id); ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public static void hardDeleteAll(Connection conn, String currentTab) {
        if (conn == null) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM dbo.Orders WHERE customer_id IN " +
                "(SELECT customer_id FROM dbo.Customers WHERE status = ?)")) {
            ps.setString(1, currentTab); ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM dbo.Customers WHERE status = ?")) {
            ps.setString(1, currentTab); ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════
    //  CSV EXPORT
    // ══════════════════════════════════════════════════════
    public static void exportCsv(List<String[]> rows, String currentTab, Pane root) {
        if (rows.isEmpty()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Customer List as CSV");
        chooser.setInitialFileName("customers_" + currentTab + ".csv");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        Stage stage = null;
        try { stage = (Stage) root.getScene().getWindow(); } catch (Exception ignored) {}

        File file = (stage != null) ? chooser.showSaveDialog(stage) : chooser.showSaveDialog(null);
        if (file == null) return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("Customer ID,Customer Name,Order ID,Loyalty Pts");
            writer.newLine();
            for (String[] row : rows) {
                writer.write(
                    escapeCsv(row[0]) + "," +
                    escapeCsv(row[1]) + "," +
                    escapeCsv(row[2]) + "," +
                    escapeCsv(row[3])
                );
                writer.newLine();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n"))
            return "\"" + value.replace("\"", "\"\"") + "\"";
        return value;
    }

    // ══════════════════════════════════════════════════════
    //  LOYALTY PILL COLORS
    //  Returns [backgroundColor, foregroundColor]
    // ══════════════════════════════════════════════════════
    private static final String PILL_GOLD_BG   = "#FFF3CD";
    private static final String PILL_GOLD_FG   = "#856404";
    private static final String PILL_SILVER_BG = "#E2E3E5";
    private static final String PILL_SILVER_FG = "#383D41";
    private static final String PILL_BRONZE_BG = "#F5E6D3";
    private static final String PILL_BRONZE_FG = "#7D4E1B";
    private static final String PILL_NONE_BG   = "#F8F9FA";
    private static final String PILL_NONE_FG   = "#6C757D";

    public static String[] loyaltyPillColors(int pts) {
        if (pts >= 50) return new String[]{ PILL_GOLD_BG,   PILL_GOLD_FG   };
        if (pts >= 30) return new String[]{ PILL_SILVER_BG, PILL_SILVER_FG };
        if (pts >= 10) return new String[]{ PILL_BRONZE_BG, PILL_BRONZE_FG };
        return             new String[]{ PILL_NONE_BG,   PILL_NONE_FG   };
    }
}