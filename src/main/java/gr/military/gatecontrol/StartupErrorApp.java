package gr.military.gatecontrol;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * Minimal JavaFX error screen shown when Spring Boot fails to start.
 * Populated via static fields by GatecontrolApplication before launch.
 */
public class StartupErrorApp extends Application {

    /** Short user-friendly description (Greek). */
    static String errorSummary = "Η εφαρμογή δεν μπόρεσε να εκκινήσει.";

    /** Full stack-trace detail for the scrollable area. */
    static String errorDetail  = "";

    // ── colours ──────────────────────────────────────────────────────────────
    private static final String BG        = "#0d1117";
    private static final String AMBER     = "#ffb300";
    private static final String RED       = "#ff4444";
    private static final String GREEN     = "#39ff14";
    private static final String GREY_TEXT = "#aaaaaa";
    private static final String PANEL     = "#161b22";

    @Override
    public void start(Stage stage) {

        // ── header ────────────────────────────────────────────────────────────
        Label header = new Label("⚠   ΣΦΑΛΜΑ ΕΚΚΙΝΗΣΗΣ  ⚠");
        header.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
        header.setTextFill(Color.web(AMBER));

        // ── summary ───────────────────────────────────────────────────────────
        Label summary = new Label(errorSummary);
        summary.setFont(Font.font("Consolas", 13));
        summary.setTextFill(Color.web(RED));
        summary.setWrapText(true);
        summary.setMaxWidth(Double.MAX_VALUE);

        // ── detail (scrollable stack-trace) ───────────────────────────────────
        Label detailHeader = new Label("Τεχνικές Λεπτομέρειες:");
        detailHeader.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        detailHeader.setTextFill(Color.web(GREY_TEXT));

        TextArea detailArea = new TextArea(errorDetail);
        detailArea.setEditable(false);
        detailArea.setFont(Font.font("Consolas", 11));
        detailArea.setStyle(
                "-fx-control-inner-background:" + PANEL + ";" +
                "-fx-text-fill:#cccccc;" +
                "-fx-border-color:#333;" +
                "-fx-border-width:1;");
        detailArea.setPrefHeight(220);
        VBox.setVgrow(detailArea, Priority.ALWAYS);

        // ── log-file hint ─────────────────────────────────────────────────────
        Label logHint = new Label("Το αρχείο gatecontrol-error.log αποθηκεύτηκε στον φάκελο εγκατάστασης.");
        logHint.setFont(Font.font("Consolas", 10));
        logHint.setTextFill(Color.web(GREY_TEXT));
        logHint.setWrapText(true);

        // ── buttons ───────────────────────────────────────────────────────────
        Button copyBtn = styledButton("Αντιγραφή", GREEN, BG);
        copyBtn.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(errorDetail);
            Clipboard.getSystemClipboard().setContent(cc);
            copyBtn.setText("✔ Αντιγράφηκε");
        });

        Button closeBtn = styledButton("Κλείσιμο", BG, RED);
        closeBtn.setOnAction(e -> { stage.close(); Platform.exit(); System.exit(1); });

        HBox btnRow = new HBox(10, copyBtn, closeBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        // ── layout ────────────────────────────────────────────────────────────
        VBox root = new VBox(14,
                header, summary,
                detailHeader, detailArea,
                logHint, btnRow);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color:" + BG + ";");

        Scene scene = new Scene(root, 760, 460);
        scene.setFill(Color.web(BG));

        stage.setScene(scene);
        stage.setTitle("GateControl — Σφάλμα Εκκίνησης");
        stage.setMinWidth(560);
        stage.setMinHeight(340);
        applyIcon(stage);
        stage.show();
        stage.toFront();
    }

    private static Button styledButton(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        b.setStyle(
                "-fx-background-color:" + bg + ";" +
                "-fx-text-fill:" + fg + ";" +
                "-fx-border-color:" + fg + ";" +
                "-fx-border-width:1;" +
                "-fx-padding:6 18 6 18;" +
                "-fx-cursor:hand;");
        return b;
    }

    private static void applyIcon(Stage stage) {
        try (java.io.InputStream is = StartupErrorApp.class.getResourceAsStream("/app.ico")) {
            if (is != null) {
                stage.getIcons().clear();
                stage.getIcons().add(new javafx.scene.image.Image(is));
            }
        } catch (Exception ignored) {}
    }
}
