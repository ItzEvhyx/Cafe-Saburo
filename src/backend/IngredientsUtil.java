package backend;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Data-access helper for the ingredients table.
 * Targets Microsoft SQL Server (T-SQL) via the mssql-jdbc driver.
 *
 * Expected DDL (see SQLQueries/ingredients_query.sql):
 *
 *   CREATE TABLE ingredients (
 *       ingredient_id   VARCHAR(10)    PRIMARY KEY,
 *       ingredient_name VARCHAR(255)   NOT NULL,
 *       price           DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
 *       status          VARCHAR(10)    NOT NULL DEFAULT 'active'
 *   );
 *
 * Row format returned by fetchIngredients():
 *   [0] ingredient_id
 *   [1] ingredient_name
 *   [2] price  (formatted as "%.2f")
 */
public class IngredientsUtil {

    private final Connection conn;

    public IngredientsUtil(Connection conn) {
        this.conn = conn;
    }

    // ══════════════════════════════════════════════════════
    //  FETCH
    // ══════════════════════════════════════════════════════

    /**
     * Returns all rows for the given tab ("active" or "archived"),
     * ordered newest-first by the numeric suffix of ingredient_id.
     *
     * T-SQL note: CAST(SUBSTRING(...) AS INT) — not UNSIGNED (MySQL-only).
     * SUBSTRING in T-SQL is 1-based; "ING-0001" → SUBSTRING(ingredient_id, 5, LEN(ingredient_id))
     * strips the first 4 chars ("ING-") and casts the remainder as INT.
     */
    public List<String[]> fetchIngredients(String tab) {
        List<String[]> rows = new ArrayList<>();
        if (conn == null) {
            System.err.println("[IngredientsUtil] fetchIngredients: connection is null");
            return rows;
        }

        String sql = "SELECT ingredient_id, ingredient_name, price " +
                     "FROM ingredients " +
                     "WHERE status = ? " +
                     "ORDER BY CAST(SUBSTRING(ingredient_id, 5, LEN(ingredient_id)) AS INT) DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tab);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("ingredient_id"),
                    rs.getString("ingredient_name"),
                    String.format("%.2f", rs.getDouble("price"))
                });
            }
            System.out.println("[IngredientsUtil] fetchIngredients(" + tab + "): " + rows.size() + " row(s) loaded.");
        } catch (SQLException e) {
            System.err.println("[IngredientsUtil] fetchIngredients error: " + e.getMessage());
            e.printStackTrace();
        }
        return rows;
    }

    // ══════════════════════════════════════════════════════
    //  INSERT
    // ══════════════════════════════════════════════════════

    /**
     * Inserts a new active ingredient and returns the auto-generated ID,
     * or null on failure.
     *
     * ID format: ING-XXXX  (e.g. ING-0001)
     */
    public String insertIngredient(String name, double price) {
        if (conn == null) {
            System.err.println("[IngredientsUtil] insertIngredient: connection is null");
            return null;
        }

        String newId = generateNextId();
        if (newId == null) {
            System.err.println("[IngredientsUtil] insertIngredient: could not generate next ID");
            return null;
        }

        String sql = "INSERT INTO ingredients (ingredient_id, ingredient_name, price, status) " +
                     "VALUES (?, ?, ?, 'active')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newId);
            ps.setString(2, name);
            ps.setDouble(3, price);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                System.out.println("[IngredientsUtil] Inserted ingredient: " + newId + " - " + name);
                return newId;
            } else {
                System.err.println("[IngredientsUtil] insertIngredient: 0 rows affected");
                return null;
            }
        } catch (SQLException e) {
            System.err.println("[IngredientsUtil] insertIngredient error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Finds the highest existing numeric suffix across ALL statuses
     * (active + archived) to avoid ID collisions, then increments by 1.
     *
     * T-SQL: uses SUBSTRING(col, 5, LEN(col)) and CAST AS INT.
     */
    private String generateNextId() {
        String sql = "SELECT TOP 1 ingredient_id FROM ingredients " +
                     "ORDER BY CAST(SUBSTRING(ingredient_id, 5, LEN(ingredient_id)) AS INT) DESC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                String last = rs.getString("ingredient_id"); // e.g. "ING-0012"
                try {
                    int num = Integer.parseInt(last.substring(4)); // strip "ING-"
                    return String.format("ING-%04d", num + 1);
                } catch (NumberFormatException nfe) {
                    System.err.println("[IngredientsUtil] generateNextId: unexpected ID format: " + last);
                    return null;
                }
            }
            // Table is empty — start from ING-0001
            return "ING-0001";
        } catch (SQLException e) {
            System.err.println("[IngredientsUtil] generateNextId error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ══════════════════════════════════════════════════════
    //  UPDATE
    // ══════════════════════════════════════════════════════

    public boolean updateIngredient(String id, String name, double price) {
        if (conn == null) return false;
        String sql = "UPDATE ingredients SET ingredient_name = ?, price = ? " +
                     "WHERE ingredient_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setDouble(2, price);
            ps.setString(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[IngredientsUtil] updateIngredient error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ══════════════════════════════════════════════════════
    //  ARCHIVE / RESTORE
    // ══════════════════════════════════════════════════════

    public void archiveSelected(Set<String> ids) {
        setStatus(ids, "archived");
    }

    public void restoreSelected(Set<String> ids) {
        setStatus(ids, "active");
    }

    private void setStatus(Set<String> ids, String status) {
        if (conn == null || ids.isEmpty()) return;
        String placeholders = String.join(",", ids.stream().map(id -> "?").toArray(String[]::new));
        String sql = "UPDATE ingredients SET status = ? WHERE ingredient_id IN (" + placeholders + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            int i = 2;
            for (String id : ids) ps.setString(i++, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[IngredientsUtil] setStatus error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════
    //  HARD DELETE
    // ══════════════════════════════════════════════════════

    public void hardDeleteAll(String tab) {
        if (conn == null) return;
        String sql = "DELETE FROM ingredients WHERE status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tab);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[IngredientsUtil] hardDeleteAll error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════
    //  SEARCH / FILTER  (client-side)
    // ══════════════════════════════════════════════════════

    public List<String[]> filterRows(List<String[]> rows, String query) {
        if (query == null || query.isBlank()) return rows;
        String q = query.toLowerCase();
        List<String[]> out = new ArrayList<>();
        for (String[] r : rows) {
            if (r[0].toLowerCase().contains(q) ||   // ingredient_id
                r[1].toLowerCase().contains(q) ||   // ingredient_name
                r[2].toLowerCase().contains(q)) {   // price
                out.add(r);
            }
        }
        return out;
    }

    // ══════════════════════════════════════════════════════
    //  CSV EXPORT
    // ══════════════════════════════════════════════════════

    public void exportToCsv(File file, List<String[]> rows, String tab) {
        if (file == null || rows.isEmpty()) return;
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("Ingredient ID,Ingredient,Price\n");
            for (String[] r : rows) {
                fw.write(escapeCsv(r[0]) + "," +
                         escapeCsv(r[1]) + "," +
                         escapeCsv(r[2]) + "\n");
            }
            System.out.println("[IngredientsUtil] CSV exported to: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[IngredientsUtil] exportToCsv error: " + e.getMessage());
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}