package frontend;

import backend.auth_util;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.time.LocalDate;
import java.time.YearMonth;

public class auth_ui {

    private final java.sql.Connection conn;

    // ── Fixed modal dimensions ────────────────────────────
    private static final double MODAL_W = 360;
    private static final double MODAL_H = 280;

    public auth_ui(java.sql.Connection conn) {
        this.conn = conn;
        auth_util.setConnection(conn);
    }

    // ══════════════════════════════════════════════════════
    //  FONT LOADER
    // ══════════════════════════════════════════════════════
    private static final Font ALEO_BOLD_ITALIC  = Font.loadFont("file:assets/fonts/Aleo-BoldItalic.ttf",   34);
    private static final Font ALEO_EXTRA_BOLD   = Font.loadFont("file:assets/fonts/Aleo-ExtraBold.ttf",    44);
    private static final Font ALEO_BOLD         = Font.loadFont("file:assets/fonts/Aleo-Bold.ttf",         16);
    private static final Font ALEO_SEMIBOLD     = Font.loadFont("file:assets/fonts/Aleo-SemiBold.ttf",     15);
    private static final Font ALEO_MEDIUM       = Font.loadFont("file:assets/fonts/Aleo-Medium.ttf",       14);
    private static final Font ALEO_REGULAR      = Font.loadFont("file:assets/fonts/Aleo-Regular.ttf",      15);
    private static final Font ALEO_LIGHT        = Font.loadFont("file:assets/fonts/Aleo-Light.ttf",        12);
    private static final Font ALEO_ITALIC       = Font.loadFont("file:assets/fonts/Aleo-Italic.ttf",       13);

    public void start(Stage stage) {

        double screenW = Screen.getPrimary().getBounds().getWidth();
        double screenH = Screen.getPrimary().getBounds().getHeight();

        ImageView background = new ImageView(new Image("file:assets/cafe_auth.png"));
        background.setFitWidth(screenW);
        background.setFitHeight(screenH);
        background.setPreserveRatio(false);

        // ══════════════════════════════════════════════════
        //  SET CUSTOM WINDOW ICON
        // ══════════════════════════════════════════════════
        try {
            stage.getIcons().add(new Image("file:assets/cafe_logo.png"));
        } catch (Exception e) {
            System.err.println("Warning: Could not load cafe_logo.png for window icon: " + e.getMessage());
        }

        // ══════════════════════════════════════════════════
        //  TOGGLE BAR  (Employee / Manager)
        // ══════════════════════════════════════════════════
        ToggleGroup toggleGroup = new ToggleGroup();
        ToggleButton btnEmployee = new ToggleButton("Employee");
        ToggleButton btnManager  = new ToggleButton("Manager");
        btnEmployee.setToggleGroup(toggleGroup);
        btnManager.setToggleGroup(toggleGroup);
        btnEmployee.setSelected(true);
        styleToggleWide(btnEmployee, true);
        styleToggleWide(btnManager, false);
        btnEmployee.setMaxWidth(Double.MAX_VALUE);
        btnManager.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnEmployee, Priority.ALWAYS);
        HBox.setHgrow(btnManager,  Priority.ALWAYS);

        HBox toggleBar = new HBox(0, btnEmployee, btnManager);
        toggleBar.setAlignment(Pos.CENTER);
        toggleBar.setStyle("-fx-background-color: #2b0d10;-fx-background-radius: 30;");
        toggleBar.setPadding(new Insets(5));
        toggleBar.setMaxWidth(Double.MAX_VALUE);

        // ══════════════════════════════════════════════════
        //  EMPLOYEE PANEL  — name + shift date, DB lookup
        // ══════════════════════════════════════════════════
        Label empTitle = panelTitle("Time - in");
        VBox.setMargin(empTitle, new Insets(4, 0, 4, 0));

        // ── Employee icon ─────────────────────────────────
        ImageView empIcon = loadIcon("file:assets/icons/employee_signin_icon.png", 120);
        VBox.setMargin(empIcon, new Insets(0, 0, 8, 0));

