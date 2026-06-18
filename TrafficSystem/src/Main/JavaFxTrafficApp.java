package Main;

import controller.LightControlMode;
import controller.MapRouteEngine;
import controller.SimulationController;
import controller.TrafficDensity;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.map.TrafficMapType;
import model.trafficlight.BasicTrafficLight;
import model.trafficlight.CountdownTrafficLight;
import model.trafficlight.SmartTrafficLight;
import model.trafficlight.TrafficLight;
import model.vehicle.Vehicle;
import model.vehicle.VehicleFactory;
import strategy.AggressiveDriver;
import strategy.EmergencyDriver;
import strategy.NormalDriver;
import util.Direction;
import util.Vector2D;
import view.renderer.DisplayMode;
import view.screen.JavaFxSimulationScreen;

public class JavaFxTrafficApp extends Application {

    private static final long FRAME_INTERVAL = 100_000_000L;
    private static final String[] VEHICLE_TYPES = {
            "CAR", "MOTORBIKE", "BUS", "CAR", "BICYCLE", "AMBULANCE", "FIRETRUCK"
    };
    private static final Direction[] CROSS_DIRECTIONS = {
            Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH
    };

    private SimulationController controller;
    private JavaFxSimulationScreen simulationScreen;
    private BorderPane root;
    private Label statusLabel;
    private AnimationTimer timer;
    private long lastFrame;
    private int tick;

    private TrafficMapType selectedMap = TrafficMapType.CROSS_JUNCTION;
    private TrafficDensity selectedDensity = TrafficDensity.LIGHT;
    private int selectedVehicleCount = selectedDensity.getVehicleCount();
    private DisplayMode selectedDisplay = DisplayMode.BASIC;
    private LightControlMode selectedControl = LightControlMode.AUTOMATIC;
    private int nextVehicleId = 1;
    private int spawnSequence = 0;
    private boolean suppressVehicleSpinnerReset;

