package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

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
    //  EMPLOYEE AUTHENTICATION  — DB-driven
    // ══════════════════════════════════════════════════════

    /**
     * Looks up the employee by name in dbo.Employees (case-insensitive).
     * Blocks Terminated employees.
     *
     * Clock-in rules:
     *  1. Auto-close any open rows from PREVIOUS days (app killed mid-shift).
     *  2. Block if there is STILL an open (time_out IS NULL) row for TODAY
     *     — genuine double clock-in; employee must time out first.
     *  3. If shiftDate != today AND a completed record already exists for that
     *     date, block it — prevents back-dating a second shift.
     *     Same-day re-clock-in (after a proper time-out) is always allowed.
     *  4. Otherwise → open a new TimeLogs row and cache the session.
     *
     * @param name      employee name as entered in the UI
     * @param shiftDate the date the employee is clocking in for (chosen in the UI)
     */
    public static AuthResult authenticateEmployee(String name, LocalDate shiftDate) {
        if (name == null || name.trim().isEmpty())
            return AuthResult.fail("Please enter your name.");
        if (shiftDate == null)
            return AuthResult.fail("Please select a valid shift date.");
        if (conn == null)
            return AuthResult.fail("No database connection.");
        try {
            if (conn.isClosed()) return AuthResult.fail("Database connection is closed.");
        } catch (Exception e) { return AuthResult.fail("Database error."); }

        // ── 1. Look up employee ───────────────────────────
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

            // ── 2. Auto-close stale open rows from PREVIOUS days ──
            //    If the app was force-killed, an open row may linger from a past date.
            //    DATEDIFF(DAY, time_in, GETDATE()) > 0 is true whenever time_in falls
            //    on any calendar day before today, regardless of the time component.
            try (PreparedStatement sp = conn.prepareStatement(
                    "UPDATE dbo.TimeLogs " +
                    "SET    time_out = GETDATE() " +
                    "WHERE  employee_id = ? " +
                    "  AND  time_out   IS NULL " +
                    "  AND  is_deleted  = 0 " +
                    "  AND  DATEDIFF(DAY, time_in, GETDATE()) > 0")) {
                sp.setString(1, empId);
                int fixed = sp.executeUpdate();
                if (fixed > 0)
                    System.out.println("[AUTH] Auto-closed " + fixed + " stale open log(s) for " + empName);
            }

            // ── 3. Block only if there is still an open row for TODAY ──
            //    After step 2 all previous-day stragglers are closed.
            //    DATEDIFF(DAY, time_in, GETDATE()) = 0 means time_in is today.
            try (PreparedStatement cp = conn.prepareStatement(
                    "SELECT COUNT(*) FROM dbo.TimeLogs " +
                    "WHERE  employee_id = ? " +
                    "  AND  time_out   IS NULL " +
                    "  AND  is_deleted  = 0 " +
                    "  AND  DATEDIFF(DAY, time_in, GETDATE()) = 0")) {
                cp.setString(1, empId);
                ResultSet cr = cp.executeQuery();
                cr.next();
                int openToday = cr.getInt(1);
                cr.close();
                if (openToday > 0)
                    return AuthResult.fail("You are already clocked in. Please time out first.");
            }

            // ── 4. Block re-clock-in only when shiftDate is NOT today ──
            //    Same-day rule: an employee can clock in multiple times today
            //    (e.g. time out for lunch, clock back in) — step 3 already
            //    ensures no duplicate OPEN rows exist.
            //    Future/past-date rule: if they pick a different date AND a
            //    completed record already exists for that date, block it.
            if (!shiftDate.equals(LocalDate.now())) {
                try (PreparedStatement dp = conn.prepareStatement(
                        "SELECT COUNT(*) FROM dbo.TimeLogs " +
                        "WHERE  employee_id = ? " +
                        "  AND  time_out   IS NOT NULL " +
                        "  AND  is_deleted  = 0 " +
                        "  AND  DATEDIFF(DAY, time_in, ?) = 0")) {
                    dp.setString(1, empId);
                    dp.setDate(2, java.sql.Date.valueOf(shiftDate));
                    ResultSet dr = dp.executeQuery();
                    dr.next();
                    int doneOnDate = dr.getInt(1);
                    dr.close();
                    if (doneOnDate > 0)
                        return AuthResult.fail(
                            "You already completed a shift on " +
                            shiftDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) +
                            ". You cannot clock in again for the same date.");
                }
            }

            // ── 5. Open a new TimeLogs row ────────────────────────
            // FIX: Use UUID-based log IDs to prevent primary key collisions.
            // The old MAX(log_id)+1 approach broke when any non-numeric ID
            // (from the System.currentTimeMillis() fallback) existed in the table,
            // causing parseInt to throw → fallback to a millis ID → next run also
            // fails parseInt → LOG-0001 is reused → INSERT fails silently on PK
            // violation → the old row stays open → "already clocked in" fires.
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

    /**
     * @deprecated Use authenticateEmployee(String name, LocalDate shiftDate) instead.
     */
    @Deprecated
    public static AuthResult authenticateEmployee(String name) {
        return authenticateEmployee(name, LocalDate.now());
    }

    // ══════════════════════════════════════════════════════
    //  TIME-OUT
    // ══════════════════════════════════════════════════════

    /**
     * Stamps time_out = GETDATE() on the open TimeLogs row, then clears session.
     * No-op for manager sessions (returns false gracefully).
     *
     * FIX: Added AND time_out IS NULL guard to the UPDATE so a double-call
     * (e.g. retry after a jitter) doesn't overwrite an already-stamped row,
     * and we can detect whether the row was actually still open via rowcount.
     */
    public static boolean timeOut() {
        if (currentLogId == null || conn == null) return false;
        try { if (conn.isClosed()) return false; } catch (Exception e) { return false; }

        String logIdToStamp = currentLogId; // snapshot before logout() nulls it

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE dbo.TimeLogs " +
                "SET    time_out = GETDATE() " +
                "WHERE  log_id   = ? " +
                "  AND  time_out IS NULL")) {          // guard: don't double-stamp
            ps.setString(1, logIdToStamp);
            int updated = ps.executeUpdate();
            System.out.println("[TIME-OUT] Log ID  : " + logIdToStamp);
            System.out.println("[TIME-OUT] Name    : " + currentName);
            System.out.println("[TIME-OUT] Stamped : " + (updated > 0 ? "YES" : "NO (already closed)"));
            logout(); // always clear session regardless of stamp result
            return updated > 0;
        } catch (Exception e) {
            e.printStackTrace();
            logout(); // clear session even if DB update failed
            return false;
        }
    }

    // ══════════════════════════════════════════════════════
    //  LOGOUT
    // ══════════════════════════════════════════════════════

    /**
     * Clears ALL session state fields.
     * Employees: call timeOut() instead so the DB row is stamped first.
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

    /**
     * Generates a guaranteed-unique log ID using UUID.
     *
     * WHY THIS REPLACES THE OLD MAX(log_id)+1 APPROACH:
     * The old method tried to parse the max log_id as an integer. If ANY row
     * in the table had a non-numeric ID (e.g. "LOG-1714900000000" from the
     * System.currentTimeMillis() fallback), parseInt threw a NumberFormatException,
     * the catch block returned another millis-based ID, and the cycle repeated —
     * eventually producing a duplicate "LOG-0001" that caused a silent PK violation
     * on INSERT, leaving the previous row's time_out as NULL, which then triggered
     * the "already clocked in" error on the employee's next login.
     *
     * UUID-based IDs are collision-proof and require no table scan.
     */
    private static String generateLogId() {
        return "LOG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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