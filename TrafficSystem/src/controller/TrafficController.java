package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.map.TrafficMapType;
import model.trafficlight.TrafficLight;
import util.SoundManager;

public class TrafficController {

    private final List<TrafficLight> trafficLights;
    private static final int GREEN_TIME = 50;
    private static final int YELLOW_TIME = 10;

    private final SoundManager soundManager;
    private LightControlMode controlMode;
    private final List<int[]> phaseGroups;
    private int phaseIndex;
    private int phaseTimer;

    public TrafficController() {
        trafficLights = new ArrayList<>();
        soundManager = new SoundManager();
        controlMode = LightControlMode.AUTOMATIC;
        phaseGroups = new ArrayList<>();
        phaseIndex = 0;
        phaseTimer = GREEN_TIME;
        configureForMap(TrafficMapType.CROSS_JUNCTION);
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

    public void configureForMap(TrafficMapType mapType) {
        TrafficMapType type = mapType == null ? TrafficMapType.CROSS_JUNCTION : mapType;
        phaseGroups.clear();
        switch (type) {
            case T_JUNCTION:
                // Hai hướng đối diện Đông/Tây đi cùng pha, hướng Nam dùng pha riêng.
                phaseGroups.add(new int[]{0, 1});
                phaseGroups.add(new int[]{2});
                break;
            case ROAD_NETWORK:
                // Chẵn: các tuyến ngang; lẻ: các tuyến dọc.
                phaseGroups.add(new int[]{0, 2, 4, 6});
                phaseGroups.add(new int[]{1, 3, 5, 7});
                break;
            case CROSS_JUNCTION:
            default:
                phaseGroups.add(new int[]{0, 2});
                phaseGroups.add(new int[]{1, 3});
                break;
        }
        phaseIndex = 0;
        phaseTimer = GREEN_TIME;
        synchronizeAutomaticLights(false);
    }

    public void updateLights() {
        if (controlMode == LightControlMode.MANUAL) {
            return;
        }

        phaseTimer--;
        if (phaseTimer <= 0) {
            phaseIndex = (phaseIndex + 1) % Math.max(1, phaseGroups.size());
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
        boolean active = isIndexInActiveGroup(index);
        if (!active) {
            return TrafficLight.LightState.RED;
        }
        return phaseTimer <= YELLOW_TIME
                ? TrafficLight.LightState.YELLOW
                : TrafficLight.LightState.GREEN;
    }

    private boolean isIndexInActiveGroup(int index) {
        if (phaseGroups.isEmpty()) return true;
        int[] activeGroup = phaseGroups.get(Math.floorMod(phaseIndex, phaseGroups.size()));
        for (int value : activeGroup) {
            if (value == index) return true;
        }
        return false;
    }

    public LightControlMode getControlMode() {
        return controlMode;
    }

    public void setControlMode(LightControlMode controlMode) {
        this.controlMode = controlMode == null ? LightControlMode.AUTOMATIC : controlMode;
        if (this.controlMode == LightControlMode.AUTOMATIC) {
            synchronizeAutomaticLights(false);
        }
    }
}
