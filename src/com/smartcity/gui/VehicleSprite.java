package com.smartcity.gui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class VehicleSprite {
    // Static sprite cache to avoid reloading files
    private static final Map<String, Image> spriteCache = new HashMap<>();

    static {
        loadSprite("Car", "/com/smartcity/gui/assets/sprites/car.png");
        loadSprite("Motorbike", "/com/smartcity/gui/assets/sprites/motorbike.png");
        loadSprite("Ambulance", "/com/smartcity/gui/assets/sprites/ambulance.png");
        loadSprite("FireTruck", "/com/smartcity/gui/assets/sprites/firetruck.png");
        loadSprite("Bicycle", "/com/smartcity/gui/assets/sprites/bicycle.png");
    }

    private static void loadSprite(String key, String path) {
        try {
            URL url = VehicleSprite.class.getResource(path);
            if (url != null) {
                Image img = new Image(url.toExternalForm());
                if (!img.isError()) {
                    spriteCache.put(key, img);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load sprite for " + key + ": " + e.getMessage());
        }
    }

    private MockVehicle vehicle;
    private Image image;

    public VehicleSprite(Image image) {
        this.image = image;
    }

    public VehicleSprite(MockVehicle vehicle) {
        this.vehicle = vehicle;
        this.image = spriteCache.get(vehicle.getType());
    }

    public void render(GraphicsContext gc) {
        if (vehicle == null) return;

        double x = vehicle.getX();
        double y = vehicle.getY();
        double angle = vehicle.getAngle();
        double speed = vehicle.getSpeed();
        double maxSpeed = vehicle.getMaxSpeed();
        String type = vehicle.getType();

        gc.save();
        gc.translate(x, y);
        gc.rotate(angle);

        // 1. Draw Headlights Glow (Premium effect)
        gc.save();
        gc.setGlobalAlpha(0.15);
        gc.setFill(Color.YELLOW);
        // Draw a light cone in front of the vehicle
        gc.fillPolygon(
            new double[]{ 15, 60, 60 },
            new double[]{ 0, -25, 25 },
            3
        );
        gc.restore();

        // 2. Draw Vehicle Body/Sprite
        if (image != null) {
            // Draw sprite image centered
            gc.drawImage(image, -20, -10, 40, 20);
        } else {
            // Fallback vector drawing if sprite is missing
            drawFallbackVehicle(gc, type);
        }

        // 3. Draw Brake Lights (Glow red when stationary or slowing down)
        boolean isBraking = speed < 15.0 || speed < maxSpeed * 0.4;
        if (isBraking) {
            gc.setFill(Color.RED);
            // Left brake light
            gc.fillOval(-21, -8, 4, 4);
            // Right brake light
            gc.fillOval(-21, 4, 4, 4);

            // Add brake light glow
            gc.save();
            gc.setGlobalAlpha(0.3);
            gc.setFill(Color.RED);
            gc.fillOval(-25, -11, 10, 10);
            gc.fillOval(-25, 1, 10, 10);
            gc.restore();
        }

        // 4. Draw Flashing Siren for Emergency Vehicles (Ambulance/FireTruck)
        if ("Ambulance".equals(type) || "FireTruck".equals(type)) {
            long time = System.currentTimeMillis();
            boolean flashBlue = (time / 150) % 2 == 0;

            gc.save();
            if (flashBlue) {
                gc.setFill(Color.BLUE);
                gc.setStroke(Color.BLUE);
            } else {
                gc.setFill(Color.RED);
                gc.setStroke(Color.RED);
            }

            // Draw siren light bar
            gc.fillRoundRect(-3, -4, 6, 8, 2, 2);

            // Siren flash aura
            gc.setGlobalAlpha(0.25);
            gc.fillOval(-15, -15, 30, 30);
            gc.restore();
        }

        gc.restore();
    }

    private void drawFallbackVehicle(GraphicsContext gc, String type) {
        Color mainColor;
        switch (type) {
            case "Ambulance":
                mainColor = Color.WHITE;
                break;
            case "FireTruck":
                mainColor = Color.RED;
                break;
            case "Car":
                mainColor = Color.DODGERBLUE;
                break;
            case "Motorbike":
                mainColor = Color.ORANGE;
                break;
            case "Bicycle":
                mainColor = Color.LIMEGREEN;
                break;
            default:
                mainColor = Color.DARKGRAY;
        }

        // Draw body
        gc.setFill(mainColor);
        gc.fillRoundRect(-18, -8, 36, 16, 4, 4);

        // Draw windshield/glass
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRect(2, -6, 8, 12);
        gc.fillRect(-10, -6, 6, 12);

        // Draw wheels
        gc.setFill(Color.BLACK);
        gc.fillRoundRect(-14, -10, 8, 3, 1, 1);
        gc.fillRoundRect(-14, 7, 8, 3, 1, 1);
        gc.fillRoundRect(8, -10, 8, 3, 1, 1);
        gc.fillRoundRect(8, 7, 8, 3, 1, 1);

        // Draw emergency cross for ambulance
        if ("Ambulance".equals(type)) {
            gc.setFill(Color.RED);
            gc.fillRect(-8, -2, 6, 4);
            gc.fillRect(-7, -4, 4, 8);
        }
    }
}