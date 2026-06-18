package view.component;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.shape.ArcType;
import model.map.TrafficMapType;

public class JavaFxRoadView {

    private final double width;
    private final double height;
    private TrafficMapType mapType;

    private static final double CENTER_X = 450;
    private static final double CENTER_Y = 350;
    private static final double MAIN_ROAD_W = 260;
    private static final double SIDE_ROAD_W = 205;
    private static final double INTERSECTION = 260;

    public JavaFxRoadView(double width, double height) {
        this.width = width;
        this.height = height;
        this.mapType = TrafficMapType.CROSS_JUNCTION;
    }

    public void setMapType(TrafficMapType mapType) {
        this.mapType = mapType == null ? TrafficMapType.CROSS_JUNCTION : mapType;
    }

    public void render(GraphicsContext graphics) {
        renderVietnamLandscape(graphics);

        if (mapType == TrafficMapType.ROAD_NETWORK) {
            renderVietnamNetwork(graphics);
            return;
        }

        renderVietnamCrossRoad(graphics);

        if (mapType == TrafficMapType.T_JUNCTION) {
            graphics.setFill(Color.web("#d7d9dc"));
            graphics.fillRect(CENTER_X - SIDE_ROAD_W / 2, 0, SIDE_ROAD_W, CENTER_Y - INTERSECTION / 2);
            renderCornerBlock(graphics, CENTER_X - 100, 30, 200, 120, "CÔNG VIÊN");
        }

        if (mapType == TrafficMapType.FIVE_WAY_JUNCTION) {
            renderDiagonalVietnamRoad(graphics);
        }

    }

    private void renderVietnamLandscape(GraphicsContext graphics) {
        graphics.setFill(Color.web("#d7d9dc"));
        graphics.fillRect(0, 0, width, height);

        // Nền đô thị đơn giản, đã xóa chữ NHÀ PHỐ/CỬA HÀNG/QUÁN CÀ PHÊ/TRẠM BUS.
        graphics.setFill(Color.web("#cfd2d5"));
        graphics.fillRect(0, 0, CENTER_X - MAIN_ROAD_W / 2 - 32, height);
        graphics.fillRect(CENTER_X + MAIN_ROAD_W / 2 + 32, 0, width, height);
        graphics.fillRect(0, 0, width, CENTER_Y - SIDE_ROAD_W / 2 - 32);
        graphics.fillRect(0, CENTER_Y + SIDE_ROAD_W / 2 + 32, width, height);

        graphics.setFill(Color.web("#bfc2c6"));
        graphics.fillRect(CENTER_X - MAIN_ROAD_W / 2 - 18, 0, 18, height);
        graphics.fillRect(CENTER_X + MAIN_ROAD_W / 2, 0, 18, height);
        graphics.fillRect(0, CENTER_Y - SIDE_ROAD_W / 2 - 18, width, 18);
        graphics.fillRect(0, CENTER_Y + SIDE_ROAD_W / 2, width, 18);
    }

    private void renderCornerBlock(GraphicsContext graphics, double x, double y, double w, double h, String label) {
        graphics.setFill(Color.web("#eef0f1"));
        graphics.fillRoundRect(x, y, w, h, 22, 22);
        graphics.setStroke(Color.web("#c3c6c9"));
        graphics.setLineWidth(2);
        graphics.strokeRoundRect(x, y, w, h, 22, 22);

        graphics.setFill(Color.web("#7cc26e"));
        graphics.fillRoundRect(x + 12, y + 12, w - 24, h - 24, 18, 18);
        graphics.setFill(Color.web("#f2d08a"));
        graphics.fillRect(x + 12, y + h - 42, w - 24, 26);

        graphics.setFill(Color.web("#777777"));
        graphics.setFont(Font.font(12));
        graphics.fillText(label, x + 22, y + h - 23);

        drawTree(graphics, x + 50, y + 48, 0.75);
        drawTree(graphics, x + w - 55, y + 62, 0.65);
    }

