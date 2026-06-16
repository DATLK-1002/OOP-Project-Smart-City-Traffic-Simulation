package com.smartcity.gui;

import javafx.animation.AnimationTimer;

public class AnimationEngine {
    
    public interface TickListener {
        void onTick(double deltaTime);
    }

    private AnimationTimer timer;
    private TickListener listener;
    private long lastTime = 0;
    private double speedMultiplier = 1.0;

    public AnimationEngine(TickListener listener) {
        this.listener = listener;
        this.timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }
                double deltaTime = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;

                // Cap deltaTime to avoid massive teleportation when lagging or window changes focus
                if (deltaTime > 0.1) {
                    deltaTime = 0.1;
                }

                if (listener != null) {
                    listener.onTick(deltaTime * speedMultiplier);
                }
            }
        };
    }

    public void setSpeedMultiplier(double multiplier) {
        this.speedMultiplier = Math.max(0.1, multiplier);
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public void start() {
        lastTime = 0;
        timer.start();
    }

    public void stop() {
        timer.stop();
    }
}