package com.smartcity.gui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.List;

import com.smartcity.traffic.road.Junction;
import com.smartcity.traffic.road.Lane;
import com.smartcity.traffic.road.Location;
import com.smartcity.traffic.road.Road;

public class MapRenderer {
    private double zoomFactor;

    public MapRenderer() {
        this.zoomFactor = 1.0;
    }

    public void setZoomFactor(double zoomFactor) {
        this.zoomFactor = zoomFactor;
    }

    public double getZoomFactor() {
        return zoomFactor;
    }

    public void render(GraphicsContext gc, List<Road> roads, List<Junction> junctions) {
        // Render layers: background, roads, lanes, junctions, traffic lights
        drawBackgroundGrid(gc);
        drawRoads(gc, roads);
        drawJunctions(gc, junctions);
        drawTrafficLights(gc, junctions);
    }

    private void drawBackgroundGrid(GraphicsContext gc) {
        double width = 2000;
        double height = 1200;

        // Draw dark background slate
        gc.setFill(Color.web("#1e272e")); // dark night mode
        gc.fillRect(-width, -height, width * 2, height * 2);

        // Draw subtle grid lines
        gc.setStroke(Color.web("#2f3542"));
        gc.setLineWidth(1.0);
        
        double gridSize = 80.0;
        for (double x = -width; x < width; x += gridSize) {
            gc.strokeLine(x, -height, x, height);
        }
        for (double y = -height; y < height; y += gridSize) {
            gc.strokeLine(-width, y, width, y);
        }

        // Draw some green zone squares (parks) for premium aesthetic
        gc.setFill(Color.web("#218c74", 0.08)); // soft green
        gc.fillRoundRect(100, 100, 400, 300, 20, 20);
        gc.fillRoundRect(900, 100, 300, 300, 20, 20);
        gc.fillRoundRect(100, 550, 450, 250, 20, 20);
        gc.fillRoundRect(950, 550, 450, 250, 20, 20);
    }

    private void drawRoads(GraphicsContext gc, List<Road> roads) {
        for (Road road : roads) {
            Location start = road.getStartLocation();
            Location end = road.getEndLocation();

            double x1 = start.getX();
            double y1 = start.getY();
            double x2 = end.getX();
            double y2 = end.getY();

            // 1. Draw asphalt base
            gc.setStroke(Color.web("#2f3542")); // asphalt gray
            gc.setLineWidth(road.getWidth());
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.setLineJoin(StrokeLineJoin.ROUND);
            gc.strokeLine(x1, y1, x2, y2);

            // 2. Draw outer shoulder lines (yellow borders)
            double dx = x2 - x1;
            double dy = y2 - y1;
            double length = Math.sqrt(dx * dx + dy * dy);
            if (length == 0) continue;

            double ux = dx / length;
            double uy = dy / length;
            double px = -uy;
            double py = ux;

            double halfWidth = road.getWidth() / 2.0;

            // Draw left yellow border
            gc.setStroke(Color.web("#eccc68", 0.7)); // solid yellow
            gc.setLineWidth(2.0);
            gc.strokeLine(
                x1 + px * halfWidth, y1 + py * halfWidth,
                x2 + px * halfWidth, y2 + py * halfWidth
            );

            // Draw right yellow border
            gc.strokeLine(
                x1 - px * halfWidth, y1 - py * halfWidth,
                x2 - px * halfWidth, y2 - py * halfWidth
            );

            // 3. Draw lane separators (dashed white lines)
            int numLanes = road.getLaneCount();
            if (numLanes > 1) {
                gc.setStroke(Color.web("#ffffff", 0.4));
                gc.setLineWidth(1.5);
                gc.setLineDashes(15.0, 15.0); // Dash pattern: 15px line, 15px gap
                
                double laneWidth = road.getWidth() / numLanes;
                for (int i = 1; i < numLanes; i++) {
                    double offset = (i - numLanes / 2.0) * laneWidth;
                    gc.strokeLine(
                        x1 + px * offset, y1 + py * offset,
                        x2 + px * offset, y2 + py * offset
                    );
                }
                gc.setLineDashes(null); // Reset dashes
            }

            // 4. Draw small arrows showing lane directions
            gc.setFill(Color.web("#ffffff", 0.25));
            for (int i = 0; i < numLanes; i++) {
                Lane lane = road.getLaneByIndex(i);
                if (lane == null) continue;

                double laneWidth = road.getWidth() / numLanes;
                double offset = (i - (numLanes - 1) / 2.0) * laneWidth;

                // Position arrow 1/4 of the way down the road
                double arrowDist = length * 0.25;
                double ax = x1 + ux * arrowDist + px * offset;
                double ay = y1 + uy * arrowDist + py * offset;
                
                drawArrowHead(gc, ax, ay, ux, uy);
            }
        }
    }

    private void drawArrowHead(GraphicsContext gc, double cx, double cy, double ux, double uy) {
        double px = -uy;
        double py = ux;

        double size = 8.0;
        // Vertices of arrow pointing in direction of (ux, uy)
        double[] xPoints = {
            cx + ux * size,
            cx - ux * size/2.0 + px * size/2.0,
            cx - ux * size/2.0 - px * size/2.0
        };
        double[] yPoints = {
            cy + uy * size,
            cy - uy * size/2.0 + py * size/2.0,
            cy - uy * size/2.0 - py * size/2.0
        };
        gc.fillPolygon(xPoints, yPoints, 3);
    }