    private void drawTree(GraphicsContext graphics, double x, double y, double scale) {
        graphics.setFill(Color.web("#7a4a27"));
        graphics.fillRect(x - 4 * scale, y + 11 * scale, 8 * scale, 28 * scale);
        graphics.setFill(Color.web("#2f8f45"));
        graphics.fillOval(x - 22 * scale, y - 12 * scale, 44 * scale, 34 * scale);
        graphics.fillOval(x - 13 * scale, y - 27 * scale, 30 * scale, 28 * scale);
    }

    private void renderVietnamCrossRoad(GraphicsContext g) {
        double hY = CENTER_Y - SIDE_ROAD_W / 2;
        double vX = CENTER_X - MAIN_ROAD_W / 2;
        double iX = CENTER_X - INTERSECTION / 2;
        double iY = CENTER_Y - INTERSECTION / 2;

        // Vỉa hè/bo cua.
        g.setFill(Color.web("#c8cace"));
        g.fillRect(0, hY - 18, width, SIDE_ROAD_W + 36);
        g.fillRect(vX - 18, 0, MAIN_ROAD_W + 36, height);

        // Mặt đường chính.
        g.setFill(Color.web("#555b58"));
        g.fillRect(0, hY, width, SIDE_ROAD_W);
        g.fillRect(vX, 0, MAIN_ROAD_W, height);
        g.fillRect(iX, iY, INTERSECTION, INTERSECTION);

        // Bo góc mở rộng giống ảnh.
        g.setFill(Color.web("#555b58"));
        g.fillPolygon(new double[]{iX, iX - 90, iX, iX}, new double[]{iY, CENTER_Y, CENTER_Y - 55, iY}, 4);
        g.fillPolygon(new double[]{iX, iX - 90, iX, iX}, new double[]{iY + INTERSECTION, CENTER_Y, CENTER_Y + 55, iY + INTERSECTION}, 4);
        g.fillPolygon(new double[]{iX + INTERSECTION, iX + INTERSECTION + 90, iX + INTERSECTION, iX + INTERSECTION}, new double[]{iY, CENTER_Y, CENTER_Y - 55, iY}, 4);
        g.fillPolygon(new double[]{iX + INTERSECTION, iX + INTERSECTION + 90, iX + INTERSECTION, iX + INTERSECTION}, new double[]{iY + INTERSECTION, CENTER_Y, CENTER_Y + 55, iY + INTERSECTION}, 4);

        // Đã xóa đường/làn rẽ phải trước đèn đỏ.
        drawMedian(g);
        drawLaneDashedLines(g);
        drawStopLinesAndCrosswalks(g);
        // Không vẽ vạch trắng liền và thanh đỏ; chỉ giữ zebra + vạch nét đứt.
    }


    private void drawRightTurnSlipLanes(GraphicsContext g) {
        // Đã xóa đường rẽ phải tách riêng trước đèn đỏ.
    }

    private void drawMedian(GraphicsContext g) {
        g.setFill(Color.web("#333735"));
        g.fillRect(0, CENTER_Y - 7, CENTER_X - INTERSECTION / 2 - 25, 14);
        g.fillRect(CENTER_X + INTERSECTION / 2 + 25, CENTER_Y - 7, width, 14);
        g.fillRect(CENTER_X - 8, 0, 16, CENTER_Y - INTERSECTION / 2 - 24);
        g.fillRect(CENTER_X - 8, CENTER_Y + INTERSECTION / 2 + 24, 16, height);

        g.setFill(Color.web("#f5df4d"));
        g.fillRect(0, CENTER_Y - 3, CENTER_X - INTERSECTION / 2 - 30, 6);
        g.fillRect(CENTER_X + INTERSECTION / 2 + 30, CENTER_Y - 3, width, 6);
        g.fillRect(CENTER_X - 3, 0, 6, CENTER_Y - INTERSECTION / 2 - 30);
        g.fillRect(CENTER_X - 3, CENTER_Y + INTERSECTION / 2 + 30, 6, height);
    }

