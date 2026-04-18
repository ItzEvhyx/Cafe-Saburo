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
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.Month;

public class auth_ui {

    // ══════════════════════════════════════════════════════
    //  ALEO FONT LOADER
    // ══════════════════════════════════════════════════════
    private static final Font ALEO_BOLD_ITALIC = Font.loadFont(
            "file:assets/fonts/Aleo-BoldItalic.ttf", 34);
    private static final Font ALEO_BOLD        = Font.loadFont(
            "file:assets/fonts/Aleo-Bold.ttf", 16);
    private static final Font ALEO_SEMIBOLD    = Font.loadFont(
            "file:assets/fonts/Aleo-SemiBold.ttf", 15);
    private static final Font ALEO_MEDIUM      = Font.loadFont(
            "file:assets/fonts/Aleo-Medium.ttf", 14);
    private static final Font ALEO_REGULAR     = Font.loadFont(
            "file:assets/fonts/Aleo-Regular.ttf", 15);
    private static final Font ALEO_LIGHT       = Font.loadFont(
            "file:assets/fonts/Aleo-Light.ttf", 12);
    private static final Font ALEO_ITALIC      = Font.loadFont(
            "file:assets/fonts/Aleo-Italic.ttf", 13);

    public void start(Stage stage) {

        double screenW = Screen.getPrimary().getBounds().getWidth();
        double screenH = Screen.getPrimary().getBounds().getHeight();

        ImageView background = new ImageView(new Image("file:assets/cafe_auth.png"));
        background.setFitWidth(screenW);
        background.setFitHeight(screenH);
        background.setPreserveRatio(false);

        // ── Toggle Buttons ────────────────────────────────
        ToggleGroup toggleGroup = new ToggleGroup();
        ToggleButton btnEmployee = new ToggleButton("Employees");
        ToggleButton btnManager  = new ToggleButton("Manager");
        btnEmployee.setToggleGroup(toggleGroup);
        btnManager.setToggleGroup(toggleGroup);
        btnEmployee.setSelected(true);
        styleToggle(btnEmployee, true);
        styleToggle(btnManager, false);

        HBox toggleBar = new HBox(0, btnEmployee, btnManager);
        toggleBar.setAlignment(Pos.CENTER);
        toggleBar.setStyle(
            "-fx-background-color: #2b0d10;" +
            "-fx-background-radius: 30;"
        );
        toggleBar.setPadding(new Insets(5));
        toggleBar.setMaxWidth(340);

        // ══════════════════════════════════════════════════
        //  EMPLOYEE PANEL
        // ══════════════════════════════════════════════════

        // Header — Aleo BoldItalic, reduced bottom margin
        Label empTitle = new Label("Time - in");
        empTitle.setFont(Font.loadFont("file:assets/fonts/Aleo-BoldItalic.ttf", 34));
        empTitle.setTextFill(Color.WHITE);
        empTitle.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.3), 6, 0, 0, 0);"
        );
        VBox.setMargin(empTitle, new Insets(0, 0, 4, 0)); // tighten gap below title

        // Input field — Aleo Regular
        TextField nameField = styledField("Enter Name");

        // ── Restricted Date Picker ────────────────────────

        // "Shift Date" label — Aleo Bold, white, bigger
        Label dateLabel = new Label("Shift Date");
        dateLabel.setTextFill(Color.WHITE);
        dateLabel.setFont(Font.loadFont("file:assets/fonts/Aleo-Bold.ttf", 16));
        dateLabel.setPadding(new Insets(0, 0, 2, 4));

        ComboBox<String> monthBox = new ComboBox<>();
        for (Month m : new Month[]{Month.APRIL, Month.MAY, Month.JUNE}) {
            String name = m.name().charAt(0) + m.name().substring(1).toLowerCase();
            monthBox.getItems().add(name);
        }
        monthBox.setPromptText("Month");
        monthBox.setPrefHeight(52);
        styleComboBox(monthBox);

        ComboBox<Integer> dayBox = new ComboBox<>();
        for (int d = 1; d <= 31; d++) dayBox.getItems().add(d);
        dayBox.setPromptText("Day");
        dayBox.setPrefHeight(52);
        styleComboBox(dayBox);

        ComboBox<Integer> yearBox = new ComboBox<>();
        yearBox.getItems().add(2026);
        yearBox.setValue(2026);
        yearBox.setPrefHeight(52);
        styleComboBox(yearBox);

        HBox.setHgrow(monthBox, Priority.ALWAYS);
        HBox.setHgrow(dayBox,   Priority.ALWAYS);
        HBox.setHgrow(yearBox,  Priority.ALWAYS);
        monthBox.setMaxWidth(Double.MAX_VALUE);
        dayBox.setMaxWidth(Double.MAX_VALUE);
        yearBox.setMaxWidth(Double.MAX_VALUE);

        HBox dateRow = new HBox(10, monthBox, dayBox, yearBox);
        dateRow.setAlignment(Pos.CENTER_LEFT);
        dateRow.setPrefWidth(455);
        dateRow.setMaxWidth(455);

        VBox dateGroup = new VBox(4, dateLabel, dateRow);
        dateGroup.setAlignment(Pos.CENTER_LEFT);

        // ── Smart Time Fields — Aleo Regular ─────────────
        TextField timeInField  = buildTimeField("Time-in");
        TextField timeOutField = buildTimeField("Time-out");

        HBox.setHgrow(timeInField,  Priority.ALWAYS);
        HBox.setHgrow(timeOutField, Priority.ALWAYS);
        timeInField.setMaxWidth(Double.MAX_VALUE);
        timeOutField.setMaxWidth(Double.MAX_VALUE);

        HBox timeRow = new HBox(14, timeInField, timeOutField);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        timeRow.setPrefWidth(455);
        timeRow.setMaxWidth(455);

        // ── Employee error label — Aleo Italic ───────────
        Label empError = errorLabel();

        Button empConfirm = confirmButton();
        VBox.setMargin(empConfirm, new Insets(-35, 0, 0, 0));

        empConfirm.setOnAction(e -> {
            String enteredName = nameField.getText();

            if (!isValidTimeFormat(timeInField.getText())) {
                empError.setText("Time-in must be a valid time, e.g. 09:30 AM");
                empError.setVisible(true);
                return;
            }
            if (!isValidTimeFormat(timeOutField.getText())) {
                empError.setText("Time-out must be a valid time, e.g. 05:00 PM");
                empError.setVisible(true);
                return;
            }

            if (auth_util.authenticateEmployee(enteredName)) {
                empError.setVisible(false);
                openPosWindow(stage);
            } else {
                empError.setText("Employee not found. Check your name.");
                empError.setVisible(true);
            }
        });

        // VBox spacing reduced from 22 → 14 to tighten gaps after the title
        VBox employeePanel = new VBox(14,
            empTitle,
            nameField,
            dateGroup,
            timeRow,
            empError,
            empConfirm
        );
        employeePanel.setAlignment(Pos.CENTER_LEFT);

        // ══════════════════════════════════════════════════
        //  MANAGER PANEL
        // ══════════════════════════════════════════════════

        // Header — Aleo BoldItalic, reduced bottom margin
        Label mgrTitle = new Label("Log - in");
        mgrTitle.setFont(Font.loadFont("file:assets/fonts/Aleo-BoldItalic.ttf", 34));
        mgrTitle.setTextFill(Color.WHITE);
        mgrTitle.setStyle(
            "-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.3), 6, 0, 0, 0);"
        );
        VBox.setMargin(mgrTitle, new Insets(0, 0, 4, 0)); // tighten gap below title

        // Input fields — Aleo Regular
        TextField     mgrName = styledField("Enter Name");
        PasswordField mgrPass = new PasswordField();
        mgrPass.setPromptText("Enter Password");
        styleFieldBase(mgrPass);

        // Error — Aleo Italic
        Label mgrError = errorLabel();

        Button mgrConfirm = confirmButton();
        VBox.setMargin(mgrConfirm, new Insets(-35, 0, 0, 0));

        mgrConfirm.setOnAction(e -> {
            String enteredName = mgrName.getText();
            String enteredPass = mgrPass.getText();
            if (auth_util.authenticateManager(enteredName, enteredPass)) {
                mgrError.setVisible(false);
                showConfirmedModal(stage);
            } else {
                mgrError.setText("Invalid name or password.");
                mgrError.setVisible(true);
            }
        });

        // VBox spacing reduced from 22 → 14 to tighten gaps after the title
        VBox managerPanel = new VBox(14, mgrTitle, mgrName, mgrPass, mgrError, mgrConfirm);
        managerPanel.setAlignment(Pos.CENTER_LEFT);

        // ══════════════════════════════════════════════════
        //  MAIN CONTAINER
        // ══════════════════════════════════════════════════
        VBox container = new VBox(26, toggleBar, employeePanel);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(44, 48, 44, 48));
        container.setStyle(
            "-fx-background-color: #68222A;" +
            "-fx-background-radius: 28;"
        );
        container.setPrefWidth(560);
        container.setMinWidth(560);
        container.setMaxWidth(560);

        VBox.setMargin(toggleBar, new Insets(0, 0, 6, 0));

        container.setLayoutX(160);
        container.setLayoutY(110);

        btnEmployee.setOnAction(e -> {
            if (!btnEmployee.isSelected()) btnEmployee.setSelected(true);
            styleToggle(btnEmployee, true);
            styleToggle(btnManager, false);
            switchPanel(container, employeePanel);
        });

        btnManager.setOnAction(e -> {
            if (!btnManager.isSelected()) btnManager.setSelected(true);
            styleToggle(btnManager, true);
            styleToggle(btnEmployee, false);
            switchPanel(container, managerPanel);
        });

        Pane root = new Pane();
        root.getChildren().addAll(background, container);

        Scene scene = new Scene(root, screenW, screenH);
        stage.setTitle("Cafe Saburo");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.show();
    }

    // ══════════════════════════════════════════════════════
    //  SMART TIME FIELD — Aleo Regular
    // ══════════════════════════════════════════════════════
    private TextField buildTimeField(String placeholder) {
        TextField tf = new TextField();
        tf.setPromptText(placeholder + "  e.g. 09:30 AM");
        tf.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-font-size: 15px;" +
            "-fx-padding: 12 16 12 16;" +
            "-fx-font-family: 'Aleo';"
        );
        tf.setPrefHeight(52);

        final String[] buf = {"", "", "", "", "AM"};
        final boolean[] programmatic = {false};

        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (programmatic[0]) return;
            programmatic[0] = true;

            String rendered = renderTime(buf);
            if (newVal.length() < rendered.length()) {
                handleBackspace(buf);
            } else {
                char typed = 0;
                for (int i = 0; i < newVal.length(); i++) {
                    if (i >= rendered.length() || newVal.charAt(i) != rendered.charAt(i)) {
                        typed = newVal.charAt(i);
                        break;
                    }
                }
                if (typed != 0) handleChar(buf, typed);
            }

            String next = renderTime(buf);
            tf.setText(next);
            tf.positionCaret(next.length());
            programmatic[0] = false;
        });

        tf.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                programmatic[0] = true;
                autoPad(buf);
                tf.setText(renderTime(buf));
                programmatic[0] = false;
            }
        });

        return tf;
    }

    private void handleChar(String[] buf, char c) {
        char upper = Character.toUpperCase(c);
        if (upper == 'A') { buf[4] = "AM"; return; }
        if (upper == 'P') { buf[4] = "PM"; return; }
        if (!Character.isDigit(c)) return;
        int digit = c - '0';

        if (buf[0].isEmpty()) {
            if (digit > 1) { buf[0] = "0"; buf[1] = String.valueOf(digit); }
            else           { buf[0] = String.valueOf(digit); }
            return;
        }
        if (buf[1].isEmpty()) {
            int firstHour = Integer.parseInt(buf[0]);
            if      (firstHour == 1 && digit <= 2) { buf[1] = String.valueOf(digit); }
            else if (firstHour == 0 && digit >= 1) { buf[1] = String.valueOf(digit); }
            else if (firstHour == 0 && digit == 0) { buf[0] = ""; buf[1] = ""; }
            return;
        }
        if (buf[2].isEmpty()) {
            if (digit > 5) { buf[2] = "0"; buf[3] = String.valueOf(digit); }
            else           { buf[2] = String.valueOf(digit); }
            return;
        }
        if (buf[3].isEmpty()) {
            buf[3] = String.valueOf(digit);
        }
    }

    private void handleBackspace(String[] buf) {
        if (!buf[3].isEmpty()) { buf[3] = ""; return; }
        if (!buf[2].isEmpty()) { buf[2] = ""; return; }
        if (!buf[1].isEmpty()) { buf[1] = ""; return; }
        if (!buf[0].isEmpty()) { buf[0] = ""; }
    }

    private void autoPad(String[] buf) {
        if (!buf[0].isEmpty() && buf[1].isEmpty()) {
            int h = Integer.parseInt(buf[0]);
            if (h == 0) { buf[0] = ""; }
            else        { buf[0] = "0"; buf[1] = String.valueOf(h); }
        }
        if (!buf[2].isEmpty() && buf[3].isEmpty()) {
            buf[3] = "0";
        }
    }

    private String renderTime(String[] buf) {
        String h1 = buf[0].isEmpty() ? "_" : buf[0];
        String h2 = buf[1].isEmpty() ? "_" : buf[1];
        String m1 = buf[2].isEmpty() ? "_" : buf[2];
        String m2 = buf[3].isEmpty() ? "_" : buf[3];
        return h1 + h2 + ":" + m1 + m2 + " " + buf[4];
    }

    private boolean isValidTimeFormat(String text) {
        if (text == null) return false;
        return text.matches("^(0[1-9]|1[0-2]):[0-5][0-9] (AM|PM)$");
    }

    // ── Modal ─────────────────────────────────────────────
    private void showConfirmedModal(Stage ownerStage) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(ownerStage);
        modal.setTitle("Access Granted");
        modal.setResizable(false);

        Label icon = new Label("✔");
        icon.setFont(Font.loadFont("file:assets/fonts/Aleo-Bold.ttf", 48));
        icon.setTextFill(Color.web("#2ecc40"));

        // Modal message — Aleo SemiBold
        Label msg = new Label("Credentials confirmed!");
        msg.setFont(Font.loadFont("file:assets/fonts/Aleo-SemiBold.ttf", 20));
        msg.setTextFill(Color.WHITE);

        // Modal button — Aleo Bold
        Button okBtn = new Button("Continue");
        okBtn.setPrefWidth(200);
        okBtn.setPrefHeight(44);
        okBtn.setFont(Font.loadFont("file:assets/fonts/Aleo-Bold.ttf", 15));
        okBtn.setStyle(
            "-fx-background-color: #2ecc40;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-family: 'Aleo';" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;"
        );
        okBtn.setOnAction(e -> {
            modal.close();
            openPosWindow(ownerStage);
        });

        VBox layout = new VBox(18, icon, msg, okBtn);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40, 60, 40, 60));
        layout.setStyle("-fx-background-color: #68222A; -fx-background-radius: 16;");

        Scene modalScene = new Scene(layout);
        modalScene.setFill(Color.TRANSPARENT);
        modal.setScene(modalScene);
        modal.showAndWait();
    }

    private void openPosWindow(Stage authStage) {
        Stage posStage = new Stage();
        pos_records_window_ui posUI = new pos_records_window_ui();
        posUI.start(posStage);
        authStage.close();
    }

    // Error label — Aleo Italic
    private Label errorLabel() {
        Label lbl = new Label();
        lbl.setTextFill(Color.web("#ff6b6b"));
        lbl.setFont(Font.loadFont("file:assets/fonts/Aleo-Italic.ttf", 13));
        lbl.setVisible(false);
        lbl.setWrapText(true);
        return lbl;
    }

    // ── Helpers ───────────────────────────────────────────

    // ComboBox — Aleo Regular via CSS
    private <T> void styleComboBox(ComboBox<T> box) {
        box.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: transparent;" +
            "-fx-font-size: 14px;" +
            "-fx-font-family: 'Aleo';" +
            "-fx-padding: 0 8 0 8;"
        );
    }

    private void switchPanel(VBox container, VBox newPanel) {
        VBox current = (VBox) container.getChildren().get(1);
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

    // Input field — Aleo Regular
    private TextField styledField(String placeholder) {
        TextField f = new TextField();
        f.setPromptText(placeholder);
        styleFieldBase(f);
        return f;
    }

    private void styleFieldBase(Control f) {
        f.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 12 16 12 16;" +
            "-fx-font-size: 15px;" +
            "-fx-font-family: 'Aleo';"
        );
        f.setPrefWidth(455);
        f.setPrefHeight(52);
    }

    // Button — Aleo Bold
    private Button confirmButton() {
        Button btn = new Button("Confirm");
        btn.setFont(Font.loadFont("file:assets/fonts/Aleo-Bold.ttf", 16));
        btn.setPrefWidth(455);
        btn.setPrefHeight(52);
        btn.setStyle(
            "-fx-background-color: #2ecc40;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-family: 'Aleo';" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;"
        );
        return btn;
    }

    // Toggle buttons — Aleo Bold (active) / Aleo Medium (inactive)
    private void styleToggle(ToggleButton btn, boolean active) {
        if (active) {
            btn.setFont(Font.loadFont("file:assets/fonts/Aleo-Bold.ttf", 14));
            btn.setStyle(
                "-fx-background-color: #68222A;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 14px;" +
                "-fx-font-family: 'Aleo';" +
                "-fx-background-radius: 30;" +
                "-fx-padding: 10 36 10 36;" +
                "-fx-cursor: hand;"
            );
        } else {
            btn.setFont(Font.loadFont("file:assets/fonts/Aleo-Medium.ttf", 14));
            btn.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #aaaaaa;" +
                "-fx-font-size: 14px;" +
                "-fx-font-family: 'Aleo';" +
                "-fx-background-radius: 30;" +
                "-fx-padding: 10 36 10 36;" +
                "-fx-cursor: hand;"
            );
        }
    }
}