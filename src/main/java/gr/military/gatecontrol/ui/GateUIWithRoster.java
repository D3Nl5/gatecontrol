package gr.military.gatecontrol.ui;

import gr.military.gatecontrol.entity.Movement;
import gr.military.gatecontrol.entity.Person;
import gr.military.gatecontrol.reader.NfcReaderService;
import gr.military.gatecontrol.service.GateAccessResult;
import gr.military.gatecontrol.service.GateAccessService;
import gr.military.gatecontrol.service.MovementService;
import gr.military.gatecontrol.service.PersonService;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Toolkit;
import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.sql.DataSource;
import com.github.sarxos.webcam.Webcam;
import javafx.scene.input.KeyCode;

public class GateUIWithRoster extends Application {

    // ── Services ──────────────────────────────────────────────────────────────
    private static GateAccessService gateService;
    private static MovementService   movementService;
    private static PersonService     personService;
    private static DataSource        backupDataSource;

    // ── NFC phone reader ──────────────────────────────────────────────────────
    private final NfcReaderService nfcReaderService = new NfcReaderService();

    public static void setServices(GateAccessService gas, MovementService ms, PersonService ps) {
        gateService = gas; movementService = ms; personService = ps;
    }

    public static void setDataSource(DataSource ds) { backupDataSource = ds; }

    // ── Theme ─────────────────────────────────────────────────────────────────
    private enum Theme { COMPACT, FULL }

    private static final class D {
        static final String BG         = "#030303";
        static final String BG_PANEL   = "#080808";
        static final String GREEN      = "#39ff14";
        static final String AMBER      = "#ffb300";
        static final String RED        = "#ff4444";
        static final String GREY       = "#555555";
        static final String GREY_LIGHT = "#888888";

        static final Color C_GREEN = Color.web(GREEN);
        static final Color C_AMBER = Color.web(AMBER);
        static final Color C_RED   = Color.web(RED);
        static final Color C_GREY  = Color.web(GREY);

        // COMPACT (150% Windows scaling → ~1280×720 effective)
        static final int CS_STATS_V = 20; static final int CS_STATS_L = 9;
        static final int CS_MSG = 13;     static final int CS_LIST = 12;
        static final int CS_BTN = 11;     static final int CS_FIELD = 11;
        static final int CS_LABEL = 11;   static final int CS_TABLE = 11;

        // FULL (normal 1080p)
        static final int FS_STATS_V = 30; static final int FS_STATS_L = 11;
        static final int FS_MSG = 20;     static final int FS_LIST = 15;
        static final int FS_BTN = 13;     static final int FS_FIELD = 13;
        static final int FS_LABEL = 13;   static final int FS_TABLE = 13;
    }

    // ── Vehicle choices ───────────────────────────────────────────────────────
    private static final List<String> VEHICLE_TYPES_EN = List.of(
            "Car", "Jeep / SUV", "Pickup", "Light Truck", "Truck",
            "Motorcycle", "Scooter", "Bicycle",
            "Bus", "Minibus / Van",
            "Ambulance", "Fire Truck", "Military Vehicle", "Tractor"
    );
    private static final List<String> VEHICLE_TYPES_GR = List.of(
            "Αυτοκίνητο", "Τζιπ / SUV", "Pickup", "Ημιφορτηγό", "Φορτηγό",
            "Μοτοσυκλέτα", "Μηχανάκι / Scooter", "Ποδήλατο",
            "Λεωφορείο", "Μικρό λεωφορείο / Van",
            "Ασθενοφόρο", "Πυροσβεστικό", "Στρατιωτικό όχημα", "Τρακτέρ"
    );

    private static final List<String> VEHICLE_COLORS_EN = List.of(
            "White", "Black", "Grey", "Silver",
            "Red", "Blue", "Light Blue", "Dark Blue",
            "Green", "Yellow", "Orange",
            "Brown", "Beige", "Gold", "Purple", "Pink"
    );
    private static final List<String> VEHICLE_COLORS_GR = List.of(
            "Λευκό", "Μαύρο", "Γκρι", "Ασημί",
            "Κόκκινο", "Μπλε", "Γαλάζιο", "Σκούρο Μπλε",
            "Πράσινο", "Κίτρινο", "Πορτοκαλί",
            "Καφέ", "Μπεζ", "Χρυσό", "Μωβ", "Ροζ"
    );

    private static List<String> vehicleTypes()  { return langEn ? VEHICLE_TYPES_EN  : VEHICLE_TYPES_GR;  }
    private static List<String> vehicleColors() { return langEn ? VEHICLE_COLORS_EN : VEHICLE_COLORS_GR; }

    // ── Preferences ──────────────────────────────────────────────────────────
    private static final Preferences PREFS      = Preferences.userNodeForPackage(GateUIWithRoster.class);
    private static final String      PREF_THEME = "theme";
    private static final String      PREF_LANG  = "lang";

    // ── Language ─────────────────────────────────────────────────────────────
    private static boolean langEn = true;
    private static String t(String en, String gr) { return langEn ? en : gr; }

    // ── State ─────────────────────────────────────────────────────────────────
    private Theme   theme   = Theme.COMPACT;
    private boolean isAdmin = false;
    private final StringBuilder rfidBuffer = new StringBuilder();
    private final ObservableList<Person> allPersons = FXCollections.observableArrayList();
    private final FilteredList<Person> filteredPersons = new FilteredList<>(allPersons, p -> true);
    private final Set<Long> currentlyInsideIds = new HashSet<>();

    // ── UI refs ───────────────────────────────────────────────────────────────
    private Stage     mainStage;
    private StackPane root;
    private Label     messageLabel;
    private Label     totalValue, insideValue, outsideValue;
    private HBox      statsBar;
    private ListView<Person>    listView;
    private TableView<Movement> historyTable;
    private Button adminButton, addPersonButton, exportExcelBtn, exportAllExcelBtn, exportInsideBtn, manualMovementBtn;
    private Label      clockLabel;
    private Label      historyHeaderLabel;
    private Timeline   clockTimeline;
    private DatePicker fromPicker, toPicker;
    private Button           backupDbBtn;
    private VBox             insideStatBox;
    private Label            lastActivityLabel;
    private Label            dateLabel;
    private Timeline         adminBreathTl;
    private ComboBox<String> rankFilter;

