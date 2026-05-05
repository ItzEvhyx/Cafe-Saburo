package backend;

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
 * suppliers_util
 *
 * Holds all business logic, database operations, CSV export,
 * and data-filtering for the Suppliers module.
 *
 * suppliers_contents (UI) delegates every non-visual concern here.
 *
 * NOTE ON NORMALIZATION:
 *  fetchSuppliers now returns ONE ROW PER INGREDIENT — not one row per supplier.
 *  This makes the in-memory data 1NF / 2NF / 3NF compliant at the display layer.
 *
 *  Each String[6] row contains:
 *    [0] supplier_id
 *    [1] supplier_name
 *    [2] ingredient_name     ← single value, never a comma-separated list
 *    [3] contact_info
 *    [4] address
 *    [5] inventory_id        ← kept for junction-table operations
 *
 *  The view (vw_Suppliers with STRING_AGG) is still used only for CSV export
 *  so the exported file stays human-readable.
 *
 * CHANGE LOG:
 *  - fetchSuppliers: fixed i.ingredient → JOIN dbo.Ingredients + ing.ingredient_name
 *  - fetchIngredientLinks: fixed ORDER BY i.ingredient → JOIN dbo.Ingredients + ing.ingredient_name
 *  - All other methods: unchanged.
 */
public class suppliers_util {

    // ══════════════════════════════════════════════════════
    //  DB HELPERS
    // ══════════════════════════════════════════════════════

    private static boolean isConnUsable(Connection conn, String caller) {
        if (conn == null) {
            System.err.println("[suppliers_util] " + caller + ": conn is null");
            return false;
        }
        try {
            if (conn.isClosed()) {
                System.err.println("[suppliers_util] " + caller + ": conn is closed");
                return false;
            }
        } catch (Exception e) {
            System.err.println("[suppliers_util] " + caller + ": isClosed() threw: " + e.getMessage());
            return false;
        }
        return true;
    }

    // ══════════════════════════════════════════════════════
    //  FETCH  —  ONE ROW PER INGREDIENT  (1NF compliant)
    // ══════════════════════════════════════════════════════

