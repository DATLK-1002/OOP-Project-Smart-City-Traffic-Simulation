package com.smartcity.gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.util.*;

import com.smartcity.traffic.road.Junction;
import com.smartcity.traffic.road.Lane;
import com.smartcity.traffic.road.Location;
import com.smartcity.traffic.road.MapManager;
import com.smartcity.traffic.road.Road;
import com.smartcity.traffic.road.VehicleSpawner;

public class MainWindow extends Application {

    private MapManager mapManager;
    private MapRenderer mapRenderer;
    private SoundManager soundManager;
    private AnimationEngine animationEngine;

    private List<MockVehicle> activeVehicles;
    private int vehicleIdCounter = 1;
    private Random random = new Random();

    // Canvas panning and zooming
    private Canvas canvas;
    private double zoom = 0.9;
    private double panX = 60.0;
    private double panY = 40.0;
    private double lastMouseX;
    private double lastMouseY;

    // Simulation states
    private boolean running = false;
    private double simulationTime = 0.0;

    // UI elements
    private Label activeVehiclesLabel;
    private Label timeLabel;
    private ProgressBar congestionProgress;
    private Label congestionValueLabel;
    
    // Type counts labels
    private Label carCountLabel;
    private Label motoCountLabel;
    private Label bikeCountLabel;
    private Label ambCountLabel;
    private Label fireCountLabel;

    private ListView<String> logList;
    private boolean spawnEnabled = true;
    private double currentSpawnRate = 0.8; // default rate

