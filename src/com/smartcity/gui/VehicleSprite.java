package com.smartcity.gui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class VehicleSprite {

    private Image image;

    private double x;
    private double y;

    private double angle;

    public VehicleSprite(Image image) {
        this.image = image;
    }

    public void render(
            GraphicsContext gc) {

        gc.save();

        gc.translate(x, y);

        gc.rotate(angle);

        gc.drawImage(
                image,
                -20,
                -10,
                40,
                20);

        gc.restore();
    }
}