package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * promotions_util
 *
 * All business logic and database operations for the Promotions module.
 * promotions_contents calls this class exclusively; it holds no JavaFX imports.
 *
 * SQL source: SQLQueries/promotions_query.sql
 */
public class promotions_util {

    // ══════════════════════════════════════════════════════
    //  SQL QUERIES  (mirrors promotions_query.sql)
    // ══════════════════════════════════════════════════════
    private static final String SQL_FETCH_BY_STATUS =
        "SELECT promo_id, promo_name, discount_type, " +
        "CAST(start_date AS VARCHAR) AS start_date, " +
        "CAST(end_date   AS VARCHAR) AS end_date " +
        "FROM promotions WHERE status = ? ORDER BY created_at DESC";

    private static final String SQL_NEXT_ID =
        "SELECT COALESCE('PRO-' || LPAD(" +
        "CAST(MAX(CAST(SUBSTRING(promo_id, 5) AS INTEGER)) + 1 AS VARCHAR)," +
        "4, '0'), 'PRO-0001') AS next_id FROM promotions";

    private static final String SQL_INSERT =
        "INSERT INTO promotions (promo_id, promo_name, discount_type, start_date, end_date, status) " +
        "VALUES (?, ?, ?, ?, ?, 'active')";

    private static final String SQL_UPDATE_START_DATE =
        "UPDATE promotions SET start_date = ? WHERE promo_id = ?";

    private static final String SQL_UPDATE_END_DATE =
        "UPDATE promotions SET end_date = ? WHERE promo_id = ?";

    private static final String SQL_ARCHIVE_ONE =
        "UPDATE promotions SET status = 'archived' WHERE promo_id = ?";

    private static final String SQL_RESTORE_ONE =
        "UPDATE promotions SET status = 'active' WHERE promo_id = ?";

    private static final String SQL_ARCHIVE_ALL =
        "UPDATE promotions SET status = 'archived' WHERE status = 'active'";

    private static final String SQL_RESTORE_ALL =
        "UPDATE promotions SET status = 'active' WHERE status = 'archived'";

    private static final String SQL_DELETE_BY_STATUS =
        "DELETE FROM promotions WHERE status = ?";

    // ══════════════════════════════════════════════════════
    //  STATE
    // ══════════════════════════════════════════════════════
    private final Connection conn;

    public promotions_util(Connection conn) {
        this.conn = conn;
    }

    // ══════════════════════════════════════════════════════
    //  READ
    // ══════════════════════════════════════════════════════

