package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class menu_util {

    // ══════════════════════════════════════════════════════
    //  MENU DATA — { name, priceSmall, priceLarge, hasCupSize }
    // ══════════════════════════════════════════════════════
    public static final String[][] ESPRESSO = {
        { "Americano",  "₱100", "₱105", "true"  },
        { "Cafe Latte", "₱120", "₱125", "true"  },
        { "Cafe Mocha", "₱140", "₱140", "false" },
        { "Cappuccino", "₱120", "₱125", "true"  },
    };
    public static final String[][] SPECIALTY = {
        { "Banoffee ★",              "₱190", "", "false" },
        { "Biscoff Cream Latte ★",   "₱195", "", "false" },
        { "Biscoff Cold Foam Latte", "₱195", "", "false" },
        { "Creme Brulee Latte",      "₱170", "", "false" },
        { "Lavender Latte",          "₱170", "", "false" },
        { "Pistachio Latte ★",       "₱190", "", "false" },
        { "Pumpkin Spice Latte",     "₱170", "", "false" },
        { "Sakura Cloud Latte",      "₱170", "", "false" },
        { "Smores Latte",            "₱170", "", "false" },
        { "Tiramisu Latte",          "₱170", "", "false" },
        { "Signature Drink ★",       "₱200", "", "false" },
    };
    public static final String[][] ICED_COFFEE = {
        { "Butterscotch Latte ★", "₱150", "", "false" },
        { "Caramel Latte",        "₱150", "", "false" },
        { "Caramel Macchiato",    "₱150", "", "false" },
        { "Hazelnut Latte",       "₱150", "", "false" },
        { "Irish Cream Latte ★",  "₱150", "", "false" },
        { "Mocha Latte",          "₱150", "", "false" },
        { "Spanish Latte ★",      "₱150", "", "false" },
        { "Vanilla Latte",        "₱150", "", "false" },
        { "White Mocha Latte",    "₱150", "", "false" },
    };
    public static final String[][] FRAPPE = {
        { "Coffee Caramel", "₱160", "", "false" },
        { "Dark Mocha",     "₱160", "", "false" },
        { "Pecan Praline",  "₱160", "", "false" },
        { "White Mocha",    "₱160", "", "false" },
    };
    public static final String[][] MATCHA = {
        { "Agave Matcha Latte ★",     "₱180", "", "false" },
        { "Banana Matcha ★",          "₱190", "", "false" },
        { "Blueberry Matcha",         "₱190", "", "false" },
        { "Dirty Matcha",             "₱190", "", "false" },
        { "Ichigo Matcha Latte",      "₱170", "", "false" },
        { "Lavender Matcha Latte",    "₱170", "", "false" },
        { "Mango Matcha",             "₱190", "", "false" },
        { "Matcha Latte ★",           "₱170", "", "false" },
        { "Matcha Pistachio Latte ★", "₱200", "", "false" },
        { "Strawberry Matcha Latte",  "₱190", "", "false" },
        { "Oreo Matcha",              "₱190", "", "false" },
        { "Premium Hojicha",          "₱180", "", "false" },
        { "Kinako Hojicha ★",         "₱200", "", "false" },
    };
    public static final String[][] SMOOTHIE = {
        { "Biscoff ★",             "₱190", "", "false" },
        { "Blueberry Cheesecake",  "₱170", "", "false" },
        { "Matcha",                "₱180", "", "false" },
        { "Oreo Frappuccino",      "₱170", "", "false" },
        { "Strawberry",            "₱160", "", "false" },
        { "Strawberry Cheesecake", "₱170", "", "false" },
    };
    public static final String[][] REFRESHER = {
        { "Four Red Fruits Tea ★", "₱125", "", "false" },
        { "Kiwi Green Apple Tea",  "₱125", "", "false" },
        { "Passion Fruit Tea",     "₱125", "", "false" },
        { "Pomegranate Lemon Tea", "₱125", "", "false" },
        { "Wild Berry Tea",        "₱125", "", "false" },
    };
    public static final String[][] ADD_ONS = {
        { "Espresso Shot", "₱30", "", "false" },
        { "Sub Oat",       "₱30", "", "false" },
    };

    public static final String[] CATEGORY_NAMES = {
        "All", "Espresso", "Specialty Coffee", "Iced Coffee",
        "Frappe", "Matcha Series", "Smoothies", "Refreshers", "Add-ons"
    };
    public static final String[][][] CATEGORY_DATA = {
        null, ESPRESSO, SPECIALTY, ICED_COFFEE,
        FRAPPE, MATCHA, SMOOTHIE, REFRESHER, ADD_ONS
    };
    public static final String[] SECTION_TITLES = {
        null, "Espresso", "Specialty Coffee", "Iced Coffee",
        "Frappe", "Matcha Series", "Smoothie", "Refresher", "Add-ons"
    };

    // ══════════════════════════════════════════════════════
    //  SUBMIT RESULT
    //  amountFormatted uses plain "%,.2f" (e.g. "1,234.00")
    //  — no peso symbol — to match what fetchPayments produces,
    //  avoiding JavaFX label encoding issues with ₱.
    // ══════════════════════════════════════════════════════
    public static class SubmitResult {
        public final boolean success;
        public final String  orderId;
        public final String  customerId;
        public final String  customerName;
        public final String  paymentMethod;
        /** Generated payment_id inserted into dbo.Payments (null on failure). */
        public final String  paymentId;
        /**
         * Total formatted as a plain number string, e.g. "1,234.00".
         * No peso symbol — payments_contents displays it as-is.
         */
        public final String  amountFormatted;

        public SubmitResult(boolean success,
                            String orderId,
                            String customerId,
                            String customerName,
                            String paymentMethod,
                            String paymentId,
                            String amountFormatted) {
            this.success         = success;
            this.orderId         = orderId;
            this.customerId      = customerId;
            this.customerName    = customerName;
            this.paymentMethod   = paymentMethod;
            this.paymentId       = paymentId;
            this.amountFormatted = amountFormatted;
        }
    }

    // ══════════════════════════════════════════════════════
    //  ORDER STATE
    // ══════════════════════════════════════════════════════
    private final List<String[]> orderItems = new ArrayList<>();
    private final Connection conn;

    public menu_util(Connection conn) {
        this.conn = conn;
    }

    // ── Order item management ─────────────────────────────

    public boolean addItem(String name, String priceStr) {
        for (String[] item : orderItems) {
            if (item[0].equals(name) && item[1].equals(priceStr)) {
                item[2] = String.valueOf(Integer.parseInt(item[2]) + 1);
                return false;
            }
        }
        orderItems.add(new String[]{ name, priceStr, "1" });
        return true;
    }

    public void incrementQty(int index) {
        if (index < 0 || index >= orderItems.size()) return;
        String[] item = orderItems.get(index);
        item[2] = String.valueOf(Integer.parseInt(item[2]) + 1);
    }

    public boolean decrementQty(int index) {
        if (index < 0 || index >= orderItems.size()) return false;
        int cur = Integer.parseInt(orderItems.get(index)[2]);
        if (cur <= 1) {
            orderItems.remove(index);
            return true;
        }
        orderItems.get(index)[2] = String.valueOf(cur - 1);
        return false;
    }

    public void clearOrder() { orderItems.clear(); }

    public List<String[]> getOrderItems() { return new ArrayList<>(orderItems); }

    public boolean isEmpty() { return orderItems.isEmpty(); }

    // ── Price calculations ────────────────────────────────

    public static double parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isBlank()) return 0;
        try {
            return Double.parseDouble(priceStr.replace("₱", "").trim());
        } catch (NumberFormatException e) { return 0; }
    }

    public double getSubtotal() {
        double total = 0;
        for (String[] item : orderItems)
            total += parsePrice(item[1]) * Integer.parseInt(item[2]);
        return total;
    }

    public double getTax()   { return getSubtotal() * 0.12; }
    public double getTotal() { return getSubtotal() + getTax(); }

    public double getChange(double amountPaid) { return amountPaid - getTotal(); }

    // ══════════════════════════════════════════════════════
    //  DATABASE PERSISTENCE
    // ══════════════════════════════════════════════════════

    /**
     * Persists the transaction in three steps:
     *   1. Upsert customer  → dbo.Customers
     *   2. Insert order     → dbo.Orders
     *   3. Insert payment   → dbo.Payments
     *
     * amountFormatted in the result is a plain comma-formatted
     * number string (e.g. "1,234.00") — no peso symbol — so it
     * renders cleanly in JavaFX labels.
     */
    public SubmitResult submitOrder(String customerName, String paymentMethod) {
        SubmitResult failed = new SubmitResult(
            false, null, null, customerName, paymentMethod, null, null
        );

        if (orderItems.isEmpty() || conn == null) return failed;
        if (customerName == null || customerName.isBlank()) return failed;
        try { if (conn.isClosed()) return failed; } catch (Exception e) { return failed; }

        try {
            // 1. Customer
            String customerId = findCustomerId(customerName);
            if (customerId == null) customerId = insertCustomer(customerName);
            if (customerId == null) return failed;

            // 2. Order
            double total   = getTotal();
            String orderId = insertOrder(customerId, paymentMethod, total);
            if (orderId == null) return failed;

            // 3. Payment — non-fatal if it fails
            String paymentId       = insertPayment(orderId, paymentMethod, total);
            // Plain number format — matches fetchPayments("%,.2f") — no ₱ symbol
            String amountFormatted = String.format("%,.2f", total);

            return new SubmitResult(
                true, orderId, customerId, customerName,
                paymentMethod, paymentId, amountFormatted
            );

        } catch (Exception e) {
            e.printStackTrace();
            return failed;
        }
    }

    // ── Private DB helpers ────────────────────────────────

    private String findCustomerId(String name) {
        String sql =
            "SELECT customer_id FROM dbo.Customers " +
            "WHERE customer_name = ? AND is_deleted = 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String id = rs.getString("customer_id");
                rs.close();
                return id;
            }
            rs.close();
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private String insertCustomer(String name) {
        String id  = "CUST-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        String sql =
            "INSERT INTO dbo.Customers " +
            "(customer_id, customer_name, status, is_deleted) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setString(3, "active");
            ps.setInt(4, 0);
            ps.executeUpdate();
            return id;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    private String insertOrder(String customerId, String paymentMethod, double total) {
        String id  = "ORD-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        String pay = (paymentMethod != null && !paymentMethod.isBlank()) ? paymentMethod : "Cash";
        String sql =
            "INSERT INTO dbo.Orders " +
            "(order_id, customer_id, order_status, payment_type, " +
            " total_amount, status, is_deleted, order_date) " +
            "VALUES (?,?,?,?,?,?,?,GETDATE())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, customerId);
            ps.setString(3, "Pending");
            ps.setString(4, pay);
            ps.setDouble(5, total);
            ps.setString(6, "active");
            ps.setInt(7, 0);
            ps.executeUpdate();
            return id;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    /**
     * Inserts one row into dbo.Payments for the order just created.
     * Returns the generated payment_id, or null if the insert failed.
     */
    private String insertPayment(String orderId, String paymentMethod, double amount) {
        String id  = "PAY-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        String pay = (paymentMethod != null && !paymentMethod.isBlank()) ? paymentMethod : "Cash";
        String sql =
            "INSERT INTO dbo.Payments " +
            "(payment_id, order_id, payment_method, amount, " +
            " payment_date, is_deleted, status) " +
            "VALUES (?,?,?,?,GETDATE(),?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, orderId);
            ps.setString(3, pay);
            ps.setDouble(4, amount);
            ps.setInt(5, 0);
            ps.setString(6, "active");
            ps.executeUpdate();
            return id;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }
}