        // ── Name label + field ────────────────────────────
        Label empNameLabel = fieldLabel("Enter your name exactly as registered to clock in.");
        empNameLabel.setFont(Font.loadFont("file:assets/fonts/Aleo-Italic.ttf", 12));
        empNameLabel.setWrapText(true);
        empNameLabel.setMaxWidth(Double.MAX_VALUE);

        TextField empNameField = styledField("Full name");
        empNameField.setMaxWidth(Double.MAX_VALUE);

        // ── Shift Date label ──────────────────────────────
        Label shiftDateLabel = fieldLabel("Shift Date:");

        // ── Month dropdown ────────────────────────────────
        ComboBox<String> monthBox = new ComboBox<>();
        monthBox.getItems().addAll(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        );
        monthBox.setPromptText("Month");
        styleComboBox(monthBox);
        monthBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(monthBox, Priority.ALWAYS);

        // ── Day dropdown ──────────────────────────────────
        ComboBox<Integer> dayBox = new ComboBox<>();
        dayBox.setPromptText("Day");
        styleComboBox(dayBox);
        dayBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(dayBox, Priority.ALWAYS);

        // ── Year dropdown ─────────────────────────────────
        ComboBox<Integer> yearBox = new ComboBox<>();
        int currentYear = LocalDate.now().getYear();
        yearBox.getItems().add(currentYear);
        yearBox.getItems().add(currentYear + 1);
        yearBox.setPromptText("Year");
        styleComboBox(yearBox);
        yearBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(yearBox, Priority.ALWAYS);

        // ── Pre-select today ──────────────────────────────
        LocalDate today = LocalDate.now();
        monthBox.getSelectionModel().select(today.getMonthValue() - 1);
        yearBox.getSelectionModel().select(Integer.valueOf(today.getYear()));
        populateDaysFromToday(dayBox, today.getMonthValue(), today.getYear());
        dayBox.getSelectionModel().select(Integer.valueOf(today.getDayOfMonth()));

        // ── Re-populate days when month/year changes ──────
        monthBox.setOnAction(e -> {
            int selMonth = monthBox.getSelectionModel().getSelectedIndex() + 1;
            Integer selYear = yearBox.getValue();
            if (selYear != null) populateDaysFromToday(dayBox, selMonth, selYear);
        });
        yearBox.setOnAction(e -> {
            int selMonth = monthBox.getSelectionModel().getSelectedIndex() + 1;
            Integer selYear = yearBox.getValue();
            if (selYear != null) populateDaysFromToday(dayBox, selMonth, selYear);
        });

        HBox dateRow = new HBox(8, monthBox, dayBox, yearBox);
        dateRow.setAlignment(Pos.CENTER_LEFT);
        dateRow.setMaxWidth(Double.MAX_VALUE);

        Label empError    = errorLabel();
        Button empConfirm = confirmButton("Confirm");
        empNameField.setOnAction(e -> empConfirm.fire());
        VBox.setMargin(empConfirm, new Insets(-4, 0, 0, 0));

        VBox employeePanel = new VBox(8,
            empTitle,
            centeredIcon(empIcon),
            empNameLabel,
            empNameField,
            shiftDateLabel,
            dateRow,
            empError,
            empConfirm
        );
        employeePanel.setAlignment(Pos.CENTER_LEFT);
        employeePanel.setMaxWidth(Double.MAX_VALUE);

        // ══════════════════════════════════════════════════
        //  MANAGER PANEL  — hardcoded credentials
        // ══════════════════════════════════════════════════
        Label mgrTitle = panelTitle("Log - in");
        VBox.setMargin(mgrTitle, new Insets(4, 0, 4, 0));

        // ── Manager icon ──────────────────────────────────
        ImageView mgrIcon = loadIcon("file:assets/icons/manager_signin_icon.png", 120);
        VBox.setMargin(mgrIcon, new Insets(0, 0, 8, 0));

