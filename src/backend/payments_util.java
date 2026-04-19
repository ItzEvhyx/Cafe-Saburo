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
 * payments_util — Business logic layer for the Payments module.
 *
 * Responsibilities:
 *   - Fetching payment rows from the database
 *   - Archiving / restoring / hard-deleting payments
 *   - Exporting the current view to CSV
 *   - Running all analytics aggregate operations
 *
 * This class is intentionally free of any JavaFX UI nodes.
 * payments_contents calls these methods and handles rendering.
 */
public class payments_util {

    // ══════════════════════════════════════════════════════
    //  ANALYTICS OPERATION REGISTRY
    //  Exposed so payments_contents can build pill buttons
    //  without duplicating the list.
    // ══════════════════════════════════════════════════════
    public static final String[][] OPERATIONS = {
        { "SUM",         "Total Revenue"          },
        { "AVERAGE",     "Average Payment"        },
        { "COUNT",       "Transaction Count"      },
        { "HIGHEST",     "Highest Payment"        },
        { "LOWEST",      "Lowest Payment"         },
        { "BY_METHOD",   "By Payment Method"      },
        { "BY_CUSTOMER", "By Customer"            },
        { "DAILY",       "Daily Totals"           },
        { "ABOVE_AVG",   "Above-Average Orders"   },
        { "NO_PAYMENTS", "Customers w/ No Orders" },
    };

    // ══════════════════════════════════════════════════════
    //  FETCH
    // ══════════════════════════════════════════════════════

