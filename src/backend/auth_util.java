package backend;

public class auth_util {

    // ── Hardcoded credentials ─────────────────────────────
    private static final String EMPLOYEE_NAME     = "Yhvhan Suba";

    private static final String MANAGER_NAME      = "Evhy Suba";
    private static final String MANAGER_PASSWORD  = "Admin@12345";

    // ── Employee auth ─────────────────────────────────────
    /**
     * Returns true if the entered name matches the hardcoded employee name.
     * Date / time fields are intentionally ignored.
     */
    public static boolean authenticateEmployee(String name) {
        if (name == null) return false;
        return name.trim().equalsIgnoreCase(EMPLOYEE_NAME);
    }

    // ── Manager auth ──────────────────────────────────────
    /**
     * Returns true if both name and password match the hardcoded manager credentials.
     */
    public static boolean authenticateManager(String name, String password) {
        if (name == null || password == null) return false;
        return name.trim().equalsIgnoreCase(MANAGER_NAME)
                && password.equals(MANAGER_PASSWORD);
    }

    // ── Legacy stubs (kept for compatibility) ─────────────
    public static boolean login(String username, String password) {
        return authenticateManager(username, password);
    }

    public static boolean register(String username, String password) {
        return false; // registration not supported
    }
}