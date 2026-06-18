package Main;

import controller.LightControlMode;
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
    private int manualSpawnIndex = 0;

    @Override
    public void start(Stage stage) {

        statusLabel = new Label("Ready");
        resetController();

        Button startButton = new Button("Start");
        Button pauseButton = new Button("Pause");
        Button resetButton = new Button("Reset");
        Button addVehicleButton = new Button("+ Xe");
        ComboBox<TrafficMapType> mapBox = new ComboBox<>();
        mapBox.getItems().addAll(TrafficMapType.values());
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
            // Thêm xe mới vào hàng chờ trước vạch đèn đỏ, không reset map và không spawn sau đèn.
            addOneVehicleBeforeTrafficLight();
            selectedVehicleCount = Math.min(60, selectedVehicleCount + 1);
            vehicleCountSpinner.getValueFactory().setValue(selectedVehicleCount);
            simulationScreen.render(tick);
        });

        mapBox.setOnAction(event -> { selectedMap = mapBox.getValue(); resetSimulation(); });
        densityBox.setOnAction(event -> {
            selectedDensity = densityBox.getValue();
            selectedVehicleCount = selectedDensity.getVehicleCount();
            vehicleCountSpinner.getValueFactory().setValue(selectedVehicleCount);
            resetSimulation();
        });
        vehicleCountSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            selectedVehicleCount = newValue == null ? selectedDensity.getVehicleCount() : newValue;
            resetSimulation();
        });
        displayBox.setOnAction(event -> { selectedDisplay = displayBox.getValue(); simulationScreen.setDisplayMode(selectedDisplay); simulationScreen.render(tick); });
        controlBox.setOnAction(event -> { selectedControl = controlBox.getValue(); controller.getTrafficController().setControlMode(selectedControl); });

        HBox toolbar = new HBox(10, startButton, pauseButton, resetButton, addVehicleButton,
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

        stage.setTitle("Traffic System JavaFX");
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
        controller.getTrafficController().setControlMode(selectedControl);
        nextVehicleId = 1;
        manualSpawnIndex = 0;
        setupDemoScenario();
        simulationScreen = new JavaFxSimulationScreen(controller, statusLabel);
        simulationScreen.setMapType(selectedMap);
        simulationScreen.setDisplayMode(selectedDisplay);
        tick = 0;
    }

    private void setupDemoScenario() {
        controller.getTrafficController().addTrafficLight(new BasicTrafficLight("TL-East"));
        controller.getTrafficController().addTrafficLight(new CountdownTrafficLight("TL-South"));
        controller.getTrafficController().addTrafficLight(new SmartTrafficLight("TL-West"));
        controller.getTrafficController().addTrafficLight(new CountdownTrafficLight("TL-North"));

        String[] types = {"CAR", "MOTORBIKE", "BUS", "CAR", "BICYCLE", "AMBULANCE", "FIRETRUCK"};
        Direction[] directions = {Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH};
        Vector2D[] starts = {
                new Vector2D(70, 350), new Vector2D(830, 350),
                new Vector2D(450, 650), new Vector2D(450, 60)
        };

        for (int i = 0; i < selectedVehicleCount; i++) {
            String type = types[i % types.length];
            Direction direction = directions[i % directions.length];
            int laneOrder = i / directions.length;
            Vehicle vehicle = VehicleFactory.createVehicle(type, typeLabel(type) + "-" + (nextVehicleId++),
                    spawnBeforeStopLine(type, direction, laneOrder), direction);
            if (type.equals("AMBULANCE") || type.equals("FIRETRUCK")) {
                vehicle.setStrategy(new EmergencyDriver());
            } else if (i % 4 == 0) {
                vehicle.setStrategy(new AggressiveDriver());
            } else {
                vehicle.setStrategy(new NormalDriver());
            }
            controller.getVehicleController().addVehicle(vehicle);
        }
    }


    private void addOneVehicleBeforeTrafficLight() {
        String[] types = {"CAR", "MOTORBIKE", "BUS", "CAR", "BICYCLE", "AMBULANCE", "FIRETRUCK"};
        Direction[] directions = {Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH};

        String type = types[manualSpawnIndex % types.length];
        Direction direction = directions[manualSpawnIndex % directions.length];
        Vector2D position = spawnBeforeStopLine(type, direction, manualSpawnIndex / directions.length);

        Vehicle vehicle = VehicleFactory.createVehicle(type, typeLabel(type) + "-" + (nextVehicleId++), position, direction);
        if (type.equals("AMBULANCE") || type.equals("FIRETRUCK")) {
            vehicle.setStrategy(new EmergencyDriver());
        } else if (manualSpawnIndex % 4 == 0) {
            vehicle.setStrategy(new AggressiveDriver());
        } else {
            vehicle.setStrategy(new NormalDriver());
        }
        controller.getVehicleController().addVehicle(vehicle);
        manualSpawnIndex++;
        statusLabel.setText("Đã thêm xe trước đèn đỏ: " + vehicle.getId());
    }

    private Vector2D spawnBeforeStopLine(String type, Direction direction, int queueIndex) {
        double laneOffset = laneOffsetFor(type);
        double gap = 58 + (queueIndex % 10) * 48;
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

    private Vector2D rightLaneStartPosition(String type, Direction direction, Vector2D base, int index) {
        double laneOffset = laneOffsetFor(type);
        switch (direction) {
            case EAST:
                return new Vector2D(base.getX() + (index % 14) * 55, 350 + laneOffset);
            case WEST:
                return new Vector2D(base.getX() - (index % 14) * 55, 350 - laneOffset);
            case NORTH:
                return new Vector2D(450 + laneOffset, base.getY() - (index % 11) * 55);
            case SOUTH:
                return new Vector2D(450 - laneOffset, base.getY() + (index % 11) * 55);
            default:
                return base;
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
            case "MOTORBIKE": return "MOTO";
            case "BICYCLE": return "BIKE";
            case "AMBULANCE": return "AMB";
            case "FIRETRUCK": return "FIRE";
            case "BUS": return "BUS";
            default: return "CAR";
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