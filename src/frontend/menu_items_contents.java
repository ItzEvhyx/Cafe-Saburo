package frontend;

import backend.inventory_util;
import backend.menu_items_util;

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
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class menu_items_contents {

    // ══════════════════════════════════════════════════════
    //  LAYOUT CONSTANTS
    // ══════════════════════════════════════════════════════
    private static final double TOP_PADDING  = 20;
    private static final double SIDE_PADDING = 24;
    private static final double HEADER_H     = 56;

    // Columns: Item ID | Item Name | Size | Price
    private static final double COL_ITEM_ID   = 0.12;
    private static final double COL_ITEM_NAME = 0.38;
    private static final double COL_SIZE      = 0.28;
    private static final double COL_PRICE     = 0.22;

    private static final double ROW_H        = 44;
    private static final double HEADER_ROW_H = 46;
    private static final double CHECKBOX_COL = 48;

    // ── Modal dimensions ──────────────────────────────────
    private static final double MODAL_W     = 440;
    private static final double MODAL_H     = 260;
    private static final double ADD_MODAL_W = 500;
    private static final double ADD_MODAL_H = 480;

    // ── Category options ──────────────────────────────────
    private static final List<String> CATEGORIES = Arrays.asList(
        "Espresso", "Specialty Coffee", "Iced Coffee",
        "Frappe", "Matcha Series", "Smoothie", "Refresher", "Add On"
    );

    // ══════════════════════════════════════════════════════
    //  STYLE CONSTANTS
    // ══════════════════════════════════════════════════════
    private static final String ACCENT            = "#882F39";
    private static final String FONT_FAMILY       = "Aleo";
    private static final String TABLE_BORDER      = "#882F39";
    private static final String ROW_ALT_BG        = "#FDF5F6";
    private static final String ROW_WHITE_BG      = "white";
    private static final String HEADER_BG         = "#F5E8EA";
    private static final String CONTINUATION_TEXT = "#888888";

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
    private Label     addMenuItemBtn;
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

    public menu_items_contents(double totalW, double totalH, Connection conn) {
        this.totalW = totalW;
        this.totalH = totalH;
        this.conn   = conn;
        inventory_util.loadFonts();
    }

    // ══════════════════════════════════════════════════════
    //  PUBLIC LIVE-UPDATE API
    // ══════════════════════════════════════════════════════
    public void prependItem(String itemId, String itemName, String sizes, String price) {
        // For a new "Small, Large" item, prepend two atomic rows; else one row
        if (root != null && currentTab.equals("active")) {
            String sizesNorm = sizes != null ? sizes.toLowerCase().trim() : "";
            if (sizesNorm.equals("small, large") && price != null && price.contains("/")) {
                String[] parts = price.split("/");
                String priceSmall = parts[0].trim();
                String priceLarge = parts.length > 1 ? parts[1].trim() : parts[0].trim();
                cachedRows.add(0, new String[]{ itemId, itemName, "Large",  priceLarge, sizes });
                cachedRows.add(0, new String[]{ itemId, itemName, "Small",  priceSmall, sizes });
            } else {
                String sizeLabel = (sizes != null && !sizes.isBlank()) ? sizes : "One Size";
                cachedRows.add(0, new String[]{ itemId, itemName, sizeLabel, price != null ? price : "0", sizes });
            }
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
    //  CUSTOM DROPDOWN
    // ══════════════════════════════════════════════════════
    private VBox buildDropdownField(FontAwesomeSolid iconCode, String label) {
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

        Label valueLabel = new Label("Select...");
        valueLabel.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #AAAAAA;"
        );
        HBox.setHgrow(valueLabel, Priority.ALWAYS);
        valueLabel.setMaxWidth(Double.MAX_VALUE);

        FontIcon arrowIcon = new FontIcon(FontAwesomeSolid.CHEVRON_DOWN);
        arrowIcon.setIconSize(11);
        arrowIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        HBox trigger = new HBox(10, fi, valueLabel, arrowIcon);
        trigger.setAlignment(Pos.CENTER_LEFT);
        trigger.setPadding(new Insets(0, 14, 0, 14));
        trigger.setPrefHeight(40);
        trigger.setMaxWidth(Double.MAX_VALUE);
        trigger.setCursor(javafx.scene.Cursor.HAND);
        trigger.setStyle(dropdownTriggerStyle(false));
        trigger.setUserData(new String[]{ null });

        trigger.setOnMouseEntered(e -> {
            if (!Boolean.TRUE.equals(trigger.getProperties().get("open")))
                trigger.setStyle(dropdownTriggerStyle(true));
        });
        trigger.setOnMouseExited(e -> {
            if (!Boolean.TRUE.equals(trigger.getProperties().get("open")))
                trigger.setStyle(dropdownTriggerStyle(false));
        });

        trigger.getProperties().put("items",      new ArrayList<String>());
        trigger.getProperties().put("valueLabel", valueLabel);
        trigger.getProperties().put("arrowIcon",  arrowIcon);
        trigger.getProperties().put("open",       false);

        trigger.setOnMouseClicked(e -> openCustomDropdown(trigger));

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

    @SuppressWarnings("unchecked")
    private void setDropdownItems(VBox fieldBox, List<String> items, String promptText) {
        HBox trigger = (HBox) fieldBox.getChildren().get(1);
        ((List<String>) trigger.getProperties().get("items")).clear();
        ((List<String>) trigger.getProperties().get("items")).addAll(items);
        Label vl = (Label) trigger.getProperties().get("valueLabel");
        vl.setText(promptText);
        vl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #AAAAAA;"
        );
        ((String[]) trigger.getUserData())[0] = null;
    }

    private String getDropdownValue(VBox fieldBox) {
        HBox trigger = (HBox) fieldBox.getChildren().get(1);
        return ((String[]) trigger.getUserData())[0];
    }

    @SuppressWarnings("unchecked")
    private void openCustomDropdown(HBox trigger) {
        List<String> items = (List<String>) trigger.getProperties().get("items");
        if (items == null || items.isEmpty()) return;

        VBox listBox = new VBox(0);
        listBox.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: " + ACCENT + ";" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 12, 0, 0, 4);"
        );
        listBox.setMaxHeight(200);

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

        String currentVal = ((String[]) trigger.getUserData())[0];

        for (int i = 0; i < items.size(); i++) {
            String item   = items.get(i);
            boolean isSel = item.equals(currentVal);
            boolean isLast = i == items.size() - 1;

            Label row = new Label(item);
            row.setMaxWidth(Double.MAX_VALUE);
            row.setPrefWidth(trigW);
            row.setPadding(new Insets(10, 14, 10, 14));
            row.setCursor(javafx.scene.Cursor.HAND);
            row.setWrapText(false);
            row.setStyle(dropdownRowStyle(isSel, false, isLast));

            row.setOnMouseEntered(e -> row.setStyle(dropdownRowStyle(isSel, true, isLast)));
            row.setOnMouseExited(e  -> row.setStyle(dropdownRowStyle(isSel, false, isLast)));
            row.setOnMouseClicked(e -> {
                ((String[]) trigger.getUserData())[0] = item;
                Label vl = (Label) trigger.getProperties().get("valueLabel");
                vl.setText(item);
                vl.setStyle(
                    "-fx-font-family: '" + FONT_FAMILY + "';" +
                    "-fx-font-size: 13px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: " + ACCENT + ";"
                );
                popup.hide();
                trigger.getProperties().put("open", false);
                trigger.setStyle(dropdownTriggerStyle(false));
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

    private String dropdownRowStyle(boolean selected, boolean hovered, boolean isLast) {
        String bg     = selected ? "#F5E8EA" : hovered ? ACCENT : "white";
        String fg     = selected ? ACCENT    : hovered ? "white" : "#333333";
        String fw     = selected ? "bold" : "normal";
        String radius = isLast ? "0 0 9 9" : "0";
        return "-fx-background-color: " + bg + ";" +
               "-fx-background-radius: " + radius + ";" +
               "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 13px;" +
               "-fx-font-weight: " + fw + ";" +
               "-fx-text-fill: " + fg + ";";
    }

    // ══════════════════════════════════════════════════════
    //  ADD MENU ITEM MODAL
    // ══════════════════════════════════════════════════════
    private void openAddMenuItemModal() {
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

        // ── Card header ───────────────────────────────────
        HBox cardHeader = new HBox(10);
        cardHeader.setPadding(new Insets(20, 24, 16, 24));
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        cardHeader.setStyle(
            "-fx-background-color: " + HEADER_BG + ";" +
            "-fx-background-radius: 14 14 0 0;" +
            "-fx-border-color: transparent transparent " + TABLE_BORDER + " transparent;" +
            "-fx-border-width: 0 0 1.5 0;"
        );

        FontIcon coffeeIcon = new FontIcon(FontAwesomeSolid.COFFEE);
        coffeeIcon.setIconSize(17);
        coffeeIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        Label modalTitle = new Label("Add Menu Item");
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
        cardHeader.getChildren().addAll(coffeeIcon, modalTitle, hSpacer, closeBtn);

        // ── Form body ─────────────────────────────────────
        VBox formBody = new VBox(16);
        formBody.setPadding(new Insets(22, 28, 10, 28));
        VBox.setVgrow(formBody, Priority.ALWAYS);

        VBox nameField  = buildFormField(FontAwesomeSolid.TAG,        "Item Name", "e.g. Caramel Latte");
        VBox priceField = buildFormField(FontAwesomeSolid.MONEY_BILL, "Price (₱)", "e.g. 150  or  100 / 120 for Small/Large");

        TextField nameInput  = extractTextField(nameField);
        TextField priceInput = extractTextField(priceField);

        // ── Category dropdown ─────────────────────────────
        VBox categoryField = buildDropdownField(FontAwesomeSolid.LIST, "Category");
        setDropdownItems(categoryField, CATEGORIES, "Select a category...");

        // ── Sizes selection ───────────────────────────────
        Label sizesLabel = new Label("Sizes");
        sizesLabel.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #555555;"
        );

        CheckBox smallCb = new CheckBox("Small");
        CheckBox largeCb = new CheckBox("Large");
        smallCb.setStyle("-fx-font-family: '" + FONT_FAMILY + "'; -fx-font-size: 13px; -fx-text-fill: #333333; -fx-cursor: hand;");
        largeCb.setStyle("-fx-font-family: '" + FONT_FAMILY + "'; -fx-font-size: 13px; -fx-text-fill: #333333; -fx-cursor: hand;");

        CheckBox oneSize = new CheckBox("One Size (no size variants)");
        oneSize.setStyle("-fx-font-family: '" + FONT_FAMILY + "'; -fx-font-size: 13px; -fx-text-fill: #333333; -fx-cursor: hand;");

        oneSize.setOnAction(e -> {
            boolean one = oneSize.isSelected();
            smallCb.setDisable(one);
            largeCb.setDisable(one);
            if (one) { smallCb.setSelected(false); largeCb.setSelected(false); }
        });
        smallCb.setOnAction(e -> {
            boolean either = smallCb.isSelected() || largeCb.isSelected();
            oneSize.setSelected(false);
            oneSize.setDisable(either);
        });
        largeCb.setOnAction(e -> {
            boolean either = smallCb.isSelected() || largeCb.isSelected();
            oneSize.setSelected(false);
            oneSize.setDisable(either);
        });

        HBox sizeCheckboxes = new HBox(16, smallCb, largeCb, oneSize);
        sizeCheckboxes.setAlignment(Pos.CENTER_LEFT);
        sizeCheckboxes.setPadding(new Insets(0, 12, 0, 12));
        sizeCheckboxes.setPrefHeight(40);
        sizeCheckboxes.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: " + ACCENT + ";" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 10;"
        );

        VBox sizesField = new VBox(6, sizesLabel, sizeCheckboxes);

        Label errorLbl = new Label("");
        errorLbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #882F39;"
        );
        errorLbl.setVisible(false);
        errorLbl.setManaged(false);

        formBody.getChildren().addAll(nameField, categoryField, sizesField, priceField, errorLbl);

        // ── Footer buttons ────────────────────────────────
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
        Label saveBtn = new Label("Add Menu Item");
        saveBtn.setGraphic(saveIcon);
        saveBtn.setGraphicTextGap(7);
        saveBtn.setCursor(javafx.scene.Cursor.HAND);
        saveBtn.setPrefWidth(150); saveBtn.setPrefHeight(38);
        saveBtn.setAlignment(Pos.CENTER);
        saveBtn.setStyle(addSaveBtnStyle(false));
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle(addSaveBtnStyle(true)));
        saveBtn.setOnMouseExited(e  -> saveBtn.setStyle(addSaveBtnStyle(false)));
        saveBtn.setOnMouseClicked(e -> {
            String nameVal     = nameInput.getText().trim();
            String categoryVal = getDropdownValue(categoryField);
            String priceVal    = priceInput.getText().trim().replace("₱", "").trim();

            String sizesVal;
            if (oneSize.isSelected()) {
                sizesVal = "One Size";
            } else {
                List<String> chosen = new ArrayList<>();
                if (smallCb.isSelected()) chosen.add("Small");
                if (largeCb.isSelected()) chosen.add("Large");
                sizesVal = String.join(", ", chosen);
            }

            if (nameVal.isEmpty() || categoryVal == null || priceVal.isEmpty() || sizesVal.isEmpty()) {
                errorLbl.setText("⚠  All fields are required.");
                errorLbl.setVisible(true);
                errorLbl.setManaged(true);
                return;
            }

            Double priceSmall = null, priceLarge = null, price = null;
            try {
                if (sizesVal.equals("Small, Large")) {
                    if (priceVal.contains("/")) {
                        String[] parts = priceVal.split("/");
                        priceSmall = Double.parseDouble(parts[0].trim());
                        priceLarge = Double.parseDouble(parts[1].trim());
                    } else {
                        priceSmall = Double.parseDouble(priceVal);
                        priceLarge = priceSmall;
                    }
                } else {
                    price = Double.parseDouble(priceVal);
                }
            } catch (NumberFormatException ex) {
                errorLbl.setText("⚠  Enter a valid price (numbers only).");
                errorLbl.setVisible(true);
                errorLbl.setManaged(true);
                return;
            }

            String newId = menu_items_util.insertMenuItem(conn, nameVal, categoryVal, sizesVal, priceSmall, priceLarge, price);
            if (newId == null) {
                errorLbl.setText("⚠  Failed to save. Check connection.");
                errorLbl.setVisible(true);
                errorLbl.setManaged(true);
                return;
            }

            stackRoot.getChildren().remove(overlay);
            if (currentTab.equals("active")) {
                String displayPrice = (sizesVal.equals("Small, Large"))
                    ? (int) Math.round(priceSmall) + " / " + (int) Math.round(priceLarge)
                    : String.valueOf((int) Math.round(price));
                prependItem(newId, nameVal, sizesVal, displayPrice);
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
        menu_items_util.runStartupDiagnostic(conn);

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
        double addW = 150;
        searchW  = 200;

        Label title = new Label("Menu Items");
        title.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 36px;" +
            "-fx-font-weight: 800;" +
            "-fx-text-fill: " + ACCENT + ";"
        );

        // ── Add Menu Item button ──────────────────────────
        FontIcon plusIcon = new FontIcon(FontAwesomeSolid.PLUS_CIRCLE);
        plusIcon.setIconSize(14);
        plusIcon.setIconColor(javafx.scene.paint.Color.web("#155724"));
        addMenuItemBtn = new Label("Add Menu Item");
        addMenuItemBtn.setGraphic(plusIcon);
        addMenuItemBtn.setGraphicTextGap(6);
        addMenuItemBtn.setCursor(javafx.scene.Cursor.HAND);
        addMenuItemBtn.setStyle(addMenuItemBtnStyle(false));
        addMenuItemBtn.setPrefHeight(btnH); addMenuItemBtn.setPrefWidth(addW);
        addMenuItemBtn.setAlignment(Pos.CENTER);
        addMenuItemBtn.setOnMouseEntered(e -> addMenuItemBtn.setStyle(addMenuItemBtnStyle(true)));
        addMenuItemBtn.setOnMouseExited(e  -> addMenuItemBtn.setStyle(addMenuItemBtnStyle(false)));
        addMenuItemBtn.setOnMouseClicked(e -> openAddMenuItemModal());

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

        // ── Archive button ────────────────────────────────
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

        HBox titleRow = new HBox(gap, title, addMenuItemBtn, editBtn, archiveBtn);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.setLayoutX(SIDE_PADDING); titleRow.setLayoutY(TOP_PADDING);
        titleRow.setPrefHeight(HEADER_H);

        // ── Right-side button layout ──────────────────────
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
                "Menu Items (" + currentTab + ")",
                "This will permanently remove all menu items in this view.\nThis action cannot be undone.",
                () -> {
                    menu_items_util.hardDeleteAll(conn, currentTab);
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
            menu_items_util.exportCsv(cachedRows, currentTab, stage);
        });

        // ── Tab buttons ───────────────────────────────────
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
            // Select by unique item_id only (not per-size row)
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
            if (currentTab.equals("active")) menu_items_util.archiveSelected(conn, selectedIds);
            else                             menu_items_util.restoreSelected(conn, selectedIds);
            selectedIds.clear(); archiveMode = false;
            updateArchiveBtnIcon();
            archiveAllBtn.setVisible(false); confirmBtn.setVisible(false);
            archiveBtn.setStyle(archiveBtnStyle(false));
            repositionSearchBar();
            cachedRows = menu_items_util.fetchMenuItems(conn, currentTab);
            rebuildTable();
        });

        // ── Search bar ────────────────────────────────────
        FontIcon searchIcon = new FontIcon(FontAwesomeSolid.SEARCH);
        searchIcon.setIconSize(14);
        searchIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        searchField = new TextField();
        searchField.setPromptText("Search item name...");
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

        cachedRows  = menu_items_util.fetchMenuItems(conn, "active");
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
        cachedRows = menu_items_util.fetchMenuItems(conn, tab);
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
        List<String[]> filtered = menu_items_util.getFilteredRows(cachedRows, searchQuery);
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
    //  Mirrors suppliers_contents grouping logic:
    //    - one row per size/price (3NF)
    //    - Item ID and Item Name shown only on the first row of each item group
    //    - alternating background per item group (not per size-row)
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
                : currentTab.equals("archived") ? "No archived menu items." : "No menu items found.";
            Label empty = new Label(msg);
            empty.setStyle(
                "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 14px;" +
                "-fx-text-fill: #AAAAAA;-fx-padding: 24 0 24 16;"
            );
            table.getChildren().add(empty);
        } else {
            String  prevItemId  = null;
            boolean isAlt       = false;

            for (int i = 0; i < rows.size(); i++) {
                String[] item      = rows.get(i);
                String   itemId    = item[0];
                boolean  isLast    = (i == rows.size() - 1);

                // Flip background color at each new item group
                boolean isFirstInGroup = !itemId.equals(prevItemId);
                if (isFirstInGroup) {
                    if (prevItemId != null) isAlt = !isAlt;
                    prevItemId = itemId;
                }

                String bg = isAlt ? ROW_ALT_BG : ROW_WHITE_BG;
                table.getChildren().add(buildDataRow(item, isFirstInGroup, bg, tableW, dataW, isLast));
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
            buildHeaderCell("Item ID",   dataW * COL_ITEM_ID),   buildColDivider(),
            buildHeaderCell("Item Name", dataW * COL_ITEM_NAME), buildColDivider(),
            buildHeaderCell("Size",      dataW * COL_SIZE),      buildColDivider(),
            buildHeaderCell("Price",     dataW * COL_PRICE)
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

    // ══════════════════════════════════════════════════════
    //  DATA ROW BUILDER
    //
    //  row: [0]=item_id, [1]=item_name, [2]=size_label, [3]=price, [4]=sizes_raw
    //
    //  isFirstInGroup → show item_id and item_name; otherwise show empty cells
    //  (mirrors suppliers_contents behaviour for supplier_id / supplier_name)
    // ══════════════════════════════════════════════════════
    private HBox buildDataRow(String[] item,
                               boolean isFirstInGroup,
                               String bg,
                               double tableW,
                               double dataW,
                               boolean isLast) {
        String itemId    = item[0];
        String itemName  = item[1];
        String sizeLabel = item[2];
        String price     = item[3];

        HBox row = new HBox(0);
        row.setAlignment(Pos.TOP_LEFT);
        String  bottomRadius = isLast ? "0 0 10 10" : "0";
        String  borderBottom = isLast ? "0" : "1";
        boolean selected     = selectedIds.contains(itemId);
        row.setStyle(rowStyle(selected ? "#FDE8EA" : bg, bottomRadius, borderBottom));
        row.setOnMouseEntered(e -> {
            if (!selectedIds.contains(itemId))
                row.setStyle(rowStyle("#F5E8EA", bottomRadius, borderBottom));
        });
        row.setOnMouseExited(e ->
            row.setStyle(rowStyle(selectedIds.contains(itemId) ? "#FDE8EA" : bg, bottomRadius, borderBottom))
        );

        // Item ID — shown only on first row of group
        javafx.scene.Node idCell = buildTextCell(
            isFirstInGroup ? itemId : "", dataW * COL_ITEM_ID, true, false);

        // Item Name — shown only on first row; editable in edit mode
        javafx.scene.Node nameCell;
        if (editMode && currentTab.equals("active") && isFirstInGroup) {
            nameCell = buildEditableTextCell(item, 1, dataW * COL_ITEM_NAME);
        } else {
            nameCell = buildTextCell(
                isFirstInGroup ? itemName : "",
                dataW * COL_ITEM_NAME, false, !isFirstInGroup);
        }

        // Size — size badge chip (atomic value per row)
        HBox sizeCell = buildSizeChipCell(sizeLabel, dataW * COL_SIZE);

        // Price — editable in edit mode; shown with ₱ prefix
        javafx.scene.Node priceCell;
        if (editMode && currentTab.equals("active")) {
            priceCell = buildEditableTextCell(item, 3, dataW * COL_PRICE);
        } else {
            String priceDisplay = (price != null && !price.isEmpty()) ? "₱" + price : "—";
            priceCell = buildTextCell(priceDisplay, dataW * COL_PRICE, false, false);
        }

        row.getChildren().addAll(
            idCell,     buildColDivider(),
            nameCell,   buildColDivider(),
            sizeCell,   buildColDivider(),
            priceCell
        );

        if (archiveMode) {
            row.getChildren().add(buildColDivider());
            if (isFirstInGroup) {
                CheckBox cb = new CheckBox();
                cb.setSelected(selected); cb.setStyle("-fx-cursor: hand;");
                cb.setOnAction(e -> {
                    if (cb.isSelected()) {
                        selectedIds.add(itemId);
                        row.setStyle(rowStyle("#FDE8EA", bottomRadius, borderBottom));
                    } else {
                        selectedIds.remove(itemId);
                        row.setStyle(rowStyle(bg, bottomRadius, borderBottom));
                    }
                    rebuildTable();
                });
                HBox cbCell = new HBox(cb);
                cbCell.setPrefWidth(CHECKBOX_COL);
                cbCell.setMinHeight(ROW_H);
                cbCell.setPadding(new Insets(12, 0, 12, 0));
                cbCell.setAlignment(Pos.TOP_CENTER);
                row.getChildren().add(cbCell);
            } else {
                // Continuation rows get an empty spacer in the checkbox column
                Region spacer = new Region();
                spacer.setPrefWidth(CHECKBOX_COL);
                spacer.setMinHeight(ROW_H);
                row.getChildren().add(spacer);
            }
        }
        return row;
    }

    // ── Size badge chip cell ──────────────────────────────
    private HBox buildSizeChipCell(String sizeLabel, double width) {
        HBox cell = new HBox(6);
        cell.setPrefWidth(width);
        cell.setMinHeight(ROW_H);
        cell.setPadding(new Insets(10, 8, 10, 16));
        cell.setAlignment(Pos.CENTER_LEFT);

        if (sizeLabel == null || sizeLabel.isBlank() || sizeLabel.equals("--")) {
            Label dash = new Label("—");
            dash.setStyle("-fx-font-family: '" + FONT_FAMILY + "'; -fx-font-size: 13px; -fx-text-fill: #AAAAAA;");
            cell.getChildren().add(dash);
            return cell;
        }

        Label chip = new Label(sizeLabel);
        boolean isSmall   = sizeLabel.equalsIgnoreCase("Small");
        boolean isLarge   = sizeLabel.equalsIgnoreCase("Large");
        boolean isOneSize = sizeLabel.equalsIgnoreCase("One Size");

        String chipBg = isSmall   ? "#E3F2FD"
                      : isLarge   ? "#F3E5F5"
                      : isOneSize ? "#E8F5E9"
                      :             "#F5E8EA";
        String chipFg = isSmall   ? "#1565C0"
                      : isLarge   ? "#6A1B9A"
                      : isOneSize ? "#2E7D32"
                      :             ACCENT;

        chip.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + chipFg + ";" +
            "-fx-background-color: " + chipBg + ";" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 2 8 2 8;"
        );
        cell.getChildren().add(chip);
        return cell;
    }

    // ══════════════════════════════════════════════════════
    //  EDITABLE TEXT CELL
    //  row: [0]=item_id, [1]=item_name, [2]=size_label, [3]=price, [4]=sizes_raw
    //  colIndex: 1 = item_name, 3 = price
    // ══════════════════════════════════════════════════════
    private HBox buildEditableTextCell(String[] item, int colIndex, double width) {
        String initialValue;
        switch (colIndex) {
            case 1:  initialValue = item[1] != null ? item[1] : ""; break;
            case 3:  initialValue = item[3] != null ? item[3] : ""; break;
            default: initialValue = ""; break;
        }

        String promptText = (colIndex == 3)
            ? "e.g. 150"
            : "";

        TextField field = new TextField(initialValue);
        field.setPromptText(promptText);
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
            String newVal = field.getText().trim().replace("₱", "").trim();
            if (newVal.isEmpty()) return;
            // Update the cached row
            item[colIndex] = newVal;
            // Persist to DB — pass allCachedRows so sibling prices can be resolved
            menu_items_util.updateMenuItemFromRow(conn, item, cachedRows);
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

    private HBox buildTextCell(String text, double width, boolean bold, boolean muted) {
        Label lbl = new Label(text != null ? text : "");
        lbl.setPrefWidth(width - 16);
        lbl.setMaxWidth(width - 16);
        lbl.setWrapText(true);
        lbl.setPadding(new Insets(10, 8, 10, 0));
        lbl.setAlignment(Pos.TOP_LEFT);
        String colour = muted ? CONTINUATION_TEXT : "#333333";
        lbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;" +
            "-fx-font-weight: " + (bold ? "bold" : "normal") + ";" +
            "-fx-text-fill: " + colour + ";"
        );
        HBox cell = new HBox(lbl);
        cell.setPrefWidth(width);
        cell.setMinHeight(ROW_H);
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

    private String addMenuItemBtnStyle(boolean hovered) {
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