    private void drawBusLanes(GraphicsContext g) {
        // Đã bỏ làn xe buýt riêng: bus chạy chung làn ô tô.
    }

    private void drawLaneDashedLines(GraphicsContext g) {
        g.setStroke(Color.web("#e9eeee"));
        g.setLineWidth(2);
        g.setLineDashes(18, 16);

        // Vạch làn chỉ vẽ trên đoạn đường thẳng, chừa vùng thanh chắn/đèn đỏ và vạch qua đường.
        double leftEnd = CENTER_X - INTERSECTION / 2 - 72;
        double rightStart = CENTER_X + INTERSECTION / 2 + 72;
        double topEnd = CENTER_Y - INTERSECTION / 2 - 72;
        double bottomStart = CENTER_Y + INTERSECTION / 2 + 72;

        double[] ys = {CENTER_Y - 68, CENTER_Y - 42, CENTER_Y + 42, CENTER_Y + 68};
        for (double y : ys) {
            g.strokeLine(0, y, leftEnd, y);
            g.strokeLine(rightStart, y, width, y);
        }
        double[] xs = {CENTER_X - 88, CENTER_X - 62, CENTER_X - 36, CENTER_X + 36, CENTER_X + 62, CENTER_X + 88};
        for (double x : xs) {
            g.strokeLine(x, 0, x, topEnd);
            g.strokeLine(x, bottomStart, x, height);
        }
        g.setLineDashes(null);
        // Không vẽ các vạch phân làn liền màu trắng.
    }

    private void drawTurnPocketLanes(GraphicsContext g, double leftEnd, double rightStart, double topEnd, double bottomStart) {
        g.setStroke(Color.WHITE);
        g.setLineWidth(2.4);
        g.setLineDashes(null);

        // Làn rẽ tách trước khi tới ngã tư.
        g.strokeLine(leftEnd - 135, CENTER_Y + 18, leftEnd, CENTER_Y + 18);
        g.strokeLine(leftEnd - 135, CENTER_Y + 72, leftEnd, CENTER_Y + 72);
        g.strokeLine(rightStart, CENTER_Y - 18, rightStart + 135, CENTER_Y - 18);
        g.strokeLine(rightStart, CENTER_Y - 72, rightStart + 135, CENTER_Y - 72);
        g.strokeLine(CENTER_X - 18, bottomStart, CENTER_X - 18, bottomStart + 135);
        g.strokeLine(CENTER_X - 72, bottomStart, CENTER_X - 72, bottomStart + 135);
        g.strokeLine(CENTER_X + 18, topEnd - 135, CENTER_X + 18, topEnd);
        g.strokeLine(CENTER_X + 72, topEnd - 135, CENTER_X + 72, topEnd);
    }

    private void drawStopLinesAndCrosswalks(GraphicsContext g) {
        double iX = CENTER_X - INTERSECTION / 2;
        double iY = CENTER_Y - INTERSECTION / 2;
        double iR = CENTER_X + INTERSECTION / 2;
        double iB = CENTER_Y + INTERSECTION / 2;

        // Chỉ giữ zebra crossing. Điểm dừng đèn đỏ vẫn được xử lý bằng tọa độ
        // trong VehicleController, không cần vẽ vạch trắng liền trên bản đồ.
        drawCrosswalkVertical(g, iX - 60, CENTER_Y - SIDE_ROAD_W / 2 + 10, 28, SIDE_ROAD_W - 20);
        drawCrosswalkVertical(g, iR + 32, CENTER_Y - SIDE_ROAD_W / 2 + 10, 28, SIDE_ROAD_W - 20);
        drawCrosswalkHorizontal(g, CENTER_X - MAIN_ROAD_W / 2 + 10, iY - 60, MAIN_ROAD_W - 20, 28);
        drawCrosswalkHorizontal(g, CENTER_X - MAIN_ROAD_W / 2 + 10, iB + 32, MAIN_ROAD_W - 20, 28);
    }

