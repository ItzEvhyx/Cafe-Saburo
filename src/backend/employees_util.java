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

/**
 * employees_util.java
 *
 * Business logic layer for the Employees module.
 * Handles all DB operations (CRUD, archive/restore, soft-delete)
 * and CSV export. No JavaFX imports — purely backend.
 *
 * Row format (String[5]):
 *   [0] employee_id
 *   [1] employee_name
 *   [2] age
 *   [3] role
 *   [4] employment_status
 */
public class employees_util {

    private final Connection conn;

    public employees_util(Connection conn) {
        this.conn = conn;
    }

    // ══════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════

    private boolean connOk() {
        if (conn == null) return false;
        try { return !conn.isClosed(); } catch (Exception e) { return false; }
    }

    private String nvl(String v) {
        return v != null ? v : "—";
    }

    private String escapeCsv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n"))
            return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }

    // ══════════════════════════════════════════════════════
    //  FETCH
    // ══════════════════════════════════════════════════════

    /**
     * Returns all non-deleted employees for the given tab ("active" | "archived"),
     * ordered by employee_name ASC.
     *
     * Each row: [employee_id, employee_name, age, role, employment_status]
     */
    public List<String[]> fetchEmployees(String tab) {
        List<String[]> rows = new ArrayList<>();
        if (!connOk()) return rows;

        String sql =
            "SELECT employee_id, employee_name, age, role, employment_status " +
            "FROM dbo.Employees " +
            "WHERE is_deleted = 0 AND status = ? " +
            "ORDER BY employee_name ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tab);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int    ageVal = rs.getInt("age");
                    String ageStr = rs.wasNull() ? "—" : String.valueOf(ageVal);
                    rows.add(new String[]{
                        nvl(rs.getString("employee_id")),
                        nvl(rs.getString("employee_name")),
                        ageStr,
                        nvl(rs.getString("role")),
                        nvl(rs.getString("employment_status"))
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("[employees_util] fetchEmployees ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return rows;
    }

    // ══════════════════════════════════════════════════════
    //  INSERT
    // ══════════════════════════════════════════════════════

    /**
     * Generates the next sequential employee ID (e.g. "EMP-042").
     * Falls back to a timestamp-based ID if parsing fails.
     */
    public String generateEmployeeId() {
        if (!connOk()) return "EMP-" + System.currentTimeMillis();

        String sql = "SELECT MAX(employee_id) FROM dbo.Employees";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getString(1) != null) {
                String last = rs.getString(1);
                try {
                    int num = Integer.parseInt(last.replace("EMP-", "").trim());
                    return String.format("EMP-%03d", num + 1);
                } catch (NumberFormatException ex) {
                    return "EMP-" + System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
            System.err.println("[employees_util] generateEmployeeId ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return "EMP-001";
    }

    /**
     * Inserts a new employee row with employment_status = 'Active', status = 'active'.
     *
     * @return the generated employee_id on success, null on failure.
     */
    public String insertEmployee(String name, int age, String role) {
        if (!connOk()) return null;

        String id = generateEmployeeId();
        String sql =
            "INSERT INTO dbo.Employees " +
            "(employee_id, employee_name, age, role, employment_status, status, is_deleted) " +
            "VALUES (?, ?, ?, ?, 'Active', 'active', 0)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, name.trim());
            ps.setInt   (3, age);
            ps.setString(4, role);
            ps.executeUpdate();
            System.out.println("[employees_util] insertEmployee: inserted " + id);
            return id;
        } catch (Exception e) {
            System.err.println("[employees_util] insertEmployee ERROR: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ══════════════════════════════════════════════════════
    //  UPDATE
    // ══════════════════════════════════════════════════════

    /**
     * Updates the employment_status column for a single employee.
     * Valid values: Active | Inactive | On Leave | Terminated
     */
    public void updateEmploymentStatus(String employeeId, String newStatus) {
        if (!connOk()) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE dbo.Employees SET employment_status = ? WHERE employee_id = ?")) {
            ps.setString(1, newStatus);
            ps.setString(2, employeeId);
            ps.executeUpdate();
            System.out.println("[employees_util] updateEmploymentStatus: " + employeeId + " → " + newStatus);
        } catch (Exception e) {
            System.err.println("[employees_util] updateEmploymentStatus ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════
    //  ARCHIVE / RESTORE
    // ══════════════════════════════════════════════════════

    /**
     * Sets status = 'archived' for each ID in the given set.
     */
    public void archiveSelected(Set<String> ids) {
        if (!connOk() || ids.isEmpty()) return;
        String sql = "UPDATE dbo.Employees SET status = 'archived' " +
                     "WHERE employee_id = ? AND is_deleted = 0";
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) {
                System.err.println("[employees_util] archiveSelected ERROR on " + id + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Sets status = 'active' for each ID in the given set (restore from archive).
     */
    public void restoreSelected(Set<String> ids) {
        if (!connOk() || ids.isEmpty()) return;
        String sql = "UPDATE dbo.Employees SET status = 'active' " +
                     "WHERE employee_id = ? AND is_deleted = 0";
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) {
                System.err.println("[employees_util] restoreSelected ERROR on " + id + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // ══════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════

    /**
     * Hard-deletes (removes from DB) all non-deleted employees
     * belonging to the given tab ("active" | "archived").
     */
    public void hardDeleteAll(String tab) {
        if (!connOk()) return;
        String sql = "DELETE FROM dbo.Employees WHERE is_deleted = 0 AND status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tab);
            int affected = ps.executeUpdate();
            System.out.println("[employees_util] hardDeleteAll: deleted " + affected + " row(s) from tab='" + tab + "'");
        } catch (Exception e) {
            System.err.println("[employees_util] hardDeleteAll ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════
    //  EXPORT
    // ══════════════════════════════════════════════════════

    /**
     * Writes the given rows to a CSV file.
     * Columns: Employee ID, Employee Name, Age, Role, Employment Status
     *
     * @param file     destination file (already chosen by FileChooser in the UI layer)
     * @param rows     the current cachedRows list from the frontend
     */
    public void exportToCsv(File file, List<String[]> rows) {
        if (file == null || rows.isEmpty()) return;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("Employee ID,Employee Name,Age,Role,Employment Status");
            writer.newLine();
            for (String[] row : rows) {
                writer.write(
                    escapeCsv(row[0]) + "," + escapeCsv(row[1]) + "," +
                    escapeCsv(row[2]) + "," + escapeCsv(row[3]) + "," + escapeCsv(row[4])
                );
                writer.newLine();
            }
            System.out.println("[employees_util] exportToCsv: wrote " + rows.size() + " row(s) to " + file.getPath());
        } catch (Exception e) {
            System.err.println("[employees_util] exportToCsv ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}