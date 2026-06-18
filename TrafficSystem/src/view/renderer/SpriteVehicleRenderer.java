package view.renderer;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import view.renderstate.VehicleRenderState;
import view.renderstate.VehicleRenderType;

/**
 * Renderer vẽ xe bằng PNG sprite.
 * - Chỉ nhận VehicleRenderState đã được chuẩn bị sẵn.
 * - Không chứa logic xử lý tọa độ mô phỏng.
 * - Kích thước khi bật đồ họa được giữ đúng bằng kích thước ở chế độ BASIC.
 */
public class SpriteVehicleRenderer implements VehicleGraphicsRenderer {

    private static final String RESOURCE_ROOT = "/assets/images/";

    private final Map<VehicleRenderType, Image> spriteCache = new EnumMap<>(VehicleRenderType.class);
    private final BasicVehicleRenderer fallbackRenderer = new BasicVehicleRenderer();

    @Override
    public void render(GraphicsContext graphics, VehicleRenderState state) {
        Image sprite = spriteFor(state.getVehicleType());
        if (sprite == null || sprite.isError()) {
            fallbackRenderer.render(graphics, state);
            return;
        }

        // Giữ nguyên kích thước hiển thị như lúc chưa bật đồ họa.
        double w = state.getBodyLength();
        double h = state.getBodyWidth();

        graphics.save();
        graphics.translate(state.getX(), state.getY());
        graphics.rotate(state.getHeadingDegrees());
        graphics.drawImage(sprite, -w / 2, -h / 2, w, h);

        if (state.shouldFlashEmergencyLight()) {
            drawEmergencyLightBar(graphics, state, w, h);
        }

        drawIndicators(graphics, state, w, h);
        graphics.restore();
    }

    private void drawEmergencyLightBar(GraphicsContext graphics, VehicleRenderState state, double w, double h) {
        // Đèn ưu tiên lắp dọc: 2 module đỏ/xanh xếp theo trục dọc của thân xe,
        // thay vì đặt ngang cạnh nhau như trước.
        double barW = Math.max(3.4, 3.4 * state.getScale());
        double barH = Math.max(6.8, 6.8 * state.getScale());
        double centerX = Math.min(w * 0.12, 4.2 * state.getScale());
        double centerY = -h * 0.18;
        double gap = Math.max(1.2, 1.1 * state.getScale());

        graphics.setFill(Color.RED);
        graphics.fillRoundRect(centerX - barW / 2,
                centerY - barH - gap / 2,
                barW,
                barH,
                3,
                3);

        graphics.setFill(Color.DODGERBLUE);
        graphics.fillRoundRect(centerX - barW / 2,
                centerY + gap / 2,
                barW,
                barH,
                3,
                3);
    }

    private void drawIndicators(GraphicsContext graphics, VehicleRenderState state, double w, double h) {
        if (!state.shouldBlinkIndicator()) return;
        if (!state.isLeftIndicatorOn() && !state.isRightIndicatorOn()) return;

        double radius = Math.max(2.0, 2.3 * state.getScale());
        double frontX = w * 0.36;
        double rearX = -w * 0.36;
        double sideY = h * 0.34;
        graphics.setFill(Color.web("#ffb000"));

        if (state.isLeftIndicatorOn()) {
            graphics.fillOval(frontX - radius, -sideY - radius, radius * 2, radius * 2);
            graphics.fillOval(rearX - radius, -sideY - radius, radius * 2, radius * 2);
        }
        if (state.isRightIndicatorOn()) {
            graphics.fillOval(frontX - radius, sideY - radius, radius * 2, radius * 2);
            graphics.fillOval(rearX - radius, sideY - radius, radius * 2, radius * 2);
        }
    }

    private Image spriteFor(VehicleRenderType type) {
        return spriteCache.computeIfAbsent(type, this::loadSprite);
    }

    private Image loadSprite(VehicleRenderType type) {
        String fileName = fileNameFor(type);

        try (InputStream resourceStream = SpriteVehicleRenderer.class.getResourceAsStream(RESOURCE_ROOT + fileName)) {
            if (resourceStream != null) {
                return new Image(resourceStream);
            }
        } catch (Exception ignored) {
            // thử fallback bên dưới
        }

        try {
            Path filePath = Path.of("src", "assets", "images", fileName);
            if (Files.exists(filePath)) {
                try (InputStream fileStream = Files.newInputStream(filePath)) {
                    return new Image(fileStream);
                }
            }
        } catch (Exception ignored) {
            // sẽ fallback sang chế độ basic
        }

        return null;
    }

    private String fileNameFor(VehicleRenderType type) {
        switch (type) {
            case BUS:
                return "bus.png";
            case AMBULANCE:
                return "ambulance.png";
            case FIRE_TRUCK:
                return "firetruck.png";
            case MOTORBIKE:
                return "motorbike.png";
            case BICYCLE:
                return "bicycle.png";
            case CAR:
            default:
                return "car.png";
        }
    }
}