    @Override
    public void start(Stage stage) {
        statusLabel = new Label("Sẵn sàng");
        resetController();

        Button startButton = new Button("Start");
        Button pauseButton = new Button("Pause");
        Button resetButton = new Button("Reset");
        Button addVehicleButton = new Button("+ Xe");

        ComboBox<TrafficMapType> mapBox = new ComboBox<>();
        mapBox.getItems().addAll(TrafficMapType.T_JUNCTION, TrafficMapType.CROSS_JUNCTION, TrafficMapType.ROAD_NETWORK);
        mapBox.setValue(selectedMap);

        ComboBox<TrafficDensity> densityBox = new ComboBox<>();
        densityBox.getItems().addAll(TrafficDensity.values());
        densityBox.setValue(selectedDensity);

        Spinner<Integer> vehicleCountSpinner = new Spinner<>(1, 60, selectedVehicleCount);
        vehicleCountSpinner.setEditable(true);
        vehicleCountSpinner.setPrefWidth(82);

        ComboBox<DisplayMode> displayBox = new ComboBox<>();
        displayBox.getItems().addAll(DisplayMode.values());
        displayBox.setValue(selectedDisplay);

        ComboBox<LightControlMode> controlBox = new ComboBox<>();
        controlBox.getItems().addAll(LightControlMode.values());
        controlBox.setValue(selectedControl);

        startButton.setOnAction(event -> controller.start());
        pauseButton.setOnAction(event -> controller.stop());
        resetButton.setOnAction(event -> resetSimulation());
        addVehicleButton.setOnAction(event -> {
            addOneVehicle();
            selectedVehicleCount = Math.min(60, selectedVehicleCount + 1);
            suppressVehicleSpinnerReset = true;
            vehicleCountSpinner.getValueFactory().setValue(selectedVehicleCount);
            suppressVehicleSpinnerReset = false;
            simulationScreen.render(tick);
        });

        mapBox.setOnAction(event -> {
            selectedMap = mapBox.getValue();
            resetSimulation();
        });

        densityBox.setOnAction(event -> {
            selectedDensity = densityBox.getValue();
            selectedVehicleCount = selectedDensity.getVehicleCount();
            suppressVehicleSpinnerReset = true;
            vehicleCountSpinner.getValueFactory().setValue(selectedVehicleCount);
            suppressVehicleSpinnerReset = false;
            resetSimulation();
        });

        vehicleCountSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (suppressVehicleSpinnerReset) return;
            selectedVehicleCount = newValue == null ? selectedDensity.getVehicleCount() : newValue;
            resetSimulation();
        });

        displayBox.setOnAction(event -> {
            selectedDisplay = displayBox.getValue();
            simulationScreen.setDisplayMode(selectedDisplay);
            simulationScreen.render(tick);
        });

        controlBox.setOnAction(event -> {
            selectedControl = controlBox.getValue();
            controller.getTrafficController().setControlMode(selectedControl);
        });

        HBox toolbar = new HBox(10,
                startButton, pauseButton, resetButton, addVehicleButton,
                new Label("Bản đồ:"), mapBox,
                new Label("Lưu lượng:"), densityBox,
                new Label("Số xe:"), vehicleCountSpinner,
                new Label("Hiển thị:"), displayBox,
                new Label("Đèn:"), controlBox,
                statusLabel);
        toolbar.setPadding(new Insets(10));

        root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(simulationScreen);

        stage.setTitle("Smart City Traffic Simulation");
        stage.setScene(new Scene(root, JavaFxSimulationScreen.WIDTH, JavaFxSimulationScreen.HEIGHT + 58));
        stage.show();

        simulationScreen.render(tick);
        runAnimationLoop();
    }

    private void runAnimationLoop() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastFrame < FRAME_INTERVAL) return;
                lastFrame = now;

                if (controller.isRunning()) {
                    controller.update();
                    tick++;
                }
                simulationScreen.render(tick);
            }
        };
        timer.start();
    }

    private void resetSimulation() {
        controller.stop();
        resetController();
        root.setCenter(simulationScreen);
        simulationScreen.render(tick);
    }

    private void resetController() {
        controller = new SimulationController();
        controller.setMapType(selectedMap);
        controller.getTrafficController().setControlMode(selectedControl);
        nextVehicleId = 1;
        spawnSequence = 0;
        setupDemoScenario();
        spawnSequence = selectedVehicleCount;

        simulationScreen = new JavaFxSimulationScreen(controller, statusLabel);
        simulationScreen.setMapType(selectedMap);
        simulationScreen.setDisplayMode(selectedDisplay);
        tick = 0;
    }

    private void setupDemoScenario() {
        setupTrafficLights();

        if (selectedMap == TrafficMapType.CROSS_JUNCTION) {
            setupCrossJunctionVehicles();
        } else {
            setupRouteVehicles();
        }
    }

    private void setupTrafficLights() {
        for (int i = 0; i < selectedMap.getTrafficLightCount(); i++) {
            String id = "TL-" + (i + 1);
            TrafficLight light;
            if (i % 3 == 1) {
                light = new CountdownTrafficLight(id);
            } else if (i % 3 == 2) {
                light = new SmartTrafficLight(id);
            } else {
                light = new BasicTrafficLight(id);
            }
            controller.getTrafficController().addTrafficLight(light);
        }
        // Đồng bộ lại pha sau khi đã tạo đủ số đèn của loại bản đồ.
        controller.getTrafficController().configureForMap(selectedMap);
        controller.getTrafficController().setControlMode(selectedControl);
    }

    private void setupCrossJunctionVehicles() {
        for (int i = 0; i < selectedVehicleCount; i++) {
            String type = VEHICLE_TYPES[i % VEHICLE_TYPES.length];
            Direction direction = CROSS_DIRECTIONS[i % CROSS_DIRECTIONS.length];
            int queueIndex = i / CROSS_DIRECTIONS.length;
            Vehicle vehicle = createVehicle(
                    type,
                    spawnBeforeStopLine(type, direction, queueIndex),
                    direction,
                    i);
            controller.addVehicle(vehicle, 0, queueIndex);
        }
    }

    private void setupRouteVehicles() {
        MapRouteEngine engine = controller.getMapRouteEngine();
        int routeCount = Math.max(1, engine.getRouteCount());
        for (int i = 0; i < selectedVehicleCount; i++) {
            int routeIndex = i % routeCount;
            int queueIndex = i / routeCount;
            String type = VEHICLE_TYPES[i % VEHICLE_TYPES.length];
            Direction direction = engine.getInitialDirection(routeIndex);
            Vector2D position = engine.getInitialPosition(routeIndex, queueIndex);
            Vehicle vehicle = createVehicle(type, position, direction, i);
            controller.addVehicle(vehicle, routeIndex, queueIndex);
        }
    }

    private void addOneVehicle() {
        String type = VEHICLE_TYPES[spawnSequence % VEHICLE_TYPES.length];
        Vehicle vehicle;
        String routeDescription;

        if (selectedMap == TrafficMapType.CROSS_JUNCTION) {
            Direction direction = CROSS_DIRECTIONS[spawnSequence % CROSS_DIRECTIONS.length];
            int queueIndex = spawnSequence / CROSS_DIRECTIONS.length;
            vehicle = createVehicle(
                    type,
                    spawnBeforeStopLine(type, direction, queueIndex),
                    direction,
                    spawnSequence);
            controller.addVehicle(vehicle, 0, queueIndex);
            routeDescription = direction.toString();
        } else {
            MapRouteEngine engine = controller.getMapRouteEngine();
            int routeCount = Math.max(1, engine.getRouteCount());
            int routeIndex = spawnSequence % routeCount;
            int queueIndex = spawnSequence / routeCount;
            Direction direction = engine.getInitialDirection(routeIndex);
            vehicle = createVehicle(
                    type,
                    engine.getInitialPosition(routeIndex, queueIndex),
                    direction,
                    spawnSequence);
            controller.addVehicle(vehicle, routeIndex, queueIndex);
            routeDescription = engine.getRouteName(routeIndex);
        }

        spawnSequence++;
        statusLabel.setText("Đã thêm " + vehicle.getId() + " | Tuyến: " + routeDescription);
    }

    private Vehicle createVehicle(String type, Vector2D position, Direction direction, int sequence) {
        Vehicle vehicle = VehicleFactory.createVehicle(
                type,
                typeLabel(type) + "-" + (nextVehicleId++),
                position,
                direction);
        if (type.equals("AMBULANCE") || type.equals("FIRETRUCK")) {
            vehicle.setStrategy(new EmergencyDriver());
        } else if (sequence % 4 == 0) {
            vehicle.setStrategy(new AggressiveDriver());
        } else {
            vehicle.setStrategy(new NormalDriver());
        }
        return vehicle;
    }

    private Vector2D spawnBeforeStopLine(String type, Direction direction, int queueIndex) {
        double laneOffset = laneOffsetFor(type);
        // Xếp hàng ban đầu với khoảng trống đủ lớn để xe không vừa sinh ra đã bám sát nhau.
        double gap = 72 + Math.max(0, queueIndex) * 68;
        double stopOffset = 190;
        double centerX = 450;
        double centerY = 350;
        switch (direction) {
            case EAST:
                return new Vector2D(centerX - stopOffset - gap, centerY + laneOffset);
            case WEST:
                return new Vector2D(centerX + stopOffset + gap, centerY - laneOffset);
            case NORTH:
                return new Vector2D(centerX + laneOffset, centerY + stopOffset + gap);
            case SOUTH:
                return new Vector2D(centerX - laneOffset, centerY - stopOffset - gap);
            default:
                return new Vector2D(centerX, centerY);
        }
    }

    private double laneOffsetFor(String type) {
        switch (type) {
            case "BUS":
                return 26;
            case "BICYCLE":
                return 68;
            case "MOTORBIKE":
                return 47;
            default:
                return 26;
        }
    }

    private String typeLabel(String type) {
        switch (type) {
            case "MOTORBIKE":
                return "MOTO";
            case "BICYCLE":
                return "BIKE";
            case "AMBULANCE":
                return "AMB";
            case "FIRETRUCK":
                return "FIRE";
            case "BUS":
                return "BUS";
            default:
                return "CAR";
        }
    }

    @Override
    public void stop() {
        if (timer != null) timer.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