    /**
     * Fetches payment rows for the given tab ("active" or "archived").
     * Returns a list of String[4]: { payment_id, order_id, payment_method, amount }
     * Amount is pre-formatted as "X,XXX.XX" (no currency symbol).
     */
    public static List<String[]> fetchPayments(Connection conn, String tab) {
        List<String[]> rows = new ArrayList<>();
        if (!isConnOpen(conn)) return rows;

        String sql =
            "SELECT payment_id, order_id, payment_method, amount " +
            "FROM dbo.Payments " +
            "WHERE is_deleted = 0 AND status = ? " +
            "ORDER BY payment_date DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tab);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                double raw    = rs.getDouble("amount");
                String amount = String.format("%,.2f", raw);
                rows.add(new String[]{
                    rs.getString("payment_id")     != null ? rs.getString("payment_id")     : "-",
                    rs.getString("order_id")       != null ? rs.getString("order_id")       : "-",
                    rs.getString("payment_method") != null ? rs.getString("payment_method") : "-",
                    amount
                });
            }
            rs.close();
        } catch (Exception e) { e.printStackTrace(); }
        return rows;
    }

    // ══════════════════════════════════════════════════════
    //  DB OPERATIONS
    // ══════════════════════════════════════════════════════

    /**
     * Sets status = 'archived' for every payment_id in the given set.
     */
    public static void archiveSelected(Connection conn, Set<String> ids) {
        if (!isConnOpen(conn) || ids.isEmpty()) return;
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Payments SET status = 'archived' " +
                    "WHERE payment_id = ? AND is_deleted = 0")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    /**
     * Sets status = 'active' for every payment_id in the given set.
     */
    public static void restoreSelected(Connection conn, Set<String> ids) {
        if (!isConnOpen(conn) || ids.isEmpty()) return;
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Payments SET status = 'active' " +
                    "WHERE payment_id = ? AND is_deleted = 0")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    /**
     * Hard-deletes (permanent) all non-deleted payments for the given tab.
     */
    public static void hardDeleteAll(Connection conn, String currentTab) {
        if (!isConnOpen(conn)) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM dbo.Payments WHERE is_deleted = 0 AND status = ?")) {
            ps.setString(1, currentTab);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════
    //  CSV EXPORT
    // ══════════════════════════════════════════════════════

    /**
     * Opens a save-file dialog and writes the current cached rows to a CSV.
     *
     * @param ownerStage  The JavaFX Stage for the FileChooser dialog (may be null).
     * @param currentTab  "active" or "archived" — used to suggest a filename.
     * @param cachedRows  The rows currently displayed in the table.
     */
    public static void exportCsv(Stage ownerStage, String currentTab, List<String[]> cachedRows) {
        if (cachedRows.isEmpty()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Payment History as CSV");
        chooser.setInitialFileName("payments_" + currentTab + ".csv");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File file = (ownerStage != null)
            ? chooser.showSaveDialog(ownerStage)
            : chooser.showSaveDialog(null);
        if (file == null) return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("Payment ID,Order ID,Payment Method,Amount");
            writer.newLine();
            for (String[] row : cachedRows) {
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

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n"))
            return "\"" + value.replace("\"", "\"\"") + "\"";
        return value;
    }

    // ══════════════════════════════════════════════════════
    //  ANALYTICS DISPATCHER
    // ══════════════════════════════════════════════════════

    /**
     * Runs the analytics query identified by opKey.
     * Always returns a List where index 0 is the header row and the last row
     * is a summary/footer row. Returns an empty list on unknown key.
     */
    public static List<String[]> runOperation(Connection conn, String opKey) {
        switch (opKey) {
            case "SUM":         return opSum(conn);
            case "AVERAGE":     return opAverage(conn);
            case "COUNT":       return opCount(conn);
            case "HIGHEST":     return opHighest(conn);
            case "LOWEST":      return opLowest(conn);
            case "BY_METHOD":   return opByMethod(conn);
            case "BY_CUSTOMER": return opByCustomer(conn);
            case "DAILY":       return opDaily(conn);
            case "ABOVE_AVG":   return opAboveAverage(conn);
            case "NO_PAYMENTS": return opNoPayments(conn);
            default:            return new ArrayList<>();
        }
    }

    // ══════════════════════════════════════════════════════
    //  ANALYTICS OPERATIONS
    // ══════════════════════════════════════════════════════

    // Q1 — Total Revenue
    private static List<String[]> opSum(Connection conn) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Payment ID", "Order ID", "Method", "Amount"});
        if (!isConnOpen(conn)) return rows;

        String sql =
            "SELECT payment_id, order_id, payment_method, amount " +
            "FROM   dbo.Payments " +
            "WHERE  is_deleted = 0 AND status = 'active' " +
            "ORDER  BY payment_date DESC";
        double total = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double amt = rs.getDouble("amount");
                total += amt;
                rows.add(new String[]{
                    rs.getString("payment_id"),
                    rs.getString("order_id"),
                    rs.getString("payment_method"),
                    String.format("%,.2f", amt)
                });
            }
        } catch (Exception e) {
            rows.add(new String[]{"Error: " + e.getMessage(), "", "", ""}); return rows;
        }
        rows.add(new String[]{"TOTAL", "", "", String.format("%,.2f", total)});
        return rows;
    }

    // Q2 — Average Payment
    private static List<String[]> opAverage(Connection conn) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Payment ID", "Order ID", "Method", "Amount"});
        if (!isConnOpen(conn)) return rows;

        double avg = 0;
        String avgSql =
            "SELECT ROUND(AVG(amount), 2) AS avg_amount " +
            "FROM   dbo.Payments " +
            "WHERE  is_deleted = 0 AND status = 'active'";
        try (PreparedStatement ps2 = conn.prepareStatement(avgSql);
             ResultSet rs2 = ps2.executeQuery()) {
            if (rs2.next()) avg = rs2.getDouble("avg_amount");
        } catch (Exception ignored) {}

        String sql =
            "SELECT payment_id, order_id, payment_method, amount " +
            "FROM   dbo.Payments " +
            "WHERE  is_deleted = 0 AND status = 'active' " +
            "ORDER  BY payment_date DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("payment_id"),
                    rs.getString("order_id"),
                    rs.getString("payment_method"),
                    String.format("%,.2f", rs.getDouble("amount"))
                });
            }
        } catch (Exception e) {
            rows.add(new String[]{"Error: " + e.getMessage(), "", "", ""}); return rows;
        }
        rows.add(new String[]{"AVERAGE", "", "", String.format("%,.2f", avg)});
        return rows;
    }

    // Q3 — Transaction Count
    private static List<String[]> opCount(Connection conn) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Payment Method", "Transactions", "Volume"});
        if (!isConnOpen(conn)) return rows;

        String sql =
            "SELECT payment_method, " +
            "       COUNT(payment_id) AS total_transactions, " +
            "       CASE WHEN COUNT(payment_id) >= 10 THEN 'High' " +
            "            WHEN COUNT(payment_id) >= 5  THEN 'Medium' " +
            "            ELSE 'Low' END AS volume " +
            "FROM   dbo.Payments " +
            "WHERE  is_deleted = 0 AND status = 'active' " +
            "GROUP  BY payment_method " +
            "HAVING COUNT(payment_id) >= 1 " +
            "ORDER  BY COUNT(payment_id) DESC";
        int grandCount = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int cnt = rs.getInt("total_transactions");
                grandCount += cnt;
                rows.add(new String[]{
                    rs.getString("payment_method"),
                    String.valueOf(cnt),
                    rs.getString("volume")
                });
            }
        } catch (Exception e) {
            rows.add(new String[]{"Error: " + e.getMessage(), "", ""}); return rows;
        }
        rows.add(new String[]{"TOTAL", String.valueOf(grandCount), ""});
        return rows;
    }

    // Q4 — Highest Payment
    private static List<String[]> opHighest(Connection conn) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Payment ID", "Customer", "Method", "Amount"});
        if (!isConnOpen(conn)) return rows;

        String sql =
            "SELECT TOP 10 p.payment_id, c.customer_name, p.payment_method, p.amount " +
            "FROM   dbo.Payments  AS p " +
            "INNER JOIN dbo.Orders    AS o ON p.order_id    = o.order_id " +
            "INNER JOIN dbo.Customers AS c ON o.customer_id = c.customer_id " +
            "WHERE  p.is_deleted = 0 AND p.status = 'active' AND o.is_deleted = 0 " +
            "ORDER  BY p.amount DESC";
        double max = 0;
        boolean first = true;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double amt = rs.getDouble("amount");
                if (first) { max = amt; first = false; }
                rows.add(new String[]{
                    rs.getString("payment_id"),
                    rs.getString("customer_name"),
                    rs.getString("payment_method"),
                    String.format("%,.2f", amt)
                });
            }
        } catch (Exception e) {
            rows.add(new String[]{"Error: " + e.getMessage(), "", "", ""}); return rows;
        }
        rows.add(new String[]{"HIGHEST", "", "", String.format("%,.2f", max)});
        return rows;
    }

    // Q5 — Lowest Payment
    private static List<String[]> opLowest(Connection conn) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Payment ID", "Customer", "Method", "Amount"});
        if (!isConnOpen(conn)) return rows;

        String sql =
            "SELECT TOP 10 p.payment_id, c.customer_name, p.payment_method, p.amount " +
            "FROM   dbo.Payments  AS p " +
            "INNER JOIN dbo.Orders    AS o ON p.order_id    = o.order_id " +
            "INNER JOIN dbo.Customers AS c ON o.customer_id = c.customer_id " +
            "WHERE  p.is_deleted = 0 AND p.status = 'active' AND o.is_deleted = 0 " +
            "ORDER  BY p.amount ASC";
        double min = Double.MAX_VALUE;
        boolean first = true;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double amt = rs.getDouble("amount");
                if (first) { min = amt; first = false; }
                rows.add(new String[]{
                    rs.getString("payment_id"),
                    rs.getString("customer_name"),
                    rs.getString("payment_method"),
                    String.format("%,.2f", amt)
                });
            }
        } catch (Exception e) {
            rows.add(new String[]{"Error: " + e.getMessage(), "", "", ""}); return rows;
        }
        if (min == Double.MAX_VALUE) min = 0;
        rows.add(new String[]{"LOWEST", "", "", String.format("%,.2f", min)});
        return rows;
    }

    // Q6 — By Payment Method
    private static List<String[]> opByMethod(Connection conn) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Method", "Transactions", "Total", "Average", "Lowest", "Highest"});
        if (!isConnOpen(conn)) return rows;

        String sql =
            "SELECT payment_method, " +
            "       COUNT(payment_id)      AS transactions, " +
            "       SUM(amount)            AS total, " +
            "       ROUND(AVG(amount), 2)  AS average, " +
            "       MIN(amount)            AS lowest, " +
            "       MAX(amount)            AS highest " +
            "FROM   dbo.Payments " +
            "WHERE  is_deleted = 0 " +
            "  AND  (status = 'active' OR status = 'archived') " +
            "GROUP  BY payment_method " +
            "HAVING COUNT(payment_id) >= 1 " +
            "ORDER  BY SUM(amount) DESC";
        double grandTotal = 0;
        int    grandCount = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double t = rs.getDouble("total");
                grandTotal += t;
                grandCount += rs.getInt("transactions");
                rows.add(new String[]{
                    rs.getString("payment_method"),
                    String.valueOf(rs.getInt("transactions")),
                    String.format("%,.2f", t),
                    String.format("%,.2f", rs.getDouble("average")),
                    String.format("%,.2f", rs.getDouble("lowest")),
                    String.format("%,.2f", rs.getDouble("highest"))
                });
            }
        } catch (Exception e) {
            rows.add(new String[]{"Error: " + e.getMessage(), "", "", "", "", ""}); return rows;
        }
        rows.add(new String[]{"TOTAL", String.valueOf(grandCount),
            String.format("%,.2f", grandTotal), "", "", ""});
        return rows;
    }

    // Q7 — By Customer (above-average spenders only)
    private static List<String[]> opByCustomer(Connection conn) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Customer", "Payments", "Total Spent"});
        if (!isConnOpen(conn)) return rows;

        String sql =
            "SELECT c.customer_name, " +
            "       COUNT(p.payment_id) AS payments, " +
            "       SUM(p.amount)       AS total_spent " +
            "FROM   dbo.Payments  AS p " +
            "INNER JOIN dbo.Orders    AS o  ON p.order_id    = o.order_id " +
            "INNER JOIN dbo.Customers AS c  ON o.customer_id = c.customer_id " +
            "WHERE  p.is_deleted = 0 AND p.status = 'active' AND o.is_deleted = 0 " +
            "GROUP  BY c.customer_name " +
            "HAVING SUM(p.amount) > (" +
            "    SELECT AVG(sub_total) FROM (" +
            "        SELECT SUM(p2.amount) AS sub_total " +
            "        FROM   dbo.Payments AS p2 " +
            "        INNER JOIN dbo.Orders AS o2 ON p2.order_id = o2.order_id " +
            "        WHERE  p2.is_deleted = 0 " +
            "        GROUP  BY o2.customer_id" +
            "    ) AS sub" +
            ") " +
            "ORDER  BY total_spent DESC";
        double grandTotal = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double t = rs.getDouble("total_spent");
                grandTotal += t;
                rows.add(new String[]{
                    rs.getString("customer_name"),
                    String.valueOf(rs.getInt("payments")),
                    String.format("%,.2f", t)
                });
            }
        } catch (Exception e) {
            rows.add(new String[]{"Error: " + e.getMessage(), "", ""}); return rows;
        }
        rows.add(new String[]{"TOTAL", "", String.format("%,.2f", grandTotal)});
        return rows;
    }

    // Q8 — Daily Totals
    private static List<String[]> opDaily(Connection conn) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Date", "Transactions", "Daily Total", "Daily Avg"});
        if (!isConnOpen(conn)) return rows;

        String sql =
            "SELECT p.payment_date, " +
            "       COUNT(p.payment_id)    AS transactions, " +
            "       SUM(p.amount)          AS daily_total, " +
            "       ROUND(AVG(p.amount),2) AS daily_avg " +
            "FROM   dbo.Payments AS p " +
            "INNER JOIN dbo.Orders AS o ON p.order_id = o.order_id " +
            "WHERE  p.is_deleted = 0 AND p.status = 'active' AND o.is_deleted = 0 " +
            "GROUP  BY p.payment_date " +
            "ORDER  BY p.payment_date DESC";
        double grandTotal = 0;
        int    grandCount = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double dt = rs.getDouble("daily_total");
                grandTotal += dt;
                grandCount += rs.getInt("transactions");
                rows.add(new String[]{
                    rs.getString("payment_date") != null ? rs.getString("payment_date") : "-",
                    String.valueOf(rs.getInt("transactions")),
                    String.format("%,.2f", dt),
                    String.format("%,.2f", rs.getDouble("daily_avg"))
                });
            }
        } catch (Exception e) {
            rows.add(new String[]{"Error: " + e.getMessage(), "", "", ""}); return rows;
        }
        rows.add(new String[]{"TOTAL", String.valueOf(grandCount),
            String.format("%,.2f", grandTotal), ""});
        return rows;
    }

    // Q9 — Above-Average Orders
    private static List<String[]> opAboveAverage(Connection conn) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Payment ID", "Customer", "Method", "Amount"});
        if (!isConnOpen(conn)) return rows;

        String sql =
            "SELECT p.payment_id, c.customer_name, p.payment_method, p.amount " +
            "FROM   dbo.Payments  AS p " +
            "INNER JOIN dbo.Orders    AS o ON p.order_id    = o.order_id " +
            "INNER JOIN dbo.Customers AS c ON o.customer_id = c.customer_id " +
            "WHERE  p.is_deleted = 0 AND p.status = 'active' AND o.is_deleted = 0 " +
            "  AND  p.amount > (SELECT AVG(amount) FROM dbo.Payments " +
            "                   WHERE is_deleted = 0 AND status = 'active') " +
            "ORDER  BY p.amount DESC";
        double total = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double amt = rs.getDouble("amount");
                total += amt;
                rows.add(new String[]{
                    rs.getString("payment_id"),
                    rs.getString("customer_name"),
                    rs.getString("payment_method"),
                    String.format("%,.2f", amt)
                });
            }
        } catch (Exception e) {
            rows.add(new String[]{"Error: " + e.getMessage(), "", "", ""}); return rows;
        }
        rows.add(new String[]{"TOTAL", "", "", String.format("%,.2f", total)});
        return rows;
    }

    // Q10 — Customers with No Orders/Payments
    private static List<String[]> opNoPayments(Connection conn) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Customer ID", "Name", "Email"});
        if (!isConnOpen(conn)) return rows;

        String sql =
            "SELECT customer_id, customer_name, customer_email " +
            "FROM   dbo.Customers " +
            "WHERE  is_deleted = 0 " +
            "  AND  (customer_id NOT IN ( " +
            "            SELECT o.customer_id " +
            "            FROM   dbo.Orders    AS o " +
            "            INNER JOIN dbo.Payments AS p ON o.order_id = p.order_id " +
            "            WHERE  o.is_deleted = 0 AND p.is_deleted = 0 " +
            "        ) OR customer_name IS NULL) " +
            "ORDER  BY customer_name ASC";
        int count = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                count++;
                rows.add(new String[]{
                    rs.getString("customer_id")    != null ? rs.getString("customer_id")    : "-",
                    rs.getString("customer_name")  != null ? rs.getString("customer_name")  : "(none)",
                    rs.getString("customer_email") != null ? rs.getString("customer_email") : "-"
                });
            }
        } catch (Exception e) {
            rows.add(new String[]{"Error: " + e.getMessage(), "", ""}); return rows;
        }
        rows.add(new String[]{"COUNT", String.valueOf(count), ""});
        return rows;
    }

    // ══════════════════════════════════════════════════════
    //  INTERNAL HELPER
    // ══════════════════════════════════════════════════════
    private static boolean isConnOpen(Connection conn) {
        if (conn == null) return false;
        try { return !conn.isClosed(); } catch (Exception e) { return false; }
    }
}