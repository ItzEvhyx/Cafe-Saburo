package frontend;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class pos_records_window_ui {

    // ══════════════════════════════════════════════════════
    //  CARD 1 CONFIGURATION
    // ══════════════════════════════════════════════════════
    private static final double CARD_W = 250;
    private static final double CARD_H = 240;
    private static final double CARD_X = 10;
    private static final double CARD_Y = 110;

    // ══════════════════════════════════════════════════════
    //  CARD 2 CONFIGURATION — adjust height and y freely
    // ══════════════════════════════════════════════════════
    private static final double CARD2_H = 480;
    private static final double CARD2_Y = 370;

    // ══════════════════════════════════════════════════════
    //  CONTENT PANE CONFIGURATION — adjust freely
    // ══════════════════════════════════════════════════════
    private static final double CONTENT_X = 275;
    private static final double CONTENT_Y = 10;
    private static final double CONTENT_H = 840;

    private static final String ACCENT     = "#882F39";
    private static final String HOVER_BG   = "#F5E8EA";
    private static final String DEFAULT_BG = "transparent";

    // ── Aleo font family name ─────────────────────────────
    private static final String FONT_FAMILY = "Aleo";

    private final StackPane contentPane = new StackPane();

    // ── Pre-load every Aleo variant once ─────────────────
    private static boolean fontsLoaded = false;
    private static void loadFonts() {
        if (fontsLoaded) return;
        String[] variants = {
            "Aleo-Black", "Aleo-BlackItalic",
            "Aleo-Bold", "Aleo-BoldItalic",
            "Aleo-ExtraBold", "Aleo-ExtraBoldItalic",
            "Aleo-ExtraLight", "Aleo-ExtraLightItalic",
            "Aleo-Italic",
            "Aleo-Light", "Aleo-LightItalic",
            "Aleo-Medium", "Aleo-MediumItalic",
            "Aleo-Regular",
            "Aleo-SemiBold", "Aleo-SemiBoldItalic",
            "Aleo-Thin", "Aleo-ThinItalic"
        };
        for (String v : variants) {
            Font.loadFont("file:assets/fonts/" + v + ".ttf", 12);
        }
        fontsLoaded = true;
    }

    public void start(Stage stage) {
        loadFonts();

        double screenW = Screen.getPrimary().getBounds().getWidth();
        double screenH = Screen.getPrimary().getBounds().getHeight();

        ImageView background = new ImageView(new Image("file:assets/cafe_bg.png"));
        background.setFitWidth(screenW);
        background.setFitHeight(screenH);
        background.setPreserveRatio(false);

        // ── Card 1: Nav buttons ───────────────────────────
        // Aleo Bold, size 23 — primary nav labels
        HBox menuBtn      = createNavButton("Menu",      "assets/icons/menu_icon.png",      46, 23);
        HBox ordersBtn    = createNavButton("Orders",    "assets/icons/orders_icon.png",    46, 23);
        HBox customersBtn = createNavButton("Customers", "assets/icons/customers_icon.png", 46, 23);

        VBox card1 = new VBox(8);
        card1.setAlignment(Pos.CENTER_LEFT);
        card1.setPadding(new Insets(10, 6, 10, 6));
        card1.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: #882F39;" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 18;"
        );
        card1.setPrefWidth(CARD_W);
        card1.setPrefHeight(CARD_H);
        card1.setLayoutX(CARD_X);
        card1.setLayoutY(CARD_Y);

        card1.getChildren().addAll(menuBtn, ordersBtn, customersBtn);

        // ── "Manager only" divider label ─────────────────
        // Aleo Light Italic — subtle section divider
        Label managerLabel = new Label("— Manager only —");
        managerLabel.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 12px;" +
            "-fx-font-style: italic;" +
            "-fx-text-fill: " + ACCENT + ";" +
            "-fx-opacity: 0.75;"
        );
        managerLabel.setPrefWidth(CARD_W);
        managerLabel.setAlignment(Pos.CENTER);
        VBox.setMargin(managerLabel, new Insets(-4, 0, -4, 0)); // pull closer to buttons above and below

        // ── Card 2: Secondary nav buttons ─────────────────
        // Aleo Bold, size 20 — secondary nav labels
        HBox paymentsBtn   = createNavButton("Payments",   "assets/icons/payments_icon.png",   38, 20);
        HBox timeLogsBtn   = createNavButton("Time Logs",  "assets/icons/timelogs_icon.png",   38, 20);
        HBox employeesBtn  = createNavButton("Employees",  "assets/icons/employees_icon.png",  38, 20);
        HBox inventoryBtn  = createNavButton("Inventory",  "assets/icons/inventory_icon.png",  38, 20);
        HBox suppliersBtn  = createNavButton("Suppliers",  "assets/icons/suppliers_icon.png",  38, 20);
        HBox purchasesBtn  = createNavButton("Purchases",  "assets/icons/purchases_icon.png",  38, 20);
        HBox promotionsBtn = createNavButton("Promotions", "assets/icons/promotions_icon.png", 38, 20);

        VBox card2 = new VBox(7); // spacing: 7 (was 14) — tighter gaps between buttons
        card2.setAlignment(Pos.CENTER_LEFT);
        card2.setPadding(new Insets(10, 6, 10, 6));
        card2.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: #882F39;" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 18;"
        );
        card2.setPrefWidth(CARD_W);
        card2.setPrefHeight(CARD2_H);
        card2.setLayoutX(CARD_X);
        card2.setLayoutY(CARD2_Y);

        card2.getChildren().addAll(
            managerLabel,
            paymentsBtn, timeLogsBtn, employeesBtn,
            inventoryBtn, suppliersBtn, purchasesBtn, promotionsBtn
        );

        // ── Content Pane (swappable area) ─────────────────
        contentPane.setAlignment(Pos.CENTER);
        contentPane.setPrefWidth(screenW - CONTENT_X - 10);
        contentPane.setPrefHeight(CONTENT_H);
        contentPane.setLayoutX(CONTENT_X);
        contentPane.setLayoutY(CONTENT_Y);
        contentPane.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: #882F39;" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 18;"
        );

        // ── Default to Menu tab on launch ─────────────────
        switchContent("Menu", screenW);

        // ── Wire up nav buttons ───────────────────────────
        menuBtn.setOnMouseClicked(e       -> switchContent("Menu",       screenW));
        ordersBtn.setOnMouseClicked(e     -> switchContent("Orders",     screenW));
        customersBtn.setOnMouseClicked(e  -> switchContent("Customers",  screenW));
        paymentsBtn.setOnMouseClicked(e   -> switchContent("Payments",   screenW));
        timeLogsBtn.setOnMouseClicked(e   -> switchContent("Time Logs",  screenW));
        employeesBtn.setOnMouseClicked(e  -> switchContent("Employees",  screenW));
        inventoryBtn.setOnMouseClicked(e  -> switchContent("Inventory",  screenW));
        suppliersBtn.setOnMouseClicked(e  -> switchContent("Suppliers",  screenW));
        purchasesBtn.setOnMouseClicked(e  -> switchContent("Purchases",  screenW));
        promotionsBtn.setOnMouseClicked(e -> switchContent("Promotions", screenW));

        Pane root = new Pane();
        root.getChildren().addAll(background, card1, card2, contentPane);

        Scene scene = new Scene(root, screenW, screenH);

        stage.setTitle("Cafe Saburo – POS & Records");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.show();
    }

    // ── Swaps the content pane ────────────────────────────
    private void switchContent(String tab, double screenW) {
        contentPane.getChildren().clear();

        switch (tab) {
            case "Menu" -> {
                menu_contents menuContents = new menu_contents(screenW - CONTENT_X - 10, CONTENT_H);
                contentPane.getChildren().add(menuContents.getView());
            }
            // ── TODO: hook up remaining tabs when ready ───
            // case "Orders"     -> contentPane.getChildren().add(new orders_contents(...).getView());
            // case "Customers"  -> contentPane.getChildren().add(new customers_contents(...).getView());
            // case "Payments"   -> contentPane.getChildren().add(new payments_contents(...).getView());
            // case "Time Logs"  -> contentPane.getChildren().add(new timelogs_contents(...).getView());
            // case "Employees"  -> contentPane.getChildren().add(new employees_contents(...).getView());
            // case "Inventory"  -> contentPane.getChildren().add(new inventory_contents(...).getView());
            // case "Suppliers"  -> contentPane.getChildren().add(new suppliers_contents(...).getView());
            // case "Purchases"  -> contentPane.getChildren().add(new purchases_contents(...).getView());
            // case "Promotions" -> contentPane.getChildren().add(new promotions_contents(...).getView());
            default -> {
                // Aleo Medium for placeholder text — readable but not too heavy
                Label placeholder = new Label(tab + " section — to be made soon!");
                placeholder.setStyle(
                    "-fx-font-family: '" + FONT_FAMILY + "';" +
                    "-fx-font-size: 22px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-text-fill: #882F39;" +
                    "-fx-opacity: 0.5;"
                );
                contentPane.getChildren().add(placeholder);
            }
        }
    }

    // ── Helper: build a single nav button row ─────────────
    private HBox createNavButton(String label, String iconPath, double iconSize, double fontSize) {
        ImageView icon = new ImageView(new Image("file:" + iconPath));
        icon.setFitWidth(iconSize);
        icon.setFitHeight(iconSize);
        icon.setPreserveRatio(true);

        // Nav labels — Aleo Bold for clear readability
        Label text = new Label(label);
        text.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: " + fontSize + "px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + ACCENT + ";"
        );

        HBox row = new HBox(12, icon, text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 6, 6, 30));
        row.setPrefWidth(CARD_W - 12);
        row.setStyle(
            "-fx-background-color: " + DEFAULT_BG + ";" +
            "-fx-background-radius: 10;"
        );
        row.setCursor(Cursor.HAND);

        row.setOnMouseEntered(e -> row.setStyle(
            "-fx-background-color: " + HOVER_BG + ";" +
            "-fx-background-radius: 10;"
        ));
        row.setOnMouseExited(e -> row.setStyle(
            "-fx-background-color: " + DEFAULT_BG + ";" +
            "-fx-background-radius: 10;"
        ));

        return row;
    }
}