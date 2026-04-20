package frontend;

import backend.auth_util;
import backend.menu_util.SubmitResult;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
    private static final double CARD2_Y = 370;
    private static final double CARD2_H = 310;

    // ══════════════════════════════════════════════════════
    //  CARD 3 CONFIGURATION
    // ══════════════════════════════════════════════════════
    private static final double CARD3_GAP = 10;
    private static final double CARD3_H   = 135;
    private static final double CARD3_Y   = CARD2_Y + CARD2_H + CARD3_GAP;

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

    // ── Fixed modal dimensions ────────────────────────────
    private static final double MODAL_W = 440;
    private static final double MODAL_H = 260;

    private final StackPane contentPane = new StackPane();
    private final Connection conn;

    private menu_contents      menuContents;
    private orders_contents    ordersContents;
    private customers_contents customersContents;
    private payments_contents  paymentsContents;
    private timelogs_contents  timelogsContents;
    private employees_contents employeesContents;
    private inventory_contents inventoryContents;
    private suppliers_contents suppliersContents;   // ← NEW

    // Live-updating labels in Card 3
    private Label     hoursWorkedValue;
    private Timeline  shiftClock;

    // Root overlay pane for modals
    private Pane      rootPane;

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

        boolean isManager = auth_util.isManager();

        double screenW = Screen.getPrimary().getBounds().getWidth();
        double screenH = Screen.getPrimary().getBounds().getHeight();

        ImageView background = new ImageView(new Image("file:assets/cafe_bg.png"));
        background.setFitWidth(screenW);
        background.setFitHeight(screenH);
        background.setPreserveRatio(false);

        double contentW = screenW - CONTENT_X - 10;

        // ── Instantiate all content views ─────────────────
        menuContents      = new menu_contents(contentW, CONTENT_H, conn);
        ordersContents    = new orders_contents(contentW, CONTENT_H, conn);
        customersContents = new customers_contents(contentW, CONTENT_H, conn);
        paymentsContents  = new payments_contents(contentW, CONTENT_H, conn);
        timelogsContents  = new timelogs_contents(contentW, CONTENT_H, conn);
        employeesContents = new employees_contents(contentW, CONTENT_H, conn);
        inventoryContents = new inventory_contents(contentW, CONTENT_H, conn);
        suppliersContents = new suppliers_contents(contentW, CONTENT_H, conn);  // ← NEW

        // ── Cross-view live-update wiring ─────────────────
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
            if (result.paymentId != null) {
                paymentsContents.prependPayment(
                    result.paymentId,
                    result.orderId,
                    result.paymentMethod,
                    result.amountFormatted
                );
            }
        });

        // ══════════════════════════════════════════════════
        //  CARD 1 — always accessible
        // ══════════════════════════════════════════════════
        HBox menuBtn      = createNavButton("Menu",      "assets/icons/menu_icon.png",      46, 23, true);
        HBox ordersBtn    = createNavButton("Orders",    "assets/icons/orders_icon.png",    46, 23, true);
        HBox customersBtn = createNavButton("Customers", "assets/icons/customers_icon.png", 46, 23, true);

        VBox card1 = new VBox(8);
        card1.setAlignment(Pos.CENTER_LEFT);
        card1.setPadding(new Insets(10, 6, 10, 6));
        card1.setStyle(cardStyle());
        card1.setPrefWidth(CARD_W);
        card1.setPrefHeight(CARD_H);
        card1.setLayoutX(CARD_X);
        card1.setLayoutY(CARD_Y);
        card1.getChildren().addAll(menuBtn, ordersBtn, customersBtn);

        // ══════════════════════════════════════════════════
        //  CARD 2 — manager-only buttons, scrollable
        // ══════════════════════════════════════════════════
        Label managerLabel = new Label("— Manager only —");
        managerLabel.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 12px;" +
            "-fx-font-style: italic;" +
            "-fx-text-fill: " + ACCENT + ";" +
            "-fx-opacity: 0.75;"
        );
        managerLabel.setPrefWidth(CARD_W - 12);
        managerLabel.setAlignment(Pos.CENTER);
        VBox.setMargin(managerLabel, new Insets(0, 0, 2, 0));

        HBox paymentsBtn   = createNavButton("Payments",   "assets/icons/payments_icon.png",   38, 20, isManager);
        HBox timeLogsBtn   = createNavButton("Time Logs",  "assets/icons/timelogs_icon.png",   38, 20, isManager);
        HBox employeesBtn  = createNavButton("Employees",  "assets/icons/employees_icon.png",  38, 20, isManager);
        HBox inventoryBtn  = createNavButton("Inventory",  "assets/icons/inventory_icon.png",  38, 20, isManager);
        HBox suppliersBtn  = createNavButton("Suppliers",  "assets/icons/suppliers_icon.png",  38, 20, isManager);  // ← now wired
        HBox purchasesBtn  = createNavButton("Purchases",  "assets/icons/purchases_icon.png",  38, 20, isManager);
        HBox promotionsBtn = createNavButton("Promotions", "assets/icons/promotions_icon.png", 38, 20, isManager);

        VBox card2Inner = new VBox(7);
        card2Inner.setAlignment(Pos.CENTER_LEFT);
        card2Inner.setPadding(new Insets(6, 6, 6, 6));
        card2Inner.getChildren().addAll(
            managerLabel,
            paymentsBtn, timeLogsBtn, employeesBtn,
            inventoryBtn, suppliersBtn, purchasesBtn, promotionsBtn
        );

        ScrollPane card2Scroll = new ScrollPane(card2Inner);
        card2Scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        card2Scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        card2Scroll.setFitToWidth(true);
        card2Scroll.setStyle(
            "-fx-background: transparent;" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;" +
            "-fx-padding: 0;"
        );
        card2Scroll.setPrefWidth(CARD_W - 4);
        card2Scroll.setPrefHeight(CARD2_H - 4);

        VBox card2 = new VBox(0);
        card2.setAlignment(Pos.TOP_LEFT);
        card2.setStyle(cardStyle());
        card2.setPrefWidth(CARD_W);
        card2.setPrefHeight(CARD2_H);
        card2.setLayoutX(CARD_X);
        card2.setLayoutY(CARD2_Y);
        card2.getChildren().add(card2Scroll);

        // ══════════════════════════════════════════════════
        //  CARD 3 — signed-in session info
        // ══════════════════════════════════════════════════
        VBox card3 = buildSessionCard(stage);
        card3.setLayoutX(CARD_X);
        card3.setLayoutY(CARD3_Y);

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

        // ── Card 1 wiring ─────────────────────────────────
        menuBtn.setOnMouseClicked(e      -> showMenu());
        ordersBtn.setOnMouseClicked(e    -> showOrders());
        customersBtn.setOnMouseClicked(e -> showCustomers());

        // ── Card 2 wiring (managers only) ─────────────────
        if (isManager) {
            paymentsBtn.setOnMouseClicked(e   -> showPayments());
            timeLogsBtn.setOnMouseClicked(e   -> showTimeLogs());
            employeesBtn.setOnMouseClicked(e  -> showEmployees());
            inventoryBtn.setOnMouseClicked(e  -> showInventory());
            suppliersBtn.setOnMouseClicked(e  -> showSuppliers());   // ← wired
            purchasesBtn.setOnMouseClicked(e  -> switchContent("Purchases"));
            promotionsBtn.setOnMouseClicked(e -> switchContent("Promotions"));
        }

        // ── Start live clock ──────────────────────────────
        startShiftClock();

        rootPane = new Pane();
        rootPane.getChildren().addAll(background, card1, card2, card3, contentPane);

        Scene scene = new Scene(rootPane, screenW, screenH);
        stage.setTitle("Cafe Saburo – POS & Records");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.show();
    }

    // ══════════════════════════════════════════════════════
    //  CARD 3 — SESSION INFO BUILDER
    // ══════════════════════════════════════════════════════
    private VBox buildSessionCard(Stage stage) {
        VBox card = new VBox(10);
        card.setPrefWidth(CARD_W);
        card.setPrefHeight(CARD3_H);
        card.setPadding(new Insets(14, 14, 14, 14));
        card.setStyle(cardStyle());

        FontIcon avatarIcon = new FontIcon(FontAwesomeSolid.USER_CIRCLE);
        avatarIcon.setIconSize(22);
        avatarIcon.setIconColor(Color.web(ACCENT));

        Label nameLabel = new Label(auth_util.getCurrentName());
        nameLabel.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + ACCENT + ";"
        );
        nameLabel.setWrapText(true);

        String roleText = auth_util.isManager() ? "Manager" : "Employee";
        Label roleBadge = new Label(roleText);
        roleBadge.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 10px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;" +
            "-fx-background-color: " + ACCENT + ";" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 1 6 1 6;"
        );

        VBox nameBox = new VBox(3, nameLabel, roleBadge);
        nameBox.setAlignment(Pos.CENTER_LEFT);

        HBox headerRow = new HBox(8, avatarIcon, nameBox);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #F0D8DA;");

        Label timeInRow = buildInfoRow(
            FontAwesomeSolid.SIGN_IN_ALT,
            "Time In",
            auth_util.getTimeInFormatted()
        );

        hoursWorkedValue = new Label(auth_util.getHoursWorked());
        hoursWorkedValue.setStyle(valueStyle());

        Label hoursIcon = buildIconLabel(FontAwesomeSolid.CLOCK);
        Label hoursKey  = buildKeyLabel("Hours Worked");

        HBox hoursRow = new HBox(6, hoursIcon, hoursKey, buildRowSpacer(), hoursWorkedValue);
        hoursRow.setAlignment(Pos.CENTER_LEFT);

        Region vSpacer = new Region();
        VBox.setVgrow(vSpacer, Priority.ALWAYS);

        FontIcon timeOutIcon = new FontIcon(FontAwesomeSolid.SIGN_OUT_ALT);
        timeOutIcon.setIconSize(13);
        timeOutIcon.setIconColor(Color.web("#721C24"));

        Label timeOutBtn = new Label("Time Out");
        timeOutBtn.setGraphic(timeOutIcon);
        timeOutBtn.setGraphicTextGap(7);
        timeOutBtn.setCursor(Cursor.HAND);
        timeOutBtn.setPrefWidth(CARD_W - 28);
        timeOutBtn.setPrefHeight(34);
        timeOutBtn.setAlignment(Pos.CENTER);
        timeOutBtn.setStyle(timeOutBtnStyle(false));
        timeOutBtn.setOnMouseEntered(e -> timeOutBtn.setStyle(timeOutBtnStyle(true)));
        timeOutBtn.setOnMouseExited(e  -> timeOutBtn.setStyle(timeOutBtnStyle(false)));
        timeOutBtn.setOnMouseClicked(e -> handleTimeOut(stage));

        card.getChildren().addAll(headerRow, divider, timeInRow, hoursRow, vSpacer, timeOutBtn);
        return card;
    }

    // ══════════════════════════════════════════════════════
    //  SHIFT CLOCK
    // ══════════════════════════════════════════════════════
    private void startShiftClock() {
        shiftClock = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> {
                if (hoursWorkedValue != null)
                    hoursWorkedValue.setText(auth_util.getHoursWorked());
            })
        );
        shiftClock.setCycleCount(Animation.INDEFINITE);
        shiftClock.play();
        if (hoursWorkedValue != null)
            hoursWorkedValue.setText(auth_util.getHoursWorked());
    }

    // ══════════════════════════════════════════════════════
    //  TIME-OUT HANDLER
    // ══════════════════════════════════════════════════════
    private void handleTimeOut(Stage posStage) {
        String hoursWorked = auth_util.getHoursWorked();

        Pane overlay = new Pane();
        overlay.setPrefWidth(rootPane.getPrefWidth() > 0 ? rootPane.getPrefWidth() : posStage.getWidth());
        overlay.setPrefHeight(rootPane.getPrefHeight() > 0 ? rootPane.getPrefHeight() : posStage.getHeight());
        overlay.setMinWidth(overlay.getPrefWidth());
        overlay.setMinHeight(overlay.getPrefHeight());
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.52);");

        VBox card = new VBox(18);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(MODAL_W); card.setMinWidth(MODAL_W); card.setMaxWidth(MODAL_W);
        card.setPrefHeight(MODAL_H); card.setMinHeight(MODAL_H); card.setMaxHeight(MODAL_H);
        card.setPadding(new Insets(36, 40, 32, 40));
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 14;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0, 0, 6);"
        );

        FontIcon warnIcon = new FontIcon(FontAwesomeSolid.SIGN_OUT_ALT);
        warnIcon.setIconSize(28);
        warnIcon.setIconColor(Color.web("#882F39"));

        Label heading = new Label("Are you sure you want to end your shift?");
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
        heading.setMaxWidth(MODAL_W - 80);

        Label sub = new Label("You've worked " + hoursWorked + " today.");
        sub.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #555555;" +
            "-fx-text-alignment: center;" +
            "-fx-alignment: center;"
        );
        sub.setAlignment(Pos.CENTER);
        sub.setWrapText(true);
        sub.setMaxWidth(MODAL_W - 80);

        Label noBtn = new Label("No, go back");
        noBtn.setCursor(Cursor.HAND);
        noBtn.setPrefWidth(140); noBtn.setPrefHeight(38);
        noBtn.setAlignment(Pos.CENTER);
        noBtn.setStyle(modalNoBtnStyle(false));
        noBtn.setOnMouseEntered(e -> noBtn.setStyle(modalNoBtnStyle(true)));
        noBtn.setOnMouseExited(e  -> noBtn.setStyle(modalNoBtnStyle(false)));
        noBtn.setOnMouseClicked(e -> rootPane.getChildren().remove(overlay));

        Label yesBtn = new Label("Yes, end shift");
        yesBtn.setCursor(Cursor.HAND);
        yesBtn.setPrefWidth(140); yesBtn.setPrefHeight(38);
        yesBtn.setAlignment(Pos.CENTER);
        yesBtn.setStyle(modalYesBtnStyle(false));
        yesBtn.setOnMouseEntered(e -> yesBtn.setStyle(modalYesBtnStyle(true)));
        yesBtn.setOnMouseExited(e  -> yesBtn.setStyle(modalYesBtnStyle(false)));
        yesBtn.setOnMouseClicked(e -> {
            if (shiftClock != null) shiftClock.stop();
            System.out.println("[TIME-OUT] User     : " + auth_util.getCurrentName());
            System.out.println("[TIME-OUT] Shift In : " + auth_util.getShiftStartFullFormatted());
            System.out.println("[TIME-OUT] Duration : " + hoursWorked);
            if (auth_util.isEmployee()) {
                boolean stamped = auth_util.timeOut();
                if (!stamped) {
                    System.err.println("[TIME-OUT] WARNING: DB stamp failed.");
                }
            } else {
                auth_util.logout();
            }
            posStage.close();
            openAuthWindow();
        });

        HBox btnRow = new HBox(16, noBtn, yesBtn);
        btnRow.setAlignment(Pos.CENTER);
        card.getChildren().addAll(warnIcon, heading, sub, btnRow);

        StackPane centred = new StackPane(card);
        centred.setPrefWidth(overlay.getPrefWidth());
        centred.setPrefHeight(overlay.getPrefHeight());
        centred.setMinWidth(overlay.getPrefWidth());
        centred.setMinHeight(overlay.getPrefHeight());
        centred.setAlignment(Pos.CENTER);
        overlay.getChildren().add(centred);
        rootPane.getChildren().add(overlay);
    }

    // ══════════════════════════════════════════════════════
    //  NAVIGATION — back to auth
    // ══════════════════════════════════════════════════════
    private void openAuthWindow() {
        Stage authStage = new Stage();
        auth_ui authUI  = new auth_ui(conn);
        authUI.start(authStage);
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

    private void showPayments() {
        contentPane.getChildren().setAll(paymentsContents.getView());
    }

    private void showTimeLogs() {
        contentPane.getChildren().setAll(timelogsContents.getView());
    }

    private void showEmployees() {
        contentPane.getChildren().setAll(employeesContents.getView());
    }

    private void showInventory() {
        contentPane.getChildren().setAll(inventoryContents.getView());
    }

    private void showSuppliers() {
        contentPane.getChildren().setAll(suppliersContents.getView());  // ← NEW
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
                "-fx-background-color: " + HOVER_BG + "; -fx-background-radius: 10;"));
            row.setOnMouseExited(e -> row.setStyle(
                "-fx-background-color: " + DEFAULT_BG + "; -fx-background-radius: 10;"));
            return row;
        } else {
            icon.setOpacity(0.25);
            text.setStyle(
                "-fx-font-family: '" + FONT_FAMILY + "';" +
                "-fx-font-size: " + fontSize + "px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #C0C0C0;"
            );
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            FontIcon lockIcon = new FontIcon(FontAwesomeSolid.LOCK);
            lockIcon.setIconSize(13);
            lockIcon.setIconColor(Color.web("#C8A0A4"));
            HBox row = new HBox(12, icon, text, spacer, lockIcon);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 12, 6, 30));
            row.setPrefWidth(CARD_W - 12);
            row.setStyle("-fx-background-color: #FAF3F4; -fx-background-radius: 10;");
            row.setCursor(Cursor.DEFAULT);
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

    // ══════════════════════════════════════════════════════
    //  CARD 3 HELPER BUILDERS
    // ══════════════════════════════════════════════════════
    private Label buildInfoRow(FontAwesomeSolid iconCode, String key, String value) {
        Label iconLbl  = buildIconLabel(iconCode);
        Label keyLbl   = buildKeyLabel(key);
        Label valueLbl = new Label(value);
        valueLbl.setStyle(valueStyle());
        HBox row = new HBox(6, iconLbl, keyLbl, buildRowSpacer(), valueLbl);
        row.setAlignment(Pos.CENTER_LEFT);
        Label wrapper = new Label();
        wrapper.setGraphic(row);
        wrapper.setPadding(Insets.EMPTY);
        return wrapper;
    }

    private Label buildIconLabel(FontAwesomeSolid iconCode) {
        FontIcon fi = new FontIcon(iconCode);
        fi.setIconSize(12);
        fi.setIconColor(Color.web(ACCENT));
        Label lbl = new Label();
        lbl.setGraphic(fi);
        return lbl;
    }

    private Label buildKeyLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 11px;" +
            "-fx-text-fill: #AA6670;"
        );
        return lbl;
    }

    private Region buildRowSpacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private String valueStyle() {
        return "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 12px;" +
               "-fx-font-weight: bold;" +
               "-fx-text-fill: #333333;";
    }

    // ══════════════════════════════════════════════════════
    //  STYLE HELPERS
    // ══════════════════════════════════════════════════════
    private String cardStyle() {
        return "-fx-background-color: white;" +
               "-fx-background-radius: 18;" +
               "-fx-border-color: #882F39;" +
               "-fx-border-width: 1.5;" +
               "-fx-border-radius: 18;";
    }

    private String timeOutBtnStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#F8D7DA" : "#FDF0F1") + ";" +
               "-fx-background-radius: 9;" +
               "-fx-border-color: #721C24;" +
               "-fx-border-radius: 9;" +
               "-fx-border-width: 1.5;" +
               "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 13px;" +
               "-fx-font-weight: bold;" +
               "-fx-text-fill: #721C24;" +
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