    /**
     * Fetches all promotion rows for the given status tab ("active" or "archived").
     * Falls back to the 10 built-in sample rows when the connection is null or the
     * query fails.
     *
     * @param status "active" or "archived"
     * @return list of String[5]: {promo_id, promo_name, discount_type, start_date, end_date}
     */
    public List<String[]> fetchPromotions(String status) {
        if (conn == null) {
            return status.equals("active") ? getSampleData() : new ArrayList<>();
        }
        List<String[]> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SQL_FETCH_BY_STATUS)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{
                        rs.getString("promo_id"),
                        rs.getString("promo_name"),
                        rs.getString("discount_type"),
                        rs.getString("start_date"),
                        rs.getString("end_date")
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("[promotions_util] fetchPromotions error: " + e.getMessage());
            if (status.equals("active")) return getSampleData();
        }
        return rows;
    }

    /**
     * Client-side keyword filter applied to an already-fetched list.
     * Matches against promo_id, promo_name, and discount_type (case-insensitive).
     *
     * @param rows  source list (typically cachedRows from promotions_contents)
     * @param query raw search string; blank/null returns the original list unchanged
     * @return filtered list (never null)
     */
    public List<String[]> filterRows(List<String[]> rows, String query) {
        if (query == null || query.isBlank()) return rows;
        String q = query.trim().toLowerCase(Locale.ENGLISH);
        List<String[]> out = new ArrayList<>();
        for (String[] row : rows) {
            if (row[0].toLowerCase().contains(q) ||
                row[1].toLowerCase().contains(q) ||
                row[2].toLowerCase().contains(q))
                out.add(row);
        }
        return out;
    }

    // ══════════════════════════════════════════════════════
    //  CREATE
    // ══════════════════════════════════════════════════════

    /**
     * Generates the next sequential promo ID ("PRO-NNNN") by querying the DB.
     * Falls back to a random ID when the connection is unavailable.
     */
    public String generateNextPromoId() {
        if (conn == null) {
            return String.format("PRO-%04d", (int)(Math.random() * 9000) + 1000);
        }
        try (PreparedStatement ps = conn.prepareStatement(SQL_NEXT_ID);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString("next_id");
        } catch (SQLException e) {
            System.err.println("[promotions_util] generateNextPromoId error: " + e.getMessage());
        }
        return String.format("PRO-%04d", (int)(Math.random() * 9000) + 1000);
    }

    /**
     * Inserts a new promotion record with status = 'active'.
     *
     * @return true if the row was inserted successfully
     */
    public boolean insertPromotion(String promoId, String promoName,
                                    String discountType, String startDate, String endDate) {
        if (conn == null) return false;
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, promoId);
            ps.setString(2, promoName);
            ps.setString(3, discountType);
            ps.setString(4, startDate);
            ps.setString(5, endDate);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[promotions_util] insertPromotion error: " + e.getMessage());
            return false;
        }
    }

    // ══════════════════════════════════════════════════════
    //  UPDATE
    // ══════════════════════════════════════════════════════

    /**
     * Updates the start_date or end_date of a single promotion row.
     *
     * @param promoId    target row identifier
     * @param forEndDate true → update end_date; false → update start_date
     * @param isoDate    date string in ISO-8601 format (yyyy-MM-dd)
     */
    public void updateDate(String promoId, boolean forEndDate, String isoDate) {
        if (conn == null) return;
        String sql = forEndDate ? SQL_UPDATE_END_DATE : SQL_UPDATE_START_DATE;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, isoDate);
            ps.setString(2, promoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[promotions_util] updateDate error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    //  ARCHIVE / RESTORE
    // ══════════════════════════════════════════════════════

    /**
     * Archives or restores a specific set of promotion IDs in one batched statement.
     *
     * @param ids     promo_id values to act on
     * @param archive true → set status = 'archived'; false → set status = 'active'
     */
    public void archiveOrRestoreSelected(Set<String> ids, boolean archive) {
        if (conn == null || ids.isEmpty()) return;
        String sql = archive ? SQL_ARCHIVE_ONE : SQL_RESTORE_ONE;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String id : ids) {
                ps.setString(1, id);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            System.err.println("[promotions_util] archiveOrRestoreSelected error: " + e.getMessage());
        }
    }

    /**
     * Archives all active promotions, or restores all archived ones.
     *
     * @param archive true → archive all active; false → restore all archived
     */
    public void archiveOrRestoreAll(boolean archive) {
        if (conn == null) return;
        String sql = archive ? SQL_ARCHIVE_ALL : SQL_RESTORE_ALL;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[promotions_util] archiveOrRestoreAll error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════

    /**
     * Hard-deletes every promotion row that matches the given status.
     *
     * @param status "active" or "archived"
     */
    public void deleteAll(String status) {
        if (conn == null) return;
        try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE_BY_STATUS)) {
            ps.setString(1, status);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[promotions_util] deleteAll error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════
    //  EXPORT
    // ══════════════════════════════════════════════════════

    /**
     * Builds a CSV string from the provided rows.
     * Columns: Promo ID, Promo Name, Discount Type, Start Date, End Date.
     *
     * @param rows the rows to serialise (typically cachedRows or a filtered subset)
     * @return CSV text with a header line; empty string if rows is null or empty
     */
    public String buildCsv(List<String[]> rows) {
        if (rows == null || rows.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(
            "Promo ID,Promo Name,Discount Type,Start Date,End Date\n");
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) sb.append(',');
                String cell = row[i] != null ? row[i] : "";
                sb.append('"').append(cell.replace("\"", "\"\"")).append('"');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════
    //  DATE HELPERS
    // ══════════════════════════════════════════════════════

    /**
     * Returns "Month YYYY" strings for the current month through the next 12 months.
     */
    public List<String> buildMonthOptions() {
        List<String> result = new ArrayList<>();
        for (int offset = 0; offset <= 12; offset++) {
            YearMonth ym = YearMonth.now().plusMonths(offset);
            result.add(ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                       + " " + ym.getYear());
        }
        return result;
    }

    /**
     * Returns day-of-month strings for the given "Month YYYY" label,
     * excluding days before today and, optionally, before minDate.
     *
     * @param monthYearLabel e.g. "April 2026"
     * @param minDate        lower bound (exclusive); pass null for no extra constraint
     * @return list of day strings ("1" … "31"), may be empty
     */
    public List<String> buildDayOptions(String monthYearLabel, LocalDate minDate) {
        if (monthYearLabel == null) return new ArrayList<>();
        try {
            String[]  parts = monthYearLabel.split(" ");
            Month     month = Month.valueOf(parts[0].toUpperCase(Locale.ENGLISH));
            int       year  = Integer.parseInt(parts[1]);
            YearMonth ym    = YearMonth.of(year, month);
            LocalDate today = LocalDate.now();

            List<String> days = new ArrayList<>();
            for (int d = 1; d <= ym.lengthOfMonth(); d++) {
                LocalDate candidate = LocalDate.of(year, month, d);
                if (candidate.isBefore(today))              continue;
                if (minDate != null && candidate.isBefore(minDate)) continue;
                days.add(String.valueOf(d));
            }
            return days;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Parses an ISO-8601 date string to a LocalDate.
     *
     * @return the parsed date, or null if the string is null, blank, "--", or unparseable
     */
    public LocalDate tryParseDate(String s) {
        if (s == null || s.isBlank() || s.equals("--") || s.equals("—")) return null;
        try { return LocalDate.parse(s); } catch (Exception e) { return null; }
    }

    /**
     * Parses a month-year label and a day string into a LocalDate.
     * Returns null if either argument is null or parsing fails.
     *
     * @param monthYearLabel e.g. "April 2026"
     * @param dayStr         e.g. "21"
     */
    public LocalDate buildDate(String monthYearLabel, String dayStr) {
        if (monthYearLabel == null || dayStr == null) return null;
        try {
            String[] parts = monthYearLabel.split(" ");
            Month    month = Month.valueOf(parts[0].toUpperCase(Locale.ENGLISH));
            int      year  = Integer.parseInt(parts[1]);
            int      day   = Integer.parseInt(dayStr);
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null;
        }
    }

    // ══════════════════════════════════════════════════════
    //  VALIDATION
    // ══════════════════════════════════════════════════════

    /**
     * Validates the fields for a new or edited promotion.
     *
     * @return null if everything is valid, or a human-readable error message
     */
    public String validate(String promoName, String discountType,
                            LocalDate startDate, LocalDate endDate) {
        if (promoName == null || promoName.isBlank())
            return "⚠  Please enter a promotion name.";
        if (discountType == null || discountType.isBlank())
            return "⚠  Please enter a discount type.";
        if (startDate == null)
            return "⚠  Please select a start date.";
        if (endDate == null)
            return "⚠  Please select an end date.";
        if (endDate.isBefore(startDate))
            return "⚠  End date must be on or after the start date.";
        return null;
    }

    // ══════════════════════════════════════════════════════
    //  SAMPLE DATA FALLBACK  (mirrors promotions_setup.sql)
    // ══════════════════════════════════════════════════════

    /**
     * Returns the 10 hard-coded sample promotions used when no DB connection exists.
     */
    public List<String[]> getSampleData() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"PRO-0001", "Summer Splash Sale",       "Percentage (15%)",     "2025-06-01", "2025-06-30"});
        rows.add(new String[]{"PRO-0002", "Mid-Year Mega Deals",      "Percentage (20%)",     "2025-07-01", "2025-07-15"});
        rows.add(new String[]{"PRO-0003", "Back to School Bonanza",   "Fixed (₱200 Off)",     "2025-08-01", "2025-08-31"});
        rows.add(new String[]{"PRO-0004", "Ber Month Kickoff",        "BOGO",                 "2025-09-01", "2025-09-10"});
        rows.add(new String[]{"PRO-0005", "Holiday Early Bird",       "Percentage (25%)",     "2025-10-15", "2025-11-01"});
        rows.add(new String[]{"PRO-0006", "November Payday Treat",    "Fixed (₱500 Off)",     "2025-11-15", "2025-11-16"});
        rows.add(new String[]{"PRO-0007", "Christmas Countdown",      "Percentage (30%)",     "2025-12-01", "2025-12-25"});
        rows.add(new String[]{"PRO-0008", "New Year New Savings",     "Percentage (10%)",     "2026-01-01", "2026-01-07"});
        rows.add(new String[]{"PRO-0009", "Valentine's Day Special",  "Fixed (₱150 Off)",     "2026-02-10", "2026-02-14"});
        rows.add(new String[]{"PRO-0010", "Anniversary Grand Sale",   "BOGO + Free Shipping", "2026-03-01", "2026-03-31"});
        return rows;
    }
}