        // ── Name label + field ────────────────────────────
        Label mgrNameLabel = fieldLabel("Enter Name:");

        TextField mgrNameField = styledField("Full name");
        mgrNameField.setMaxWidth(Double.MAX_VALUE);

        // ── Password label + field ────────────────────────
        Label mgrPassLabel = fieldLabel("Enter Password:");

        PasswordField mgrPassHidden = new PasswordField();
        mgrPassHidden.setPromptText("Password");
        styleFieldBase(mgrPassHidden);
        mgrPassHidden.setMaxWidth(Double.MAX_VALUE);

        TextField mgrPassVisible = new TextField();
        mgrPassVisible.setPromptText("Password");
        styleFieldBase(mgrPassVisible);
        mgrPassVisible.setMaxWidth(Double.MAX_VALUE);
        mgrPassVisible.setVisible(false);
        mgrPassVisible.setManaged(false);

        mgrPassHidden.textProperty().addListener((obs, o, n) -> {
            if (!mgrPassVisible.isFocused()) mgrPassVisible.setText(n);
        });
        mgrPassVisible.textProperty().addListener((obs, o, n) -> {
            if (!mgrPassHidden.isFocused()) mgrPassHidden.setText(n);
        });

        FontIcon eyeIcon      = new FontIcon(FontAwesomeSolid.EYE);
        eyeIcon.setIconSize(18);
        eyeIcon.setIconColor(Color.web("#68222A"));
        FontIcon eyeSlashIcon = new FontIcon(FontAwesomeSolid.EYE_SLASH);
        eyeSlashIcon.setIconSize(18);
        eyeSlashIcon.setIconColor(Color.web("#68222A"));

        Button toggleVisBtn = new Button();
        toggleVisBtn.setGraphic(eyeIcon);
        toggleVisBtn.setStyle(
            "-fx-background-color: transparent;-fx-cursor: hand;-fx-padding: 0 12 0 12;"
        );

        final boolean[] passwordVisible = {false};
        toggleVisBtn.setOnAction(e -> {
            passwordVisible[0] = !passwordVisible[0];
            if (passwordVisible[0]) {
                mgrPassVisible.setText(mgrPassHidden.getText());
                mgrPassHidden.setVisible(false);  mgrPassHidden.setManaged(false);
                mgrPassVisible.setVisible(true);  mgrPassVisible.setManaged(true);
                toggleVisBtn.setGraphic(eyeSlashIcon);
            } else {
                mgrPassHidden.setText(mgrPassVisible.getText());
                mgrPassVisible.setVisible(false); mgrPassVisible.setManaged(false);
                mgrPassHidden.setVisible(true);   mgrPassHidden.setManaged(true);
                toggleVisBtn.setGraphic(eyeIcon);
            }
        });

        StackPane passStack = new StackPane();
        passStack.setMaxWidth(Double.MAX_VALUE);
        passStack.setAlignment(Pos.CENTER_RIGHT);
        passStack.getChildren().addAll(mgrPassHidden, mgrPassVisible, toggleVisBtn);
        StackPane.setAlignment(toggleVisBtn, Pos.CENTER_RIGHT);

        Label  mgrError   = errorLabel();
        Button mgrConfirm = confirmButton("Log In");
        mgrPassHidden.setOnAction(e  -> mgrConfirm.fire());
        mgrPassVisible.setOnAction(e -> mgrConfirm.fire());
        VBox.setMargin(mgrConfirm, new Insets(-4, 0, 0, 0));

        VBox managerPanel = new VBox(8,
            mgrTitle,
            centeredIcon(mgrIcon),
            mgrNameLabel,
            mgrNameField,
            mgrPassLabel,
            passStack,
            mgrError,
            mgrConfirm
        );
        managerPanel.setAlignment(Pos.CENTER_LEFT);
        managerPanel.setMaxWidth(Double.MAX_VALUE);

