package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

public class auth_util {

    // ══════════════════════════════════════════════════════
    //  ROLE ENUM
    // ══════════════════════════════════════════════════════
    public enum UserRole {
        EMPLOYEE,
        MANAGER
    }

    // ── Hardcoded manager credentials ────────────────────
    private static final String MANAGER_NAME     = "Evhy Suba";
    private static final String MANAGER_PASSWORD = "Admin@12345";

    // ── Formatters ────────────────────────────────────────
    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_TIME_FMT =
        DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

    // ── Session state ─────────────────────────────────────
    private static UserRole      currentRole  = null;
    private static String        currentName  = null;
    private static String        currentId    = null;   // employee_id from DB (null for manager)
    private static String        currentLogId = null;   // active TimeLogs row (null for manager)
    private static LocalDateTime shiftStart   = null;

    // ── DB connection (injected once at app start) ────────
    private static Connection conn = null;

    /** Call once from Main.java or auth_ui constructor. */
    public static void setConnection(Connection c) { conn = c; }

    // ══════════════════════════════════════════════════════
    //  ROLE / SESSION QUERIES
    // ══════════════════════════════════════════════════════
    public static UserRole  getCurrentRole() { return currentRole; }
    public static boolean   isManager()      { return currentRole == UserRole.MANAGER; }
    public static boolean   isEmployee()     { return currentRole == UserRole.EMPLOYEE; }

    public static String    getCurrentName() { return currentName != null ? currentName : ""; }
    public static String    getCurrentId()   { return currentId   != null ? currentId   : ""; }
    public static String    getCurrentLogId(){ return currentLogId != null ? currentLogId : ""; }
    public static LocalDateTime getShiftStart() { return shiftStart; }

    public static String getTimeInFormatted() {
        return shiftStart != null ? shiftStart.format(TIME_FMT) : "--";
    }
    public static String getShiftStartFullFormatted() {
        return shiftStart != null ? shiftStart.format(DATE_TIME_FMT) : "--";
    }
    public static String getHoursWorked() {
        if (shiftStart == null) return "--";
        Duration d = Duration.between(shiftStart, LocalDateTime.now());
        long h = d.toHours(), m = d.toMinutesPart(), s = d.toSecondsPart();
        if (h > 0)  return h + "h " + m + "m " + s + "s";
        if (m > 0)  return m + "m " + s + "s";
        return s + "s";
    }
    public static Duration getElapsedDuration() {
        if (shiftStart == null) return Duration.ZERO;
        return Duration.between(shiftStart, LocalDateTime.now());
    }

    // ══════════════════════════════════════════════════════
    //  MANAGER AUTHENTICATION  — hardcoded, no DB lookup
    // ══════════════════════════════════════════════════════

    /**
     * Validates against the single hardcoded manager account (Evhy Suba).
     * On success sets role to MANAGER. Manager sessions do NOT open a TimeLogs row.
     */
    public static AuthResult authenticateManager(String name, String password) {
        if (name == null || name.trim().isEmpty())
            return AuthResult.fail("Please enter your name.");
        if (password == null || password.isEmpty())
            return AuthResult.fail("Please enter your password.");

        boolean ok = name.trim().equalsIgnoreCase(MANAGER_NAME)
                  && password.equals(MANAGER_PASSWORD);

        if (!ok) return AuthResult.fail("Invalid credentials.");

        currentRole  = UserRole.MANAGER;
        currentName  = MANAGER_NAME;
        currentId    = null;
        currentLogId = null;
        shiftStart   = LocalDateTime.now();

        System.out.println("[MANAGER LOGIN] Name : " + currentName);
        System.out.println("[MANAGER LOGIN] Time : " + getShiftStartFullFormatted());

        return AuthResult.ok(MANAGER_NAME);
    }

    // ══════════════════════════════════════════════════════
    //  EMPLOYEE AUTHENTICATION  — DB-driven, no hardcoded names
    // ══════════════════════════════════════════════════════

    /**
     * Looks up the employee by name in dbo.Employees (case-insensitive).
     * Blocks Terminated employees and double clock-ins.
     * On success: inserts an open TimeLogs row and caches session.
     */
    public static AuthResult authenticateEmployee(String name) {
        if (name == null || name.trim().isEmpty())
            return AuthResult.fail("Please enter your name.");
        if (conn == null)
            return AuthResult.fail("No database connection.");
        try {
            if (conn.isClosed()) return AuthResult.fail("Database connection is closed.");
        } catch (Exception e) { return AuthResult.fail("Database error."); }

        String sql =
            "SELECT employee_id, employee_name, employment_status " +
            "FROM dbo.Employees " +
            "WHERE LOWER(LTRIM(RTRIM(employee_name))) = LOWER(LTRIM(RTRIM(?))) " +
            "  AND is_deleted = 0 AND status = 'active'";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                rs.close();
                return AuthResult.fail("Employee not found. Check your name.");
            }