    private void drawJunctions(GraphicsContext gc, List<Junction> junctions) {
        for (Junction junction : junctions) {
            Location loc = junction.getLocation();
            double x = loc.getX();
            double y = loc.getY();

            // Draw central hub
            gc.setFill(Color.web("#34495e")); // darker gray-blue
            gc.setStroke(Color.web("#7f8c8d"));
            gc.setLineWidth(3.0);
            gc.fillOval(x - 45, y - 45, 90, 90);
            gc.strokeOval(x - 45, y - 45, 90, 90);

            // Inner styling circle
            gc.setFill(Color.web("#2c3e50"));
            gc.fillOval(x - 15, y - 15, 30, 30);

            // Draw Junction ID label
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            gc.fillText(junction.getId(), x - 7, y + 4);
        }
    }

    private void drawTrafficLights(GraphicsContext gc, List<Junction> junctions) {
        for (Junction junction : junctions) {
            Location loc = junction.getLocation();
            double jx = loc.getX();
            double jy = loc.getY();

            // Draw simulated traffic lights for each connected road
            for (Road road : junction.getConnectedRoads()) {
                Location start = road.getStartLocation();
                Location end = road.getEndLocation();

                // Compute road unit vector
                double dx = end.getX() - start.getX();
                double dy = end.getY() - start.getY();
                double len = Math.sqrt(dx * dx + dy * dy);
                if (len == 0) continue;

                double ux = dx / len;
                double uy = dy / len;
                double px = -uy;
                double py = ux;

                // Draw a traffic light box at the stop line (approx 55px away from junction center)
                double stopDist = 55.0;
                
                // Determine direction: is road ending or starting at the junction?
                boolean isEnteringJunction = Math.abs(end.getX() - jx) < 5.0 && Math.abs(end.getY() - jy) < 5.0;
                
                if (isEnteringJunction) {
                    // Stop line coordinates
                    double sx = jx - ux * stopDist;
                    double sy = jy - uy * stopDist;

                    // Place traffic light on the right shoulder of the entering road
                    double rightOffset = road.getWidth() / 2.0 + 12.0;
                    double tx = sx + px * rightOffset;
                    double ty = sy + py * rightOffset;

                    // Draw light post
                    gc.setStroke(Color.web("#7f8c8d"));
                    gc.setLineWidth(2.0);
                    gc.strokeLine(tx, ty, tx - px * 6, ty - py * 6);

                    // Draw black background casing
                    gc.setFill(Color.web("#2c3e50"));
                    gc.fillRoundRect(tx - 7, ty - 7, 14, 14, 4, 4);
                    gc.setStroke(Color.web("#95a5a6"));
                    gc.setLineWidth(1.0);
                    gc.strokeRoundRect(tx - 7, ty - 7, 14, 14, 4, 4);

                    // Get simulated traffic light state
                    Lane.Direction lightDir = getApproxDirection(ux, uy);
                    Color lightColor = getLightColor(lightDir);

                    // Draw colored lamp
                    gc.setFill(lightColor);
                    gc.fillOval(tx - 5, ty - 5, 10, 10);

                    // Draw glowing effect for red/green
                    gc.save();
                    gc.setGlobalAlpha(0.35);
                    gc.setFill(lightColor);
                    gc.fillOval(tx - 9, ty - 9, 18, 18);
                    gc.restore();
                }
            }
        }
    }

    private Lane.Direction getApproxDirection(double ux, double uy) {
        if (Math.abs(ux) > Math.abs(uy)) {
            return ux > 0 ? Lane.Direction.EAST : Lane.Direction.WEST;
        } else {
            return uy > 0 ? Lane.Direction.SOUTH : Lane.Direction.NORTH;
        }
    }

    private Color getLightColor(Lane.Direction dir) {
        long timeMs = System.currentTimeMillis();
        long cycleTime = (timeMs / 1000) % 12; // 12-second total cycle
        boolean isEastWest = (dir == Lane.Direction.EAST || dir == Lane.Direction.WEST);

        if (cycleTime < 5) {
            // East-West Green, North-South Red
            return isEastWest ? Color.LIME : Color.RED;
        } else if (cycleTime < 7) {
            // East-West Yellow, North-South Red
            return isEastWest ? Color.GOLD : Color.RED;
        } else {
            // East-West Red, North-South Green
            return isEastWest ? Color.RED : Color.LIME;
        }
    }

    public static boolean isLightRedOrYellowForLane(Lane lane, Junction junction) {
        if (junction == null) return false;

        long timeMs = System.currentTimeMillis();
        long cycleTime = (timeMs / 1000) % 12;
        Lane.Direction dir = lane.getDirection();
        boolean isEastWest = (dir == Lane.Direction.EAST || dir == Lane.Direction.WEST);

        if (cycleTime < 5) {
            // EW Green, NS Red
            return !isEastWest; // Red for NS
        } else if (cycleTime < 7) {
            // EW Yellow (true), NS Red (true)
            return true; // Stop for both
        } else {
            // EW Red, NS Green
            return isEastWest; // Red for EW
        }
    }
}