        // ══════════════════════════════════════════════════
        //  MAIN CONTAINER
        // ══════════════════════════════════════════════════
        VBox container = new VBox(20, toggleBar, employeePanel);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(36, 40, 40, 40));
        container.setStyle("-fx-background-color: #68222A;-fx-background-radius: 28;");
        container.setPrefWidth(480);
        container.setMinWidth(480);
        container.setMaxWidth(480);
        container.setLayoutX(160);
        container.setLayoutY(110);

        btnEmployee.setOnAction(e -> {
            if (!btnEmployee.isSelected()) btnEmployee.setSelected(true);
            styleToggleWide(btnEmployee, true);
            styleToggleWide(btnManager,  false);
            switchPanel(container, employeePanel);
        });
        btnManager.setOnAction(e -> {
            if (!btnManager.isSelected()) btnManager.setSelected(true);
            styleToggleWide(btnManager,  true);
            styleToggleWide(btnEmployee, false);
            switchPanel(container, managerPanel);
        });

        // ══════════════════════════════════════════════════
        //  IN-SCENE OVERLAY MODAL  (no new Stage/window)
        // ══════════════════════════════════════════════════
        StackPane overlayBackdrop = new StackPane();
        overlayBackdrop.setPrefSize(screenW, screenH);
        overlayBackdrop.setStyle("-fx-background-color: rgba(0,0,0,0.58);");
        overlayBackdrop.setVisible(false);
        overlayBackdrop.setMouseTransparent(false);

        VBox modalCard = new VBox(20);
        modalCard.setAlignment(Pos.CENTER);
        modalCard.setPadding(new Insets(48, 64, 48, 64));
        modalCard.setPrefWidth(MODAL_W);  modalCard.setMinWidth(MODAL_W);  modalCard.setMaxWidth(MODAL_W);
        modalCard.setPrefHeight(MODAL_H); modalCard.setMinHeight(MODAL_H); modalCard.setMaxHeight(MODAL_H);
        modalCard.setStyle(
            "-fx-background-color: #68222A;" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: white;" +
            "-fx-border-width: 4;" +
            "-fx-border-radius: 20;"
        );

        Label modalIcon = new Label("✔");
        modalIcon.setFont(Font.loadFont("file:assets/fonts/Aleo-Bold.ttf", 52));
        modalIcon.setTextFill(Color.web("#2ecc40"));

        Label modalMsg = new Label();
        modalMsg.setFont(Font.loadFont("file:assets/fonts/Aleo-SemiBold.ttf", 20));
        modalMsg.setTextFill(Color.WHITE);
        modalMsg.setWrapText(true);
        modalMsg.setAlignment(Pos.CENTER);
        modalMsg.setMaxWidth(Double.MAX_VALUE);

        Button modalOkBtn = new Button("Continue");
        modalOkBtn.setPrefWidth(200);
        modalOkBtn.setPrefHeight(44);
        modalOkBtn.setFont(Font.loadFont("file:assets/fonts/Aleo-Bold.ttf", 15));
        String okBase  = "-fx-background-color: #2ecc40;-fx-text-fill: white;" +
                         "-fx-font-size: 15px;-fx-font-weight: bold;-fx-font-family: 'Aleo';" +
                         "-fx-background-radius: 12;-fx-cursor: hand;";
        String okHover = "-fx-background-color: #27ae38;-fx-text-fill: white;" +
                         "-fx-font-size: 15px;-fx-font-weight: bold;-fx-font-family: 'Aleo';" +
                         "-fx-background-radius: 12;-fx-cursor: hand;";
        modalOkBtn.setStyle(okBase);
        modalOkBtn.setOnMouseEntered(e -> modalOkBtn.setStyle(okHover));
        modalOkBtn.setOnMouseExited(e  -> modalOkBtn.setStyle(okBase));
        modalOkBtn.setOnAction(e -> {
            overlayBackdrop.setVisible(false);
            openPosWindow(stage);
        });

        modalCard.getChildren().addAll(modalIcon, modalMsg, modalOkBtn);
        overlayBackdrop.getChildren().add(modalCard);

        // ── Wire manager confirm ───────────────────────────
        mgrConfirm.setOnAction(e -> {
            String pass = passwordVisible[0]
                    ? mgrPassVisible.getText()
                    : mgrPassHidden.getText();
            auth_util.AuthResult result = auth_util.authenticateManager(
                    mgrNameField.getText(), pass);
            if (result.success) {
                mgrError.setVisible(false);
                modalMsg.setText("Welcome, " + auth_util.getCurrentName() + "!");
                showOverlay(overlayBackdrop);
            } else {
                mgrError.setText(result.message);
                mgrError.setVisible(true);
            }
        });

        // ── Wire employee confirm ──────────────────────────
        empConfirm.setOnAction(e -> {
            String selMonth = monthBox.getValue();
            Integer selDay  = dayBox.getValue();
            Integer selYear = yearBox.getValue();

            if (selMonth == null || selDay == null || selYear == null) {
                empError.setText("Please select a complete shift date.");
                empError.setVisible(true);
                return;
            }

            int monthIdx = monthBox.getSelectionModel().getSelectedIndex() + 1;
            LocalDate shiftDate = LocalDate.of(selYear, monthIdx, selDay);

            if (shiftDate.isBefore(LocalDate.now())) {
                empError.setText("Shift date cannot be in the past.");
                empError.setVisible(true);
                return;
            }

            auth_util.AuthResult result = auth_util.authenticateEmployee(
                    empNameField.getText(), shiftDate);

            if (result.success) {
                empError.setVisible(false);
                openPosWindow(stage);
            } else {
                empError.setText(result.message);
                empError.setVisible(true);
                empNameField.requestFocus();
                empNameField.selectAll();
            }
        });

        Pane root = new Pane();
        root.getChildren().addAll(background, container, overlayBackdrop);

        Scene scene = new Scene(root, screenW, screenH);
        stage.setTitle("Cafe Saburo");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.show();
    }

    // ══════════════════════════════════════════════════════
    //  OVERLAY FADE-IN
    // ══════════════════════════════════════════════════════
    private void showOverlay(StackPane overlay) {
        overlay.setOpacity(0);
        overlay.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(220), overlay);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    // ══════════════════════════════════════════════════════
    //  DAY POPULATOR — only today and future days
    // ══════════════════════════════════════════════════════
    private void populateDaysFromToday(ComboBox<Integer> dayBox, int month, int year) {
        Integer prev = dayBox.getValue();
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();

        LocalDate today = LocalDate.now();
        int firstDay = 1;
        if (year == today.getYear() && month == today.getMonthValue()) {
            firstDay = today.getDayOfMonth();
        } else if (year < today.getYear() ||
                   (year == today.getYear() && month < today.getMonthValue())) {
            dayBox.getItems().clear();
            return;
        }

        dayBox.getItems().clear();
        for (int d = firstDay; d <= daysInMonth; d++) {
            dayBox.getItems().add(d);
        }

        if (prev != null && prev >= firstDay && prev <= daysInMonth) {
            dayBox.getSelectionModel().select(prev);
        } else {
            dayBox.getSelectionModel().selectFirst();
        }
    }

    // ══════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════
    private void openPosWindow(Stage authStage) {
        Stage posStage = new Stage();
        pos_records_window_ui posUI = new pos_records_window_ui(conn);
        posUI.start(posStage);
        authStage.close();
    }

    // ══════════════════════════════════════════════════════
    //  UI HELPERS
    // ══════════════════════════════════════════════════════

    /** Loads an icon ImageView. Wrap with centeredIcon() for display. */
    private ImageView loadIcon(String path, double size) {
        ImageView iv = new ImageView();
        try {
            iv.setImage(new Image(path, size, size, true, true));
        } catch (Exception ignored) { /* icon won't show if file missing */ }
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        return iv;
    }

    /** Wraps an ImageView in a full-width HBox so it appears centered. */
    private HBox centeredIcon(ImageView iv) {
        HBox box = new HBox(iv);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private Label panelTitle(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.loadFont("file:assets/fonts/Aleo-ExtraBold.ttf", 44));
        lbl.setTextFill(Color.WHITE);
        lbl.setStyle(
            "-fx-font-size: 44px;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: 800;"
        );
        lbl.setAlignment(Pos.CENTER);
        lbl.setMaxWidth(Double.MAX_VALUE);
        return lbl;
    }

    private Label fieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setTextFill(Color.web("#e8b4b8"));
        lbl.setFont(Font.loadFont("file:assets/fonts/Aleo-SemiBold.ttf", 13));
        lbl.setWrapText(true);
        lbl.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(lbl, new Insets(4, 0, 0, 2));
        return lbl;
    }

    private Label errorLabel() {
        Label lbl = new Label();
        lbl.setTextFill(Color.web("#ff6b6b"));
        lbl.setFont(Font.loadFont("file:assets/fonts/Aleo-Italic.ttf", 13));
        lbl.setVisible(false);
        lbl.setWrapText(true);
        lbl.setMaxWidth(Double.MAX_VALUE);
        return lbl;
    }

    private void switchPanel(VBox container, VBox newPanel) {
        VBox current = (VBox) container.getChildren().get(1);
        if (current == newPanel) return;
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), current);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            container.getChildren().set(1, newPanel);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), newPanel);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    private TextField styledField(String placeholder) {
        TextField f = new TextField();
        f.setPromptText(placeholder);
        styleFieldBase(f);
        return f;
    }

    private void styleFieldBase(Control f) {
        f.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 12 16 12 16;" +
            "-fx-font-size: 15px;" +
            "-fx-font-family: 'Aleo';"
        );
        f.setPrefWidth(Double.MAX_VALUE);
        f.setPrefHeight(52);
    }

    private <T> void styleComboBox(ComboBox<T> box) {
        box.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 4 8 4 8;" +
            "-fx-font-size: 14px;" +
            "-fx-font-family: 'Aleo';" +
            "-fx-cursor: hand;"
        );
        box.setPrefHeight(52);
    }

    private Button confirmButton(String text) {
        Button btn = new Button(text);
        btn.setFont(Font.loadFont("file:assets/fonts/Aleo-Bold.ttf", 16));
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(52);
        String base  = "-fx-background-color: #2ecc40;-fx-text-fill: white;" +
                       "-fx-font-size: 16px;-fx-font-weight: bold;-fx-font-family: 'Aleo';" +
                       "-fx-background-radius: 14;-fx-cursor: hand;";
        String hover = "-fx-background-color: #27ae38;-fx-text-fill: white;" +
                       "-fx-font-size: 16px;-fx-font-weight: bold;-fx-font-family: 'Aleo';" +
                       "-fx-background-radius: 14;-fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private void styleToggleWide(ToggleButton btn, boolean active) {
        if (active) {
            btn.setFont(Font.loadFont("file:assets/fonts/Aleo-Bold.ttf", 14));
            btn.setStyle(
                "-fx-background-color: #68222A;-fx-text-fill: white;" +
                "-fx-font-weight: bold;-fx-font-size: 14px;-fx-font-family: 'Aleo';" +
                "-fx-background-radius: 30;-fx-padding: 10 0 10 0;-fx-cursor: hand;"
            );
        } else {
            btn.setFont(Font.loadFont("file:assets/fonts/Aleo-Medium.ttf", 14));
            btn.setStyle(
                "-fx-background-color: transparent;-fx-text-fill: #aaaaaa;" +
                "-fx-font-size: 14px;-fx-font-family: 'Aleo';" +
                "-fx-background-radius: 30;-fx-padding: 10 0 10 0;-fx-cursor: hand;"
            );
        }
    }
}