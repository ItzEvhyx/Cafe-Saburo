package frontend;

import backend.menu_util;
import backend.menu_util.SubmitResult;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class menu_contents {

    private static final double SIDE_PANEL_W = 290;
    private static final double HEADER_H     = 60;
    private static final double FILTER_BAR_H = 56;
    private static final double TOP_PADDING  = 16;
    private static final double SIDE_PADDING = 20;
    private static final double CARD_W     = 210;
    private static final double CARD_H     = 280;
    private static final double CARD_IMG_H = 130;
    private static final double CARD_GAP   = 16;

    private static final String ACCENT      = "#882F39";
    private static final String ACCENT_DARK = "#6B1E26";
    private static final String GREEN       = "#28A745";
    private static final String GREEN_DARK  = "#1E7E34";
    private static final String FONT_FAMILY = "Aleo";

    private static final String CARD_STYLE_NORMAL =
        "-fx-background-color: white;" +
        "-fx-background-radius: 16;" +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 14, 0, 0, 4);";

    private static final String CARD_STYLE_HOVER =
        "-fx-background-color: white;" +
        "-fx-background-radius: 16;" +
        "-fx-effect: dropshadow(gaussian, rgba(136,47,57,0.22), 24, 0, 0, 8);" +
        "-fx-translate-y: -3;";

    private final double    totalW;
    private final double    totalH;
    private final menu_util util;

    private VBox             orderListBox;
    private Label            amountLabel;
    private TextField        amountPaidField;
    private Label            changeLabel;
    private TextField        customerNameField;
    private ComboBox<String> paymentCombo;
    private Label            nameErrorLabel;
    private Label            amountErrorLabel;
    private Label            paymentErrorLabel;
    private StackPane        rootStack;

    private Consumer<SubmitResult> onOrderSubmitted;

    public menu_contents(double totalW, double totalH) {
        this(totalW, totalH, null);
    }

    public menu_contents(double totalW, double totalH, Connection conn) {
        this.totalW = totalW;
        this.totalH = totalH;
        this.util   = new menu_util(conn);
        loadFonts();
    }

    public void setOnOrderSubmitted(Consumer<SubmitResult> callback) {
        this.onOrderSubmitted = callback;
    }

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

    // ══════════════════════════════════════════════════════
    //  ROOT VIEW
    // ══════════════════════════════════════════════════════
    public Pane getView() {
        rootStack = new StackPane();
        rootStack.setPrefWidth(totalW);
        rootStack.setPrefHeight(totalH);
        rootStack.setAlignment(Pos.TOP_LEFT);

        Pane inner = new Pane();
        inner.setPrefWidth(totalW);
        inner.setPrefHeight(totalH);
        inner.getChildren().addAll(
            buildMainArea(totalW - SIDE_PANEL_W),
            buildSidePanel()
        );
        rootStack.getChildren().add(inner);
        return rootStack;
    }

    // ══════════════════════════════════════════════════════
    //  MAIN AREA  ← search wired here
    // ══════════════════════════════════════════════════════
    private Pane buildMainArea(double mainW) {
        Pane area = new Pane();
        area.setPrefWidth(mainW);
        area.setPrefHeight(totalH);

        Label titleLabel = new Label("Menu Items");
        titleLabel.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 36px;" +
            "-fx-font-weight: 800;" +
            "-fx-text-fill: " + ACCENT + ";"
        );
        titleLabel.setLayoutX(SIDE_PADDING);
        titleLabel.setLayoutY(TOP_PADDING);
        titleLabel.setPrefHeight(HEADER_H);

        // ── Search bar ────────────────────────────────────
        double searchW = 230, searchH = 38;
        FontIcon searchIcon = new FontIcon(FontAwesomeSolid.SEARCH);
        searchIcon.setIconSize(16);
        searchIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        TextField searchField = new TextField();
        searchField.setPromptText("Search Item...");
        searchField.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;" +
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 14px;" +
            "-fx-text-fill: #333333;" +
            "-fx-prompt-text-fill: #AAAAAA;"
        );
        searchField.setPrefWidth(searchW - 40);

        HBox searchBar = new HBox(8, searchIcon, searchField);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(0, 12, 0, 14));
        searchBar.setPrefWidth(searchW);
        searchBar.setPrefHeight(searchH);
        searchBar.setLayoutX(mainW - SIDE_PADDING - searchW);
        searchBar.setLayoutY(TOP_PADDING + (HEADER_H - searchH) / 2.0);
        searchBar.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: " + ACCENT + ";" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 20;"
        );

        // ── Build sections + track their raw item data ────
        // sectionNodes[i] pairs: { VBox (the section node), FlowPane (its grid) }
        // We also keep the raw item names per section for filtering.
        double filterY = TOP_PADDING + HEADER_H + 4;

        // sectionNodes — the full VBox for each category section (index 0 = Espresso etc.)
        List<VBox>      sectionNodes    = new ArrayList<>();
        // sectionGrids  — the FlowPane inside each section so we can show/hide individual cards
        List<FlowPane>  sectionGrids    = new ArrayList<>();
        // sectionItems  — raw item data mirroring CATEGORY_DATA[1..n]
        List<String[][]> sectionItems   = new ArrayList<>();

        for (int i = 1; i < menu_util.CATEGORY_NAMES.length; i++) {
            String[][]  data    = menu_util.CATEGORY_DATA[i];
            String      title   = menu_util.SECTION_TITLES[i];

            double wrapLen = 4 * CARD_W + 3 * CARD_GAP + 2;
            FlowPane grid = new FlowPane(CARD_GAP, CARD_GAP);
            grid.setPrefWrapLength(wrapLen);
            for (String[] item : data)
                grid.getChildren().add(
                    buildMenuCard(item[0], item[1], item[2], Boolean.parseBoolean(item[3]))
                );

            VBox section = new VBox(12);
            section.getChildren().addAll(buildDivider(title), grid);

            sectionNodes.add(section);
            sectionGrids.add(grid);
            sectionItems.add(data);
        }

        VBox allSections = new VBox(28);
        allSections.setPadding(new Insets(4, SIDE_PADDING, 20, SIDE_PADDING));
        allSections.getChildren().addAll(sectionNodes);

        double gridY = filterY + FILTER_BAR_H + 8;
        ScrollPane gridScroll = new ScrollPane(allSections);
        gridScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        gridScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        gridScroll.setFitToWidth(true);
        gridScroll.setPannable(true);
        gridScroll.setStyle(
            "-fx-background: transparent;" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;" +
            "-fx-padding: 0;"
        );
        gridScroll.setPrefWidth(mainW - SIDE_PADDING);
        gridScroll.setPrefHeight(totalH - gridY - 10);
        gridScroll.setLayoutX(SIDE_PADDING / 2);
        gridScroll.setLayoutY(gridY);

        // ── Pills ─────────────────────────────────────────
        HBox pillRow = new HBox(12);
        pillRow.setAlignment(Pos.CENTER_LEFT);
        pillRow.setPadding(new Insets(6, 12, 6, 0));
        final int[] activePillIdx = { 0 };
        List<Label> pills = new ArrayList<>();

        for (int i = 0; i < menu_util.CATEGORY_NAMES.length; i++) {
            final int idx = i;
            Label pill = new Label(menu_util.CATEGORY_NAMES[i]);
            pill.setStyle(i == 0 ? pillStyle(false, true) : pillStyle(false, false));
            pills.add(pill);
            pill.setOnMouseEntered(e -> { if (activePillIdx[0] != idx) pill.setStyle(pillStyle(true, false)); });
            pill.setOnMouseExited(e  -> { if (activePillIdx[0] != idx) pill.setStyle(pillStyle(false, false)); });
            pill.setOnMouseClicked(e -> {
                // Clear search when switching pill
                searchField.clear();

                pills.get(activePillIdx[0]).setStyle(pillStyle(false, false));
                activePillIdx[0] = idx;
                pill.setStyle(pillStyle(false, true));
                allSections.getChildren().clear();
                gridScroll.setVvalue(0);
                if (idx == 0) {
                    // Restore all cards to full visibility before showing all sections
                    for (int s = 0; s < sectionGrids.size(); s++) {
                        FlowPane   grid    = sectionGrids.get(s);
                        String[][] data    = sectionItems.get(s);
                        for (int c = 0; c < grid.getChildren().size(); c++)
                            grid.getChildren().get(c).setVisible(true);
                        sectionNodes.get(s).setVisible(true);
                        sectionNodes.get(s).setManaged(true);
                    }
                    allSections.getChildren().addAll(sectionNodes);
                } else {
                    // Show only the chosen section, all cards visible
                    VBox   chosen = sectionNodes.get(idx - 1);
                    FlowPane grd  = sectionGrids.get(idx - 1);
                    for (int c = 0; c < grd.getChildren().size(); c++)
                        grd.getChildren().get(c).setVisible(true);
                    chosen.setVisible(true);
                    chosen.setManaged(true);
                    allSections.getChildren().add(chosen);
                }
            });
            pillRow.getChildren().add(pill);
        }

        ScrollPane filterScroll = new ScrollPane(pillRow);
        filterScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        filterScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        filterScroll.setFitToHeight(true);
        filterScroll.setPannable(true);
        filterScroll.setStyle(
            "-fx-background: transparent;" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;" +
            "-fx-padding: 0;"
        );
        filterScroll.setPrefWidth(mainW - SIDE_PADDING * 2);
        filterScroll.setPrefHeight(FILTER_BAR_H);
        filterScroll.setLayoutX(SIDE_PADDING);
        filterScroll.setLayoutY(filterY);

        // ── Search listener ───────────────────────────────
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal == null ? "" : newVal.trim().toLowerCase();

            // Reset pill to "All" when the user starts typing
            if (!query.isEmpty() && activePillIdx[0] != 0) {
                pills.get(activePillIdx[0]).setStyle(pillStyle(false, false));
                activePillIdx[0] = 0;
                pills.get(0).setStyle(pillStyle(false, true));
            }

            allSections.getChildren().clear();
            gridScroll.setVvalue(0);

            if (query.isEmpty()) {
                // Restore everything
                for (int s = 0; s < sectionNodes.size(); s++) {
                    FlowPane   grid = sectionGrids.get(s);
                    for (int c = 0; c < grid.getChildren().size(); c++)
                        grid.getChildren().get(c).setVisible(true);
                    sectionNodes.get(s).setVisible(true);
                    sectionNodes.get(s).setManaged(true);
                }
                allSections.getChildren().addAll(sectionNodes);
                return;
            }

            // Filter: show only cards whose name contains the query
            for (int s = 0; s < sectionNodes.size(); s++) {
                FlowPane   grid = sectionGrids.get(s);
                String[][] data = sectionItems.get(s);
                boolean    anyVisible = false;

                for (int c = 0; c < grid.getChildren().size(); c++) {
                    String itemName = data[c][0].toLowerCase();
                    boolean matches = itemName.contains(query);
                    grid.getChildren().get(c).setVisible(matches);
                    grid.getChildren().get(c).setManaged(matches);
                    if (matches) anyVisible = true;
                }

                // Hide the whole section (header + grid) if no cards match
                sectionNodes.get(s).setVisible(anyVisible);
                sectionNodes.get(s).setManaged(anyVisible);
                if (anyVisible) allSections.getChildren().add(sectionNodes.get(s));
            }
        });

        area.getChildren().addAll(titleLabel, searchBar, filterScroll, gridScroll);
        return area;
    }

    // ══════════════════════════════════════════════════════
    //  SIDE PANEL
    // ══════════════════════════════════════════════════════
    private StackPane buildSidePanel() {
        StackPane panel = new StackPane();
        panel.setPrefWidth(SIDE_PANEL_W);
        panel.setPrefHeight(totalH);
        panel.setLayoutX(totalW - SIDE_PANEL_W);
        panel.setStyle("-fx-border-color: " + ACCENT + "; -fx-border-width: 0 0 0 1.5;");
        panel.setAlignment(Pos.TOP_CENTER);

        ImageView bg = new ImageView(new Image("file:assets/cafe_bg_wpanel.png"));
        bg.setFitWidth(SIDE_PANEL_W);
        bg.setFitHeight(totalH);
        bg.setPreserveRatio(false);

        panel.getChildren().addAll(bg, buildOrderDetailsOverlay());
        return panel;
    }

    // ══════════════════════════════════════════════════════
    //  ORDER DETAILS OVERLAY
    // ══════════════════════════════════════════════════════
    private VBox buildOrderDetailsOverlay() {
        double pad = 13;

        VBox overlay = new VBox(0);
        overlay.setPrefWidth(SIDE_PANEL_W);
        overlay.setPrefHeight(totalH);
        overlay.setPadding(new Insets(12, pad, 10, pad));
        overlay.setAlignment(Pos.TOP_CENTER);
        overlay.setStyle("-fx-background-color: transparent;");

        Label title = new Label("Order Details");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);
        title.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 30px;" +
            "-fx-font-weight: 800;" +
            "-fx-text-fill: " + ACCENT + ";" +
            "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.98), 8, 0, 0, 1);"
        );
        VBox.setMargin(title, new Insets(4, 0, 10, 0));

        customerNameField = new TextField();
        customerNameField.setPromptText("Customer name");
        customerNameField.setMaxWidth(Double.MAX_VALUE);
        customerNameField.setStyle(inputStyle(false));
        VBox.setMargin(customerNameField, new Insets(0, 0, 2, 0));
        customerNameField.textProperty().addListener((obs, o, n) -> {
            if (!n.isBlank()) clearError(nameErrorLabel, customerNameField, false);
        });

        nameErrorLabel = buildErrorLabel();
        VBox.setMargin(nameErrorLabel, new Insets(0, 0, 6, 2));

        orderListBox = new VBox(7);
        orderListBox.setPadding(new Insets(8, 8, 8, 8));
        orderListBox.setStyle("-fx-background-color: transparent;");

        ScrollPane itemsScroll = new ScrollPane(orderListBox);
        itemsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        itemsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        itemsScroll.setFitToWidth(true);
        itemsScroll.setPannable(true);
        itemsScroll.setMinHeight(60);
        itemsScroll.setStyle(
            "-fx-background: transparent;" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;" +
            "-fx-padding: 0;"
        );

        VBox itemsCard = new VBox(itemsScroll);
        itemsCard.setMaxWidth(Double.MAX_VALUE);
        itemsCard.setPadding(new Insets(2));
        itemsCard.setStyle(
            "-fx-background-color: rgba(255,255,255,0.94);" +
            "-fx-background-radius: 14;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.13), 10, 0, 0, 3);"
        );
        VBox.setVgrow(itemsCard, Priority.ALWAYS);
        VBox.setMargin(itemsCard, new Insets(0, 0, 10, 0));

        VBox bottomBlock = buildBottomBlock();
        VBox.setMargin(bottomBlock, new Insets(0, 0, 8, 0));

        Button submitBtn = buildActionBtn("Submit Order", FontAwesomeSolid.CHECK_CIRCLE, true);
        Button deleteBtn = buildActionBtn("Delete Order",  FontAwesomeSolid.TRASH_ALT,   false);

        submitBtn.setOnMouseClicked(e -> onSubmitOrder());
        deleteBtn.setOnMouseClicked(e -> onDeleteOrder());

        VBox.setMargin(submitBtn, new Insets(0, 0, 4, 0));
        VBox.setMargin(deleteBtn, new Insets(0, 0, 8, 0));

        overlay.getChildren().addAll(
            title, customerNameField, nameErrorLabel,
            itemsCard, bottomBlock, submitBtn, deleteBtn
        );

        refreshOrderList();
        return overlay;
    }

    private VBox buildBottomBlock() {
        Label taxNote = new Label("(Total + 12% tax)");
        taxNote.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 12px;" +
            "-fx-font-style: italic;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #444444;" +
            "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.9), 4, 0, 0, 1);"
        );

        amountLabel = new Label("Amount: ₱0.00");
        amountLabel.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 22px;" +
            "-fx-font-weight: 800;" +
            "-fx-text-fill: " + ACCENT + ";" +
            "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.96), 5, 0, 0, 1);"
        );
        VBox.setMargin(amountLabel, new Insets(0, 0, 4, 0));

        Region sep = new Region();
        sep.setPrefHeight(1.5);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color: rgba(136,47,57,0.35);");
        VBox.setMargin(sep, new Insets(0, 0, 8, 0));

        Label paidLbl = new Label("Amount Paid");
        paidLbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #333333;" +
            "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.9), 4, 0, 0, 1);"
        );

        amountPaidField = new TextField();
        amountPaidField.setPromptText("₱ Amount paid");
        amountPaidField.setMaxWidth(Double.MAX_VALUE);
        amountPaidField.setStyle(inputStyle(false));
        amountPaidField.textProperty().addListener((obs, o, n) -> {
            refreshChange();
            String raw = n.trim().replace("₱", "");
            if (!raw.isBlank()) clearError(amountErrorLabel, amountPaidField, false);
        });
        VBox.setMargin(amountPaidField, new Insets(2, 0, 2, 0));

        amountErrorLabel = buildErrorLabel();
        VBox.setMargin(amountErrorLabel, new Insets(0, 0, 4, 2));

        Label changeLbl = new Label("Change:");
        changeLbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #333333;" +
            "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.9), 4, 0, 0, 1);"
        );
        changeLabel = new Label("₱0.00");
        changeLabel.setStyle(changeLabelStyle(0));

        HBox changeRow = new HBox(8, changeLbl, changeLabel);
        changeRow.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(changeRow, new Insets(0, 0, 6, 0));

        Label payLbl = new Label("Payment Method");
        payLbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #333333;" +
            "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.9), 4, 0, 0, 1);"
        );

        paymentCombo = new ComboBox<>();
        paymentCombo.getItems().addAll("Cash", "GCash", "Card");
        paymentCombo.setPromptText("Payment");
        paymentCombo.setMaxWidth(Double.MAX_VALUE);
        paymentCombo.setStyle(comboStyle(false));
        paymentCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                clearError(paymentErrorLabel, null, true);
                paymentCombo.setStyle(comboStyle(false));
            }
        });
        VBox.setMargin(paymentCombo, new Insets(2, 0, 2, 0));

        paymentErrorLabel = buildErrorLabel();
        VBox.setMargin(paymentErrorLabel, new Insets(0, 0, 0, 2));

        VBox block = new VBox(0,
            taxNote, amountLabel, sep,
            paidLbl, amountPaidField, amountErrorLabel,
            changeRow,
            payLbl, paymentCombo, paymentErrorLabel
        );
        block.setStyle("-fx-background-color: transparent;");
        return block;
    }

    // ══════════════════════════════════════════════════════
    //  VALIDATION + SUBMIT
    // ══════════════════════════════════════════════════════
    private void onSubmitOrder() {
        if (util.isEmpty()) return;

        boolean hasError = false;

        String name = customerNameField.getText().trim();
        if (name.isBlank()) {
            showError(nameErrorLabel, customerNameField, "Customer name is required.", false);
            hasError = true;
        }

        String rawPaid = amountPaidField.getText().trim().replace("₱", "");
        double paid    = 0;
        if (rawPaid.isBlank()) {
            showError(amountErrorLabel, amountPaidField, "Amount paid is required.", false);
            hasError = true;
        } else {
            try {
                paid = Double.parseDouble(rawPaid);
            } catch (NumberFormatException ex) {
                showError(amountErrorLabel, amountPaidField, "Enter a valid number.", false);
                hasError = true;
            }
        }

        String payment = paymentCombo.getValue();
        if (payment == null || payment.isBlank()) {
            showError(paymentErrorLabel, null, "Select a payment method.", true);
            paymentCombo.setStyle(comboStyle(true));
            hasError = true;
        }

        if (hasError) return;

        double total = util.getTotal();
        if (paid < total) {
            showInsufficientModal(paid, total, total - paid);
            return;
        }

        SubmitResult result = util.submitOrder(name, payment);
        if (result.success) {
            util.clearOrder();
            resetOrderUI();
            if (onOrderSubmitted != null) onOrderSubmitted.accept(result);
        }
    }

    private void onDeleteOrder() {
        util.clearOrder();
        resetOrderUI();
    }

    private void resetOrderUI() {
        customerNameField.clear();
        customerNameField.setStyle(inputStyle(false));
        amountPaidField.clear();
        amountPaidField.setStyle(inputStyle(false));
        paymentCombo.setValue(null);
        paymentCombo.setStyle(comboStyle(false));
        hideError(nameErrorLabel);
        hideError(amountErrorLabel);
        hideError(paymentErrorLabel);
        refreshOrderList();
    }

    // ══════════════════════════════════════════════════════
    //  INSUFFICIENT PAYMENT MODAL
    // ══════════════════════════════════════════════════════
    private void showInsufficientModal(double paid, double total, double needed) {
        Pane overlay = new Pane();
        overlay.setPrefWidth(totalW);
        overlay.setPrefHeight(totalH);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.50);");

        FontIcon warnIcon = new FontIcon(FontAwesomeSolid.EXCLAMATION_CIRCLE);
        warnIcon.setIconSize(36);
        warnIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        Label heading = new Label("Insufficient Amount!");
        heading.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 20px;" +
            "-fx-font-weight: 800;" +
            "-fx-text-fill: " + ACCENT + ";"
        );

        Label detailPaid  = styledDetailLabel(String.format("Amount Paid:   ₱%.2f", paid));
        Label detailTotal = styledDetailLabel(String.format("Order Total:     ₱%.2f", total));

        Region sep = new Region();
        sep.setPrefHeight(1.5);
        sep.setMaxWidth(320);
        sep.setStyle("-fx-background-color: rgba(136,47,57,0.25);");
        VBox.setMargin(sep, new Insets(4, 0, 4, 0));

        Label detailNeeded = new Label(String.format("Please pay  ₱%.2f  more to submit.", needed));
        detailNeeded.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #721C24;" +
            "-fx-text-alignment: center;"
        );
        detailNeeded.setAlignment(Pos.CENTER);
        detailNeeded.setWrapText(true);

        Label okBtn = new Label("OK, I'll update the amount");
        okBtn.setCursor(javafx.scene.Cursor.HAND);
        okBtn.setPrefWidth(220);
        okBtn.setPrefHeight(40);
        okBtn.setAlignment(Pos.CENTER);
        okBtn.setStyle(modalOkBtnStyle(false));
        okBtn.setOnMouseEntered(e -> okBtn.setStyle(modalOkBtnStyle(true)));
        okBtn.setOnMouseExited(e  -> okBtn.setStyle(modalOkBtnStyle(false)));
        okBtn.setOnMouseClicked(e -> rootStack.getChildren().remove(overlay));

        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(36, 44, 32, 44));
        card.setMaxWidth(400);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 28, 0, 0, 6);"
        );
        card.getChildren().addAll(warnIcon, heading, detailPaid, detailTotal, sep, detailNeeded, okBtn);

        StackPane centred = new StackPane(card);
        centred.setPrefWidth(totalW);
        centred.setPrefHeight(totalH);
        centred.setAlignment(Pos.CENTER);
        overlay.getChildren().add(centred);
        rootStack.getChildren().add(overlay);
    }

    private Label styledDetailLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #333333;" +
            "-fx-text-alignment: center;"
        );
        lbl.setAlignment(Pos.CENTER);
        lbl.setMaxWidth(Double.MAX_VALUE);
        return lbl;
    }

    // ══════════════════════════════════════════════════════
    //  ERROR HELPERS
    // ══════════════════════════════════════════════════════
    private Label buildErrorLabel() {
        Label lbl = new Label();
        lbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #C0392B;"
        );
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setVisible(false);
        lbl.setManaged(false);
        return lbl;
    }

    private void showError(Label errorLbl, TextField field, String msg, boolean isCombo) {
        errorLbl.setText("⚠  " + msg);
        errorLbl.setVisible(true);
        errorLbl.setManaged(true);
        if (field != null) field.setStyle(inputStyle(true));
    }

    private void clearError(Label errorLbl, TextField field, boolean isCombo) {
        hideError(errorLbl);
        if (field != null) field.setStyle(inputStyle(false));
    }

    private void hideError(Label errorLbl) {
        errorLbl.setVisible(false);
        errorLbl.setManaged(false);
    }

    // ══════════════════════════════════════════════════════
    //  UI REFRESH
    // ══════════════════════════════════════════════════════
    private void refreshOrderList() {
        orderListBox.getChildren().clear();
        List<String[]> items = util.getOrderItems();

        if (items.isEmpty()) {
            Label empty = new Label("No items added yet.");
            empty.setPadding(new Insets(10, 6, 10, 6));
            empty.setStyle(
                "-fx-font-family: '" + FONT_FAMILY + "';" +
                "-fx-font-size: 13px;" +
                "-fx-font-style: italic;" +
                "-fx-text-fill: #AAAAAA;"
            );
            orderListBox.getChildren().add(empty);
            updateAmountLabel();
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            final int idx  = i;
            String[]  item = items.get(i);
            String    name = item[0];
            String    price= item[1];
            int       qty  = Integer.parseInt(item[2]);
            double    line = menu_util.parsePrice(price) * qty;

            Label nameLbl = new Label(name);
            nameLbl.setWrapText(true);
            nameLbl.setMaxWidth(SIDE_PANEL_W - 110);
            nameLbl.setStyle(
                "-fx-font-family: '" + FONT_FAMILY + "';" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #222222;"
            );

            Label priceLbl = new Label(String.format("₱%.0f", line));
            priceLbl.setStyle(
                "-fx-font-family: '" + FONT_FAMILY + "';" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: 800;" +
                "-fx-text-fill: " + ACCENT + ";"
            );

            Region namespacer = new Region();
            HBox.setHgrow(namespacer, Priority.ALWAYS);
            HBox topRow = new HBox(4, nameLbl, namespacer, priceLbl);
            topRow.setAlignment(Pos.CENTER_LEFT);

            Label minusBtn = new Label("−");
            minusBtn.setCursor(javafx.scene.Cursor.HAND);
            minusBtn.setStyle(qtyBtnStyle());
            minusBtn.setOnMouseClicked(e -> { util.decrementQty(idx); refreshOrderList(); });

            Label qtyLbl = new Label(qty + "x");
            qtyLbl.setStyle(
                "-fx-font-family: '" + FONT_FAMILY + "';" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #333333;"
            );

            Label plusBtn = new Label("+");
            plusBtn.setCursor(javafx.scene.Cursor.HAND);
            plusBtn.setStyle(qtyBtnStyle());
            plusBtn.setOnMouseClicked(e -> { util.incrementQty(idx); refreshOrderList(); });

            HBox qtyRow = new HBox(8, minusBtn, qtyLbl, plusBtn);
            qtyRow.setAlignment(Pos.CENTER_LEFT);

            VBox itemBox = new VBox(4, topRow, qtyRow);
            itemBox.setPadding(new Insets(7, 9, 7, 9));
            itemBox.setStyle(
                "-fx-background-color: #FDF5F6;" +
                "-fx-background-radius: 8;"
            );
            orderListBox.getChildren().add(itemBox);
        }

        updateAmountLabel();
        refreshChange();
    }

    private void updateAmountLabel() {
        amountLabel.setText(String.format("Amount: ₱%.2f", util.getTotal()));
    }

    private void refreshChange() {
        String raw = amountPaidField != null
            ? amountPaidField.getText().trim().replace("₱", "")
            : "";
        try {
            double paid   = raw.isEmpty() ? 0 : Double.parseDouble(raw);
            double change = util.getChange(paid);
            changeLabel.setText(String.format("₱%.2f", change));
            changeLabel.setStyle(changeLabelStyle(change));
        } catch (NumberFormatException ignored) {
            changeLabel.setText("—");
        }
    }

    private void addToOrder(String name, String priceStr) {
        util.addItem(name, priceStr);
        refreshOrderList();
    }

    // ══════════════════════════════════════════════════════
    //  MENU GRID BUILDERS
    // ══════════════════════════════════════════════════════
    private VBox buildSection(String title, String[][] items) {
        VBox section = new VBox(12);
        section.getChildren().add(buildDivider(title));

        double wrapLen = 4 * CARD_W + 3 * CARD_GAP + 2;
        FlowPane grid  = new FlowPane(CARD_GAP, CARD_GAP);
        grid.setPrefWrapLength(wrapLen);

        for (String[] item : items)
            grid.getChildren().add(buildMenuCard(item[0], item[1], item[2], Boolean.parseBoolean(item[3])));

        section.getChildren().add(grid);
        return section;
    }

    private HBox buildDivider(String title) {
        javafx.scene.shape.Line left = new javafx.scene.shape.Line(0, 0, 30, 0);
        left.setStroke(javafx.scene.paint.Color.web(ACCENT));
        left.setStrokeWidth(1.5);
        left.setOpacity(0.5);

        Label lbl = new Label("  " + title + "  ");
        lbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + ACCENT + ";" +
            "-fx-opacity: 0.8;"
        );

        javafx.scene.shape.Line right = new javafx.scene.shape.Line(0, 0, 30, 0);
        right.setStroke(javafx.scene.paint.Color.web(ACCENT));
        right.setStrokeWidth(1.5);
        right.setOpacity(0.5);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(6, left, lbl, right, spacer);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 2, 0));
        return row;
    }

    private VBox buildMenuCard(String name, String priceS, String priceL, boolean hasCupSize) {
        VBox card = new VBox(0);
        card.setPrefWidth(CARD_W);
        card.setPrefHeight(CARD_H);
        card.setStyle(CARD_STYLE_NORMAL);
        card.setCursor(javafx.scene.Cursor.HAND);

        Region imgPlaceholder = new Region();
        imgPlaceholder.setPrefWidth(CARD_W);
        imgPlaceholder.setMinHeight(CARD_IMG_H);
        imgPlaceholder.setPrefHeight(CARD_IMG_H);
        imgPlaceholder.setMaxHeight(CARD_IMG_H);
        imgPlaceholder.setStyle(
            "-fx-background-color: #E8D5D8;" +
            "-fx-background-radius: 14 14 0 0;"
        );

        FontIcon camIcon = new FontIcon(FontAwesomeSolid.CAMERA);
        camIcon.setIconSize(28);
        camIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));
        camIcon.setOpacity(0.4);

        StackPane imgArea = new StackPane(imgPlaceholder, camIcon);
        imgArea.setPrefWidth(CARD_W);
        imgArea.setMinHeight(CARD_IMG_H);
        imgArea.setPrefHeight(CARD_IMG_H);
        imgArea.setMaxHeight(CARD_IMG_H);

        VBox info = new VBox(4);
        info.setPadding(new Insets(10, 12, 6, 12));

        Label nameLbl = new Label(name);
        nameLbl.setWrapText(true);
        nameLbl.setMaxWidth(CARD_W - 24);
        nameLbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: 600;" +
            "-fx-text-fill: #333333;"
        );

        final String[] selectedPrice = { priceS };

        Label priceLbl = new Label(priceS);
        priceLbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + ACCENT + ";"
        );

        if (hasCupSize) {
            Label sizeHdr = new Label("Cup Size");
            sizeHdr.setStyle(
                "-fx-font-family: '" + FONT_FAMILY + "';" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #999999;"
            );
            Label sBtn = new Label("S");
            sBtn.setStyle(sizeBadgeStyle(true));
            sBtn.setCursor(javafx.scene.Cursor.HAND);

            Label lBtn = new Label("L");
            lBtn.setStyle(sizeBadgeStyle(false));
            lBtn.setCursor(javafx.scene.Cursor.HAND);

            sBtn.setOnMouseClicked(e -> { sBtn.setStyle(sizeBadgeStyle(true));  lBtn.setStyle(sizeBadgeStyle(false)); priceLbl.setText(priceS); selectedPrice[0] = priceS; });
            lBtn.setOnMouseClicked(e -> { lBtn.setStyle(sizeBadgeStyle(true));  sBtn.setStyle(sizeBadgeStyle(false)); priceLbl.setText(priceL); selectedPrice[0] = priceL; });

            HBox sizeRow = new HBox(8, sBtn, lBtn);
            sizeRow.setAlignment(Pos.CENTER_LEFT);
            sizeRow.setPadding(new Insets(2, 0, 2, 0));
            info.getChildren().addAll(nameLbl, sizeHdr, sizeRow, priceLbl);
        } else {
            info.getChildren().addAll(nameLbl, priceLbl);
        }

        FontIcon cartIcon = new FontIcon(FontAwesomeSolid.SHOPPING_CART);
        cartIcon.setIconSize(14);
        cartIcon.setIconColor(javafx.scene.paint.Color.WHITE);

        Button addBtn = new Button("Add to Cart");
        addBtn.setGraphic(cartIcon);
        addBtn.setGraphicTextGap(8);
        addBtn.setPrefWidth(CARD_W - 24);
        addBtn.setStyle(addBtnStyle(false));
        addBtn.setOnMouseEntered(e -> addBtn.setStyle(addBtnStyle(true)));
        addBtn.setOnMouseExited(e  -> addBtn.setStyle(addBtnStyle(false)));
        addBtn.setOnMouseClicked(e -> addToOrder(name, selectedPrice[0]));

        VBox.setMargin(addBtn, new Insets(4, 12, 10, 12));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(imgArea, info, spacer, addBtn);
        card.setOnMouseEntered(e -> card.setStyle(CARD_STYLE_HOVER));
        card.setOnMouseExited(e  -> card.setStyle(CARD_STYLE_NORMAL));
        return card;
    }

    // ══════════════════════════════════════════════════════
    //  STYLE HELPERS
    // ══════════════════════════════════════════════════════
    private String inputStyle(boolean error) {
        String borderColor = error ? "#C0392B" : ACCENT;
        return "-fx-background-color: rgba(255,255,255,0.94);" +
               "-fx-background-radius: 22;" +
               "-fx-border-color: " + borderColor + ";" +
               "-fx-border-radius: 22;" +
               "-fx-border-width: " + (error ? "2.5" : "2") + ";" +
               "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 15px;" +
               "-fx-font-weight: bold;" +
               "-fx-text-fill: #222222;" +
               "-fx-prompt-text-fill: #BBBBBB;" +
               "-fx-padding: 10 16 10 16;";
    }

    private String comboStyle(boolean error) {
        String borderColor = error ? "#C0392B" : ACCENT;
        return "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 14px;" +
               "-fx-font-weight: bold;" +
               "-fx-background-color: rgba(255,255,255,0.94);" +
               "-fx-border-color: " + borderColor + ";" +
               "-fx-border-radius: 10;" +
               "-fx-background-radius: 10;" +
               "-fx-border-width: " + (error ? "2.5" : "2") + ";" +
               "-fx-cursor: hand;";
    }

    private String pillStyle(boolean hovered, boolean active) {
        String bg = active ? ACCENT : (hovered ? "#F5E8EA" : "transparent");
        String fg = active ? "white" : ACCENT;
        return "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 15px;" +
               "-fx-font-weight: 600;" +
               "-fx-text-fill: " + fg + ";" +
               "-fx-background-color: " + bg + ";" +
               "-fx-background-radius: 25;" +
               "-fx-border-color: " + ACCENT + ";" +
               "-fx-border-width: 1.5;" +
               "-fx-border-radius: 25;" +
               "-fx-padding: 8 20 8 20;" +
               "-fx-cursor: hand;";
    }
    
    private String sizeBadgeStyle(boolean active) {
        return "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 13px;" +
               "-fx-font-weight: bold;" +
               "-fx-text-fill: " + (active ? "white" : "#888888") + ";" +
               "-fx-background-color: " + (active ? ACCENT : "#EFEFEF") + ";" +
               "-fx-background-radius: 30;" +
               "-fx-padding: 5 16 5 16;" +
               "-fx-cursor: hand;";
    }

    private String addBtnStyle(boolean hovered) {
        return "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 13px;" +
               "-fx-font-weight: bold;" +
               "-fx-text-fill: white;" +
               "-fx-background-color: " + (hovered ? ACCENT_DARK : ACCENT) + ";" +
               "-fx-background-radius: 22;" +
               "-fx-cursor: hand;" +
               "-fx-padding: 8 10 8 10;";
    }

    private Button buildActionBtn(String text, FontAwesomeSolid iconType, boolean isSubmit) {
        FontIcon icon = new FontIcon(iconType);
        icon.setIconSize(16);
        icon.setIconColor(javafx.scene.paint.Color.WHITE);

        Button btn = new Button(text);
        btn.setGraphic(icon);
        btn.setGraphicTextGap(8);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(actionBtnStyle(false, isSubmit));
        btn.setOnMouseEntered(e -> btn.setStyle(actionBtnStyle(true,  isSubmit)));
        btn.setOnMouseExited(e  -> btn.setStyle(actionBtnStyle(false, isSubmit)));
        return btn;
    }

    private String actionBtnStyle(boolean hovered, boolean isSubmit) {
        String bg = isSubmit
            ? (hovered ? GREEN_DARK : GREEN)
            : (hovered ? ACCENT_DARK : ACCENT);
        return "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 15px;" +
               "-fx-font-weight: bold;" +
               "-fx-text-fill: white;" +
               "-fx-background-color: " + bg + ";" +
               "-fx-background-radius: 22;" +
               "-fx-cursor: hand;" +
               "-fx-padding: 11 14 11 14;";
    }

    private String qtyBtnStyle() {
        return "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 14px;" +
               "-fx-font-weight: bold;" +
               "-fx-text-fill: white;" +
               "-fx-background-color: " + ACCENT + ";" +
               "-fx-background-radius: 6;" +
               "-fx-padding: 2 9 3 9;" +
               "-fx-cursor: hand;";
    }

    private String changeLabelStyle(double change) {
        String color = change >= 0 ? "#155724" : "#721C24";
        return "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 17px;" +
               "-fx-font-weight: 800;" +
               "-fx-text-fill: " + color + ";" +
               "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.9), 4, 0, 0, 1);";
    }

    private String modalOkBtnStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? ACCENT_DARK : ACCENT) + ";" +
               "-fx-background-radius: 22;" +
               "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 14px;" +
               "-fx-font-weight: bold;" +
               "-fx-text-fill: white;" +
               "-fx-cursor: hand;" +
               "-fx-padding: 10 20 10 20;";
    }
}