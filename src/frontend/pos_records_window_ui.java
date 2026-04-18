package frontend;

import backend.auth_util;
import backend.menu_util.SubmitResult;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.sql.Connection;

public class pos_records_window_ui {

    // ══════════════════════════════════════════════════════
    //  CARD 1 CONFIGURATION
    // ══════════════════════════════════════════════════════
    private static final double CARD_W = 250;
    private static final double CARD_H = 240;
    private static final double CARD_X = 10;
    private static final double CARD_Y = 110;

    // ══════════════════════════════════════════════════════
    //  CARD 2 CONFIGURATION
    // ══════════════════════════════════════════════════════
    private static final double CARD2_H = 480;
    private static final double CARD2_Y = 370;

    // ══════════════════════════════════════════════════════
    //  CONTENT PANE CONFIGURATION
    // ══════════════════════════════════════════════════════
    private static final double CONTENT_X = 275;
    private static final double CONTENT_Y = 10;
    private static final double CONTENT_H = 840;

    private static final String ACCENT      = "#882F39";
    private static final String HOVER_BG    = "#F5E8EA";
    private static final String DEFAULT_BG  = "transparent";
    private static final String FONT_FAMILY = "Aleo";

    private final StackPane contentPane = new StackPane();
    private final Connection conn;

    private menu_contents      menuContents;
    private orders_contents    ordersContents;
    private customers_contents customersContents;

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
        for (String v : variants) Font.loadFont("file:assets/fonts/" + v + ".ttf", 12);
        fontsLoaded = true;
    }

    public pos_records_window_ui(Connection conn) {
        this.conn = conn;
        if (conn == null) {
            System.err.println("[pos_records_window_ui] WARNING: conn is null! " +
                               "Make sure auth_ui passes Main.conn when constructing this class.");
        }
    }

    public void start(Stage stage) {
        loadFonts();

        // ── Read role once ─────────────────────────────────
        boolean isManager = auth_util.isManager();

        double screenW = Screen.getPrimary().getBounds().getWidth();
        double screenH = Screen.getPrimary().getBounds().getHeight();

        ImageView background = new ImageView(new Image("file:assets/cafe_bg.png"));
        background.setFitWidth(screenW);
        background.setFitHeight(screenH);
        background.setPreserveRatio(false);

        double contentW = screenW - CONTENT_X - 10;

        menuContents      = new menu_contents(contentW, CONTENT_H, conn);
        ordersContents    = new orders_contents(contentW, CONTENT_H, conn);
        customersContents = new customers_contents(contentW, CONTENT_H, conn);

        menuContents.setOnOrderSubmitted(result -> {
            ordersContents.prependOrder(
                result.orderId,
                result.customerId,
                result.paymentMethod
            );
            customersContents.prependOrUpdateCustomer(
                result.customerId,
                result.customerName,
                result.orderId
            );
        });

        // ── Card 1: always accessible ─────────────────────
        HBox menuBtn      = createNavButton("Menu",      "assets/icons/menu_icon.png",      46, 23, true);
        HBox ordersBtn    = createNavButton("Orders",    "assets/icons/orders_icon.png",    46, 23, true);
        HBox customersBtn = createNavButton("Customers", "assets/icons/customers_icon.png", 46, 23, true);

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

        // ── "Manager only" divider label ──────────────────
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
        VBox.setMargin(managerLabel, new Insets(-4, 0, -4, 0));

        // ── Card 2: locked when employee ──────────────────
        HBox paymentsBtn   = createNavButton("Payments",   "assets/icons/payments_icon.png",   38, 20, isManager);
        HBox timeLogsBtn   = createNavButton("Time Logs",  "assets/icons/timelogs_icon.png",   38, 20, isManager);
        HBox employeesBtn  = createNavButton("Employees",  "assets/icons/employees_icon.png",  38, 20, isManager);
        HBox inventoryBtn  = createNavButton("Inventory",  "assets/icons/inventory_icon.png",  38, 20, isManager);
        HBox suppliersBtn  = createNavButton("Suppliers",  "assets/icons/suppliers_icon.png",  38, 20, isManager);
        HBox purchasesBtn  = createNavButton("Purchases",  "assets/icons/purchases_icon.png",  38, 20, isManager);
        HBox promotionsBtn = createNavButton("Promotions", "assets/icons/promotions_icon.png", 38, 20, isManager);

        VBox card2 = new VBox(7);
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

        // ── Content pane ──────────────────────────────────
        contentPane.setAlignment(Pos.CENTER);
        contentPane.setPrefWidth(contentW);
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

        showMenu();

        // ── Card 1 — always wired ─────────────────────────
        menuBtn.setOnMouseClicked(e      -> showMenu());
        ordersBtn.setOnMouseClicked(e    -> showOrders());
        customersBtn.setOnMouseClicked(e -> showCustomers());

        // ── Card 2 — only wired for managers ─────────────
        if (isManager) {
            paymentsBtn.setOnMouseClicked(e   -> switchContent("Payments"));
            timeLogsBtn.setOnMouseClicked(e   -> switchContent("Time Logs"));
            employeesBtn.setOnMouseClicked(e  -> switchContent("Employees"));
            inventoryBtn.setOnMouseClicked(e  -> switchContent("Inventory"));
            suppliersBtn.setOnMouseClicked(e  -> switchContent("Suppliers"));
            purchasesBtn.setOnMouseClicked(e  -> switchContent("Purchases"));
            promotionsBtn.setOnMouseClicked(e -> switchContent("Promotions"));
        }

        Pane root = new Pane();
        root.getChildren().addAll(background, card1, card2, contentPane);

        Scene scene = new Scene(root, screenW, screenH);
        stage.setTitle("Cafe Saburo – POS & Records");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.show();
    }

    // ══════════════════════════════════════════════════════
    //  SHOW HELPERS
    // ══════════════════════════════════════════════════════
    private void showMenu() {
        contentPane.getChildren().setAll(menuContents.getView());
    }

    private void showOrders() {
        contentPane.getChildren().setAll(ordersContents.getView());
    }

    private void showCustomers() {
        contentPane.getChildren().setAll(customersContents.getView());
    }

    private void switchContent(String tab) {
        contentPane.getChildren().setAll(makePlaceholder(tab));
    }

    private Label makePlaceholder(String tab) {
        Label label = new Label(tab + " section — to be made soon!");
        label.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 22px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #882F39;" +
            "-fx-opacity: 0.5;"
        );
        return label;
    }

    // ══════════════════════════════════════════════════════
    //  NAV BUTTON FACTORY
    //
    //  unlocked = true  → normal, hoverable, clickable
    //  unlocked = false → greyed icon + text, lock icon on
    //                     the right, no hover, no cursor hand,
    //                     no click handler (never wired above)
    // ══════════════════════════════════════════════════════
    private HBox createNavButton(String label, String iconPath,
                                  double iconSize, double fontSize,
                                  boolean unlocked) {
        ImageView icon = new ImageView(new Image("file:" + iconPath));
        icon.setFitWidth(iconSize);
        icon.setFitHeight(iconSize);
        icon.setPreserveRatio(true);

        Label text = new Label(label);

        if (unlocked) {
            // ── Active / unlocked row ─────────────────────
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
            row.setStyle("-fx-background-color: " + DEFAULT_BG + "; -fx-background-radius: 10;");
            row.setCursor(Cursor.HAND);

            row.setOnMouseEntered(e -> row.setStyle(
                "-fx-background-color: " + HOVER_BG + "; -fx-background-radius: 10;"
            ));
            row.setOnMouseExited(e -> row.setStyle(
                "-fx-background-color: " + DEFAULT_BG + "; -fx-background-radius: 10;"
            ));
            return row;

        } else {
            // ── Locked row ────────────────────────────────
            icon.setOpacity(0.25);

            text.setStyle(
                "-fx-font-family: '" + FONT_FAMILY + "';" +
                "-fx-font-size: " + fontSize + "px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #C0C0C0;"
            );

            // Spacer pushes lock icon to the far right
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            FontIcon lockIcon = new FontIcon(FontAwesomeSolid.LOCK);
            lockIcon.setIconSize(13);
            lockIcon.setIconColor(Color.web("#C8A0A4"));

            HBox row = new HBox(12, icon, text, spacer, lockIcon);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 12, 6, 30));
            row.setPrefWidth(CARD_W - 12);
            // Slightly tinted background so the row visually reads as disabled
            row.setStyle("-fx-background-color: #FAF3F4; -fx-background-radius: 10;");
            row.setCursor(Cursor.DEFAULT);
            // No mouse-enter/exit handlers → no hover effect
            // No click handlers → never wired in start()

            // Tooltip on hover so the user understands why it is disabled
            Tooltip tip = new Tooltip("For managers only");
            tip.setShowDelay(Duration.millis(200));
            tip.setStyle(
                "-fx-font-family: '" + FONT_FAMILY + "';" +
                "-fx-font-size: 12px;" +
                "-fx-background-color: #68222A;" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 6;" +
                "-fx-padding: 4 10 4 10;"
            );
            Tooltip.install(row, tip);

            return row;
        }
    }
}