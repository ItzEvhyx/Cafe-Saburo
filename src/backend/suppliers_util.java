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
 * Note: font loading is shared via inventory_util.loadFonts().
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
    //  FETCH
    // ══════════════════════════════════════════════════════

    /**
     * Fetches all non-deleted supplier rows for the given tab
     * ("active" or "archived"), ordered alphabetically by supplier_name.
     *
     * Returns rows as String[5]: { supplier_id, supplier_name, ingredients, contact_info, address }
     */
    public static List<String[]> fetchSuppliers(Connection conn, String tab) {
        List<String[]> rows = new ArrayList<>();
        if (!isConnUsable(conn, "fetchSuppliers")) return rows;

        String sql =
            "SELECT supplier_id, supplier_name, ingredients, contact_info, address " +
            "FROM dbo.vw_Suppliers " +
            "WHERE is_deleted = 0 AND [status] = ? " +
            "ORDER BY supplier_name ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tab);
            System.out.println("[suppliers_util] fetchSuppliers: executing query for tab='" + tab + "'");
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    String suppId  = rs.getString("supplier_id");   if (suppId  == null) suppId  = "—";
                    String name    = rs.getString("supplier_name"); if (name    == null) name    = "—";
                    String ingreds = rs.getString("ingredients");   if (ingreds == null) ingreds = "—";
                    String contact = rs.getString("contact_info");  if (contact == null) contact = "—";
                    String address = rs.getString("address");       if (address == null) address = "—";
                    rows.add(new String[]{ suppId, name, ingreds, contact, address });
                    count++;
                }
                System.out.println("[suppliers_util] fetchSuppliers: loaded " + count + " row(s) for tab='" + tab + "'");
            }
        } catch (Exception e) {
            System.err.println("[suppliers_util] fetchSuppliers ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return rows;
    }

    // ══════════════════════════════════════════════════════
    //  INSERT
    // ══════════════════════════════════════════════════════

    /**
     * Inserts a new supplier row, auto-generating the next SUP-NNNN id.
     *
     * @return the generated supplier_id, or null on failure.
     */
    public static String insertSupplier(Connection conn, String supplierName,
                                        String ingredients, String contactInfo, String address) {
        if (!isConnUsable(conn, "insertSupplier")) return null;

        String newId = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT MAX(CAST(SUBSTRING(supplier_id, 5, LEN(supplier_id)) AS INT)) AS max_num " +
                "FROM dbo.vw_Suppliers WHERE is_deleted = 0")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int maxNum = rs.getInt("max_num");
                    if (rs.wasNull()) maxNum = 0;
                    newId = String.format("SUP-%04d", maxNum + 1);
                }
            }
        } catch (Exception e) { e.printStackTrace(); return null; }

        if (newId == null) return null;

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO dbo.Suppliers (supplier_id, supplier_name, ingredients, contact_info, address) " +
                "VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, newId);
            ps.setString(2, supplierName);
            ps.setString(3, ingredients);
            ps.setString(4, contactInfo);
            ps.setString(5, address);
            ps.executeUpdate();
            System.out.println("[suppliers_util] insertSupplier: inserted " + newId);
            return newId;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    // ══════════════════════════════════════════════════════
    //  UPDATE
    // ══════════════════════════════════════════════════════

    /**
     * Updates all editable fields for the given supplier_id.
     */
    public static void updateSupplier(Connection conn, String supplierId, String supplierName,
                                      String ingredients, String contactInfo, String address) {
        if (!isConnUsable(conn, "updateSupplier")) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE dbo.Suppliers SET supplier_name = ?, ingredients = ?, " +
                "contact_info = ?, address = ? " +
                "WHERE supplier_id = ? AND is_deleted = 0")) {
            ps.setString(1, supplierName);
            ps.setString(2, ingredients);
            ps.setString(3, contactInfo);
            ps.setString(4, address);
            ps.setString(5, supplierId);
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
                    "UPDATE dbo.Suppliers SET [status] = 'archived' " +
                    "WHERE supplier_id = ? AND is_deleted = 0")) {
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
     * Returns the subset of rows whose supplier_id, supplier_name, or ingredients
     * contains the search query (case-insensitive).
     * If the query is blank, the full list is returned as-is.
     */
    public static List<String[]> getFilteredRows(List<String[]> cachedRows, String searchQuery) {
        if (searchQuery == null || searchQuery.isBlank()) return cachedRows;
        String q = searchQuery.trim().toLowerCase();
        List<String[]> filtered = new ArrayList<>();
        for (String[] row : cachedRows) {
            if (row[0].toLowerCase().contains(q) ||
                row[1].toLowerCase().contains(q) ||
                row[2].toLowerCase().contains(q))
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
            for (String[] row : cachedRows) {
                writer.write(
                    inventory_util.escapeCsv(row[0]) + "," + inventory_util.escapeCsv(row[1]) + "," +
                    inventory_util.escapeCsv(row[2]) + "," + inventory_util.escapeCsv(row[3]) + "," +
                    inventory_util.escapeCsv(row[4])
                );
                writer.newLine();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}