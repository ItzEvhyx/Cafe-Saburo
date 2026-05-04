package backend;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * payments_util — Business logic layer for the Payments module.
 *
 * Responsibilities:
 *   - Fetching payment rows via usp_GetPayments
 *   - Archiving / restoring via usp_ArchivePayments / usp_RestorePayments
 *   - Hard-deleting via usp_HardDeleteAll
 *   - Exporting the current view to CSV
 *   - Running all analytics operations via usp_Payment* procedures
 *
 * All database communication uses CallableStatement ({CALL dbo.proc(...)}).
 * No inline SQL strings remain in this class — every query lives in a
 * named stored procedure in the database.
 *
 * Analytics procedures each return two result sets:
 *   RS1 — detail rows  (header derived from ResultSetMetaData)
 *   RS2 — summary row  (footer appended by Java after getMoreResults())
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
    //  PROC NAME MAPPING
    //  Maps each operation key to its stored procedure name.
    // ══════════════════════════════════════════════════════
    private static String procNameFor(String opKey) {
        switch (opKey) {
            case "SUM":         return "usp_PaymentSum";
            case "AVERAGE":     return "usp_PaymentAvg";
            case "COUNT":       return "usp_PaymentCount";
            case "HIGHEST":     return "usp_PaymentHighest";
            case "LOWEST":      return "usp_PaymentLowest";
            case "BY_METHOD":   return "usp_ByMethod";
            case "BY_CUSTOMER": return "usp_ByCustomer";
            case "DAILY":       return "usp_Daily";
            case "ABOVE_AVG":   return "usp_AboveAvg";
            case "NO_PAYMENTS": return "usp_NoPayments";
            default:            return null;
        }
    }

    // ══════════════════════════════════════════════════════
    //  FETCH
    // ══════════════════════════════════════════════════════

    /**
     * Calls usp_GetPayments to fetch payment rows for the given tab.
     *
     * @param conn  Active SQL Server connection.
     * @param tab   "active" | "archived"
     * @return List of String[4]: { payment_id, order_id, payment_method, amount }
     *         Amount is pre-formatted as "X,XXX.XX" (no currency symbol).
     */
    public static List<String[]> fetchPayments(Connection conn, String tab) {
        List<String[]> rows = new ArrayList<>();
        if (!isConnOpen(conn)) return rows;

        try (CallableStatement cs = conn.prepareCall(
                "{CALL dbo.usp_GetPayments(?, ?)}")) {

            cs.setString(1, tab);
            cs.setNull(2, Types.NVARCHAR);   // no search filter on initial load

            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    double raw    = rs.getDouble("amount");
                    String amount = String.format("%,.2f", raw);
                    rows.add(new String[]{
                        nvl(rs.getString("payment_id")),
                        nvl(rs.getString("order_id")),
                        nvl(rs.getString("payment_method")),
                        amount
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    /**
     * Calls usp_GetPayments with a search term to return filtered rows.
     * Used when the search is better delegated to the database (large datasets).
     *
     * @param conn    Active SQL Server connection.
     * @param tab     "active" | "archived"
     * @param search  Partial match string for payment_id / order_id.
     */
    public static List<String[]> fetchPaymentsFiltered(
            Connection conn, String tab, String search) {

        List<String[]> rows = new ArrayList<>();
        if (!isConnOpen(conn)) return rows;

        try (CallableStatement cs = conn.prepareCall(
                "{CALL dbo.usp_GetPayments(?, ?)}")) {

            cs.setString(1, tab);
            if (search == null || search.isBlank()) {
                cs.setNull(2, Types.NVARCHAR);
            } else {
                cs.setString(2, search.trim());
            }

            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    double raw    = rs.getDouble("amount");
                    String amount = String.format("%,.2f", raw);
                    rows.add(new String[]{
                        nvl(rs.getString("payment_id")),
                        nvl(rs.getString("order_id")),
                        nvl(rs.getString("payment_method")),
                        amount
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    // ══════════════════════════════════════════════════════
    //  DB OPERATIONS
    // ══════════════════════════════════════════════════════

    /**
     * Calls usp_ArchivePayments with all selected IDs in a single round trip.
     * The previous implementation looped N PreparedStatement calls.
     *
     * @param conn  Active SQL Server connection.
     * @param ids   Set of payment_id strings to archive.
     */
    public static void archiveSelected(Connection conn, Set<String> ids) {
        if (!isConnOpen(conn) || ids.isEmpty()) return;

        String joined = String.join(",", ids);
        try (CallableStatement cs = conn.prepareCall(
                "{CALL dbo.usp_ArchivePayments(?)}")) {

            cs.setString(1, joined);
            cs.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Calls usp_RestorePayments with all selected IDs in a single round trip.
     *
     * @param conn  Active SQL Server connection.
     * @param ids   Set of payment_id strings to restore to 'active'.
     */
    public static void restoreSelected(Connection conn, Set<String> ids) {
        if (!isConnOpen(conn) || ids.isEmpty()) return;

        String joined = String.join(",", ids);
        try (CallableStatement cs = conn.prepareCall(
                "{CALL dbo.usp_RestorePayments(?)}")) {

            cs.setString(1, joined);
            cs.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Calls usp_HardDeleteAll to permanently remove all non-soft-deleted
     * payments for the given tab. The trg_PreventDeleteActive trigger will
     * raise an error if any active rows are accidentally targeted.
     *
     * @param conn        Active SQL Server connection.
     * @param currentTab  "active" | "archived"
     */
    public static void hardDeleteAll(Connection conn, String currentTab) {
        if (!isConnOpen(conn)) return;

        try (CallableStatement cs = conn.prepareCall(
                "{CALL dbo.usp_HardDeleteAll(?)}")) {

            cs.setString(1, currentTab);
            cs.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════
    //  CSV EXPORT
    // ══════════════════════════════════════════════════════

    /**
     * Opens a save-file dialog and writes the current cached rows to a CSV.
     *
     * @param ownerStage  The JavaFX Stage for the FileChooser dialog (may be null).
     * @param currentTab  "active" | "archived" — used to suggest a filename.
     * @param cachedRows  The rows currently displayed in the table.
     */
    public static void exportCsv(
            Stage ownerStage, String currentTab, List<String[]> cachedRows) {

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
        } catch (Exception e) {
            e.printStackTrace();
        }
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
     * Dispatches to the correct analytics stored procedure by opKey.
     *
     * Each analytics proc returns two result sets:
     *   RS1 — detail rows. The header row is built dynamically from
     *          ResultSetMetaData so the Java code never hardcodes column names.
     *   RS2 — summary/footer row (one row with aggregated values).
     *
     * Returns a List where index 0 is the header row and the last entry
     * is the footer row. Returns an empty list on unknown key or DB error.
     *
     * @param conn   Active SQL Server connection.
     * @param opKey  One of the keys defined in OPERATIONS[][0].
     */
    public static List<String[]> runOperation(Connection conn, String opKey) {
        List<String[]> rows = new ArrayList<>();
        if (!isConnOpen(conn)) return rows;

        String procName = procNameFor(opKey);
        if (procName == null) return rows;

        try (CallableStatement cs = conn.prepareCall(
                "{CALL dbo." + procName + "()}")) {

            boolean hasFirstRS = cs.execute();

            // ── Result set 1: detail rows ─────────────────
            if (hasFirstRS) {
                try (ResultSet rs = cs.getResultSet()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();

                    // Build header row from column labels (defined in proc AS [...])
                    String[] header = new String[cols];
                    for (int c = 1; c <= cols; c++) {
                        header[c - 1] = meta.getColumnLabel(c);
                    }
                    rows.add(header);

                    // Detail data rows
                    while (rs.next()) {
                        String[] row = new String[cols];
                        for (int c = 1; c <= cols; c++) {
                            String val = rs.getString(c);
                            row[c - 1] = (val != null) ? val : "-";
                        }
                        rows.add(row);
                    }
                }

                // ── Result set 2: summary/footer row ─────────
                if (cs.getMoreResults()) {
                    try (ResultSet rs2 = cs.getResultSet()) {
                        if (rs2.next()) {
                            ResultSetMetaData meta2 = rs2.getMetaData();
                            int cols2 = meta2.getColumnCount();

                            // Footer has the same column count as detail rows
                            int detailCols = rows.isEmpty() ? cols2
                                           : rows.get(0).length;
                            String[] footer = new String[detailCols];

                            // Fill from the right (last col = the aggregate value)
                            for (int c = 1; c <= cols2 && c <= detailCols; c++) {
                                String val = rs2.getString(c);
                                footer[detailCols - cols2 + c - 1] =
                                    (val != null) ? val : "-";
                            }

                            // Label in the first cell (e.g. "TOTAL", "AVERAGE")
                            if (detailCols > cols2) {
                                footer[0] = rs2.getMetaData()
                                               .getColumnLabel(1)
                                               .toUpperCase();
                            }

                            rows.add(footer);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Surface the error as the first (and only) data row so the UI
            // can display it inside the analytics result table.
            if (!rows.isEmpty()) {
                int cols = rows.get(0).length;
                String[] errRow = new String[cols];
                errRow[0] = "Error: " + e.getMessage();
                for (int i = 1; i < cols; i++) errRow[i] = "";
                rows.add(errRow);
            } else {
                rows.add(new String[]{ "Error: " + e.getMessage() });
            }
        }

        return rows;
    }

    // ══════════════════════════════════════════════════════
    //  INTERNAL HELPERS
    // ══════════════════════════════════════════════════════

    /** Returns "-" when a nullable DB string is null. */
    private static String nvl(String value) {
        return (value != null) ? value : "-";
    }

    /** Returns true when the connection is non-null and not closed. */
    private static boolean isConnOpen(Connection conn) {
        if (conn == null) return false;
        try { return !conn.isClosed(); } catch (Exception e) { return false; }
    }
}