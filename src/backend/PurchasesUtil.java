package backend;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PurchasesUtil
 *
 * Contains all business logic and database operations for the Purchases module.
 * The UI layer (purchases_contents) delegates every data/logic call here.
 *
 * Row format throughout: String[] { purchase_id, supplier_id, inventory_id, ingredient, order_date, status }
 */
public class PurchasesUtil {

    private final Connection conn;

    public PurchasesUtil(Connection conn) {
        this.conn = conn;
    }

    // ══════════════════════════════════════════════════════
    //  CONNECTION GUARD
    // ══════════════════════════════════════════════════════

    /** Returns true when the connection is usable. */
    private boolean isConnAvailable() {
        if (conn == null) {
            System.err.println("[PurchasesUtil] conn is null");
            return false;
        }
        try {
            if (conn.isClosed()) {
                System.err.println("[PurchasesUtil] conn is closed");
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    // ══════════════════════════════════════════════════════
    //  FETCH
    // ══════════════════════════════════════════════════════

    /**
     * Fetches all purchases for the given tab ("active" or "archived").
     *
     * @param tab "active" → all non-archived rows; "archived" → status = 'archived'
     * @return list of rows: [purchase_id, supplier_id, inventory_id, ingredient, order_date, status]
     */
    public List<String[]> fetchPurchases(String tab) {
        List<String[]> rows = new ArrayList<>();
        if (!isConnAvailable()) return rows;

        String sql = tab.equals("archived")
            ? "SELECT p.purchase_id, p.supplier_id, p.inventory_id, " +
              "ISNULL(i.ingredient, '—') AS ingredient, " +
              "CONVERT(VARCHAR(10), p.order_date, 120) AS order_date, p.[status] " +
              "FROM dbo.Purchases p " +
              "LEFT JOIN dbo.Inventory i ON i.inventory_id = p.inventory_id AND i.is_deleted = 0 " +
              "WHERE p.is_deleted = 0 AND p.[status] = 'archived' " +
              "ORDER BY p.order_date DESC, p.purchase_id ASC"
            : "SELECT p.purchase_id, p.supplier_id, p.inventory_id, " +
              "ISNULL(i.ingredient, '—') AS ingredient, " +
              "CONVERT(VARCHAR(10), p.order_date, 120) AS order_date, p.[status] " +
              "FROM dbo.Purchases p " +
              "LEFT JOIN dbo.Inventory i ON i.inventory_id = p.inventory_id AND i.is_deleted = 0 " +
              "WHERE p.is_deleted = 0 AND p.[status] <> 'archived' " +
              "ORDER BY p.order_date DESC, p.purchase_id ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            System.out.println("[PurchasesUtil] fetchPurchases: executing for tab='" + tab + "'");
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    String purchaseId = nvl(rs.getString(1));
                    String suppId     = nvl(rs.getString(2));
                    String invId      = nvl(rs.getString(3));
                    String ingredient = nvl(rs.getString(4));
                    String orderDate  = nvl(rs.getString(5));
                    String status     = nvl(rs.getString(6));
                    rows.add(new String[]{ purchaseId, suppId, invId, ingredient, orderDate, status });
                    count++;
                }
                System.out.println("[PurchasesUtil] fetchPurchases: loaded " + count + " row(s)");
            }
        } catch (Exception e) {
            System.err.println("[PurchasesUtil] fetchPurchases ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return rows;
    }

    /**
     * Returns { supplierId → supplierName } for all active, non-deleted suppliers.
     * Used to populate the supplier dropdown in the Make an Order modal.
     */
    public Map<String, String> fetchActiveSuppliers() {
        Map<String, String> map = new LinkedHashMap<>();
        if (!isConnAvailable()) return map;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT supplier_id, supplier_name FROM dbo.Suppliers " +
                "WHERE is_deleted = 0 AND [status] = 'active' ORDER BY supplier_name ASC")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getString(1), rs.getString(2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * Returns list of { inventory_id, ingredient, unit } for items linked to the given supplier.
     * Used to populate the item dropdown after a supplier is selected.
     */
    public List<String[]> fetchItemsForSupplier(String supplierId) {
        List<String[]> items = new ArrayList<>();
        if (!isConnAvailable() || supplierId == null) return items;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT si.inventory_id, i.ingredient, i.unit " +
                "FROM dbo.Supplier_Ingredients si " +
                "JOIN dbo.Inventory i ON i.inventory_id = si.inventory_id AND i.is_deleted = 0 " +
                "WHERE si.supplier_id = ? ORDER BY i.ingredient ASC")) {
            ps.setString(1, supplierId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    items.add(new String[]{ rs.getString(1), rs.getString(2), rs.getString(3) });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    // ══════════════════════════════════════════════════════
    //  WRITE OPERATIONS
    // ══════════════════════════════════════════════════════

    /**
     * Updates only the [status] column for a single purchase row.
     */
    public void updateStatus(String purchaseId, String newStatus) {
        if (!isConnAvailable()) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE dbo.Purchases SET [status] = ? WHERE purchase_id = ? AND is_deleted = 0")) {
            ps.setString(1, newStatus);
            ps.setString(2, purchaseId);
            ps.executeUpdate();
            System.out.println("[PurchasesUtil] updateStatus: " + purchaseId + " → " + newStatus);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Inserts one purchase line and returns the generated purchase_id, or null on failure.
     *
     * @param supplierId  supplier's ID
     * @param inventoryId inventory item's ID
     * @param quantity    quantity ordered (must be > 0)
     * @param groupId     purchase group ID (see {@link #nextGroupId()})
     * @return the new purchase_id string, or null if the insert failed
     */
    public String insertPurchase(String supplierId, String inventoryId, int quantity, String groupId) {
        if (!isConnAvailable()) return null;

        String newId = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ISNULL(MAX(CAST(SUBSTRING(purchase_id, 5, LEN(purchase_id)) AS INT)), 0) " +
                "FROM dbo.Purchases")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) newId = String.format("PUR-%04d", rs.getInt(1) + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        if (newId == null) return null;

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO dbo.Purchases " +
                "(purchase_id, purchase_group_id, supplier_id, inventory_id, " +
                " quantity_ordered, order_date, [status]) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'Pending')")) {
            ps.setString(1, newId);
            ps.setString(2, groupId);
            ps.setString(3, supplierId);
            ps.setString(4, inventoryId);
            ps.setInt   (5, quantity);
            ps.setString(6, LocalDate.now().toString());
            ps.executeUpdate();
            System.out.println("[PurchasesUtil] insertPurchase: inserted " + newId);
            return newId;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generates the next purchase_group_id (e.g. "PG-016").
     */
    public String nextGroupId() {
        if (!isConnAvailable()) return "PG-001";
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ISNULL(MAX(CAST(SUBSTRING(purchase_group_id, 4, LEN(purchase_group_id)) AS INT)), 0) " +
                "FROM dbo.Purchases")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return String.format("PG-%03d", rs.getInt(1) + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "PG-001";
    }

    /**
     * Sets status = 'archived' for every purchase_id in {@code ids}.
     */
    public void archiveSelected(Set<String> ids) {
        if (!isConnAvailable() || ids.isEmpty()) return;
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Purchases SET [status] = 'archived' " +
                    "WHERE purchase_id = ? AND is_deleted = 0")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Restores archived rows to 'Pending' for every purchase_id in {@code ids}.
     */
    public void restoreSelected(Set<String> ids) {
        if (!isConnAvailable() || ids.isEmpty()) return;
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Purchases SET [status] = 'Pending' " +
                    "WHERE purchase_id = ? AND is_deleted = 0")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Soft-deletes (is_deleted = 1) all rows in the given tab.
     *
     * @param currentTab "active" deletes non-archived rows; "archived" deletes archived rows
     */
    public void hardDeleteAll(String currentTab) {
        if (!isConnAvailable()) return;
        String sql = currentTab.equals("archived")
            ? "UPDATE dbo.Purchases SET is_deleted = 1 WHERE is_deleted = 0 AND [status] = 'archived'"
            : "UPDATE dbo.Purchases SET is_deleted = 1 WHERE is_deleted = 0 AND [status] <> 'archived'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════
    //  SEARCH / FILTER
    // ══════════════════════════════════════════════════════

    /**
     * Filters a list of purchase rows against a search query.
     * Matches on: purchase_id, supplier_id, inventory_id, ingredient, and status.
     *
     * @param rows        source rows (row format: [purchase_id, supplier_id, inventory_id, ingredient, order_date, status])
     * @param searchQuery the raw query string (blank = no filter)
     * @return filtered list (same row references, not copies)
     */
    public List<String[]> filterRows(List<String[]> rows, String searchQuery) {
        if (searchQuery == null || searchQuery.isBlank()) return rows;
        String q = searchQuery.trim().toLowerCase();
        List<String[]> filtered = new ArrayList<>();
        for (String[] row : rows) {
            if (row[0].toLowerCase().contains(q) ||   // purchase_id
                row[1].toLowerCase().contains(q) ||   // supplier_id
                row[2].toLowerCase().contains(q) ||   // inventory_id
                row[3].toLowerCase().contains(q) ||   // ingredient
                row[5].toLowerCase().contains(q))     // status
                filtered.add(row);
        }
        return filtered;
    }

    // ══════════════════════════════════════════════════════
    //  EXPORT
    // ══════════════════════════════════════════════════════

    /**
     * Writes the given rows to a CSV file.
     *
     * @param file       destination file (chosen by the user via FileChooser in the UI)
     * @param rows       rows to export
     * @param currentTab used only for a log message
     */
    public void exportToCsv(File file, List<String[]> rows, String currentTab) {
        if (file == null || rows.isEmpty()) return;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("Purchase ID,Supplier ID,Inventory ID,Ingredient,Order Date,Status");
            writer.newLine();
            for (String[] row : rows) {
                writer.write(
                    escapeCsv(row[0]) + "," + escapeCsv(row[1]) + "," +
                    escapeCsv(row[2]) + "," + escapeCsv(row[3]) + "," +
                    escapeCsv(row[4]) + "," + escapeCsv(row[5])
                );
                writer.newLine();
            }
            System.out.println("[PurchasesUtil] exportToCsv: wrote " + rows.size() +
                               " row(s) to " + file.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════

    /** Replaces null DB strings with the em-dash placeholder. */
    private String nvl(String value) {
        return value != null ? value : "—";
    }

    /** Escapes a value for CSV output. */
    private String escapeCsv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n"))
            return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }
}