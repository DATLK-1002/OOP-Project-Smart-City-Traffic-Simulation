package view.component;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import model.map.TrafficMapType;

public class JavaFxRoadView {

    private final double width;

    private final double height;
    private TrafficMapType mapType;

    public JavaFxRoadView(double width, double height) {
        this.width = width;
        this.height = height;
        this.mapType = TrafficMapType.CROSS_JUNCTION;
    }

    public void setMapType(TrafficMapType mapType) {
        this.mapType = mapType == null ? TrafficMapType.CROSS_JUNCTION : mapType;
    }

    public void render(GraphicsContext graphics) {
        renderLandscape(graphics);

        if (mapType == TrafficMapType.ROAD_NETWORK) {
            renderNetwork(graphics);
            return;
        }

        renderHorizontalRoad(graphics, 245, 210);
        renderVerticalRoad(graphics, 345, 210);
        renderCornerDecorations(graphics);

        if (mapType == TrafficMapType.T_JUNCTION) {
            graphics.setFill(Color.web("#2f8f45"));
            graphics.fillRect(345, 0, 210, 240);
            renderParkPatch(graphics, 365, 20, 170, 170);
        }

        if (mapType == TrafficMapType.FIVE_WAY_JUNCTION) {
            graphics.setStroke(Color.web("#4a4a4a"));
            graphics.setLineWidth(168);
            graphics.strokeLine(450, 350, 760, 80);
            graphics.setStroke(Color.WHITE);
            graphics.setLineWidth(2);
            graphics.setLineDashes(18);
            graphics.strokeLine(450, 350, 760, 80);
            graphics.setLineDashes(null);
        }

        graphics.setFill(Color.WHITE);
        graphics.fillText(mapType.getLabel() + " - đường rộng, vỉa hè, cây xanh và vạch qua đường", 20, 28);
    }

    private void renderLandscape(GraphicsContext graphics) {
        graphics.setFill(Color.web("#2f8f45"));
        graphics.fillRect(0, 0, width, height);
        renderParkPatch(graphics, 25, 35, 250, 150);
        renderParkPatch(graphics, 625, 35, 245, 150);
        renderParkPatch(graphics, 25, 515, 250, 140);
        renderParkPatch(graphics, 625, 515, 245, 140);
    }

    private void renderParkPatch(GraphicsContext graphics, double x, double y, double w, double h) {
        graphics.setFill(Color.web("#1f7a35"));
        graphics.fillRoundRect(x, y, w, h, 24, 24);
        graphics.setFill(Color.web("#e0b37a"));
        graphics.fillRect(x, y + h - 24, w, 24);
        drawTree(graphics, x + 45, y + 45, 1.0);
        drawTree(graphics, x + w - 55, y + 60, 0.8);
        drawBushes(graphics, x + 95, y + h - 45, w - 190);
    }

    private void drawTree(GraphicsContext graphics, double x, double y, double scale) {
        graphics.setFill(Color.web("#6b3f20"));
        graphics.fillRect(x - 5 * scale, y + 12 * scale, 10 * scale, 34 * scale);
        graphics.setFill(Color.web("#1f6f2b"));
        graphics.fillOval(x - 28 * scale, y - 10 * scale, 56 * scale, 42 * scale);
        graphics.setFill(Color.web("#2e8b3c"));
        graphics.fillOval(x - 18 * scale, y - 28 * scale, 40 * scale, 38 * scale);
        graphics.fillOval(x - 38 * scale, y + 2 * scale, 34 * scale, 30 * scale);
    }

    private void drawBushes(GraphicsContext graphics, double x, double y, double width) {
        graphics.setFill(Color.web("#d92626"));
        for (int i = 0; i < 10; i++) {
            graphics.fillOval(x + i * width / 10.0, y + (i % 2) * 8, 8, 8);
        }
    }

    private void renderNetwork(GraphicsContext graphics) {
        renderHorizontalRoad(graphics, 102, 105);
        renderHorizontalRoad(graphics, 302, 105);
        renderHorizontalRoad(graphics, 502, 105);
        renderVerticalRoad(graphics, 152, 105);
        renderVerticalRoad(graphics, 402, 105);
        renderVerticalRoad(graphics, 652, 105);

        graphics.setFill(Color.WHITE);
        graphics.fillText("Mạng lưới đường rộng - xe thu nhỏ để quan sát toàn vùng", 20, 28);
    }

    private void renderHorizontalRoad(GraphicsContext graphics, double y, double roadHeight) {
        graphics.setFill(Color.DARKGRAY);
        graphics.setFill(Color.web("#d0b48a"));
        graphics.fillRect(0, y - 18, width, roadHeight + 36);
        graphics.setFill(Color.web("#4a4a4a"));
        graphics.fillRect(0, y, width, roadHeight);
        graphics.setStroke(Color.WHITE);
        graphics.setLineWidth(3);
        graphics.setLineDashes(24);
        graphics.strokeLine(0, y + roadHeight / 2, width, y + roadHeight / 2);
        graphics.setLineDashes(null);
        graphics.setStroke(Color.web("#f4f4f4"));
        graphics.setLineWidth(2);
        graphics.strokeLine(0, y + 8, width, y + 8);
        graphics.strokeLine(0, y + roadHeight - 8, width, y + roadHeight - 8);
    }

    private void renderVerticalRoad(GraphicsContext graphics, double x, double roadWidth) {
        graphics.setFill(Color.web("#d0b48a"));
        graphics.fillRect(x - 18, 0, roadWidth + 36, height);
        graphics.setFill(Color.web("#4a4a4a"));
        graphics.fillRect(x, 0, roadWidth, height);
        graphics.setStroke(Color.WHITE);
        graphics.setLineWidth(3);
        graphics.setLineDashes(24);
        graphics.strokeLine(x + roadWidth / 2, 0, x + roadWidth / 2, 245);
        graphics.strokeLine(x + roadWidth / 2, 455, x + roadWidth / 2, height);
        graphics.setLineDashes(null);
        graphics.setStroke(Color.web("#f4f4f4"));
        graphics.setLineWidth(2);
        graphics.strokeLine(x + 8, 0, x + 8, height);
        graphics.strokeLine(x + roadWidth - 8, 0, x + roadWidth - 8, height);
    }   

    


    private void drawPedestrianSign(GraphicsContext graphics, double x, double y) {
        graphics.setStroke(Color.web("#6f6f6f"));
        graphics.setLineWidth(3);
        graphics.strokeLine(x + 18, y + 30, x + 18, y + 72);
        graphics.setFill(Color.web("#1c75bc"));
        graphics.fillRect(x, y, 36, 30);
        graphics.setFill(Color.WHITE);
        graphics.fillPolygon(new double[] {x + 18, x + 8, x + 28}, new double[] {y + 4, y + 26, y + 26}, 3);
        graphics.setFill(Color.web("#1c75bc"));
        graphics.fillOval(x + 15, y + 10, 6, 6);
    }

    private void renderCornerDecorations(GraphicsContext graphics) {
        graphics.setFill(Color.web("#2a2a2a"));
        graphics.fillPolygon(new double[] {345, 450, 345}, new double[] {245, 350, 455}, 3);
        graphics.fillPolygon(new double[] {555, 450, 555}, new double[] {245, 350, 455}, 3);
    }
}
