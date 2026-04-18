package frontend;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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

/**
 * orders_contents — Order History view.
 *
 * Tabs        : Active (default) | Archived
 * Edit mode   : pen icon toggles inline ComboBox for order_status
 * Archive mode: archive icon toggles checkboxes.
 *               "Archive All" selects all rows. "Confirm" archives/restores selected rows.
 * Delete      : hard delete — removes rows from DB and GUI entirely.
 * Export CSV  : downloads the current tab's rows as a .csv file.
 *
 * Live update : call prependOrder(orderId, customerId, paymentMethod) after a new
 *               order is submitted from menu_contents to insert it at the top of the
 *               active-tab cache without a full re-fetch.
 */
public class orders_contents {

    // ══════════════════════════════════════════════════════
    //  LAYOUT CONSTANTS
    // ══════════════════════════════════════════════════════
    private static final double TOP_PADDING  = 20;
    private static final double SIDE_PADDING = 24;
    private static final double HEADER_H     = 56;

    private static final double COL_ORDER_ID = 0.16;
    private static final double COL_CUST_ID  = 0.24;
    private static final double COL_STATUS   = 0.30;
    private static final double COL_PAYMENT  = 0.30;

    private static final double ROW_H        = 44;
    private static final double HEADER_ROW_H = 46;
    private static final double CHECKBOX_COL = 48;

    // ══════════════════════════════════════════════════════
    //  STYLE CONSTANTS
    // ══════════════════════════════════════════════════════
    private static final String ACCENT       = "#882F39";
    private static final String FONT_FAMILY  = "Aleo";
    private static final String TABLE_BORDER = "#882F39";
    private static final String ROW_ALT_BG   = "#FDF5F6";
    private static final String ROW_WHITE_BG = "white";
    private static final String HEADER_BG    = "#F5E8EA";

    private static final String PILL_PENDING_BG   = "#FFF3CD";
    private static final String PILL_PENDING_FG   = "#856404";
    private static final String PILL_COMPLETED_BG = "#D4EDDA";
    private static final String PILL_COMPLETED_FG = "#155724";
    private static final String PILL_CANCELLED_BG = "#F8D7DA";
    private static final String PILL_CANCELLED_FG = "#721C24";
    private static final String PILL_PREPARING_BG = "#D1ECF1";
    private static final String PILL_PREPARING_FG = "#0C5460";

    // ══════════════════════════════════════════════════════
    //  STATE
    // ══════════════════════════════════════════════════════
    private final double     totalW;
    private final double     totalH;
    private final Connection conn;

    private String         currentTab  = "active";
    private boolean        editMode    = false;
    private boolean        archiveMode = false;
    private Pane           root;
    private StackPane      stackRoot;
    private ScrollPane     tableScroll;
    private List<String[]> cachedRows  = new ArrayList<>();
    private Set<String>    selectedIds = new HashSet<>();

    private Label editBtn;
    private Label archiveBtn;
    private Label archiveAllBtn;
    private Label confirmBtn;
    private Label activeTabBtn;
    private Label archivedTabBtn;
    private Label deleteBtn;
    private Label exportCsvBtn;

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

    public orders_contents(double totalW, double totalH, Connection conn) {
        this.totalW = totalW;
        this.totalH = totalH;
        this.conn   = conn;
        loadFonts();
    }

    // ══════════════════════════════════════════════════════
    //  PUBLIC LIVE-UPDATE API
    // ══════════════════════════════════════════════════════

    /**
     * Called by menu_contents (via the app controller) after a successful order submit.
     * Inserts the new order at the top of cachedRows (matching DB sort: newest first)
     * and rebuilds the table — no DB round-trip needed.
     *
     * Only takes effect when the Active tab is showing; if the user is currently
     * viewing Archived the data will be correct the next time they switch back.
     *
     * @param orderId       Generated order ID (e.g. "ORD-XXXXXXXX")
     * @param customerId    Customer ID of the order owner
     * @param paymentMethod "Cash", "GCash", or "Card"
     */
    public void prependOrder(String orderId, String customerId, String paymentMethod) {
        String[] newRow = new String[]{
            orderId,
            customerId,
            "Pending",
            paymentMethod != null ? paymentMethod : "Cash"
        };

        if (currentTab.equals("active")) {
            cachedRows.add(0, newRow);
            rebuildTable();
        }
        // If on archived tab, just silently add; the row will appear on next switchTab("active").
        else {
            // Keep a shadow list so switching back to active shows it without re-fetch.
            // Simplest correct approach: we'll add it and it will appear on next switch.
            // (The fetch on switchTab will pick it up from the DB anyway.)
        }
    }

