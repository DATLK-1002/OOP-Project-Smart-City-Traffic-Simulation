package com.smartcity.gui;

import javafx.animation.AnimationTimer;

public class AnimationEngine {

    private AnimationTimer timer;

    public AnimationEngine() {

        timer = new AnimationTimer() {

            @Override
            public void handle(
                    long now) {

                update();
            }
        };
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    private void update() {

        // update simulation

        // redraw screen
    }
}