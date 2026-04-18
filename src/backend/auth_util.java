package backend;

public class auth_util {

    // ══════════════════════════════════════════════════════
    //  ROLE ENUM
    // ══════════════════════════════════════════════════════
    public enum UserRole {
        EMPLOYEE,
        MANAGER
    }

    // ── Hardcoded credentials ─────────────────────────────
    private static final String EMPLOYEE_NAME    = "Yhvhan Suba";

    private static final String MANAGER_NAME     = "Evhy Suba";
    private static final String MANAGER_PASSWORD = "Admin@12345";

    // ── Session role (set on successful login) ────────────
    private static UserRole currentRole = null;

    public static UserRole getCurrentRole() {
        return currentRole;
    }

    public static boolean isManager() {
        return currentRole == UserRole.MANAGER;
    }

    public static boolean isEmployee() {
        return currentRole == UserRole.EMPLOYEE;
    }

    // ── Employee auth ─────────────────────────────────────
    /**
     * Returns true if the entered name matches the hardcoded employee name.
     * Sets session role to EMPLOYEE on success.
     */
    public static boolean authenticateEmployee(String name) {
        if (name == null) return false;
        boolean ok = name.trim().equalsIgnoreCase(EMPLOYEE_NAME);
        if (ok) currentRole = UserRole.EMPLOYEE;
        return ok;
    }

    // ── Manager auth ──────────────────────────────────────
    /**
     * Returns true if both name and password match the hardcoded manager credentials.
     * Sets session role to MANAGER on success.
     */
    public static boolean authenticateManager(String name, String password) {
        if (name == null || password == null) return false;
        boolean ok = name.trim().equalsIgnoreCase(MANAGER_NAME)
                  && password.equals(MANAGER_PASSWORD);
        if (ok) currentRole = UserRole.MANAGER;
        return ok;
    }

    // ── Logout helper ─────────────────────────────────────
    public static void logout() {
        currentRole = null;
    }

    // ── Legacy stubs (kept for compatibility) ─────────────
    public static boolean login(String username, String password) {
        return authenticateManager(username, password);
    }

    public static boolean register(String username, String password) {
        return false; // registration not supported
    }
}