    // ══════════════════════════════════════════════════════
    //  FETCH ROWS
    // ══════════════════════════════════════════════════════
    private List<String[]> fetchOrders(String tab) {
        List<String[]> rows = new ArrayList<>();
        if (conn == null) return rows;
        try { if (conn.isClosed()) return rows; } catch (Exception e) { return rows; }

        String sql =
            "SELECT order_id, customer_id, order_status, payment_type " +
            "FROM dbo.Orders " +
            "WHERE is_deleted = 0 AND status = ? " +
            "ORDER BY order_date DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tab);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("order_id")     != null ? rs.getString("order_id")     : "—",
                    rs.getString("customer_id")  != null ? rs.getString("customer_id")  : "—",
                    rs.getString("order_status") != null ? rs.getString("order_status") : "—",
                    rs.getString("payment_type") != null ? rs.getString("payment_type") : "—"
                });
            }
            rs.close();
        } catch (Exception e) { e.printStackTrace(); }
        return rows;
    }

    // ══════════════════════════════════════════════════════
    //  DB OPERATIONS
    // ══════════════════════════════════════════════════════
    private void updateOrderStatus(String orderId, String newStatus) {
        if (conn == null) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE dbo.Orders SET order_status = ? WHERE order_id = ?")) {
            ps.setString(1, newStatus);
            ps.setString(2, orderId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void archiveSelected(Set<String> ids) {
        if (conn == null || ids.isEmpty()) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Orders SET status = 'archived' WHERE order_id = ? AND is_deleted = 0")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void restoreSelected(Set<String> ids) {
        if (conn == null || ids.isEmpty()) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Orders SET status = 'active' WHERE order_id = ? AND is_deleted = 0")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void hardDeleteAll() {
        if (conn == null) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM dbo.Orders WHERE is_deleted = 0 AND status = ?")) {
            ps.setString(1, currentTab);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════
    //  CSV EXPORT
    // ══════════════════════════════════════════════════════
    private void exportCsv() {
        if (cachedRows.isEmpty()) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Order History as CSV");
        chooser.setInitialFileName("orders_" + currentTab + ".csv");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        Stage stage = null;
        try {
            stage = (Stage) root.getScene().getWindow();
        } catch (Exception ignored) {}

        File file = (stage != null) ? chooser.showSaveDialog(stage) : chooser.showSaveDialog(null);
        if (file == null) return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("Order ID,Customer ID,Order Status,Payment Type");
            writer.newLine();
            for (String[] row : cachedRows) {
                writer.write(
                    escapeCsv(row[0]) + "," +
                    escapeCsv(row[1]) + "," +
                    escapeCsv(row[2]) + "," +
                    escapeCsv(row[3])
                );
                writer.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
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

        double btnH     = 36;
        double btnY     = TOP_PADDING + 10;
        double iconW    = 36;
        double gap      = 8;
        double tabW     = 90;
        double archAllW = 100;
        double confirmW = 90;
        double csvW     = 120;

        // ── Title label ───────────────────────────────────
        Label title = new Label("Order History");
        title.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 36px;" +
            "-fx-font-weight: extra-bold;" +
            "-fx-text-fill: " + ACCENT + ";"
        );

        // ── Edit button ───────────────────────────────────
        FontIcon penIcon = new FontIcon(FontAwesomeSolid.PEN);
        penIcon.setIconSize(15);
        penIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));
        editBtn = new Label();
        editBtn.setGraphic(penIcon);
        editBtn.setCursor(javafx.scene.Cursor.HAND);
        editBtn.setStyle(editBtnStyle(false));
        editBtn.setPrefHeight(btnH);
        editBtn.setPrefWidth(iconW);
        editBtn.setAlignment(Pos.CENTER);
        editBtn.setOnMouseEntered(e -> editBtn.setStyle(editBtnHoverStyle(editMode)));
        editBtn.setOnMouseExited(e  -> editBtn.setStyle(editBtnStyle(editMode)));
        editBtn.setOnMouseClicked(e -> {
            editMode = !editMode;
            FontIcon icon = new FontIcon(editMode ? FontAwesomeSolid.CHECK : FontAwesomeSolid.PEN);
            icon.setIconSize(15);
            icon.setIconColor(editMode
                ? javafx.scene.paint.Color.web("#155724")
                : javafx.scene.paint.Color.web(ACCENT));
            editBtn.setGraphic(icon);
            editBtn.setStyle(editBtnStyle(editMode));
            rebuildTable();
        });

        // ── Archive button ────────────────────────────────
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

        HBox titleRow = new HBox(gap, title, editBtn, archiveBtn);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.setLayoutX(SIDE_PADDING);
        titleRow.setLayoutY(TOP_PADDING);
        titleRow.setPrefHeight(HEADER_H);

        // ── Right-side buttons ────────────────────────────
        double deleteX      = totalW - SIDE_PADDING - iconW;
        double exportCsvX   = deleteX - gap - csvW;
        double archivedTabX = exportCsvX - gap - tabW;
        double activeTabX   = archivedTabX - gap - tabW;
        double confirmX     = activeTabX - gap - confirmW;
        double archAllX     = confirmX - gap - archAllW;

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
                "Orders (" + currentTab + ")",
                "This will permanently remove all orders in this view.\nThis action cannot be undone.",
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

        // ── Archive All / Confirm buttons ─────────────────
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
            cachedRows = fetchOrders(currentTab);
            rebuildTable();
        });

        // ── Initial build ─────────────────────────────────
        double tableY = TOP_PADDING + HEADER_H + 10;
        double tableW = totalW - SIDE_PADDING * 2;
        double tableH = totalH - tableY - SIDE_PADDING;

        cachedRows  = fetchOrders("active");
        tableScroll = buildScrollPane(tableW, tableH, tableY);

        root.getChildren().addAll(
            titleRow, archiveAllBtn, confirmBtn,
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
        editMode    = false;
        archiveMode = false;
        selectedIds.clear();

        FontIcon penIcon = new FontIcon(FontAwesomeSolid.PEN);
        penIcon.setIconSize(15);
        penIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));
        editBtn.setGraphic(penIcon);
        editBtn.setStyle(editBtnStyle(false));
        editBtn.setVisible(tab.equals("active"));

        updateArchiveBtnIcon();
        archiveAllBtn.setText(tab.equals("archived") ? "Restore All" : "Archive All");
        archiveAllBtn.setVisible(false);
        confirmBtn.setVisible(false);
        archiveBtn.setStyle(archiveBtnStyle(false));

        activeTabBtn.setStyle(tabBtnStyle(tab.equals("active")));
        archivedTabBtn.setStyle(tabBtnStyle(tab.equals("archived")));
        cachedRows = fetchOrders(tab);
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
        overlay.setPrefWidth(totalW);
        overlay.setPrefHeight(totalH);
        overlay.setMinHeight(totalH);
        overlay.setMaxHeight(totalH);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.45);");

        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(36, 40, 32, 40));
        card.setMaxWidth(440);
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

        Label noBtn = new Label("No, cancel");
        noBtn.setCursor(javafx.scene.Cursor.HAND);
        noBtn.setPrefWidth(140);
        noBtn.setPrefHeight(38);
        noBtn.setAlignment(Pos.CENTER);
        noBtn.setStyle(modalNoBtnStyle(false));
        noBtn.setOnMouseEntered(e -> noBtn.setStyle(modalNoBtnStyle(true)));
        noBtn.setOnMouseExited(e  -> noBtn.setStyle(modalNoBtnStyle(false)));
        noBtn.setOnMouseClicked(e -> stackRoot.getChildren().remove(overlay));

        Label yesBtn = new Label("Yes, delete");
        yesBtn.setCursor(javafx.scene.Cursor.HAND);
        yesBtn.setPrefWidth(140);
        yesBtn.setPrefHeight(38);
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
        centred.setPrefWidth(totalW);
        centred.setPrefHeight(totalH);
        centred.setMinHeight(totalH);
        centred.setMaxHeight(totalH);
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
        VBox tableBox = buildTable(tableW, cachedRows);
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
            String msg = currentTab.equals("archived") ? "No archived orders." : "No orders found.";
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
                String[] order  = rows.get(i);
                boolean  isLast = (i == rows.size() - 1);
                table.getChildren().add(
                    buildDataRow(order[0], order[1], order[2], order[3],
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
            buildHeaderCell("Order ID",     dataW * COL_ORDER_ID),
            buildColDivider(),
            buildHeaderCell("Customer ID",  dataW * COL_CUST_ID),
            buildColDivider(),
            buildHeaderCell("Order Status", dataW * COL_STATUS),
            buildColDivider(),
            buildHeaderCell("Payment Type", dataW * COL_PAYMENT)
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

    private HBox buildDataRow(String orderId, String custId, String status,
                               String payment, String bg, double tableW, double dataW, boolean isLast) {
        HBox row = new HBox(0);
        row.setPrefHeight(ROW_H);
        row.setAlignment(Pos.CENTER_LEFT);

        String  bottomRadius = isLast ? "0 0 10 10" : "0";
        String  borderBottom = isLast ? "0" : "1";
        boolean selected     = selectedIds.contains(orderId);
        String  rowBg        = selected ? "#FDE8EA" : bg;

        row.setStyle(rowStyle(rowBg, bottomRadius, borderBottom));
        row.setOnMouseEntered(e -> {
            if (!selectedIds.contains(orderId)) row.setStyle(rowStyle("#F5E8EA", bottomRadius, borderBottom));
        });
        row.setOnMouseExited(e -> {
            String cur = selectedIds.contains(orderId) ? "#FDE8EA" : bg;
            row.setStyle(rowStyle(cur, bottomRadius, borderBottom));
        });

        row.getChildren().addAll(
            buildTextCell(orderId, dataW * COL_ORDER_ID, true),
            buildColDivider(),
            buildTextCell(custId,  dataW * COL_CUST_ID,  false),
            buildColDivider(),
            (editMode && currentTab.equals("active"))
                ? buildStatusDropdown(orderId, status, dataW * COL_STATUS)
                : buildStatusCell(status, dataW * COL_STATUS),
            buildColDivider(),
            buildTextCell(payment, dataW * COL_PAYMENT, false)
        );

        if (archiveMode) {
            row.getChildren().add(buildColDivider());
            CheckBox cb = new CheckBox();
            cb.setSelected(selected);
            cb.setStyle("-fx-cursor: hand;");
            cb.setOnAction(e -> {
                if (cb.isSelected()) {
                    selectedIds.add(orderId);
                    row.setStyle(rowStyle("#FDE8EA", bottomRadius, borderBottom));
                } else {
                    selectedIds.remove(orderId);
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

    private HBox buildStatusCell(String status, double width) {
        if (status == null) status = "—";
        String[] colors = pillColors(status);
        Label pill = new Label(status);
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

    private HBox buildStatusDropdown(String orderId, String currentStatus, double width) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll("Pending", "Preparing", "Completed", "Cancelled");
        combo.setValue(currentStatus != null ? currentStatus : "Pending");
        combo.setPrefWidth(width - 20);
        combo.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 12px;" +
            "-fx-background-color: white;" +
            "-fx-border-color: " + ACCENT + ";" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-cursor: hand;"
        );
        combo.setOnAction(e -> {
            String chosen = combo.getValue();
            if (chosen == null || chosen.equals(currentStatus)) return;
            updateOrderStatus(orderId, chosen);
            for (String[] row : cachedRows) {
                if (row[0].equals(orderId)) { row[2] = chosen; break; }
            }
        });
        HBox cell = new HBox(combo);
        cell.setPrefWidth(width);
        cell.setPrefHeight(ROW_H);
        cell.setPadding(new Insets(0, 0, 0, 10));
        cell.setAlignment(Pos.CENTER_LEFT);
        return cell;
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

    private Region buildColDivider() {
        Region div = new Region();
        div.setPrefWidth(1.5);
        div.setMinWidth(1.5);
        div.setMaxWidth(1.5);
        div.setStyle("-fx-background-color: " + TABLE_BORDER + "; -fx-opacity: 0.35;");
        VBox.setVgrow(div, Priority.ALWAYS);
        return div;
    }

    private String[] pillColors(String status) {
        if (status == null) return new String[]{ PILL_PENDING_BG, PILL_PENDING_FG };
        return switch (status.toLowerCase()) {
            case "completed" -> new String[]{ PILL_COMPLETED_BG, PILL_COMPLETED_FG };
            case "cancelled" -> new String[]{ PILL_CANCELLED_BG, PILL_CANCELLED_FG };
            case "preparing" -> new String[]{ PILL_PREPARING_BG, PILL_PREPARING_FG };
            default          -> new String[]{ PILL_PENDING_BG,   PILL_PENDING_FG   };
        };
    }

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

    private String editBtnStyle(boolean active) {
        return "-fx-background-color: " + (active ? "#D4EDDA" : "#F5E8EA") + ";" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: " + (active ? "#155724" : ACCENT) + ";" +
               "-fx-border-radius: 8;" +
               "-fx-border-width: 1.5;" +
               "-fx-cursor: hand;";
    }

    private String editBtnHoverStyle(boolean active) {
        return "-fx-background-color: " + (active ? "#C3E6CB" : "#EDD5D8") + ";" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: " + (active ? "#155724" : ACCENT) + ";" +
               "-fx-border-radius: 8;" +
               "-fx-border-width: 1.5;" +
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