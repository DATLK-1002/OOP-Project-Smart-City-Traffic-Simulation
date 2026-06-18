package view.component;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import model.trafficlight.TrafficLight;

public class JavaFxTrafficLightView {

    private static final double WIDTH = 42;
    private static final double HEIGHT = 112;

    private final TrafficLight trafficLight;

    public JavaFxTrafficLightView(TrafficLight trafficLight) {

        this.trafficLight = trafficLight;
    }

    public void render(GraphicsContext graphics,
                       double x,
                       double y) {

        graphics.setFill(Color.BLACK);
        graphics.fillRoundRect(x, y, WIDTH, HEIGHT, 12, 12);
        drawBulb(graphics, x + 21, y + 24, Color.RED, trafficLight.isRed());
        drawBulb(graphics, x + 21, y + 56, Color.YELLOW, trafficLight.isYellow());
        drawBulb(graphics, x + 21, y + 88, Color.LIMEGREEN, trafficLight.isGreen());

        graphics.setFill(Color.WHITE);
        graphics.fillText(trafficLight.getId(), x - 4, y - 8);
        if (trafficLight.shouldDisplayTimer()) {
            graphics.fillText(String.valueOf(trafficLight.getTimer()), x + 14, y + HEIGHT + 16);
        }
    }

    private void drawBulb(GraphicsContext graphics,
                          double x,
                          double y,
                          Color color,
                          boolean active) {

        graphics.setFill(active ? color : Color.web("#333333"));
        graphics.fillOval(x - 10, y - 10, 20, 20);
    }
}
