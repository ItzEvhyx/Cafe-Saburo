package frontend;

import backend.payments_util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class payments_contents {

    // ══════════════════════════════════════════════════════
    //  LAYOUT CONSTANTS
    // ══════════════════════════════════════════════════════
    private static final double TOP_PADDING  = 20;
    private static final double SIDE_PADDING = 24;
    private static final double HEADER_H     = 56;

    private static final double COL_PAYMENT_ID = 0.20;
    private static final double COL_ORDER_ID   = 0.25;
    private static final double COL_METHOD     = 0.30;
    private static final double COL_AMOUNT     = 0.25;

    private static final double ROW_H        = 44;
    private static final double HEADER_ROW_H = 46;
    private static final double CHECKBOX_COL = 48;

    // ── Fixed modal dimensions ────────────────────────────
    private static final double MODAL_W = 440;
    private static final double MODAL_H = 260;

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
    private boolean        archiveMode = false;
    private Pane           root;
    private StackPane      stackRoot;
    private ScrollPane     tableScroll;
    private List<String[]> cachedRows  = new ArrayList<>();
    private Set<String>    selectedIds = new HashSet<>();

    private Label archiveBtn;
    private Label analyticsBtn;
    private Label archiveAllBtn;
    private Label confirmBtn;
    private Label activeTabBtn;
    private Label archivedTabBtn;
    private Label deleteBtn;
    private Label exportCsvBtn;

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

    public payments_contents(double totalW, double totalH, Connection conn) {
        this.totalW = totalW;
        this.totalH = totalH;
        this.conn   = conn;
        loadFonts();
    }

    // ══════════════════════════════════════════════════════
    //  PUBLIC LIVE-UPDATE API
    // ══════════════════════════════════════════════════════
    public void prependPayment(String paymentId, String orderId,
                                String paymentMethod, String amount) {
        String[] newRow = new String[]{
            paymentId,
            orderId,
            paymentMethod != null ? paymentMethod : "Cash",
            amount        != null ? amount         : "0.00"
        };
        if (root != null && currentTab.equals("active")) {
            cachedRows.add(0, newRow);
            rebuildTable();
        }
    }

    // ══════════════════════════════════════════════════════
    //  ANALYTICS MODAL
    // ══════════════════════════════════════════════════════
    private void openAnalyticsModal() {
        if (conn == null) return;
        try { if (conn.isClosed()) return; } catch (Exception e) { return; }

        Pane overlay = new Pane();
        overlay.setPrefWidth(totalW);
        overlay.setPrefHeight(totalH);
        overlay.setMinHeight(totalH);
        overlay.setMaxHeight(totalH);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.50);");

        VBox card = new VBox(0);
        card.setMaxWidth(860);
        card.setMaxHeight(totalH * 0.88);
        card.setMinWidth(600);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 32, 0, 0, 8);"
        );

        HBox cardHeader = new HBox(10);
        cardHeader.setPadding(new Insets(20, 24, 16, 24));
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        cardHeader.setStyle(
            "-fx-background-color: " + HEADER_BG + ";" +
            "-fx-background-radius: 16 16 0 0;" +
            "-fx-border-color: transparent transparent " + TABLE_BORDER + " transparent;" +
            "-fx-border-width: 0 0 1.5 0;"
        );

        FontIcon chartIcon = new FontIcon(FontAwesomeSolid.CHART_BAR);
        chartIcon.setIconSize(18);
        chartIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));

        Label modalTitle = new Label("Payment Analytics");
        modalTitle.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 20px;" +
            "-fx-font-weight: 800;" +
            "-fx-text-fill: " + ACCENT + ";"
        );

        Label subtitle = new Label("Select an operation below to view results");
        subtitle.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #AA6670;" +
            "-fx-padding: 4 0 0 10;"
        );

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Label closeBtn = new Label();
        FontIcon xIcon = new FontIcon(FontAwesomeSolid.TIMES);
        xIcon.setIconSize(13);
        xIcon.setIconColor(javafx.scene.paint.Color.web("#555555"));
        closeBtn.setGraphic(xIcon);
        closeBtn.setCursor(javafx.scene.Cursor.HAND);
        closeBtn.setPrefWidth(30);
        closeBtn.setPrefHeight(30);
        closeBtn.setAlignment(Pos.CENTER);
        closeBtn.setStyle("-fx-background-color: #E9ECEF; -fx-background-radius: 6;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
            "-fx-background-color: #DEE2E6; -fx-background-radius: 6;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
            "-fx-background-color: #E9ECEF; -fx-background-radius: 6;"));
        closeBtn.setOnMouseClicked(e -> stackRoot.getChildren().remove(overlay));

        cardHeader.getChildren().addAll(chartIcon, modalTitle, subtitle, hSpacer, closeBtn);

        HBox pillRow1 = new HBox(8);
        HBox pillRow2 = new HBox(8);
        pillRow1.setAlignment(Pos.CENTER_LEFT);
        pillRow2.setAlignment(Pos.CENTER_LEFT);

        VBox pillSection = new VBox(8, pillRow1, pillRow2);
        pillSection.setPadding(new Insets(14, 24, 14, 24));
        pillSection.setStyle(
            "-fx-background-color: #FAFAFA;" +
            "-fx-border-color: transparent transparent #F0D8DA transparent;" +
            "-fx-border-width: 0 0 1 0;"
        );

        VBox resultArea = new VBox(0);
        VBox.setVgrow(resultArea, Priority.ALWAYS);

        ScrollPane resultScroll = new ScrollPane(resultArea);
        resultScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        resultScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        resultScroll.setFitToWidth(true);
        resultScroll.setStyle(
            "-fx-background: transparent;" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;" +
            "-fx-padding: 0;"
        );
        VBox.setVgrow(resultScroll, Priority.ALWAYS);

        VBox placeholder = new VBox();
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setPadding(new Insets(40, 24, 40, 24));
        FontIcon placeholderIcon = new FontIcon(FontAwesomeSolid.HAND_POINT_UP);
        placeholderIcon.setIconSize(28);
        placeholderIcon.setIconColor(javafx.scene.paint.Color.web("#DDBBBD"));
        Label placeholderLbl = new Label("Pick an operation above to see results.");
        placeholderLbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #CCAAAC;" +
            "-fx-padding: 10 0 0 0;"
        );
        placeholder.getChildren().addAll(placeholderIcon, placeholderLbl);
        resultArea.getChildren().add(placeholder);

        Label[] activePill = { null };

        for (int i = 0; i < payments_util.OPERATIONS.length; i++) {
            final String opKey   = payments_util.OPERATIONS[i][0];
            final String opLabel = payments_util.OPERATIONS[i][1];

            Label pill = new Label(opLabel);
            pill.setCursor(javafx.scene.Cursor.HAND);
            pill.setPrefHeight(30);
            pill.setPadding(new Insets(0, 14, 0, 14));
            pill.setAlignment(Pos.CENTER);
            pill.setStyle(pillStyle(false));

            pill.setOnMouseEntered(e -> {
                if (activePill[0] != pill) pill.setStyle(pillHoverStyle());
            });
            pill.setOnMouseExited(e -> {
                if (activePill[0] != pill) pill.setStyle(pillStyle(false));
            });
            pill.setOnMouseClicked(e -> {
                if (activePill[0] != null) activePill[0].setStyle(pillStyle(false));
                activePill[0] = pill;
                pill.setStyle(pillStyle(true));

                resultArea.getChildren().clear();
                List<String[]> data = payments_util.runOperation(conn, opKey);
                if (data == null || data.size() <= 1) {
                    VBox noData = new VBox();
                    noData.setAlignment(Pos.CENTER);
                    noData.setPadding(new Insets(32));
                    Label noDataLbl = new Label("No data available.");
                    noDataLbl.setStyle(
                        "-fx-font-family: '" + FONT_FAMILY + "';" +
                        "-fx-font-size: 13px;" +
                        "-fx-text-fill: #AAAAAA;"
                    );
                    noData.getChildren().add(noDataLbl);
                    resultArea.getChildren().add(noData);
                } else {
                    resultArea.getChildren().add(buildResultTable(data));
                }
            });

            if (i < 5) pillRow1.getChildren().add(pill);
            else        pillRow2.getChildren().add(pill);
        }

        card.getChildren().addAll(cardHeader, pillSection, resultScroll);

        StackPane centred = new StackPane(card);
        centred.setPrefWidth(totalW);
        centred.setPrefHeight(totalH);
        centred.setAlignment(Pos.CENTER);
        overlay.getChildren().add(centred);
        stackRoot.getChildren().add(overlay);
    }

    // ══════════════════════════════════════════════════════
    //  RESULT TABLE BUILDER
    // ══════════════════════════════════════════════════════
    private VBox buildResultTable(List<String[]> data) {
        VBox wrapper = new VBox(0);
        wrapper.setPadding(new Insets(16, 20, 20, 20));

        int colCount  = data.get(0).length;
        int totalRows = data.size();
        double pct    = 100.0 / colCount;

        GridPane grid = new GridPane();
        grid.setHgap(0);
        grid.setVgap(0);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.setStyle(
            "-fx-border-color: " + TABLE_BORDER + ";" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 10;" +
            "-fx-background-color: white;" +
            "-fx-background-radius: 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10, 0, 0, 3);"
        );

        for (int c = 0; c < colCount; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(pct);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }

        for (int r = 0; r < totalRows; r++) {
            String[] rowData  = data.get(r);
            boolean  isHeader = (r == 0);
            boolean  isFooter = (r == totalRows - 1) && (totalRows > 1);
            boolean  isLast   = (r == totalRows - 1);

            String bg = isHeader ? HEADER_BG
                      : isFooter ? "#F5E8EA"
                      : (r % 2 == 1 ? ROW_WHITE_BG : ROW_ALT_BG);

            String borderBottom = isLast ? "0" : "1";

            for (int c = 0; c < colCount; c++) {
                String val  = (c < rowData.length && rowData[c] != null) ? rowData[c] : "";
                Label  cell = new Label(val);
                cell.setMaxWidth(Double.MAX_VALUE);
                cell.setPrefHeight(38);
                cell.setPadding(new Insets(0, 8, 0, 12));
                cell.setAlignment(Pos.CENTER_LEFT);
                cell.setWrapText(false);

                String tlR = (r == 0     && c == 0)            ? "10" : "0";
                String trR = (r == 0     && c == colCount - 1) ? "10" : "0";
                String brR = (isLast     && c == colCount - 1) ? "10" : "0";
                String blR = (isLast     && c == 0)            ? "10" : "0";

                String rightBorderColor = (c < colCount - 1) ? TABLE_BORDER : "transparent";
                String rightBorderW     = (c < colCount - 1) ? "0.75" : "0";

                cell.setStyle(
                    "-fx-background-color: " + bg + ";" +
                    "-fx-background-radius: " + tlR + " " + trR + " " + brR + " " + blR + ";" +
                    "-fx-border-color: transparent " + rightBorderColor + " " +
                        (isLast ? "transparent" : TABLE_BORDER) + " transparent;" +
                    "-fx-border-width: 0 " + rightBorderW + " " + borderBottom + " 0;" +
                    "-fx-font-family: '" + FONT_FAMILY + "';" +
                    "-fx-font-size: 13px;" +
                    "-fx-font-weight: " + (isHeader || isFooter ? "bold" : "normal") + ";" +
                    "-fx-text-fill: " + (isHeader || isFooter ? ACCENT : "#333333") + ";"
                );

                grid.add(cell, c, r);
            }
        }

        wrapper.getChildren().add(grid);
        return wrapper;
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

        double btnH     = 36;
        double btnY     = TOP_PADDING + 10;
        double iconW    = 36;
        double gap      = 8;
        double tabW     = 90;
        double archAllW = 100;
        double confirmW = 90;
        double csvW     = 120;

        Label title = new Label("Payment History");
        title.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 36px;" +
            "-fx-font-weight: 800;" +
            "-fx-text-fill: " + ACCENT + ";"
        );

        FontIcon analyticsIcon = new FontIcon(FontAwesomeSolid.CHART_BAR);
        analyticsIcon.setIconSize(15);
        analyticsIcon.setIconColor(javafx.scene.paint.Color.web("#155724"));
        analyticsBtn = new Label();
        analyticsBtn.setGraphic(analyticsIcon);
        analyticsBtn.setCursor(javafx.scene.Cursor.HAND);
        analyticsBtn.setStyle(analyticsBtnStyle(false));
        analyticsBtn.setPrefHeight(btnH);
        analyticsBtn.setPrefWidth(iconW);
        analyticsBtn.setAlignment(Pos.CENTER);
        analyticsBtn.setOnMouseEntered(e -> analyticsBtn.setStyle(analyticsBtnStyle(true)));
        analyticsBtn.setOnMouseExited(e  -> analyticsBtn.setStyle(analyticsBtnStyle(false)));
        analyticsBtn.setOnMouseClicked(e -> openAnalyticsModal());

        FontIcon boxIcon = new FontIcon(FontAwesomeSolid.ARCHIVE);
        boxIcon.setIconSize(15);
        boxIcon.setIconColor(javafx.scene.paint.Color.web(ACCENT));
        archiveBtn = new Label();
        archiveBtn.setGraphic(boxIcon);
        archiveBtn.setCursor(javafx.scene.Cursor.HAND);
        archiveBtn.setStyle(archiveBtnStyle(false));
        archiveBtn.setPrefHeight(btnH);
        archiveBtn.setPrefWidth(iconW);
        archiveBtn.setAlignment(Pos.CENTER);
        archiveBtn.setOnMouseEntered(e -> archiveBtn.setStyle(archiveBtnStyle(archiveMode)));
        archiveBtn.setOnMouseExited(e  -> archiveBtn.setStyle(archiveBtnStyle(archiveMode)));
        archiveBtn.setOnMouseClicked(e -> toggleArchiveMode());

        HBox titleRow = new HBox(gap, title, analyticsBtn, archiveBtn);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.setLayoutX(SIDE_PADDING);
        titleRow.setLayoutY(TOP_PADDING);
        titleRow.setPrefHeight(HEADER_H);

        double deleteX      = totalW - SIDE_PADDING - iconW;
        double exportCsvX   = deleteX - gap - csvW;
        double archivedTabX = exportCsvX - gap - tabW;
        double activeTabX   = archivedTabX - gap - tabW;
        double confirmX     = activeTabX - gap - confirmW;
        double archAllX     = confirmX - gap - archAllW;

        deleteBtn = new Label();
        FontIcon trashIcon = new FontIcon(FontAwesomeSolid.TRASH_ALT);
        trashIcon.setIconSize(15);
        trashIcon.setIconColor(javafx.scene.paint.Color.web("#721C24"));
        deleteBtn.setGraphic(trashIcon);
        deleteBtn.setCursor(javafx.scene.Cursor.HAND);
        deleteBtn.setStyle(deleteBtnStyle(false));
        deleteBtn.setLayoutX(deleteX);
        deleteBtn.setLayoutY(btnY);
        deleteBtn.setPrefHeight(btnH);
        deleteBtn.setPrefWidth(iconW);
        deleteBtn.setAlignment(Pos.CENTER);
        deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(deleteBtnStyle(true)));
        deleteBtn.setOnMouseExited(e  -> deleteBtn.setStyle(deleteBtnStyle(false)));
        deleteBtn.setOnMouseClicked(e ->
            stackRoot.getChildren().add(buildConfirmModal(
                "Payments (" + currentTab + ")",
                "This will permanently remove all payments in this view.\nThis action cannot be undone.",
                () -> {
                    payments_util.hardDeleteAll(conn, currentTab);
                    cachedRows.clear();
                    selectedIds.clear();
                    rebuildTable();
                }
            ))
        );

        exportCsvBtn = new Label("Export CSV");
        FontIcon csvIcon = new FontIcon(FontAwesomeSolid.FILE_DOWNLOAD);
        csvIcon.setIconSize(13);
        csvIcon.setIconColor(javafx.scene.paint.Color.web("#155724"));
        exportCsvBtn.setGraphic(csvIcon);
        exportCsvBtn.setGraphicTextGap(6);
        exportCsvBtn.setCursor(javafx.scene.Cursor.HAND);
        exportCsvBtn.setStyle(exportCsvBtnStyle(false));
        exportCsvBtn.setLayoutX(exportCsvX);
        exportCsvBtn.setLayoutY(btnY);
        exportCsvBtn.setPrefHeight(btnH);
        exportCsvBtn.setPrefWidth(csvW);
        exportCsvBtn.setAlignment(Pos.CENTER);
        exportCsvBtn.setOnMouseEntered(e -> exportCsvBtn.setStyle(exportCsvBtnStyle(true)));
        exportCsvBtn.setOnMouseExited(e  -> exportCsvBtn.setStyle(exportCsvBtnStyle(false)));
        exportCsvBtn.setOnMouseClicked(e -> {
            Stage stage = null;
            try { stage = (Stage) root.getScene().getWindow(); } catch (Exception ignored) {}
            payments_util.exportCsv(stage, currentTab, cachedRows);
        });

        activeTabBtn = buildTabLabel("Active", true);
        activeTabBtn.setLayoutX(activeTabX);
        activeTabBtn.setLayoutY(btnY);

        archivedTabBtn = buildTabLabel("Archived", false);
        archivedTabBtn.setLayoutX(archivedTabX);
        archivedTabBtn.setLayoutY(btnY);

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

        archiveAllBtn = new Label("Archive All");
        archiveAllBtn.setCursor(javafx.scene.Cursor.HAND);
        archiveAllBtn.setPrefWidth(archAllW);
        archiveAllBtn.setPrefHeight(btnH);
        archiveAllBtn.setAlignment(Pos.CENTER);
        archiveAllBtn.setStyle(archiveAllBtnStyle(false));
        archiveAllBtn.setVisible(false);
        archiveAllBtn.setLayoutX(archAllX);
        archiveAllBtn.setLayoutY(btnY);
        archiveAllBtn.setOnMouseEntered(e -> archiveAllBtn.setStyle(archiveAllBtnStyle(true)));
        archiveAllBtn.setOnMouseExited(e  -> archiveAllBtn.setStyle(archiveAllBtnStyle(false)));
        archiveAllBtn.setOnMouseClicked(e -> {
            selectedIds.clear();
            for (String[] row : cachedRows) selectedIds.add(row[0]);
            rebuildTable();
        });

        confirmBtn = new Label("Confirm");
        confirmBtn.setCursor(javafx.scene.Cursor.HAND);
        confirmBtn.setPrefWidth(confirmW);
        confirmBtn.setPrefHeight(btnH);
        confirmBtn.setAlignment(Pos.CENTER);
        confirmBtn.setStyle(confirmBtnStyle(false));
        confirmBtn.setVisible(false);
        confirmBtn.setLayoutX(confirmX);
        confirmBtn.setLayoutY(btnY);
        confirmBtn.setOnMouseEntered(e -> confirmBtn.setStyle(confirmBtnStyle(true)));
        confirmBtn.setOnMouseExited(e  -> confirmBtn.setStyle(confirmBtnStyle(false)));
        confirmBtn.setOnMouseClicked(e -> {
            if (selectedIds.isEmpty()) return;
            if (currentTab.equals("active")) payments_util.archiveSelected(conn, selectedIds);
            else                             payments_util.restoreSelected(conn, selectedIds);
            selectedIds.clear();
            archiveMode = false;
            updateArchiveBtnIcon();
            archiveAllBtn.setVisible(false);
            confirmBtn.setVisible(false);
            archiveBtn.setStyle(archiveBtnStyle(false));
            cachedRows = payments_util.fetchPayments(conn, currentTab);
            rebuildTable();
        });

        double tableY = TOP_PADDING + HEADER_H + 10;
        double tableW = totalW - SIDE_PADDING * 2;
        double tableH = totalH - tableY - SIDE_PADDING;

        cachedRows  = payments_util.fetchPayments(conn, "active");
        tableScroll = buildScrollPane(tableW, tableH, tableY);

        root.getChildren().addAll(
            titleRow, archiveAllBtn, confirmBtn,
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
        archiveMode = false;
        selectedIds.clear();

        updateArchiveBtnIcon();
        archiveAllBtn.setText(tab.equals("archived") ? "Restore All" : "Archive All");
        archiveAllBtn.setVisible(false);
        confirmBtn.setVisible(false);
        archiveBtn.setStyle(archiveBtnStyle(false));

        activeTabBtn.setStyle(tabBtnStyle(tab.equals("active")));
        archivedTabBtn.setStyle(tabBtnStyle(tab.equals("archived")));
        cachedRows = payments_util.fetchPayments(conn, tab);
        rebuildTable();
    }

    private Label buildTabLabel(String text, boolean selected) {
        Label lbl = new Label(text);
        lbl.setCursor(javafx.scene.Cursor.HAND);
        lbl.setPrefWidth(90);
        lbl.setPrefHeight(36);
        lbl.setAlignment(Pos.CENTER);
        lbl.setStyle(tabBtnStyle(selected));
        return lbl;
    }

    // ══════════════════════════════════════════════════════
    //  CONFIRMATION MODAL — fixed size
    // ══════════════════════════════════════════════════════
    private Pane buildConfirmModal(String context, String subMessage, Runnable onConfirm) {
        Pane overlay = new Pane();
        overlay.setPrefWidth(totalW);
        overlay.setPrefHeight(totalH);
        overlay.setMinWidth(totalW);
        overlay.setMinHeight(totalH);
        overlay.setMaxWidth(totalW);
        overlay.setMaxHeight(totalH);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.45);");

        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(MODAL_W);
        card.setMinWidth(MODAL_W);
        card.setMaxWidth(MODAL_W);
        card.setPrefHeight(MODAL_H);
        card.setMinHeight(MODAL_H);
        card.setMaxHeight(MODAL_H);
        card.setPadding(new Insets(36, 40, 32, 40));
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 14;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0, 0, 6);"
        );

        FontIcon warnIcon = new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
        warnIcon.setIconSize(28);
        warnIcon.setIconColor(javafx.scene.paint.Color.web("#882F39"));

        Label heading = new Label("Are you sure you want to delete\nall entries for " + context + "?");
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

        Label sub = new Label(subMessage);
        sub.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #777777;" +
            "-fx-text-alignment: center;" +
            "-fx-alignment: center;"
        );
        sub.setAlignment(Pos.CENTER);
        sub.setWrapText(true);
        sub.setMaxWidth(MODAL_W - 80);

        Label noBtn = new Label("No, cancel");
        noBtn.setCursor(javafx.scene.Cursor.HAND);
        noBtn.setPrefWidth(140);
        noBtn.setPrefHeight(38);
        noBtn.setAlignment(Pos.CENTER);
        noBtn.setStyle(modalNoBtnStyle(false));
        noBtn.setOnMouseEntered(e -> noBtn.setStyle(modalNoBtnStyle(true)));
        noBtn.setOnMouseExited(e  -> noBtn.setStyle(modalNoBtnStyle(false)));
        noBtn.setOnMouseClicked(e -> stackRoot.getChildren().remove(overlay));

        Label yesBtn = new Label("Yes, delete");
        yesBtn.setCursor(javafx.scene.Cursor.HAND);
        yesBtn.setPrefWidth(140);
        yesBtn.setPrefHeight(38);
        yesBtn.setAlignment(Pos.CENTER);
        yesBtn.setStyle(modalYesBtnStyle(false));
        yesBtn.setOnMouseEntered(e -> yesBtn.setStyle(modalYesBtnStyle(true)));
        yesBtn.setOnMouseExited(e  -> yesBtn.setStyle(modalYesBtnStyle(false)));
        yesBtn.setOnMouseClicked(e -> {
            stackRoot.getChildren().remove(overlay);
            onConfirm.run();
        });

        HBox btnRow = new HBox(16, noBtn, yesBtn);
        btnRow.setAlignment(Pos.CENTER);
        card.getChildren().addAll(warnIcon, heading, sub, btnRow);

        StackPane centred = new StackPane(card);
        centred.setPrefWidth(totalW);
        centred.setPrefHeight(totalH);
        centred.setMinWidth(totalW);
        centred.setMinHeight(totalH);
        centred.setMaxWidth(totalW);
        centred.setMaxHeight(totalH);
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
        VBox tableBox = buildTable(tableW, cachedRows);
        ScrollPane sp = new ScrollPane(tableBox);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setFitToWidth(true);
        sp.setPannable(true);
        sp.setStyle(
            "-fx-background: transparent;" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;" +
            "-fx-padding: 0;"
        );
        sp.setPrefWidth(tableW);
        sp.setPrefHeight(tableH);
        sp.setLayoutX(SIDE_PADDING);
        sp.setLayoutY(tableY);
        return sp;
    }

    // ══════════════════════════════════════════════════════
    //  TABLE BUILDER
    // ══════════════════════════════════════════════════════
    private VBox buildTable(double tableW, List<String[]> rows) {
        double dataW = archiveMode ? tableW - CHECKBOX_COL : tableW;

        VBox table = new VBox(0);
        table.setStyle(
            "-fx-border-color: " + TABLE_BORDER + ";" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 10;" +
            "-fx-background-color: white;" +
            "-fx-background-radius: 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 10, 0, 0, 3);"
        );
        table.getChildren().add(buildHeaderRow(tableW, dataW));

        if (rows.isEmpty()) {
            String msg = currentTab.equals("archived")
                ? "No archived payments."
                : "No payments found.";
            Label empty = new Label(msg);
            empty.setStyle(
                "-fx-font-family: '" + FONT_FAMILY + "';" +
                "-fx-font-size: 14px;" +
                "-fx-text-fill: #AAAAAA;" +
                "-fx-padding: 24 0 24 16;"
            );
            table.getChildren().add(empty);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                String[] payment = rows.get(i);
                boolean  isLast  = (i == rows.size() - 1);
                table.getChildren().add(
                    buildDataRow(
                        payment[0], payment[1], payment[2], payment[3],
                        i % 2 == 0 ? ROW_WHITE_BG : ROW_ALT_BG,
                        tableW, dataW, isLast
                    )
                );
            }
        }
        return table;
    }

    private HBox buildHeaderRow(double tableW, double dataW) {
        HBox row = new HBox(0);
        row.setPrefHeight(HEADER_ROW_H);
        row.setStyle(
            "-fx-background-color: " + HEADER_BG + ";" +
            "-fx-background-radius: 10 10 0 0;" +
            "-fx-border-color: transparent transparent " + TABLE_BORDER + " transparent;" +
            "-fx-border-width: 0 0 1.5 0;"
        );
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(
            buildHeaderCell("Payment ID",     dataW * COL_PAYMENT_ID),
            buildColDivider(),
            buildHeaderCell("Order ID",       dataW * COL_ORDER_ID),
            buildColDivider(),
            buildHeaderCell("Payment Method", dataW * COL_METHOD),
            buildColDivider(),
            buildHeaderCell("Amount",         dataW * COL_AMOUNT)
        );
        if (archiveMode) {
            row.getChildren().add(buildColDivider());
            Label cbHeader = buildHeaderCell("", CHECKBOX_COL);
            cbHeader.setAlignment(Pos.CENTER);
            row.getChildren().add(cbHeader);
        }
        return row;
    }

    private Label buildHeaderCell(String text, double width) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(width);
        lbl.setPrefHeight(HEADER_ROW_H);
        lbl.setPadding(new Insets(0, 0, 0, 16));
        lbl.setAlignment(Pos.CENTER_LEFT);
        lbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + ACCENT + ";"
        );
        return lbl;
    }

    private HBox buildDataRow(String paymentId, String orderId, String method,
                               String amount, String bg,
                               double tableW, double dataW, boolean isLast) {
        HBox row = new HBox(0);
        row.setPrefHeight(ROW_H);
        row.setAlignment(Pos.CENTER_LEFT);

        String  bottomRadius = isLast ? "0 0 10 10" : "0";
        String  borderBottom = isLast ? "0" : "1";
        boolean selected     = selectedIds.contains(paymentId);
        String  rowBg        = selected ? "#FDE8EA" : bg;

        row.setStyle(rowStyle(rowBg, bottomRadius, borderBottom));
        row.setOnMouseEntered(e -> {
            if (!selectedIds.contains(paymentId))
                row.setStyle(rowStyle("#F5E8EA", bottomRadius, borderBottom));
        });
        row.setOnMouseExited(e -> {
            String cur = selectedIds.contains(paymentId) ? "#FDE8EA" : bg;
            row.setStyle(rowStyle(cur, bottomRadius, borderBottom));
        });

        row.getChildren().addAll(
            buildTextCell(paymentId, dataW * COL_PAYMENT_ID, true),
            buildColDivider(),
            buildTextCell(orderId,   dataW * COL_ORDER_ID,   false),
            buildColDivider(),
            buildTextCell(method,    dataW * COL_METHOD,     false),
            buildColDivider(),
            buildTextCell(amount,    dataW * COL_AMOUNT,     false)
        );

        if (archiveMode) {
            row.getChildren().add(buildColDivider());
            CheckBox cb = new CheckBox();
            cb.setSelected(selected);
            cb.setStyle("-fx-cursor: hand;");
            cb.setOnAction(e -> {
                if (cb.isSelected()) {
                    selectedIds.add(paymentId);
                    row.setStyle(rowStyle("#FDE8EA", bottomRadius, borderBottom));
                } else {
                    selectedIds.remove(paymentId);
                    row.setStyle(rowStyle(bg, bottomRadius, borderBottom));
                }
            });
            HBox cbCell = new HBox(cb);
            cbCell.setPrefWidth(CHECKBOX_COL);
            cbCell.setPrefHeight(ROW_H);
            cbCell.setAlignment(Pos.CENTER);
            row.getChildren().add(cbCell);
        }
        return row;
    }

    // ══════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════
    private String rowStyle(String bg, String bottomRadius, String borderBottom) {
        return "-fx-background-color: " + bg + ";" +
               "-fx-background-radius: " + bottomRadius + ";" +
               "-fx-border-color: transparent transparent " + TABLE_BORDER + " transparent;" +
               "-fx-border-width: 0 0 " + borderBottom + " 0;";
    }

    private Label buildTextCell(String text, double width, boolean bold) {
        Label lbl = new Label(text != null ? text : "-");
        lbl.setPrefWidth(width);
        lbl.setPrefHeight(ROW_H);
        lbl.setPadding(new Insets(0, 0, 0, 16));
        lbl.setAlignment(Pos.CENTER_LEFT);
        lbl.setStyle(
            "-fx-font-family: '" + FONT_FAMILY + "';" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: " + (bold ? "bold" : "normal") + ";" +
            "-fx-text-fill: #333333;"
        );
        return lbl;
    }

    private Region buildColDivider() {
        Region div = new Region();
        div.setPrefWidth(1.5);
        div.setMinWidth(1.5);
        div.setMaxWidth(1.5);
        div.setStyle("-fx-background-color: " + TABLE_BORDER + "; -fx-opacity: 0.35;");
        VBox.setVgrow(div, Priority.ALWAYS);
        return div;
    }

    private String pillStyle(boolean active) {
        return active
            ? "-fx-background-color: " + ACCENT + ";" +
              "-fx-background-radius: 20;" +
              "-fx-font-family: '" + FONT_FAMILY + "';" +
              "-fx-font-size: 12px;" +
              "-fx-font-weight: bold;" +
              "-fx-text-fill: white;" +
              "-fx-cursor: hand;"
            : "-fx-background-color: white;" +
              "-fx-background-radius: 20;" +
              "-fx-border-color: " + ACCENT + ";" +
              "-fx-border-radius: 20;" +
              "-fx-border-width: 1.5;" +
              "-fx-font-family: '" + FONT_FAMILY + "';" +
              "-fx-font-size: 12px;" +
              "-fx-font-weight: bold;" +
              "-fx-text-fill: " + ACCENT + ";" +
              "-fx-cursor: hand;";
    }

    private String pillHoverStyle() {
        return "-fx-background-color: #F5E8EA;" +
               "-fx-background-radius: 20;" +
               "-fx-border-color: " + ACCENT + ";" +
               "-fx-border-radius: 20;" +
               "-fx-border-width: 1.5;" +
               "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 12px;" +
               "-fx-font-weight: bold;" +
               "-fx-text-fill: " + ACCENT + ";" +
               "-fx-cursor: hand;";
    }

    private String analyticsBtnStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#C3E6CB" : "#D4EDDA") + ";" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: #155724;" +
               "-fx-border-radius: 8;" +
               "-fx-border-width: 1.5;" +
               "-fx-cursor: hand;";
    }

    private String tabBtnStyle(boolean selected) {
        return selected
            ? "-fx-background-color: " + ACCENT + ";" +
              "-fx-background-radius: 8;" +
              "-fx-font-family: '" + FONT_FAMILY + "';" +
              "-fx-font-size: 13px;" +
              "-fx-font-weight: bold;" +
              "-fx-text-fill: white;" +
              "-fx-cursor: hand;"
            : "-fx-background-color: #F5E8EA;" +
              "-fx-background-radius: 8;" +
              "-fx-border-color: " + ACCENT + ";" +
              "-fx-border-radius: 8;" +
              "-fx-border-width: 1.5;" +
              "-fx-font-family: '" + FONT_FAMILY + "';" +
              "-fx-font-size: 13px;" +
              "-fx-font-weight: bold;" +
              "-fx-text-fill: " + ACCENT + ";" +
              "-fx-cursor: hand;";
    }

    private String tabBtnHoverStyle() {
        return "-fx-background-color: #EDD5D8;" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: " + ACCENT + ";" +
               "-fx-border-radius: 8;" +
               "-fx-border-width: 1.5;" +
               "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 13px;" +
               "-fx-font-weight: bold;" +
               "-fx-text-fill: " + ACCENT + ";" +
               "-fx-cursor: hand;";
    }

    private String archiveBtnStyle(boolean active) {
        return "-fx-background-color: " + (active ? "#D4EDDA" : "#F5E8EA") + ";" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: " + (active ? "#155724" : ACCENT) + ";" +
               "-fx-border-radius: 8;" +
               "-fx-border-width: 1.5;" +
               "-fx-cursor: hand;";
    }

    private String archiveAllBtnStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#EDD5D8" : "#F5E8EA") + ";" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: " + ACCENT + ";" +
               "-fx-border-radius: 8;" +
               "-fx-border-width: 1.5;" +
               "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 13px;" +
               "-fx-font-weight: bold;" +
               "-fx-text-fill: " + ACCENT + ";" +
               "-fx-cursor: hand;";
    }

    private String confirmBtnStyle(boolean hovered) {
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

    private String deleteBtnStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#F8D7DA" : "#FDF0F1") + ";" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: #721C24;" +
               "-fx-border-radius: 8;" +
               "-fx-border-width: 1.5;" +
               "-fx-cursor: hand;";
    }

    private String exportCsvBtnStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "#C3E6CB" : "#D4EDDA") + ";" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: #155724;" +
               "-fx-border-radius: 8;" +
               "-fx-border-width: 1.5;" +
               "-fx-font-family: '" + FONT_FAMILY + "';" +
               "-fx-font-size: 13px;" +
               "-fx-font-weight: bold;" +
               "-fx-text-fill: #155724;" +
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