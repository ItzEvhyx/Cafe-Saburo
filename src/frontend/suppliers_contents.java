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

public class suppliers_contents {

    // ══════════════════════════════════════════════════════
    //  LAYOUT CONSTANTS
    // ══════════════════════════════════════════════════════
    private static final double TOP_PADDING  = 20;
    private static final double SIDE_PADDING = 24;
    private static final double HEADER_H     = 56;

    private static final double COL_SUPP_ID    = 0.12;
    private static final double COL_SUPP_NAME  = 0.22;
    private static final double COL_INGREDIENT = 0.26;
    private static final double COL_CONTACT    = 0.18;
    private static final double COL_ADDRESS    = 0.22;

    private static final double ROW_H        = 44;
    private static final double HEADER_ROW_H = 46;
    private static final double CHECKBOX_COL = 48;

    // ── Modal dimensions ──────────────────────────────────
    private static final double MODAL_W     = 440;
    private static final double MODAL_H     = 260;
    private static final double ADD_MODAL_W = 500;
    private static final double ADD_MODAL_H = 460;

    // ══════════════════════════════════════════════════════
    //  STYLE CONSTANTS
    // ══════════════════════════════════════════════════════
    private static final String ACCENT       = "#882F39";
    private static final String FONT_FAMILY  = "Aleo";
    private static final String TABLE_BORDER = "#882F39";
    private static final String ROW_ALT_BG   = "#FDF5F6";
    private static final String ROW_WHITE_BG = "white";
    private static final String HEADER_BG    = "#F5E8EA";

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
    // cachedRows: [supplier_id, supplier_name, ingredients, contact_info, address]
    private List<String[]> cachedRows  = new ArrayList<>();
    private Set<String>    selectedIds = new HashSet<>();

    private Label     editBtn;
    private Label     archiveBtn;
    private Label     addSupplierBtn;
    private Label     archiveAllBtn;
    private Label     confirmBtn;
    private Label     activeTabBtn;
    private Label     archivedTabBtn;
    private Label     deleteBtn;
    private Label     exportCsvBtn;
    private TextField searchField;
    private HBox      searchBar;

    // ── Layout values ─────────────────────────────────────
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

    public suppliers_contents(double totalW, double totalH, Connection conn) {
        this.totalW = totalW;
        this.totalH = totalH;
        this.conn   = conn;
        loadFonts();
    }

    // ══════════════════════════════════════════════════════
    //  PUBLIC LIVE-UPDATE API
    // ══════════════════════════════════════════════════════
    public void prependItem(String supplierId, String supplierName,
                             String ingredients, String contactInfo, String address) {
        String[] newRow = new String[]{
            supplierId   != null ? supplierId   : "--",
            supplierName != null ? supplierName : "--",
            ingredients  != null ? ingredients  : "--",
            contactInfo  != null ? contactInfo  : "--",
            address      != null ? address      : "--"
        };
        if (root != null && currentTab.equals("active")) {
            cachedRows.add(0, newRow);
            rebuildTable();
        }
    }

    // ── Filtered rows ─────────────────────────────────────
    private List<String[]> getFilteredRows() {
        if (searchQuery == null || searchQuery.isBlank()) return cachedRows;
        String q = searchQuery.trim().toLowerCase();
        List<String[]> filtered = new ArrayList<>();
        for (String[] row : cachedRows) {
            if (row[1].toLowerCase().contains(q) ||
                row[0].toLowerCase().contains(q) ||
                row[2].toLowerCase().contains(q))
                filtered.add(row);
        }
        return filtered;
    }

