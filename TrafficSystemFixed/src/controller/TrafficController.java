package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.trafficlight.TrafficLight;
import util.SoundManager;

public class TrafficController {

    private final List<TrafficLight> trafficLights;
    private static final int GREEN_TIME = 50;
    private static final int YELLOW_TIME = 10;

    private final SoundManager soundManager;
    private LightControlMode controlMode;
    private boolean eastWestPhase;
    private int phaseTimer;

    public TrafficController() {
        trafficLights = new ArrayList<>();
        soundManager = new SoundManager();
        controlMode = LightControlMode.AUTOMATIC;
        eastWestPhase = true;
        phaseTimer = GREEN_TIME;
    }

    
    public void addTrafficLight(TrafficLight light) {
        if (light != null) {
            trafficLights.add(light);
            synchronizeAutomaticLights(false);
        }
    }

    public void removeTrafficLight(TrafficLight light) {
        trafficLights.remove(light);
    }

    public List<TrafficLight> getTrafficLights() {
        return Collections.unmodifiableList(trafficLights);
    }

    public void updateLights() {
        if (controlMode == LightControlMode.MANUAL) {
            return;
        }

        phaseTimer--;
        if (phaseTimer <= 0) {
            eastWestPhase = !eastWestPhase;
            phaseTimer = GREEN_TIME;
        }

        synchronizeAutomaticLights(true);
    }

    public void switchLight(TrafficLight light) {
        if (light != null) {
            light.switchState();
            soundManager.playTrafficLightChange();
        }
    }

    private void synchronizeAutomaticLights(boolean playSoundOnChange) {
        if (trafficLights.isEmpty()) {
            return;
        }

        boolean changed = false;
        for (int i = 0; i < trafficLights.size(); i++) {
            TrafficLight light = trafficLights.get(i);
            TrafficLight.LightState nextState = automaticStateForIndex(i);
            if (light.getCurrentState() != nextState) {
                light.setState(nextState);
                changed = true;
            }
            light.setTimer(phaseTimer);
        }

        if (changed && playSoundOnChange) {
            soundManager.playTrafficLightChange();
        }
    }

    private TrafficLight.LightState automaticStateForIndex(int index) {
        boolean eastWestLight = index == 0 || index == 2;
        boolean activePair = eastWestPhase == eastWestLight;
        if (!activePair) {
            return TrafficLight.LightState.RED;
        }
        return phaseTimer <= YELLOW_TIME ? TrafficLight.LightState.YELLOW : TrafficLight.LightState.GREEN;
    }

    public LightControlMode getControlMode() {
        return controlMode;
    }

    public void setControlMode(LightControlMode controlMode) {
        this.controlMode = controlMode == null ? LightControlMode.AUTOMATIC : controlMode;
    }
}