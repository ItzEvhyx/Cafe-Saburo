package frontend;

import backend.inventory_util;

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
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class inventory_contents {

    // ══════════════════════════════════════════════════════
    //  LAYOUT CONSTANTS
    // ══════════════════════════════════════════════════════
    private static final double TOP_PADDING  = 20;
    private static final double SIDE_PADDING = 24;
    private static final double HEADER_H     = 56;

    private static final double COL_INV_ID     = 0.12;
    private static final double COL_INGREDIENT = 0.31;
    private static final double COL_QUANTITY   = 0.18;
    private static final double COL_UNIT       = 0.16;
    private static final double COL_REORDER    = 0.23;

    private static final double ROW_H        = 44;
    private static final double HEADER_ROW_H = 46;
    private static final double CHECKBOX_COL = 48;

    // ── Modal dimensions ──────────────────────────────────
    private static final double MODAL_W     = 440;
    private static final double MODAL_H     = 260;
    private static final double ADD_MODAL_W = 480;
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
    private List<String[]> cachedRows  = new ArrayList<>();
    private Set<String>    selectedIds = new HashSet<>();

    private Label     editBtn;
    private Label     archiveBtn;
    private Label     addIngredientBtn;
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

    public inventory_contents(double totalW, double totalH, Connection conn) {
        this.totalW = totalW;
        this.totalH = totalH;
        this.conn   = conn;
        inventory_util.loadFonts();
    }

    // ══════════════════════════════════════════════════════
    //  PUBLIC LIVE-UPDATE API
    // ══════════════════════════════════════════════════════
    public void prependItem(String inventoryId, String ingredient,
                             String quantity, String unit, String reorderLevel) {
        String[] newRow = new String[]{
            inventoryId  != null ? inventoryId  : "--",
            ingredient   != null ? ingredient   : "--",
            quantity     != null ? quantity     : "--",
            unit         != null ? unit         : "--",
            reorderLevel != null ? reorderLevel : "--"
        };
        if (root != null && currentTab.equals("active")) {
            cachedRows.add(0, newRow);
            rebuildTable();
        }
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
    //  INGREDIENT MULTI-SELECT DROPDOWN
    //  Mirrors the pattern used in suppliers_contents.
    //
    //  Items stored in trigger.getProperties():
    //    "items"        → List<String[]> { [0]=ingredient_id, [1]=ingredient_name }
    //    "selectedIds"  → Set<String> of currently selected ingredient_ids
    //    "summaryLabel" → Label showing the selection summary
    //    "open"         → Boolean popup-open flag
    // ══════════════════════════════════════════════════════

    private VBox buildIngredientDropdownField(FontAwesomeSolid iconCode, String label) {
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

        Label summaryLabel = new Label("Select ingredient(s)...");
        summaryLabel.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #AAAAAA;"
        );
        summaryLabel.setMaxWidth(Double.MAX_VALUE);
        summaryLabel.setEllipsisString("…");
        summaryLabel.setWrapText(false);
        HBox.setHgrow(summaryLabel, Priority.ALWAYS);

        FontIcon arrowIcon = new FontIcon(FontAwesomeSolid.CHEVRON_DOWN);
        arrowIcon.setIconSize(11);
        arrowIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        HBox trigger = new HBox(10, fi, summaryLabel, arrowIcon);
        trigger.setAlignment(Pos.CENTER_LEFT);
        trigger.setPadding(new Insets(0, 14, 0, 14));
        trigger.setPrefHeight(40);
        trigger.setMaxWidth(Double.MAX_VALUE);
        trigger.setCursor(javafx.scene.Cursor.HAND);
        trigger.setStyle(dropdownTriggerStyle(false));

        Set<String> selectedIngredientIds = new HashSet<>();
        trigger.getProperties().put("items",        new ArrayList<String[]>());
        trigger.getProperties().put("selectedIds",  selectedIngredientIds);
        trigger.getProperties().put("summaryLabel", summaryLabel);
        trigger.getProperties().put("open",         false);

        trigger.setOnMouseEntered(e -> {
            if (!Boolean.TRUE.equals(trigger.getProperties().get("open")))
                trigger.setStyle(dropdownTriggerStyle(true));
        });
        trigger.setOnMouseExited(e -> {
            if (!Boolean.TRUE.equals(trigger.getProperties().get("open")))
                trigger.setStyle(dropdownTriggerStyle(false));
        });
        trigger.setOnMouseClicked(e -> openIngredientDropdown(trigger));

        VBox wrapper = new VBox(6, fieldLabel, trigger);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        return wrapper;
    }

    private String dropdownTriggerStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#FDF0F1" : "white") + ";" +
               "-fx-background-radius: 10;" +
               "-fx-border-color: " + ACCENT + ";" +
               "-fx-border-width: 1.5;" +
               "-fx-border-radius: 10;";
    }

    /** Populates the dropdown items. Each String[] must be: [0]=ingredient_id, [1]=ingredient_name */
    @SuppressWarnings("unchecked")
    private void setIngredientDropdownItems(VBox fieldBox, List<String[]> ingredients) {
        HBox trigger = (HBox) fieldBox.getChildren().get(1);
        List<String[]> items = (List<String[]>) trigger.getProperties().get("items");
        items.clear();
        items.addAll(ingredients);
    }

    /** Returns the Set of selected ingredient_ids from the dropdown. */
    @SuppressWarnings("unchecked")
    private Set<String> getIngredientDropdownSelected(VBox fieldBox) {
        HBox trigger = (HBox) fieldBox.getChildren().get(1);
        return (Set<String>) trigger.getProperties().get("selectedIds");
    }

    /**
     * Returns the ingredient_name for a given ingredient_id
     * from the dropdown's item list. Used to resolve names
     * for the prependItem() live-update call after saving.
     */
    @SuppressWarnings("unchecked")
    private String getIngredientNameById(VBox fieldBox, String ingredientId) {
        HBox trigger = (HBox) fieldBox.getChildren().get(1);
        List<String[]> items = (List<String[]>) trigger.getProperties().get("items");
        for (String[] item : items) {
            if (item[0].equals(ingredientId)) return item[1];
        }
        return ingredientId;
    }

    /** Opens the multi-select popup for ingredient selection. */
    @SuppressWarnings("unchecked")
    private void openIngredientDropdown(HBox trigger) {
        List<String[]> items = (List<String[]>) trigger.getProperties().get("items");
        if (items == null || items.isEmpty()) return;

        Set<String> selectedIngredientIds = (Set<String>) trigger.getProperties().get("selectedIds");
        Label summaryLabel = (Label) trigger.getProperties().get("summaryLabel");

        VBox listBox = new VBox(0);
        listBox.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: " + ACCENT + ";" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 12, 0, 0, 4);"
        );

        ScrollPane sp = new ScrollPane(listBox);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setStyle(
            "-fx-background: transparent;-fx-background-color: transparent;" +
            "-fx-border-color: transparent;-fx-padding: 0;" +
            "-fx-background-radius: 10;"
        );
        sp.setMaxHeight(200);

        Popup popup = new Popup();
        popup.setAutoHide(true);

        double trigW = trigger.getWidth() > 0 ? trigger.getWidth() : ADD_MODAL_W - 56;
        sp.setPrefWidth(trigW);
        listBox.setPrefWidth(trigW);

        Runnable refreshSummary = () -> {
            if (selectedIngredientIds.isEmpty()) {
                summaryLabel.setText("Select ingredient(s)...");
                summaryLabel.setStyle(
                    "-fx-font-family: '" + FONT_FAMILY + "';" +
                    "-fx-font-size: 13px;" +
                    "-fx-text-fill: #AAAAAA;"
                );
            } else {
                String names = items.stream()
                    .filter(it -> selectedIngredientIds.contains(it[0]))
                    .map(it -> it[1])
                    .collect(Collectors.joining(", "));
                summaryLabel.setText(selectedIngredientIds.size() + " selected: " + names);
                summaryLabel.setStyle(
                    "-fx-font-family: '" + FONT_FAMILY + "';" +
                    "-fx-font-size: 13px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: " + ACCENT + ";"
                );
            }
        };

        for (int i = 0; i < items.size(); i++) {
            String[] ing    = items.get(i);
            String   ingId  = ing[0];
            String   ingName = ing[1];
            boolean  isSel  = selectedIngredientIds.contains(ingId);
            boolean  isLast = (i == items.size() - 1);

            CheckBox cb = new CheckBox(ingName);
            cb.setSelected(isSel);
            cb.setStyle(
                "-fx-font-family: '" + FONT_FAMILY + "';" +
                "-fx-font-size: 13px;" +
                "-fx-text-fill: #333333;" +
                "-fx-cursor: hand;"
            );

            HBox row = new HBox(cb);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(9, 14, 9, 14));
            row.setMaxWidth(Double.MAX_VALUE);
            row.setCursor(javafx.scene.Cursor.HAND);
            String rowRadius = isLast ? "0 0 9 9" : "0";
            row.setStyle(ingredRowStyle(isSel, false, rowRadius));

            cb.setOnAction(e -> {
                if (cb.isSelected()) selectedIngredientIds.add(ingId);
                else                 selectedIngredientIds.remove(ingId);
                row.setStyle(ingredRowStyle(cb.isSelected(), false, rowRadius));
                refreshSummary.run();
            });
            row.setOnMouseEntered(e -> row.setStyle(ingredRowStyle(cb.isSelected(), true, rowRadius)));
            row.setOnMouseExited(e  -> row.setStyle(ingredRowStyle(cb.isSelected(), false, rowRadius)));
            row.setOnMouseClicked(e -> {
                if (e.getTarget() != cb) {
                    cb.setSelected(!cb.isSelected());
                    if (cb.isSelected()) selectedIngredientIds.add(ingId);
                    else                 selectedIngredientIds.remove(ingId);
                    row.setStyle(ingredRowStyle(cb.isSelected(), false, rowRadius));
                    refreshSummary.run();
                }
            });

            listBox.getChildren().add(row);
        }

        popup.getContent().add(sp);
        popup.setOnHidden(e -> {
            trigger.getProperties().put("open", false);
            trigger.setStyle(dropdownTriggerStyle(false));
        });

        javafx.geometry.Bounds bounds = trigger.localToScreen(trigger.getBoundsInLocal());
        if (bounds != null) {
            popup.show(trigger, bounds.getMinX(), bounds.getMaxY() + 2);
        }
        trigger.getProperties().put("open", true);
        trigger.setStyle(dropdownTriggerStyle(false));
    }

    private String ingredRowStyle(boolean selected, boolean hovered, String radius) {
        String bg = selected && hovered ? "#EDD5D8"
                  : selected            ? "#F5E8EA"
                  : hovered             ? "#FDF0F1"
                  :                       "white";
        return "-fx-background-color: " + bg + ";" +
               "-fx-background-radius: " + radius + ";";
    }

    // ══════════════════════════════════════════════════════
    //  ADD INGREDIENT MODAL
    //  The Ingredient Name field is now a multi-select
    //  dropdown populated from dbo.Ingredients via
    //  inventory_util.fetchAllIngredients(). One inventory
    //  row is inserted per selected ingredient.
    // ══════════════════════════════════════════════════════
    private void openAddIngredientModal() {
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

        // ── Modal header ──────────────────────────────────
        HBox cardHeader = new HBox(10);
        cardHeader.setPadding(new Insets(20, 24, 16, 24));
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        cardHeader.setStyle(
            "-fx-background-color: " + HEADER_BG + ";" +
            "-fx-background-radius: 14 14 0 0;" +
            "-fx-border-color: transparent transparent " + TABLE_BORDER + " transparent;" +
            "-fx-border-width: 0 0 1.5 0;"
        );

        FontIcon leafIcon = new FontIcon(FontAwesomeSolid.LEAF);
        leafIcon.setIconSize(17);
        leafIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        Label modalTitle = new Label("Add Ingredient");
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

        cardHeader.getChildren().addAll(leafIcon, modalTitle, hSpacer, closeBtn);

        // ── Form body ─────────────────────────────────────
        VBox formBody = new VBox(18);
        formBody.setPadding(new Insets(26, 28, 10, 28));
        VBox.setVgrow(formBody, Priority.ALWAYS);

        // Ingredient dropdown — replaces the old free-text TextField
        VBox ingredientDropdown = buildIngredientDropdownField(
            FontAwesomeSolid.SEEDLING, "Ingredient (select one or more)");

        // Load ingredients from dbo.Ingredients directly
        List<String[]> allIngredients;
        try {
            allIngredients = inventory_util.fetchAllIngredients(conn);
            if (allIngredients == null) allIngredients = new ArrayList<>();
        } catch (Exception ex) {
            System.err.println("[inventory_contents] fetchAllIngredients threw: " + ex.getMessage());
            ex.printStackTrace();
            allIngredients = new ArrayList<>();
        }

        if (!allIngredients.isEmpty()) {
            setIngredientDropdownItems(ingredientDropdown, allIngredients);
        }

        // Keep a final reference for the save lambda
        final List<String[]> finalAllIngredients = allIngredients;

        VBox quantityField = buildFormField(FontAwesomeSolid.BALANCE_SCALE,      "Quantity",             "e.g. 50");
        VBox unitField     = buildFormField(FontAwesomeSolid.TAG,                "Unit (ml/l/g/kg/pcs)", "e.g. kg");
        VBox reorderField  = buildFormField(FontAwesomeSolid.EXCLAMATION_CIRCLE, "Reorder Level",        "e.g. 10");

        TextField quantityInput = extractTextField(quantityField);
        TextField unitInput     = extractTextField(unitField);
        TextField reorderInput  = extractTextField(reorderField);

        // Integer-only filter for quantity and reorder
        quantityInput.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) quantityInput.setText(n.replaceAll("[^\\d]", ""));
        });
        reorderInput.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) reorderInput.setText(n.replaceAll("[^\\d]", ""));
        });

        HBox qtyUnitRow = new HBox(14, quantityField, unitField);
        HBox.setHgrow(quantityField, Priority.ALWAYS);
        HBox.setHgrow(unitField, Priority.ALWAYS);

        Label errorLbl = new Label("");
        errorLbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #882F39;"
        );
        errorLbl.setVisible(false);
        errorLbl.setManaged(false);

        formBody.getChildren().addAll(ingredientDropdown, qtyUnitRow, reorderField, errorLbl);

        // ── Footer ────────────────────────────────────────
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(18, 28, 24, 28));

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
        Label saveBtn = new Label("Add Ingredient");
        saveBtn.setGraphic(saveIcon);
        saveBtn.setGraphicTextGap(7);
        saveBtn.setCursor(javafx.scene.Cursor.HAND);
        saveBtn.setPrefWidth(150); saveBtn.setPrefHeight(38);
        saveBtn.setAlignment(Pos.CENTER);
        saveBtn.setStyle(addSaveBtnStyle(false));
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle(addSaveBtnStyle(true)));
        saveBtn.setOnMouseExited(e  -> saveBtn.setStyle(addSaveBtnStyle(false)));

        saveBtn.setOnMouseClicked(e -> {
            String qtyVal     = quantityInput.getText().trim();
            String unitVal    = unitInput.getText().trim();
            String reorderVal = reorderInput.getText().trim();

            // Validate shared fields first
            if (qtyVal.isEmpty() || unitVal.isEmpty() || reorderVal.isEmpty()) {
                showError(errorLbl, "⚠  Quantity, unit, and reorder level are required.");
                return;
            }
            if (!inventory_util.isValidUnit(unitVal)) {
                showError(errorLbl, "⚠  Unit must be one of: ml, l, g, kg, pcs.");
                return;
            }
            if (finalAllIngredients.isEmpty()) {
                showError(errorLbl, "⚠  No ingredients found. Add ingredients in the Ingredients module first.");
                return;
            }

            Set<String> selectedIngredientIds = getIngredientDropdownSelected(ingredientDropdown);
            if (selectedIngredientIds.isEmpty()) {
                showError(errorLbl, "⚠  Please select at least one ingredient.");
                return;
            }

            int qty     = Integer.parseInt(qtyVal);
            int reorder = Integer.parseInt(reorderVal);
            String unitLower = unitVal.toLowerCase();

            // Insert one inventory row per selected ingredient
            boolean anyFailed = false;
            for (String ingId : selectedIngredientIds) {
                String newId = inventory_util.insertIngredientById(conn, ingId, qty, unitLower, reorder);
                if (newId == null) {
                    anyFailed = true;
                    System.err.println("[inventory_contents] insertIngredientById failed for ingredient_id=" + ingId);
                } else if (currentTab.equals("active")) {
                    String ingName = getIngredientNameById(ingredientDropdown, ingId);
                    prependItem(newId, ingName, String.valueOf(qty), unitLower, String.valueOf(reorder));
                }
            }

            if (anyFailed) {
                // Partial failure — stay open and warn, table already updated for successes
                showError(errorLbl, "⚠  One or more ingredients failed to save. Check connection.");
                return;
            }

            stackRoot.getChildren().remove(overlay);
            // Full reload to ensure DB order is respected
            cachedRows = inventory_util.fetchInventory(conn, currentTab);
            rebuildTable();
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

    private void showError(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
    }

    // ── Form field factory ────────────────────────────────
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
        double addW = 142;
        searchW  = 200;

        // ── Title ─────────────────────────────────────────
        Label title = new Label("Inventory");
        title.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 36px;" +
            "-fx-font-weight: 800;" +
            "-fx-text-fill: " + ACCENT + ";"
        );

        // ── Add Ingredient button ──────────────────────────
        FontIcon plusIcon = new FontIcon(FontAwesomeSolid.PLUS_CIRCLE);
        plusIcon.setIconSize(14);
        plusIcon.setIconColor(javafx.scene.paint.Color.web("#155724"));
        addIngredientBtn = new Label("Add Ingredient");
        addIngredientBtn.setGraphic(plusIcon);
        addIngredientBtn.setGraphicTextGap(6);
        addIngredientBtn.setCursor(javafx.scene.Cursor.HAND);
        addIngredientBtn.setStyle(addIngredientBtnStyle(false));
        addIngredientBtn.setPrefHeight(btnH); addIngredientBtn.setPrefWidth(addW);
        addIngredientBtn.setAlignment(Pos.CENTER);
        addIngredientBtn.setOnMouseEntered(e -> addIngredientBtn.setStyle(addIngredientBtnStyle(true)));
        addIngredientBtn.setOnMouseExited(e  -> addIngredientBtn.setStyle(addIngredientBtnStyle(false)));
        addIngredientBtn.setOnMouseClicked(e -> openAddIngredientModal());

        // ── Edit button ───────────────────────────────────
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

        // ── Archive icon button ───────────────────────────
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

        HBox titleRow = new HBox(gap, title, addIngredientBtn, editBtn, archiveBtn);
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
                "Inventory (" + currentTab + ")",
                "This will permanently remove all inventory items in this view.\nThis action cannot be undone.",
                () -> {
                    inventory_util.hardDeleteAll(conn, currentTab);
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
        exportCsvBtn.setLayoutX(exportCsvX); exportCsvBtn.setLayoutY(btnY);
        exportCsvBtn.setPrefHeight(btnH); exportCsvBtn.setPrefWidth(csvW);
        exportCsvBtn.setAlignment(Pos.CENTER);
        exportCsvBtn.setOnMouseEntered(e -> exportCsvBtn.setStyle(exportCsvBtnStyle(true)));
        exportCsvBtn.setOnMouseExited(e  -> exportCsvBtn.setStyle(exportCsvBtnStyle(false)));
        exportCsvBtn.setOnMouseClicked(e -> {
            Stage stage = null;
            try { stage = (Stage) root.getScene().getWindow(); } catch (Exception ignored) {}
            inventory_util.exportCsv(cachedRows, currentTab, stage);
        });

        // ── Active / Archived tab buttons ─────────────────
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

        // ── Archive All / Restore All button ─────────────
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
            if (currentTab.equals("active")) inventory_util.archiveSelected(conn, selectedIds);
            else                             inventory_util.restoreSelected(conn, selectedIds);
            selectedIds.clear(); archiveMode = false;
            updateArchiveBtnIcon();
            archiveAllBtn.setVisible(false); confirmBtn.setVisible(false);
            archiveBtn.setStyle(archiveBtnStyle(false));
            repositionSearchBar();
            cachedRows = inventory_util.fetchInventory(conn, currentTab);
            rebuildTable();
        });

        // ── Search bar ────────────────────────────────────
        FontIcon searchIcon = new FontIcon(FontAwesomeSolid.SEARCH);
        searchIcon.setIconSize(14);
        searchIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        searchField = new TextField();
        searchField.setPromptText("Search ingredient or ID...");
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

        // ── Table ─────────────────────────────────────────
        double tableY = TOP_PADDING + HEADER_H + 10;
        double tableW = totalW - SIDE_PADDING * 2;
        double tableH = totalH - tableY - SIDE_PADDING;

        cachedRows  = inventory_util.fetchInventory(conn, "active");
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
        cachedRows = inventory_util.fetchInventory(conn, tab);
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
        List<String[]> filtered = inventory_util.getFilteredRows(cachedRows, searchQuery);
        VBox tableBox = buildTable(tableW, filtered);
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
            String msg = (searchQuery != null && !searchQuery.isBlank())
                ? "No results found for \"" + searchQuery + "\"."
                : currentTab.equals("archived") ? "No archived inventory items." : "No inventory items found.";
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
            buildHeaderCell("Inventory ID",  dataW * COL_INV_ID),    buildColDivider(),
            buildHeaderCell("Ingredient",    dataW * COL_INGREDIENT), buildColDivider(),
            buildHeaderCell("Quantity",      dataW * COL_QUANTITY),   buildColDivider(),
            buildHeaderCell("Unit",          dataW * COL_UNIT),       buildColDivider(),
            buildHeaderCell("Reorder Level", dataW * COL_REORDER)
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

    private HBox buildDataRow(String inventoryId, String ingredient,
                               String quantity, String unit, String reorderLevel,
                               String bg, double tableW, double dataW, boolean isLast) {
        HBox row = new HBox(0);
        row.setPrefHeight(ROW_H); row.setAlignment(Pos.CENTER_LEFT);
        String  bottomRadius = isLast ? "0 0 10 10" : "0";
        String  borderBottom = isLast ? "0" : "1";
        boolean selected     = selectedIds.contains(inventoryId);
        row.setStyle(rowStyle(selected ? "#FDE8EA" : bg, bottomRadius, borderBottom));
        row.setOnMouseEntered(e -> {
            if (!selectedIds.contains(inventoryId))
                row.setStyle(rowStyle("#F5E8EA", bottomRadius, borderBottom));
        });
        row.setOnMouseExited(e ->
            row.setStyle(rowStyle(selectedIds.contains(inventoryId) ? "#FDE8EA" : bg, bottomRadius, borderBottom))
        );

        javafx.scene.Node qtyCell;
        javafx.scene.Node reorderCell;

        if (editMode && currentTab.equals("active")) {
            qtyCell     = buildEditableIntCell(inventoryId, quantity, reorderLevel, true,  dataW * COL_QUANTITY);
            reorderCell = buildEditableIntCell(inventoryId, quantity, reorderLevel, false, dataW * COL_REORDER);
        } else {
            qtyCell     = buildTextCell(quantity,     dataW * COL_QUANTITY, false);
            reorderCell = buildTextCell(reorderLevel, dataW * COL_REORDER,  false);
        }

        row.getChildren().addAll(
            buildTextCell(inventoryId, dataW * COL_INV_ID,    true),  buildColDivider(),
            buildTextCell(ingredient,  dataW * COL_INGREDIENT, false), buildColDivider(),
            qtyCell,                                                   buildColDivider(),
            buildTextCell(unit,        dataW * COL_UNIT,       false), buildColDivider(),
            reorderCell
        );

        if (archiveMode) {
            row.getChildren().add(buildColDivider());
            CheckBox cb = new CheckBox();
            cb.setSelected(selected); cb.setStyle("-fx-cursor: hand;");
            cb.setOnAction(e -> {
                if (cb.isSelected()) {
                    selectedIds.add(inventoryId);
                    row.setStyle(rowStyle("#FDE8EA", bottomRadius, borderBottom));
                } else {
                    selectedIds.remove(inventoryId);
                    row.setStyle(rowStyle(bg, bottomRadius, borderBottom));
                }
            });
            HBox cbCell = new HBox(cb);
            cbCell.setPrefWidth(CHECKBOX_COL); cbCell.setPrefHeight(ROW_H);
            cbCell.setAlignment(Pos.CENTER);
            row.getChildren().add(cbCell);
        }
        return row;
    }

    // ══════════════════════════════════════════════════════
    //  EDITABLE INT CELL
    // ══════════════════════════════════════════════════════
    private HBox buildEditableIntCell(String inventoryId, String currentQty,
                                       String currentReorder, boolean isQty, double width) {
        String initialValue = isQty ? currentQty : currentReorder;
        TextField field = new TextField(initialValue);
        field.setPrefWidth(width - 24);
        field.setPrefHeight(ROW_H - 12);

        field.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) field.setText(n.replaceAll("[^\\d]", ""));
        });

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
            String raw = field.getText().trim();
            if (raw.isEmpty()) return;
            try {
                int newVal = Integer.parseInt(raw);
                for (String[] r : cachedRows) {
                    if (r[0].equals(inventoryId)) {
                        if (isQty) r[2] = String.valueOf(newVal);
                        else       r[4] = String.valueOf(newVal);

                        int qty     = inventory_util.parseIntSafe(r[2]);
                        int reorder = inventory_util.parseIntSafe(r[4]);
                        inventory_util.updateIngredient(conn, inventoryId, qty, reorder);
                        break;
                    }
                }
            } catch (NumberFormatException ignored) { }
        };

        field.setOnAction(e -> save.run());
        field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) save.run();
        });

        HBox cell = new HBox(field);
        cell.setPrefWidth(width); cell.setPrefHeight(ROW_H);
        cell.setPadding(new Insets(0, 6, 0, 10));
        cell.setAlignment(Pos.CENTER_LEFT);
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

    private Label buildTextCell(String text, double width, boolean bold) {
        Label lbl = new Label(text != null ? text : "—");
        lbl.setPrefWidth(width); lbl.setPrefHeight(ROW_H);
        lbl.setPadding(new Insets(0, 0, 0, 16));
        lbl.setAlignment(Pos.CENTER_LEFT);
        lbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;" +
            "-fx-font-weight: " + (bold ? "bold" : "normal") + ";-fx-text-fill: #333333;"
        );
        return lbl;
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

    private String addIngredientBtnStyle(boolean hovered) {
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