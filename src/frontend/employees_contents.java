package frontend;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
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

public class employees_contents {

    // ══════════════════════════════════════════════════════
    //  LAYOUT CONSTANTS
    // ══════════════════════════════════════════════════════
    private static final double TOP_PADDING  = 20;
    private static final double SIDE_PADDING = 24;
    private static final double HEADER_H     = 56;

    private static final double COL_EMPLOYEE_ID = 0.18;
    private static final double COL_EMP_NAME    = 0.27;
    private static final double COL_AGE         = 0.10;
    private static final double COL_ROLE        = 0.20;
    private static final double COL_EMP_STATUS  = 0.25;

    private static final double ROW_H        = 44;
    private static final double HEADER_ROW_H = 46;
    private static final double CHECKBOX_COL = 48;

    private static final double ADD_MODAL_W  = 460;
    private static final double ADD_MODAL_H  = 460;
    private static final double CONF_MODAL_W = 460;
    private static final double CONF_MODAL_H = 260;

    // ══════════════════════════════════════════════════════
    //  STYLE CONSTANTS
    // ══════════════════════════════════════════════════════
    private static final String ACCENT            = "#882F39";
    private static final String FONT_FAMILY       = "Aleo";
    private static final String TABLE_BORDER      = "#882F39";
    private static final String ROW_ALT_BG        = "#FDF5F6";
    private static final String ROW_WHITE_BG      = "white";
    private static final String HEADER_BG         = "#F5E8EA";

    private static final String PILL_ACTIVE_BG     = "#D4EDDA";
    private static final String PILL_ACTIVE_FG     = "#155724";
    private static final String PILL_INACTIVE_BG   = "#F8D7DA";
    private static final String PILL_INACTIVE_FG   = "#721C24";
    private static final String PILL_ON_LEAVE_BG   = "#FFF3CD";
    private static final String PILL_ON_LEAVE_FG   = "#856404";
    private static final String PILL_TERMINATED_BG = "#E2E3E5";
    private static final String PILL_TERMINATED_FG = "#383D41";

    // ══════════════════════════════════════════════════════
    //  STATE
    // ══════════════════════════════════════════════════════
    private final double     totalW;
    private final double     totalH;
    private final Connection conn;

    private String         currentTab  = "active";
    private boolean        editMode    = false;
    private boolean        archiveMode = false;
    private String         searchQuery = "";
    private Pane           root;
    private StackPane      stackRoot;
    private ScrollPane     tableScroll;
    private List<String[]> cachedRows  = new ArrayList<>();
    private Set<String>    selectedIds = new HashSet<>();

    private Label     editBtn;
    private Label     addBtn;
    private Label     archiveBtn;
    private Label     archiveAllBtn;
    private Label     confirmBtn;
    private Label     activeTabBtn;
    private Label     archivedTabBtn;
    private Label     deleteBtn;
    private Label     exportCsvBtn;
    private TextField searchField;
    private HBox      searchBar;

    // ── Layout values computed once in getView() ─────────
    private double btnY;
    private double iconW;
    private double gap;
    private double tabW;
    private double archAllW;
    private double confirmW;
    private double csvW;
    private double searchW;
    private double deleteX;
    private double exportCsvX;
    private double archivedTabX;
    private double activeTabX;
    private double confirmX;
    private double archAllX;

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

    public employees_contents(double totalW, double totalH, Connection conn) {
        this.totalW = totalW;
        this.totalH = totalH;
        this.conn   = conn;
        loadFonts();
    }

    // ══════════════════════════════════════════════════════
    //  PUBLIC LIVE-UPDATE API
    // ══════════════════════════════════════════════════════
    public void prependEmployee(String employeeId, String employeeName,
                                 String age, String role, String employmentStatus) {
        String[] newRow = new String[]{
            employeeId,
            employeeName     != null ? employeeName     : "--",
            age              != null ? age              : "--",
            role             != null ? role             : "--",
            employmentStatus != null ? employmentStatus : "Active"
        };
        if (root != null && currentTab.equals("active")) {
            cachedRows.add(0, newRow);
            rebuildTable();
        }
    }

    // ── Filtered rows based on searchQuery ───────────────
    private List<String[]> getFilteredRows() {
        if (searchQuery == null || searchQuery.isBlank()) return cachedRows;
        String q = searchQuery.trim().toLowerCase();
        List<String[]> filtered = new ArrayList<>();
        for (String[] row : cachedRows) {
            if (row[1].toLowerCase().contains(q) || row[0].toLowerCase().contains(q))
                filtered.add(row);
        }
        return filtered;
    }

