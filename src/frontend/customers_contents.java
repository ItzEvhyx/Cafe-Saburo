package frontend;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class customers_contents {

    // ══════════════════════════════════════════════════════
    //  LAYOUT CONSTANTS
    // ══════════════════════════════════════════════════════
    private static final double TOP_PADDING  = 20;
    private static final double SIDE_PADDING = 24;
    private static final double HEADER_H     = 56;

    private static final double COL_CUST_ID   = 0.20;
    private static final double COL_CUST_NAME = 0.30;
    private static final double COL_ORDER_ID  = 0.25;
    private static final double COL_LOYALTY   = 0.25;

    private static final double ROW_H        = 44;
    private static final double HEADER_ROW_H = 46;
    private static final double CHECKBOX_COL = 48;

    // ── Fixed modal dimensions ────────────────────────────
    private static final double MODAL_W = 440;
    private static final double MODAL_H = 260;

    // ══════════════════════════════════════════════════════
    //  STYLE CONSTANTS
    // ══════════════════════════════════════════════════════
    private static final String ACCENT       = "#882F39";
    private static final String FONT_FAMILY  = "Aleo";
    private static final String TABLE_BORDER = "#882F39";
    private static final String ROW_ALT_BG   = "#FDF5F6";
    private static final String ROW_WHITE_BG = "white";
    private static final String HEADER_BG    = "#F5E8EA";

    private static final String PILL_GOLD_BG   = "#FFF3CD";
    private static final String PILL_GOLD_FG   = "#856404";
    private static final String PILL_SILVER_BG = "#E2E3E5";
    private static final String PILL_SILVER_FG = "#383D41";
    private static final String PILL_BRONZE_BG = "#F5E6D3";
    private static final String PILL_BRONZE_FG = "#7D4E1B";
    private static final String PILL_NONE_BG   = "#F8F9FA";
    private static final String PILL_NONE_FG   = "#6C757D";

    // ══════════════════════════════════════════════════════
    //  STATE
    // ══════════════════════════════════════════════════════
    private final double     totalW;
    private final double     totalH;
    private final Connection conn;

    private String         currentTab  = "active";
    private boolean        archiveMode = false;
    private String         searchQuery = "";
    private Pane           root;
    private StackPane      stackRoot;
    private ScrollPane     tableScroll;
    // rows: [customerId, customerName, latestOrderId, loyaltyPts]
    private List<String[]> cachedRows  = new ArrayList<>();
    private Set<String>    selectedIds = new HashSet<>();

    private Label     activeTabBtn;
    private Label     archivedTabBtn;
    private Label     archiveBtn;
    private Label     archiveAllBtn;
    private Label     confirmBtn;
    private Label     deleteBtn;
    private Label     exportCsvBtn;
    private TextField searchField;
    private HBox      searchBar;

    // ── Layout values needed for repositioning ────────────
    private double btnY;
    private double gap;
    private double searchW;
    private double activeTabX;
    private double archAllX;
    private double confirmX;

    private static boolean fontsLoaded = false;

    private static void loadFonts() {
        if (fontsLoaded) return;
        String[] variants = {
            "Aleo-Black","Aleo-BlackItalic","Aleo-Bold","Aleo-BoldItalic",
            "Aleo-ExtraBold","Aleo-ExtraBoldItalic","Aleo-ExtraLight","Aleo-ExtraLightItalic",
            "Aleo-Italic","Aleo-Light","Aleo-LightItalic","Aleo-Medium","Aleo-MediumItalic",
            "Aleo-Regular","Aleo-SemiBold","Aleo-SemiBoldItalic","Aleo-Thin","Aleo-ThinItalic"
        };
        for (String v : variants) Font.loadFont("file:assets/fonts/" + v + ".ttf", 12);
        fontsLoaded = true;
    }

    public customers_contents(double totalW, double totalH, Connection conn) {
        this.totalW = totalW;
        this.totalH = totalH;
        this.conn   = conn;
        loadFonts();
    }

    // ── Reposition search bar depending on archiveMode ────
    // When archiveMode is ON:  right edge clamps to left of archiveAll button
    // When archiveMode is OFF: right edge clamps to left of activeTab button
    private void repositionSearchBar() {
        if (searchBar == null) return;
        double rightEdge = archiveMode ? (archAllX - gap) : (activeTabX - gap);
        double newX = rightEdge - searchW;
        searchBar.setLayoutX(newX);
    }

    // ══════════════════════════════════════════════════════
    //  PUBLIC LIVE-UPDATE API
    // ══════════════════════════════════════════════════════
    public void prependOrUpdateCustomer(String customerId, String customerName, String orderId) {
        if (!currentTab.equals("active")) return;

        boolean found = false;
        for (String[] row : cachedRows) {
            if (row[0].equals(customerId)) {
                row[2] = orderId;
                int pts = 0;
                try { pts = Integer.parseInt(row[3]); } catch (NumberFormatException ignored) {}
                row[3] = String.valueOf(pts + 10);
                found = true;
                break;
            }
        }

        if (!found) {
            cachedRows.add(0, new String[]{
                customerId,
                customerName,
                orderId,
                "10"
            });
        }

        cachedRows.sort((a, b) -> {
            int ptsA = 0, ptsB = 0;
            try { ptsA = Integer.parseInt(a[3]); } catch (NumberFormatException ignored) {}
            try { ptsB = Integer.parseInt(b[3]); } catch (NumberFormatException ignored) {}
            if (ptsB != ptsA) return Integer.compare(ptsB, ptsA);
            return a[1].compareToIgnoreCase(b[1]);
        });

        rebuildTable();
    }

    // ══════════════════════════════════════════════════════
    //  FETCH ROWS
    // ══════════════════════════════════════════════════════
    private List<String[]> fetchCustomers(String tab) {
        List<String[]> rows = new ArrayList<>();
        if (conn == null) return rows;
        try { if (conn.isClosed()) return rows; } catch (Exception e) { return rows; }

        String sql =
            "SELECT " +
            "    c.customer_id, " +
            "    c.customer_name, " +
            "    COALESCE(o.latest_order_id, 'N/A') AS latest_order_id, " +
            "    COALESCE(o.order_count * 10, 0)    AS loyalty_pts " +
            "FROM dbo.Customers AS c " +
            "LEFT JOIN ( " +
            "    SELECT customer_id, COUNT(order_id) AS order_count, MAX(order_id) AS latest_order_id " +
            "    FROM dbo.Orders WHERE is_deleted = 0 GROUP BY customer_id " +
            ") AS o ON c.customer_id = o.customer_id " +
            "WHERE c.is_deleted = 0 AND c.status = ? " +
            "ORDER BY loyalty_pts DESC, c.customer_name ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tab);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("customer_id")     != null ? rs.getString("customer_id")     : "—",
                    rs.getString("customer_name")   != null ? rs.getString("customer_name")   : "—",
                    rs.getString("latest_order_id") != null ? rs.getString("latest_order_id") : "N/A",
                    String.valueOf(rs.getInt("loyalty_pts"))
                });
            }
            rs.close();
        } catch (Exception e) { e.printStackTrace(); }
        return rows;
    }

    // ── Filtered rows based on current searchQuery ────────
    // Searches by customer name (index 1) or order ID (index 2)
    private List<String[]> getFilteredRows() {
        if (searchQuery == null || searchQuery.isBlank()) return cachedRows;
        String q = searchQuery.trim().toLowerCase();
        List<String[]> filtered = new ArrayList<>();
        for (String[] row : cachedRows) {
            if (row[1].toLowerCase().contains(q) || row[2].toLowerCase().contains(q))
                filtered.add(row);
        }
        return filtered;
    }

    // ══════════════════════════════════════════════════════
    //  DB OPERATIONS
    // ══════════════════════════════════════════════════════
    private void archiveSelected(Set<String> ids) {
        if (conn == null || ids.isEmpty()) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Orders SET status = 'archived' WHERE customer_id = ? AND is_deleted = 0")) {
                ps.setString(1, id); ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Customers SET status = 'archived' WHERE customer_id = ?")) {
                ps.setString(1, id); ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void restoreSelected(Set<String> ids) {
        if (conn == null || ids.isEmpty()) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Orders SET status = 'active' WHERE customer_id = ? AND is_deleted = 0")) {
                ps.setString(1, id); ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Customers SET status = 'active' WHERE customer_id = ?")) {
                ps.setString(1, id); ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void hardDeleteAll() {
        if (conn == null) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM dbo.Orders WHERE customer_id IN " +
                "(SELECT customer_id FROM dbo.Customers WHERE status = ?)")) {
            ps.setString(1, currentTab); ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM dbo.Customers WHERE status = ?")) {
            ps.setString(1, currentTab); ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════
    //  CSV EXPORT
    // ══════════════════════════════════════════════════════
    private void exportCsv() {
        List<String[]> rows = getFilteredRows();
        if (rows.isEmpty()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Customer List as CSV");
        chooser.setInitialFileName("customers_" + currentTab + ".csv");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        Stage stage = null;
        try { stage = (Stage) root.getScene().getWindow(); } catch (Exception ignored) {}

        File file = (stage != null) ? chooser.showSaveDialog(stage) : chooser.showSaveDialog(null);
        if (file == null) return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("Customer ID,Customer Name,Order ID,Loyalty Pts");
            writer.newLine();
            for (String[] row : rows) {
                writer.write(
                    escapeCsv(row[0]) + "," +
                    escapeCsv(row[1]) + "," +
                    escapeCsv(row[2]) + "," +
                    escapeCsv(row[3])
                );
                writer.newLine();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n"))
            return "\"" + value.replace("\"", "\"\"") + "\"";
        return value;
    }

    // ══════════════════════════════════════════════════════
    //  MAIN VIEW
    // ══════════════════════════════════════════════════════
    public Pane getView() {
        stackRoot = new StackPane();
        stackRoot.setPrefWidth(totalW);
        stackRoot.setPrefHeight(totalH);
        stackRoot.setAlignment(Pos.TOP_LEFT);

        root = new Pane();
        root.setPrefWidth(totalW);
        root.setPrefHeight(totalH);

        double btnH    = 36;
        btnY           = TOP_PADDING + 10;
        double iconW   = 36;
        gap            = 8;
        double tabW    = 90;
        double archAllW = 100;
        double confirmW = 90;
        double csvW    = 120;
        searchW        = 200;

        Label title = new Label("Customer List");
        title.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 36px;" +
            "-fx-font-weight: 800;" +
            "-fx-text-fill: " + ACCENT + ";"
        );

        FontIcon boxIcon = new FontIcon(FontAwesomeSolid.ARCHIVE);
        boxIcon.setIconSize(15);
        boxIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));
        archiveBtn = new Label();
        archiveBtn.setGraphic(boxIcon);
        archiveBtn.setCursor(javafx.scene.Cursor.HAND);
        archiveBtn.setStyle(archiveBtnStyle(false));
        archiveBtn.setPrefHeight(btnH);
        archiveBtn.setPrefWidth(iconW);
        archiveBtn.setAlignment(Pos.CENTER);
        archiveBtn.setOnMouseEntered(e -> archiveBtn.setStyle(archiveBtnStyle(archiveMode)));
        archiveBtn.setOnMouseExited(e  -> archiveBtn.setStyle(archiveBtnStyle(archiveMode)));
        archiveBtn.setOnMouseClicked(e -> toggleArchiveMode());

        HBox titleRow = new HBox(gap, title, archiveBtn);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.setLayoutX(SIDE_PADDING);
        titleRow.setLayoutY(TOP_PADDING);
        titleRow.setPrefHeight(HEADER_H);

        // ── Right-side button layout (right → left) ───────
        // delete | gap | exportCsv | gap | archivedTab | gap | activeTab | gap | confirm | gap | archiveAll | gap | search
        double deleteX      = totalW - SIDE_PADDING - iconW;
        double exportCsvX   = deleteX      - gap - csvW;
        double archivedTabX = exportCsvX   - gap - tabW;
        activeTabX          = archivedTabX - gap - tabW;
        confirmX            = activeTabX   - gap - confirmW;
        archAllX            = confirmX     - gap - archAllW;

        // Initial search bar position (archive mode OFF → clamp to left of activeTab)
        double searchRightEdge = activeTabX - gap;
        double searchX         = searchRightEdge - searchW;

        // ── Delete button ─────────────────────────────────
        deleteBtn = new Label();
        FontIcon trashIcon = new FontIcon(FontAwesomeSolid.TRASH_ALT);
        trashIcon.setIconSize(15);
        trashIcon.setIconColor(javafx.scene.paint.Color.web("#721C24"));
        deleteBtn.setGraphic(trashIcon);
        deleteBtn.setCursor(javafx.scene.Cursor.HAND);
        deleteBtn.setStyle(deleteBtnStyle(false));
        deleteBtn.setLayoutX(deleteX);
        deleteBtn.setLayoutY(btnY);
        deleteBtn.setPrefHeight(btnH);
        deleteBtn.setPrefWidth(iconW);
        deleteBtn.setAlignment(Pos.CENTER);
        deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(deleteBtnStyle(true)));
        deleteBtn.setOnMouseExited(e  -> deleteBtn.setStyle(deleteBtnStyle(false)));
        deleteBtn.setOnMouseClicked(e ->
            stackRoot.getChildren().add(buildConfirmModal(
                "Customers (" + currentTab + ")",
                "This will permanently remove all customers and their orders in this view.\nThis action cannot be undone.",
                () -> {
                    hardDeleteAll();
                    cachedRows.clear();
                    selectedIds.clear();
                    rebuildTable();
                }
            ))
        );

        // ── Export CSV button ─────────────────────────────
        exportCsvBtn = new Label("Export CSV");
        FontIcon csvIcon = new FontIcon(FontAwesomeSolid.FILE_DOWNLOAD);
        csvIcon.setIconSize(13);
        csvIcon.setIconColor(javafx.scene.paint.Color.web("#155724"));
        exportCsvBtn.setGraphic(csvIcon);
        exportCsvBtn.setGraphicTextGap(6);
        exportCsvBtn.setCursor(javafx.scene.Cursor.HAND);
        exportCsvBtn.setStyle(exportCsvBtnStyle(false));
        exportCsvBtn.setLayoutX(exportCsvX);
        exportCsvBtn.setLayoutY(btnY);
        exportCsvBtn.setPrefHeight(btnH);
        exportCsvBtn.setPrefWidth(csvW);
        exportCsvBtn.setAlignment(Pos.CENTER);
        exportCsvBtn.setOnMouseEntered(e -> exportCsvBtn.setStyle(exportCsvBtnStyle(true)));
        exportCsvBtn.setOnMouseExited(e  -> exportCsvBtn.setStyle(exportCsvBtnStyle(false)));
        exportCsvBtn.setOnMouseClicked(e -> exportCsv());

        // ── Tab buttons ───────────────────────────────────
        activeTabBtn = buildTabLabel("Active", true);
        activeTabBtn.setLayoutX(activeTabX);
        activeTabBtn.setLayoutY(btnY);

        archivedTabBtn = buildTabLabel("Archived", false);
        archivedTabBtn.setLayoutX(archivedTabX);
        archivedTabBtn.setLayoutY(btnY);

        activeTabBtn.setOnMouseEntered(e -> {
            if (!currentTab.equals("active")) activeTabBtn.setStyle(tabBtnHoverStyle());
        });
        activeTabBtn.setOnMouseExited(e -> activeTabBtn.setStyle(tabBtnStyle(currentTab.equals("active"))));
        activeTabBtn.setOnMouseClicked(e -> switchTab("active"));

        archivedTabBtn.setOnMouseEntered(e -> {
            if (!currentTab.equals("archived")) archivedTabBtn.setStyle(tabBtnHoverStyle());
        });
        archivedTabBtn.setOnMouseExited(e -> archivedTabBtn.setStyle(tabBtnStyle(currentTab.equals("archived"))));
        archivedTabBtn.setOnMouseClicked(e -> switchTab("archived"));

        // ── Archive All button ────────────────────────────
        archiveAllBtn = new Label("Archive All");
        archiveAllBtn.setCursor(javafx.scene.Cursor.HAND);
        archiveAllBtn.setPrefWidth(archAllW);
        archiveAllBtn.setPrefHeight(btnH);
        archiveAllBtn.setAlignment(Pos.CENTER);
        archiveAllBtn.setStyle(archiveAllBtnStyle(false));
        archiveAllBtn.setVisible(false);
        archiveAllBtn.setLayoutX(archAllX);
        archiveAllBtn.setLayoutY(btnY);
        archiveAllBtn.setOnMouseEntered(e -> archiveAllBtn.setStyle(archiveAllBtnStyle(true)));
        archiveAllBtn.setOnMouseExited(e  -> archiveAllBtn.setStyle(archiveAllBtnStyle(false)));
        archiveAllBtn.setOnMouseClicked(e -> {
            selectedIds.clear();
            for (String[] row : cachedRows) selectedIds.add(row[0]);
            rebuildTable();
        });

        // ── Confirm button ────────────────────────────────
        confirmBtn = new Label("Confirm");
        confirmBtn.setCursor(javafx.scene.Cursor.HAND);
        confirmBtn.setPrefWidth(confirmW);
        confirmBtn.setPrefHeight(btnH);
        confirmBtn.setAlignment(Pos.CENTER);
        confirmBtn.setStyle(confirmBtnStyle(false));
        confirmBtn.setVisible(false);
        confirmBtn.setLayoutX(confirmX);
        confirmBtn.setLayoutY(btnY);
        confirmBtn.setOnMouseEntered(e -> confirmBtn.setStyle(confirmBtnStyle(true)));
        confirmBtn.setOnMouseExited(e  -> confirmBtn.setStyle(confirmBtnStyle(false)));
        confirmBtn.setOnMouseClicked(e -> {
            if (selectedIds.isEmpty()) return;
            if (currentTab.equals("active")) archiveSelected(selectedIds);
            else restoreSelected(selectedIds);
            selectedIds.clear();
            archiveMode = false;
            updateArchiveBtnIcon();
            archiveAllBtn.setVisible(false);
            confirmBtn.setVisible(false);
            archiveBtn.setStyle(archiveBtnStyle(false));
            repositionSearchBar();
            cachedRows = fetchCustomers(currentTab);
            rebuildTable();
        });

        // ── Search bar ────────────────────────────────────
        FontIcon searchIcon = new FontIcon(FontAwesomeSolid.SEARCH);
        searchIcon.setIconSize(14);
        searchIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        searchField = new TextField();
        searchField.setPromptText("Search name or order ID...");
        searchField.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;" +
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #333333;" +
            "-fx-prompt-text-fill: #AAAAAA;"
        );
        searchField.setPrefWidth(searchW - 42);

        searchBar = new HBox(6, searchIcon, searchField);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(0, 10, 0, 12));
        searchBar.setPrefWidth(searchW);
        searchBar.setPrefHeight(btnH);
        searchBar.setLayoutX(searchX);
        searchBar.setLayoutY(btnY);
        searchBar.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: " + ACCENT + ";" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 20;"
        );

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchQuery = newVal == null ? "" : newVal.trim();
            rebuildTable();
        });

        // ── Table ─────────────────────────────────────────
        double tableY = TOP_PADDING + HEADER_H + 10;
        double tableW = totalW - SIDE_PADDING * 2;
        double tableH = totalH - tableY - SIDE_PADDING;

        cachedRows  = fetchCustomers("active");
        tableScroll = buildScrollPane(tableW, tableH, tableY);

        root.getChildren().addAll(
            titleRow, searchBar, archiveAllBtn, confirmBtn,
            activeTabBtn, archivedTabBtn, exportCsvBtn, deleteBtn, tableScroll
        );
        stackRoot.getChildren().add(root);
        return stackRoot;
    }

    // ══════════════════════════════════════════════════════
    //  ARCHIVE MODE TOGGLE
    // ══════════════════════════════════════════════════════
    private void toggleArchiveMode() {
        archiveMode = !archiveMode;
        selectedIds.clear();
        updateArchiveBtnIcon();
        archiveAllBtn.setText(currentTab.equals("archived") ? "Restore All" : "Archive All");
        archiveAllBtn.setVisible(archiveMode);
        confirmBtn.setVisible(archiveMode);
        archiveBtn.setStyle(archiveBtnStyle(archiveMode));
        repositionSearchBar();
        rebuildTable();
    }

    private void updateArchiveBtnIcon() {
        FontIcon icon = archiveMode
            ? new FontIcon(FontAwesomeSolid.TIMES)
            : new FontIcon(FontAwesomeSolid.ARCHIVE);
        icon.setIconSize(15);
        icon.setIconColor(javafx.scene.paint.Color.web(archiveMode ? "#155724" : ACCENT));
        archiveBtn.setGraphic(icon);
    }

    // ══════════════════════════════════════════════════════
    //  TAB SWITCHING
    // ══════════════════════════════════════════════════════
    private void switchTab(String tab) {
        if (currentTab.equals(tab)) return;
        currentTab  = tab;
        archiveMode = false;
        selectedIds.clear();
        searchQuery = "";
        if (searchField != null) searchField.clear();
        updateArchiveBtnIcon();
        archiveAllBtn.setText(tab.equals("archived") ? "Restore All" : "Archive All");
        archiveAllBtn.setVisible(false);
        confirmBtn.setVisible(false);
        archiveBtn.setStyle(archiveBtnStyle(false));
        activeTabBtn.setStyle(tabBtnStyle(tab.equals("active")));
        archivedTabBtn.setStyle(tabBtnStyle(tab.equals("archived")));
        repositionSearchBar();
        cachedRows = fetchCustomers(tab);
        rebuildTable();
    }

    private Label buildTabLabel(String text, boolean selected) {
        Label lbl = new Label(text);
        lbl.setCursor(javafx.scene.Cursor.HAND);
        lbl.setPrefWidth(90);
        lbl.setPrefHeight(36);
        lbl.setAlignment(Pos.CENTER);
        lbl.setStyle(tabBtnStyle(selected));
        return lbl;
    }

    // ══════════════════════════════════════════════════════
    //  CONFIRMATION MODAL
    // ══════════════════════════════════════════════════════
    private Pane buildConfirmModal(String context, String subMessage, Runnable onConfirm) {
        Pane overlay = new Pane();
        overlay.setPrefWidth(totalW); overlay.setPrefHeight(totalH);
        overlay.setMinWidth(totalW);  overlay.setMinHeight(totalH);
        overlay.setMaxWidth(totalW);  overlay.setMaxHeight(totalH);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.45);");

        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(MODAL_W); card.setMinWidth(MODAL_W); card.setMaxWidth(MODAL_W);
        card.setPrefHeight(MODAL_H); card.setMinHeight(MODAL_H); card.setMaxHeight(MODAL_H);
        card.setPadding(new Insets(36, 40, 32, 40));
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 14;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0, 0, 6);"
        );

        FontIcon warnIcon = new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
        warnIcon.setIconSize(28);
        warnIcon.setIconColor(javafx.scene.paint.Color.web("#882F39"));

        Label heading = new Label("Are you sure you want to delete\nall entries for " + context + "?");
        heading.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #222222;" +
            "-fx-text-alignment: center;" +
            "-fx-alignment: center;"
        );
        heading.setAlignment(Pos.CENTER);
        heading.setWrapText(true);
        heading.setMaxWidth(MODAL_W - 80);

        Label sub = new Label(subMessage);
        sub.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #777777;" +
            "-fx-text-alignment: center;" +
            "-fx-alignment: center;"
        );
        sub.setAlignment(Pos.CENTER);
        sub.setWrapText(true);
        sub.setMaxWidth(MODAL_W - 80);

        Label noBtn = new Label("No, cancel");
        noBtn.setCursor(javafx.scene.Cursor.HAND);
        noBtn.setPrefWidth(140); noBtn.setPrefHeight(38);
        noBtn.setAlignment(Pos.CENTER);
        noBtn.setStyle(modalNoBtnStyle(false));
        noBtn.setOnMouseEntered(e -> noBtn.setStyle(modalNoBtnStyle(true)));
        noBtn.setOnMouseExited(e  -> noBtn.setStyle(modalNoBtnStyle(false)));
        noBtn.setOnMouseClicked(e -> stackRoot.getChildren().remove(overlay));

        Label yesBtn = new Label("Yes, delete");
        yesBtn.setCursor(javafx.scene.Cursor.HAND);
        yesBtn.setPrefWidth(140); yesBtn.setPrefHeight(38);
        yesBtn.setAlignment(Pos.CENTER);
        yesBtn.setStyle(modalYesBtnStyle(false));
        yesBtn.setOnMouseEntered(e -> yesBtn.setStyle(modalYesBtnStyle(true)));
        yesBtn.setOnMouseExited(e  -> yesBtn.setStyle(modalYesBtnStyle(false)));
        yesBtn.setOnMouseClicked(e -> {
            stackRoot.getChildren().remove(overlay);
            onConfirm.run();
        });

        HBox btnRow = new HBox(16, noBtn, yesBtn);
        btnRow.setAlignment(Pos.CENTER);
        card.getChildren().addAll(warnIcon, heading, sub, btnRow);

        StackPane centred = new StackPane(card);
        centred.setPrefWidth(totalW); centred.setPrefHeight(totalH);
        centred.setMinWidth(totalW);  centred.setMinHeight(totalH);
        centred.setMaxWidth(totalW);  centred.setMaxHeight(totalH);
        centred.setAlignment(Pos.CENTER);
        overlay.getChildren().add(centred);
        return overlay;
    }

    // ══════════════════════════════════════════════════════
    //  REBUILD TABLE
    // ══════════════════════════════════════════════════════
    private void rebuildTable() {
        double tableY = TOP_PADDING + HEADER_H + 10;
        double tableW = totalW - SIDE_PADDING * 2;
        double tableH = totalH - tableY - SIDE_PADDING;
        root.getChildren().remove(tableScroll);
        tableScroll = buildScrollPane(tableW, tableH, tableY);
        root.getChildren().add(tableScroll);
    }

    private ScrollPane buildScrollPane(double tableW, double tableH, double tableY) {
        VBox tableBox = buildTable(tableW, getFilteredRows());
        ScrollPane sp = new ScrollPane(tableBox);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setFitToWidth(true);
        sp.setPannable(true);
        sp.setStyle(
            "-fx-background: transparent;" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;" +
            "-fx-padding: 0;"
        );
        sp.setPrefWidth(tableW);
        sp.setPrefHeight(tableH);
        sp.setLayoutX(SIDE_PADDING);
        sp.setLayoutY(tableY);
        return sp;
    }

    // ══════════════════════════════════════════════════════
    //  TABLE BUILDER
    // ══════════════════════════════════════════════════════
    private VBox buildTable(double tableW, List<String[]> rows) {
        double dataW = archiveMode ? tableW - CHECKBOX_COL : tableW;

        VBox table = new VBox(0);
        table.setStyle(
            "-fx-border-color: " + TABLE_BORDER + ";" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 10;" +
            "-fx-background-color: white;" +
            "-fx-background-radius: 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 10, 0, 0, 3);"
        );
        table.getChildren().add(buildHeaderRow(tableW, dataW));

        if (rows.isEmpty()) {
            String msg = !searchQuery.isBlank() ? "No results found for \"" + searchQuery + "\"."
                       : currentTab.equals("archived") ? "No archived customers." : "No customers found.";
            Label empty = new Label(msg);
            empty.setStyle(
                "-fx-font-family: '" + FONT_FAMILY + "';" +
                "-fx-font-size: 14px;" +
                "-fx-text-fill: #AAAAAA;" +
                "-fx-padding: 24 0 24 16;"
            );
            table.getChildren().add(empty);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                String[] row    = rows.get(i);
                boolean  isLast = (i == rows.size() - 1);
                table.getChildren().add(
                    buildDataRow(row[0], row[1], row[2], row[3],
                        i % 2 == 0 ? ROW_WHITE_BG : ROW_ALT_BG, tableW, dataW, isLast)
                );
            }
        }
        return table;
    }

    private HBox buildHeaderRow(double tableW, double dataW) {
        HBox row = new HBox(0);
        row.setPrefHeight(HEADER_ROW_H);
        row.setStyle(
            "-fx-background-color: " + HEADER_BG + ";" +
            "-fx-background-radius: 10 10 0 0;" +
            "-fx-border-color: transparent transparent " + TABLE_BORDER + " transparent;" +
            "-fx-border-width: 0 0 1.5 0;"
        );
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(
            buildHeaderCell("Customer ID",   dataW * COL_CUST_ID),
            buildColDivider(),
            buildHeaderCell("Customer Name", dataW * COL_CUST_NAME),
            buildColDivider(),
            buildHeaderCell("Order ID",      dataW * COL_ORDER_ID),
            buildColDivider(),
            buildHeaderCell("Loyalty Pts",   dataW * COL_LOYALTY)
        );
        if (archiveMode) {
            row.getChildren().add(buildColDivider());
            Label cbHeader = buildHeaderCell("", CHECKBOX_COL);
            cbHeader.setAlignment(Pos.CENTER);
            row.getChildren().add(cbHeader);
        }
        return row;
    }

    private Label buildHeaderCell(String text, double width) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(width);
        lbl.setPrefHeight(HEADER_ROW_H);
        lbl.setPadding(new Insets(0, 0, 0, 16));
        lbl.setAlignment(Pos.CENTER_LEFT);
        lbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + ACCENT + ";"
        );
        return lbl;
    }

    private HBox buildDataRow(String custId, String custName, String orderId,
                               String loyaltyPts, String bg, double tableW, double dataW, boolean isLast) {
        HBox row = new HBox(0);
        row.setPrefHeight(ROW_H);
        row.setAlignment(Pos.CENTER_LEFT);

        String  bottomRadius = isLast ? "0 0 10 10" : "0";
        String  borderBottom = isLast ? "0" : "1";
        boolean selected     = selectedIds.contains(custId);
        String  rowBg        = selected ? "#FDE8EA" : bg;

        row.setStyle(rowStyle(rowBg, bottomRadius, borderBottom));
        row.setOnMouseEntered(e -> {
            if (!selectedIds.contains(custId)) row.setStyle(rowStyle("#F5E8EA", bottomRadius, borderBottom));
        });
        row.setOnMouseExited(e -> {
            String cur = selectedIds.contains(custId) ? "#FDE8EA" : bg;
            row.setStyle(rowStyle(cur, bottomRadius, borderBottom));
        });

        row.getChildren().addAll(
            buildTextCell(custId,   dataW * COL_CUST_ID,   true),
            buildColDivider(),
            buildTextCell(custName, dataW * COL_CUST_NAME, false),
            buildColDivider(),
            buildTextCell(orderId,  dataW * COL_ORDER_ID,  false),
            buildColDivider(),
            buildLoyaltyCell(loyaltyPts, dataW * COL_LOYALTY)
        );

        if (archiveMode) {
            row.getChildren().add(buildColDivider());
            CheckBox cb = new CheckBox();
            cb.setSelected(selected);
            cb.setStyle("-fx-cursor: hand;");
            cb.setOnAction(e -> {
                if (cb.isSelected()) {
                    selectedIds.add(custId);
                    row.setStyle(rowStyle("#FDE8EA", bottomRadius, borderBottom));
                } else {
                    selectedIds.remove(custId);
                    row.setStyle(rowStyle(bg, bottomRadius, borderBottom));
                }
            });
            HBox cbCell = new HBox(cb);
            cbCell.setPrefWidth(CHECKBOX_COL);
            cbCell.setPrefHeight(ROW_H);
            cbCell.setAlignment(Pos.CENTER);
            row.getChildren().add(cbCell);
        }
        return row;
    }

    // ══════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════
    private String rowStyle(String bg, String bottomRadius, String borderBottom) {
        return "-fx-background-color: " + bg + ";" +
               "-fx-background-radius: " + bottomRadius + ";" +
               "-fx-border-color: transparent transparent " + TABLE_BORDER + " transparent;" +
               "-fx-border-width: 0 0 " + borderBottom + " 0;";
    }

    private Label buildTextCell(String text, double width, boolean bold) {
        Label lbl = new Label(text != null ? text : "—");
        lbl.setPrefWidth(width);
        lbl.setPrefHeight(ROW_H);
        lbl.setPadding(new Insets(0, 0, 0, 16));
        lbl.setAlignment(Pos.CENTER_LEFT);
        lbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: " + (bold ? "bold" : "normal") + ";" +
            "-fx-text-fill: #333333;"
        );
        return lbl;
    }

    private HBox buildLoyaltyCell(String loyaltyPts, double width) {
        int pts = 0;
        try { pts = Integer.parseInt(loyaltyPts); } catch (NumberFormatException ignored) {}
        String[] colors = loyaltyPillColors(pts);
        Label pill = new Label(pts + " pts");
        pill.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + colors[1] + ";" +
            "-fx-background-color: " + colors[0] + ";" +
            "-fx-background-radius: 20;" +
            "-fx-padding: 4 14 4 14;"
        );
        HBox cell = new HBox(pill);
        cell.setPrefWidth(width);
        cell.setPrefHeight(ROW_H);
        cell.setPadding(new Insets(0, 0, 0, 16));
        cell.setAlignment(Pos.CENTER_LEFT);
        return cell;
    }

    private String[] loyaltyPillColors(int pts) {
        if (pts >= 50) return new String[]{ PILL_GOLD_BG,   PILL_GOLD_FG   };
        if (pts >= 30) return new String[]{ PILL_SILVER_BG, PILL_SILVER_FG };
        if (pts >= 10) return new String[]{ PILL_BRONZE_BG, PILL_BRONZE_FG };
        return             new String[]{ PILL_NONE_BG,   PILL_NONE_FG   };
    }

    private Region buildColDivider() {
        Region div = new Region();
        div.setPrefWidth(1.5);
        div.setMinWidth(1.5);
        div.setMaxWidth(1.5);
        div.setStyle("-fx-background-color: " + TABLE_BORDER + "; -fx-opacity: 0.35;");
        VBox.setVgrow(div, Priority.ALWAYS);
        return div;
    }

    // ══════════════════════════════════════════════════════
    //  STYLE HELPERS
    // ══════════════════════════════════════════════════════
    private String tabBtnStyle(boolean selected) {
        return selected
            ? "-fx-background-color: " + ACCENT + ";" +
              "-fx-background-radius: 8;" +
              "-fx-font-family: '" + FONT_FAMILY + "';" +
              "-fx-font-size: 13px;" +
              "-fx-font-weight: bold;" +
              "-fx-text-fill: white;" +
              "-fx-cursor: hand;"
            : "-fx-background-color: #F5E8EA;" +
              "-fx-background-radius: 8;" +
              "-fx-border-color: " + ACCENT + ";" +
              "-fx-border-radius: 8;" +
              "-fx-border-width: 1.5;" +
              "-fx-font-family: '" + FONT_FAMILY + "';" +
              "-fx-font-size: 13px;" +
              "-fx-font-weight: bold;" +
              "-fx-text-fill: " + ACCENT + ";" +
              "-fx-cursor: hand;";
    }

    private String tabBtnHoverStyle() {
        return "-fx-background-color: #EDD5D8;" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: " + ACCENT + ";" +
               "-fx-border-radius: 8;" +
               "-fx-border-width: 1.5;" +
               "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 13px;" +
               "-fx-font-weight: bold;" +
               "-fx-text-fill: " + ACCENT + ";" +
               "-fx-cursor: hand;";
    }

    private String archiveBtnStyle(boolean active) {
        return "-fx-background-color: " + (active ? "#D4EDDA" : "#F5E8EA") + ";" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: " + (active ? "#155724" : ACCENT) + ";" +
               "-fx-border-radius: 8;" +
               "-fx-border-width: 1.5;" +
               "-fx-cursor: hand;";
    }

    private String archiveAllBtnStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#EDD5D8" : "#F5E8EA") + ";" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: " + ACCENT + ";" +
               "-fx-border-radius: 8;" +
               "-fx-border-width: 1.5;" +
               "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 13px;" +
               "-fx-font-weight: bold;" +
               "-fx-text-fill: " + ACCENT + ";" +
               "-fx-cursor: hand;";
    }

    private String confirmBtnStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#A93226" : "#882F39") + ";" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: transparent;" +
               "-fx-border-radius: 8;" +
               "-fx-border-width: 0;" +
               "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 13px;" +
               "-fx-font-weight: bold;" +
               "-fx-text-fill: white;" +
               "-fx-cursor: hand;";
    }

    private String deleteBtnStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#F8D7DA" : "#FDF0F1") + ";" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: #721C24;" +
               "-fx-border-radius: 8;" +
               "-fx-border-width: 1.5;" +
               "-fx-cursor: hand;";
    }

    private String exportCsvBtnStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#C3E6CB" : "#D4EDDA") + ";" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: #155724;" +
               "-fx-border-radius: 8;" +
               "-fx-border-width: 1.5;" +
               "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 13px;" +
               "-fx-font-weight: bold;" +
               "-fx-text-fill: #155724;" +
               "-fx-cursor: hand;";
    }

    private String modalNoBtnStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#E9ECEF" : "#F8F9FA") + ";" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: #CCCCCC;" +
               "-fx-border-radius: 8;" +
               "-fx-border-width: 1.5;" +
               "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 13px;" +
               "-fx-font-weight: bold;" +
               "-fx-text-fill: #555555;" +
               "-fx-cursor: hand;";
    }

    private String modalYesBtnStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#A93226" : "#882F39") + ";" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: transparent;" +
               "-fx-border-radius: 8;" +
               "-fx-border-width: 0;" +
               "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 13px;" +
               "-fx-font-weight: bold;" +
               "-fx-text-fill: white;" +
               "-fx-cursor: hand;";
    }
}