    // ══════════════════════════════════════════════════════
    //  DB OPERATIONS
    // ══════════════════════════════════════════════════════
    private List<String[]> fetchSuppliers(String tab) {
        List<String[]> rows = new ArrayList<>();
        if (conn == null) { System.err.println("[suppliers_contents] conn is null"); return rows; }
        try { if (conn.isClosed()) { System.err.println("[suppliers_contents] conn is closed"); return rows; } }
        catch (Exception e) { return rows; }

        String sql =
            "SELECT supplier_id, supplier_name, ingredients, contact_info, address " +
            "FROM dbo.Suppliers " +
            "WHERE is_deleted = 0 AND [status] = ? " +
            "ORDER BY supplier_name ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tab);
            System.out.println("[suppliers_contents] fetchSuppliers: executing query for tab='" + tab + "'");
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    String suppId   = rs.getString("supplier_id");   if (suppId   == null) suppId   = "—";
                    String name     = rs.getString("supplier_name"); if (name     == null) name     = "—";
                    String ingreds  = rs.getString("ingredients");   if (ingreds  == null) ingreds  = "—";
                    String contact  = rs.getString("contact_info");  if (contact  == null) contact  = "—";
                    String address  = rs.getString("address");       if (address  == null) address  = "—";
                    rows.add(new String[]{ suppId, name, ingreds, contact, address });
                    count++;
                }
                System.out.println("[suppliers_contents] fetchSuppliers: loaded " + count + " row(s) for tab='" + tab + "'");
            }
        } catch (Exception e) {
            System.err.println("[suppliers_contents] fetchSuppliers ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return rows;
    }

    private String insertSupplier(String supplierName, String ingredients,
                                   String contactInfo, String address) {
        if (conn == null) return null;
        try { if (conn.isClosed()) return null; } catch (Exception e) { return null; }

        String newId = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT MAX(CAST(SUBSTRING(supplier_id, 6, LEN(supplier_id)) AS INT)) AS max_num " +
                "FROM dbo.Suppliers WHERE is_deleted = 0")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int maxNum = rs.getInt("max_num");
                    if (rs.wasNull()) maxNum = 0;
                    newId = String.format("SUP-%04d", maxNum + 1);
                }
            }
        } catch (Exception e) { e.printStackTrace(); return null; }

        if (newId == null) return null;

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO dbo.Suppliers (supplier_id, supplier_name, ingredients, contact_info, address) " +
                "VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, newId);
            ps.setString(2, supplierName);
            ps.setString(3, ingredients);
            ps.setString(4, contactInfo);
            ps.setString(5, address);
            ps.executeUpdate();
            System.out.println("[suppliers_contents] insertSupplier: inserted " + newId);
            return newId;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    private void updateSupplier(String supplierId, String supplierName,
                                 String ingredients, String contactInfo, String address) {
        if (conn == null) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE dbo.Suppliers SET supplier_name = ?, ingredients = ?, " +
                "contact_info = ?, address = ? " +
                "WHERE supplier_id = ? AND is_deleted = 0")) {
            ps.setString(1, supplierName);
            ps.setString(2, ingredients);
            ps.setString(3, contactInfo);
            ps.setString(4, address);
            ps.setString(5, supplierId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void archiveSelected(Set<String> ids) {
        if (conn == null || ids.isEmpty()) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Suppliers SET [status] = 'archived' WHERE supplier_id = ? AND is_deleted = 0")) {
                ps.setString(1, id); ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void restoreSelected(Set<String> ids) {
        if (conn == null || ids.isEmpty()) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        for (String id : ids) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE dbo.Suppliers SET [status] = 'active' WHERE supplier_id = ? AND is_deleted = 0")) {
                ps.setString(1, id); ps.executeUpdate();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void hardDeleteAll() {
        if (conn == null) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE dbo.Suppliers SET is_deleted = 1 WHERE is_deleted = 0 AND [status] = ?")) {
            ps.setString(1, currentTab); ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void exportCsv() {
        if (cachedRows.isEmpty()) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Suppliers as CSV");
        chooser.setInitialFileName("suppliers_" + currentTab + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        Stage stage = null;
        try { stage = (Stage) root.getScene().getWindow(); } catch (Exception ignored) {}
        File file = (stage != null) ? chooser.showSaveDialog(stage) : chooser.showSaveDialog(null);
        if (file == null) return;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("Supplier ID,Supplier Name,Ingredients,Contact Info,Address");
            writer.newLine();
            for (String[] row : cachedRows) {
                writer.write(
                    escapeCsv(row[0]) + "," + escapeCsv(row[1]) + "," +
                    escapeCsv(row[2]) + "," + escapeCsv(row[3]) + "," + escapeCsv(row[4])
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
    //  SEARCH BAR REPOSITIONING
    // ══════════════════════════════════════════════════════
    private void repositionSearchBar() {
        if (searchBar == null) return;
        double rightAnchor = archiveMode ? archAllX : activeTabX;
        double newSearchX  = rightAnchor - gap - searchW;
        searchBar.setLayoutX(newSearchX);
    }

    // ══════════════════════════════════════════════════════
    //  ADD SUPPLIER MODAL
    // ══════════════════════════════════════════════════════
    private void openAddSupplierModal() {
        Pane overlay = new Pane();
        overlay.setPrefWidth(totalW); overlay.setPrefHeight(totalH);
        overlay.setMinWidth(totalW);  overlay.setMinHeight(totalH);
        overlay.setMaxWidth(totalW);  overlay.setMaxHeight(totalH);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.45);");

        VBox card = new VBox(0);
        card.setAlignment(Pos.TOP_LEFT);
        card.setMinWidth(ADD_MODAL_W);  card.setMaxWidth(ADD_MODAL_W);  card.setPrefWidth(ADD_MODAL_W);
        card.setMinHeight(ADD_MODAL_H); card.setMaxHeight(ADD_MODAL_H); card.setPrefHeight(ADD_MODAL_H);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 14;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0, 0, 6);"
        );

        HBox cardHeader = new HBox(10);
        cardHeader.setPadding(new Insets(20, 24, 16, 24));
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        cardHeader.setStyle(
            "-fx-background-color: " + HEADER_BG + ";" +
            "-fx-background-radius: 14 14 0 0;" +
            "-fx-border-color: transparent transparent " + TABLE_BORDER + " transparent;" +
            "-fx-border-width: 0 0 1.5 0;"
        );

        FontIcon truckIcon = new FontIcon(FontAwesomeSolid.TRUCK);
        truckIcon.setIconSize(17);
        truckIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        Label modalTitle = new Label("Add Supplier");
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
        cardHeader.getChildren().addAll(truckIcon, modalTitle, hSpacer, closeBtn);

        VBox formBody = new VBox(16);
        formBody.setPadding(new Insets(22, 28, 10, 28));
        VBox.setVgrow(formBody, Priority.ALWAYS);

        VBox nameField     = buildFormField(FontAwesomeSolid.BUILDING,        "Supplier Name",    "e.g. Fresh Farms Co.");
        VBox ingredField   = buildFormField(FontAwesomeSolid.SEEDLING,        "Ingredients",      "e.g. Espresso Beans, Oat Milk");
        VBox contactField  = buildFormField(FontAwesomeSolid.PHONE,           "Contact Info",     "e.g. +63 912 345 6789");
        VBox addressField  = buildFormField(FontAwesomeSolid.MAP_MARKER_ALT,  "Physical Address", "e.g. 123 Rizal St., Makati City");

        TextField nameInput    = extractTextField(nameField);
        TextField ingredInput  = extractTextField(ingredField);
        TextField contactInput = extractTextField(contactField);
        TextField addressInput = extractTextField(addressField);

        Label errorLbl = new Label("");
        errorLbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #882F39;"
        );
        errorLbl.setVisible(false);
        errorLbl.setManaged(false);

        formBody.getChildren().addAll(nameField, ingredField, contactField, addressField, errorLbl);

        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 28, 24, 28));

        Label cancelBtn = new Label("Cancel");
        cancelBtn.setCursor(javafx.scene.Cursor.HAND);
        cancelBtn.setPrefWidth(120); cancelBtn.setPrefHeight(38);
        cancelBtn.setAlignment(Pos.CENTER);
        cancelBtn.setStyle(modalNoBtnStyle(false));
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(modalNoBtnStyle(true)));
        cancelBtn.setOnMouseExited(e  -> cancelBtn.setStyle(modalNoBtnStyle(false)));
        cancelBtn.setOnMouseClicked(e -> stackRoot.getChildren().remove(overlay));

        FontIcon saveIcon = new FontIcon(FontAwesomeSolid.PLUS_CIRCLE);
        saveIcon.setIconSize(13);
        saveIcon.setIconColor(javafx.scene.paint.Color.WHITE);
        Label saveBtn = new Label("Add Supplier");
        saveBtn.setGraphic(saveIcon);
        saveBtn.setGraphicTextGap(7);
        saveBtn.setCursor(javafx.scene.Cursor.HAND);
        saveBtn.setPrefWidth(140); saveBtn.setPrefHeight(38);
        saveBtn.setAlignment(Pos.CENTER);
        saveBtn.setStyle(addSaveBtnStyle(false));
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle(addSaveBtnStyle(true)));
        saveBtn.setOnMouseExited(e  -> saveBtn.setStyle(addSaveBtnStyle(false)));
        saveBtn.setOnMouseClicked(e -> {
            String nameVal    = nameInput.getText().trim();
            String ingredVal  = ingredInput.getText().trim();
            String contactVal = contactInput.getText().trim();
            String addressVal = addressInput.getText().trim();

            if (nameVal.isEmpty() || ingredVal.isEmpty() || contactVal.isEmpty() || addressVal.isEmpty()) {
                errorLbl.setText("⚠  All fields are required.");
                errorLbl.setVisible(true);
                errorLbl.setManaged(true);
                return;
            }

            String newId = insertSupplier(nameVal, ingredVal, contactVal, addressVal);
            if (newId == null) {
                errorLbl.setText("⚠  Failed to save. Check connection.");
                errorLbl.setVisible(true);
                errorLbl.setManaged(true);
                return;
            }

            stackRoot.getChildren().remove(overlay);
            if (currentTab.equals("active")) {
                prependItem(newId, nameVal, ingredVal, contactVal, addressVal);
            }
        });

        footer.getChildren().addAll(cancelBtn, saveBtn);
        card.getChildren().addAll(cardHeader, formBody, footer);

        StackPane centred = new StackPane(card);
        centred.setPrefWidth(totalW); centred.setPrefHeight(totalH);
        centred.setMinWidth(totalW);  centred.setMinHeight(totalH);
        centred.setMaxWidth(totalW);  centred.setMaxHeight(totalH);
        centred.setAlignment(Pos.CENTER);
        overlay.getChildren().add(centred);
        stackRoot.getChildren().add(overlay);
    }

    // ── Form field factory (empty) ────────────────────────
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

    // ── Form field factory (with pre-filled value) ────────
    private VBox buildFormFieldWithValue(FontAwesomeSolid iconCode, String label, String value) {
        VBox field = buildFormField(iconCode, label, "");
        extractTextField(field).setText(value);
        return field;
    }

    private TextField extractTextField(VBox fieldBox) {
        HBox inputBox = (HBox) fieldBox.getChildren().get(1);
        return (TextField) inputBox.getChildren().get(1);
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

        double btnH = 36;
        btnY     = TOP_PADDING + 10;
        iconW    = 36;
        gap      = 8;
        tabW     = 90;
        archAllW = 100;
        confirmW = 90;
        csvW     = 120;
        double addW = 130;
        searchW  = 200;

        Label title = new Label("Suppliers");
        title.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 36px;" +
            "-fx-font-weight: 800;" +
            "-fx-text-fill: " + ACCENT + ";"
        );

        FontIcon plusIcon = new FontIcon(FontAwesomeSolid.PLUS_CIRCLE);
        plusIcon.setIconSize(14);
        plusIcon.setIconColor(javafx.scene.paint.Color.web("#155724"));
        addSupplierBtn = new Label("Add Supplier");
        addSupplierBtn.setGraphic(plusIcon);
        addSupplierBtn.setGraphicTextGap(6);
        addSupplierBtn.setCursor(javafx.scene.Cursor.HAND);
        addSupplierBtn.setStyle(addSupplierBtnStyle(false));
        addSupplierBtn.setPrefHeight(btnH); addSupplierBtn.setPrefWidth(addW);
        addSupplierBtn.setAlignment(Pos.CENTER);
        addSupplierBtn.setOnMouseEntered(e -> addSupplierBtn.setStyle(addSupplierBtnStyle(true)));
        addSupplierBtn.setOnMouseExited(e  -> addSupplierBtn.setStyle(addSupplierBtnStyle(false)));
        addSupplierBtn.setOnMouseClicked(e -> openAddSupplierModal());

        FontIcon penIcon = new FontIcon(FontAwesomeSolid.PEN);
        penIcon.setIconSize(15);
        penIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));
        editBtn = new Label();
        editBtn.setGraphic(penIcon);
        editBtn.setCursor(javafx.scene.Cursor.HAND);
        editBtn.setStyle(editBtnStyle(false));
        editBtn.setPrefHeight(btnH); editBtn.setPrefWidth(iconW);
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

        FontIcon boxIcon = new FontIcon(FontAwesomeSolid.ARCHIVE);
        boxIcon.setIconSize(15);
        boxIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));
        archiveBtn = new Label();
        archiveBtn.setGraphic(boxIcon);
        archiveBtn.setCursor(javafx.scene.Cursor.HAND);
        archiveBtn.setStyle(archiveBtnStyle(false));
        archiveBtn.setPrefHeight(btnH); archiveBtn.setPrefWidth(iconW);
        archiveBtn.setAlignment(Pos.CENTER);
        archiveBtn.setOnMouseEntered(e -> archiveBtn.setStyle(archiveBtnStyle(archiveMode)));
        archiveBtn.setOnMouseExited(e  -> archiveBtn.setStyle(archiveBtnStyle(archiveMode)));
        archiveBtn.setOnMouseClicked(e -> toggleArchiveMode());

        HBox titleRow = new HBox(gap, title, addSupplierBtn, editBtn, archiveBtn);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.setLayoutX(SIDE_PADDING); titleRow.setLayoutY(TOP_PADDING);
        titleRow.setPrefHeight(HEADER_H);

        deleteX      = totalW - SIDE_PADDING - iconW;
        exportCsvX   = deleteX      - gap - csvW;
        archivedTabX = exportCsvX   - gap - tabW;
        activeTabX   = archivedTabX - gap - tabW;
        confirmX     = activeTabX   - gap - confirmW;
        archAllX     = confirmX     - gap - archAllW;

        double initialSearchX = activeTabX - gap - searchW;

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
                "Suppliers (" + currentTab + ")",
                "This will permanently remove all suppliers in this view.\nThis action cannot be undone.",
                () -> { hardDeleteAll(); cachedRows.clear(); selectedIds.clear(); rebuildTable(); }
            ))
        );

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

        activeTabBtn = buildTabLabel("Active", true);
        activeTabBtn.setLayoutX(activeTabX); activeTabBtn.setLayoutY(btnY);

        archivedTabBtn = buildTabLabel("Archived", false);
        archivedTabBtn.setLayoutX(archivedTabX); archivedTabBtn.setLayoutY(btnY);

        activeTabBtn.setOnMouseEntered(e -> {
            if (!currentTab.equals("active")) activeTabBtn.setStyle(tabBtnHoverStyle());
        });
        activeTabBtn.setOnMouseExited(e ->
            activeTabBtn.setStyle(tabBtnStyle(currentTab.equals("active"))));
        activeTabBtn.setOnMouseClicked(e -> switchTab("active"));

        archivedTabBtn.setOnMouseEntered(e -> {
            if (!currentTab.equals("archived")) archivedTabBtn.setStyle(tabBtnHoverStyle());
        });
        archivedTabBtn.setOnMouseExited(e ->
            archivedTabBtn.setStyle(tabBtnStyle(currentTab.equals("archived"))));
        archivedTabBtn.setOnMouseClicked(e -> switchTab("archived"));

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
            archiveBtn.setStyle(archiveBtnStyle(false));
            repositionSearchBar();
            cachedRows = fetchSuppliers(currentTab);
            rebuildTable();
        });

        FontIcon searchIcon = new FontIcon(FontAwesomeSolid.SEARCH);
        searchIcon.setIconSize(14);
        searchIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        searchField = new TextField();
        searchField.setPromptText("Search supplier or ingredient...");
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
        searchBar.setPrefWidth(searchW); searchBar.setPrefHeight(btnH);
        searchBar.setLayoutX(initialSearchX); searchBar.setLayoutY(btnY);
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

        double tableY = TOP_PADDING + HEADER_H + 10;
        double tableW = totalW - SIDE_PADDING * 2;
        double tableH = totalH - tableY - SIDE_PADDING;

        cachedRows  = fetchSuppliers("active");
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
        currentTab = tab; editMode = false; archiveMode = false; selectedIds.clear();
        searchQuery = "";
        if (searchField != null) searchField.clear();

        FontIcon penIcon = new FontIcon(FontAwesomeSolid.PEN);
        penIcon.setIconSize(15);
        penIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));
        editBtn.setGraphic(penIcon);
        editBtn.setStyle(editBtnStyle(false));

        updateArchiveBtnIcon();
        archiveAllBtn.setText(tab.equals("archived") ? "Restore All" : "Archive All");
        archiveAllBtn.setVisible(false); confirmBtn.setVisible(false);
        archiveBtn.setStyle(archiveBtnStyle(false));
        activeTabBtn.setStyle(tabBtnStyle(tab.equals("active")));
        archivedTabBtn.setStyle(tabBtnStyle(tab.equals("archived")));
        repositionSearchBar();
        cachedRows = fetchSuppliers(tab);
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
            "-fx-background-color: white;-fx-background-radius: 14;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0, 0, 6);"
        );

        FontIcon warnIcon = new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
        warnIcon.setIconSize(28);
        warnIcon.setIconColor(javafx.scene.paint.Color.web("#882F39"));

        Label heading = new Label("Are you sure you want to delete\nall entries for " + context + "?");
        heading.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 14px;-fx-font-weight: bold;" +
            "-fx-text-fill: #222222;-fx-text-alignment: center;-fx-alignment: center;"
        );
        heading.setAlignment(Pos.CENTER); heading.setWrapText(true); heading.setMaxWidth(MODAL_W - 80);

        Label sub = new Label(subMessage);
        sub.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 12px;" +
            "-fx-text-fill: #777777;-fx-text-alignment: center;-fx-alignment: center;"
        );
        sub.setAlignment(Pos.CENTER); sub.setWrapText(true); sub.setMaxWidth(MODAL_W - 80);

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
        sp.setFitToWidth(true); sp.setPannable(true);
        sp.setStyle(
            "-fx-background: transparent;-fx-background-color: transparent;" +
            "-fx-border-color: transparent;-fx-padding: 0;"
        );
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
        table.setStyle(
            "-fx-border-color: " + TABLE_BORDER + ";-fx-border-width: 1.5;" +
            "-fx-border-radius: 10;-fx-background-color: white;-fx-background-radius: 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 10, 0, 0, 3);"
        );
        table.getChildren().add(buildHeaderRow(tableW, dataW));
        if (rows.isEmpty()) {
            String msg = !searchQuery.isBlank()
                ? "No results found for \"" + searchQuery + "\"."
                : currentTab.equals("archived") ? "No archived suppliers." : "No suppliers found.";
            Label empty = new Label(msg);
            empty.setStyle(
                "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 14px;" +
                "-fx-text-fill: #AAAAAA;-fx-padding: 24 0 24 16;"
            );
            table.getChildren().add(empty);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                String[] item = rows.get(i);
                table.getChildren().add(buildDataRow(
                    item[0], item[1], item[2], item[3], item[4],
                    i % 2 == 0 ? ROW_WHITE_BG : ROW_ALT_BG,
                    tableW, dataW, i == rows.size() - 1
                ));
            }
        }
        return table;
    }

    private HBox buildHeaderRow(double tableW, double dataW) {
        HBox row = new HBox(0);
        row.setPrefHeight(HEADER_ROW_H);
        row.setStyle(
            "-fx-background-color: " + HEADER_BG + ";-fx-background-radius: 10 10 0 0;" +
            "-fx-border-color: transparent transparent " + TABLE_BORDER + " transparent;" +
            "-fx-border-width: 0 0 1.5 0;"
        );
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(
            buildHeaderCell("Supplier ID",   dataW * COL_SUPP_ID),    buildColDivider(),
            buildHeaderCell("Supplier Name", dataW * COL_SUPP_NAME),  buildColDivider(),
            buildHeaderCell("Ingredient",    dataW * COL_INGREDIENT), buildColDivider(),
            buildHeaderCell("Contact Info",  dataW * COL_CONTACT),    buildColDivider(),
            buildHeaderCell("Address",       dataW * COL_ADDRESS)
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
        lbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 14px;" +
            "-fx-font-weight: bold;-fx-text-fill: " + ACCENT + ";"
        );
        return lbl;
    }

    private HBox buildDataRow(String supplierId, String supplierName,
                               String ingredients, String contactInfo, String address,
                               String bg, double tableW, double dataW, boolean isLast) {
        HBox row = new HBox(0);
        // Use BASELINE alignment so wrapped cells align correctly
        row.setAlignment(Pos.TOP_LEFT);
        String  bottomRadius = isLast ? "0 0 10 10" : "0";
        String  borderBottom = isLast ? "0" : "1";
        boolean selected     = selectedIds.contains(supplierId);
        row.setStyle(rowStyle(selected ? "#FDE8EA" : bg, bottomRadius, borderBottom));
        row.setOnMouseEntered(e -> {
            if (!selectedIds.contains(supplierId))
                row.setStyle(rowStyle("#F5E8EA", bottomRadius, borderBottom));
        });
        row.setOnMouseExited(e ->
            row.setStyle(rowStyle(selectedIds.contains(supplierId) ? "#FDE8EA" : bg, bottomRadius, borderBottom))
        );

        // ── Build cells: text or editable depending on editMode ──
        javafx.scene.Node nameCell;
        javafx.scene.Node ingredCell;
        javafx.scene.Node contactCell;
        javafx.scene.Node addressCell;

        if (editMode && currentTab.equals("active")) {
            nameCell    = buildEditableTextCell(supplierId, supplierName,   ingredients, contactInfo, address, 1, dataW * COL_SUPP_NAME);
            ingredCell  = buildEditableTextCell(supplierId, supplierName,   ingredients, contactInfo, address, 2, dataW * COL_INGREDIENT);
            contactCell = buildEditableTextCell(supplierId, supplierName,   ingredients, contactInfo, address, 3, dataW * COL_CONTACT);
            addressCell = buildEditableTextCell(supplierId, supplierName,   ingredients, contactInfo, address, 4, dataW * COL_ADDRESS);
        } else {
            nameCell    = buildTextCell(supplierName, dataW * COL_SUPP_NAME,  false);
            ingredCell  = buildTextCell(ingredients,  dataW * COL_INGREDIENT, false);
            contactCell = buildTextCell(contactInfo,  dataW * COL_CONTACT,    false);
            addressCell = buildTextCell(address,      dataW * COL_ADDRESS,    false);
        }

        row.getChildren().addAll(
            buildTextCell(supplierId, dataW * COL_SUPP_ID, true), buildColDivider(),
            nameCell,                                              buildColDivider(),
            ingredCell,                                            buildColDivider(),
            contactCell,                                           buildColDivider(),
            addressCell
        );

        if (archiveMode) {
            row.getChildren().add(buildColDivider());
            CheckBox cb = new CheckBox();
            cb.setSelected(selected); cb.setStyle("-fx-cursor: hand;");
            cb.setOnAction(e -> {
                if (cb.isSelected()) {
                    selectedIds.add(supplierId);
                    row.setStyle(rowStyle("#FDE8EA", bottomRadius, borderBottom));
                } else {
                    selectedIds.remove(supplierId);
                    row.setStyle(rowStyle(bg, bottomRadius, borderBottom));
                }
            });
            HBox cbCell = new HBox(cb);
            cbCell.setPrefWidth(CHECKBOX_COL);
            cbCell.setMinHeight(ROW_H);
            cbCell.setPadding(new Insets(12, 0, 12, 0));
            cbCell.setAlignment(Pos.TOP_CENTER);
            row.getChildren().add(cbCell);
        }
        return row;
    }

    // ══════════════════════════════════════════════════════
    //  EDITABLE TEXT CELL
    //  colIndex: 1=supplier_name, 2=ingredients,
    //            3=contact_info,  4=address
    // ══════════════════════════════════════════════════════
    private HBox buildEditableTextCell(String supplierId,
                                        String currentName, String currentIngred,
                                        String currentContact, String currentAddress,
                                        int colIndex, double width) {

        // Seed field with the value for this column
        String initialValue;
        switch (colIndex) {
            case 1:  initialValue = currentName;    break;
            case 2:  initialValue = currentIngred;  break;
            case 3:  initialValue = currentContact; break;
            default: initialValue = currentAddress; break;
        }

        TextField field = new TextField(initialValue);
        field.setPrefWidth(width - 24);
        field.setPrefHeight(ROW_H - 12);
        field.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 6;" +
            "-fx-border-color: " + ACCENT + ";" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 6;" +
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #222222;" +
            "-fx-padding: 4 8 4 8;"
        );

        Runnable save = () -> {
            String newVal = field.getText().trim();
            if (newVal.isEmpty()) return;
            for (String[] r : cachedRows) {
                if (r[0].equals(supplierId)) {
                    // Update only this column in the cache
                    r[colIndex] = newVal;
                    // Persist all four editable columns to DB
                    updateSupplier(supplierId, r[1], r[2], r[3], r[4]);
                    break;
                }
            }
        };

        field.setOnAction(e -> save.run());
        field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) save.run();
        });

        HBox cell = new HBox(field);
        cell.setPrefWidth(width); cell.setMinHeight(ROW_H);
        cell.setPadding(new Insets(6, 6, 6, 10));
        cell.setAlignment(Pos.TOP_LEFT);
        return cell;
    }

    // ══════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════
    private String rowStyle(String bg, String bottomRadius, String borderBottom) {
        return "-fx-background-color: " + bg + ";-fx-background-radius: " + bottomRadius + ";" +
               "-fx-border-color: transparent transparent " + TABLE_BORDER + " transparent;" +
               "-fx-border-width: 0 0 " + borderBottom + " 0;";
    }

    /**
     * Builds a text cell that wraps its content so long values are never clipped.
     * The label has no fixed height — it grows with the text — and the containing
     * HBox has a minimum height of ROW_H so short rows still look comfortable.
     */
    private HBox buildTextCell(String text, double width, boolean bold) {
        Label lbl = new Label(text != null ? text : "—");
        lbl.setPrefWidth(width - 16);   // leave room for left padding
        lbl.setMaxWidth(width - 16);
        lbl.setWrapText(true);          // ← KEY: allow wrapping
        lbl.setPadding(new Insets(10, 8, 10, 0));
        lbl.setAlignment(Pos.TOP_LEFT);
        lbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;" +
            "-fx-font-weight: " + (bold ? "bold" : "normal") + ";-fx-text-fill: #333333;"
        );

        HBox cell = new HBox(lbl);
        cell.setPrefWidth(width);
        cell.setMinHeight(ROW_H);       // never shorter than one normal row
        cell.setPadding(new Insets(0, 0, 0, 16));
        cell.setAlignment(Pos.TOP_LEFT);
        return cell;
    }

    private Region buildColDivider() {
        Region div = new Region();
        div.setPrefWidth(1.5); div.setMinWidth(1.5); div.setMaxWidth(1.5);
        div.setStyle("-fx-background-color: " + TABLE_BORDER + "; -fx-opacity: 0.35;");
        VBox.setVgrow(div, Priority.ALWAYS);
        return div;
    }

    // ══════════════════════════════════════════════════════
    //  STYLE HELPERS
    // ══════════════════════════════════════════════════════
    private String tabBtnStyle(boolean selected) {
        return selected
            ? "-fx-background-color: " + ACCENT + ";-fx-background-radius: 8;" +
              "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;" +
              "-fx-font-weight: bold;-fx-text-fill: white;-fx-cursor: hand;"
            : "-fx-background-color: #F5E8EA;-fx-background-radius: 8;" +
              "-fx-border-color: " + ACCENT + ";-fx-border-radius: 8;-fx-border-width: 1.5;" +
              "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;" +
              "-fx-font-weight: bold;-fx-text-fill: " + ACCENT + ";-fx-cursor: hand;";
    }

    private String tabBtnHoverStyle() {
        return "-fx-background-color: #EDD5D8;-fx-background-radius: 8;" +
               "-fx-border-color: " + ACCENT + ";-fx-border-radius: 8;-fx-border-width: 1.5;" +
               "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;" +
               "-fx-font-weight: bold;-fx-text-fill: " + ACCENT + ";-fx-cursor: hand;";
    }

    private String archiveBtnStyle(boolean active) {
        return "-fx-background-color: " + (active ? "#D4EDDA" : "#F5E8EA") + ";-fx-background-radius: 8;" +
               "-fx-border-color: " + (active ? "#155724" : ACCENT) + ";-fx-border-radius: 8;" +
               "-fx-border-width: 1.5;-fx-cursor: hand;";
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

    private String editBtnStyle(boolean active) {
        return "-fx-background-color: " + (active ? "#D4EDDA" : "#F5E8EA") + ";-fx-background-radius: 8;" +
               "-fx-border-color: " + (active ? "#155724" : ACCENT) + ";" +
               "-fx-border-radius: 8;-fx-border-width: 1.5;-fx-cursor: hand;";
    }

    private String editBtnHoverStyle(boolean active) {
        return "-fx-background-color: " + (active ? "#C3E6CB" : "#EDD5D8") + ";-fx-background-radius: 8;" +
               "-fx-border-color: " + (active ? "#155724" : ACCENT) + ";" +
               "-fx-border-radius: 8;-fx-border-width: 1.5;-fx-cursor: hand;";
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

    private String addSupplierBtnStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#C3E6CB" : "#D4EDDA") + ";-fx-background-radius: 8;" +
               "-fx-border-color: #155724;-fx-border-radius: 8;-fx-border-width: 1.5;" +
               "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;-fx-font-weight: bold;" +
               "-fx-text-fill: #155724;-fx-cursor: hand;";
    }

    private String addSaveBtnStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#A93226" : "#882F39") + ";-fx-background-radius: 8;" +
               "-fx-border-color: transparent;-fx-border-radius: 8;-fx-border-width: 0;" +
               "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;-fx-font-weight: bold;" +
               "-fx-text-fill: white;-fx-cursor: hand;";
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