    private void drawCrosswalkVertical(GraphicsContext g, double x, double y, double w, double h) {
        g.setFill(Color.web("#f4f4f4"));
        for (int i = 0; i < 12; i++) {
            g.fillRect(x, y + 8 + i * (h - 16) / 12.0, w, 7);
        }
    }

    private void drawCrosswalkHorizontal(GraphicsContext g, double x, double y, double w, double h) {
        g.setFill(Color.web("#f4f4f4"));
        for (int i = 0; i < 13; i++) {
            g.fillRect(x + 8 + i * (w - 16) / 13.0, y, 7, h);
        }
    }

    private void drawVietnamTurnArrows(GraphicsContext g) {
        // Đã xóa mũi tên chỉ đường trên mặt đường.
    }

    private void drawArrow(GraphicsContext g, double x, double y, double angle) {
        g.save();
        g.translate(x, y);
        g.rotate(angle);
        g.strokeLine(-24, 0, 20, 0);
        g.fillPolygon(new double[]{20, 7, 7}, new double[]{0, -7, 7}, 3);
        g.restore();
    }

    private void drawMotorbikeBoxes(GraphicsContext g) {
        g.setStroke(Color.web("#f5df4d"));
        g.setLineWidth(3);
        g.strokeRect(CENTER_X - 130, CENTER_Y + 96, 86, 22);
        g.strokeRect(CENTER_X + 44, CENTER_Y - 118, 86, 22);
        g.strokeRect(CENTER_X - 155, CENTER_Y - 130, 24, 86);
        g.strokeRect(CENTER_X + 131, CENTER_Y + 44, 24, 86);
        g.setFill(Color.web("#f5df4d"));
        g.setFont(Font.font(10));
        g.fillText("XE MÁY", CENTER_X - 120, CENTER_Y + 112);
        g.fillText("XE MÁY", CENTER_X + 56, CENTER_Y - 103);
    }

    private void drawTrafficLightBars(GraphicsContext g) {
        g.setStroke(Color.RED);
        g.setLineWidth(7);
        // Thanh chắn đỏ nằm trước zebra crossing, cùng phía với vạch dừng.
        g.strokeLine(CENTER_X - 190, CENTER_Y + 122, CENTER_X - 84, CENTER_Y + 122);
        g.strokeLine(CENTER_X + 84, CENTER_Y - 122, CENTER_X + 190, CENTER_Y - 122);
        g.strokeLine(CENTER_X - 190, CENTER_Y - 122, CENTER_X - 190, CENTER_Y - 52);
        g.strokeLine(CENTER_X + 190, CENTER_Y + 52, CENTER_X + 190, CENTER_Y + 122);
    }

    private void renderDiagonalVietnamRoad(GraphicsContext g) {
        g.setStroke(Color.web("#555b58"));
        g.setLineWidth(92);
        g.strokeLine(0, 665, 285, 455);
        g.setStroke(Color.web("#e9eeee"));
        g.setLineWidth(2);
        g.setLineDashes(20, 16);
        g.strokeLine(0, 665, 285, 455);
        g.setLineDashes(null);
    }

    private void renderVietnamNetwork(GraphicsContext graphics) {
        graphics.setFill(Color.web("#d7d9dc"));
        graphics.fillRect(0, 0, width, height);
        graphics.setFill(Color.web("#555b58"));
        for (int y = 90; y <= 590; y += 170) {
            graphics.fillRect(0, y, width, 72);
        }
        for (int x = 120; x <= 760; x += 210) {
            graphics.fillRect(x, 0, 72, height);
        }
        graphics.setStroke(Color.WHITE);
        graphics.setLineWidth(2);
        graphics.setLineDashes(18, 14);
        for (int y = 126; y <= 626; y += 170) graphics.strokeLine(0, y, width, y);
        for (int x = 156; x <= 796; x += 210) graphics.strokeLine(x, 0, x, height);
        graphics.setLineDashes(null);
        graphics.setFill(Color.WHITE);
        graphics.setFont(Font.font(15));
        graphics.fillText("Mạng lưới đường Việt Nam - đường chính, đường gom và nhiều nút giao", 20, 27);
    }
}
