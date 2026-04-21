package backend;

import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * inventory_util
 *
 * Holds all business logic, database operations, CSV export,
 * font loading, and data-filtering for the Inventory module.
 *
 * inventory_contents (UI) delegates every non-visual concern here.
 */
public class inventory_util {

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
    //  DB HELPERS
    // ══════════════════════════════════════════════════════

    /**
     * Returns true if the connection is usable, logging the reason if not.
     */
    private static boolean isConnUsable(Connection conn, String caller) {
        if (conn == null) {
            System.err.println("[inventory_util] " + caller + ": conn is null");
            return false;
        }
        try {
            if (conn.isClosed()) {
                System.err.println("[inventory_util] " + caller + ": conn is closed");
                return false;
            }
        } catch (Exception e) {
            System.err.println("[inventory_util] " + caller + ": isClosed() threw: " + e.getMessage());
            return false;
        }
        return true;
    }

    // ══════════════════════════════════════════════════════
    //  FETCH
    // ══════════════════════════════════════════════════════

    /**
     * Fetches all non-deleted inventory rows for the given tab
     * ("active" or "archived"), ordered alphabetically by ingredient.
     *
     * Returns rows as String[5]: { inventory_id, ingredient, quantity, unit, reorder_level }
     */
    public static List<String[]> fetchInventory(Connection conn, String tab) {
        List<String[]> rows = new ArrayList<>();
        if (!isConnUsable(conn, "fetchInventory")) return rows;

        String sql =
            "SELECT inventory_id, ingredient, quantity, unit, reorder_level " +
            "FROM dbo.Inventory " +
            "WHERE is_deleted = 0 AND status = ? " +
            "ORDER BY ingredient ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tab);
            System.out.println("[inventory_util] fetchInventory: executing query for tab='" + tab + "'");

            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    // inventory_id
                    String invId = rs.getString("inventory_id");
                    if (invId == null) invId = "—";

                    // ingredient
                    String ingredient = rs.getString("ingredient");
                    if (ingredient == null) ingredient = "—";

                    // quantity: DECIMAL(10,2) — read as BigDecimal to avoid driver quirks
                    String qtyStr;
                    try {
                        java.math.BigDecimal bd = rs.getBigDecimal("quantity");
                        if (bd == null) {
                            qtyStr = "0";
                        } else {
                            bd = bd.stripTrailingZeros();
                            // Avoid scientific notation for whole numbers
                            qtyStr = bd.scale() <= 0
                                ? bd.toBigIntegerExact().toString()
                                : bd.toPlainString();
                        }
                    } catch (Exception ex) {
                        double d = rs.getDouble("quantity");
                        qtyStr = (d == Math.floor(d) && !Double.isInfinite(d))
                            ? String.valueOf((long) d)
                            : String.valueOf(d);
                    }

                    // unit
                    String unit = rs.getString("unit");
                    if (unit == null) unit = "—";

                    // reorder_level: INT — use getInt() directly; getString() on INT
                    // can return null on some JDBC drivers
                    String reorderStr;
                    try {
                        int reorder = rs.getInt("reorder_level");
                        reorderStr = rs.wasNull() ? "0" : String.valueOf(reorder);
                    } catch (Exception ex) {
                        String raw = rs.getString("reorder_level");
                        reorderStr = (raw != null) ? raw : "0";
                    }

                    rows.add(new String[]{ invId, ingredient, qtyStr, unit, reorderStr });
                    count++;
                }
                System.out.println("[inventory_util] fetchInventory: loaded " + count + " row(s) for tab='" + tab + "'");
            }

        } catch (Exception e) {
            System.err.println("[inventory_util] fetchInventory ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        return rows;
    }

    // ══════════════════════════════════════════════════════
    //  UPDATE
    // ══════════════════════════════════════════════════════

    /**
     * Updates the quantity and reorder_level for the given inventory_id.
     */
    public static void updateIngredient(Connection conn, String inventoryId,
                                        int quantity, int reorderLevel) {
        if (!isConnUsable(conn, "updateIngredient")) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE dbo.Inventory SET quantity = ?, reorder_level = ? " +
                "WHERE inventory_id = ? AND is_deleted = 0")) {
            ps.setInt(1, quantity);
            ps.setInt(2, reorderLevel);
            ps.setString(3, inventoryId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════
    //  ARCHIVE / RESTORE
    // ══════════════════════════════════════════════════════

    /**
     * Sets status = 'archived' for each id in the given set.
     */
    public static void archiveSelected(Connection conn, Set<String> ids) {
        if (!isConnUsable(conn, "archiveSelected") || ids.isEmpty()) return;
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Inventory SET status = 'archived' " +
                    "WHERE inventory_id = ? AND is_deleted = 0")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    /**
     * Sets status = 'active' for each id in the given set.
     */
    public static void restoreSelected(Connection conn, Set<String> ids) {
        if (!isConnUsable(conn, "restoreSelected") || ids.isEmpty()) return;
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Inventory SET status = 'active' " +
                    "WHERE inventory_id = ? AND is_deleted = 0")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // ══════════════════════════════════════════════════════
    //  HARD DELETE
    // ══════════════════════════════════════════════════════

    /**
     * Soft-deletes (is_deleted = 1) all rows for the given tab.
     */
    public static void hardDeleteAll(Connection conn, String currentTab) {
        if (!isConnUsable(conn, "hardDeleteAll")) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE dbo.Inventory SET is_deleted = 1 " +
                "WHERE is_deleted = 0 AND status = ?")) {
            ps.setString(1, currentTab);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════
    //  INSERT
    // ══════════════════════════════════════════════════════

    /**
     * Inserts a new ingredient row, auto-generating the next INV-NNNN id.
     *
     * @return the generated inventory_id, or null on failure.
     */
    public static String insertIngredient(Connection conn, String ingredient,
                                          int quantity, String unit, int reorderLevel) {
        if (!isConnUsable(conn, "insertIngredient")) return null;

        // Generate next id by finding the max existing numeric suffix
        String newId = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT MAX(CAST(SUBSTRING(inventory_id, 5, LEN(inventory_id)) AS INT)) AS max_num " +
                "FROM dbo.Inventory WHERE is_deleted = 0")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int maxNum = rs.getInt("max_num");
                    if (rs.wasNull()) maxNum = 0;
                    newId = String.format("INV-%04d", maxNum + 1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        if (newId == null) return null;

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO dbo.Inventory (inventory_id, ingredient, quantity, unit, reorder_level) " +
                "VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, newId);
            ps.setString(2, ingredient);
            ps.setInt(3, quantity);
            ps.setString(4, unit);
            ps.setInt(5, reorderLevel);
            ps.executeUpdate();
            System.out.println("[inventory_util] insertIngredient: inserted " + newId);
            return newId;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ══════════════════════════════════════════════════════
    //  FILTERING
    // ══════════════════════════════════════════════════════

    /**
     * Returns the subset of rows whose ingredient name or inventory_id
     * contains the search query (case-insensitive).
     * If the query is blank, the full list is returned as-is.
     */
    public static List<String[]> getFilteredRows(List<String[]> cachedRows, String searchQuery) {
        if (searchQuery == null || searchQuery.isBlank()) return cachedRows;
        String q = searchQuery.trim().toLowerCase();
        List<String[]> filtered = new ArrayList<>();
        for (String[] row : cachedRows) {
            if (row[1].toLowerCase().contains(q) || row[0].toLowerCase().contains(q))
                filtered.add(row);
        }
        return filtered;
    }

    // ══════════════════════════════════════════════════════
    //  CSV EXPORT
    // ══════════════════════════════════════════════════════

    /**
     * Opens a save-file dialog and writes the cached rows to a CSV.
     *
     * @param cachedRows  the current in-memory rows
     * @param currentTab  used to suggest a default filename
     * @param ownerStage  the JavaFX stage to anchor the dialog to (may be null)
     */
    public static void exportCsv(List<String[]> cachedRows, String currentTab, Stage ownerStage) {
        if (cachedRows.isEmpty()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Inventory as CSV");
        chooser.setInitialFileName("inventory_" + currentTab + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = (ownerStage != null)
            ? chooser.showSaveDialog(ownerStage)
            : chooser.showSaveDialog(null);

        if (file == null) return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("Inventory ID,Ingredient,Quantity,Unit,Reorder Level");
            writer.newLine();
            for (String[] row : cachedRows) {
                writer.write(
                    escapeCsv(row[0]) + "," + escapeCsv(row[1]) + "," +
                    escapeCsv(row[2]) + "," + escapeCsv(row[3]) + "," + escapeCsv(row[4])
                );
                writer.newLine();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    /** Wraps a CSV field in quotes if it contains commas, quotes, or newlines. */
    public static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n"))
            return "\"" + value.replace("\"", "\"\"") + "\"";
        return value;
    }

    // ══════════════════════════════════════════════════════
    //  GENERAL HELPERS
    // ══════════════════════════════════════════════════════

    /** Safe int parse; returns 0 on null, blank, or non-numeric input. */
    public static int parseIntSafe(String s) {
        if (s == null || s.isBlank()) return 0;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    /**
     * Validates that a unit string is one of the accepted values.
     * Accepted values: ml, l, g, kg, pcs (case-insensitive).
     */
    public static boolean isValidUnit(String unit) {
        if (unit == null) return false;
        for (String u : new String[]{"ml", "l", "g", "kg", "pcs"}) {
            if (u.equalsIgnoreCase(unit)) return true;
        }
        return false;
    }
}