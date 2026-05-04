package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class menu_util {

    // ══════════════════════════════════════════════════════
    //  MENU DATA — { name, priceSmall, priceLarge, hasCupSize, imagePath }
    // ══════════════════════════════════════════════════════
    public static final String[][] ESPRESSO = {
        { "Americano",  "₱100", "₱105", "true",  "assets/menu_items/americano_img.png"  },
        { "Cafe Latte", "₱120", "₱125", "true",  "assets/menu_items/cafelatte_img.png"  },
        { "Cafe Mocha", "₱140", "₱140", "false", "assets/menu_items/cafemocha_img.png"  },
        { "Cappuccino", "₱120", "₱125", "true",  "assets/menu_items/cappuccino_img.png" },
    };
    public static final String[][] SPECIALTY = {
        { "Banoffee ★",              "₱190", "", "false", "assets/menu_items/banoffee_img.png"           },
        { "Biscoff Cream Latte ★",   "₱195", "", "false", "assets/menu_items/biscoffcreamlatte_img.png"  },
        { "Biscoff Cold Foam Latte", "₱195", "", "false", "assets/menu_items/biscoffcoldfoamlatte_img.png"},
        { "Creme Brulee Latte",      "₱170", "", "false", "assets/menu_items/cremebruleelatte_img.png"   },
        { "Lavender Latte",          "₱170", "", "false", "assets/menu_items/lavenderlatte_img.png"      },
        { "Pistachio Latte ★",       "₱190", "", "false", "assets/menu_items/pistachiolatte_img.png"     },
        { "Pumpkin Spice Latte",     "₱170", "", "false", "assets/menu_items/pumpkinspicelatte_img.png"  },
        { "Sakura Cloud Latte",      "₱170", "", "false", "assets/menu_items/sakuracloudlatte_img.png"   },
        { "Smores Latte",            "₱170", "", "false", "assets/menu_items/smoreslatte_img.png"        },
        { "Tiramisu Latte",          "₱170", "", "false", "assets/menu_items/tiramisulatte_img.png"      },
        { "Signature Drink ★",       "₱200", "", "false", "assets/menu_items/signaturedrink_img.png"     },
    };
    public static final String[][] ICED_COFFEE = {
        { "Butterscotch Latte ★", "₱150", "", "false", "assets/menu_items/butterscotchlatte_img.png"  },
        { "Caramel Latte",        "₱150", "", "false", "assets/menu_items/caramellatte_img.png"       },
        { "Caramel Macchiato",    "₱150", "", "false", "assets/menu_items/caramelmacchiato_img.png"   },
        { "Hazelnut Latte",       "₱150", "", "false", "assets/menu_items/hazelnutlatte_img.png"      },
        { "Irish Cream Latte ★",  "₱150", "", "false", "assets/menu_items/irishcreamlatte_img.png"    },
        { "Mocha Latte",          "₱150", "", "false", "assets/menu_items/mochalatte_img.png"         },
        { "Spanish Latte ★",      "₱150", "", "false", "assets/menu_items/spanishlatte_img.png"       },
        { "Vanilla Latte",        "₱150", "", "false", "assets/menu_items/vanilalatte_img.png"        },
        { "White Mocha Latte",    "₱150", "", "false", "assets/menu_items/whitemochalatte_img.png"    },
    };
    public static final String[][] FRAPPE = {
        { "Coffee Caramel", "₱160", "", "false", "assets/menu_items/coffeecaramel_img.png" },
        { "Dark Mocha",     "₱160", "", "false", "assets/menu_items/darkmocha_img.png"     },
        { "Pecan Praline",  "₱160", "", "false", "assets/menu_items/pecanpraline_img.png"  },
        { "White Mocha",    "₱160", "", "false", "assets/menu_items/whitemocha_img.png"    },
    };
    public static final String[][] MATCHA = {
        { "Agave Matcha Latte ★",     "₱180", "", "false", "assets/menu_items/agavematchalatte_img.png"     },
        { "Banana Matcha ★",          "₱190", "", "false", "assets/menu_items/bananamatcha_img.png"         },
        { "Blueberry Matcha",         "₱190", "", "false", "assets/menu_items/blueberrymatcha_img.png"      },
        { "Dirty Matcha",             "₱190", "", "false", "assets/menu_items/dirtymatcha_img.png"          },
        { "Ichigo Matcha Latte",      "₱170", "", "false", "assets/menu_items/ichigomatchalatte_img.png"    },
        { "Lavender Matcha Latte",    "₱170", "", "false", "assets/menu_items/lavendermatchalatte_img.png"  },
        { "Mango Matcha",             "₱190", "", "false", "assets/menu_items/mangomatcha_img.png"          },
        { "Matcha Latte ★",           "₱170", "", "false", "assets/menu_items/matchalatte_img.png"          },
        { "Matcha Pistachio Latte ★", "₱200", "", "false", "assets/menu_items/matchapistachiolatte_img.png" },
        { "Strawberry Matcha Latte",  "₱190", "", "false", "assets/menu_items/strawberrymatchalatte_img.png"},
        { "Oreo Matcha",              "₱190", "", "false", "assets/menu_items/oreomatcha_img.png"           },
        { "Premium Hojicha",          "₱180", "", "false", "assets/menu_items/premiumhojicha_img.png"       },
        { "Kinako Hojicha ★",         "₱200", "", "false", "assets/menu_items/kinakohojicha_img.png"        },
    };
    public static final String[][] SMOOTHIE = {
        { "Biscoff ★",             "₱190", "", "false", "assets/menu_items/biscoffsmoothie_img.png"           },
        { "Blueberry Cheesecake",  "₱170", "", "false", "assets/menu_items/blueberrycheesecakesmoothie_img.png"},
        { "Matcha",                "₱180", "", "false", "assets/menu_items/matchasmoothie_img.png"             },
        { "Oreo Frappuccino",      "₱170", "", "false", "assets/menu_items/oreofrappucinosmoothie_img.png"     },
        { "Strawberry",            "₱160", "", "false", "assets/menu_items/strawberrysmoothie_img.png"         },
        { "Strawberry Cheesecake", "₱170", "", "false", "assets/menu_items/strawberrycheesecakesmoothie_img.png"},
    };
    public static final String[][] REFRESHER = {
        { "Four Red Fruits Tea ★", "₱125", "", "false", "assets/menu_items/fourredfruitsea_img.png"      },
        { "Kiwi Green Apple Tea",  "₱125", "", "false", "assets/menu_items/kiwigreenappletea_img.png"    },
        { "Passion Fruit Tea",     "₱125", "", "false", "assets/menu_items/passionfruittea_img.png"      },
        { "Pomegranate Lemon Tea", "₱125", "", "false", "assets/menu_items/pomegranatelemontea_img.png"  },
        { "Wild Berry Tea",        "₱125", "", "false", "assets/menu_items/wildberrytea_img.png"         },
    };
    public static final String[][] ADD_ONS = {
        { "Espresso Shot", "₱30", "", "false", "assets/menu_items/espressoshot_img.png" },
        { "Sub Oat",       "₱30", "", "false", "assets/menu_items/suboat_img.png"       },
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
    //  IMAGE INDEX — column index of the image path in each row
    // ══════════════════════════════════════════════════════
    public static final int COL_IMAGE = 4;

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