    // ══════════════════════════════════════════════════════
    //  DB OPERATIONS
    // ══════════════════════════════════════════════════════
    private List<String[]> fetchEmployees(String tab) {
        List<String[]> rows = new ArrayList<>();
        if (conn == null) return rows;
        try { if (conn.isClosed()) return rows; } catch (Exception e) { return rows; }
        String sql =
            "SELECT employee_id, employee_name, age, role, employment_status " +
            "FROM dbo.Employees " +
            "WHERE is_deleted = 0 AND status = ? " +
            "ORDER BY employee_name ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tab);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int    ageVal = rs.getInt("age");
                String ageStr = rs.wasNull() ? "—" : String.valueOf(ageVal);
                rows.add(new String[]{
                    nvl(rs.getString("employee_id")),
                    nvl(rs.getString("employee_name")),
                    ageStr,
                    nvl(rs.getString("role")),
                    nvl(rs.getString("employment_status"))
                });
            }
            rs.close();
        } catch (Exception e) { e.printStackTrace(); }
        return rows;
    }

    private String generateEmployeeId() {
        if (conn == null) return "EMP-" + System.currentTimeMillis();
        String sql = "SELECT MAX(employee_id) FROM dbo.Employees";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getString(1) != null) {
                String last = rs.getString(1);
                rs.close();
                try {
                    int num = Integer.parseInt(last.replace("EMP-", "").trim());
                    return String.format("EMP-%03d", num + 1);
                } catch (NumberFormatException ex) {
                    return "EMP-" + System.currentTimeMillis();
                }
            }
            rs.close();
        } catch (Exception e) { e.printStackTrace(); }
        return "EMP-001";
    }

    private boolean insertEmployee(String name, int age, String role) {
        if (conn == null) return false;
        try { if (conn.isClosed()) return false; } catch (Exception e) { return false; }
        String id = generateEmployeeId();
        String sql =
            "INSERT INTO dbo.Employees " +
            "(employee_id, employee_name, age, role, employment_status, status, is_deleted) " +
            "VALUES (?, ?, ?, ?, 'Active', 'active', 0)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, name.trim());
            ps.setInt(3, age);
            ps.setString(4, role);
            ps.executeUpdate();
            prependEmployee(id, name.trim(), String.valueOf(age), role, "Active");
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    private void updateEmploymentStatus(String employeeId, String newStatus) {
        if (conn == null) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE dbo.Employees SET employment_status = ? WHERE employee_id = ?")) {
            ps.setString(1, newStatus);
            ps.setString(2, employeeId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void archiveSelected(Set<String> ids) {
        if (conn == null || ids.isEmpty()) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Employees SET status = 'archived' WHERE employee_id = ? AND is_deleted = 0")) {
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
                    "UPDATE dbo.Employees SET status = 'active' WHERE employee_id = ? AND is_deleted = 0")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void hardDeleteAll() {
        if (conn == null) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM dbo.Employees WHERE is_deleted = 0 AND status = ?")) {
            ps.setString(1, currentTab);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void exportCsv() {
        if (cachedRows.isEmpty()) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Employee List as CSV");
        chooser.setInitialFileName("employees_" + currentTab + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        Stage stage = null;
        try { stage = (Stage) root.getScene().getWindow(); } catch (Exception ignored) {}
        File file = (stage != null) ? chooser.showSaveDialog(stage) : chooser.showSaveDialog(null);
        if (file == null) return;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("Employee ID,Employee Name,Age,Role,Employment Status");
            writer.newLine();
            for (String[] row : cachedRows) {
                writer.write(
                    escapeCsv(row[0])+","+escapeCsv(row[1])+","+
                    escapeCsv(row[2])+","+escapeCsv(row[3])+","+escapeCsv(row[4])
                );
                writer.newLine();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String escapeCsv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n"))
            return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }

    private String nvl(String v) { return v != null ? v : "—"; }

    // ══════════════════════════════════════════════════════
    //  SEARCH BAR REPOSITIONING
    // ══════════════════════════════════════════════════════
    private void repositionSearchBar() {
        if (searchBar == null) return;
        double rightAnchor = archiveMode ? archAllX : activeTabX;
        double newSearchX  = rightAnchor - gap - searchW;
        searchBar.setLayoutX(newSearchX);
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
        iconW          = 36;
        gap            = 8;
        tabW           = 90;
        archAllW       = 100;
        confirmW       = 90;
        csvW           = 120;
        // ── Width for the labeled Add Employee button ─────
        double addEmpW = 140;
        searchW        = 200;

        // ── Title ─────────────────────────────────────────
        Label title = new Label("Employees");
        title.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 36px;" +
            "-fx-font-weight: 800;" +
            "-fx-text-fill: " + ACCENT + ";"
        );

        // ── Edit (pen) button — icon-only, unchanged ──────
        FontIcon penIcon = new FontIcon(FontAwesomeSolid.PEN);
        penIcon.setIconSize(15);
        penIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));
        editBtn = new Label();
        editBtn.setGraphic(penIcon);
        editBtn.setCursor(javafx.scene.Cursor.HAND);
        editBtn.setStyle(iconBtnStyle(false, ACCENT, "#F5E8EA"));
        editBtn.setPrefHeight(btnH); editBtn.setPrefWidth(iconW);
        editBtn.setAlignment(Pos.CENTER);
        editBtn.setOnMouseEntered(e -> editBtn.setStyle(iconBtnStyle(editMode, ACCENT, "#EDD5D8")));
        editBtn.setOnMouseExited(e  -> editBtn.setStyle(iconBtnStyle(editMode, editMode ? "#155724" : ACCENT,
                                                                      editMode ? "#D4EDDA" : "#F5E8EA")));
        editBtn.setOnMouseClicked(e -> {
            editMode = !editMode;
            FontIcon ic = new FontIcon(editMode ? FontAwesomeSolid.CHECK : FontAwesomeSolid.PEN);
            ic.setIconSize(15);
            ic.setIconColor(javafx.scene.paint.Color.web(editMode ? "#155724" : ACCENT));
            editBtn.setGraphic(ic);
            editBtn.setStyle(iconBtnStyle(editMode, editMode ? "#155724" : ACCENT,
                                          editMode ? "#D4EDDA" : "#F5E8EA"));
            rebuildTable();
        });

        // ── Add Employee button — labeled green, mirrors addSupplierBtn ──
        FontIcon plusIcon = new FontIcon(FontAwesomeSolid.USER_PLUS);
        plusIcon.setIconSize(14);
        plusIcon.setIconColor(javafx.scene.paint.Color.web("#155724"));
        addBtn = new Label("Add Employee");
        addBtn.setGraphic(plusIcon);
        addBtn.setGraphicTextGap(6);
        addBtn.setCursor(javafx.scene.Cursor.HAND);
        addBtn.setStyle(addEmpBtnStyle(false));
        addBtn.setPrefHeight(btnH);
        addBtn.setPrefWidth(addEmpW);
        addBtn.setAlignment(Pos.CENTER);
        addBtn.setOnMouseEntered(e -> addBtn.setStyle(addEmpBtnStyle(true)));
        addBtn.setOnMouseExited(e  -> addBtn.setStyle(addEmpBtnStyle(false)));
        addBtn.setOnMouseClicked(e -> stackRoot.getChildren().add(buildAddEmployeeModal()));

        // ── Archive button — icon-only, unchanged ─────────
        FontIcon boxIcon = new FontIcon(FontAwesomeSolid.ARCHIVE);
        boxIcon.setIconSize(15);
        boxIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));
        archiveBtn = new Label();
        archiveBtn.setGraphic(boxIcon);
        archiveBtn.setCursor(javafx.scene.Cursor.HAND);
        archiveBtn.setStyle(iconBtnStyle(false, ACCENT, "#F5E8EA"));
        archiveBtn.setPrefHeight(btnH); archiveBtn.setPrefWidth(iconW);
        archiveBtn.setAlignment(Pos.CENTER);
        archiveBtn.setOnMouseEntered(e -> archiveBtn.setStyle(iconBtnStyle(archiveMode, archiveMode ? "#155724" : ACCENT, "#EDD5D8")));
        archiveBtn.setOnMouseExited(e  -> archiveBtn.setStyle(iconBtnStyle(archiveMode, archiveMode ? "#155724" : ACCENT,
                                                                            archiveMode ? "#D4EDDA" : "#F5E8EA")));
        archiveBtn.setOnMouseClicked(e -> toggleArchiveMode());

        HBox titleRow = new HBox(gap, title, editBtn, addBtn, archiveBtn);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.setLayoutX(SIDE_PADDING); titleRow.setLayoutY(TOP_PADDING);
        titleRow.setPrefHeight(HEADER_H);

        // ── Right-side button layout (right → left) ───────
        deleteX      = totalW - SIDE_PADDING - iconW;
        exportCsvX   = deleteX      - gap - csvW;
        archivedTabX = exportCsvX   - gap - tabW;
        activeTabX   = archivedTabX - gap - tabW;
        confirmX     = activeTabX   - gap - confirmW;
        archAllX     = confirmX     - gap - archAllW;

        double initialSearchX = activeTabX - gap - searchW;

        // ── Delete button ─────────────────────────────────
        deleteBtn = new Label();
        FontIcon trashIcon = new FontIcon(FontAwesomeSolid.TRASH_ALT);
        trashIcon.setIconSize(15);
        trashIcon.setIconColor(javafx.scene.paint.Color.web("#721C24"));
        deleteBtn.setGraphic(trashIcon);
        deleteBtn.setCursor(javafx.scene.Cursor.HAND);
        deleteBtn.setStyle(deleteBtnStyle(false));
        deleteBtn.setLayoutX(deleteX); deleteBtn.setLayoutY(btnY);
        deleteBtn.setPrefHeight(btnH); deleteBtn.setPrefWidth(iconW);
        deleteBtn.setAlignment(Pos.CENTER);
        deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(deleteBtnStyle(true)));
        deleteBtn.setOnMouseExited(e  -> deleteBtn.setStyle(deleteBtnStyle(false)));
        deleteBtn.setOnMouseClicked(e ->
            stackRoot.getChildren().add(buildConfirmModal(
                "Employees (" + currentTab + ")",
                "This will permanently remove all employees in this view.\nThis action cannot be undone.",
                () -> { hardDeleteAll(); cachedRows.clear(); selectedIds.clear(); rebuildTable(); }
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
        exportCsvBtn.setLayoutX(exportCsvX); exportCsvBtn.setLayoutY(btnY);
        exportCsvBtn.setPrefHeight(btnH); exportCsvBtn.setPrefWidth(csvW);
        exportCsvBtn.setAlignment(Pos.CENTER);
        exportCsvBtn.setOnMouseEntered(e -> exportCsvBtn.setStyle(exportCsvBtnStyle(true)));
        exportCsvBtn.setOnMouseExited(e  -> exportCsvBtn.setStyle(exportCsvBtnStyle(false)));
        exportCsvBtn.setOnMouseClicked(e -> exportCsv());

        // ── Tab buttons ───────────────────────────────────
        activeTabBtn   = buildTabLabel("Active",   true);
        activeTabBtn.setLayoutX(activeTabX);    activeTabBtn.setLayoutY(btnY);
        archivedTabBtn = buildTabLabel("Archived", false);
        archivedTabBtn.setLayoutX(archivedTabX); archivedTabBtn.setLayoutY(btnY);

        activeTabBtn.setOnMouseEntered(e  -> { if (!currentTab.equals("active"))   activeTabBtn.setStyle(tabBtnHoverStyle()); });
        activeTabBtn.setOnMouseExited(e   -> activeTabBtn.setStyle(tabBtnStyle(currentTab.equals("active"))));
        activeTabBtn.setOnMouseClicked(e  -> switchTab("active"));
        archivedTabBtn.setOnMouseEntered(e -> { if (!currentTab.equals("archived")) archivedTabBtn.setStyle(tabBtnHoverStyle()); });
        archivedTabBtn.setOnMouseExited(e  -> archivedTabBtn.setStyle(tabBtnStyle(currentTab.equals("archived"))));
        archivedTabBtn.setOnMouseClicked(e -> switchTab("archived"));

        // ── Archive All button ────────────────────────────
        archiveAllBtn = new Label("Archive All");
        archiveAllBtn.setCursor(javafx.scene.Cursor.HAND);
        archiveAllBtn.setPrefWidth(archAllW); archiveAllBtn.setPrefHeight(btnH);
        archiveAllBtn.setAlignment(Pos.CENTER);
        archiveAllBtn.setStyle(archiveAllBtnStyle(false));
        archiveAllBtn.setVisible(false);
        archiveAllBtn.setLayoutX(archAllX); archiveAllBtn.setLayoutY(btnY);
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
        confirmBtn.setPrefWidth(confirmW); confirmBtn.setPrefHeight(btnH);
        confirmBtn.setAlignment(Pos.CENTER);
        confirmBtn.setStyle(confirmBtnStyle(false));
        confirmBtn.setVisible(false);
        confirmBtn.setLayoutX(confirmX); confirmBtn.setLayoutY(btnY);
        confirmBtn.setOnMouseEntered(e -> confirmBtn.setStyle(confirmBtnStyle(true)));
        confirmBtn.setOnMouseExited(e  -> confirmBtn.setStyle(confirmBtnStyle(false)));
        confirmBtn.setOnMouseClicked(e -> {
            if (selectedIds.isEmpty()) return;
            if (currentTab.equals("active")) archiveSelected(selectedIds);
            else                             restoreSelected(selectedIds);
            selectedIds.clear(); archiveMode = false;
            updateArchiveBtnIcon();
            archiveAllBtn.setVisible(false); confirmBtn.setVisible(false);
            archiveBtn.setStyle(iconBtnStyle(false, ACCENT, "#F5E8EA"));
            repositionSearchBar();
            cachedRows = fetchEmployees(currentTab);
            rebuildTable();
        });

        // ── Search bar ────────────────────────────────────
        FontIcon searchIcon = new FontIcon(FontAwesomeSolid.SEARCH);
        searchIcon.setIconSize(14);
        searchIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        searchField = new TextField();
        searchField.setPromptText("Search name or employee ID...");
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
        searchBar.setLayoutX(initialSearchX);
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

        // ── Initial table ─────────────────────────────────
        double tableY = TOP_PADDING + HEADER_H + 10;
        double tableW = totalW - SIDE_PADDING * 2;
        double tableH = totalH - tableY - SIDE_PADDING;

        cachedRows  = fetchEmployees("active");
        tableScroll = buildScrollPane(tableW, tableH, tableY);

        root.getChildren().addAll(
            titleRow, searchBar, archiveAllBtn, confirmBtn,
            activeTabBtn, archivedTabBtn, exportCsvBtn, deleteBtn, tableScroll
        );
        stackRoot.getChildren().add(root);
        return stackRoot;
    }

    // ══════════════════════════════════════════════════════
    //  ADD EMPLOYEE MODAL
    // ══════════════════════════════════════════════════════
    private Pane buildAddEmployeeModal() {
        Pane overlay = new Pane();
        overlay.setPrefWidth(totalW);  overlay.setPrefHeight(totalH);
        overlay.setMinWidth(totalW);   overlay.setMinHeight(totalH);
        overlay.setMaxWidth(totalW);   overlay.setMaxHeight(totalH);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.45);");

        VBox card = new VBox(0);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPrefWidth(ADD_MODAL_W);  card.setMinWidth(ADD_MODAL_W);  card.setMaxWidth(ADD_MODAL_W);
        card.setPrefHeight(ADD_MODAL_H); card.setMinHeight(ADD_MODAL_H); card.setMaxHeight(ADD_MODAL_H);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 14;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0, 0, 6);"
        );

        // ── Colored modal header ──────────────────────────
        HBox cardHeader = new HBox(10);
        cardHeader.setPadding(new Insets(20, 24, 16, 24));
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        cardHeader.setStyle(
            "-fx-background-color: " + HEADER_BG + ";" +
            "-fx-background-radius: 14 14 0 0;" +
            "-fx-border-color: transparent transparent " + TABLE_BORDER + " transparent;" +
            "-fx-border-width: 0 0 1.5 0;"
        );

        FontIcon userIcon = new FontIcon(FontAwesomeSolid.USER_PLUS);
        userIcon.setIconSize(17);
        userIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        Label modalTitle = new Label("Add New Employee");
        modalTitle.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 20px;" +
            "-fx-font-weight: 800;" +
            "-fx-text-fill: " + ACCENT + ";"
        );

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Label closeBtn = new Label();
        FontIcon xIcon = new FontIcon(FontAwesomeSolid.TIMES);
        xIcon.setIconSize(13);
        xIcon.setIconColor(javafx.scene.paint.Color.web("#555555"));
        closeBtn.setGraphic(xIcon);
        closeBtn.setCursor(javafx.scene.Cursor.HAND);
        closeBtn.setPrefWidth(30); closeBtn.setPrefHeight(30);
        closeBtn.setAlignment(Pos.CENTER);
        closeBtn.setStyle("-fx-background-color: #E9ECEF; -fx-background-radius: 6;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #DEE2E6; -fx-background-radius: 6;"));
        closeBtn.setOnMouseExited(e  -> closeBtn.setStyle("-fx-background-color: #E9ECEF; -fx-background-radius: 6;"));
        closeBtn.setOnMouseClicked(e -> stackRoot.getChildren().remove(overlay));

        cardHeader.getChildren().addAll(userIcon, modalTitle, hSpacer, closeBtn);

        // ── Form body ─────────────────────────────────────
        VBox formBody = new VBox(18);
        formBody.setPadding(new Insets(26, 28, 10, 28));
        VBox.setVgrow(formBody, Priority.ALWAYS);

        VBox nameFieldBox = buildFormField(FontAwesomeSolid.USER, "Full Name", "e.g. Juan Dela Cruz");
        TextField nameInput = extractTextField(nameFieldBox);

        VBox ageFieldBox = buildFormField(FontAwesomeSolid.BIRTHDAY_CAKE, "Age", "e.g. 24");
        TextField ageInput = extractTextField(ageFieldBox);
        ageInput.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) ageInput.setText(newVal.replaceAll("[^\\d]", ""));
            if (newVal.length() > 3)    ageInput.setText(oldVal);
        });

        Label roleLabel = new Label("Role");
        roleLabel.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #555555;"
        );

        FontIcon roleIcon = new FontIcon(FontAwesomeSolid.ID_BADGE);
        roleIcon.setIconSize(13);
        roleIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("Barista", "Cashier", "Cleaner");
        roleCombo.setValue("Barista");
        roleCombo.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setDisable(false); return; }
                setText(item);
                boolean disabled = item.equals("Cashier") || item.equals("Cleaner");
                setDisable(disabled);
                setStyle(disabled
                    ? "-fx-text-fill: #AAAAAA; -fx-font-family: '" + FONT_FAMILY + "'; -fx-font-size: 13px;"
                    : "-fx-font-family: '" + FONT_FAMILY + "'; -fx-font-size: 13px;"
                );
            }
        });
        roleCombo.setButtonCell(new javafx.scene.control.ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setStyle("-fx-font-family: '" + FONT_FAMILY + "'; -fx-font-size: 13px; -fx-background-color: transparent;");
            }
        });
        HBox.setHgrow(roleCombo, Priority.ALWAYS);
        roleCombo.setMaxWidth(Double.MAX_VALUE);
        roleCombo.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;" +
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 13px;"
        );

        HBox roleInputBox = new HBox(8, roleIcon, roleCombo);
        roleInputBox.setAlignment(Pos.CENTER_LEFT);
        roleInputBox.setPadding(new Insets(0, 12, 0, 12));
        roleInputBox.setPrefHeight(40);
        roleInputBox.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: " + ACCENT + ";" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 10;"
        );

        VBox roleFieldBox = new VBox(6, roleLabel, roleInputBox);

        Label errorLbl = new Label("");
        errorLbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #882F39;"
        );
        errorLbl.setVisible(false);
        errorLbl.setWrapText(true);
        errorLbl.setMaxWidth(ADD_MODAL_W - 56);

        formBody.getChildren().addAll(nameFieldBox, ageFieldBox, roleFieldBox, errorLbl);

        // ── Footer ────────────────────────────────────────
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(18, 28, 24, 28));

        Label cancelBtn = new Label("Cancel");
        cancelBtn.setCursor(javafx.scene.Cursor.HAND);
        cancelBtn.setPrefWidth(140); cancelBtn.setPrefHeight(38);
        cancelBtn.setAlignment(Pos.CENTER);
        cancelBtn.setStyle(modalNoBtnStyle(false));
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(modalNoBtnStyle(true)));
        cancelBtn.setOnMouseExited(e  -> cancelBtn.setStyle(modalNoBtnStyle(false)));
        cancelBtn.setOnMouseClicked(e -> stackRoot.getChildren().remove(overlay));

        FontIcon saveIcon = new FontIcon(FontAwesomeSolid.USER_PLUS);
        saveIcon.setIconSize(13);
        saveIcon.setIconColor(javafx.scene.paint.Color.WHITE);
        Label saveBtn = new Label("Add Employee");
        saveBtn.setGraphic(saveIcon);
        saveBtn.setGraphicTextGap(7);
        saveBtn.setCursor(javafx.scene.Cursor.HAND);
        saveBtn.setPrefWidth(160); saveBtn.setPrefHeight(38);
        saveBtn.setAlignment(Pos.CENTER);
        saveBtn.setStyle(confirmBtnStyle(false));
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle(confirmBtnStyle(true)));
        saveBtn.setOnMouseExited(e  -> saveBtn.setStyle(confirmBtnStyle(false)));
        saveBtn.setOnMouseClicked(e -> {
            String name    = nameInput.getText().trim();
            String ageText = ageInput.getText().trim();
            String role    = roleCombo.getValue();

            if (name.isEmpty()) {
                errorLbl.setText("Please enter the employee's full name.");
                errorLbl.setVisible(true); return;
            }
            if (name.length() < 2) {
                errorLbl.setText("Name must be at least 2 characters.");
                errorLbl.setVisible(true); return;
            }
            int age = 0;
            if (!ageText.isEmpty()) {
                try {
                    age = Integer.parseInt(ageText);
                    if (age < 15 || age > 99) {
                        errorLbl.setText("Please enter a valid age (15–99).");
                        errorLbl.setVisible(true); return;
                    }
                } catch (NumberFormatException ex) {
                    errorLbl.setText("Age must be a number.");
                    errorLbl.setVisible(true); return;
                }
            }
            if (role == null || role.isEmpty()) {
                errorLbl.setText("Please select a role.");
                errorLbl.setVisible(true); return;
            }
            errorLbl.setVisible(false);
            boolean ok = insertEmployee(name, age, role);
            if (ok) {
                stackRoot.getChildren().remove(overlay);
            } else {
                errorLbl.setText("Failed to save employee. Please try again.");
                errorLbl.setVisible(true);
            }
        });

        footer.getChildren().addAll(cancelBtn, saveBtn);
        card.getChildren().addAll(cardHeader, formBody, footer);

        StackPane centred = new StackPane(card);
        centred.setPrefWidth(totalW);  centred.setPrefHeight(totalH);
        centred.setMinWidth(totalW);   centred.setMinHeight(totalH);
        centred.setMaxWidth(totalW);   centred.setMaxHeight(totalH);
        centred.setAlignment(Pos.CENTER);
        overlay.getChildren().add(centred);
        return overlay;
    }

    private VBox buildFormField(FontAwesomeSolid iconCode, String label, String prompt) {
        Label fieldLabel = new Label(label);
        fieldLabel.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #555555;"
        );

        FontIcon fi = new FontIcon(iconCode);
        fi.setIconSize(13);
        fi.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        TextField input = new TextField();
        input.setPromptText(prompt);
        HBox.setHgrow(input, Priority.ALWAYS);
        input.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;" +
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #333333;" +
            "-fx-prompt-text-fill: #AAAAAA;"
        );

        HBox inputBox = new HBox(8, fi, input);
        inputBox.setAlignment(Pos.CENTER_LEFT);
        inputBox.setPadding(new Insets(0, 12, 0, 12));
        inputBox.setPrefHeight(40);
        inputBox.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: " + ACCENT + ";" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 10;"
        );

        return new VBox(6, fieldLabel, inputBox);
    }

    private TextField extractTextField(VBox fieldBox) {
        HBox inputBox = (HBox) fieldBox.getChildren().get(1);
        return (TextField) inputBox.getChildren().get(1);
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
        archiveBtn.setStyle(iconBtnStyle(archiveMode, archiveMode ? "#155724" : ACCENT,
                                         archiveMode ? "#D4EDDA" : "#F5E8EA"));
        repositionSearchBar();
        rebuildTable();
    }

    private void updateArchiveBtnIcon() {
        FontIcon icon = archiveMode ? new FontIcon(FontAwesomeSolid.TIMES) : new FontIcon(FontAwesomeSolid.ARCHIVE);
        icon.setIconSize(15);
        icon.setIconColor(javafx.scene.paint.Color.web(archiveMode ? "#155724" : ACCENT));
        archiveBtn.setGraphic(icon);
    }

    // ══════════════════════════════════════════════════════
    //  TAB SWITCHING
    // ══════════════════════════════════════════════════════
    private void switchTab(String tab) {
        if (currentTab.equals(tab)) return;
        currentTab = tab; editMode = false; archiveMode = false;
        selectedIds.clear();
        searchQuery = "";
        if (searchField != null) searchField.clear();
        FontIcon penIcon = new FontIcon(FontAwesomeSolid.PEN);
        penIcon.setIconSize(15);
        penIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));
        editBtn.setGraphic(penIcon);
        editBtn.setStyle(iconBtnStyle(false, ACCENT, "#F5E8EA"));
        editBtn.setVisible(tab.equals("active"));
        addBtn.setVisible(tab.equals("active"));
        updateArchiveBtnIcon();
        archiveAllBtn.setText(tab.equals("archived") ? "Restore All" : "Archive All");
        archiveAllBtn.setVisible(false); confirmBtn.setVisible(false);
        archiveBtn.setStyle(iconBtnStyle(false, ACCENT, "#F5E8EA"));
        activeTabBtn.setStyle(tabBtnStyle(tab.equals("active")));
        archivedTabBtn.setStyle(tabBtnStyle(tab.equals("archived")));
        repositionSearchBar();
        cachedRows = fetchEmployees(tab);
        rebuildTable();
    }

    private Label buildTabLabel(String text, boolean selected) {
        Label lbl = new Label(text);
        lbl.setCursor(javafx.scene.Cursor.HAND);
        lbl.setPrefWidth(90); lbl.setPrefHeight(36);
        lbl.setAlignment(Pos.CENTER);
        lbl.setStyle(tabBtnStyle(selected));
        return lbl;
    }

    // ══════════════════════════════════════════════════════
    //  CONFIRMATION MODAL
    // ══════════════════════════════════════════════════════
    private Pane buildConfirmModal(String context, String subMessage, Runnable onConfirm) {
        Pane overlay = new Pane();
        overlay.setPrefWidth(totalW);  overlay.setPrefHeight(totalH);
        overlay.setMinWidth(totalW);   overlay.setMinHeight(totalH);
        overlay.setMaxWidth(totalW);   overlay.setMaxHeight(totalH);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.45);");

        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(CONF_MODAL_W);  card.setMinWidth(CONF_MODAL_W);  card.setMaxWidth(CONF_MODAL_W);
        card.setPrefHeight(CONF_MODAL_H); card.setMinHeight(CONF_MODAL_H); card.setMaxHeight(CONF_MODAL_H);
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
        heading.setStyle("-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 14px;-fx-font-weight: bold;" +
            "-fx-text-fill: #222222;-fx-text-alignment: center;-fx-alignment: center;");
        heading.setAlignment(Pos.CENTER); heading.setWrapText(true); heading.setMaxWidth(CONF_MODAL_W - 80);

        Label sub = new Label(subMessage);
        sub.setStyle("-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 12px;" +
            "-fx-text-fill: #777777;-fx-text-alignment: center;-fx-alignment: center;");
        sub.setAlignment(Pos.CENTER); sub.setWrapText(true); sub.setMaxWidth(CONF_MODAL_W - 80);

        Label noBtn = new Label("No, cancel");
        noBtn.setCursor(javafx.scene.Cursor.HAND);
        noBtn.setPrefWidth(140); noBtn.setPrefHeight(38);
        noBtn.setAlignment(Pos.CENTER); noBtn.setStyle(modalNoBtnStyle(false));
        noBtn.setOnMouseEntered(e -> noBtn.setStyle(modalNoBtnStyle(true)));
        noBtn.setOnMouseExited(e  -> noBtn.setStyle(modalNoBtnStyle(false)));
        noBtn.setOnMouseClicked(e -> stackRoot.getChildren().remove(overlay));

        Label yesBtn = new Label("Yes, delete");
        yesBtn.setCursor(javafx.scene.Cursor.HAND);
        yesBtn.setPrefWidth(140); yesBtn.setPrefHeight(38);
        yesBtn.setAlignment(Pos.CENTER); yesBtn.setStyle(modalYesBtnStyle(false));
        yesBtn.setOnMouseEntered(e -> yesBtn.setStyle(modalYesBtnStyle(true)));
        yesBtn.setOnMouseExited(e  -> yesBtn.setStyle(modalYesBtnStyle(false)));
        yesBtn.setOnMouseClicked(e -> { stackRoot.getChildren().remove(overlay); onConfirm.run(); });

        HBox btnRow = new HBox(16, noBtn, yesBtn);
        btnRow.setAlignment(Pos.CENTER);
        card.getChildren().addAll(warnIcon, heading, sub, btnRow);

        StackPane centred = new StackPane(card);
        centred.setPrefWidth(totalW);  centred.setPrefHeight(totalH);
        centred.setMinWidth(totalW);   centred.setMinHeight(totalH);
        centred.setMaxWidth(totalW);   centred.setMaxHeight(totalH);
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
        sp.setFitToWidth(true); sp.setPannable(true);
        sp.setStyle("-fx-background: transparent;-fx-background-color: transparent;" +
            "-fx-border-color: transparent;-fx-padding: 0;");
        sp.setPrefWidth(tableW); sp.setPrefHeight(tableH);
        sp.setLayoutX(SIDE_PADDING); sp.setLayoutY(tableY);
        return sp;
    }

    // ══════════════════════════════════════════════════════
    //  TABLE BUILDER
    // ══════════════════════════════════════════════════════
    private VBox buildTable(double tableW, List<String[]> rows) {
        double dataW = archiveMode ? tableW - CHECKBOX_COL : tableW;
        VBox table = new VBox(0);
        table.setStyle("-fx-border-color: " + TABLE_BORDER + ";-fx-border-width: 1.5;" +
            "-fx-border-radius: 10;-fx-background-color: white;-fx-background-radius: 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 10, 0, 0, 3);");
        table.getChildren().add(buildHeaderRow(tableW, dataW));
        if (rows.isEmpty()) {
            String msg = !searchQuery.isBlank() ? "No results found for \"" + searchQuery + "\"."
                       : currentTab.equals("archived") ? "No archived employees." : "No employees found.";
            Label empty = new Label(msg);
            empty.setStyle("-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 14px;" +
                "-fx-text-fill: #AAAAAA;-fx-padding: 24 0 24 16;");
            table.getChildren().add(empty);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                String[] emp = rows.get(i);
                table.getChildren().add(buildDataRow(
                    emp[0], emp[1], emp[2], emp[3], emp[4],
                    i % 2 == 0 ? ROW_WHITE_BG : ROW_ALT_BG,
                    tableW, dataW, i == rows.size() - 1));
            }
        }
        return table;
    }

    private HBox buildHeaderRow(double tableW, double dataW) {
        HBox row = new HBox(0);
        row.setPrefHeight(HEADER_ROW_H);
        row.setStyle("-fx-background-color: " + HEADER_BG + ";-fx-background-radius: 10 10 0 0;" +
            "-fx-border-color: transparent transparent " + TABLE_BORDER + " transparent;-fx-border-width: 0 0 1.5 0;");
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(
            buildHeaderCell("Employee ID",       dataW * COL_EMPLOYEE_ID), buildColDivider(),
            buildHeaderCell("Employee Name",     dataW * COL_EMP_NAME),    buildColDivider(),
            buildHeaderCell("Age",               dataW * COL_AGE),         buildColDivider(),
            buildHeaderCell("Role",              dataW * COL_ROLE),         buildColDivider(),
            buildHeaderCell("Employment Status", dataW * COL_EMP_STATUS)
        );
        if (archiveMode) {
            row.getChildren().add(buildColDivider());
            Label cbH = buildHeaderCell("", CHECKBOX_COL);
            cbH.setAlignment(Pos.CENTER);
            row.getChildren().add(cbH);
        }
        return row;
    }

    private Label buildHeaderCell(String text, double width) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(width); lbl.setPrefHeight(HEADER_ROW_H);
        lbl.setPadding(new Insets(0, 0, 0, 16));
        lbl.setAlignment(Pos.CENTER_LEFT);
        lbl.setStyle("-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 14px;" +
            "-fx-font-weight: bold;-fx-text-fill: " + ACCENT + ";");
        return lbl;
    }

    private HBox buildDataRow(String employeeId, String employeeName,
                               String age, String role, String employmentStatus,
                               String bg, double tableW, double dataW, boolean isLast) {
        HBox row = new HBox(0);
        row.setPrefHeight(ROW_H); row.setAlignment(Pos.CENTER_LEFT);
        String bottomRadius = isLast ? "0 0 10 10" : "0";
        String borderBottom = isLast ? "0" : "1";
        boolean selected = selectedIds.contains(employeeId);
        row.setStyle(rowStyle(selected ? "#FDE8EA" : bg, bottomRadius, borderBottom));
        row.setOnMouseEntered(e -> { if (!selectedIds.contains(employeeId)) row.setStyle(rowStyle("#F5E8EA", bottomRadius, borderBottom)); });
        row.setOnMouseExited(e  -> row.setStyle(rowStyle(selectedIds.contains(employeeId) ? "#FDE8EA" : bg, bottomRadius, borderBottom)));
        row.getChildren().addAll(
            buildTextCell(employeeId,   dataW * COL_EMPLOYEE_ID, true),  buildColDivider(),
            buildTextCell(employeeName, dataW * COL_EMP_NAME,    false), buildColDivider(),
            buildTextCell(age,          dataW * COL_AGE,          false), buildColDivider(),
            buildTextCell(role,         dataW * COL_ROLE,          false), buildColDivider(),
            (editMode && currentTab.equals("active"))
                ? buildStatusDropdown(employeeId, employmentStatus, dataW * COL_EMP_STATUS)
                : buildStatusCell(employmentStatus, dataW * COL_EMP_STATUS)
        );
        if (archiveMode) {
            row.getChildren().add(buildColDivider());
            CheckBox cb = new CheckBox();
            cb.setSelected(selected); cb.setStyle("-fx-cursor: hand;");
            cb.setOnAction(e -> {
                if (cb.isSelected()) { selectedIds.add(employeeId);    row.setStyle(rowStyle("#FDE8EA", bottomRadius, borderBottom)); }
                else                 { selectedIds.remove(employeeId); row.setStyle(rowStyle(bg, bottomRadius, borderBottom)); }
            });
            HBox cbCell = new HBox(cb);
            cbCell.setPrefWidth(CHECKBOX_COL); cbCell.setPrefHeight(ROW_H);
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
            "-fx-font-size: 12px;-fx-font-weight: bold;" +
            "-fx-text-fill: " + colors[1] + ";" +
            "-fx-background-color: " + colors[0] + ";" +
            "-fx-background-radius: 20;" +
            "-fx-padding: 4 14 4 14;"
        );
        HBox cell = new HBox(pill);
        cell.setPrefWidth(width); cell.setPrefHeight(ROW_H);
        cell.setPadding(new Insets(0, 0, 0, 16));
        cell.setAlignment(Pos.CENTER_LEFT);
        return cell;
    }

    private HBox buildStatusDropdown(String employeeId, String currentStatus, double width) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll("Active", "Inactive", "On Leave", "Terminated");
        combo.setValue(currentStatus != null ? currentStatus : "Active");
        combo.setPrefWidth(width - 20);
        combo.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 12px;" +
            "-fx-background-color: white;-fx-border-color: " + ACCENT + ";" +
            "-fx-border-radius: 6;-fx-background-radius: 6;-fx-cursor: hand;"
        );
        combo.setOnAction(e -> {
            String chosen = combo.getValue();
            if (chosen == null || chosen.equals(currentStatus)) return;
            updateEmploymentStatus(employeeId, chosen);
            for (String[] r : cachedRows) { if (r[0].equals(employeeId)) { r[4] = chosen; break; } }
        });
        HBox cell = new HBox(combo);
        cell.setPrefWidth(width); cell.setPrefHeight(ROW_H);
        cell.setPadding(new Insets(0, 0, 0, 10));
        cell.setAlignment(Pos.CENTER_LEFT);
        return cell;
    }

    private String rowStyle(String bg, String bottomRadius, String borderBottom) {
        return "-fx-background-color: " + bg + ";-fx-background-radius: " + bottomRadius + ";" +
               "-fx-border-color: transparent transparent " + TABLE_BORDER + " transparent;" +
               "-fx-border-width: 0 0 " + borderBottom + " 0;";
    }

    private Label buildTextCell(String text, double width, boolean bold) {
        Label lbl = new Label(text != null ? text : "—");
        lbl.setPrefWidth(width); lbl.setPrefHeight(ROW_H);
        lbl.setPadding(new Insets(0, 0, 0, 16));
        lbl.setAlignment(Pos.CENTER_LEFT);
        lbl.setStyle("-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;" +
            "-fx-font-weight: " + (bold ? "bold" : "normal") + ";-fx-text-fill: #333333;");
        return lbl;
    }

    private Region buildColDivider() {
        Region div = new Region();
        div.setPrefWidth(1.5); div.setMinWidth(1.5); div.setMaxWidth(1.5);
        div.setStyle("-fx-background-color: " + TABLE_BORDER + "; -fx-opacity: 0.35;");
        VBox.setVgrow(div, Priority.ALWAYS);
        return div;
    }

    private String[] pillColors(String status) {
        if (status == null) return new String[]{ PILL_ACTIVE_BG, PILL_ACTIVE_FG };
        return switch (status.toLowerCase()) {
            case "inactive"   -> new String[]{ PILL_INACTIVE_BG,   PILL_INACTIVE_FG   };
            case "on leave"   -> new String[]{ PILL_ON_LEAVE_BG,   PILL_ON_LEAVE_FG   };
            case "terminated" -> new String[]{ PILL_TERMINATED_BG, PILL_TERMINATED_FG };
            default           -> new String[]{ PILL_ACTIVE_BG,     PILL_ACTIVE_FG     };
        };
    }

    // ══════════════════════════════════════════════════════
    //  STYLE HELPERS
    // ══════════════════════════════════════════════════════
    private String iconBtnStyle(boolean active, String borderColor, String bgColor) {
        return "-fx-background-color: " + bgColor + ";-fx-background-radius: 8;" +
               "-fx-border-color: " + borderColor + ";-fx-border-radius: 8;-fx-border-width: 1.5;-fx-cursor: hand;";
    }

    // ── Labeled green button — mirrors addSupplierBtn style ──
    private String addEmpBtnStyle(boolean hovered) {
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

    private String tabBtnStyle(boolean selected) {
        return selected
            ? "-fx-background-color: " + ACCENT + ";-fx-background-radius: 8;-fx-font-family: '" + FONT_FAMILY + "';" +
              "-fx-font-size: 13px;-fx-font-weight: bold;-fx-text-fill: white;-fx-cursor: hand;"
            : "-fx-background-color: #F5E8EA;-fx-background-radius: 8;-fx-border-color: " + ACCENT + ";" +
              "-fx-border-radius: 8;-fx-border-width: 1.5;-fx-font-family: '" + FONT_FAMILY + "';" +
              "-fx-font-size: 13px;-fx-font-weight: bold;-fx-text-fill: " + ACCENT + ";-fx-cursor: hand;";
    }
    private String tabBtnHoverStyle() {
        return "-fx-background-color: #EDD5D8;-fx-background-radius: 8;-fx-border-color: " + ACCENT + ";" +
               "-fx-border-radius: 8;-fx-border-width: 1.5;-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 13px;-fx-font-weight: bold;-fx-text-fill: " + ACCENT + ";-fx-cursor: hand;";
    }
    private String archiveAllBtnStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#EDD5D8" : "#F5E8EA") + ";-fx-background-radius: 8;" +
               "-fx-border-color: " + ACCENT + ";-fx-border-radius: 8;-fx-border-width: 1.5;" +
               "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;-fx-font-weight: bold;" +
               "-fx-text-fill: " + ACCENT + ";-fx-cursor: hand;";
    }
    private String confirmBtnStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#A93226" : "#882F39") + ";-fx-background-radius: 8;" +
               "-fx-border-color: transparent;-fx-border-radius: 8;-fx-border-width: 0;" +
               "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;-fx-font-weight: bold;" +
               "-fx-text-fill: white;-fx-cursor: hand;";
    }
    private String deleteBtnStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#F8D7DA" : "#FDF0F1") + ";-fx-background-radius: 8;" +
               "-fx-border-color: #721C24;-fx-border-radius: 8;-fx-border-width: 1.5;-fx-cursor: hand;";
    }
    private String exportCsvBtnStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#C3E6CB" : "#D4EDDA") + ";-fx-background-radius: 8;" +
               "-fx-border-color: #155724;-fx-border-radius: 8;-fx-border-width: 1.5;" +
               "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;-fx-font-weight: bold;" +
               "-fx-text-fill: #155724;-fx-cursor: hand;";
    }
    private String modalNoBtnStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#E9ECEF" : "#F8F9FA") + ";-fx-background-radius: 8;" +
               "-fx-border-color: #CCCCCC;-fx-border-radius: 8;-fx-border-width: 1.5;" +
               "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;-fx-font-weight: bold;" +
               "-fx-text-fill: #555555;-fx-cursor: hand;";
    }
    private String modalYesBtnStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#A93226" : "#882F39") + ";-fx-background-radius: 8;" +
               "-fx-border-color: transparent;-fx-border-radius: 8;-fx-border-width: 0;" +
               "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;-fx-font-weight: bold;" +
               "-fx-text-fill: white;-fx-cursor: hand;";
    }
}