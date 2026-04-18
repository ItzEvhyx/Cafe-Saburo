package frontend;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.ArrayList;
import java.util.List;

public class menu_contents {

    // ══════════════════════════════════════════════════════
    //  SIDE PANEL CONFIGURATION
    // ══════════════════════════════════════════════════════
    private static final double SIDE_PANEL_W = 250;

    // ══════════════════════════════════════════════════════
    //  HEADER / FILTER BAR CONFIGURATION
    // ══════════════════════════════════════════════════════
    private static final double HEADER_H     = 60;
    private static final double FILTER_BAR_H = 56;
    private static final double TOP_PADDING  = 16;
    private static final double SIDE_PADDING = 20;

    // ══════════════════════════════════════════════════════
    //  CARD CONFIGURATION — bigger cards, 4 per row
    // ══════════════════════════════════════════════════════
    private static final double CARD_W     = 210;
    private static final double CARD_H     = 280;
    private static final double CARD_IMG_H = 130;
    private static final double CARD_GAP   = 16;

    private static final String ACCENT      = "#882F39";
    private static final String ACCENT_DARK = "#6B1E26";
    private static final String FONT_FAMILY = "Aleo";

    // No border — drop-shadow lift effect only
    private static final String CARD_STYLE_NORMAL =
        "-fx-background-color: white;" +
        "-fx-background-radius: 16;" +
        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 14, 0, 0, 4);";

    private static final String CARD_STYLE_HOVER =
        "-fx-background-color: white;" +
        "-fx-background-radius: 16;" +
        "-fx-effect: dropshadow(gaussian, rgba(136,47,57,0.22), 24, 0, 0, 8);" +
        "-fx-translate-y: -3;";

    private final double totalW;
    private final double totalH;

    // ══════════════════════════════════════════════════════
    //  MENU DATA — { name, priceSmall, priceLarge, hasCupSize }
    //  Espresso: priceSmall = S price, priceLarge = L price
    //  All others: priceSmall = display price, priceLarge = ""
    // ══════════════════════════════════════════════════════
    private static final String[][] ESPRESSO = {
        { "Americano",  "₱100", "₱105", "true"  },
        { "Cafe Latte", "₱120", "₱125", "true"  },
        { "Cafe Mocha", "₱140", "₱140", "false" },
        { "Cappuccino", "₱120", "₱125", "true"  },
    };

    private static final String[][] SPECIALTY = {
        { "Banoffee ★",              "₱190", "", "false" },
        { "Biscoff Cream Latte ★",   "₱195", "", "false" },
        { "Biscoff Cold Foam Latte", "₱195", "", "false" },
        { "Creme Brulee Latte",      "₱170", "", "false" },
        { "Lavender Latte",          "₱170", "", "false" },
        { "Pistachio Latte ★",       "₱190", "", "false" },
        { "Pumpkin Spice Latte",     "₱170", "", "false" },
        { "Sakura Cloud Latte",      "₱170", "", "false" },
        { "Smores Latte",            "₱170", "", "false" },
        { "Tiramisu Latte",          "₱170", "", "false" },
        { "Signature Drink ★",       "₱200", "", "false" },
    };

    private static final String[][] ICED_COFFEE = {
        { "Butterscotch Latte ★", "₱150", "", "false" },
        { "Caramel Latte",        "₱150", "", "false" },
        { "Caramel Macchiato",    "₱150", "", "false" },
        { "Hazelnut Latte",       "₱150", "", "false" },
        { "Irish Cream Latte ★",  "₱150", "", "false" },
        { "Mocha Latte",          "₱150", "", "false" },
        { "Spanish Latte ★",      "₱150", "", "false" },
        { "Vanilla Latte",        "₱150", "", "false" },
        { "White Mocha Latte",    "₱150", "", "false" },
    };

    private static final String[][] FRAPPE = {
        { "Coffee Caramel", "₱160", "", "false" },
        { "Dark Mocha",     "₱160", "", "false" },
        { "Pecan Praline",  "₱160", "", "false" },
        { "White Mocha",    "₱160", "", "false" },
    };

    private static final String[][] MATCHA = {
        { "Agave Matcha Latte ★",     "₱180", "", "false" },
        { "Banana Matcha ★",          "₱190", "", "false" },
        { "Blueberry Matcha",         "₱190", "", "false" },
        { "Dirty Matcha",             "₱190", "", "false" },
        { "Ichigo Matcha Latte",      "₱170", "", "false" },
        { "Lavender Matcha Latte",    "₱170", "", "false" },
        { "Mango Matcha",             "₱190", "", "false" },
        { "Matcha Latte ★",           "₱170", "", "false" },
        { "Matcha Pistachio Latte ★", "₱200", "", "false" },
        { "Strawberry Matcha Latte",  "₱190", "", "false" },
        { "Oreo Matcha",              "₱190", "", "false" },
        { "Premium Hojicha",          "₱180", "", "false" },
        { "Kinako Hojicha ★",         "₱200", "", "false" },
    };

    private static final String[][] SMOOTHIE = {
        { "Biscoff ★",             "₱190", "", "false" },
        { "Blueberry Cheesecake",  "₱170", "", "false" },
        { "Matcha",                "₱180", "", "false" },
        { "Oreo Frappuccino",      "₱170", "", "false" },
        { "Strawberry",            "₱160", "", "false" },
        { "Strawberry Cheesecake", "₱170", "", "false" },
    };

    private static final String[][] REFRESHER = {
        { "Four Red Fruits Tea ★", "₱125", "", "false" },
        { "Kiwi Green Apple Tea",  "₱125", "", "false" },
        { "Passion Fruit Tea",     "₱125", "", "false" },
        { "Pomegranate Lemon Tea", "₱125", "", "false" },
        { "Wild Berry Tea",        "₱125", "", "false" },
    };

    private static final String[][] ADD_ONS = {
        { "Espresso Shot", "₱30", "", "false" },
        { "Sub Oat",       "₱30", "", "false" },
    };

    private static final String[] CATEGORY_NAMES = {
        "All", "Espresso", "Specialty Coffee", "Iced Coffee",
        "Frappe", "Matcha Series", "Smoothies", "Refreshers", "Add-ons"
    };

    private static final String[][][] CATEGORY_DATA = {
        null, ESPRESSO, SPECIALTY, ICED_COFFEE,
        FRAPPE, MATCHA, SMOOTHIE, REFRESHER, ADD_ONS
    };

    private static final String[] SECTION_TITLES = {
        null, "Espresso", "Specialty Coffee", "Iced Coffee",
        "Frappe", "Matcha Series", "Smoothie", "Refresher", "Add-ons"
    };

    public menu_contents(double totalW, double totalH) {
        this.totalW = totalW;
        this.totalH = totalH;
        loadFonts();
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
    //  MAIN VIEW
    // ══════════════════════════════════════════════════════
    public Pane getView() {
        Pane root = new Pane();
        root.setPrefWidth(totalW);
        root.setPrefHeight(totalH);

        double mainW = totalW - SIDE_PANEL_W;

        Pane mainArea = new Pane();
        mainArea.setPrefWidth(mainW);
        mainArea.setPrefHeight(totalH);
        mainArea.setLayoutX(0);
        mainArea.setLayoutY(0);

        // ── Title + Search ────────────────────────────────
        double rowY = TOP_PADDING;
        double rowW = mainW - SIDE_PADDING * 2;

        Label titleLabel = new Label("Menu Items");
        titleLabel.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 36px;" +
            "-fx-font-weight: extra-bold;" +
            "-fx-text-fill: " + ACCENT + ";"
        );
        titleLabel.setLayoutX(SIDE_PADDING);
        titleLabel.setLayoutY(rowY);
        titleLabel.setPrefHeight(HEADER_H);

        double searchW = 230;
        double searchH = 38;

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
        searchBar.setLayoutY(rowY + (HEADER_H - searchH) / 2.0);
        searchBar.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: " + ACCENT + ";" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 20;"
        );

        // ── Filter pills ──────────────────────────────────
        double filterY = rowY + HEADER_H + 4;

        // Pre-build all section nodes
        List<VBox> sectionNodes = new ArrayList<>();
        for (int i = 1; i < CATEGORY_NAMES.length; i++) {
            sectionNodes.add(buildSection(SECTION_TITLES[i], CATEGORY_DATA[i], mainW));
        }

        VBox allSections = new VBox(28);
        allSections.setPadding(new Insets(4, SIDE_PADDING, 20, SIDE_PADDING));
        allSections.getChildren().addAll(sectionNodes);

        double gridY = filterY + FILTER_BAR_H + 8;
        double gridH = totalH - gridY - 10;

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
        gridScroll.setPrefHeight(gridH);
        gridScroll.setLayoutX(SIDE_PADDING / 2);
        gridScroll.setLayoutY(gridY);

        // Build pill row with active-state tracking
        HBox pillRow = new HBox(12);
        pillRow.setAlignment(Pos.CENTER_LEFT);
        pillRow.setPadding(new Insets(6, 12, 6, 0));

        final int[] activePillIndex = { 0 };
        List<Label> pills = new ArrayList<>();

        for (int i = 0; i < CATEGORY_NAMES.length; i++) {
            final int idx = i;
            Label pill = new Label(CATEGORY_NAMES[i]);
            pill.setStyle(i == 0 ? buildPillStyle(false, true) : buildPillStyle(false, false));
            pills.add(pill);

            pill.setOnMouseEntered(e -> {
                if (activePillIndex[0] != idx)
                    pill.setStyle(buildPillStyle(true, false));
            });
            pill.setOnMouseExited(e -> {
                if (activePillIndex[0] != idx)
                    pill.setStyle(buildPillStyle(false, false));
            });
            pill.setOnMouseClicked(e -> {
                pills.get(activePillIndex[0]).setStyle(buildPillStyle(false, false));
                activePillIndex[0] = idx;
                pill.setStyle(buildPillStyle(false, true));
                allSections.getChildren().clear();
                gridScroll.setVvalue(0);
                if (idx == 0) {
                    allSections.getChildren().addAll(sectionNodes);
                } else {
                    allSections.getChildren().add(sectionNodes.get(idx - 1));
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
        filterScroll.setPrefWidth(rowW);
        filterScroll.setPrefHeight(FILTER_BAR_H);
        filterScroll.setLayoutX(SIDE_PADDING);
        filterScroll.setLayoutY(filterY);

        mainArea.getChildren().addAll(titleLabel, searchBar, filterScroll, gridScroll);

        // ── Side panel ────────────────────────────────────
        StackPane sidePanel = new StackPane();
        sidePanel.setPrefWidth(SIDE_PANEL_W);
        sidePanel.setPrefHeight(totalH);
        sidePanel.setLayoutX(totalW - SIDE_PANEL_W);
        sidePanel.setLayoutY(0);
        sidePanel.setStyle(
            "-fx-background-radius: 0 18 18 0;" +
            "-fx-border-color: #882F39;" +
            "-fx-border-width: 0 0 0 1.5;" +
            "-fx-border-radius: 0 18 18 0;"
        );

        ImageView panelBg = new ImageView(new Image("file:assets/cafe_bg_wpanel.png"));
        panelBg.setFitWidth(SIDE_PANEL_W);
        panelBg.setFitHeight(totalH);
        panelBg.setPreserveRatio(false);
        sidePanel.getChildren().add(panelBg);

        root.getChildren().addAll(mainArea, sidePanel);
        sidePanel.toFront();
        return root;
    }

    // ══════════════════════════════════════════════════════
    //  BUILD SECTION
    // ══════════════════════════════════════════════════════
    private VBox buildSection(String title, String[][] items, double mainW) {
        VBox section = new VBox(12);
        section.getChildren().add(buildDivider(title, mainW));

        // 4 cards per row — calculate exact wrap length
        double wrapLen = 4 * CARD_W + 3 * CARD_GAP + 2;

        FlowPane grid = new FlowPane(CARD_GAP, CARD_GAP);
        grid.setPrefWrapLength(wrapLen);

        for (String[] item : items) {
            String  name       = item[0];
            String  priceS     = item[1];
            String  priceL     = item[2];
            boolean hasCupSize = Boolean.parseBoolean(item[3]);
            grid.getChildren().add(buildItemCard(name, priceS, priceL, hasCupSize));
        }

        section.getChildren().add(grid);
        return section;
    }

    // ══════════════════════════════════════════════════════
    //  DIVIDER HEADER
    // ══════════════════════════════════════════════════════
    private HBox buildDivider(String title, double mainW) {
        javafx.scene.shape.Line leftLine = new javafx.scene.shape.Line(0, 0, 30, 0);
        leftLine.setStroke(javafx.scene.paint.Color.web(ACCENT));
        leftLine.setStrokeWidth(1.5);
        leftLine.setOpacity(0.5);

        Label lbl = new Label("  " + title + "  ");
        lbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + ACCENT + ";" +
            "-fx-opacity: 0.8;"
        );

        javafx.scene.shape.Line rightLine = new javafx.scene.shape.Line(0, 0, 30, 0);
        rightLine.setStroke(javafx.scene.paint.Color.web(ACCENT));
        rightLine.setStrokeWidth(1.5);
        rightLine.setOpacity(0.5);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox row = new HBox(6, leftLine, lbl, rightLine, spacer);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 2, 0));
        return row;
    }

    // ══════════════════════════════════════════════════════
    //  ITEM CARD
    //  priceS = small/only price | priceL = large price (espresso only)
    // ══════════════════════════════════════════════════════
    private VBox buildItemCard(String name, String priceS, String priceL, boolean hasCupSize) {
        VBox card = new VBox(0);
        card.setPrefWidth(CARD_W);
        card.setPrefHeight(CARD_H);
        card.setStyle(CARD_STYLE_NORMAL);
        card.setCursor(javafx.scene.Cursor.HAND);

        // ── Image placeholder ─────────────────────────────
        javafx.scene.layout.Region imgPlaceholder = new javafx.scene.layout.Region();
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

        // ── Info area ─────────────────────────────────────
        VBox info = new VBox(4);
        info.setPadding(new Insets(10, 12, 6, 12));

        Label nameLabel = new Label(name);
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(CARD_W - 24);
        nameLabel.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: semi-bold;" +
            "-fx-text-fill: #333333;"
        );

        // Price label — dynamically updated when S/L toggled
        Label priceLabel = new Label(priceS);
        priceLabel.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + ACCENT + ";"
        );

        if (hasCupSize) {
            // Cup size label
            Label sizeHeader = new Label("Cup Size");
            sizeHeader.setStyle(
                "-fx-font-family: '" + FONT_FAMILY + "';" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #999999;"
            );

            // S button — active by default
            Label sBtn = new Label("S");
            sBtn.setStyle(buildSizeBadgeStyle(true));
            sBtn.setCursor(javafx.scene.Cursor.HAND);

            // L button
            Label lBtn = new Label("L");
            lBtn.setStyle(buildSizeBadgeStyle(false));
            lBtn.setCursor(javafx.scene.Cursor.HAND);

            sBtn.setOnMouseClicked(e -> {
                sBtn.setStyle(buildSizeBadgeStyle(true));
                lBtn.setStyle(buildSizeBadgeStyle(false));
                priceLabel.setText(priceS);
            });
            lBtn.setOnMouseClicked(e -> {
                lBtn.setStyle(buildSizeBadgeStyle(true));
                sBtn.setStyle(buildSizeBadgeStyle(false));
                priceLabel.setText(priceL);
            });

            HBox sizeRow = new HBox(8, sBtn, lBtn);
            sizeRow.setAlignment(Pos.CENTER_LEFT);
            sizeRow.setPadding(new Insets(2, 0, 2, 0));

            info.getChildren().addAll(nameLabel, sizeHeader, sizeRow, priceLabel);
        } else {
            info.getChildren().addAll(nameLabel, priceLabel);
        }

        // ── Add to Cart button ────────────────────────────
        FontIcon cartIcon = new FontIcon(FontAwesomeSolid.SHOPPING_CART);
        cartIcon.setIconSize(14);
        cartIcon.setIconColor(javafx.scene.paint.Color.WHITE);

        Button addBtn = new Button("Add to Cart");
        addBtn.setGraphic(cartIcon);
        addBtn.setGraphicTextGap(8);
        addBtn.setPrefWidth(CARD_W - 24);
        addBtn.setStyle(buildAddBtnStyle(false));
        addBtn.setOnMouseEntered(e -> addBtn.setStyle(buildAddBtnStyle(true)));
        addBtn.setOnMouseExited(e  -> addBtn.setStyle(buildAddBtnStyle(false)));

        VBox.setMargin(addBtn, new Insets(4, 12, 10, 12));

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        card.getChildren().addAll(imgArea, info, spacer, addBtn);

        // Lift on hover — no border, just shadow + translateY
        card.setOnMouseEntered(e -> card.setStyle(CARD_STYLE_HOVER));
        card.setOnMouseExited(e  -> card.setStyle(CARD_STYLE_NORMAL));

        return card;
    }

    // ══════════════════════════════════════════════════════
    //  STYLE HELPERS
    // ══════════════════════════════════════════════════════

    /** Size badge: active = filled maroon, inactive = light grey pill */
    private String buildSizeBadgeStyle(boolean active) {
        return active
            ? "-fx-font-family: '" + FONT_FAMILY + "';" +
              "-fx-font-size: 13px;" +
              "-fx-font-weight: bold;" +
              "-fx-text-fill: white;" +
              "-fx-background-color: " + ACCENT + ";" +
              "-fx-background-radius: 30;" +
              "-fx-padding: 5 16 5 16;" +
              "-fx-cursor: hand;"
            : "-fx-font-family: '" + FONT_FAMILY + "';" +
              "-fx-font-size: 13px;" +
              "-fx-font-weight: bold;" +
              "-fx-text-fill: #888888;" +
              "-fx-background-color: #EFEFEF;" +
              "-fx-background-radius: 30;" +
              "-fx-padding: 5 16 5 16;" +
              "-fx-cursor: hand;";
    }

    /** Add to Cart button style */
    private String buildAddBtnStyle(boolean hovered) {
        return "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 13px;" +
               "-fx-font-weight: bold;" +
               "-fx-text-fill: white;" +
               "-fx-background-color: " + (hovered ? ACCENT_DARK : ACCENT) + ";" +
               "-fx-background-radius: 22;" +
               "-fx-cursor: hand;" +
               "-fx-padding: 8 10 8 10;";
    }

    /** Filter pill style */
    private String buildPillStyle(boolean hovered, boolean active) {
        String bg = active ? "#882F39" : (hovered ? "#F5E8EA" : "transparent");
        String fg = active ? "white" : ACCENT;
        return "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 15px;" +
               "-fx-font-weight: semi-bold;" +
               "-fx-text-fill: " + fg + ";" +
               "-fx-background-color: " + bg + ";" +
               "-fx-background-radius: 25;" +
               "-fx-border-color: #882F39;" +
               "-fx-border-width: 1.5;" +
               "-fx-border-radius: 25;" +
               "-fx-padding: 8 20 8 20;" +
               "-fx-cursor: hand;";
    }
}