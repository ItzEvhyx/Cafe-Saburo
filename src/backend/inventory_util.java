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
 * SCHEMA:
 *  dbo.Inventory columns: inventory_id, ingredient_id, quantity, unit, reorder_level, is_deleted, status
 *  dbo.Ingredients columns: ingredient_id, ingredient_name, price, is_deleted, status
 *
 * fetchAllActive returns String[3]:
 *   [0] inventory_id
 *   [1] ingredient_name
 *   [2] ingredient_id
 *
 * fetchAllIngredients returns String[2] from dbo.Ingredients directly:
 *   [0] ingredient_id
 *   [1] ingredient_name
 *   Used by the Add Ingredient modal so the user picks from real existing
 *   ingredients instead of typing a free-text name.
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
    //  FETCH INVENTORY
    // ══════════════════════════════════════════════════════

    /**
     * Fetches all non-deleted inventory rows for the given tab
     * ("active" or "archived"), ordered alphabetically by ingredient name.
     *
     * Returns rows as String[5]:
     *   { inventory_id, ingredient_name, quantity, unit, reorder_level }
     */
    public static List<String[]> fetchInventory(Connection conn, String tab) {
        List<String[]> rows = new ArrayList<>();
        if (!isConnUsable(conn, "fetchInventory")) return rows;

        String sql =
            "SELECT inv.inventory_id, " +
            "       ISNULL(ing.ingredient_name, N'—') AS ingredient_name, " +
            "       inv.quantity, inv.unit, inv.reorder_level " +
            "FROM   dbo.Inventory inv " +
            "LEFT JOIN dbo.Ingredients ing ON ing.ingredient_id = inv.ingredient_id " +
            "                             AND ing.is_deleted = 0 " +
            "WHERE  inv.is_deleted = 0 AND inv.[status] = ? " +
            "ORDER  BY ing.ingredient_name ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tab);
            System.out.println("[inventory_util] fetchInventory: executing query for tab='" + tab + "'");

            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    String invId = rs.getString("inventory_id");
                    if (invId == null) invId = "—";

                    String ingredient = rs.getString("ingredient_name");
                    if (ingredient == null) ingredient = "—";

                    String qtyStr;
                    try {
                        java.math.BigDecimal bd = rs.getBigDecimal("quantity");
                        if (bd == null) {
                            qtyStr = "0";
                        } else {
                            bd = bd.stripTrailingZeros();
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

                    String unit = rs.getString("unit");
                    if (unit == null) unit = "—";

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
    //  FETCH ALL ACTIVE (from dbo.Inventory)
    //  Used by the Add/Edit Supplier modal to populate the
    //  ingredient checkbox list.
    //
    //  Returns List<String[3]>:
    //    [0] inventory_id
    //    [1] ingredient_name  (display label shown in dropdown)
    //    [2] ingredient_id    (FK — written to Supplier_Ingredients)
    // ══════════════════════════════════════════════════════

    /**
     * Returns every non-deleted active inventory item for use in selector UIs.
     * Each String[3] contains: { inventory_id, ingredient_name, ingredient_id }
     */
    public static List<String[]> fetchAllActive(Connection conn) {
        List<String[]> rows = new ArrayList<>();
        if (!isConnUsable(conn, "fetchAllActive")) return rows;

        String sql =
            "SELECT inv.inventory_id, " +
            "       ISNULL(ing.ingredient_name, N'—') AS ingredient_name, " +
            "       inv.ingredient_id " +
            "FROM   dbo.Inventory inv " +
            "JOIN   dbo.Ingredients ing ON ing.ingredient_id = inv.ingredient_id " +
            "                          AND ing.is_deleted = 0 " +
            "WHERE  inv.is_deleted = 0 AND inv.[status] = 'active' " +
            "ORDER  BY ing.ingredient_name ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String invId  = rs.getString("inventory_id");
                String ingred = rs.getString("ingredient_name");
                String ingId  = rs.getString("ingredient_id");
                if (invId  == null) invId  = "";
                if (ingred == null) ingred = "—";
                if (ingId  == null) ingId  = "";
                rows.add(new String[]{ invId, ingred, ingId });
            }
            System.out.println("[inventory_util] fetchAllActive: loaded " + rows.size() + " active ingredient(s)");
        } catch (Exception e) {
            System.err.println("[inventory_util] fetchAllActive ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return rows;
    }

    // ══════════════════════════════════════════════════════
    //  FETCH ALL INGREDIENTS (from dbo.Ingredients directly)
    //  Used by the Add Ingredient modal in inventory_contents
    //  so the user selects from real existing ingredients
    //  instead of typing a free-text name.
    //
    //  Returns List<String[2]>:
    //    [0] ingredient_id
    //    [1] ingredient_name
    // ══════════════════════════════════════════════════════

    /**
     * Fetches all active, non-deleted ingredients from dbo.Ingredients.
     * Each String[2] contains: { ingredient_id, ingredient_name }
     *
     * This is intentionally separate from fetchAllActive() which reads
     * dbo.Inventory. Use this method when you need the master ingredient
     * catalogue (e.g. to populate the Add Inventory modal dropdown).
     */
    public static List<String[]> fetchAllIngredients(Connection conn) {
        List<String[]> rows = new ArrayList<>();
        if (!isConnUsable(conn, "fetchAllIngredients")) return rows;

        String sql =
            "SELECT ingredient_id, ingredient_name " +
            "FROM   dbo.Ingredients " +
            "WHERE  is_deleted = 0 AND [status] = 'active' " +
            "ORDER  BY ingredient_name ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String ingId   = rs.getString("ingredient_id");
                String ingName = rs.getString("ingredient_name");
                if (ingId   == null) ingId   = "";
                if (ingName == null) ingName = "—";
                rows.add(new String[]{ ingId, ingName });
            }
            System.out.println("[inventory_util] fetchAllIngredients: loaded " + rows.size() + " ingredient(s)");
        } catch (Exception e) {
            System.err.println("[inventory_util] fetchAllIngredients ERROR: " + e.getMessage());
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

    public static void archiveSelected(Connection conn, Set<String> ids) {
        if (!isConnUsable(conn, "archiveSelected") || ids.isEmpty()) return;
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Inventory SET [status] = 'archived' " +
                    "WHERE inventory_id = ? AND is_deleted = 0")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public static void restoreSelected(Connection conn, Set<String> ids) {
        if (!isConnUsable(conn, "restoreSelected") || ids.isEmpty()) return;
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Inventory SET [status] = 'active' " +
                    "WHERE inventory_id = ? AND is_deleted = 0")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // ══════════════════════════════════════════════════════
    //  HARD DELETE
    // ══════════════════════════════════════════════════════

    public static void hardDeleteAll(Connection conn, String currentTab) {
        if (!isConnUsable(conn, "hardDeleteAll")) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE dbo.Inventory SET is_deleted = 1 " +
                "WHERE is_deleted = 0 AND [status] = ?")) {
            ps.setString(1, currentTab);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════
    //  INSERT
    // ══════════════════════════════════════════════════════

    /**
     * Inserts a new inventory row using the ingredient_id directly.
     * Auto-generates the next INV-NNNN id.
     *
     * This overload accepts ingredient_id directly (used by the modal
     * dropdown flow where the user selects from dbo.Ingredients).
     *
     * @return the generated inventory_id, or null on failure.
     */
    public static String insertIngredientById(Connection conn, String ingredientId,
                                               int quantity, String unit, int reorderLevel) {
        if (!isConnUsable(conn, "insertIngredientById")) return null;

        // Verify ingredient_id exists and is active
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ingredient_id FROM dbo.Ingredients " +
                "WHERE ingredient_id = ? AND is_deleted = 0")) {
            ps.setString(1, ingredientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.err.println("[inventory_util] insertIngredientById: ingredient_id not found: " + ingredientId);
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        // Generate next INV-NNNN id
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

        // Insert the inventory row with ingredient_id
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO dbo.Inventory (inventory_id, ingredient_id, quantity, unit, reorder_level) " +
                "VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, newId);
            ps.setString(2, ingredientId);
            ps.setInt   (3, quantity);
            ps.setString(4, unit);
            ps.setInt   (5, reorderLevel);
            ps.executeUpdate();
            System.out.println("[inventory_util] insertIngredientById: inserted " + newId + " linked to " + ingredientId);
            return newId;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Inserts a new inventory row, auto-generating the next INV-NNNN id.
     * Looks up ingredient_id from dbo.Ingredients by ingredient_name.
     * If the ingredient name doesn't exist in dbo.Ingredients, returns null.
     *
     * @return the generated inventory_id, or null on failure.
     */
    public static String insertIngredient(Connection conn, String ingredientName,
                                          int quantity, String unit, int reorderLevel) {
        if (!isConnUsable(conn, "insertIngredient")) return null;

        // Resolve ingredient_id from dbo.Ingredients by name
        String ingredientId = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ingredient_id FROM dbo.Ingredients " +
                "WHERE ingredient_name = ? AND is_deleted = 0")) {
            ps.setString(1, ingredientName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) ingredientId = rs.getString("ingredient_id");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        if (ingredientId == null) {
            System.err.println("[inventory_util] insertIngredient: no Ingredients row found for name='" + ingredientName + "'");
            return null;
        }

        return insertIngredientById(conn, ingredientId, quantity, unit, reorderLevel);
    }

    // ══════════════════════════════════════════════════════
    //  FILTERING
    // ══════════════════════════════════════════════════════

    /**
     * Filters rows by inventory_id or ingredient name (row[1]).
     */
    public static List<String[]> getFilteredRows(List<String[]> cachedRows, String searchQuery) {
        if (searchQuery == null || searchQuery.isBlank()) return cachedRows;
        String q = searchQuery.trim().toLowerCase();
        List<String[]> filtered = new ArrayList<>();
        for (String[] row : cachedRows) {
            if (row[0].toLowerCase().contains(q) ||
                row[1].toLowerCase().contains(q))
                filtered.add(row);
        }
        return filtered;
    }

    // ══════════════════════════════════════════════════════
    //  CSV EXPORT
    // ══════════════════════════════════════════════════════

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

    public static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n"))
            return "\"" + value.replace("\"", "\"\"") + "\"";
        return value;
    }

    // ══════════════════════════════════════════════════════
    //  GENERAL HELPERS
    // ══════════════════════════════════════════════════════

    public static int parseIntSafe(String s) {
        if (s == null || s.isBlank()) return 0;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    public static boolean isValidUnit(String unit) {
        if (unit == null) return false;
        for (String u : new String[]{"ml", "l", "g", "kg", "pcs"}) {
            if (u.equalsIgnoreCase(unit)) return true;
        }
        return false;
    }
}