    @Override
    public void start(Stage stage) {
        // 1. Initialize components
        mapManager = MapManager.getInstance();
        mapRenderer = new MapRenderer();
        soundManager = new SoundManager();
        activeVehicles = new ArrayList<>();

        initializeRoadNetwork();

        // 2. Build the Layout (Premium Dark UI)
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1e272e;");

        // Set Center Canvas Panel
        AnchorPane canvasContainer = new AnchorPane();
        canvasContainer.setStyle("-fx-background-color: #111;");
        canvas = new Canvas(920, 750);
        canvasContainer.getChildren().add(canvas);
        root.setCenter(canvasContainer);

        // Bind canvas resizing
        canvasContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setWidth(newVal.doubleValue());
            draw();
        });
        canvasContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            canvas.setHeight(newVal.doubleValue());
            draw();
        });

        // Setup mouse drag-to-pan and scroll-to-zoom
        setupInteractiveCanvas();

        // Set Left Panel (Controls Panel)
        VBox leftPanel = buildLeftControlPanel();
        root.setLeft(leftPanel);

        // Set Right Panel (Logs & Stats)
        VBox rightPanel = buildRightStatsPanel();
        root.setRight(rightPanel);

        // Add float HUD overlay inside canvas container
        VBox hudOverlay = buildHudOverlay();
        canvasContainer.getChildren().add(hudOverlay);
        AnchorPane.setTopAnchor(hudOverlay, 15.0);
        AnchorPane.setRightAnchor(hudOverlay, 15.0);

        // Create animation loop
        animationEngine = new AnimationEngine(this::onTick);
        animationEngine.setSpeedMultiplier(1.0);

        // First initial draw
        draw();

        Scene scene = new Scene(root, 1440, 850);
        stage.setTitle("Smart City Traffic Simulation Dashboard");
        stage.setScene(scene);

        // Stop sounds on window close
        stage.setOnCloseRequest(e -> {
            animationEngine.stop();
            soundManager.stopAllSounds();
        });

        stage.show();
    }

    private void initializeRoadNetwork() {
        mapManager.clear();
        mapManager.setMapWidth(1600);
        mapManager.setMapHeight(900);

        // Center junction J1 at (600, 400)
        Location j1Loc = new Location(600, 400);
        Junction j1 = new Junction("J1", Junction.JunctionType.FOUR_WAY, j1Loc);
        mapManager.addJunction(j1);

        // 4 Roads meeting at J1
        // roadEast: Left edge (0, 400) to junction (555, 400)
        Road roadEast = new Road("R_EAST", "Đại lộ Đông", 555.0, 80.0, new Location(0, 400), new Location(555, 400));
        Lane laneEastIn = new Lane("L_EAST_IN", Lane.Direction.EAST, 60.0, 40.0, 555.0);
        Lane laneEastOut = new Lane("L_EAST_OUT", Lane.Direction.WEST, 60.0, 40.0, 555.0);
        roadEast.addLane(laneEastIn);
        roadEast.addLane(laneEastOut);

        // roadWest: Right edge (1200, 400) to junction (645, 400)
        Road roadWest = new Road("R_WEST", "Đại lộ Tây", 555.0, 80.0, new Location(1200, 400), new Location(645, 400));
        Lane laneWestIn = new Lane("L_WEST_IN", Lane.Direction.WEST, 60.0, 40.0, 555.0);
        Lane laneWestOut = new Lane("L_WEST_OUT", Lane.Direction.EAST, 60.0, 40.0, 555.0);
        roadWest.addLane(laneWestIn);
        roadWest.addLane(laneWestOut);

        // roadNorth: Top edge (600, 0) to junction (600, 355)
        Road roadNorth = new Road("R_NORTH", "Đường Bắc", 355.0, 80.0, new Location(600, 0), new Location(600, 355));
        Lane laneNorthIn = new Lane("L_NORTH_IN", Lane.Direction.SOUTH, 50.0, 40.0, 355.0);
        Lane laneNorthOut = new Lane("L_NORTH_OUT", Lane.Direction.NORTH, 50.0, 40.0, 355.0);
        roadNorth.addLane(laneNorthIn);
        roadNorth.addLane(laneNorthOut);

        // roadSouth: Bottom edge (600, 800) to junction (600, 445)
        Road roadSouth = new Road("R_SOUTH", "Đường Nam", 355.0, 80.0, new Location(600, 800), new Location(600, 445));
        Lane laneSouthIn = new Lane("L_SOUTH_IN", Lane.Direction.NORTH, 50.0, 40.0, 355.0);
        Lane laneSouthOut = new Lane("L_SOUTH_OUT", Lane.Direction.SOUTH, 50.0, 40.0, 355.0);
        roadSouth.addLane(laneSouthIn);
        roadSouth.addLane(laneSouthOut);

        // Connect roads to J1
        j1.addConnectedRoad(roadEast);
        j1.addConnectedRoad(roadWest);
        j1.addConnectedRoad(roadNorth);
        j1.addConnectedRoad(roadSouth);

        mapManager.addRoad(roadEast);
        mapManager.addRoad(roadWest);
        mapManager.addRoad(roadNorth);
        mapManager.addRoad(roadSouth);

        // Setup spawners at entrance lanes
        VehicleSpawner sp1 = new VehicleSpawner("SP_EAST", laneEastIn, currentSpawnRate);
        VehicleSpawner sp2 = new VehicleSpawner("SP_WEST", laneWestIn, currentSpawnRate);
        VehicleSpawner sp3 = new VehicleSpawner("SP_NORTH", laneNorthIn, currentSpawnRate * 0.7);
        VehicleSpawner sp4 = new VehicleSpawner("SP_SOUTH", laneSouthIn, currentSpawnRate * 0.7);

        String[] types = {"Car", "Motorbike", "Bicycle", "Ambulance", "FireTruck"};
        for (String t : types) {
            sp1.addVehicleType(t);
            sp2.addVehicleType(t);
            sp3.addVehicleType(t);
            sp4.addVehicleType(t);
        }

        sp1.startSpawning();
        sp2.startSpawning();
        sp3.startSpawning();
        sp4.startSpawning();

        mapManager.addSpawner(sp1);
        mapManager.addSpawner(sp2);
        mapManager.addSpawner(sp3);
        mapManager.addSpawner(sp4);
    }

    private void setupInteractiveCanvas() {
        canvas.setOnMousePressed(e -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
        });

        canvas.setOnMouseDragged(e -> {
            double dx = e.getX() - lastMouseX;
            double dy = e.getY() - lastMouseY;
            panX += dx;
            panY += dy;
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            draw();
        });

        canvas.setOnScroll(e -> {
            double factor = e.getDeltaY() > 0 ? 1.05 : 0.95;
            zoom = Math.max(0.2, Math.min(zoom * factor, 3.0));
            mapRenderer.setZoomFactor(zoom);
            draw();
        });
    }

    private VBox buildLeftControlPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        panel.setPrefWidth(260);
        panel.setStyle("-fx-background-color: #2c3e50; -fx-border-color: #34495e; -fx-border-width: 0 1 0 0;");

        // App Logo
        Label appTitle = new Label("SMART TRAFFIC");
        appTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        appTitle.setTextFill(Color.web("#1abc9c"));

        Label appSub = new Label("GUI & Simulation Console");
        appSub.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        appSub.setTextFill(Color.web("#95a5a6"));
        appSub.setPadding(new Insets(-15, 0, 15, 0));

        // 1. Simulation controls card
        VBox simControls = new VBox(12);
        simControls.setPadding(new Insets(15));
        simControls.setStyle("-fx-background-color: #34495e; -fx-background-radius: 8;");

        Label simHeader = new Label("BỘ ĐIỀU KHIỂN");
        simHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        simHeader.setTextFill(Color.WHITE);

        HBox btnBox = new HBox(8);
        Button playBtn = new Button("Chạy");
        playBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        playBtn.setPrefWidth(70);

        Button pauseBtn = new Button("Tạm dừng");
        pauseBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        pauseBtn.setPrefWidth(85);

        Button resetBtn = new Button("Đặt lại");
        resetBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        resetBtn.setPrefWidth(70);

        btnBox.getChildren().addAll(playBtn, pauseBtn, resetBtn);

        // Actions
        playBtn.setOnAction(e -> {
            running = true;
            animationEngine.start();
            addEventLog("Simulation resumed.");
        });

        pauseBtn.setOnAction(e -> {
            running = false;
            animationEngine.stop();
            soundManager.stopAllSounds();
            addEventLog("Simulation paused.");
        });

        resetBtn.setOnAction(e -> {
            running = false;
            animationEngine.stop();
            soundManager.stopAllSounds();
            simulationTime = 0.0;
            activeVehicles.clear();
            // Clear vehicle references in lanes
            for (Road r : mapManager.getRoads()) {
                for (Lane l : r.getLanes()) {
                    l.getVehicles().clear();
                }
            }
            initializeRoadNetwork();
            vehicleIdCounter = 1;
            logList.getItems().clear();
            addEventLog("Simulation reset completed.");
            draw();
            updateDashboardUI();
        });

        // Speed Multiplier slider
        Label speedLabel = new Label("Tốc độ mô phỏng: 1.0x");
        speedLabel.setTextFill(Color.web("#bdc3c7"));
        speedLabel.setFont(Font.font(11));

        Slider speedSlider = new Slider(0.5, 4.0, 1.0);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.setMajorTickUnit(1.0);
        speedSlider.setBlockIncrement(0.5);
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double val = Math.round(newVal.doubleValue() * 10.0) / 10.0;
            speedLabel.setText(String.format("Tốc độ mô phỏng: %.1fx", val));
            animationEngine.setSpeedMultiplier(val);
        });

        simControls.getChildren().addAll(simHeader, btnBox, speedLabel, speedSlider);

        // 2. Spawners control card
        VBox spawnControls = new VBox(12);
        spawnControls.setPadding(new Insets(15));
        spawnControls.setStyle("-fx-background-color: #34495e; -fx-background-radius: 8;");

        Label spawnHeader = new Label("ĐIỂM SINH XE (SPAWNERS)");
        spawnHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        spawnHeader.setTextFill(Color.WHITE);

        CheckBox spawnToggle = new CheckBox("Kích hoạt sinh xe tự động");
        spawnToggle.setSelected(true);
        spawnToggle.setTextFill(Color.WHITE);
        spawnToggle.setOnAction(e -> {
            spawnEnabled = spawnToggle.isSelected();
            for (VehicleSpawner sp : mapManager.getSpawners()) {
                if (spawnEnabled) sp.startSpawning();
                else sp.stopSpawning();
            }
            addEventLog("Vehicle spawning " + (spawnEnabled ? "enabled" : "disabled"));
        });

        Label spawnRateLabel = new Label("Tần suất sinh: 0.8 xe/s");
        spawnRateLabel.setTextFill(Color.web("#bdc3c7"));
        spawnRateLabel.setFont(Font.font(11));

        Slider spawnRateSlider = new Slider(0.2, 3.0, 0.8);
        spawnRateSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double val = Math.round(newVal.doubleValue() * 10.0) / 10.0;
            spawnRateLabel.setText(String.format("Tần suất sinh: %.1f xe/s", val));
            currentSpawnRate = val;
            for (VehicleSpawner sp : mapManager.getSpawners()) {
                sp.setSpawnRate(val);
            }
        });

        spawnControls.getChildren().addAll(spawnHeader, spawnToggle, spawnRateLabel, spawnRateSlider);

        // 3. Audio manager card
        VBox audioControls = new VBox(12);
        audioControls.setPadding(new Insets(15));
        audioControls.setStyle("-fx-background-color: #34495e; -fx-background-radius: 8;");

        Label audioHeader = new Label("ÂM THANH & CẢNH BÁO");
        audioHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        audioHeader.setTextFill(Color.WHITE);

        CheckBox soundToggle = new CheckBox("Kích hoạt âm thanh");
        soundToggle.setSelected(true);
        soundToggle.setTextFill(Color.WHITE);
        soundToggle.setOnAction(e -> {
            soundManager.setSoundEnabled(soundToggle.isSelected());
            addEventLog("Audio effects " + (soundToggle.isSelected() ? "unmuted" : "muted"));
        });

        Button honkBtn = new Button("Bấm còi xe ngẫu nhiên 🔊");
        honkBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        honkBtn.setPrefWidth(210);
        honkBtn.setOnAction(e -> {
            soundManager.playHorn();
            addEventLog("Manual horn triggered.");
        });

        audioControls.getChildren().addAll(audioHeader, soundToggle, honkBtn);

        panel.getChildren().addAll(appTitle, appSub, simControls, spawnControls, audioControls);
        return panel;
    }

    private VBox buildRightStatsPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.setPrefWidth(260);
        panel.setStyle("-fx-background-color: #2c3e50; -fx-border-color: #34495e; -fx-border-width: 0 0 0 1;");

        Label statsTitle = new Label("THÔNG SỐ MÔ PHỎNG");
        statsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        statsTitle.setTextFill(Color.WHITE);

        // Stats Box
        VBox statsGrid = new VBox(10);
        statsGrid.setPadding(new Insets(12));
        statsGrid.setStyle("-fx-background-color: #34495e; -fx-background-radius: 8;");

        activeVehiclesLabel = new Label("0 Phương tiện");
        activeVehiclesLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        activeVehiclesLabel.setTextFill(Color.web("#eec111"));

        timeLabel = new Label("Thời gian: 0.0s");
        timeLabel.setFont(Font.font("System", 13));
        timeLabel.setTextFill(Color.WHITE);

        Label conLabel = new Label("Mức độ tắc nghẽn trung bình:");
        conLabel.setTextFill(Color.web("#bdc3c7"));
        conLabel.setFont(Font.font(11));

        congestionProgress = new ProgressBar(0.0);
        congestionProgress.setPrefWidth(200);
        congestionProgress.setStyle("-fx-accent: #2ecc71;");

        congestionValueLabel = new Label("0.00%");
        congestionValueLabel.setTextFill(Color.WHITE);
        congestionValueLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        statsGrid.getChildren().addAll(activeVehiclesLabel, timeLabel, new Separator(), conLabel, congestionProgress, congestionValueLabel);

        // Distribution list
        VBox distBox = new VBox(8);
        distBox.setPadding(new Insets(12));
        distBox.setStyle("-fx-background-color: #34495e; -fx-background-radius: 8;");

        Label distTitle = new Label("PHÂN BỔ LOẠI XE");
        distTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        distTitle.setTextFill(Color.WHITE);

        carCountLabel = new Label("• Ô tô (Car): 0");
        carCountLabel.setTextFill(Color.web("#bdc3c7"));
        motoCountLabel = new Label("• Xe máy (Motorbike): 0");
        motoCountLabel.setTextFill(Color.web("#bdc3c7"));
        bikeCountLabel = new Label("• Xe đạp (Bicycle): 0");
        bikeCountLabel.setTextFill(Color.web("#bdc3c7"));
        ambCountLabel = new Label("• Cấp cứu (Ambulance): 0");
        ambCountLabel.setTextFill(Color.web("#bdc3c7"));
        fireCountLabel = new Label("• Cứu hỏa (FireTruck): 0");
        fireCountLabel.setTextFill(Color.web("#bdc3c7"));

        distBox.getChildren().addAll(distTitle, carCountLabel, motoCountLabel, bikeCountLabel, ambCountLabel, fireCountLabel);

        // Events Log Panel
        VBox logsBox = new VBox(8);
        VBox.setVgrow(logsBox, Priority.ALWAYS);

        Label logsTitle = new Label("NHẬT KÝ HỆ THỐNG");
        logsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        logsTitle.setTextFill(Color.WHITE);

        logList = new ListView<>();
        logList.setStyle("-fx-control-inner-background: #1e272e; -fx-text-fill: #2ecc71; -fx-font-family: 'Consolas'; -fx-font-size: 11px;");
        VBox.setVgrow(logList, Priority.ALWAYS);

        logsBox.getChildren().addAll(logsTitle, logList);

        panel.getChildren().addAll(statsTitle, statsGrid, distBox, logsBox);
        return panel;
    }

    private VBox buildHudOverlay() {
        VBox hud = new VBox(6);
        hud.setPadding(new Insets(10, 15, 10, 15));
        hud.setStyle("-fx-background-color: rgba(44, 62, 80, 0.85); -fx-background-radius: 6; -fx-border-color: rgba(52, 73, 94, 0.9); -fx-border-width: 1;");
        hud.setMouseTransparent(true); // clicks pass through to canvas

        Label l1 = new Label("Camera: Kéo để di chuyển | Cuộn để phóng to");
        l1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        l1.setTextFill(Color.LIGHTGRAY);

        Label l2 = new Label("Chu kỳ đèn giao thông: 12 giây");
        l2.setFont(Font.font("Segoe UI", 10));
        l2.setTextFill(Color.web("#1abc9c"));

        hud.getChildren().addAll(l1, l2);
        return hud;
    }

    private void addEventLog(String msg) {
        String timeStr = String.format("[%05.1fs] ", simulationTime);
        logList.getItems().add(0, timeStr + msg);
        if (logList.getItems().size() > 50) {
            logList.getItems().remove(logList.getItems().size() - 1);
        }
    }

    private void onTick(double deltaTime) {
        if (!running) return;

        simulationTime += deltaTime;

        // 1. Spawning
        for (VehicleSpawner sp : mapManager.getSpawners()) {
            if (sp.isActive() && sp.update(deltaTime)) {
                String type = sp.spawnVehicle();
                if (type != null) {
                    String id = "V-" + (vehicleIdCounter++);
                    MockVehicle vehicle = new MockVehicle(id, type, sp.getSpawnLane());
                    activeVehicles.add(vehicle);
                    sp.getSpawnLane().addVehicle(vehicle);

                    addEventLog("Xe mới: " + type + " [" + id + "] vào làn " + sp.getSpawnLane().getId());

                    // Sound alert
                    if ("Ambulance".equals(type) || "FireTruck".equals(type)) {
                        soundManager.playSiren();
                    } else if (Math.random() < 0.15) {
                        soundManager.playHorn();
                    }
                }
            }
        }

        // 2. Physics & movement
        List<MockVehicle> toRemove = new ArrayList<>();
        for (int i = 0; i < activeVehicles.size(); i++) {
            MockVehicle v = activeVehicles.get(i);

            // Compute distance to vehicle ahead on the same lane
            double distanceToAhead = -1.0;
            MockVehicle ahead = null;
            for (Object obj : v.getLane().getVehicles()) {
                if (obj instanceof MockVehicle other) {
                    if (other != v && other.getPositionOnLane() > v.getPositionOnLane()) {
                        double dist = other.getPositionOnLane() - v.getPositionOnLane();
                        if (ahead == null || dist < (ahead.getPositionOnLane() - v.getPositionOnLane())) {
                            ahead = other;
                            distanceToAhead = dist;
                        }
                    }
                }
            }

            // Junction traffic light state
            Junction destJunction = getDestJunction(v.getLane());
            boolean isLightRedOrYellow = false;
            if (destJunction != null) {
                isLightRedOrYellow = MapRenderer.isLightRedOrYellowForLane(v.getLane(), destJunction);
            }

            v.update(deltaTime, distanceToAhead, isLightRedOrYellow);

            // Check if finished lane
            if (v.isFinished()) {
                // Route vehicle through junction J1
                if (destJunction != null) {
                    Road currentRoad = getRoadForLane(v.getLane());
                    List<Road> outgoingRoads = new ArrayList<>();
                    for (Road r : destJunction.getConnectedRoads()) {
                        if (r == currentRoad) continue;
                        // Outgoing roads: either end or start is near the junction
                        boolean startNearJunction = r.getStartLocation().distanceTo(destJunction.getLocation()) < 50.0;
                        boolean endNearJunction = r.getEndLocation().distanceTo(destJunction.getLocation()) < 50.0;
                        if (startNearJunction || endNearJunction) {
                            outgoingRoads.add(r);
                        }
                    }

                    if (!outgoingRoads.isEmpty()) {
                        Road nextRoad = outgoingRoads.get(random.nextInt(outgoingRoads.size()));
                        if (nextRoad.getLaneCount() > 0) {
                            // Always pick index 0 (the primary incoming lane of the next road)
                            Lane nextLane = nextRoad.getLaneByIndex(0);

                            v.getLane().removeVehicle(v);
                            v.setLane(nextLane);
                            v.setPositionOnLane(0.0);
                            nextLane.addVehicle(v);

                            soundManager.playTurnSignal();
                            addEventLog("Xe [" + v.getId() + "] rẽ sang " + nextRoad.getName() + " (" + nextLane.getDirection() + ")");
                            continue;
                        }
                    }
                }

                // If no outgoing road available, remove vehicle
                v.getLane().removeVehicle(v);
                toRemove.add(v);
                addEventLog("Xe [" + v.getId() + "] rời khu vực mô phỏng.");
            }
        }

        activeVehicles.removeAll(toRemove);

        // 3. Update dashboard UI
        updateDashboardUI();

        // 4. Render
        draw();
    }

    private Road getRoadForLane(Lane lane) {
        for (Road road : mapManager.getRoads()) {
            if (road.getLanes().contains(lane)) {
                return road;
            }
        }
        return null;
    }

    private Junction getDestJunction(Lane lane) {
        Road road = getRoadForLane(lane);
        if (road == null) return null;
        Location endLoc = road.getEndLocation();
        for (Junction j : mapManager.getJunctions()) {
            if (j.getLocation().distanceTo(endLoc) < 60.0) {
                return j;
            }
        }
        return null;
    }

    private void updateDashboardUI() {
        // Stats
        activeVehiclesLabel.setText(activeVehicles.size() + " Phương tiện");
        timeLabel.setText(String.format("Thời gian: %.1fs", simulationTime));

        double avgCongestion = mapManager.getAverageCongestion();
        congestionProgress.setProgress(avgCongestion);
        congestionValueLabel.setText(String.format("%.2f%%", avgCongestion * 100));

        // Adjust color based on congestion
        if (avgCongestion < 0.35) {
            congestionProgress.setStyle("-fx-accent: #2ecc71;"); // green
        } else if (avgCongestion < 0.7) {
            congestionProgress.setStyle("-fx-accent: #f1c40f;"); // yellow
        } else {
            congestionProgress.setStyle("-fx-accent: #e74c3c;"); // red
        }

        // Count type distributions
        int cars = 0, motos = 0, bikes = 0, ambulances = 0, fireTrucks = 0;
        for (MockVehicle v : activeVehicles) {
            switch (v.getType()) {
                case "Car" -> cars++;
                case "Motorbike" -> motos++;
                case "Bicycle" -> bikes++;
                case "Ambulance" -> ambulances++;
                case "FireTruck" -> fireTrucks++;
            }
        }

        carCountLabel.setText("• Ô tô (Car): " + cars);
        motoCountLabel.setText("• Xe máy (Motorbike): " + motos);
        bikeCountLabel.setText("• Xe đạp (Bicycle): " + bikes);
        ambCountLabel.setText("• Cấp cứu (Ambulance): " + ambulances);
        fireCountLabel.setText("• Cứu hỏa (FireTruck): " + fireTrucks);
    }

    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.save();
        // Apply camera translate and scale
        gc.translate(panX, panY);
        gc.scale(zoom, zoom);

        // Render road layout
        mapRenderer.render(gc, mapManager.getRoads(), mapManager.getJunctions());

        // Render vehicle sprites
        for (MockVehicle v : activeVehicles) {
            VehicleSprite sprite = new VehicleSprite(v);
            sprite.render(gc);
        }

        gc.restore();
    }

    public static void main(String[] args) {
        launch(args);
    }
}