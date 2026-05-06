package backend;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class order_util {

    private final Connection conn;

    // ── State ─────────────────────────────────────────────
    private List<String[]> cachedRows  = new ArrayList<>();
    private String         currentTab  = "active";
    private String         searchQuery = "";

    public order_util(Connection conn) {
        this.conn = conn;
    }

    // ══════════════════════════════════════════════════════
    //  STATE ACCESSORS / MUTATORS
    // ══════════════════════════════════════════════════════

    public List<String[]> getCachedRows()              { return cachedRows; }
    public void           setCachedRows(List<String[]> rows) { cachedRows = rows; }
    public String         getCurrentTab()              { return currentTab; }
    public void           setCurrentTab(String tab)   { currentTab = tab; }
    public String         getSearchQuery()             { return searchQuery; }
    public void           setSearchQuery(String q)    { searchQuery = q == null ? "" : q; }

    // ══════════════════════════════════════════════════════
    //  DB FETCH
    // ══════════════════════════════════════════════════════

    /**
     * Fetches all orders for the given tab ("active" or "archived").
     * Returns rows as: [orderId, customerId, customerName, orderStatus, paymentType]
     */
    public List<String[]> fetchOrders(String tab) {
        List<String[]> rows = new ArrayList<>();
        if (conn == null) return rows;
        try { if (conn.isClosed()) return rows; } catch (Exception e) { return rows; }

        String sql =
            "SELECT o.order_id, o.customer_id, " +
            "       COALESCE(c.customer_name, '—') AS customer_name, " +
            "       o.order_status, o.payment_type " +
            "FROM dbo.Orders AS o " +
            "LEFT JOIN dbo.Customers AS c ON o.customer_id = c.customer_id " +
            "WHERE o.is_deleted = 0 AND o.status = ? " +
            "ORDER BY o.order_date DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tab);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("order_id")      != null ? rs.getString("order_id")      : "—",
                    rs.getString("customer_id")   != null ? rs.getString("customer_id")   : "—",
                    rs.getString("customer_name") != null ? rs.getString("customer_name") : "—",
                    rs.getString("order_status")  != null ? rs.getString("order_status")  : "—",
                    rs.getString("payment_type")  != null ? rs.getString("payment_type")  : "—"
                });
            }
            rs.close();
        } catch (Exception e) { e.printStackTrace(); }
        return rows;
    }

    /**
     * Fetches a single customer's name by their ID.
     * Returns "—" if not found or on error.
     */
    public String fetchCustomerName(String customerId) {
        if (conn == null || customerId == null) return "—";
        try { if (conn.isClosed()) return "—"; } catch (Exception e) { return "—"; }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT customer_name FROM dbo.Customers WHERE customer_id = ? AND is_deleted = 0")) {
            ps.setString(1, customerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String name = rs.getString("customer_name");
                rs.close();
                return name != null ? name : "—";
            }
            rs.close();
        } catch (Exception e) { e.printStackTrace(); }
        return "—";
    }

    // ══════════════════════════════════════════════════════
    //  FILTERING
    // ══════════════════════════════════════════════════════

    /**
     * Returns cachedRows filtered by the current searchQuery.
     * Searches across customer name (index 2) and order ID (index 0).
     */
    public List<String[]> getFilteredRows() {
        if (searchQuery == null || searchQuery.isBlank()) return cachedRows;
        String q = searchQuery.trim().toLowerCase();
        List<String[]> filtered = new ArrayList<>();
        for (String[] row : cachedRows) {
            if (row[2].toLowerCase().contains(q) || row[0].toLowerCase().contains(q))
                filtered.add(row);
        }
        return filtered;
    }

    // ══════════════════════════════════════════════════════
    //  ORDER MUTATIONS
    // ══════════════════════════════════════════════════════

    /** Updates the order_status column for a single order. */
    public void updateOrderStatus(String orderId, String newStatus) {
        if (conn == null) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE dbo.Orders SET order_status = ? WHERE order_id = ?")) {
            ps.setString(1, newStatus);
            ps.setString(2, orderId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    /** Moves the given order IDs to status = 'archived'. */
    public void archiveSelected(Set<String> ids) {
        if (conn == null || ids.isEmpty()) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Orders SET status = 'archived' WHERE order_id = ? AND is_deleted = 0")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    /** Moves the given order IDs back to status = 'active'. */
    public void restoreSelected(Set<String> ids) {
        if (conn == null || ids.isEmpty()) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Orders SET status = 'active' WHERE order_id = ? AND is_deleted = 0")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    /**
     * Permanently deletes all orders matching is_deleted = 0
     * and the current tab's status value.
     */
    public void hardDeleteAll() {
        if (conn == null) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM dbo.Orders WHERE is_deleted = 0 AND status = ?")) {
            ps.setString(1, currentTab);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════
    //  EXPORT
    // ══════════════════════════════════════════════════════

    /**
     * Exports the currently filtered rows to a CSV file.
     * Returns true on success, false if rows are empty or writing fails.
     */
    public boolean exportCsv(File file) {
        List<String[]> rows = getFilteredRows();
        if (rows.isEmpty() || file == null) return false;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("Order ID,Customer ID,Customer Name,Order Status,Payment Type");
            writer.newLine();
            for (String[] row : rows) {
                writer.write(
                    escapeCsv(row[0]) + "," +
                    escapeCsv(row[1]) + "," +
                    escapeCsv(row[2]) + "," +
                    escapeCsv(row[3]) + "," +
                    escapeCsv(row[4])
                );
                writer.newLine();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Escapes a value for safe CSV output. */
    public String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n"))
            return "\"" + value.replace("\"", "\"\"") + "\"";
        return value;
    }

    // ══════════════════════════════════════════════════════
    //  LIVE PREPEND HELPER
    // ══════════════════════════════════════════════════════

    /**
     * Builds a new order row array for live-prepending into cachedRows.
     * Looks up the customer name from DB automatically.
     * Returns: [orderId, customerId, customerName, "Pending", paymentMethod]
     */
    public String[] buildNewOrderRow(String orderId, String customerId, String paymentMethod) {
        String customerName = fetchCustomerName(customerId);
        return new String[]{
            orderId,
            customerId,
            customerName,
            "Pending",
            paymentMethod != null ? paymentMethod : "Cash"
        };
    }
}