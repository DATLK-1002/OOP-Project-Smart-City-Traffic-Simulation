package view.component;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import model.trafficlight.TrafficLight;

public class JavaFxTrafficLightView {

    public static final double WIDTH = 42;
    public static final double HEIGHT = 112;
    public static final double COMPACT_WIDTH = 18;
    public static final double COMPACT_HEIGHT = 46;

    private final TrafficLight trafficLight;

    public JavaFxTrafficLightView(TrafficLight trafficLight) {
        this.trafficLight = trafficLight;
    }

    public void render(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.BLACK);
        graphics.fillRoundRect(x, y, WIDTH, HEIGHT, 12, 12);
        drawBulb(graphics, x + 21, y + 24, 10, Color.RED, trafficLight.isRed());
        drawBulb(graphics, x + 21, y + 56, 10, Color.YELLOW, trafficLight.isYellow());
        drawBulb(graphics, x + 21, y + 88, 10, Color.LIMEGREEN, trafficLight.isGreen());

        graphics.setFill(Color.WHITE);
        graphics.setFont(Font.font(11));
        graphics.fillText(trafficLight.getId(), x - 4, y - 8);
        if (trafficLight.shouldDisplayTimer()) {
            graphics.fillText(String.valueOf(trafficLight.getTimer()), x + 14, y + HEIGHT + 16);
        }
    }

    public void renderCompact(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.web("#101214"));
        graphics.fillRoundRect(x, y, COMPACT_WIDTH, COMPACT_HEIGHT, 6, 6);
        drawBulb(graphics, x + COMPACT_WIDTH / 2, y + 9, 4.2, Color.RED, trafficLight.isRed());
        drawBulb(graphics, x + COMPACT_WIDTH / 2, y + 23, 4.2, Color.YELLOW, trafficLight.isYellow());
        drawBulb(graphics, x + COMPACT_WIDTH / 2, y + 37, 4.2, Color.LIMEGREEN, trafficLight.isGreen());
    }

    private void drawBulb(GraphicsContext graphics,
                          double x,
                          double y,
                          double radius,
                          Color color,
                          boolean active) {
        graphics.setFill(active ? color : Color.web("#333333"));
        graphics.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    }
}