    // Persisted across theme switches so the footer survives a rebuild
    private String lastActivityText  = "—";
    private Color  lastActivityColor = null;  // null → grey

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ─────────────────────────────────────────────────────────────────────────
    // Entry point
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void start(Stage stage) {
        this.mainStage = stage;
        theme  = "FULL".equals(PREFS.get(PREF_THEME, "COMPACT")) ? Theme.FULL : Theme.COMPACT;
        langEn = !"GR".equals(PREFS.get(PREF_LANG, "EN"));
        stage.setOnCloseRequest(e -> {
            e.consume();
            showConfirm(t("Exit","Έξοδος"), t("Are you sure you want to close the application?","Θέλετε σίγουρα να κλείσετε την εφαρμογή;"))
                    .filter(r -> r == ButtonType.OK)
                    .ifPresent(r -> { nfcReaderService.stop(); Platform.exit(); System.exit(0); });
        });
        try {
            java.awt.Taskbar taskbar = java.awt.Taskbar.getTaskbar();
            java.net.URL iconUrl = getClass().getResource("/app.ico");
            if (iconUrl != null) {
                java.awt.Image awtIcon = java.awt.Toolkit.getDefaultToolkit().getImage(iconUrl);
                taskbar.setIconImage(awtIcon);
            }
        } catch (Exception ignored) {}
        applyAppIcon(stage);
        buildUI();
        stage.setFullScreen(true);
        stage.show();
        Platform.runLater(root::requestFocus);
        startClock();
        loadAllPersons();
        // Start NFC phone reader (ACR122U or any PC/SC reader) alongside RFID keyboard reader
        nfcReaderService.start(uid -> handleCardRead(uid));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // App icon helper
    // ─────────────────────────────────────────────────────────────────────────
    private void applyAppIcon(Stage stage) {
        try (java.io.InputStream is = getClass().getResourceAsStream("/app.ico")) {
            if (is != null) { stage.getIcons().clear(); stage.getIcons().add(new Image(is)); }
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Build UI
    // ─────────────────────────────────────────────────────────────────────────
    private void buildUI() {
        final boolean C = (theme == Theme.COMPACT);
        final int fBtn   = C ? D.CS_BTN   : D.FS_BTN;
        final int fField = C ? D.CS_FIELD : D.FS_FIELD;
        final int fList  = C ? D.CS_LIST  : D.FS_LIST;
        final int fLabel = C ? D.CS_LABEL : D.FS_LABEL;
        final int fMsg   = C ? D.CS_MSG   : D.FS_MSG;
        final int fTable = C ? D.CS_TABLE : D.FS_TABLE;
        final int gap    = C ? 5 : 8;
        final int pad    = C ? 4 : 7;

        messageLabel = new Label(t("Waiting for card...", "Αναμονή για κάρτα..."));
        messageLabel.setFont(Font.font("Consolas", FontWeight.BOLD, fMsg));
        messageLabel.setTextFill(D.C_GREEN);

        totalValue = new Label("0"); insideValue = new Label("0"); outsideValue = new Label("0");
        statsBar = new HBox(); listView = new ListView<>(); historyTable = new TableView<>();

        // Filter bar
        TextField searchField = new TextField();
        searchField.setPromptText(t("Search...", "Αναζήτηση..."));
        searchField.setPrefWidth(C ? 155 : 200);
        applyFieldStyle(searchField, fField);

        rankFilter = buildRankFilter(fField);
        Button clearFiltersBtn = ghostBtn("✕", fBtn, D.GREEN);
        clearFiltersBtn.setTooltip(new Tooltip(t("Clear filters", "Καθαρισμός φίλτρων")));
        clearFiltersBtn.setOnAction(e -> { searchField.clear(); rankFilter.setValue(t("ALL", "ΟΛΟΙ")); });

        Runnable applyFilters = () -> {
            String txt  = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            String rank = rankFilter.getValue();
            filteredPersons.setPredicate(p -> {
                boolean nm = (p.getFirstName() + " " + p.getLastName()).toLowerCase().contains(txt);
                boolean rm = rank == null || t("ALL","ΟΛΟΙ").equals(rank) || safe(p.getMrank()).equals(rank);
                return nm && rm;
            });
        };
        searchField.textProperty().addListener((obs, o, n) -> applyFilters.run());
        rankFilter.valueProperty().addListener((obs, o, n) -> applyFilters.run());

        HBox filterBar = row(gap, Pos.CENTER_LEFT,
                dimLabel("🔍", fLabel), searchField,
                dimLabel(t("Rank:", "Βαθμός:"), fLabel), rankFilter, clearFiltersBtn);
        filterBar.setPadding(new Insets(pad, 10, pad, 10));
        filterBar.setStyle(panelStyle());

        // Roster
        setupListView(fList);
        VBox.setVgrow(listView, Priority.ALWAYS);

        // Action bar
        addPersonButton = accentBtn(t("＋ New", "＋ Νέα"), D.AMBER, fBtn);
        addPersonButton.setVisible(false); addPersonButton.setManaged(false);
        addPersonButton.setOnAction(e -> openPersonModal(null));

        manualMovementBtn = accentBtn(t("✎ Movement", "✎ Κίνηση"), D.AMBER, fBtn);
        manualMovementBtn.setTooltip(new Tooltip(t("Manual entry/exit movement", "Χειροκίνητη καταχώριση κίνησης εισόδου / εξόδου")));
        manualMovementBtn.setVisible(false); manualMovementBtn.setManaged(false);
        manualMovementBtn.setOnAction(e -> openManualMovementOverlay());

        Button infoBtn    = accentBtn(t("👁  Details", "👁  Στοιχεία"), D.GREEN, fBtn);
        Button historyBtn = accentBtn(t("📋  History", "📋  Ιστορικό"), D.GREEN, fBtn);
        infoBtn.setDisable(true); historyBtn.setDisable(true);
        infoBtn.setFocusTraversable(false); historyBtn.setFocusTraversable(false);
        infoBtn.setOnAction(e -> { Person sel = listView.getSelectionModel().getSelectedItem(); if (sel != null) openPersonModal(sel); });
        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            infoBtn.setDisable(sel == null); historyBtn.setDisable(sel == null);
            if (sel != null) loadHistory(sel, fromPicker, toPicker);
        });

        HBox actionBar = row(gap, Pos.CENTER_LEFT, infoBtn, historyBtn, addPersonButton);
        actionBar.setPadding(new Insets(pad, 10, pad, 10));
        actionBar.setStyle(panelStyle() + "-fx-border-color: rgba(57,255,20,0.10); -fx-border-width: 1 0 0 0;");

        VBox leftPanel = new VBox(0, filterBar, listView, actionBar);
        leftPanel.setStyle("-fx-background-color:" + D.BG + ";");
        VBox.setVgrow(listView, Priority.ALWAYS);

        // History panel
        buildHistoryTable(fTable);

        fromPicker = new DatePicker(); toPicker = new DatePicker();
        fromPicker.setPrefWidth(C ? 112 : 135); toPicker.setPrefWidth(C ? 112 : 135);
        styleDatePicker(fromPicker, fField); styleDatePicker(toPicker, fField);

        historyBtn.setOnAction(e -> loadHistory(listView.getSelectionModel().getSelectedItem(), fromPicker, toPicker));

        Button clearDateBtn = ghostBtn("✕", fBtn, D.GREEN);
        clearDateBtn.setTooltip(new Tooltip(t("Clear dates", "Καθαρισμός ημερομηνιών")));
        clearDateBtn.setOnAction(e -> { fromPicker.setValue(null); toPicker.setValue(null); loadHistory(listView.getSelectionModel().getSelectedItem(), fromPicker, toPicker); });

        Button todayBtn = ghostBtn(t("Today", "Σήμερα"), fBtn, D.GREEN);
        todayBtn.setTooltip(new Tooltip(t("Show today's movements", "Εμφάνιση κινήσεων σήμερα")));
        todayBtn.setOnAction(e -> {
            LocalDate today = LocalDate.now();
            fromPicker.setValue(today); toPicker.setValue(today);
            loadHistory(listView.getSelectionModel().getSelectedItem(), fromPicker, toPicker);
        });

        exportExcelBtn    = accentBtn("⬇ Excel",                         D.AMBER, fBtn);
        exportAllExcelBtn = accentBtn(t("⬇ All",    "⬇ Όλα"),           D.AMBER, fBtn);
        exportInsideBtn   = accentBtn(t("⬇ Inside", "⬇ Εντός"),         D.AMBER, fBtn);
        exportExcelBtn.setVisible(false);    exportExcelBtn.setManaged(false);
        exportAllExcelBtn.setVisible(false); exportAllExcelBtn.setManaged(false);
        exportInsideBtn.setVisible(false);   exportInsideBtn.setManaged(false);
        exportExcelBtn.setTooltip(new Tooltip(t("Export selected person's movement history", "Εξαγωγή ιστορικού κινήσεων επιλεγμένου ατόμου")));
        exportAllExcelBtn.setTooltip(new Tooltip(t("Export all movements (with date filter)", "Εξαγωγή όλων των κινήσεων (με φίλτρο ημερομηνίας)")));
        exportInsideBtn.setTooltip(new Tooltip(t("Export list of personnel currently inside", "Εξαγωγή λίστας ατόμων που βρίσκονται αυτή τη στιγμή εντός")));
        exportExcelBtn.setOnAction(e -> exportPersonHistory(mainStage, fromPicker, toPicker));
        exportAllExcelBtn.setOnAction(e -> exportAllMovements(mainStage, fromPicker, toPicker));
        exportInsideBtn.setOnAction(e -> exportCurrentlyInside(mainStage));

        backupDbBtn = accentBtn("💾 Backup", D.AMBER, fBtn);
        backupDbBtn.setTooltip(new Tooltip(t("Create database backup", "Δημιουργία αντιγράφου ασφαλείας βάσης δεδομένων")));
        backupDbBtn.setVisible(false); backupDbBtn.setManaged(false);
        backupDbBtn.setOnAction(e -> performDatabaseBackup());

        HBox dateBar = row(gap, Pos.CENTER_LEFT,
                dimLabel(t("From:", "Από:"), fLabel), fromPicker, dimLabel(t("To:", "Έως:"), fLabel), toPicker,
                clearDateBtn, todayBtn, exportExcelBtn, exportAllExcelBtn, exportInsideBtn);
        dateBar.setPadding(new Insets(pad, 10, pad, 10));
        dateBar.setStyle(panelStyle());

        historyHeaderLabel = new Label(t("MOVEMENT HISTORY", "ΙΣΤΟΡΙΚΟ ΚΙΝΗΣΕΩΝ"));
        historyHeaderLabel.setFont(Font.font("Consolas", FontWeight.BOLD, C ? 10 : 12));
        historyHeaderLabel.setTextFill(D.C_GREEN);
        HBox histHeader = new HBox(historyHeaderLabel);
        histHeader.setAlignment(Pos.CENTER_LEFT);
        histHeader.setPadding(new Insets(4, 10, 4, 10));
        histHeader.setStyle("-fx-background-color:#060606; -fx-border-color: rgba(57,255,20,0.15); -fx-border-width: 0 0 1 0;");
        VBox rightPanel = new VBox(0, histHeader, dateBar, historyTable);
        rightPanel.setStyle("-fx-background-color:" + D.BG + ";");
        VBox.setVgrow(historyTable, Priority.ALWAYS);

        SplitPane split = new SplitPane(leftPanel, rightPanel);
        split.setDividerPositions(0.40);
        split.setStyle("-fx-background-color:" + D.BG + "; -fx-box-border: transparent; -fx-padding:0;");
        VBox.setVgrow(split, Priority.ALWAYS);

        setupStatsBar(C);

        // Top bar
        adminButton = new Button(t("Administrator", "Διαχειριστής"));
        styleAdminButton(isAdmin, fBtn);
        adminButton.setFocusTraversable(false);
        adminButton.setOnAction(e -> handleAdminLogin());

        Button themeBtn = ghostBtn(C ? t("⊞ Full","⊞ Πλήρης") : t("⊟ Compact","⊟ Συμπαγής"), fBtn, D.GREEN);
        themeBtn.setOnAction(e -> {
            theme = (theme == Theme.COMPACT) ? Theme.FULL : Theme.COMPACT;
            PREFS.put(PREF_THEME, theme.name());
            switchTheme();
        });

        Button langBtn = ghostBtn(langEn ? "🇬🇷 ΕΛ" : "🇬🇧 EN", fBtn, D.GREEN);
        langBtn.setTooltip(new Tooltip(langEn ? "Switch to Greek" : "Switch to English"));
        langBtn.setOnAction(e -> {
            langEn = !langEn;
            PREFS.put(PREF_LANG, langEn ? "EN" : "GR");
            switchTheme();
        });

        Button fsBtn = ghostBtn("⛶", fBtn, D.GREEN);
        fsBtn.setTooltip(new Tooltip(t("Full Screen", "Πλήρης Οθόνη")));
        fsBtn.setOnAction(e -> mainStage.setFullScreen(!mainStage.isFullScreen()));

        Button exitBtn = new Button("✕");
        exitBtn.setFocusTraversable(false);
        exitBtn.setTooltip(new Tooltip(t("Exit application", "Έξοδος από την εφαρμογή")));
        String exitBase  = "-fx-background-color: transparent; -fx-text-fill:" + D.RED + ";" +
                "-fx-border-color:" + D.RED + "55; -fx-border-width:1;" +
                "-fx-font-family:Consolas; -fx-font-size:" + fBtn + "; -fx-font-weight:bold;" +
                "-fx-padding:3 10 3 10; -fx-border-radius:4; -fx-background-radius:4;";
        String exitHover = "-fx-background-color:" + D.RED + "1a; -fx-text-fill:" + D.RED + ";" +
                "-fx-border-color:" + D.RED + "; -fx-border-width:1;" +
                "-fx-font-family:Consolas; -fx-font-size:" + fBtn + "; -fx-font-weight:bold;" +
                "-fx-padding:3 10 3 10; -fx-border-radius:4; -fx-background-radius:4;" +
                "-fx-effect: dropshadow(gaussian," + D.RED + ",8,0.25,0,0);";
        exitBtn.setStyle(exitBase);
        exitBtn.setOnMouseEntered(e -> exitBtn.setStyle(exitHover));
        exitBtn.setOnMouseExited(e  -> exitBtn.setStyle(exitBase));
        exitBtn.setOnAction(e ->
                showConfirm(t("Exit","Έξοδος"), t("Are you sure you want to close the application?","Θέλετε σίγουρα να κλείσετε την εφαρμογή;"))
                        .filter(r -> r == ButtonType.OK)
                        .ifPresent(r -> { nfcReaderService.stop(); Platform.exit(); System.exit(0); }));

        clockLabel = new Label(LocalDateTime.now().format(TIME_FMT));
        clockLabel.setFont(Font.font("Consolas", FontWeight.BOLD, fMsg));
        clockLabel.setTextFill(D.C_GREEN);
        clockLabel.setEffect(new DropShadow(6, D.C_GREEN));

        dateLabel = new Label(LocalDate.now().format(DATE_FMT));
        dateLabel.setFont(Font.font("Consolas", FontWeight.BOLD, fMsg));
        dateLabel.setTextFill(D.C_GREEN);
        dateLabel.setEffect(new DropShadow(6, D.C_GREEN));

        Region sp1 = new Region(); HBox.setHgrow(sp1, Priority.ALWAYS);
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        Region sp3 = new Region(); sp3.setMinWidth(C ? 18 : 26);

        HBox topBar = row(gap, Pos.CENTER_LEFT, adminButton, manualMovementBtn, backupDbBtn, sp1, messageLabel, sp2, dateLabel, clockLabel, sp3, langBtn, themeBtn, fsBtn, exitBtn);
        topBar.setPadding(new Insets(pad, 10, pad, 10));
        topBar.setStyle("-fx-background-color: linear-gradient(to right, #050505, #0a0f0a, #050505);" +
                "-fx-border-color: rgba(57,255,20,0.15); -fx-border-width: 0 0 1 0;");

        // Footer — slim last-activity bar (text/color persisted across theme switches)
        lastActivityLabel = new Label(lastActivityText);
        lastActivityLabel.setFont(Font.font("Consolas", C ? 10 : 11));
        lastActivityLabel.setTextFill(lastActivityColor != null ? lastActivityColor : Color.web(D.GREY));
        Label footerPrefix = new Label(t("Last movement:  ", "Τελευταία κίνηση:  "));
        footerPrefix.setFont(Font.font("Consolas", C ? 10 : 11));
        footerPrefix.setTextFill(Color.web(D.GREY));
        HBox footerBar = new HBox(0, footerPrefix, lastActivityLabel);
        footerBar.setAlignment(Pos.CENTER_LEFT);
        footerBar.setPadding(new Insets(2, 12, 2, 12));
        footerBar.setStyle("-fx-background-color:#020202;" +
                "-fx-border-color: rgba(57,255,20,0.08); -fx-border-width: 1 0 0 0;");

        VBox layout = new VBox(0, topBar, statsBar, split, footerBar);
        layout.setStyle("-fx-background-color:" + D.BG + ";");
        VBox.setVgrow(split, Priority.ALWAYS);

        root = new StackPane(layout);
        root.setStyle("-fx-background-color:" + D.BG + ";");

        Scene scene = new Scene(root, 1280, 720, Color.web(D.BG));
        scene.setOnKeyTyped(event -> {
            String ch = event.getCharacter();
            if ("\r".equals(ch)) {
                String uid = rfidBuffer.toString().trim();
                rfidBuffer.setLength(0);
                if (!uid.isEmpty()) handleCardRead(uid);
            } else rfidBuffer.append(ch);
        });
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && root.getChildren().size() > 1) {
                Node topmost = root.getChildren().get(root.getChildren().size() - 1);
                if ("dismissable".equals(topmost.getUserData())) {
                    root.getChildren().remove(topmost);
                    event.consume();
                }
            }
        });

        mainStage.setScene(scene);
        mainStage.setTitle(t("Gate Control Terminal", "Τερματικό Ελέγχου Πύλης"));
    }

    private void switchTheme() {
        boolean wasFS = mainStage.isFullScreen();
        buildUI();
        refreshAdminUI(isAdmin);   // buildUI() hides all admin buttons; restore if still logged in
        if (wasFS) mainStage.setFullScreen(true);
        Platform.runLater(() -> { root.requestFocus(); listView.refresh(); updateStats(); });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ListView
    // ─────────────────────────────────────────────────────────────────────────
    private void setupListView(int fs) {
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Person sel = listView.getSelectionModel().getSelectedItem();
                if (sel != null) openPersonModal(sel);
            }
        });
        listView.setStyle("-fx-background-color:" + D.BG + "; -fx-control-inner-background:" + D.BG + ";" +
                "-fx-border-color: rgba(57,255,20,0.10); -fx-border-width: 0 1 0 0;" +
                "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        listView.setItems(filteredPersons);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Person p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setText(null); setGraphic(null); setStyle("-fx-background-color:" + D.BG + ";"); return; }
                boolean inactive = !p.isActive();
                boolean inside   = !inactive && currentlyInsideIds.contains(p.getId());
                Color  accent = inactive ? D.C_GREY : (inside ? D.C_GREEN : D.C_RED);
                String hex    = inactive ? D.GREY   : (inside ? D.GREEN   : D.RED);
                Rectangle dot = new Rectangle(7, 7);
                dot.setArcWidth(7); dot.setArcHeight(7); dot.setFill(accent);
                if (!inactive) dot.setEffect(new DropShadow(5, accent));
                String nameText = p.getFirstName() + " " + p.getLastName() + (inactive ? t("  [IA]","  [ΑΝ]") : "");
                Label name = new Label(nameText);
                name.setFont(Font.font("Consolas", FontWeight.BOLD, fs)); name.setTextFill(accent);
                Label rank = new Label(safe(p.getMrank()));
                rank.setFont(Font.font("Consolas", fs - 1)); rank.setTextFill(Color.web(D.GREY_LIGHT)); rank.setMinWidth(55);
                HBox cellRow = new HBox(7, dot, rank, name);
                cellRow.setAlignment(Pos.CENTER_LEFT); cellRow.setPadding(new Insets(3, 8, 3, 8));
                if (inactive) cellRow.setOpacity(0.55);
                setGraphic(cellRow); setText(null);
                setStyle(isSelected()
                        ? "-fx-background-color:" + D.BG_PANEL + "; -fx-border-color:" + hex + "55; -fx-border-width: 0 0 0 2;"
                        : "-fx-background-color:" + D.BG + ";");
            }
        });
        // Dark scrollbar (same treatment as history table)
        Platform.runLater(() -> listView.lookupAll(".scroll-bar").forEach(n -> {
            n.setStyle("-fx-background-color:" + D.BG + ";");
            n.lookupAll(".thumb").forEach(t -> t.setStyle("-fx-background-color: rgba(57,255,20,0.18); -fx-background-radius:3;"));
            n.lookupAll(".track").forEach(t -> t.setStyle("-fx-background-color:#0a0a0a;"));
            n.lookupAll(".increment-button,.decrement-button").forEach(b -> b.setStyle("-fx-background-color:" + D.BG + ";"));
        }));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats bar
    // ─────────────────────────────────────────────────────────────────────────
    private void setupStatsBar(boolean C) {
        int fv = C ? D.CS_STATS_V : D.FS_STATS_V;
        int fl = C ? D.CS_STATS_L : D.FS_STATS_L;
        VBox tb = statBox(t("STRENGTH","ΔΥΝΑΜΗ"), totalValue   = new Label("0"), fl, fv, D.C_AMBER, D.AMBER);
        insideStatBox = statBox(t("INSIDE","ΕΝΤΟΣ"),  insideValue = new Label("0"), fl, fv, D.C_GREEN, D.GREEN);
        insideStatBox.setCursor(javafx.scene.Cursor.HAND);
        Tooltip.install(insideStatBox, new Tooltip(t("Click for inside personnel list","Κλικ για λίστα εντός βάσης")));
        insideStatBox.setOnMouseClicked(e -> openInsideTracker());
        VBox ob = statBox(t("OUTSIDE","ΕΚΤΟΣ"), outsideValue = new Label("0"), fl, fv, D.C_RED,   D.RED);
        statsBar.getChildren().setAll(tb, insideStatBox, ob);
        statsBar.setSpacing(C ? 8 : 14); statsBar.setAlignment(Pos.CENTER_LEFT);
        statsBar.setPadding(new Insets(C ? 3 : 5, 12, C ? 3 : 5, 12));
        statsBar.setMaxHeight(C ? 42 : 56);
        statsBar.setStyle("-fx-background-color:#060606; -fx-border-color: rgba(57,255,20,0.08); -fx-border-width: 0 0 1 0;");
    }

    private VBox statBox(String title, Label val, int fl, int fv, Color c, String hex) {
        Label lbl = new Label(title); lbl.setFont(Font.font("Consolas", fl)); lbl.setTextFill(D.C_GREY);
        val.setFont(Font.font("Consolas", FontWeight.BOLD, fv)); val.setTextFill(c); val.setEffect(new DropShadow(8, c));
        VBox box = new VBox(1, lbl, val); box.setAlignment(Pos.CENTER_LEFT); box.setPadding(new Insets(4, 14, 4, 14));
        box.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(0,0,0,0.5), rgba(0,0,0,0.85));" +
                "-fx-border-color:" + hex + "44; -fx-border-width: 0 0 0 2;");
        return box;
    }

    private void updateStats() {
        Set<Long> activeIds = allPersons.stream()
                .filter(Person::isActive).map(Person::getId)
                .collect(Collectors.toSet());
        long insideActive = currentlyInsideIds.stream().filter(activeIds::contains).count();
        animateStatChange(totalValue,   (int) activeIds.size(),                  D.C_AMBER);
        animateStatChange(insideValue,  (int) insideActive,                      D.C_GREEN);
        animateStatChange(outsideValue, (int) (activeIds.size() - insideActive), D.C_RED);
    }

    /** Counts a stat label from its current value to {@code newValue} and briefly flares the glow. */
    private void animateStatChange(Label label, int newValue, Color color) {
        int oldValue;
        try { oldValue = Integer.parseInt(label.getText()); } catch (NumberFormatException ex) { oldValue = 0; }
        if (oldValue == newValue) return;

        final int from  = oldValue;
        final int to    = newValue;
        final int steps = Math.min(Math.abs(to - from), 18);

        // Counter animation
        Timeline countTl = new Timeline();
        for (int i = 0; i <= steps; i++) {
            final int step = i;
            countTl.getKeyFrames().add(new KeyFrame(Duration.millis(380.0 * i / steps),
                    e -> label.setText(String.valueOf(from + (int) Math.round((double)(to - from) * step / steps)))));
        }

        // Glow flash via property animation
        DropShadow ds = (label.getEffect() instanceof DropShadow d) ? d : new DropShadow(8, color);
        label.setEffect(ds);
        Timeline glowTl = new Timeline(
                new KeyFrame(Duration.millis(0),   new KeyValue(ds.radiusProperty(), 8)),
                new KeyFrame(Duration.millis(160), new KeyValue(ds.radiusProperty(), 32)),
                new KeyFrame(Duration.millis(520), new KeyValue(ds.radiusProperty(), 8)));

        countTl.play();
        glowTl.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // History table
    // ─────────────────────────────────────────────────────────────────────────
    private void buildHistoryTable(int fs) {
        TableColumn<Movement, String> dateCol = new TableColumn<>(t("DATE","ΗΜΕΡΟΜΗΝΙΑ"));
        dateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMovementTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        dateCol.setCellFactory(histColorCell(fs));
        TableColumn<Movement, String> timeCol = new TableColumn<>(t("TIME","ΩΡΑ"));
        timeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMovementTime().toLocalTime().format(TIME_FMT)));
        timeCol.setCellFactory(histColorCell(fs));
        TableColumn<Movement, String> stateCol = new TableColumn<>(t("STATUS","ΚΑΤΑΣΤΑΣΗ"));
        stateCol.setCellValueFactory(new PropertyValueFactory<>("movementType"));
        stateCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                boolean in = "ΕΝΤΟΣ".equals(item);
                setText(in ? t("INSIDE","ΕΝΤΟΣ") : t("OUTSIDE","ΕΚΤΟΣ"));
                setTextFill(in ? D.C_GREEN : D.C_RED);
                setStyle("-fx-font-weight:bold; -fx-alignment:CENTER; -fx-font-family:Consolas; -fx-font-size:" + fs + ";" +
                        "-fx-background-color:" + (in ? D.GREEN : D.RED) + "0d;");
            }
        });
        historyTable.getColumns().setAll(dateCol, timeCol, stateCol);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(historyTable, Priority.ALWAYS);
        Label empty = new Label(t("Select person → History","Επιλέξτε άτομο → Ιστορικό")); empty.setTextFill(D.C_GREY); empty.setFont(Font.font("Consolas", fs));
        historyTable.setPlaceholder(empty);
        historyTable.setStyle("-fx-background-color:" + D.BG + "; -fx-control-inner-background:" + D.BG + ";" +
                "-fx-table-cell-border-color: transparent; -fx-faint-focus-color: transparent;" +
                "-fx-font-family:Consolas; -fx-font-size:" + fs + "px;" +
                "-fx-selection-bar:" + D.BG_PANEL + "; -fx-selection-bar-non-focused:" + D.BG_PANEL + ";");
        historyTable.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Movement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setStyle("-fx-background-color:" + D.BG + ";"); return; }
                boolean in = "ΕΝΤΟΣ".equals(item.getMovementType());
                String accent = in ? D.GREEN : D.RED;
                setStyle(isSelected()
                        ? "-fx-background-color:" + D.BG_PANEL + "; -fx-border-color:" + accent + "55; -fx-border-width: 0 0 0 2;"
                        : "-fx-background-color:" + D.BG + "; -fx-border-color:" + accent + "1a; -fx-border-width: 0 0 1 0;");
            }
        });
        historyTable.getColumns().forEach(col -> col.setStyle("-fx-alignment:CENTER; -fx-text-fill:" + D.GREEN + "; -fx-font-family:Consolas; -fx-font-size:" + fs + ";"));
        Platform.runLater(() -> {
            historyTable.lookupAll(".column-header .label").forEach(n ->
                    n.setStyle("-fx-text-fill:" + D.GREEN + "; -fx-font-family:Consolas; -fx-font-size:" + fs + "; -fx-font-weight:bold;"));
            historyTable.lookupAll(".column-header").forEach(n ->
                    n.setStyle("-fx-background-color:#060606; -fx-border-color: rgba(57,255,20,0.15); -fx-border-width: 0 0 1 0;"));
            historyTable.lookupAll(".scroll-bar").forEach(n -> {
                n.setStyle("-fx-background-color:" + D.BG + ";");
                n.lookupAll(".thumb").forEach(t -> t.setStyle("-fx-background-color: rgba(57,255,20,0.2); -fx-background-radius:3;"));
                n.lookupAll(".track").forEach(t -> t.setStyle("-fx-background-color:#0a0a0a;"));
                n.lookupAll(".increment-button,.decrement-button").forEach(b -> b.setStyle("-fx-background-color:" + D.BG + ";"));
            });
        });
    }

    /** Shared cell factory: colors text green (INSIDE) or red (OUTSIDE) based on the row's movement type. */
    private javafx.util.Callback<TableColumn<Movement, String>, TableCell<Movement, String>> histColorCell(int fs) {
        return col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                try {
                    Movement m = getTableView().getItems().get(getIndex());
                    boolean in = "ΕΝΤΟΣ".equals(m.getMovementType());
                    setTextFill(in ? D.C_GREEN : D.C_RED);
                } catch (Exception ignored) { setTextFill(Color.web(D.GREY_LIGHT)); }
                setStyle("-fx-font-family:Consolas; -fx-font-size:" + fs + "; -fx-alignment:CENTER;");
            }
        };
    }

    private void loadHistory(Person person, DatePicker from, DatePicker to) {
        if (person == null) return;
        List<Movement> moves = movementService.getByPerson(person).stream()
                .filter(m -> from.getValue() == null || !m.getMovementTime().toLocalDate().isBefore(from.getValue()))
                .filter(m -> to.getValue()   == null || !m.getMovementTime().toLocalDate().isAfter(to.getValue()))
                .sorted(Comparator.comparing(Movement::getMovementTime).reversed())
                .toList();
        historyTable.setItems(FXCollections.observableArrayList(moves));
        if (historyHeaderLabel != null)
            historyHeaderLabel.setText(t("HISTORY","ΙΣΤΟΡΙΚΟ") + " — "
                    + person.getLastName().toUpperCase() + " " + person.getFirstName().toUpperCase()
                    + "  (" + moves.size() + ")");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Card read handler
    // ─────────────────────────────────────────────────────────────────────────
    private void handleCardRead(String uid) {
        GateAccessResult result = gateService.handleCardRead(uid);
        if (result.isGranted()) {
            showScanOverlay(result.getPerson(), result.getMovementType());
            root.requestFocus();
            messageLabel.setText("✔  " + result.getPersonName() + "  ·  " + moveLabel(result.getMovementType()));
            messageLabel.setTextFill(D.C_GREEN);
            updateLastActivity(result.getPerson(), result.getMovementType());
            updateAllPersonsStatus();
        } else {
            messageLabel.setText(t("✘  ACCESS DENIED", "✘  ΑΠΑΓΟΡΕΥΣΗ ΠΡΟΣΒΑΣΗΣ"));
            messageLabel.setTextFill(D.C_RED);
        }
        Toolkit.getDefaultToolkit().beep();
        PauseTransition p = new PauseTransition(Duration.seconds(3));
        p.setOnFinished(e -> { messageLabel.setText(t("Waiting for card...","Αναμονή για κάρτα...")); messageLabel.setTextFill(D.C_GREEN); });
        p.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scan overlay  — big, beautiful, with vehicle cards
    // ─────────────────────────────────────────────────────────────────────────
    private void showScanOverlay(Person person, String movementType) {
        boolean inside = "ΕΝΤΟΣ".equals(movementType);
        Color   accent = inside ? D.C_GREEN : D.C_RED;
        String  aHex   = inside ? D.GREEN   : D.RED;

        // Photo with glow
        ImageView photo = buildPhotoView(person, 190, 190);
        photo.setStyle("-fx-effect: dropshadow(gaussian," + aHex + ",20,0.55,0,0);");

        // Name
        Label nameLbl = new Label(person.getFirstName() + " " + person.getLastName());
        nameLbl.setFont(Font.font("Consolas", FontWeight.BOLD, 24));
        nameLbl.setTextFill(accent);
        nameLbl.setEffect(new DropShadow(12, accent));
        nameLbl.setWrapText(true);

        // Rank
        Label rankLbl = new Label(safe(person.getMrank()));
        rankLbl.setFont(Font.font("Consolas", 14));
        rankLbl.setTextFill(D.C_GREY);

        // Status badge
        Label statusLbl = new Label(inside ? t("▲   INSIDE","▲   ΕΝΤΟΣ") : t("▼   OUTSIDE","▼   ΕΚΤΟΣ"));
        statusLbl.setFont(Font.font("Consolas", FontWeight.BOLD, 17));
        statusLbl.setTextFill(accent);
        statusLbl.setPadding(new Insets(6, 22, 6, 22));
        statusLbl.setMaxWidth(Double.MAX_VALUE);
        statusLbl.setAlignment(Pos.CENTER);
        statusLbl.setEffect(new DropShadow(16, accent));
        statusLbl.setStyle("-fx-background-color:" + aHex + "18;" +
                "-fx-border-color:" + aHex + "99;" +
                "-fx-border-width:1; -fx-border-radius:5; -fx-background-radius:5;");

        VBox infoCol = new VBox(8, nameLbl, rankLbl, statusLbl);
        infoCol.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoCol, Priority.ALWAYS);

        HBox topRow = new HBox(20, photo, infoCol);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(4, 0, 4, 0));

        // Gradient separator
        Region sep = new Region();
        sep.setMinHeight(1); sep.setMaxHeight(1); sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color: linear-gradient(to right, transparent," + aHex + "88, transparent);");

        // Vehicle cards
        VBox vehicleSection = buildVehicleInfo(person, accent, aHex);

        // Decorative top accent bar
        Region topAccent = new Region();
        topAccent.setMinHeight(3); topAccent.setMaxHeight(3); topAccent.setMaxWidth(Double.MAX_VALUE);
        topAccent.setStyle("-fx-background-color: linear-gradient(to right, transparent," + aHex + ", transparent);");

        VBox card = new VBox(14, topAccent, topRow, sep, vehicleSection);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(0, 26, 22, 26));
        card.setMinWidth(560);
        card.setMaxWidth(620);
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #111111, #050505);" +
                        "-fx-border-color:" + aHex + ";" +
                        "-fx-border-width:1; -fx-border-radius:10; -fx-background-radius:10;" +
                        "-fx-effect: dropshadow(gaussian," + aHex + ",45,0.55,0,0);");

        StackPane overlay = new StackPane(card);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.68);");

        FadeTransition fi = new FadeTransition(Duration.millis(200), card);
        fi.setFromValue(0); fi.setToValue(1);
        root.getChildren().add(overlay);
        fi.play();

        PauseTransition delay = new PauseTransition(Duration.seconds(5));
        delay.setOnFinished(e -> {
            FadeTransition fo = new FadeTransition(Duration.millis(300), overlay);
            fo.setFromValue(1); fo.setToValue(0);
            fo.setOnFinished(f -> root.getChildren().remove(overlay));
            fo.play();
        });
        delay.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Vehicle section inside scan overlay
    // ─────────────────────────────────────────────────────────────────────────
    private VBox buildVehicleInfo(Person p, Color accent, String aHex) {
        boolean hv1 = hasVehicle(p.getVehicle1Plate());
        boolean hv2 = hasVehicle(p.getVehicle2Plate());

        if (!hv1 && !hv2) {
            Label none = new Label(t("No registered vehicles","Χωρίς καταχωρημένα οχήματα"));
            none.setFont(Font.font("Consolas", 12)); none.setTextFill(D.C_GREY);
            VBox b = new VBox(none); b.setAlignment(Pos.CENTER_LEFT); return b;
        }

        Label header = new Label(t("VEHICLES","ΟΧΗΜΑΤΑ"));
        header.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        header.setTextFill(D.C_GREY);

        HBox cards = new HBox(12);
        cards.setAlignment(Pos.CENTER_LEFT);
        if (hv1) cards.getChildren().add(buildVehicleCard("I",  p.getVehicle1Plate(), p.getVehicle1Type(), p.getVehicle1Color(), accent, aHex));
        if (hv2) cards.getChildren().add(buildVehicleCard("II", p.getVehicle2Plate(), p.getVehicle2Type(), p.getVehicle2Color(), accent, aHex));

        VBox box = new VBox(6, header, cards);
        return box;
    }

    private VBox buildVehicleCard(String num, String plate, String type, String color,
                                  Color accent, String aHex) {
        Label numLbl = new Label(t("🚗  VEHICLE ","🚗  ΟΧΗΜΑ ") + num);
        numLbl.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        numLbl.setTextFill(accent);

        Label plateLbl = new Label(safe(plate));
        plateLbl.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        plateLbl.setTextFill(accent);
        plateLbl.setEffect(new DropShadow(6, accent));

        Label typeLbl = new Label(safe(type));
        typeLbl.setFont(Font.font("Consolas", 12));
        typeLbl.setTextFill(Color.web(D.GREY_LIGHT));

        // Colour dot + name
        String dotHex = colorNameToHex(color);
        Region dot = new Region();
        dot.setMinSize(11, 11); dot.setMaxSize(11, 11);
        dot.setStyle("-fx-background-color:" + dotHex + "; -fx-background-radius:6;" +
                "-fx-effect: dropshadow(gaussian," + dotHex + ",5,0.6,0,0);");

        Label colorLbl = new Label(safe(color));
        colorLbl.setFont(Font.font("Consolas", 12));
        colorLbl.setTextFill(Color.web(D.GREY_LIGHT));

        HBox colorRow = new HBox(7, dot, colorLbl);
        colorRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(5, numLbl, plateLbl, typeLbl, colorRow);
        card.setPadding(new Insets(10, 16, 10, 16));
        card.setStyle("-fx-background-color:#0d0d0d;" +
                "-fx-border-color:" + aHex + "44;" +
                "-fx-border-width:1; -fx-border-radius:5; -fx-background-radius:5;");
        card.setMinWidth(160);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    /** Maps colour names to approximate hex for the colour dot in the scan overlay. */
    private String colorNameToHex(String c) {
        if (c == null) return "#555555";
        return switch (c.trim().toLowerCase()) {
            case "white"                    -> "#f5f5f5";
            case "black"                    -> "#333333";
            case "grey", "gray"             -> "#9e9e9e";
            case "silver"                   -> "#bdbdbd";
            case "red"                      -> "#e53935";
            case "blue"                     -> "#1565c0";
            case "light blue"               -> "#42a5f5";
            case "dark blue"                -> "#0d47a1";
            case "green"                    -> "#43a047";
            case "yellow"                   -> "#fdd835";
            case "orange"                   -> "#fb8c00";
            case "brown"                    -> "#795548";
            case "beige"                    -> "#d7ccc8";
            case "gold"                     -> "#ffc107";
            case "purple"                   -> "#8e24aa";
            case "pink"                     -> "#f48fb1";
            default                         -> "#555555";
        };
    }

    private boolean hasVehicle(String plate) { return plate != null && !plate.isBlank(); }

    // ─────────────────────────────────────────────────────────────────────────
    // Person modal
    // ─────────────────────────────────────────────────────────────────────────
    private void openPersonModal(Person existing) {
        boolean C = (theme == Theme.COMPACT);
        Stage modal = new Stage();
        modal.initOwner(mainStage);
        modal.initModality(Modality.WINDOW_MODAL);   // stays in front; blocks main window
        applyAppIcon(modal);
        modal.setTitle(existing == null ? t("New Registration","Νέα Καταχώριση") : t("Person Details","Στοιχεία Ατόμου"));
        VBox content = buildPersonModalContent(existing, isAdmin, C);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:" + D.BG + "; -fx-background-color:" + D.BG + ";");
        modal.setScene(new Scene(scroll, C ? 660 : 820, C ? 560 : 680));
        modal.show();
    }

    private VBox buildPersonModalContent(Person existing, boolean editable, boolean C) {
        final boolean isNew  = (existing == null);
        final Person  person = isNew ? new Person() : existing;
        final int ff  = C ? D.CS_FIELD : D.FS_FIELD;
        final int fl  = C ? D.CS_LABEL : D.FS_LABEL;
        final int fb  = C ? D.CS_BTN   : D.FS_BTN;
        final int gap = C ? 6 : 10;
        final int photoSize = C ? 160 : 200;

        // ── Photo ─────────────────────────────────────────────────────────────
        ImageView photoView = buildPhotoView(person, photoSize, photoSize);

        // ── Fields GridPane (expands to fill right column) ────────────────────
        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(gap);
        ColumnConstraints kc = new ColumnConstraints(); kc.setHgrow(Priority.NEVER);
        ColumnConstraints vc = new ColumnConstraints(); vc.setHgrow(Priority.ALWAYS); vc.setFillWidth(true);
        form.getColumnConstraints().addAll(kc, vc);
        HBox.setHgrow(form, Priority.ALWAYS);

        // ── Photo column: photo + buttons stacked below ───────────────────────
        VBox photoCol = new VBox(gap);
        photoCol.setAlignment(Pos.TOP_CENTER);
        photoCol.setMinWidth(photoSize + 8);
        photoCol.getChildren().add(photoView);

        VBox wrapper = new VBox(gap);

        if (editable) {
            // Text fields
            TextField fFirst = wideField(nullToEmpty(person.getFirstName()), ff);
            TextField fLast  = wideField(nullToEmpty(person.getLastName()),  ff);
            TextField fRank  = wideField(nullToEmpty(person.getMrank()),     ff);
            TextField fRfid  = wideField(nullToEmpty(person.getRfidUid()),   ff);

            form.add(formKey(t("First Name","Όνομα"),    fl), 0, 0); form.add(fFirst, 1, 0);
            form.add(formKey(t("Last Name","Επώνυμο"),   fl), 0, 1); form.add(fLast,  1, 1);
            form.add(formKey(t("Rank","Βαθμός"),         fl), 0, 2); form.add(fRank,  1, 2);
            form.add(formKey("RFID UID",                 fl), 0, 3); form.add(fRfid,  1, 3);

            // Active toggle
            ToggleButton activeToggle = new ToggleButton();
            activeToggle.setSelected(person.isActive());
            activeToggle.setMaxWidth(Double.MAX_VALUE);
            styleActiveToggle(activeToggle, person.isActive(), fb);
            activeToggle.setOnAction(e -> styleActiveToggle(activeToggle, activeToggle.isSelected(), fb));

            // Photo buttons (stacked under photo)
            Button uploadBtn = accentBtn(t("📁 From File","📁 Από Αρχείο"), D.GREEN, fb);
            uploadBtn.setMaxWidth(Double.MAX_VALUE);
            uploadBtn.setOnAction(r -> {
                File f = new FileChooser().showOpenDialog(null);
                if (f != null) try {
                    person.setPhoto(Files.readAllBytes(f.toPath()));
                    photoView.setImage(new Image(new ByteArrayInputStream(person.getPhoto())));
                } catch (IOException ex) { ex.printStackTrace(); }
            });
            Button cameraBtn = accentBtn(t("📷 Camera","📷 Κάμερα"), D.AMBER, fb);
            cameraBtn.setMaxWidth(Double.MAX_VALUE);
            cameraBtn.setOnAction(r -> openCameraCapture(
                    (Stage) cameraBtn.getScene().getWindow(),
                    bytes -> {
                        person.setPhoto(bytes);
                        photoView.setImage(new Image(new ByteArrayInputStream(bytes)));
                    }));
            photoCol.getChildren().addAll(uploadBtn, cameraBtn, activeToggle);

            // Vehicle fields
            TextField v1P = fieldTF(nullToEmpty(person.getVehicle1Plate()), ff);
            TextField v2P = fieldTF(nullToEmpty(person.getVehicle2Plate()), ff);
            ComboBox<String> v1T = typeCB(person.getVehicle1Type(),  ff);
            ComboBox<String> v1C = colorCB(person.getVehicle1Color(), ff);
            ComboBox<String> v2T = typeCB(person.getVehicle2Type(),  ff);
            ComboBox<String> v2C = colorCB(person.getVehicle2Color(), ff);
            HBox vehicleRow = new HBox(gap,
                    vehicleEditPane(t("VEHICLE 1","ΟΧΗΜΑ 1"), v1P, v1T, v1C, fl, C),
                    vehicleEditPane(t("VEHICLE 2","ΟΧΗΜΑ 2"), v2P, v2T, v2C, fl, C));
            vehicleRow.setAlignment(Pos.CENTER);

            // Action buttons: save + close left, delete right
            Button saveBtn  = accentBtn(isNew ? t("＋ Register","＋ Καταχώριση") : t("💾 Save","💾 Αποθήκευση"), D.GREEN, fb);
            Button closeBtn = accentBtn(t("✕ Close","✕ Κλείσιμο"), D.GREY, fb);
            closeBtn.setOnAction(e -> closeStage(closeBtn));
            saveBtn.setOnAction(ev -> {
                String rfid = fRfid.getText().trim();
                if (!rfid.isEmpty()) {
                    boolean dup = movementService.getAllPersons().stream()
                            .filter(px -> !px.getId().equals(person.getId()))
                            .anyMatch(px -> rfid.equalsIgnoreCase(px.getRfidUid() == null ? "" : px.getRfidUid().trim()));
                    if (dup) { showError(t("Duplicate RFID","Διπλό RFID"), t("This ID already belongs to another person.","Αυτό το ID ανήκει ήδη σε άλλο άτομο.")); fRfid.requestFocus(); fRfid.selectAll(); return; }
                }
                person.setFirstName(fFirst.getText().trim());
                person.setLastName(fLast.getText().trim());
                person.setMrank(fRank.getText().trim());
                person.setRfidUid(rfid.isEmpty() ? null : rfid);
                person.setVehicle1Plate(emptyToNull(v1P.getText()));
                person.setVehicle1Type(emptyToNull(v1T.getValue()));
                person.setVehicle1Color(emptyToNull(v1C.getValue()));
                person.setVehicle2Plate(emptyToNull(v2P.getText()));
                person.setVehicle2Type(emptyToNull(v2T.getValue()));
                person.setVehicle2Color(emptyToNull(v2C.getValue()));
                person.setActive(activeToggle.isSelected());
                personService.save(person);
                loadAllPersons();
                closeStage(saveBtn);
            });

            HBox btnRow;
            if (!isNew) {
                Button delBtn = accentBtn(t("🗑 Delete","🗑 Διαγραφή"), D.RED, fb);
                delBtn.setOnAction(e -> {
                    showConfirm(t("Confirm","Επιβεβαίωση"), t("Delete person? This action cannot be undone.","Διαγραφή ατόμου; Αυτή η ενέργεια δεν μπορεί να αναιρεθεί.")).ifPresent(r -> {
                        if (r == ButtonType.OK) { personService.delete(person); loadAllPersons(); closeStage(delBtn); }
                    });
                });
                Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
                btnRow = new HBox(gap, saveBtn, closeBtn, spacer, delBtn);
            } else {
                btnRow = new HBox(gap, saveBtn, closeBtn);
            }
            btnRow.setAlignment(Pos.CENTER_LEFT);

            HBox topRow = new HBox(C ? 14 : 18, photoCol, form);
            topRow.setAlignment(Pos.TOP_LEFT);
            wrapper.getChildren().addAll(topRow, vehicleRow, divider(), btnRow);

        } else {
            // Read-only view
            form.add(formKey(t("First Name","Όνομα"),    fl), 0, 0); form.add(valLbl(person.getFirstName(), ff), 1, 0);
            form.add(formKey(t("Last Name","Επώνυμο"),   fl), 0, 1); form.add(valLbl(person.getLastName(),  ff), 1, 1);
            form.add(formKey(t("Rank","Βαθμός"),         fl), 0, 2); form.add(valLbl(person.getMrank(),     ff), 1, 2);
            form.add(formKey("RFID UID",                 fl), 0, 3); form.add(valLbl(person.getRfidUid(),   ff), 1, 3);

            HBox vehicleRow = new HBox(gap,
                    vehicleReadPane(t("VEHICLE 1","ΟΧΗΜΑ 1"), person.getVehicle1Plate(), person.getVehicle1Type(), person.getVehicle1Color(), fl, C),
                    vehicleReadPane(t("VEHICLE 2","ΟΧΗΜΑ 2"), person.getVehicle2Plate(), person.getVehicle2Type(), person.getVehicle2Color(), fl, C));
            vehicleRow.setAlignment(Pos.CENTER);

            HBox topRow = new HBox(C ? 14 : 18, photoCol, form);
            topRow.setAlignment(Pos.TOP_LEFT);
            wrapper.getChildren().addAll(topRow, vehicleRow);
        }

        // Modal chrome
        Label title = new Label(isNew ? t("NEW REGISTRATION","ΝΕΑ ΚΑΤΑΧΩΡΙΣΗ") : (person.getFirstName() + " " + person.getLastName()).toUpperCase());
        title.setFont(Font.font("Consolas", FontWeight.BOLD, C ? 13 : 16));
        title.setTextFill(D.C_GREEN); title.setEffect(new DropShadow(8, D.C_GREEN));

        Region titleSep = new Region();
        titleSep.setMinHeight(1); titleSep.setMaxHeight(1); titleSep.setMaxWidth(Double.MAX_VALUE);
        titleSep.setStyle("-fx-background-color: rgba(57,255,20,0.20);");

        VBox outer = new VBox(C ? 10 : 14);
        outer.setPadding(new Insets(C ? 14 : 18));
        outer.setStyle("-fx-background-color: linear-gradient(to bottom, #0d0d0d, " + D.BG + ");" +
                "-fx-border-color:" + D.GREEN + "44; -fx-border-width:1;" +
                "-fx-border-radius:6; -fx-background-radius:6;");
        outer.getChildren().addAll(title, titleSep, wrapper);
        return outer;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Vehicle pane builders
    // ─────────────────────────────────────────────────────────────────────────
    private VBox vehicleEditPane(String title, TextField plate,
                                 ComboBox<String> type, ComboBox<String> color, int fl, boolean C) {
        GridPane g = new GridPane(); g.setHgap(8); g.setVgap(C ? 4 : 6);
        g.add(formKey(t("Plate:","Πινακίδα:"), fl), 0, 0); g.add(plate, 1, 0);
        g.add(formKey(t("Type:","Τύπος:"),     fl), 0, 1); g.add(type,  1, 1);
        g.add(formKey(t("Color:","Χρώμα:"),    fl), 0, 2); g.add(color, 1, 2);
        return vehiclePaneWrap(title, g, C);
    }

    private VBox vehicleReadPane(String title, String plate, String type, String color, int fl, boolean C) {
        GridPane g = new GridPane(); g.setHgap(8); g.setVgap(C ? 4 : 6);
        g.add(formKey(t("Plate:","Πινακίδα:"), fl), 0, 0); g.add(valLbl(plate, fl), 1, 0);
        g.add(formKey(t("Type:","Τύπος:"),     fl), 0, 1); g.add(valLbl(type,  fl), 1, 1);
        g.add(formKey(t("Color:","Χρώμα:"),    fl), 0, 2); g.add(valLbl(color, fl), 1, 2);
        return vehiclePaneWrap(title, g, C);
    }

    private VBox vehiclePaneWrap(String title, GridPane grid, boolean C) {
        Label t = new Label(title);
        t.setFont(Font.font("Consolas", FontWeight.BOLD, C ? 10 : 12));
        t.setTextFill(D.C_GREEN); t.setMaxWidth(Double.MAX_VALUE); t.setAlignment(Pos.CENTER);
        VBox pane = new VBox(C ? 5 : 7, t, grid);
        pane.setAlignment(Pos.CENTER); pane.setPadding(new Insets(C ? 7 : 10));
        pane.setStyle("-fx-background-color:#080808; -fx-border-color:" + D.GREEN + "33;" +
                "-fx-border-width:1; -fx-border-radius:4; -fx-background-radius:4;");
        HBox.setHgrow(pane, Priority.ALWAYS);
        return pane;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Vehicle type / color ComboBoxes
    // ─────────────────────────────────────────────────────────────────────────
    private ComboBox<String> typeCB(String current, int fs) {
        ComboBox<String> cb = new ComboBox<>();
        cb.setEditable(true);
        cb.getItems().addAll(vehicleTypes());
        String val = nullToEmpty(current);
        cb.setValue(val.isEmpty() ? null : current);
        styledCB(cb, fs);
        return cb;
    }

    private ComboBox<String> colorCB(String current, int fs) {
        ComboBox<String> cb = new ComboBox<>();
        cb.setEditable(true);
        cb.getItems().addAll(vehicleColors());
        String val = nullToEmpty(current);
        cb.setValue(val.isEmpty() ? null : current);
        styledCB(cb, fs);
        return cb;
    }

    private void styledCB(ComboBox<String> cb, int fs) {
        cb.setPrefWidth(180); cb.setMinWidth(80);
        String s = "-fx-background-color:#0a0a0a; -fx-text-fill:" + D.GREEN + ";" +
                "-fx-border-color:" + D.GREEN + "44; -fx-border-width:1;" +
                "-fx-font-family:Consolas; -fx-font-size:" + fs + ";" +
                "-fx-border-radius:3; -fx-background-radius:3;";
        cb.setStyle(s);
        cb.getEditor().setStyle("-fx-background-color:#0a0a0a; -fx-text-fill:" + D.GREEN + ";" +
                "-fx-font-family:Consolas; -fx-font-size:" + fs + ";");
        cb.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setTextFill(D.C_GREEN); setFont(Font.font("Consolas", fs));
                setStyle("-fx-background-color:" + D.BG + ";");
            }
        });
        cb.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "" : item);
                setTextFill(D.C_GREEN); setFont(Font.font("Consolas", fs));
                setStyle("-fx-background-color:#0a0a0a;");
            }
        });
        cb.focusedProperty().addListener((obs, o, f) -> cb.setStyle(f
                ? s.replace(D.GREEN + "44", D.GREEN) + "-fx-effect: dropshadow(gaussian," + D.GREEN + ",5,0.18,0,0);"
                : s));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Manual movement overlay  (admin only)
    // ─────────────────────────────────────────────────────────────────────────
    private void openManualMovementOverlay() {
        final boolean C      = (theme == Theme.COMPACT);
        final int fBtn       = C ? D.CS_BTN   : D.FS_BTN;
        final int fField     = C ? D.CS_FIELD : D.FS_FIELD;
        final int fLabel     = C ? D.CS_LABEL : D.FS_LABEL;

        // ── Mode selector ────────────────────────────────────────────────────
        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton rbRfid   = manualRadio("RFID Code",                           modeGroup, fLabel);
        RadioButton rbPerson = manualRadio(t("Select Person","Επιλογή Ατόμου"), modeGroup, fLabel);
        rbRfid.setSelected(true);
        HBox modeBar = row(18, Pos.CENTER_LEFT, rbRfid, rbPerson);

        // ── RFID pane ────────────────────────────────────────────────────────
        TextField rfidField = fieldTF("", fField);
        rfidField.setPromptText(t("Scan or type UID...","Σαρώστε ή πληκτρολογήστε UID..."));
        rfidField.setPrefWidth(C ? 230 : 270);
        VBox rfidPane = new VBox(6, dimLabel(t("RFID Code:","Κωδικός RFID:"), fLabel), rfidField);

        // ── Person pane ──────────────────────────────────────────────────────
        TextField searchField = fieldTF("", fField);
        searchField.setPromptText(t("Search name / rank...","Αναζήτηση ονόματος / βαθμού..."));
        searchField.setPrefWidth(C ? 230 : 270);

        ObservableList<Person> personItems = FXCollections.observableArrayList(allPersons);
        FilteredList<Person> filteredPeople = new FilteredList<>(personItems, p -> true);
        ListView<Person> personList = new ListView<>(filteredPeople);
        personList.setPrefHeight(C ? 95 : 115);
        personList.setMaxHeight(C ? 95 : 115);
        personList.setStyle("-fx-background-color:" + D.BG + "; -fx-control-inner-background:" + D.BG + ";" +
                "-fx-border-color:" + D.GREEN + "33; -fx-border-width:1;" +
                "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        personList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Person p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setText(null); setStyle("-fx-background-color:" + D.BG + ";"); return; }
                boolean inside = currentlyInsideIds.contains(p.getId());
                setText(safe(p.getMrank()) + "  " + p.getLastName() + " " + p.getFirstName());
                setFont(Font.font("Consolas", fLabel));
                setTextFill(inside ? D.C_GREEN : D.C_RED);
                setStyle(isSelected() ? "-fx-background-color:" + D.BG_PANEL + ";" : "-fx-background-color:" + D.BG + ";");
            }
        });
        searchField.textProperty().addListener((obs, old, text) -> {
            String lower = text == null ? "" : text.toLowerCase();
            filteredPeople.setPredicate(p -> lower.isBlank() ||
                    (p.getFirstName() + " " + p.getLastName() + " " + safe(p.getMrank())).toLowerCase().contains(lower));
        });

        // Current status label
        Label statusLbl = new Label("");
        statusLbl.setFont(Font.font("Consolas", FontWeight.BOLD, fLabel));

        // INSIDE / OUTSIDE direction toggle
        ToggleGroup dirGroup  = new ToggleGroup();
        ToggleButton btnIn    = manualDirBtn(t("▲  INSIDE","▲  ΕΝΤΟΣ"),   D.GREEN, fBtn, dirGroup);
        ToggleButton btnOut   = manualDirBtn(t("▼  OUTSIDE","▼  ΕΚΤΟΣ"), D.RED,   fBtn, dirGroup);
        btnIn.setSelected(true);
        HBox dirBar = row(8, Pos.CENTER_LEFT, btnIn, btnOut);

        // Auto-update status + pre-select direction when person is chosen
        personList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null) { statusLbl.setText(""); return; }
            Movement last = movementService.getLastMovement(sel);
            boolean inside = last != null && "ΕΝΤΟΣ".equals(last.getMovementType());
            statusLbl.setText(inside ? t("▲  Currently INSIDE","▲  Αυτή τη στιγμή ΕΝΤΟΣ") : t("▼  Currently OUTSIDE","▼  Αυτή τη στιγμή ΕΚΤΟΣ"));
            statusLbl.setTextFill(inside ? D.C_GREEN : D.C_RED);
            if (inside) btnOut.setSelected(true); else btnIn.setSelected(true);
        });

        VBox personPane = new VBox(6,
                dimLabel(t("Search:","Αναζήτηση:"), fLabel), searchField, personList,
                statusLbl,
                dimLabel(t("Register as:","Καταχώριση ως:"), fLabel), dirBar);
        personPane.setVisible(false); personPane.setManaged(false);

        // ── Mode switch listener ─────────────────────────────────────────────
        modeGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            boolean isRfid = (sel == rbRfid);
            rfidPane.setVisible(isRfid);    rfidPane.setManaged(isRfid);
            personPane.setVisible(!isRfid); personPane.setManaged(!isRfid);
            Platform.runLater(() -> { if (isRfid) rfidField.requestFocus(); else searchField.requestFocus(); });
        });

        // ── Error label + action buttons ─────────────────────────────────────
        Label errorLbl = new Label("");
        errorLbl.setFont(Font.font("Consolas", FontWeight.BOLD, fLabel));
        errorLbl.setTextFill(D.C_RED);

        Button confirmBtn = accentBtn(t("✔ Submit","✔ Υποβολή"), D.GREEN, fBtn);
        Button cancelBtn  = accentBtn(t("✕ Cancel","✕ Ακύρωση"), D.GREY,  fBtn);

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.75);");

        Runnable doSubmit = () -> {
            if (rbRfid.isSelected()) {
                // ── RFID mode: same as a real card scan ──────────────────────
                String uid = rfidField.getText().trim();
                if (uid.isEmpty()) { errorLbl.setText(t("✘  Enter RFID code.","✘  Εισάγετε κωδικό RFID.")); return; }
                GateAccessResult res = gateService.handleCardRead(uid);
                if (!res.isGranted()) {
                    errorLbl.setText(t("✘  Person not found or inactive for this code.","✘  Το άτομο δεν βρέθηκε ή είναι ανενεργό για αυτόν τον κωδικό."));
                    Toolkit.getDefaultToolkit().beep(); return;
                }
                root.getChildren().remove(overlay);
                showScanOverlay(res.getPerson(), res.getMovementType());
                updateAllPersonsStatus();
                updateLastActivity(res.getPerson(), res.getMovementType());
                messageLabel.setText("✔  " + res.getPersonName() + "  ·  " + moveLabel(res.getMovementType()));
                messageLabel.setTextFill(D.C_GREEN);
            } else {
                // ── Person-selection mode: direct movement log ───────────────
                Person sel = personList.getSelectionModel().getSelectedItem();
                if (sel == null) { errorLbl.setText(t("✘  Select a person from the list.","✘  Επιλέξτε άτομο από τη λίστα.")); return; }
                if (dirGroup.getSelectedToggle() == null) { errorLbl.setText(t("✘  Select direction.","✘  Επιλέξτε κατεύθυνση.")); return; }
                String moveType = (dirGroup.getSelectedToggle() == btnIn) ? "ΕΝΤΟΣ" : "ΕΚΤΟΣ";
                Movement m = new Movement();
                m.setPerson(sel);
                m.setMovementType(moveType);
                m.setMovementTime(LocalDateTime.now());
                m.setGateName(gr.military.gatecontrol.config.AppConfig.getGateName());
                m.setOperator("Admin");
                movementService.logMovement(m);
                root.getChildren().remove(overlay);
                showScanOverlay(sel, moveType);
                updateAllPersonsStatus();
                updateLastActivity(sel, moveType);
                messageLabel.setText("✔  " + sel.getFirstName() + " " + sel.getLastName() + "  ·  " + moveLabel(moveType));
                messageLabel.setTextFill(D.C_GREEN);
            }
            Toolkit.getDefaultToolkit().beep();
            PauseTransition p = new PauseTransition(Duration.seconds(3));
            p.setOnFinished(e -> { messageLabel.setText(t("Waiting for card...","Αναμονή για κάρτα...")); messageLabel.setTextFill(D.C_GREEN); });
            p.play();
        };

        rfidField.setOnAction(e -> doSubmit.run());
        confirmBtn.setOnAction(e -> doSubmit.run());
        cancelBtn.setOnAction(e -> root.getChildren().remove(overlay));

        // ── Card chrome ──────────────────────────────────────────────────────
        Region topAccent = new Region();
        topAccent.setMinHeight(3); topAccent.setMaxHeight(3); topAccent.setMaxWidth(Double.MAX_VALUE);
        topAccent.setStyle("-fx-background-color: linear-gradient(to right, transparent," + D.AMBER + ", transparent);");

        Label title = new Label(t("MANUAL MOVEMENT","ΧΕΙΡΟΚΙΝΗΤΗ ΚΙΝΗΣΗ"));
        title.setFont(Font.font("Consolas", FontWeight.BOLD, C ? 14 : 16));
        title.setTextFill(D.C_AMBER);
        title.setEffect(new DropShadow(12, D.C_AMBER));

        VBox card = new VBox(C ? 10 : 12, topAccent, title, modeBar, divider(),
                rfidPane, personPane, divider(),
                row(8, Pos.CENTER_LEFT, confirmBtn, cancelBtn), errorLbl);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(0, C ? 24 : 28, C ? 18 : 22, C ? 24 : 28));
        card.setMaxWidth(C ? 350 : 420);
        card.setStyle("-fx-background-color: linear-gradient(to bottom, #111108, #050505);" +
                "-fx-border-color:" + D.AMBER + "66; -fx-border-width:1;" +
                "-fx-border-radius:8; -fx-background-radius:8;" +
                "-fx-effect: dropshadow(gaussian," + D.AMBER + ",30,0.4,0,0);");

        overlay.setUserData("dismissable");
        FadeTransition fi = new FadeTransition(Duration.millis(180), card);
        fi.setFromValue(0); fi.setToValue(1);
        overlay.getChildren().add(card);
        root.getChildren().add(overlay);
        fi.play();
        Platform.runLater(rfidField::requestFocus);
    }

    private RadioButton manualRadio(String text, ToggleGroup group, int fs) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(group);
        rb.setFont(Font.font("Consolas", fs));
        rb.setTextFill(D.C_GREEN);
        rb.setStyle("-fx-color:" + D.AMBER + "; -fx-focus-color: transparent;");
        return rb;
    }

    private ToggleButton manualDirBtn(String text, String color, int fs, ToggleGroup group) {
        ToggleButton tb = new ToggleButton(text);
        tb.setToggleGroup(group);
        String base = "-fx-background-color: transparent; -fx-text-fill:" + color + ";" +
                "-fx-border-color:" + color + "66; -fx-border-width:1;" +
                "-fx-font-family:Consolas; -fx-font-size:" + fs + "; -fx-font-weight:bold;" +
                "-fx-padding: 4 16 4 16; -fx-border-radius:4; -fx-background-radius:4;";
        String sel = "-fx-background-color:" + color + "22; -fx-text-fill:" + color + ";" +
                "-fx-border-color:" + color + "; -fx-border-width:2;" +
                "-fx-font-family:Consolas; -fx-font-size:" + fs + "; -fx-font-weight:bold;" +
                "-fx-padding: 4 16 4 16; -fx-border-radius:4; -fx-background-radius:4;" +
                "-fx-effect: dropshadow(gaussian," + color + ",8,0.3,0,0);";
        tb.setStyle(base);
        tb.selectedProperty().addListener((obs, o, n) -> tb.setStyle(n ? sel : base));
        return tb;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Admin login overlay
    // ─────────────────────────────────────────────────────────────────────────
    private void handleAdminLogin() {
        if (isAdmin) {
            isAdmin = false;
            styleAdminButton(false, (theme == Theme.COMPACT) ? D.CS_BTN : D.FS_BTN);
            refreshAdminUI(false);
            return;
        }

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.88);");

        Label icon = new Label("⚙");
        icon.setFont(Font.font(36)); icon.setTextFill(D.C_AMBER); icon.setEffect(new DropShadow(18, D.C_AMBER));

        Label title = new Label(t("ADMINISTRATOR","ΔΙΑΧΕΙΡΙΣΤΗΣ"));
        title.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
        title.setTextFill(D.C_AMBER); title.setEffect(new DropShadow(12, D.C_AMBER));

        Label sub = new Label(t("Enter your password","Εισάγετε τον κωδικό σας"));
        sub.setFont(Font.font("Consolas", 11)); sub.setTextFill(D.C_GREY);

        PasswordField pwField = new PasswordField();
        pwField.setPromptText("••••••••"); pwField.setMaxWidth(260);
        pwField.setStyle("-fx-background-color:#0d0d0d; -fx-text-fill:" + D.GREEN + ";" +
                "-fx-border-color:" + D.AMBER + "55; -fx-border-width:1;" +
                "-fx-font-family:Consolas; -fx-font-size:15px; -fx-padding:8;" +
                "-fx-border-radius:4; -fx-background-radius:4;");
        pwField.focusedProperty().addListener((obs, o, f) ->
                pwField.setStyle(pwField.getStyle().replace(f ? D.AMBER+"55": D.AMBER, f ? D.AMBER : D.AMBER+"55")));

        Label statusLbl = new Label("");
        statusLbl.setFont(Font.font("Consolas", FontWeight.BOLD, 12)); statusLbl.setTextFill(D.C_RED);

        Button loginBtn  = accentBtn(t("LOGIN","ΕΙΣΟΔΟΣ"),   D.AMBER, 13);
        Button cancelBtn = accentBtn(t("CANCEL","ΑΚΥΡΩΣΗ"), D.GREEN, 13);

        Runnable doLogin = () -> {
            if (gr.military.gatecontrol.auth.AdminAuth.verify(pwField.getText())) {
                // Auto-upgrade legacy SHA-256 hash to PBKDF2 on first successful login
                if (gr.military.gatecontrol.auth.AdminAuth.needsHashUpgrade()) {
                    String upgraded = gr.military.gatecontrol.auth.AdminAuth.hashPassword(pwField.getText());
                    if (savePasswordHash(upgraded)) {
                        gr.military.gatecontrol.auth.AdminAuth.setHashFromProperties(upgraded);
                    }
                }
                isAdmin = true;
                styleAdminButton(true, (theme == Theme.COMPACT) ? D.CS_BTN : D.FS_BTN);
                refreshAdminUI(true);
                FadeTransition fo = new FadeTransition(Duration.millis(200), overlay);
                fo.setFromValue(1); fo.setToValue(0);
                fo.setOnFinished(e -> root.getChildren().remove(overlay));
                fo.play();
            } else {
                Toolkit.getDefaultToolkit().beep();
                statusLbl.setText(t("✘  INCORRECT PASSWORD","✘  ΛΑΝΘΑΣΜΕΝΟΣ ΚΩΔΙΚΟΣ"));
                pwField.clear();
            }
        };

        Button changePwBtn = accentBtn(t("Change Password","Αλλαγή Κωδικού"), D.GREY, 11);
        changePwBtn.setOnAction(e -> showChangePasswordDialog());

        loginBtn.setOnAction(e -> doLogin.run());
        pwField.setOnAction(e -> doLogin.run());
        cancelBtn.setOnAction(e -> root.getChildren().remove(overlay));

        Region topAccent = new Region();
        topAccent.setMinHeight(3); topAccent.setMaxHeight(3); topAccent.setMaxWidth(Double.MAX_VALUE);
        topAccent.setStyle("-fx-background-color: linear-gradient(to right, transparent, " + D.AMBER + ", transparent);");

        VBox card = new VBox(14, topAccent, icon, title, sub, pwField,
                row(10, Pos.CENTER, loginBtn, cancelBtn), statusLbl, changePwBtn);
        card.setAlignment(Pos.CENTER); card.setPadding(new Insets(0, 36, 28, 36)); card.setMaxWidth(340);
        card.setStyle("-fx-background-color: linear-gradient(to bottom, #111108, #050505);" +
                "-fx-border-color:" + D.AMBER + "66; -fx-border-width:1;" +
                "-fx-border-radius:8; -fx-background-radius:8;" +
                "-fx-effect: dropshadow(gaussian," + D.AMBER + ",30,0.4,0,0);");

        overlay.setUserData("dismissable");
        FadeTransition fi = new FadeTransition(Duration.millis(180), card);
        fi.setFromValue(0); fi.setToValue(1);
        overlay.getChildren().add(card);
        root.getChildren().add(overlay);
        fi.play();
        Platform.runLater(pwField::requestFocus);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data loading
    // ─────────────────────────────────────────────────────────────────────────
    private void loadAllPersons() {
        List<Person> all = movementService.getAllPersons();
        List<Person> visible = isAdmin ? all : all.stream().filter(Person::isActive).toList();
        // Active persons first, then sorted by rank → last name
        List<Person> sorted = visible.stream()
                .sorted(Comparator.comparingInt((Person p) -> p.isActive() ? 0 : 1)
                        .thenComparing(p -> safe(p.getMrank()))
                        .thenComparing(p -> safe(p.getLastName())))
                .toList();
        allPersons.setAll(sorted);
        rebuildInsideSet(sorted);
        refreshRankFilter();
        listView.refresh();
        updateStats();
    }

    /** Repopulates the rank ComboBox with distinct ranks from the current person list. */
    private void refreshRankFilter() {
        if (rankFilter == null) return;
        String current = rankFilter.getValue();
        rankFilter.getItems().clear();
        rankFilter.getItems().add(t("ALL","ΟΛΟΙ"));
        allPersons.stream()
                .map(Person::getMrank)
                .filter(r -> r != null && !r.isBlank())
                .distinct()
                .sorted()
                .forEach(rankFilter.getItems()::add);
        // Restore selection if it still exists, otherwise reset to ALL
        if (current != null && rankFilter.getItems().contains(current))
            rankFilter.setValue(current);
        else
            rankFilter.setValue(t("ALL","ΟΛΟΙ"));
    }

    private void updateAllPersonsStatus() {
        rebuildInsideSet(new java.util.ArrayList<>(allPersons));
        listView.refresh();
        updateStats();
    }

    private void updateLastActivity(Person person, String movementType) {
        boolean in = "ΕΝΤΟΣ".equals(movementType);
        String time = LocalDateTime.now().format(TIME_FMT);
        // Persist across theme switches
        lastActivityText  = (in ? "▲" : "▼") + "  " + moveLabel(movementType)
                + "  ·  " + safe(person.getMrank())
                + "  " + person.getLastName().toUpperCase() + " " + person.getFirstName()
                + "  ·  " + time;
        lastActivityColor = in ? D.C_GREEN : D.C_RED;
        if (lastActivityLabel != null) {
            lastActivityLabel.setText(lastActivityText);
            lastActivityLabel.setTextFill(lastActivityColor);
        }
    }

    private void rebuildInsideSet(List<Person> persons) {
        currentlyInsideIds.clear();
        movementService.getLastMovementsForAll().stream()
                .filter(m -> "ΕΝΤΟΣ".equals(m.getMovementType()))
                .map(m -> m.getPerson().getId())
                .forEach(currentlyInsideIds::add);
    }

    private void refreshAdminUI(boolean admin) {
        setVM(addPersonButton, admin); setVM(manualMovementBtn, admin);
        setVM(exportExcelBtn, admin); setVM(exportAllExcelBtn, admin);
        setVM(exportInsideBtn, admin); setVM(backupDbBtn, admin);
        loadAllPersons(); // admin sees inactive persons; non-admin sees only active
        if (messageLabel != null) {
            messageLabel.setText(admin ? t("⚙  ADMINISTRATOR MODE","⚙  ΛΕΙΤΟΥΡΓΙΑ ΔΙΑΧΕΙΡΙΣΤΗ") : t("Waiting for card...","Αναμονή για κάρτα..."));
            messageLabel.setTextFill(admin ? D.C_AMBER : D.C_GREEN);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Excel export
    // ─────────────────────────────────────────────────────────────────────────
    private static final String[] COLS_EN = {"RANK","FULL NAME","DATE","TIME","STATUS"};
    private static final String[] COLS_GR = {"ΒΑΘΜΟΣ","ΟΝΟΜΑΤΕΠΩΝΥΜΟ","ΗΜΕΡΟΜΗΝΙΑ","ΩΡΑ","ΚΑΤΑΣΤΑΣΗ"};
    private static String[] cols() { return langEn ? COLS_EN : COLS_GR; }

    private void exportPersonHistory(Stage stage, DatePicker from, DatePicker to) {
        if (historyTable.getItems().isEmpty()) { showWarning(t("No data available.","Δεν υπάρχουν δεδομένα.")); return; }
        File file = excelChooser(stage, t("Save Excel","Αποθήκευση Excel"), "movement_history.xlsx");
        if (file == null) return;
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("History"); xlsHeader(wb, sh); int r = 1;
            for (Movement m : historyTable.getItems()) xlsRow(sh, r++, m);
            xlsAutosize(sh);
            try (FileOutputStream fos = new FileOutputStream(file)) { wb.write(fos); }
            showInfo(t("Excel file created.","Αρχείο Excel δημιουργήθηκε."));
        } catch (Exception ex) { ex.printStackTrace(); showError(t("Error","Σφάλμα"), t("Excel export failed.","Αποτυχία εξαγωγής Excel.")); }
    }

    private void exportAllMovements(Stage stage, DatePicker from, DatePicker to) {
        List<Movement> list = movementService.getAll().stream()
                .filter(m -> from.getValue() == null || !m.getMovementTime().toLocalDate().isBefore(from.getValue()))
                .filter(m -> to.getValue()   == null || !m.getMovementTime().toLocalDate().isAfter(to.getValue()))
                .toList();
        File file = excelChooser(stage, t("Export All","Εξαγωγή Όλων"), "all_movements.xlsx");
        if (file == null) return;
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("Movements"); xlsHeader(wb, sh); int r = 1;
            for (Movement m : list) xlsRow(sh, r++, m);
            xlsAutosize(sh);
            try (FileOutputStream fos = new FileOutputStream(file)) { wb.write(fos); }
            showInfo(t("Export successful.","Εξαγωγή επιτυχής."));
        } catch (Exception ex) { ex.printStackTrace(); showError(t("Error","Σφάλμα"), t("Export failed.","Αποτυχία εξαγωγής.")); }
    }

    private void exportCurrentlyInside(Stage stage) {
        List<Person> inside = allPersons.stream()
                .filter(p -> currentlyInsideIds.contains(p.getId()))
                .sorted(Comparator.comparing(Person::getMrank,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(p -> p.getLastName() + p.getFirstName()))
                .toList();
        if (inside.isEmpty()) { showWarning(t("No personnel currently inside.","Δεν υπάρχει προσωπικό εντός αυτή τη στιγμή.")); return; }
        File file = excelChooser(stage, t("Export Inside","Εξαγωγή Εντός"), "inside_personnel.xlsx");
        if (file == null) return;
        String[] cols = langEn ? new String[]{"RANK", "FULL NAME", "RFID"} : new String[]{"ΒΑΘΜΟΣ", "ΟΝΟΜΑΤΕΠΩΝΥΜΟ", "RFID"};
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("Inside");
            CellStyle s = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font f = wb.createFont(); f.setBold(true); s.setFont(f);
            Row hdr = sh.createRow(0);
            for (int i = 0; i < cols.length; i++) { Cell c = hdr.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(s); }
            int r = 1;
            for (Person p : inside) {
                Row row = sh.createRow(r++);
                row.createCell(0).setCellValue(safe(p.getMrank()));
                row.createCell(1).setCellValue(safe(p.getFirstName()) + " " + safe(p.getLastName()));
                row.createCell(2).setCellValue(safe(p.getRfidUid()));
            }
            for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
            try (FileOutputStream fos = new FileOutputStream(file)) { wb.write(fos); }
            showInfo(t("Inside export successful (","Εξαγωγή εντός επιτυχής (") + inside.size() + t(" persons)."," άτομα)."));
        } catch (Exception ex) { ex.printStackTrace(); showError(t("Error","Σφάλμα"), t("Export failed.","Αποτυχία εξαγωγής.")); }
    }

    /**
     * Excel export called from the Inside Tracker overlay.
     * Includes entry time, which the generic exportCurrentlyInside() does not have.
     */
    private void exportInsideListToExcel(List<Person> inside, Map<Long, LocalDateTime> entryTimes) {
        if (inside.isEmpty()) { showWarning(t("No personnel currently inside.","Δεν υπάρχει προσωπικό εντός αυτή τη στιγμή.")); return; }
        File file = excelChooser(mainStage, t("Export Inside","Εξαγωγή Εντός"), "inside_personnel.xlsx");
        if (file == null) return;
        String[] cols = langEn ? new String[]{"RANK","FULL NAME","RFID","ENTRY TIME","DATE"} : new String[]{"ΒΑΘΜΟΣ","ΟΝΟΜΑΤΕΠΩΝΥΜΟ","RFID","ΩΡΑ ΕΙΣΟΔΟΥ","ΗΜΕΡΟΜΗΝΙΑ"};
        DateTimeFormatter tFmt = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("Inside");
            CellStyle hdrStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font hdrFont = wb.createFont();
            hdrFont.setBold(true); hdrStyle.setFont(hdrFont);
            Row hdr = sh.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = hdr.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(hdrStyle);
            }
            int r = 1;
            for (Person p : inside) {
                Row row = sh.createRow(r++);
                LocalDateTime et = entryTimes.get(p.getId());
                row.createCell(0).setCellValue(safe(p.getMrank()));
                row.createCell(1).setCellValue(safe(p.getFirstName()) + " " + safe(p.getLastName()));
                row.createCell(2).setCellValue(safe(p.getRfidUid()));
                row.createCell(3).setCellValue(et != null ? et.format(tFmt) : "-");
                row.createCell(4).setCellValue(et != null ? et.format(dFmt) : "-");
            }
            for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
            try (FileOutputStream fos = new FileOutputStream(file)) { wb.write(fos); }
            showInfo(t("Inside export successful (","Εξαγωγή εντός επιτυχής (") + inside.size() + t(" persons)."," άτομα)."));
        } catch (Exception ex) { ex.printStackTrace(); showError(t("Error","Σφάλμα"), t("Export failed.","Αποτυχία εξαγωγής.")); }
    }

    private void xlsHeader(Workbook wb, Sheet sh) {
        CellStyle s = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font f = wb.createFont(); f.setBold(true); s.setFont(f);
        Row r = sh.createRow(0);
        String[] cols = cols(); for (int i = 0; i < cols.length; i++) { Cell c = r.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(s); }
    }

    private void xlsRow(Sheet sh, int n, Movement m) {
        Person p = m.getPerson(); Row r = sh.createRow(n);
        r.createCell(0).setCellValue(safe(p.getMrank()));
        r.createCell(1).setCellValue(safe(p.getFirstName()) + " " + safe(p.getLastName()));
        r.createCell(2).setCellValue(m.getMovementTime().toLocalDate().toString());
        r.createCell(3).setCellValue(m.getMovementTime().toLocalTime().format(TIME_FMT));
        r.createCell(4).setCellValue(moveLabel(m.getMovementType()));
    }

    private void xlsAutosize(Sheet sh) { for (int i = 0; i < cols().length; i++) sh.autoSizeColumn(i); }

    private File excelChooser(Stage stage, String title, String name) {
        FileChooser fc = new FileChooser(); fc.setTitle(title); fc.setInitialFileName(name);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        return fc.showSaveDialog(stage);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Builders
    // ─────────────────────────────────────────────────────────────────────────
    private ComboBox<String> buildRankFilter(int fs) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().add(t("ALL","ΟΛΟΙ"));
        allPersons.stream().map(Person::getMrank)
                .filter(r -> r != null && !r.isBlank()).distinct().sorted().forEach(cb.getItems()::add);
        cb.setValue(t("ALL","ΟΛΟΙ")); cb.setPrefWidth(fs <= 12 ? 100 : 125);
        String s = "-fx-background-color:#0a0a0a; -fx-text-fill:" + D.GREEN + ";" +
                "-fx-border-color:" + D.GREEN + "44; -fx-border-width:1;" +
                "-fx-font-family:Consolas; -fx-font-size:" + fs + ";";
        cb.setStyle(s);
        cb.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty); setText(empty || item == null ? null : item);
                setTextFill(D.C_GREEN); setFont(Font.font("Consolas", fs)); setStyle("-fx-background-color:" + D.BG + ";");
            }
        });
        cb.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty); setText(item == null ? t("ALL","ΟΛΟΙ") : item);
                setTextFill(D.C_GREEN); setFont(Font.font("Consolas", fs)); setStyle("-fx-background-color:#0a0a0a;");
            }
        });
        return cb;
    }

    private ImageView buildPhotoView(Person person, double w, double h) {
        Image photo;
        try {
            if (person.getPhoto() != null && person.getPhoto().length > 0)
                photo = new Image(new ByteArrayInputStream(person.getPhoto()));
            else {
                InputStream def = getClass().getResourceAsStream("/images/soldier_default.png");
                if (def == null) throw new FileNotFoundException("soldier_default.png not found");
                photo = new Image(def);
            }
        } catch (Exception ex) { throw new RuntimeException("Could not load photo", ex); }
        ImageView v = new ImageView(photo);
        v.setFitWidth(w); v.setFitHeight(h); v.setPreserveRatio(true);
        v.setStyle("-fx-effect: dropshadow(gaussian, rgba(57,255,20,0.25), 8, 0.25, 0, 0);");
        return v;
    }

    private void styleDatePicker(DatePicker picker, int fs) {
        picker.setStyle("-fx-background-color:#0a0a0a; -fx-border-color:" + D.GREEN + "44;" +
                "-fx-border-width:1; -fx-font-family:Consolas; -fx-font-size:" + fs + ";");
        Platform.runLater(() -> {
            picker.getEditor().setStyle("-fx-background-color:#0a0a0a; -fx-text-fill:" + D.GREEN + ";" +
                    "-fx-font-family:Consolas; -fx-font-size:" + fs + ";");
            Node arr = picker.lookup(".arrow-button");
            if (arr != null) arr.setStyle("-fx-background-color:#0a0a0a; -fx-border-color:" + D.GREEN + "44;");
            Node ico = picker.lookup(".arrow");
            if (ico != null) ico.setStyle("-fx-background-color:" + D.GREEN + ";");
        });
    }

    private void styleAdminButton(boolean loggedIn, int fs) {
        if (adminButton == null) return;

        // Stop any running breath animation before applying new style
        if (adminBreathTl != null) { adminBreathTl.stop(); adminBreathTl = null; }

        if (loggedIn) {
            adminButton.setText(t("⚙  ADMIN  ✕","⚙  ΔΙΑΧ.  ✕"));
            // CSS without -fx-effect so the Java DropShadow (animated below) takes precedence
            String base  = "-fx-background-color:" + D.AMBER + "2a; -fx-text-fill:" + D.AMBER + ";" +
                    "-fx-border-color:" + D.AMBER + "; -fx-border-width:1;" +
                    "-fx-font-family:Consolas; -fx-font-size:" + fs + "; -fx-font-weight:bold;" +
                    "-fx-padding:4 12 4 12; -fx-border-radius:4; -fx-background-radius:4;";
            String hover = "-fx-background-color:" + D.AMBER + "44; -fx-text-fill:" + D.AMBER + ";" +
                    "-fx-border-color:" + D.AMBER + "; -fx-border-width:1;" +
                    "-fx-font-family:Consolas; -fx-font-size:" + fs + "; -fx-font-weight:bold;" +
                    "-fx-padding:4 12 4 12; -fx-border-radius:4; -fx-background-radius:4;";
            adminButton.setStyle(base);
            adminButton.setOnMouseEntered(e -> adminButton.setStyle(hover));
            adminButton.setOnMouseExited(e  -> adminButton.setStyle(base));

            // Breathing glow — DropShadow radius pulses between 6 and 22 (~2 s cycle)
            DropShadow ds = new DropShadow(6, Color.web(D.AMBER));
            adminButton.setEffect(ds);
            adminBreathTl = new Timeline(
                    new KeyFrame(Duration.millis(0),    new KeyValue(ds.radiusProperty(), 6)),
                    new KeyFrame(Duration.millis(1000), new KeyValue(ds.radiusProperty(), 22)),
                    new KeyFrame(Duration.millis(2000), new KeyValue(ds.radiusProperty(), 6)));
            adminBreathTl.setCycleCount(Timeline.INDEFINITE);
            adminBreathTl.play();
        } else {
            adminButton.setText(t("Administrator","Διαχειριστής"));
            adminButton.setEffect(null);   // remove glow
            String base = accentBtnStyle(D.AMBER, fs), hover = accentBtnHover(D.AMBER, fs);
            adminButton.setStyle(base);
            adminButton.setOnMouseEntered(e -> adminButton.setStyle(hover));
            adminButton.setOnMouseExited(e  -> adminButton.setStyle(base));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Clock
    // ─────────────────────────────────────────────────────────────────────────
    private void startClock() {
        if (clockTimeline != null) clockTimeline.stop();
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            LocalDateTime now = LocalDateTime.now();
            if (clockLabel != null) clockLabel.setText(now.format(TIME_FMT));
            if (dateLabel  != null) dateLabel.setText(now.toLocalDate().format(DATE_FMT));
        }));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Active/inactive toggle style
    // ─────────────────────────────────────────────────────────────────────────
    private void styleActiveToggle(ToggleButton tb, boolean active, int fs) {
        tb.setText(active ? t("✔ Active","✔ Ενεργός") : t("✖ Inactive","✖ Ανενεργός"));
        String color = active ? D.GREEN : D.RED;
        String style = "-fx-background-color: transparent; -fx-text-fill:" + color + ";" +
                "-fx-border-color:" + color + (active ? "66" : "88") + "; -fx-border-width:1;" +
                "-fx-font-family:Consolas; -fx-font-size:" + fs + "; -fx-font-weight:bold;" +
                "-fx-padding: 4 12 4 12; -fx-border-radius:4; -fx-background-radius:4;";
        tb.setStyle(style);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Style factories
    // ─────────────────────────────────────────────────────────────────────────
    private String panelStyle() { return "-fx-background-color:" + D.BG_PANEL + ";"; }

    private String accentBtnStyle(String color, int fs) {
        return "-fx-background-color: transparent; -fx-text-fill:" + color + ";" +
                "-fx-border-color:" + color + "66; -fx-border-width:1;" +
                "-fx-font-family:Consolas; -fx-font-size:" + fs + "; -fx-font-weight:bold;" +
                "-fx-padding: 4 12 4 12; -fx-border-radius:4; -fx-background-radius:4;";
    }

    private String accentBtnHover(String color, int fs) {
        return "-fx-background-color:" + color + "1a; -fx-text-fill:" + color + ";" +
                "-fx-border-color:" + color + "; -fx-border-width:1;" +
                "-fx-font-family:Consolas; -fx-font-size:" + fs + "; -fx-font-weight:bold;" +
                "-fx-padding: 4 12 4 12; -fx-border-radius:4; -fx-background-radius:4;" +
                "-fx-effect: dropshadow(gaussian," + color + ",8,0.25,0,0);";
    }

    private String ghostBtnStyle(int fs) {
        return "-fx-background-color: transparent; -fx-text-fill:" + D.GREEN + ";" +
                "-fx-border-color:" + D.GREEN + "33; -fx-border-width:1;" +
                "-fx-font-family:Consolas; -fx-font-size:" + fs + ";" +
                "-fx-padding:3 8 3 8; -fx-border-radius:3; -fx-background-radius:3;";
    }

    private String fieldStyleFor(int fs) {
        return "-fx-background-color:#0a0a0a; -fx-text-fill:" + D.GREEN + ";" +
                "-fx-border-color:" + D.GREEN + "44; -fx-border-width:1;" +
                "-fx-font-family:Consolas; -fx-font-size:" + fs + ";" +
                "-fx-border-radius:3; -fx-background-radius:3;";
    }

    private String fieldFocusStyleFor(int fs) {
        return "-fx-background-color:#0a0a0a; -fx-text-fill:" + D.GREEN + ";" +
                "-fx-border-color:" + D.GREEN + "; -fx-border-width:1;" +
                "-fx-font-family:Consolas; -fx-font-size:" + fs + ";" +
                "-fx-border-radius:3; -fx-background-radius:3;" +
                "-fx-effect: dropshadow(gaussian," + D.GREEN + ",5,0.18,0,0);";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Widget builders
    // ─────────────────────────────────────────────────────────────────────────
    private Button accentBtn(String text, String color, int fs) {
        Button b = new Button(text);
        String base = accentBtnStyle(color, fs), hover = accentBtnHover(color, fs);
        b.setStyle(base); b.setOnMouseEntered(e -> b.setStyle(hover)); b.setOnMouseExited(e -> b.setStyle(base));
        return b;
    }

    private Button ghostBtn(String text, int fs, String color) {
        Button b = new Button(text);
        String base = ghostBtnStyle(fs);
        String hover = "-fx-background-color:" + color + "15; -fx-text-fill:" + color + ";" +
                "-fx-border-color:" + color + "88; -fx-border-width:1;" +
                "-fx-font-family:Consolas; -fx-font-size:" + fs + ";" +
                "-fx-padding:3 8 3 8; -fx-border-radius:3; -fx-background-radius:3;";
        b.setStyle(base); b.setOnMouseEntered(e -> b.setStyle(hover)); b.setOnMouseExited(e -> b.setStyle(base));
        return b;
    }

    private TextField fieldTF(String value, int fs) {
        TextField tf = new TextField(value); tf.setPrefWidth(180); tf.setMinWidth(80);
        tf.setStyle(fieldStyleFor(fs));
        tf.focusedProperty().addListener((obs, o, f) -> tf.setStyle(f ? fieldFocusStyleFor(fs) : fieldStyleFor(fs)));
        return tf;
    }

    /** Like fieldTF but stretches to fill available width (for GridPane column-fill layouts). */
    private TextField wideField(String value, int fs) {
        TextField tf = new TextField(value);
        tf.setMaxWidth(Double.MAX_VALUE);
        tf.setStyle(fieldStyleFor(fs));
        tf.focusedProperty().addListener((obs, o, f) -> tf.setStyle(f ? fieldFocusStyleFor(fs) : fieldStyleFor(fs)));
        return tf;
    }

    private void applyFieldStyle(TextField tf, int fs) {
        tf.setStyle(fieldStyleFor(fs));
        tf.focusedProperty().addListener((obs, o, f) -> tf.setStyle(f ? fieldFocusStyleFor(fs) : fieldStyleFor(fs)));
    }

    private Label formKey(String text, int fs) {
        Label l = new Label(text); l.setFont(Font.font("Consolas", fs)); l.setTextFill(Color.web(D.GREY_LIGHT)); return l;
    }

    private Label valLbl(String value, int fs) {
        Label l = new Label(safe(value)); l.setFont(Font.font("Consolas", fs)); l.setTextFill(D.C_GREEN); return l;
    }

    private Label dimLabel(String text, int fs) {
        Label l = new Label(text); l.setFont(Font.font("Consolas", fs)); l.setTextFill(D.C_GREY); return l;
    }

    private HBox sectionHeader(String text, int fs) {
        Label l = new Label(text); l.setFont(Font.font("Consolas", FontWeight.BOLD, fs)); l.setTextFill(D.C_GREEN);
        HBox box = new HBox(l); box.setAlignment(Pos.CENTER_LEFT); box.setPadding(new Insets(4, 10, 4, 10));
        box.setStyle("-fx-background-color:#060606; -fx-border-color: rgba(57,255,20,0.15); -fx-border-width: 0 0 1 0;");
        return box;
    }

    private Region divider() {
        Region r = new Region(); r.setMinHeight(1); r.setMaxHeight(1); r.setMaxWidth(Double.MAX_VALUE);
        r.setStyle("-fx-background-color: rgba(57,255,20,0.12);"); return r;
    }

    private HBox row(int spacing, Pos alignment, Node... nodes) {
        HBox box = new HBox(spacing, nodes); box.setAlignment(alignment); return box;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────────
    private static String moveLabel(String type) {
        if ("ΕΝΤΟΣ".equals(type)) return t("INSIDE", "ΕΝΤΟΣ");
        if ("ΕΚΤΟΣ".equals(type)) return t("OUTSIDE", "ΕΚΤΟΣ");
        return type != null ? type : "-";
    }

    private static String safe(String s)        { return (s == null || s.isBlank()) ? "-" : s; }
    private static String nullToEmpty(String s) { return (s == null || s.isBlank() || "-".equals(s.trim())) ? "" : s; }
    private static String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
    private static void setVM(Node node, boolean v) { if (node != null) { node.setVisible(v); node.setManaged(v); } }
    private static void closeStage(Node node)   { ((Stage) node.getScene().getWindow()).close(); }

    // ─────────────────────────────────────────────────────────────────────────
    // Alerts
    // ─────────────────────────────────────────────────────────────────────────
    // ── Fullscreen-safe alert helpers ─────────────────────────────────────────
    // JavaFX alerts go behind the window on Windows when in fullscreen.
    // Solution: exit fullscreen, show alert, restore fullscreen.
    private void showAlertFS(Alert a) {
        boolean wasFS = mainStage.isFullScreen();
        if (wasFS) mainStage.setFullScreen(false);
        a.initOwner(mainStage);
        try (java.io.InputStream is = getClass().getResourceAsStream("/app.ico")) {
            if (is != null) ((Stage) a.getDialogPane().getScene().getWindow()).getIcons().add(new Image(is));
        } catch (Exception ignored) {}
        a.showAndWait();
        if (wasFS) mainStage.setFullScreen(true);
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setHeaderText(null); a.setContentText(msg);
        showAlertFS(a);
    }
    private void showWarning(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING); a.setHeaderText(null); a.setContentText(msg);
        showAlertFS(a);
    }
    private void showError(String h, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(t("Error","Σφάλμα")); a.setHeaderText(h); a.setContentText(msg);
        showAlertFS(a);
    }
    private java.util.Optional<ButtonType> showConfirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        showAlertFS(a);
        return a.getResult() != null ? java.util.Optional.of(a.getResult()) : java.util.Optional.empty();
    }
    // ─────────────────────────────────────────────────────────────────────────
    // Change password dialog
    // ─────────────────────────────────────────────────────────────────────────
    private void showChangePasswordDialog() {
        Stage dialog = new Stage();
        dialog.initOwner(mainStage);
        dialog.initModality(Modality.WINDOW_MODAL);
        applyAppIcon(dialog);
        dialog.setTitle(t("Change Admin Password","Αλλαγή Κωδικού Διαχειριστή"));

        Label title = new Label(t("CHANGE PASSWORD","ΑΛΛΑΓΗ ΚΩΔΙΚΟΥ"));
        title.setFont(Font.font("Consolas", FontWeight.BOLD, 15));
        title.setTextFill(D.C_AMBER);

        PasswordField oldPw  = styledPwField(t("Current password","Τρέχων κωδικός"));
        PasswordField newPw  = styledPwField(t("New password","Νέος κωδικός"));
        PasswordField newPw2 = styledPwField(t("Confirm new password","Επιβεβαίωση νέου κωδικού"));

        Label status = new Label("");
        status.setFont(Font.font("Consolas", 11));
        status.setTextFill(D.C_RED);

        Button saveBtn   = accentBtn(t("💾 Save","💾 Αποθήκευση"),   D.GREEN, 12);
        Button cancelBtn = accentBtn(t("✕ Cancel","✕ Ακύρωση"), D.GREY,  12);
        cancelBtn.setOnAction(e -> dialog.close());

        saveBtn.setOnAction(e -> {
            String oldVal  = oldPw.getText();
            String newVal  = newPw.getText();
            String newVal2 = newPw2.getText();

            if (!gr.military.gatecontrol.auth.AdminAuth.verify(oldVal)) {
                status.setText(t("✘ Incorrect current password.","✘ Λανθασμένος τρέχων κωδικός."));
                oldPw.clear(); return;
            }
            if (newVal.length() < 4) {
                status.setText(t("✘ New password must be at least 4 characters.","✘ Ο νέος κωδικός πρέπει να έχει τουλάχιστον 4 χαρακτήρες."));
                return;
            }
            if (!newVal.equals(newVal2)) {
                status.setText(t("✘ New passwords do not match.","✘ Οι νέοι κωδικοί δεν ταιριάζουν."));
                newPw2.clear(); return;
            }

            String newHash = gr.military.gatecontrol.auth.AdminAuth.hashPassword(newVal);
            if (savePasswordHash(newHash)) {
                gr.military.gatecontrol.auth.AdminAuth.setHashFromProperties(newHash);
                status.setTextFill(D.C_GREEN);
                status.setText(t("✔ Password changed successfully.","✔ Ο κωδικός άλλαξε επιτυχώς."));
                saveBtn.setDisable(true);
                PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                delay.setOnFinished(ev -> dialog.close());
                delay.play();
            } else {
                status.setText(t("✘ Failed to save to db.properties.","✘ Αποτυχία αποθήκευσης στο db.properties."));
            }
        });

        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        grid.add(formKey(t("Current password:","Τρέχων κωδικός:"), 12), 0, 0); grid.add(oldPw,  1, 0);
        grid.add(formKey(t("New password:","Νέος κωδικός:"), 12),       0, 1); grid.add(newPw,  1, 1);
        grid.add(formKey(t("Confirm:","Επιβεβαίωση:"), 12),             0, 2); grid.add(newPw2, 1, 2);

        VBox content = new VBox(14, title, divider(), grid, row(8, Pos.CENTER_LEFT, saveBtn, cancelBtn), status);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: linear-gradient(to bottom, #111108, #050505);" +
                "-fx-border-color:" + D.AMBER + "55; -fx-border-width:1;" +
                "-fx-border-radius:6; -fx-background-radius:6;");

        dialog.setScene(new Scene(content, 420, 280));
        dialog.show();
        oldPw.requestFocus();
    }

    private PasswordField styledPwField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt); pf.setPrefWidth(200);
        pf.setStyle("-fx-background-color:#0a0a0a; -fx-text-fill:" + D.GREEN + ";" +
                "-fx-border-color:" + D.GREEN + "44; -fx-border-width:1;" +
                "-fx-font-family:Consolas; -fx-font-size:12px;" +
                "-fx-border-radius:3; -fx-background-radius:3;");
        pf.focusedProperty().addListener((obs, o, f) -> pf.setStyle(f
                ? "-fx-background-color:#0a0a0a; -fx-text-fill:" + D.GREEN + ";" +
                  "-fx-border-color:" + D.GREEN + "; -fx-border-width:1;" +
                  "-fx-font-family:Consolas; -fx-font-size:12px;" +
                  "-fx-border-radius:3; -fx-background-radius:3;" +
                  "-fx-effect: dropshadow(gaussian," + D.GREEN + ",5,0.18,0,0);"
                : "-fx-background-color:#0a0a0a; -fx-text-fill:" + D.GREEN + ";" +
                  "-fx-border-color:" + D.GREEN + "44; -fx-border-width:1;" +
                  "-fx-font-family:Consolas; -fx-font-size:12px;" +
                  "-fx-border-radius:3; -fx-background-radius:3;"));
        return pf;
    }

    /**
     * Writes the new admin.password.hash into db.properties using a line-by-line
     * replace so that no Properties escaping is applied (which would corrupt the
     * ENC: encrypted password value and db.url colons).
     * Returns true on success.
     */
    private boolean savePasswordHash(String newHash) {
        try {
            java.nio.file.Path propsPath = resolveDbPropertiesPath();
            if (propsPath == null || !propsPath.toFile().exists()) {
                System.err.println("savePasswordHash: db.properties not found");
                return false;
            }
            if (!propsPath.toFile().canWrite()) {
                System.err.println("savePasswordHash: db.properties is not writable: " + propsPath);
                return false;
            }

            java.util.List<String> lines = java.nio.file.Files.readAllLines(
                    propsPath, java.nio.charset.StandardCharsets.UTF_8);
            java.util.List<String> updated = new java.util.ArrayList<>();
            boolean replaced = false;

            for (String line : lines) {
                if (line.startsWith("admin.password.hash=")) {
                    updated.add("admin.password.hash=" + newHash);
                    replaced = true;
                } else {
                    updated.add(line);
                }
            }
            if (!replaced) {          // key missing from file — append it
                updated.add("admin.password.hash=" + newHash);
            }

            java.nio.file.Files.write(propsPath, updated, java.nio.charset.StandardCharsets.UTF_8);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Currently Inside Base tracker overlay
    // ─────────────────────────────────────────────────────────────────────────

    private void openInsideTracker() {
        final boolean C      = (theme == Theme.COMPACT);
        final int fBtn       = C ? D.CS_BTN   : D.FS_BTN;
        final int fLabel     = C ? D.CS_LABEL : D.FS_LABEL;

        // Build personId → entry-time map from the cached last-movements query
        Map<Long, LocalDateTime> entryTimes = movementService.getLastMovementsForAll().stream()
                .filter(m -> "ΕΝΤΟΣ".equals(m.getMovementType()))
                .collect(Collectors.toMap(
                        m -> m.getPerson().getId(),
                        Movement::getMovementTime,
                        (a, b) -> a));  // keep first if duplicates (shouldn't happen)

        List<Person> inside = allPersons.stream()
                .filter(p -> currentlyInsideIds.contains(p.getId()))
                .sorted(Comparator.comparing(p -> safe(p.getMrank())))
                .toList();

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.80);");
        overlay.setUserData("dismissable");

        // Decorative top bar
        Region topAccent = new Region();
        topAccent.setMinHeight(3); topAccent.setMaxHeight(3); topAccent.setMaxWidth(Double.MAX_VALUE);
        topAccent.setStyle("-fx-background-color: linear-gradient(to right, transparent," + D.GREEN + ", transparent);");

        Label title = new Label(t("PERSONNEL INSIDE BASE","ΠΡΟΣΩΠΙΚΟ ΕΝΤΟΣ ΒΑΣΗΣ") + "    [ " + inside.size() + " ]");
        title.setFont(Font.font("Consolas", FontWeight.BOLD, C ? 14 : 16));
        title.setTextFill(D.C_GREEN);
        title.setEffect(new DropShadow(12, D.C_GREEN));

        // Person rows
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm  dd/MM");
        VBox cardList = new VBox(0);
        if (inside.isEmpty()) {
            Label empty = new Label(t("No personnel currently inside base.","Δεν υπάρχει προσωπικό εντός βάσης αυτή τη στιγμή."));
            empty.setFont(Font.font("Consolas", fLabel));
            empty.setTextFill(D.C_GREY);
            empty.setPadding(new Insets(16));
            cardList.getChildren().add(empty);
        } else {
            for (Person p : inside) {
                cardList.getChildren().add(
                        buildInsidePersonCard(p, entryTimes.get(p.getId()), timeFmt, fLabel, C));
            }
        }

        ScrollPane scroll = new ScrollPane(cardList);
        scroll.setFitToWidth(true);
        double rowH = C ? 54.0 : 64.0;
        scroll.setPrefHeight(Math.min(inside.size() * rowH + 20, C ? 340 : 400));
        scroll.setMinHeight(64);
        scroll.setStyle("-fx-background:" + D.BG + "; -fx-background-color:" + D.BG + ";");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button excelInsideBtn = accentBtn("⬇ Excel", D.AMBER, fBtn);
        excelInsideBtn.setTooltip(new Tooltip(t("Export inside list to Excel (with entry time)","Εξαγωγή λίστας εντός σε Excel (με ώρα εισόδου)")));
        excelInsideBtn.setOnAction(e -> exportInsideListToExcel(inside, entryTimes));

        Button refreshBtn = accentBtn(t("↺ Refresh","↺ Ανανέωση"), D.AMBER, fBtn);
        refreshBtn.setOnAction(e -> {
            root.getChildren().remove(overlay);
            updateAllPersonsStatus();
            openInsideTracker();
        });
        Button closeBtn = accentBtn(t("✕ Close","✕ Κλείσιμο"), D.GREEN, fBtn);
        closeBtn.setOnAction(e -> {
            FadeTransition fo = new FadeTransition(Duration.millis(200), overlay);
            fo.setFromValue(1); fo.setToValue(0);
            fo.setOnFinished(ev -> root.getChildren().remove(overlay));
            fo.play();
        });

        VBox card = new VBox(C ? 10 : 14, topAccent, title, divider(), scroll,
                row(8, Pos.CENTER_RIGHT, excelInsideBtn, refreshBtn, closeBtn));
        card.setPadding(new Insets(0, C ? 20 : 26, C ? 16 : 20, C ? 20 : 26));
        card.setMaxWidth(C ? 620 : 740);
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #0a120a, #050505);" +
                "-fx-border-color:" + D.GREEN + "88; -fx-border-width:1;" +
                "-fx-border-radius:10; -fx-background-radius:10;" +
                "-fx-effect: dropshadow(gaussian," + D.GREEN + ",40,0.45,0,0);");

        FadeTransition fi = new FadeTransition(Duration.millis(200), card);
        fi.setFromValue(0); fi.setToValue(1);
        overlay.getChildren().add(card);
        root.getChildren().add(overlay);
        fi.play();
    }

    /** Single row in the inside-tracker overlay. */
    private HBox buildInsidePersonCard(Person p, LocalDateTime entryTime,
                                        DateTimeFormatter timeFmt, int fLabel, boolean C) {
        ImageView photo = buildPhotoView(p, C ? 52 : 64, C ? 52 : 64);

        Label rankLbl = new Label(safe(p.getMrank()));
        rankLbl.setFont(Font.font("Consolas", fLabel - 1));
        rankLbl.setTextFill(D.C_GREY);

        Label nameLbl = new Label(p.getLastName().toUpperCase() + " " + p.getFirstName());
        nameLbl.setFont(Font.font("Consolas", FontWeight.BOLD, fLabel));
        nameLbl.setTextFill(D.C_GREEN);

        VBox nameBox = new VBox(1, rankLbl, nameLbl);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        Label timeLbl = new Label(entryTime != null ? "▲  " + entryTime.format(timeFmt) : "▲  --:--");
        timeLbl.setFont(Font.font("Consolas", FontWeight.BOLD, fLabel - 1));
        timeLbl.setTextFill(D.C_GREEN);
        timeLbl.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(C ? 10 : 14, photo, nameBox, timeLbl);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(C ? 6 : 8, 10, C ? 6 : 8, 10));
        row.setStyle("-fx-background-color:#080808;" +
                "-fx-border-color:" + D.GREEN + "1a; -fx-border-width:0 0 1 0;");
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Database Backup
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isH2DataSource() {
        if (backupDataSource == null) return false;
        try (java.sql.Connection c = backupDataSource.getConnection()) {
            return c.getMetaData().getURL().startsWith("jdbc:h2");
        } catch (Exception e) { return false; }
    }

    private void performDatabaseBackup() {
        if (backupDataSource == null) {
            showError(t("Backup Error","Σφάλμα Αντιγράφου"), t("No database connection available.","Δεν υπάρχει σύνδεση με τη βάση δεδομένων."));
            return;
        }
        if (isH2DataSource()) { performH2Backup(); return; }

        boolean wasFS = mainStage.isFullScreen();
        if (wasFS) mainStage.setFullScreen(false);

        FileChooser fc = new FileChooser();
        fc.setTitle(t("Save Database Backup","Αποθήκευση Αντιγράφου Βάσης"));
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        fc.setInitialFileName("GateControl_backup_" + stamp + ".bak");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("SQL Server Backup (*.bak)", "*.bak"));
        File file = fc.showSaveDialog(mainStage);

        if (wasFS) mainStage.setFullScreen(true);
        if (file == null) return;

        final String backupPath = file.getAbsolutePath().replace("'", "''");

        backupDbBtn.setDisable(true);
        backupDbBtn.setText("⏳ ...");

        Thread t = new Thread(() -> {
            try (java.sql.Connection conn = backupDataSource.getConnection()) {

                // Discover current DB name
                String dbName;
                try (java.sql.Statement st = conn.createStatement();
                     java.sql.ResultSet rs = st.executeQuery("SELECT DB_NAME()")) {
                    rs.next();
                    dbName = rs.getString(1);
                }

                // BACKUP DATABASE — cannot use PreparedStatement for DDL
                String sql = "BACKUP DATABASE [" + dbName + "] "
                        + "TO DISK = N'" + backupPath + "' WITH FORMAT, INIT";
                try (java.sql.Statement st = conn.createStatement()) {
                    st.setQueryTimeout(600); // 10 min max
                    st.execute(sql);
                }

                Platform.runLater(() -> {
                    backupDbBtn.setDisable(false);
                    backupDbBtn.setText("💾 Backup");
                    showInfo(t("✔  Backup completed!","✔  Αντίγραφο ολοκληρώθηκε!") + "\n\n" + file.getAbsolutePath());
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    backupDbBtn.setDisable(false);
                    backupDbBtn.setText("💾 Backup");
                    showError(t("Backup Error","Σφάλμα Αντιγράφου"),
                            t("Backup failed.\n\nNote: SQL Server needs write permission\nto the selected folder\n(or choose a folder on the C:\\ drive).\n\n",
                              "Αποτυχία αντιγράφου.\n\nΣημ.: Το SQL Server χρειάζεται δικαίωμα εγγραφής\nστον επιλεγμένο φάκελο\n(ή επιλέξτε φάκελο στο C:\\).\n\n")
                            + ex.getMessage());
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void performH2Backup() {
        boolean wasFS = mainStage.isFullScreen();
        if (wasFS) mainStage.setFullScreen(false);

        FileChooser fc = new FileChooser();
        fc.setTitle(t("Save Database Backup","Αποθήκευση Αντιγράφου Βάσης"));
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        fc.setInitialFileName("GateControl_backup_" + stamp + ".zip");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("H2 Backup (*.zip)", "*.zip"));
        File file = fc.showSaveDialog(mainStage);

        if (wasFS) mainStage.setFullScreen(true);
        if (file == null) return;

        backupDbBtn.setDisable(true);
        backupDbBtn.setText("⏳ ...");

        final String backupPath = file.getAbsolutePath().replace("'", "''");

        Thread t = new Thread(() -> {
            try (java.sql.Connection conn = backupDataSource.getConnection()) {
                conn.createStatement().execute("BACKUP TO '" + backupPath + "'");
                Platform.runLater(() -> {
                    backupDbBtn.setDisable(false);
                    backupDbBtn.setText("💾 Backup");
                    showInfo(t("✔  Backup completed!","✔  Αντίγραφο ολοκληρώθηκε!") + "\n\n" + file.getAbsolutePath());
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    backupDbBtn.setDisable(false);
                    backupDbBtn.setText("💾 Backup");
                    showError(t("Backup Error","Σφάλμα Αντιγράφου"),
                            t("Backup failed.\n\n","Αποτυχία αντιγράφου.\n\n") + ex.getMessage());
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Camera capture  (admin — photo for selected person)
    // ─────────────────────────────────────────────────────────────────────────

    private void openCameraCapture(Stage owner, Consumer<byte[]> onPhotoAccepted) {
        final boolean C      = (theme == Theme.COMPACT);
        final int fBtn       = C ? D.CS_BTN   : D.FS_BTN;
        final int fLabel     = C ? D.CS_LABEL : D.FS_LABEL;

        // ── Discover and open webcam ─────────────────────────────────────────
        Webcam webcam;
        try {
            webcam = Webcam.getDefault();
            if (webcam == null) {
                showError(t("Camera","Κάμερα"), t("No camera found connected to the system.","Δεν βρέθηκε κάμερα συνδεδεμένη στο σύστημα."));
                return;
            }
            webcam.setViewSize(new Dimension(640, 480));
            webcam.open();
        } catch (Exception ex) {
            showError(t("Camera","Κάμερα"), t("Failed to open camera:\n","Αποτυχία ανοίγματος κάμερας:\n") + ex.getMessage());
            return;
        }

        final Webcam cam = webcam;
        AtomicBoolean livePreview = new AtomicBoolean(true);
        final byte[][] capturedBytes = {null};

        // ── Stage ────────────────────────────────────────────────────────────
        Stage camStage = new Stage();
        camStage.initOwner(owner);
        camStage.initModality(Modality.WINDOW_MODAL);
        applyAppIcon(camStage);
        camStage.setTitle(t("Take Photo","Λήψη Φωτογραφίας"));
        camStage.setResizable(false);

        // ── Single portrait display view (live preview AND captured frame) ────
        ImageView displayView = new ImageView();
        displayView.setFitWidth(C ? 300 : 340);
        displayView.setFitHeight(C ? 400 : 460);
        displayView.setPreserveRatio(true);
        displayView.setStyle("-fx-effect: dropshadow(gaussian," + D.GREEN + "55,10,0.25,0,0);");

        // ── Status label ─────────────────────────────────────────────────────
        Label statusLbl = new Label(t("● Live preview","● Ζωντανή προβολή"));
        statusLbl.setFont(Font.font("Consolas", FontWeight.BOLD, fLabel));
        statusLbl.setTextFill(D.C_GREEN);

        // ── Background preview loop (shared Runnable so retry can restart it) ─
        Runnable previewLoop = () -> {
            while (livePreview.get() && cam.isOpen()) {
                try {
                    BufferedImage img = cam.getImage();
                    if (img == null) { Thread.sleep(33); continue; }
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    ImageIO.write(img, "jpg", baos);
                    byte[] data = baos.toByteArray();
                    Platform.runLater(() ->
                            displayView.setImage(new Image(new ByteArrayInputStream(data))));
                    Thread.sleep(50); // ~20 fps
                } catch (InterruptedException ie) { break; }
                catch (Exception e) { break; }
            }
        };

        // ── Buttons ─────────────────────────────────────────────────────────
        Button captureBtn = accentBtn(t("📸 Capture","📸 Λήψη"),   D.GREEN, fBtn);
        Button useBtn     = accentBtn(t("✔ Use","✔ Χρήση"),         D.GREEN, fBtn);
        Button retryBtn   = accentBtn(t("🔄 Retry","🔄 Επανάληψη"), D.AMBER, fBtn);
        Button cancelBtn  = accentBtn(t("✕ Cancel","✕ Ακύρωση"),    D.GREY,  fBtn);
        setVM(useBtn, false); setVM(retryBtn, false);

        captureBtn.setOnAction(e -> {
            livePreview.set(false);
            try {
                BufferedImage img = cam.getImage();
                if (img == null) return;
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                ImageIO.write(img, "jpg", baos);
                capturedBytes[0] = baos.toByteArray();
                displayView.setImage(new Image(new ByteArrayInputStream(capturedBytes[0])));
                statusLbl.setText(t("✔ Photo captured","✔ Φωτογραφία ελήφθη"));
                statusLbl.setTextFill(D.C_AMBER);
                setVM(captureBtn, false);
                setVM(useBtn, true); setVM(retryBtn, true);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        useBtn.setOnAction(e -> {
            if (cam.isOpen()) cam.close();
            onPhotoAccepted.accept(capturedBytes[0]);
            camStage.close();
        });

        retryBtn.setOnAction(e -> {
            capturedBytes[0] = null;
            statusLbl.setText(t("● Live preview","● Ζωντανή προβολή"));
            statusLbl.setTextFill(D.C_GREEN);
            setVM(captureBtn, true);
            setVM(useBtn, false); setVM(retryBtn, false);
            livePreview.set(true);
            Thread nt = new Thread(previewLoop);
            nt.setDaemon(true);
            nt.start();
        });

        cancelBtn.setOnAction(e -> {
            livePreview.set(false);
            if (cam.isOpen()) cam.close();
            camStage.close();
        });

        camStage.setOnCloseRequest(e -> {
            livePreview.set(false);
            if (cam.isOpen()) cam.close();
        });

        // ── Layout (portrait / passport style — narrow, tall) ────────────────
        Region topAccent = new Region();
        topAccent.setMinHeight(3); topAccent.setMaxHeight(3); topAccent.setMaxWidth(Double.MAX_VALUE);
        topAccent.setStyle("-fx-background-color: linear-gradient(to right, transparent," + D.GREEN + ", transparent);");

        Label titleLbl = new Label(t("PHOTO CAPTURE","ΛΗΨΗ ΦΩΤΟΓΡΑΦΙΑΣ"));
        titleLbl.setFont(Font.font("Consolas", FontWeight.BOLD, C ? 13 : 15));
        titleLbl.setTextFill(D.C_GREEN);
        titleLbl.setEffect(new DropShadow(8, D.C_GREEN));

        VBox content = new VBox(C ? 10 : 12,
                topAccent, titleLbl, divider(),
                statusLbl, displayView, divider(),
                row(8, Pos.CENTER_LEFT, captureBtn, useBtn, retryBtn, cancelBtn));
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(0, C ? 18 : 22, C ? 14 : 18, C ? 18 : 22));
        content.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #0a0d0a, " + D.BG + ");" +
                "-fx-border-color:" + D.GREEN + "44; -fx-border-width:1;" +
                "-fx-border-radius:6; -fx-background-radius:6;");

        Scene scene = new Scene(content, C ? 380 : 420, C ? 570 : 640, Color.web(D.BG));
        camStage.setScene(scene);
        camStage.show();

        // Start live preview
        Thread previewThread = new Thread(previewLoop);
        previewThread.setDaemon(true);
        previewThread.start();
    }

    /**
     * Finds db.properties using the same priority order as DatabaseConfig:
     * 1. Explicit -Dapp.db.properties system property (set by jpackage launcher)
     * 2. Derive from code source URL — handles Spring Boot 3.x "nested:" scheme
     * 3. user.dir/app/db.properties  (jpackage install layout)
     * 4. Current working directory
     * 5. src/main/resources (IntelliJ dev layout)
     */
    private java.nio.file.Path resolveDbPropertiesPath() {
        // 1. Explicit system property injected by the jpackage launcher
        String explicit = System.getProperty("app.db.properties");
        if (explicit != null) {
            java.nio.file.Path p = java.nio.file.Paths.get(explicit);
            if (p.toFile().exists()) return p;
        }

        // 2. Derive from code source URL.
        //    Spring Boot 3.x uses "nested:/path/app.jar!/BOOT-INF/classes/" —
        //    strip scheme + inner path to recover the outer JAR path.
        try {
            String loc = getClass().getProtectionDomain().getCodeSource().getLocation().toString();
            if (loc.contains("!/")) {
                loc = loc.replaceFirst("^(jar:|nested:)", "").split("!")[0];
                if (!loc.startsWith("file:")) loc = "file:" + loc;
            }
            java.nio.file.Path p = java.nio.file.Paths.get(new java.net.URI(loc));
            java.nio.file.Path candidate = (p.toFile().isDirectory() ? p : p.getParent()).resolve("db.properties");
            if (candidate.toFile().exists()) return candidate;
        } catch (Exception ignored) {}

        // 3. jpackage layout: user.dir = install root, db.properties is in app/
        java.nio.file.Path appDir = java.nio.file.Paths.get(
                System.getProperty("user.dir"), "app", "db.properties");
        if (appDir.toFile().exists()) return appDir;

        // 4. Current working directory
        java.nio.file.Path cwd = java.nio.file.Paths.get("db.properties");
        if (cwd.toFile().exists()) return cwd;

        // 5. IntelliJ dev layout
        java.nio.file.Path dev = java.nio.file.Paths.get("src", "main", "resources", "db.properties");
        if (dev.toFile().exists()) return dev;

        return null;
    }


}