    /**
     * Fetches supplier-ingredient data for the given tab ("active" or "archived").
     *
     * Returns ONE String[6] per ingredient link, ordered by supplier_name then ingredient_name:
     *   { supplier_id, supplier_name, ingredient_name, contact_info, address, inventory_id }
     *
     * A supplier with N ingredients produces N rows.
     * A supplier with zero ingredients produces one row with ingredient_name = "—" and inventory_id = "".
     */
    public static List<String[]> fetchSuppliers(Connection conn, String tab) {
        List<String[]> rows = new ArrayList<>();
        if (!isConnUsable(conn, "fetchSuppliers")) return rows;

        // FIX: dbo.Inventory no longer has 'ingredient' column.
        // Added JOIN dbo.Ingredients to resolve ingredient_name from ingredient_id.
        // Old:  LEFT JOIN dbo.Inventory i ON i.inventory_id = si.inventory_id ...
        //       ISNULL(i.ingredient, N'—') AS ingredient
        //       ORDER BY ... i.ingredient ASC
        // New:  also LEFT JOIN dbo.Ingredients ing ON ing.ingredient_id = inv.ingredient_id ...
        //       ISNULL(ing.ingredient_name, N'—') AS ingredient_name
        //       ORDER BY ... ing.ingredient_name ASC
        String sql =
            "SELECT " +
            "    s.supplier_id, " +
            "    s.supplier_name, " +
            "    ISNULL(ing.ingredient_name, N'—') AS ingredient_name, " +
            "    s.contact_info, " +
            "    s.address, " +
            "    ISNULL(si.inventory_id, N'') AS inventory_id " +
            "FROM       dbo.Suppliers            s " +
            "LEFT JOIN  dbo.Supplier_Ingredients si  ON si.supplier_id   = s.supplier_id " +
            "LEFT JOIN  dbo.Inventory            inv ON inv.inventory_id  = si.inventory_id " +
            "                                       AND inv.is_deleted   = 0 " +
            "LEFT JOIN  dbo.Ingredients          ing ON ing.ingredient_id = inv.ingredient_id " +
            "                                       AND ing.is_deleted   = 0 " +
            "WHERE s.is_deleted = 0 " +
            "  AND s.[status]   = ? " +
            "ORDER BY s.supplier_name ASC, ing.ingredient_name ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tab);
            System.out.println("[suppliers_util] fetchSuppliers: executing for tab='" + tab + "'");
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    rows.add(new String[]{
                        rs.getString("supplier_id"),
                        rs.getString("supplier_name"),
                        rs.getString("ingredient_name"),
                        rs.getString("contact_info"),
                        rs.getString("address"),
                        rs.getString("inventory_id")
                    });
                    count++;
                }
                System.out.println("[suppliers_util] fetchSuppliers: loaded " + count + " ingredient-row(s) for tab='" + tab + "'");
            }
        } catch (Exception e) {
            System.err.println("[suppliers_util] fetchSuppliers ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return rows;
    }

    // ══════════════════════════════════════════════════════
    //  FETCH INGREDIENT LINKS
    // ══════════════════════════════════════════════════════

    /**
     * Returns every inventory_id currently linked to the given supplier
     * in the Supplier_Ingredients junction table.
     * Used to pre-populate the ingredient selector in the Add / Edit modal.
     */
    public static List<String> fetchIngredientLinks(Connection conn, String supplierId) {
        List<String> ids = new ArrayList<>();
        if (!isConnUsable(conn, "fetchIngredientLinks")) return ids;

        // FIX: was "ORDER BY i.ingredient ASC" — i.ingredient column no longer exists.
        // Added JOIN dbo.Ingredients so we can ORDER BY ing.ingredient_name.
        // Old:  JOIN dbo.Inventory i ON i.inventory_id = si.inventory_id AND i.is_deleted = 0
        //       ORDER BY i.ingredient ASC
        // New:  JOIN dbo.Inventory inv ...
        //       JOIN dbo.Ingredients ing ON ing.ingredient_id = inv.ingredient_id AND ing.is_deleted = 0
        //       ORDER BY ing.ingredient_name ASC
        String sql =
            "SELECT si.inventory_id " +
            "FROM   dbo.Supplier_Ingredients si " +
            "JOIN   dbo.Inventory   inv ON inv.inventory_id  = si.inventory_id  AND inv.is_deleted = 0 " +
            "JOIN   dbo.Ingredients ing ON ing.ingredient_id = inv.ingredient_id AND ing.is_deleted = 0 " +
            "WHERE  si.supplier_id = ? " +
            "ORDER  BY ing.ingredient_name ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, supplierId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getString("inventory_id"));
            }
        } catch (Exception e) {
            System.err.println("[suppliers_util] fetchIngredientLinks ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return ids;
    }

    // ══════════════════════════════════════════════════════
    //  INSERT
    // ══════════════════════════════════════════════════════

    /**
     * Inserts a new supplier header row, then links each selected
     * inventory_id into Supplier_Ingredients.
     *
     * @param inventoryIds  list of inventory_id values chosen in the UI
     * @return the generated supplier_id, or null on failure
     */
    public static String insertSupplier(Connection conn,
                                        String supplierName,
                                        List<String> inventoryIds,
                                        String contactInfo,
                                        String address) {
        if (!isConnUsable(conn, "insertSupplier")) return null;

        // Step 1 — generate next SUP-NNNN id
        String newId = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT MAX(CAST(SUBSTRING(supplier_id, 5, LEN(supplier_id)) AS INT)) AS max_num " +
                "FROM dbo.Suppliers WHERE is_deleted = 0")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int maxNum = rs.getInt("max_num");
                    if (rs.wasNull()) maxNum = 0;
                    newId = String.format("SUP-%04d", maxNum + 1);
                }
            }
        } catch (Exception e) { e.printStackTrace(); return null; }

        if (newId == null) return null;

        // Step 2 — insert header row (no ingredients column)
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO dbo.Suppliers (supplier_id, supplier_name, contact_info, address) " +
                "VALUES (?, ?, ?, ?)")) {
            ps.setString(1, newId);
            ps.setString(2, supplierName);
            ps.setString(3, contactInfo);
            ps.setString(4, address);
            ps.executeUpdate();
            System.out.println("[suppliers_util] insertSupplier: inserted " + newId);
        } catch (Exception e) { e.printStackTrace(); return null; }

        // Step 3 — insert junction rows for each selected ingredient
        insertIngredientLinks(conn, newId, inventoryIds);

        return newId;
    }

    // ══════════════════════════════════════════════════════
    //  UPDATE
    // ══════════════════════════════════════════════════════

    /**
     * Updates supplier header fields only (name, contact, address).
     * Ingredient links are managed separately via replaceIngredientLinks().
     */
    public static void updateSupplier(Connection conn,
                                      String supplierId,
                                      String supplierName,
                                      String contactInfo,
                                      String address) {
        if (!isConnUsable(conn, "updateSupplier")) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE dbo.Suppliers " +
                "SET supplier_name = ?, contact_info = ?, address = ? " +
                "WHERE supplier_id = ? AND is_deleted = 0")) {
            ps.setString(1, supplierName);
            ps.setString(2, contactInfo);
            ps.setString(3, address);
            ps.setString(4, supplierId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════
    //  JUNCTION TABLE HELPERS
    // ══════════════════════════════════════════════════════

    /**
     * Inserts one junction row per inventory_id.
     * Silently skips duplicates (PK violation caught and ignored).
     */
    public static void insertIngredientLinks(Connection conn,
                                              String supplierId,
                                              List<String> inventoryIds) {
        if (!isConnUsable(conn, "insertIngredientLinks") || inventoryIds == null) return;
        for (String invId : inventoryIds) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO dbo.Supplier_Ingredients (supplier_id, inventory_id) VALUES (?, ?)")) {
                ps.setString(1, supplierId);
                ps.setString(2, invId);
                ps.executeUpdate();
            } catch (Exception e) {
                System.err.println("[suppliers_util] insertIngredientLinks: skipped " + invId + " — " + e.getMessage());
            }
        }
    }

    /**
     * Replaces all junction rows for the given supplier in one operation:
     * DELETE existing links, then INSERT the new set.
     */
    public static void replaceIngredientLinks(Connection conn,
                                               String supplierId,
                                               List<String> inventoryIds) {
        if (!isConnUsable(conn, "replaceIngredientLinks")) return;

        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM dbo.Supplier_Ingredients WHERE supplier_id = ?")) {
            ps.setString(1, supplierId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); return; }

        insertIngredientLinks(conn, supplierId, inventoryIds);
    }

    /**
     * Removes a single ingredient link from the junction table.
     */
    public static void removeIngredientLink(Connection conn,
                                             String supplierId,
                                             String inventoryId) {
        if (!isConnUsable(conn, "removeIngredientLink")) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM dbo.Supplier_Ingredients " +
                "WHERE supplier_id = ? AND inventory_id = ?")) {
            ps.setString(1, supplierId);
            ps.setString(2, inventoryId);
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
                    "UPDATE dbo.Suppliers SET [status] = 'archived' " +
                    "WHERE supplier_id = ? AND is_deleted = 0")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public static void restoreSelected(Connection conn, Set<String> ids) {
        if (!isConnUsable(conn, "restoreSelected") || ids.isEmpty()) return;
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Suppliers SET [status] = 'active' " +
                    "WHERE supplier_id = ? AND is_deleted = 0")) {
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
     * Junction rows are left intact (unreachable via the view).
     */
    public static void hardDeleteAll(Connection conn, String currentTab) {
        if (!isConnUsable(conn, "hardDeleteAll")) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE dbo.Suppliers SET is_deleted = 1 " +
                "WHERE is_deleted = 0 AND [status] = ?")) {
            ps.setString(1, currentTab);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════
    //  FILTERING
    // ══════════════════════════════════════════════════════

    /**
     * Returns the subset of rows whose supplier_id, supplier_name, or ingredient_name
     * contains the search query (case-insensitive).
     * Operates on the one-row-per-ingredient list from fetchSuppliers().
     */
    public static List<String[]> getFilteredRows(List<String[]> cachedRows, String searchQuery) {
        if (searchQuery == null || searchQuery.isBlank()) return cachedRows;
        String q = searchQuery.trim().toLowerCase();
        List<String[]> filtered = new ArrayList<>();
        for (String[] row : cachedRows) {
            if (row[0].toLowerCase().contains(q) ||   // supplier_id
                row[1].toLowerCase().contains(q) ||   // supplier_name
                row[2].toLowerCase().contains(q))     // ingredient_name (single value)
                filtered.add(row);
        }
        return filtered;
    }

    // ══════════════════════════════════════════════════════
    //  CSV EXPORT  —  uses vw_Suppliers (STRING_AGG) for readability
    // ══════════════════════════════════════════════════════

    /**
     * Opens a save-file dialog and writes supplier data to CSV.
     * Queries vw_Suppliers so each supplier occupies one CSV row with
     * all ingredients comma-separated — more useful for a spreadsheet export.
     */
    public static void exportCsv(Connection conn, String currentTab, Stage ownerStage) {
        if (!isConnUsable(conn, "exportCsv")) return;

        List<String[]> exportRows = new ArrayList<>();
        String sql =
            "SELECT supplier_id, supplier_name, ingredients, contact_info, address " +
            "FROM dbo.vw_Suppliers " +
            "WHERE is_deleted = 0 AND [status] = ? " +
            "ORDER BY supplier_name ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, currentTab);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    exportRows.add(new String[]{
                        rs.getString("supplier_id"),
                        rs.getString("supplier_name"),
                        rs.getString("ingredients"),
                        rs.getString("contact_info"),
                        rs.getString("address")
                    });
                }
            }
        } catch (Exception e) { e.printStackTrace(); return; }

        if (exportRows.isEmpty()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Suppliers as CSV");
        chooser.setInitialFileName("suppliers_" + currentTab + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = (ownerStage != null)
            ? chooser.showSaveDialog(ownerStage)
            : chooser.showSaveDialog(null);
        if (file == null) return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("Supplier ID,Supplier Name,Ingredients,Contact Info,Address");
            writer.newLine();
            for (String[] row : exportRows) {
                writer.write(
                    inventory_util.escapeCsv(row[0]) + "," +
                    inventory_util.escapeCsv(row[1]) + "," +
                    inventory_util.escapeCsv(row[2]) + "," +
                    inventory_util.escapeCsv(row[3]) + "," +
                    inventory_util.escapeCsv(row[4])
                );
                writer.newLine();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}