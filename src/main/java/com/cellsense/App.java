package com.cellsense;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.Styles;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
// ^ If you don't have icons yet, we'll use text buttons for now to avoid errors

import java.awt.SystemTray;
import java.awt.TrayIcon;

public class App extends Application {

    // Remove or comment out: private BatteryRing batteryRing;
    private BatteryLiquid batteryLiquidRef;
    private ToggleSwitch alarmSwitch;
    private Label alarmStatusLabel;
    private Stage primaryStage;
    private Label timeRemainingLabel;
    private Label powerModeLabel;
    private boolean maxAlarmTriggered = false;
    private boolean minAlarmTriggered = false;
    private BorderPane mainLayout;
    private VBox dashboardView;
    private VBox analyticsView;
    private VBox historyView;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        try {
            Image icon = new Image(getClass().getResourceAsStream("/icon.png"));
            stage.getIcons().add(icon);
        } catch (Exception e) { }

        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        setupSystemTray();

        mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #0d1117;");

        // Initialize Dashboard View immediately
        dashboardView = createDashboard();

        VBox sidebar = createSidebar();
        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(dashboardView);

        animateStartup();
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> checkBattery()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        Scene scene = new Scene(mainLayout, 1000, 700);
        stage.setTitle("CellSense");
        stage.setScene(scene);
        stage.show();
    }

    // --- SIDEBAR (Same as before) ---
    private VBox createSidebar() {
        VBox sidebar = new VBox(20);
        sidebar.setPadding(new Insets(30, 20, 30, 20));
        sidebar.setPrefWidth(240);
        sidebar.setStyle("-fx-background-color: #010409; -fx-border-color: #30363d; -fx-border-width: 0 1 0 0;");

        Text cellText = new Text("Cell");
        cellText.setStyle("-fx-fill: #2f81f7; -fx-font-weight: bold; -fx-font-size: 26px;");
        Text senseText = new Text("Sense");
        senseText.setStyle("-fx-fill: white; -fx-font-weight: bold; -fx-font-size: 26px;");
        TextFlow logo = new TextFlow(cellText, senseText);

        // NAVIGATION BUTTONS
        Button dashBtn = createNavButton("Dashboard", true);
        Button analyticsBtn = createNavButton("Analytics", false);
        Button historyBtn = createNavButton("History", false);

        // ACTION: Switch Views
        dashBtn.setOnAction(e -> {
            updateNavStyles(dashBtn, analyticsBtn, historyBtn);
            mainLayout.setCenter(dashboardView);
        });

        analyticsBtn.setOnAction(e -> {
            updateNavStyles(analyticsBtn, dashBtn, historyBtn);
            if (analyticsView == null) {
                analyticsView = AnalyticsView.create();
            }
            mainLayout.setCenter(analyticsView);
        });

        historyBtn.setOnAction(e -> {
            updateNavStyles(historyBtn, dashBtn, analyticsBtn);
            if (historyView == null) {
                historyView = HistoryView.create();
            }
            mainLayout.setCenter(historyView);
        });
        // --- MISSING DEFINITIONS FIXED HERE ---

        // 1. Spacer (Pushes exit button to bottom)
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // 2. Exit Button
        Button exitBtn = new Button("Quit App");
        exitBtn.setMaxWidth(Double.MAX_VALUE);
        exitBtn.getStyleClass().add(Styles.DANGER);
        exitBtn.setOnAction(e -> {
            if (SystemTray.isSupported()) {
                // Remove icon to prevent it getting stuck in taskbar
                TrayIcon[] icons = SystemTray.getSystemTray().getTrayIcons();
                if (icons.length > 0) {
                    SystemTray.getSystemTray().remove(icons[0]);
                }
            }
            Platform.exit();
            System.exit(0);
        });

        // 3. Version Label
        Label version = new Label("v0.2.0 Beta");
        version.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");
        version.setAlignment(Pos.CENTER);
        version.setMaxWidth(Double.MAX_VALUE);

        // Add everything to sidebar
        sidebar.getChildren().addAll(logo, new Separator(), dashBtn, analyticsBtn, historyBtn, spacer, exitBtn, version);
        return sidebar;
    }

    // Helper to toggle the "Active Blue" style on buttons
    private void updateNavStyles(Button active, Button... others) {
        active.setStyle("-fx-background-color: #1f6feb; -fx-text-fill: white; -fx-font-weight: bold;");
        for (Button b : others) {
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: #8b949e;");
        }
    }

    private Button createNavButton(String text, boolean isActive) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.getStyleClass().add(Styles.FLAT);
        btn.setStyle(isActive ? "-fx-background-color: #1f6feb; -fx-text-fill: white; -fx-font-weight: bold;" : "-fx-text-fill: #c9d1d9;");
        return btn;
    }

    // --- DASHBOARD ---
    private VBox createDashboard() {
        Label headerTitle = new Label("Battery Overview");
        headerTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: white;");

        GridPane grid = new GridPane();
        grid.setHgap(20); // Consistent gap
        grid.setVgap(20);
        grid.setPadding(new Insets(10, 0, 0, 0));

        // --- 1. SET UP COLUMNS (50% / 50%) ---
        // This forces both cards to take equal width, filling the space perfectly
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        // --- 2. THE LIQUID CARD ---
        // We use the new Liquid Component
        BatteryLiquid batteryLiquid = new BatteryLiquid();
        // Save reference to update it later
        this.batteryLiquidRef = batteryLiquid;

        VBox liquidCard = new VBox(batteryLiquid);
        liquidCard.setAlignment(Pos.CENTER);
        liquidCard.setPadding(new Insets(20));
        styleCard(liquidCard);
        // Make it fill the grid cell height
        GridPane.setVgrow(liquidCard, Priority.ALWAYS);

        // --- 3. SETTINGS CARD ---
        VBox settingsCard = createSettingsCard();
        GridPane.setVgrow(settingsCard, Priority.ALWAYS); // Match height

        // --- 4. INFO CARDS ---
        VBox timeCard = createInfoCard("Time Remaining", "Calculating...", "Est. time until empty");
        timeRemainingLabel = (Label) timeCard.getChildren().get(1);

        VBox powerCard = createInfoCard("Power Status", "Battery Power", "Current Source");
        powerModeLabel = (Label) powerCard.getChildren().get(1);

        // --- 5. ADD TO GRID ---
        grid.add(liquidCard, 0, 0);   // Top Left
        grid.add(settingsCard, 1, 0); // Top Right
        grid.add(timeCard, 0, 1);     // Bottom Left
        grid.add(powerCard, 1, 1);    // Bottom Right

        alarmStatusLabel = new Label("");
        alarmStatusLabel.setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold; -fx-font-size: 16px;");
        HBox statusBar = new HBox(alarmStatusLabel);
        statusBar.setAlignment(Pos.CENTER);
        statusBar.setPadding(new Insets(20));

        VBox content = new VBox(25, headerTitle, grid, statusBar);
        content.setPadding(new Insets(40));
        return content;
    }

    // --- NEW SETTINGS CARD WITH TONE SELECTORS ---
    private VBox createSettingsCard() {
        // Master Toggle
        alarmSwitch = new ToggleSwitch("Smart Alarms");
        alarmSwitch.setSelected(PreferenceManager.isAlarmActive());
        alarmSwitch.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        alarmSwitch.selectedProperty().addListener((obs, old, val) -> {
            PreferenceManager.setAlarmActive(val);
            if (!val) SoundManager.stopAlarm();
        });

        // --- UPPER LIMIT SECTION ---
        Label maxTitle = new Label("Stop Charging at");
        maxTitle.setStyle("-fx-text-fill: #8b949e; -fx-font-weight: bold;");
        Label maxVal = new Label((int)PreferenceManager.getMaxLimit() + "%");
        maxVal.setStyle("-fx-text-fill: #2f81f7; -fx-font-size: 22px; -fx-font-weight: 900;");

        Slider maxSlider = new Slider(50, 100, PreferenceManager.getMaxLimit());
        maxSlider.valueProperty().addListener((obs, old, val) -> {
            maxVal.setText(val.intValue() + "%");
            PreferenceManager.setMaxLimit(val.doubleValue());
        });

        // Upper Tone Selector
        HBox maxToneBox = createToneSelector(true); // true = Upper

        // --- LOWER LIMIT SECTION ---
        Label minTitle = new Label("Warn me at");
        minTitle.setStyle("-fx-text-fill: #8b949e; -fx-font-weight: bold;");
        Label minVal = new Label((int)PreferenceManager.getMinLimit() + "%");
        minVal.setStyle("-fx-text-fill: #2f81f7; -fx-font-size: 22px; -fx-font-weight: 900;");

        Slider minSlider = new Slider(5, 50, PreferenceManager.getMinLimit());
        minSlider.valueProperty().addListener((obs, old, val) -> {
            minVal.setText(val.intValue() + "%");
            PreferenceManager.setMinLimit(val.doubleValue());
        });

        // Lower Tone Selector
        HBox minToneBox = createToneSelector(false); // false = Lower

        VBox card = new VBox(15,
                new Label("Configuration"),
                new Separator(),
                alarmSwitch,
                new VBox(5, new HBox(10, maxTitle, maxVal), maxSlider, maxToneBox),
                new VBox(5, new HBox(10, minTitle, minVal), minSlider, minToneBox)
        );
        card.getChildren().get(0).setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        card.setPadding(new Insets(25));
        styleCard(card);
        return card;
    }

    // Helper to create the Tone Selector + Loop Checkbox
    private HBox createToneSelector(boolean isUpper) {
        // 1. Dropdown
        ComboBox<String> toneBox = new ComboBox<>();
        toneBox.getItems().addAll(SoundManager.getSoundNames());
        String savedTone = isUpper ? PreferenceManager.getMaxSound() : PreferenceManager.getMinSound();
        toneBox.setValue(savedTone);
        toneBox.setOnAction(e -> {
            if (isUpper) PreferenceManager.setMaxSound(toneBox.getValue());
            else PreferenceManager.setMinSound(toneBox.getValue());
        });
        toneBox.setPrefWidth(140);

        // 2. Play Button
        Button playBtn = new Button("▶");
        playBtn.getStyleClass().add(Styles.BUTTON_OUTLINED);
        playBtn.setOnAction(e -> SoundManager.previewSound(toneBox.getValue()));

        // 3. Loop Checkbox
        CheckBox loopBox = new CheckBox("Loop");
        loopBox.setStyle("-fx-text-fill: white;");
        boolean savedLoop = isUpper ? PreferenceManager.isMaxLoop() : PreferenceManager.isMinLoop();
        loopBox.setSelected(savedLoop);

        loopBox.selectedProperty().addListener((obs, old, val) -> {
            if (isUpper) PreferenceManager.setMaxLoop(val);
            else PreferenceManager.setMinLoop(val);
        });

        HBox box = new HBox(10, toneBox, playBtn, loopBox);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private VBox createInfoCard(String title, String value, String subtext) {
        Label titleLbl = new Label(title); titleLbl.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 14px; -fx-font-weight: bold;");
        Label valueLbl = new Label(value); valueLbl.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        Label subLbl = new Label(subtext); subLbl.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");
        VBox card = new VBox(10, titleLbl, valueLbl, subLbl);
        card.setPadding(new Insets(20));
        styleCard(card);
        GridPane.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private void styleCard(VBox box) {
        box.setStyle("-fx-background-color: #161b22; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");
    }

    // --- LOGIC ---
    private void setupSystemTray() {
        Platform.setImplicitExit(false);
        primaryStage.setOnCloseRequest(event -> { event.consume(); primaryStage.hide(); });
        if (SystemTray.isSupported()) {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                java.awt.Image image = java.awt.Toolkit.getDefaultToolkit().createImage(getClass().getResource("/icon.png"));
                TrayIcon trayIcon = new TrayIcon(image, "CellSense");
                trayIcon.setImageAutoSize(true);
                trayIcon.addActionListener(e -> Platform.runLater(() -> primaryStage.show()));

                java.awt.PopupMenu popup = new java.awt.PopupMenu();
                java.awt.MenuItem showItem = new java.awt.MenuItem("Show Dashboard");
                showItem.addActionListener(e -> Platform.runLater(() -> primaryStage.show()));
                java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit Completely");
                exitItem.addActionListener(e -> { tray.remove(trayIcon); Platform.exit(); System.exit(0); });
                popup.add(showItem); popup.add(exitItem);
                trayIcon.setPopupMenu(popup); tray.add(trayIcon);
            } catch (Exception e) {}
        }
    }

    private void animateStartup() {
        batteryLiquidRef.setProgress(0, false);
        new Timeline(new KeyFrame(Duration.millis(500), e -> checkBattery())).play();
    }

    private void checkBattery() {
        int level = BatteryManager.getBatteryLevel();
        boolean isPlugged = BatteryManager.isPluggedIn();
        if (batteryLiquidRef != null) {
            batteryLiquidRef.setProgress(level, isPlugged);
        }
        // Update Info Cards...
        if (isPlugged) {
            powerModeLabel.setText("AC Power"); powerModeLabel.setStyle("-fx-text-fill: #2ea043; -fx-font-size: 24px; -fx-font-weight: bold;");
            timeRemainingLabel.setText("Charging...");
        } else {
            powerModeLabel.setText("Battery"); powerModeLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 24px; -fx-font-weight: bold;");
            timeRemainingLabel.setText("~" + (level * 2) + " mins");
        }

        if (!alarmSwitch.isSelected()) {
            alarmStatusLabel.setText("");
            SoundManager.stopAlarm();
            return;
        }

        double maxLimit = PreferenceManager.getMaxLimit();
        double minLimit = PreferenceManager.getMinLimit();

        // SCENARIO 1: Upper Limit Reached
        if (isPlugged && level >= maxLimit) {
            alarmStatusLabel.setText("⚠ UNPLUG CHARGER NOW");

            // Only play if we haven't triggered it for this session yet
            if (!maxAlarmTriggered) {
                SoundManager.playAlarm(PreferenceManager.getMaxSound(), PreferenceManager.isMaxLoop());

                // If looping is OFF, mark as triggered so it doesn't ring again in 2 seconds
                if (!PreferenceManager.isMaxLoop()) {
                    maxAlarmTriggered = true;
                }
            }
        }
        // SCENARIO 2: Lower Limit Reached
        else if (!isPlugged && level <= minLimit) {
            alarmStatusLabel.setText("⚠ LOW BATTERY - PLUG IN");

            if (!minAlarmTriggered) {
                SoundManager.playAlarm(PreferenceManager.getMinSound(), PreferenceManager.isMinLoop());

                if (!PreferenceManager.isMinLoop()) {
                    minAlarmTriggered = true;
                }
            }
        }
        // SCENARIO 3: Normal Range (Reset Triggers)
        else {
            alarmStatusLabel.setText("");
            SoundManager.stopAlarm();

            // Reset the triggers so they can ring again next time the condition is met
            maxAlarmTriggered = false;
            minAlarmTriggered = false;
        }
    }
    public static void main(String[] args) {
        launch();
    }
}