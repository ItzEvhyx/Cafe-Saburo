package frontend;

import backend.promotions_util;

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
import javafx.stage.Popup;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class promotions_contents {

    // ══════════════════════════════════════════════════════
    //  LAYOUT CONSTANTS
    // ══════════════════════════════════════════════════════
    private static final double TOP_PADDING  = 20;
    private static final double SIDE_PADDING = 24;
    private static final double HEADER_H     = 56;

    // Column proportions — must sum to 1.0
    // promo_id | promo_name | discount_type | start_date | end_date
    private static final double COL_PROMO_ID      = 0.12;
    private static final double COL_PROMO_NAME    = 0.28;
    private static final double COL_DISCOUNT_TYPE = 0.22;
    private static final double COL_START_DATE    = 0.19;
    private static final double COL_END_DATE      = 0.19;

    private static final double ROW_H        = 44;
    private static final double HEADER_ROW_H = 46;
    private static final double CHECKBOX_COL = 48;

    // ── Modal dimensions ──────────────────────────────────
    private static final double MODAL_W     = 440;
    private static final double MODAL_H     = 260;
    private static final double ADD_MODAL_W = 520;
    private static final double ADD_MODAL_H = 580;

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
    private final double           totalW;
    private final double           totalH;
    private final promotions_util  util;

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
    private Label     addPromoBtn;
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

    public promotions_contents(double totalW, double totalH, Connection conn) {
        this.totalW = totalW;
        this.totalH = totalH;
        this.util   = new promotions_util(conn);
        loadFonts();
        cachedRows = util.fetchPromotions(currentTab);
    }

    // ══════════════════════════════════════════════════════
    //  PUBLIC LIVE-UPDATE API
    // ══════════════════════════════════════════════════════
    public void prependItem(String promoId, String promoName,
                             String discountType, String startDate, String endDate) {
        String[] newRow = new String[]{
            promoId      != null ? promoId      : "--",
            promoName    != null ? promoName    : "--",
            discountType != null ? discountType : "--",
            startDate    != null ? startDate    : "--",
            endDate      != null ? endDate      : "--"
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
        searchBar.setLayoutX(rightAnchor - gap - searchW);
    }

    // ══════════════════════════════════════════════════════
    //  CUSTOM DROPDOWN ENGINE
    // ══════════════════════════════════════════════════════

    private VBox buildDropdownField(FontAwesomeSolid iconCode, String label) {
        Label fieldLabel = buildFieldLabel(label);

        FontIcon fi = new FontIcon(iconCode);
        fi.setIconSize(13);
        fi.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        Label valueLabel = new Label("Select...");
        valueLabel.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;-fx-text-fill: #AAAAAA;");
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
        trigger.getProperties().put("disabled",   false);
        trigger.setOnMouseClicked(e -> openCustomDropdown(trigger));

        VBox wrapper = new VBox(6, fieldLabel, trigger);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        return wrapper;
    }

    private String dropdownTriggerStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#FDF0F1" : "white") + ";" +
               "-fx-background-radius: 10;-fx-border-color: " + ACCENT + ";" +
               "-fx-border-width: 1.5;-fx-border-radius: 10;";
    }

    private String dropdownTriggerDisabledStyle() {
        return "-fx-background-color: #F8F9FA;-fx-background-radius: 10;" +
               "-fx-border-color: #CCCCCC;-fx-border-width: 1.5;-fx-border-radius: 10;";
    }

    @SuppressWarnings("unchecked")
    private void setDropdownItems(VBox fieldBox, List<String> items, String promptText) {
        HBox trigger = (HBox) fieldBox.getChildren().get(1);
        ((List<String>) trigger.getProperties().get("items")).clear();
        ((List<String>) trigger.getProperties().get("items")).addAll(items);
        Label vl = (Label) trigger.getProperties().get("valueLabel");
        vl.setText(promptText);
        vl.setStyle("-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;-fx-text-fill: #AAAAAA;");
        ((String[]) trigger.getUserData())[0] = null;
    }

    private void setDropdownDisabled(VBox fieldBox, boolean disabled) {
        HBox trigger = (HBox) fieldBox.getChildren().get(1);
        trigger.getProperties().put("disabled", disabled);
        trigger.setCursor(disabled ? javafx.scene.Cursor.DEFAULT : javafx.scene.Cursor.HAND);
        Label    vl  = (Label)    trigger.getProperties().get("valueLabel");
        FontIcon arr = (FontIcon) trigger.getProperties().get("arrowIcon");
        if (disabled) {
            trigger.setStyle(dropdownTriggerDisabledStyle());
            vl.setStyle("-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;-fx-text-fill: #BBBBBB;");
            arr.setIconColor(javafx.scene.paint.Color.web("#BBBBBB"));
        } else {
            trigger.setStyle(dropdownTriggerStyle(false));
            vl.setStyle("-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;-fx-text-fill: #AAAAAA;");
            arr.setIconColor(javafx.scene.paint.Color.web(ACCENT));
        }
    }

    private String getDropdownValue(VBox fieldBox) {
        HBox trigger = (HBox) fieldBox.getChildren().get(1);
        return ((String[]) trigger.getUserData())[0];
    }

    private void resetDropdown(VBox fieldBox, String promptText) {
        HBox trigger = (HBox) fieldBox.getChildren().get(1);
        ((String[]) trigger.getUserData())[0] = null;
        Label vl = (Label) trigger.getProperties().get("valueLabel");
        vl.setText(promptText);
        vl.setStyle("-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;-fx-text-fill: #AAAAAA;");
    }

    private void setDropdownOnAction(VBox fieldBox, Runnable action) {
        HBox trigger = (HBox) fieldBox.getChildren().get(1);
        trigger.getProperties().put("onAction", action);
    }

    @SuppressWarnings("unchecked")
    private void openCustomDropdown(HBox trigger) {
        if (Boolean.TRUE.equals(trigger.getProperties().get("disabled"))) return;
        List<String> items = (List<String>) trigger.getProperties().get("items");
        if (items == null || items.isEmpty()) return;

        VBox listBox = new VBox(0);
        listBox.setStyle(
            "-fx-background-color: white;-fx-border-color: " + ACCENT + ";-fx-border-width: 1.5;" +
            "-fx-border-radius: 10;-fx-background-radius: 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 12, 0, 0, 4);");
        listBox.setMaxHeight(200);

        ScrollPane sp = new ScrollPane(listBox);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setStyle(
            "-fx-background: transparent;-fx-background-color: transparent;" +
            "-fx-border-color: transparent;-fx-padding: 0;-fx-background-radius: 10;");
        sp.setMaxHeight(200);

        Popup popup = new Popup();
        popup.setAutoHide(true);

        double trigW = trigger.getWidth() > 0 ? trigger.getWidth() : ADD_MODAL_W - 56;
        sp.setPrefWidth(trigW);
        listBox.setPrefWidth(trigW);

        String currentVal = ((String[]) trigger.getUserData())[0];

        for (int i = 0; i < items.size(); i++) {
            String  item   = items.get(i);
            boolean isSel  = item.equals(currentVal);
            boolean isLast = (i == items.size() - 1);

            Label rowLbl = new Label(item);
            rowLbl.setMaxWidth(Double.MAX_VALUE);
            rowLbl.setPrefWidth(trigW);
            rowLbl.setPadding(new Insets(10, 14, 10, 14));
            rowLbl.setCursor(javafx.scene.Cursor.HAND);
            rowLbl.setWrapText(false);
            rowLbl.setStyle(dropdownRowStyle(isSel, false, isLast));
            rowLbl.setOnMouseEntered(e -> rowLbl.setStyle(dropdownRowStyle(isSel, true,  isLast)));
            rowLbl.setOnMouseExited(e  -> rowLbl.setStyle(dropdownRowStyle(isSel, false, isLast)));
            rowLbl.setOnMouseClicked(e -> {
                ((String[]) trigger.getUserData())[0] = item;
                Label vl = (Label) trigger.getProperties().get("valueLabel");
                vl.setText(item);
                vl.setStyle("-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;" +
                            "-fx-font-weight: bold;-fx-text-fill: " + ACCENT + ";");
                popup.hide();
                trigger.getProperties().put("open", false);
                trigger.setStyle(dropdownTriggerStyle(false));
                Runnable cb = (Runnable) trigger.getProperties().get("onAction");
                if (cb != null) cb.run();
            });
            listBox.getChildren().add(rowLbl);
        }

        popup.getContent().add(sp);
        popup.setOnHidden(e -> {
            trigger.getProperties().put("open", false);
            trigger.setStyle(dropdownTriggerStyle(false));
        });
        javafx.geometry.Bounds bounds = trigger.localToScreen(trigger.getBoundsInLocal());
        if (bounds != null) popup.show(trigger, bounds.getMinX(), bounds.getMaxY() + 2);
        trigger.getProperties().put("open", true);
    }

    private String dropdownRowStyle(boolean selected, boolean hovered, boolean isLast) {
        String bg     = selected ? "#F5E8EA" : hovered ? ACCENT : "white";
        String fg     = selected ? ACCENT    : hovered ? "white" : "#333333";
        String fw     = selected ? "bold"    : "normal";
        String radius = isLast   ? "0 0 9 9" : "0";
        return "-fx-background-color: " + bg + ";-fx-background-radius: " + radius + ";" +
               "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;" +
               "-fx-font-weight: " + fw + ";-fx-text-fill: " + fg + ";";
    }

    // ══════════════════════════════════════════════════════
    //  DATE PICKER WIDGET  (modal — full-size Month + Day)
    // ══════════════════════════════════════════════════════

    private VBox buildDatePickerField(String label, LocalDate[] minDateRef) {
        Label fieldLabel = buildFieldLabel(label);

        VBox monthBox = buildDropdownField(FontAwesomeSolid.CALENDAR_ALT, "Month");
        setDropdownItems(monthBox, util.buildMonthOptions(), "Select month...");

        VBox dayBox = buildDropdownField(FontAwesomeSolid.CALENDAR_DAY, "Day");
        setDropdownItems(dayBox, new ArrayList<>(), "Select day...");
        setDropdownDisabled(dayBox, true);

        setDropdownOnAction(monthBox, () -> {
            String selMonth = getDropdownValue(monthBox);
            if (selMonth == null) { setDropdownDisabled(dayBox, true); return; }
            LocalDate minD = minDateRef != null ? minDateRef[0] : null;
            List<String> days = util.buildDayOptions(selMonth, minD);
            setDropdownItems(dayBox, days, days.isEmpty() ? "No available days" : "Select day...");
            setDropdownDisabled(dayBox, days.isEmpty());
        });

        HBox pairRow = new HBox(12, monthBox, dayBox);
        pairRow.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(monthBox, Priority.ALWAYS);
        HBox.setHgrow(dayBox,   Priority.ALWAYS);

        VBox wrapper = new VBox(6, fieldLabel, pairRow);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.getProperties().put("monthBox", monthBox);
        wrapper.getProperties().put("dayBox",   dayBox);
        return wrapper;
    }

    private LocalDate getDatePickerValue(VBox datePicker) {
        VBox monthBox = (VBox) datePicker.getProperties().get("monthBox");
        VBox dayBox   = (VBox) datePicker.getProperties().get("dayBox");
        return util.buildDate(getDropdownValue(monthBox), getDropdownValue(dayBox));
    }

    // ══════════════════════════════════════════════════════
    //  INLINE DATE EDITOR  (edit-mode table cells)
    // ══════════════════════════════════════════════════════

    private HBox buildInlineDateEditorCell(String promoId, String currentDateStr,
                                            boolean forEndDate, double width,
                                            LocalDate[] minDateRef) {
        LocalDate existing   = util.tryParseDate(currentDateStr);
        String existingMonth = existing != null
            ? existing.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
              + " " + existing.getYear()
            : null;
        String existingDay   = existing != null ? String.valueOf(existing.getDayOfMonth()) : null;

        VBox monthBox = buildMiniDropdownVBox(
            FontAwesomeSolid.CALENDAR_ALT,
            util.buildMonthOptions(),
            existingMonth != null ? existingMonth : "Month",
            existingMonth);

        List<String> initDays = existing != null
            ? util.buildDayOptions(existingMonth, minDateRef != null ? minDateRef[0] : null)
            : new ArrayList<>();
        VBox dayBox = buildMiniDropdownVBox(
            FontAwesomeSolid.CALENDAR_DAY,
            initDays,
            existingDay != null ? existingDay : "Day",
            existingDay);
        setDropdownDisabled(dayBox, initDays.isEmpty());

        setDropdownOnAction(monthBox, () -> {
            String selMonth = getDropdownValue(monthBox);
            if (selMonth == null) { setDropdownDisabled(dayBox, true); return; }
            LocalDate minD = minDateRef != null ? minDateRef[0] : null;
            List<String> days = util.buildDayOptions(selMonth, minD);
            setDropdownItems(dayBox, days, days.isEmpty() ? "—" : "Day");
            setDropdownDisabled(dayBox, days.isEmpty());
        });

        setDropdownOnAction(dayBox, () -> {
            LocalDate chosen = util.buildDate(getDropdownValue(monthBox), getDropdownValue(dayBox));
            if (chosen == null) return;
            int colIdx = forEndDate ? 4 : 3;
            for (String[] r : cachedRows) {
                if (r[0].equals(promoId)) { r[colIdx] = chosen.toString(); break; }
            }
            util.updateDate(promoId, forEndDate, chosen.toString());
            System.out.println("[promotions_contents] date edit: " + promoId
                + (forEndDate ? " end" : " start") + " → " + chosen);
        });

        HBox cell = new HBox(6, monthBox, dayBox);
        cell.setPrefWidth(width);
        cell.setMinHeight(ROW_H);
        cell.setPadding(new Insets(4, 6, 4, 6));
        cell.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(monthBox, Priority.ALWAYS);
        HBox.setHgrow(dayBox,   Priority.ALWAYS);
        return cell;
    }

    private VBox buildMiniDropdownVBox(FontAwesomeSolid iconCode, List<String> items,
                                        String promptText, String preselected) {
        FontIcon fi = new FontIcon(iconCode);
        fi.setIconSize(10);
        fi.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        Label valueLabel = new Label(promptText);
        valueLabel.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 11px;" +
            "-fx-text-fill: " + (preselected != null ? ACCENT : "#AAAAAA") + ";" +
            (preselected != null ? "-fx-font-weight: bold;" : ""));
        HBox.setHgrow(valueLabel, Priority.ALWAYS);
        valueLabel.setMaxWidth(Double.MAX_VALUE);

        FontIcon arrowIcon = new FontIcon(FontAwesomeSolid.CHEVRON_DOWN);
        arrowIcon.setIconSize(9);
        arrowIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        HBox trigger = new HBox(5, fi, valueLabel, arrowIcon);
        trigger.setAlignment(Pos.CENTER_LEFT);
        trigger.setPadding(new Insets(0, 8, 0, 8));
        trigger.setPrefHeight(30);
        trigger.setMaxWidth(Double.MAX_VALUE);
        trigger.setCursor(javafx.scene.Cursor.HAND);
        trigger.setStyle(dropdownTriggerStyle(false));
        trigger.setUserData(new String[]{ preselected });
        trigger.setOnMouseEntered(e -> {
            if (!Boolean.TRUE.equals(trigger.getProperties().get("open")))
                trigger.setStyle(dropdownTriggerStyle(true));
        });
        trigger.setOnMouseExited(e -> {
            if (!Boolean.TRUE.equals(trigger.getProperties().get("open")))
                trigger.setStyle(dropdownTriggerStyle(false));
        });
        trigger.getProperties().put("items",      new ArrayList<>(items));
        trigger.getProperties().put("valueLabel", valueLabel);
        trigger.getProperties().put("arrowIcon",  arrowIcon);
        trigger.getProperties().put("open",       false);
        trigger.getProperties().put("disabled",   false);
        trigger.setOnMouseClicked(e -> openCustomDropdown(trigger));

        VBox wrapper = new VBox(0, trigger);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        return wrapper;
    }

    // ══════════════════════════════════════════════════════
    //  ADD NEW PROMOTION MODAL
    // ══════════════════════════════════════════════════════
    private void openAddPromoModal() {
        Pane overlay = new Pane();
        overlay.setPrefWidth(totalW);  overlay.setPrefHeight(totalH);
        overlay.setMinWidth(totalW);   overlay.setMinHeight(totalH);
        overlay.setMaxWidth(totalW);   overlay.setMaxHeight(totalH);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.45);");

        VBox card = new VBox(0);
        card.setAlignment(Pos.TOP_LEFT);
        card.setMinWidth(ADD_MODAL_W);  card.setMaxWidth(ADD_MODAL_W);  card.setPrefWidth(ADD_MODAL_W);
        card.setMinHeight(ADD_MODAL_H); card.setMaxHeight(ADD_MODAL_H); card.setPrefHeight(ADD_MODAL_H);
        card.setStyle(
            "-fx-background-color: white;-fx-background-radius: 14;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0, 0, 6);");

        // ── Header ────────────────────────────────────────
        HBox cardHeader = new HBox(10);
        cardHeader.setPadding(new Insets(20, 24, 16, 24));
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        cardHeader.setStyle(
            "-fx-background-color: " + HEADER_BG + ";-fx-background-radius: 14 14 0 0;" +
            "-fx-border-color: transparent transparent " + TABLE_BORDER + " transparent;" +
            "-fx-border-width: 0 0 1.5 0;");

        FontIcon tagIcon = new FontIcon(FontAwesomeSolid.TAG);
        tagIcon.setIconSize(17);
        tagIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        Label modalTitle = new Label("Add New Promotion");
        modalTitle.setStyle("-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 20px;" +
                            "-fx-font-weight: 800;-fx-text-fill: " + ACCENT + ";");

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
        closeBtn.setStyle("-fx-background-color: #E9ECEF;-fx-background-radius: 6;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #DEE2E6;-fx-background-radius: 6;"));
        closeBtn.setOnMouseExited(e  -> closeBtn.setStyle("-fx-background-color: #E9ECEF;-fx-background-radius: 6;"));
        closeBtn.setOnMouseClicked(e -> stackRoot.getChildren().remove(overlay));
        cardHeader.getChildren().addAll(tagIcon, modalTitle, hSpacer, closeBtn);

        // ── Form body ─────────────────────────────────────
        VBox formBody = new VBox(16);
        formBody.setPadding(new Insets(22, 28, 10, 28));
        VBox.setVgrow(formBody, Priority.ALWAYS);

        VBox promoIdBox      = buildReadOnlyField(FontAwesomeSolid.HASHTAG, "Promo ID", "Auto-generated");
        VBox promoNameBox    = buildTextInputField(FontAwesomeSolid.PERCENT, "Promotion Name",
                                                   "e.g. Summer Sale 10%");
        VBox discountTypeBox = buildTextInputField(FontAwesomeSolid.TAG, "Discount Type",
                                                   "e.g. Percentage, Fixed, BOGO");

        LocalDate[] startDateRef = new LocalDate[]{ null };
        VBox startDatePicker = buildDatePickerField("Start Date", null);
        VBox endDatePicker   = buildDatePickerField("End Date", startDateRef);

        VBox startDayBox = (VBox) startDatePicker.getProperties().get("dayBox");
        VBox endMonthBox = (VBox) endDatePicker.getProperties().get("monthBox");
        VBox endDayBox   = (VBox) endDatePicker.getProperties().get("dayBox");

        // When start day changes → cache start date and reset end pickers
        setDropdownOnAction(startDayBox, () -> {
            startDateRef[0] = getDatePickerValue(startDatePicker);
            resetDropdown(endMonthBox, "Select month...");
            resetDropdown(endDayBox,   "Select day...");
            setDropdownDisabled(endDayBox, true);
        });

        Label errorLbl = new Label("");
        errorLbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 12px;-fx-text-fill: #882F39;");
        errorLbl.setVisible(false);
        errorLbl.setManaged(false);

        formBody.getChildren().addAll(promoIdBox, promoNameBox, discountTypeBox,
                                      startDatePicker, endDatePicker, errorLbl);

        // ── Footer ────────────────────────────────────────
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

        FontIcon saveIcon = new FontIcon(FontAwesomeSolid.CHECK);
        saveIcon.setIconSize(13);
        saveIcon.setIconColor(javafx.scene.paint.Color.WHITE);
        Label saveBtn = new Label("Add Promotion");
        saveBtn.setGraphic(saveIcon);
        saveBtn.setGraphicTextGap(7);
        saveBtn.setCursor(javafx.scene.Cursor.HAND);
        saveBtn.setPrefWidth(150); saveBtn.setPrefHeight(38);
        saveBtn.setAlignment(Pos.CENTER);
        saveBtn.setStyle(addSaveBtnStyle(false));
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle(addSaveBtnStyle(true)));
        saveBtn.setOnMouseExited(e  -> saveBtn.setStyle(addSaveBtnStyle(false)));

        TextField promoNameField    = extractTextField(promoNameBox);
        TextField discountTypeField = extractTextField(discountTypeBox);

        saveBtn.setOnMouseClicked(e -> {
            String    promoName    = promoNameField.getText().trim();
            String    discountType = discountTypeField.getText().trim();
            LocalDate startDate    = getDatePickerValue(startDatePicker);
            LocalDate endDate      = getDatePickerValue(endDatePicker);

            // Delegate validation to util
            String error = util.validate(promoName, discountType, startDate, endDate);
            if (error != null) { showError(errorLbl, error); return; }

            String newId = util.generateNextPromoId();
            util.insertPromotion(newId, promoName, discountType,
                                 startDate.toString(), endDate.toString());
            stackRoot.getChildren().remove(overlay);
            prependItem(newId, promoName, discountType,
                        startDate.toString(), endDate.toString());
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

    // ══════════════════════════════════════════════════════
    //  FORM FIELD BUILDERS
    // ══════════════════════════════════════════════════════

    private VBox buildReadOnlyField(FontAwesomeSolid iconCode, String label, String value) {
        Label fieldLabel = buildFieldLabel(label);
        FontIcon fi = new FontIcon(iconCode);
        fi.setIconSize(13);
        fi.setIconColor(javafx.scene.paint.Color.web("#AAAAAA"));
        Label valueLbl = new Label(value);
        HBox.setHgrow(valueLbl, Priority.ALWAYS);
        valueLbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;" +
            "-fx-text-fill: #AAAAAA;-fx-font-style: italic;");
        HBox box = new HBox(8, fi, valueLbl);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 12, 0, 12));
        box.setPrefHeight(40);
        box.setStyle(
            "-fx-background-color: #F8F9FA;-fx-background-radius: 10;" +
            "-fx-border-color: #DDDDDD;-fx-border-width: 1.5;-fx-border-radius: 10;");
        return new VBox(6, fieldLabel, box);
    }

    private VBox buildTextInputField(FontAwesomeSolid iconCode, String label, String prompt) {
        Label fieldLabel = buildFieldLabel(label);
        FontIcon fi = new FontIcon(iconCode);
        fi.setIconSize(13);
        fi.setIconColor(javafx.scene.paint.Color.web(ACCENT));
        TextField input = new TextField();
        input.setPromptText(prompt);
        HBox.setHgrow(input, Priority.ALWAYS);
        input.setStyle(
            "-fx-background-color: transparent;-fx-border-color: transparent;" +
            "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;" +
            "-fx-text-fill: #333333;-fx-prompt-text-fill: #AAAAAA;");
        HBox box = new HBox(8, fi, input);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 12, 0, 12));
        box.setPrefHeight(40);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle(
            "-fx-background-color: white;-fx-background-radius: 10;" +
            "-fx-border-color: " + ACCENT + ";-fx-border-width: 1.5;-fx-border-radius: 10;");
        VBox wrapper = new VBox(6, fieldLabel, box);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        return wrapper;
    }

    private Label buildFieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 12px;" +
            "-fx-font-weight: bold;-fx-text-fill: #555555;");
        return lbl;
    }

    private TextField extractTextField(VBox fieldBox) {
        HBox box = (HBox) fieldBox.getChildren().get(1);
        return (TextField) box.getChildren().get(1);
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

        double btnH  = 36;
        btnY     = TOP_PADDING + 10;
        iconW    = 36;
        gap      = 8;
        tabW     = 90;
        archAllW = 100;
        confirmW = 90;
        csvW     = 120;
        double addW  = 168;
        searchW  = 200;

        // ── Title ─────────────────────────────────────────
        Label title = new Label("Promotions");
        title.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 36px;" +
            "-fx-font-weight: 800;-fx-text-fill: " + ACCENT + ";");

        // ── Add New Promotion button ───────────────────────
        FontIcon plusIcon = new FontIcon(FontAwesomeSolid.PLUS);
        plusIcon.setIconSize(13);
        plusIcon.setIconColor(javafx.scene.paint.Color.web("#155724"));
        addPromoBtn = new Label("Add New Promotion");
        addPromoBtn.setGraphic(plusIcon);
        addPromoBtn.setGraphicTextGap(6);
        addPromoBtn.setCursor(javafx.scene.Cursor.HAND);
        addPromoBtn.setStyle(addPromoBtnStyle(false));
        addPromoBtn.setPrefHeight(btnH); addPromoBtn.setPrefWidth(addW);
        addPromoBtn.setAlignment(Pos.CENTER);
        addPromoBtn.setOnMouseEntered(e -> addPromoBtn.setStyle(addPromoBtnStyle(true)));
        addPromoBtn.setOnMouseExited(e  -> addPromoBtn.setStyle(addPromoBtnStyle(false)));
        addPromoBtn.setOnMouseClicked(e -> openAddPromoModal());

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

        HBox titleRow = new HBox(gap, title, addPromoBtn, editBtn, archiveBtn);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.setLayoutX(SIDE_PADDING); titleRow.setLayoutY(TOP_PADDING);
        titleRow.setPrefHeight(HEADER_H);

        // ── Right-side controls (right → left) ────────────
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
                "Promotions (" + currentTab + ")",
                "This will permanently remove all promotions in this view.\nThis action cannot be undone.",
                () -> {
                    util.deleteAll(currentTab);
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
        exportCsvBtn.setPrefHeight(btnH);    exportCsvBtn.setPrefWidth(csvW);
        exportCsvBtn.setAlignment(Pos.CENTER);
        exportCsvBtn.setOnMouseEntered(e -> exportCsvBtn.setStyle(exportCsvBtnStyle(true)));
        exportCsvBtn.setOnMouseExited(e  -> exportCsvBtn.setStyle(exportCsvBtnStyle(false)));
        exportCsvBtn.setOnMouseClicked(e -> exportCsv());

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

        // ── Archive All / Restore All ─────────────────────
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

        // ── Confirm ───────────────────────────────────────
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
            boolean archiving = currentTab.equals("active");
            util.archiveOrRestoreSelected(selectedIds, archiving);
            selectedIds.clear();
            archiveMode = false;
            updateArchiveBtnIcon();
            archiveAllBtn.setVisible(false);
            confirmBtn.setVisible(false);
            archiveBtn.setStyle(archiveBtnStyle(false));
            repositionSearchBar();
            cachedRows = util.fetchPromotions(currentTab);
            rebuildTable();
        });

        // ── Search bar ────────────────────────────────────
        FontIcon searchIcon = new FontIcon(FontAwesomeSolid.SEARCH);
        searchIcon.setIconSize(14);
        searchIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        searchField = new TextField();
        searchField.setPromptText("Search promotions...");
        searchField.setStyle(
            "-fx-background-color: transparent;-fx-border-color: transparent;" +
            "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;" +
            "-fx-text-fill: #333333;-fx-prompt-text-fill: #AAAAAA;");
        searchField.setPrefWidth(searchW - 42);

        searchBar = new HBox(6, searchIcon, searchField);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(0, 10, 0, 12));
        searchBar.setPrefWidth(searchW); searchBar.setPrefHeight(btnH);
        searchBar.setLayoutX(initialSearchX); searchBar.setLayoutY(btnY);
        searchBar.setStyle(
            "-fx-background-color: white;-fx-background-radius: 20;" +
            "-fx-border-color: " + ACCENT + ";-fx-border-width: 1.5;-fx-border-radius: 20;");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchQuery = newVal == null ? "" : newVal.trim();
            rebuildTable();
        });

        // ── Table ─────────────────────────────────────────
        double tableY = TOP_PADDING + HEADER_H + 10;
        double tableW = totalW - SIDE_PADDING * 2;
        double tableH = totalH - tableY - SIDE_PADDING;

        tableScroll = buildScrollPane(tableW, tableH, tableY);

        root.getChildren().addAll(
            titleRow, searchBar, archiveAllBtn, confirmBtn,
            activeTabBtn, archivedTabBtn, exportCsvBtn, deleteBtn, tableScroll
        );
        stackRoot.getChildren().add(root);
        return stackRoot;
    }

    private void exportCsv() {
        String csv = util.buildCsv(cachedRows);
        if (csv.isEmpty()) return;
        System.out.println("[promotions_contents] CSV export ready (" + cachedRows.size() + " rows).");
        // TODO: open FileChooser and write csv to chosen path
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
        editMode    = false;
        archiveMode = false;
        selectedIds.clear();
        searchQuery = "";
        if (searchField != null) searchField.clear();

        FontIcon penIcon = new FontIcon(FontAwesomeSolid.PEN);
        penIcon.setIconSize(15);
        penIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));
        editBtn.setGraphic(penIcon);
        editBtn.setStyle(editBtnStyle(false));

        updateArchiveBtnIcon();
        archiveAllBtn.setText(tab.equals("archived") ? "Restore All" : "Archive All");
        archiveAllBtn.setVisible(false);
        confirmBtn.setVisible(false);
        archiveBtn.setStyle(archiveBtnStyle(false));
        activeTabBtn.setStyle(tabBtnStyle(tab.equals("active")));
        archivedTabBtn.setStyle(tabBtnStyle(tab.equals("archived")));
        repositionSearchBar();
        cachedRows = util.fetchPromotions(tab);
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
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0, 0, 6);");

        FontIcon warnIcon = new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
        warnIcon.setIconSize(28);
        warnIcon.setIconColor(javafx.scene.paint.Color.web("#882F39"));

        Label heading = new Label("Are you sure you want to delete\nall entries for " + context + "?");
        heading.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 14px;-fx-font-weight: bold;" +
            "-fx-text-fill: #222222;-fx-text-alignment: center;-fx-alignment: center;");
        heading.setAlignment(Pos.CENTER); heading.setWrapText(true); heading.setMaxWidth(MODAL_W - 80);

        Label sub = new Label(subMessage);
        sub.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 12px;" +
            "-fx-text-fill: #777777;-fx-text-alignment: center;-fx-alignment: center;");
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
    //  TABLE REBUILD
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
        List<String[]> visible = util.filterRows(cachedRows, searchQuery);
        VBox tableBox = buildTable(tableW, visible);
        ScrollPane sp = new ScrollPane(tableBox);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setFitToWidth(true); sp.setPannable(true);
        sp.setStyle(
            "-fx-background: transparent;-fx-background-color: transparent;" +
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
        table.setStyle(
            "-fx-border-color: " + TABLE_BORDER + ";-fx-border-width: 1.5;" +
            "-fx-border-radius: 10;-fx-background-color: white;-fx-background-radius: 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 10, 0, 0, 3);");
        table.getChildren().add(buildHeaderRow(tableW, dataW));

        if (rows.isEmpty()) {
            String msg = !searchQuery.isBlank()
                ? "No results found for \"" + searchQuery + "\"."
                : currentTab.equals("archived") ? "No archived promotions." : "No promotions yet.";
            Label empty = new Label(msg);
            empty.setStyle(
                "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 14px;" +
                "-fx-text-fill: #AAAAAA;-fx-padding: 24 0 24 16;");
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
            "-fx-border-width: 0 0 1.5 0;");
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(
            buildHeaderCell("Promo ID",      dataW * COL_PROMO_ID),      buildColDivider(),
            buildHeaderCell("Promo Name",    dataW * COL_PROMO_NAME),    buildColDivider(),
            buildHeaderCell("Discount Type", dataW * COL_DISCOUNT_TYPE), buildColDivider(),
            buildHeaderCell("Start Date",    dataW * COL_START_DATE),    buildColDivider(),
            buildHeaderCell("End Date",      dataW * COL_END_DATE)
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
            "-fx-font-weight: bold;-fx-text-fill: " + ACCENT + ";");
        return lbl;
    }

    private HBox buildDataRow(String promoId, String promoName,
                               String discountType, String startDate, String endDate,
                               String bg, double tableW, double dataW, boolean isLast) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.TOP_LEFT);
        String  bottomRadius = isLast ? "0 0 10 10" : "0";
        String  borderBottom = isLast ? "0" : "1";
        boolean selected     = selectedIds.contains(promoId);
        row.setStyle(rowStyle(selected ? "#FDE8EA" : bg, bottomRadius, borderBottom));
        row.setOnMouseEntered(e -> {
            if (!selectedIds.contains(promoId))
                row.setStyle(rowStyle("#F5E8EA", bottomRadius, borderBottom));
        });
        row.setOnMouseExited(e ->
            row.setStyle(rowStyle(selectedIds.contains(promoId) ? "#FDE8EA" : bg,
                                  bottomRadius, borderBottom)));

        LocalDate parsedStart    = util.tryParseDate(startDate);
        LocalDate[] startDateRef = new LocalDate[]{ parsedStart };

        javafx.scene.Node startCell;
        javafx.scene.Node endCell;

        if (editMode && currentTab.equals("active")) {
            startCell = buildInlineDateEditorCell(promoId, startDate, false,
                                                  dataW * COL_START_DATE, null);
            endCell   = buildInlineDateEditorCell(promoId, endDate,   true,
                                                  dataW * COL_END_DATE,   startDateRef);
        } else {
            startCell = buildTextCell(startDate, dataW * COL_START_DATE, false);
            endCell   = buildTextCell(endDate,   dataW * COL_END_DATE,   false);
        }

        row.getChildren().addAll(
            buildTextCell(promoId,      dataW * COL_PROMO_ID,      true),  buildColDivider(),
            buildTextCell(promoName,    dataW * COL_PROMO_NAME,    false), buildColDivider(),
            buildTextCell(discountType, dataW * COL_DISCOUNT_TYPE, false), buildColDivider(),
            startCell,                                                       buildColDivider(),
            endCell
        );

        if (archiveMode) {
            row.getChildren().add(buildColDivider());
            CheckBox cb = new CheckBox();
            cb.setSelected(selected); cb.setStyle("-fx-cursor: hand;");
            cb.setOnAction(e -> {
                if (cb.isSelected()) {
                    selectedIds.add(promoId);
                    row.setStyle(rowStyle("#FDE8EA", bottomRadius, borderBottom));
                } else {
                    selectedIds.remove(promoId);
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
    //  CELL HELPERS
    // ══════════════════════════════════════════════════════
    private String rowStyle(String bg, String bottomRadius, String borderBottom) {
        return "-fx-background-color: " + bg + ";-fx-background-radius: " + bottomRadius + ";" +
               "-fx-border-color: transparent transparent " + TABLE_BORDER + " transparent;" +
               "-fx-border-width: 0 0 " + borderBottom + " 0;";
    }

    private HBox buildTextCell(String text, double width, boolean bold) {
        Label lbl = new Label(text != null ? text : "—");
        lbl.setPrefWidth(width - 16);
        lbl.setMaxWidth(width - 16);
        lbl.setWrapText(true);
        lbl.setPadding(new Insets(10, 8, 10, 0));
        lbl.setAlignment(Pos.TOP_LEFT);
        lbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';-fx-font-size: 13px;" +
            "-fx-font-weight: " + (bold ? "bold" : "normal") + ";-fx-text-fill: #333333;");
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
        div.setStyle("-fx-background-color: " + TABLE_BORDER + ";-fx-opacity: 0.35;");
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
    private String addPromoBtnStyle(boolean hovered) {
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