            String empId     = rs.getString("employee_id");
            String empName   = rs.getString("employee_name");
            String empStatus = rs.getString("employment_status");
            rs.close();

            if ("Terminated".equalsIgnoreCase(empStatus))
                return AuthResult.fail("Access denied. This employee has been terminated.");

            // ── Block double clock-in ─────────────────────
            try (PreparedStatement cp = conn.prepareStatement(
                    "SELECT COUNT(*) FROM dbo.TimeLogs " +
                    "WHERE employee_id = ? AND time_out IS NULL AND is_deleted = 0")) {
                cp.setString(1, empId);
                ResultSet cr = cp.executeQuery();
                cr.next();
                int open = cr.getInt(1);
                cr.close();
                if (open > 0) return AuthResult.fail("You are already clocked in.");
            }

            // ── Open a new TimeLogs row ───────────────────
            String logId = generateLogId();
            try (PreparedStatement ip = conn.prepareStatement(
                    "INSERT INTO dbo.TimeLogs " +
                    "(log_id, employee_id, employee_name, time_in, time_out, status, is_deleted) " +
                    "VALUES (?, ?, ?, GETDATE(), NULL, 'active', 0)")) {
                ip.setString(1, logId);
                ip.setString(2, empId);
                ip.setString(3, empName);
                ip.executeUpdate();
            }

            // ── Cache session ─────────────────────────────
            currentRole  = UserRole.EMPLOYEE;
            currentName  = empName;
            currentId    = empId;
            currentLogId = logId;
            shiftStart   = LocalDateTime.now();

            System.out.println("[TIME-IN] Name    : " + currentName);
            System.out.println("[TIME-IN] ID      : " + currentId);
            System.out.println("[TIME-IN] Log ID  : " + currentLogId);
            System.out.println("[TIME-IN] Time In : " + getShiftStartFullFormatted());

            return AuthResult.ok(empName);

        } catch (Exception e) {
            e.printStackTrace();
            return AuthResult.fail("Database error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    //  TIME-OUT
    // ══════════════════════════════════════════════════════

    /**
     * Stamps time_out on the open TimeLogs row, then clears session.
     * No-op for manager sessions (returns false gracefully).
     */
    public static boolean timeOut() {
        if (currentLogId == null || conn == null) return false;
        try { if (conn.isClosed()) return false; } catch (Exception e) { return false; }

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE dbo.TimeLogs SET time_out = GETDATE() WHERE log_id = ?")) {
            ps.setString(1, currentLogId);
            int updated = ps.executeUpdate();
            System.out.println("[TIME-OUT] Log ID : " + currentLogId);
            System.out.println("[TIME-OUT] Name   : " + currentName);
            logout();
            return updated > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ══════════════════════════════════════════════════════
    //  LOGOUT
    // ══════════════════════════════════════════════════════

    /**
     * Clears all session state.
     * Employees: call timeOut() instead so the DB row is stamped.
     * Manager: call this directly.
     */
    public static void logout() {
        currentRole  = null;
        currentName  = null;
        currentId    = null;
        currentLogId = null;
        shiftStart   = null;
    }

    // ══════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════

    private static String generateLogId() {
        if (conn == null) return "LOG-" + System.currentTimeMillis();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT MAX(log_id) FROM dbo.TimeLogs")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getString(1) != null) {
                String last = rs.getString(1);
                rs.close();
                try {
                    int num = Integer.parseInt(last.replace("LOG-", "").trim());
                    return String.format("LOG-%04d", num + 1);
                } catch (NumberFormatException e) {
                    return "LOG-" + System.currentTimeMillis();
                }
            }
            rs.close();
        } catch (Exception e) { e.printStackTrace(); }
        return "LOG-0001";
    }

    // ══════════════════════════════════════════════════════
    //  LEGACY STUBS
    // ══════════════════════════════════════════════════════

    /** @deprecated Use authenticateManager(String, String) instead. */
    @Deprecated
    public static boolean login(String username, String password) {
        return authenticateManager(username, password).success;
    }

    /** @deprecated Registration is not supported. */
    @Deprecated
    public static boolean register(String username, String password) { return false; }

    // ══════════════════════════════════════════════════════
    //  AUTH RESULT
    // ══════════════════════════════════════════════════════
    public static class AuthResult {
        public final boolean success;
        public final String  message;  // display name on success, error text on failure
        private AuthResult(boolean s, String m) { success = s; message = m; }
        public static AuthResult ok(String name)    { return new AuthResult(true,  name); }
        public static AuthResult fail(String error) { return new AuthResult(false, error); }
    }
}