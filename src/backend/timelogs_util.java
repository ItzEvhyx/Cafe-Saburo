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
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class timelogs_util {

    // ══════════════════════════════════════════════════════
    //  CONSTANTS
    // ══════════════════════════════════════════════════════
    public static final String FONT_FAMILY = "Aleo";
    private static final SimpleDateFormat DISPLAY_FMT = new SimpleDateFormat("MMM dd, yyyy hh:mm a");

    // ══════════════════════════════════════════════════════
    //  FONT LOADING
    // ══════════════════════════════════════════════════════
    private static boolean fontsLoaded = false;

    public static void loadFonts() {
        if (fontsLoaded) return;
        String[] variants = {
            "Aleo-Black", "Aleo-BlackItalic", "Aleo-Bold", "Aleo-BoldItalic",
            "Aleo-ExtraBold", "Aleo-ExtraBoldItalic", "Aleo-ExtraLight", "Aleo-ExtraLightItalic",
            "Aleo-Italic", "Aleo-Light", "Aleo-LightItalic", "Aleo-Medium", "Aleo-MediumItalic",
            "Aleo-Regular", "Aleo-SemiBold", "Aleo-SemiBoldItalic", "Aleo-Thin", "Aleo-ThinItalic"
        };
        for (String v : variants) Font.loadFont("file:assets/fonts/" + v + ".ttf", 12);
        fontsLoaded = true;
    }

    // ══════════════════════════════════════════════════════
    //  ROW FILTERING
    // ══════════════════════════════════════════════════════
    /**
     * Filters cached rows by name or employee ID based on the given search query.
     * Returns the full list unchanged if the query is null or blank.
     */
    public static List<String[]> getFilteredRows(List<String[]> cachedRows, String searchQuery) {
        if (searchQuery == null || searchQuery.isBlank()) return cachedRows;
        String q = searchQuery.trim().toLowerCase();
        List<String[]> filtered = new ArrayList<>();
        for (String[] row : cachedRows) {
            if (row[2].toLowerCase().contains(q) || row[1].toLowerCase().contains(q))
                filtered.add(row);
        }
        return filtered;
    }

    // ══════════════════════════════════════════════════════
    //  PREPEND LOG (data-side only)
    // ══════════════════════════════════════════════════════
    /**
     * Builds a new row array from the given fields and inserts it at index 0
     * of the provided list. Returns the same list for convenience.
     */
    public static List<String[]> prependLog(List<String[]> cachedRows,
                                             String logId, String employeeId,
                                             String employeeName, String timeIn, String timeOut) {
        String[] newRow = new String[]{
            logId,
            employeeId,
            employeeName != null ? employeeName : "--",
            timeIn       != null ? timeIn       : "--",
            timeOut      != null ? timeOut       : "--"
        };
        cachedRows.add(0, newRow);
        return cachedRows;
    }

    // ══════════════════════════════════════════════════════
    //  DB OPERATIONS
    // ══════════════════════════════════════════════════════
    public static List<String[]> fetchLogs(Connection conn, String tab) {
        List<String[]> rows = new ArrayList<>();
        if (conn == null) return rows;
        try { if (conn.isClosed()) return rows; } catch (Exception e) { return rows; }

        String sql =
            "SELECT t.log_id, t.employee_id, " +
            "       COALESCE(e.employee_name, t.employee_name) AS employee_name, " +
            "       t.time_in, t.time_out " +
            "FROM dbo.TimeLogs t " +
            "LEFT JOIN dbo.Employees e ON e.employee_id = t.employee_id " +
            "WHERE t.is_deleted = 0 AND t.status = ? " +
            "ORDER BY t.time_in DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tab);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Timestamp timeIn  = rs.getTimestamp("time_in");
                Timestamp timeOut = rs.getTimestamp("time_out");
                rows.add(new String[]{
                    rs.getString("log_id")        != null ? rs.getString("log_id")        : "—",
                    rs.getString("employee_id")   != null ? rs.getString("employee_id")   : "—",
                    rs.getString("employee_name") != null ? rs.getString("employee_name") : "—",
                    timeIn  != null ? DISPLAY_FMT.format(timeIn)  : "—",
                    timeOut != null ? DISPLAY_FMT.format(timeOut) : "Still in"
                });
            }
            rs.close();
        } catch (Exception e) { e.printStackTrace(); }
        return rows;
    }

    public static void archiveSelected(Connection conn, Iterable<String> ids) {
        if (conn == null) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.TimeLogs SET status = 'archived' WHERE log_id = ? AND is_deleted = 0")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public static void restoreSelected(Connection conn, Iterable<String> ids) {
        if (conn == null) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.TimeLogs SET status = 'active' WHERE log_id = ? AND is_deleted = 0")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public static void hardDeleteAll(Connection conn, String currentTab) {
        if (conn == null) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM dbo.TimeLogs WHERE is_deleted = 0 AND status = ?")) {
            ps.setString(1, currentTab);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════
    //  CSV EXPORT
    // ══════════════════════════════════════════════════════
    /**
     * Opens a save dialog and writes the cached rows to a CSV file.
     *
     * @param cachedRows  The rows currently displayed in the table.
     * @param currentTab  Used to build the suggested file name.
     * @param ownerStage  The JavaFX stage to parent the dialog to (may be null).
     */
    public static void exportCsv(List<String[]> cachedRows, String currentTab, Stage ownerStage) {
        if (cachedRows.isEmpty()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Time Logs as CSV");
        chooser.setInitialFileName("timelogs_" + currentTab + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = (ownerStage != null)
                ? chooser.showSaveDialog(ownerStage)
                : chooser.showSaveDialog(null);
        if (file == null) return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("Log ID,Employee ID,Employee Name,Time In,Time Out");
            writer.newLine();
            for (String[] row : cachedRows) {
                writer.write(
                    escapeCsv(row[0]) + "," +
                    escapeCsv(row[1]) + "," +
                    escapeCsv(row[2]) + "," +
                    escapeCsv(row[3]) + "," +
                    escapeCsv(row[4])
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
}