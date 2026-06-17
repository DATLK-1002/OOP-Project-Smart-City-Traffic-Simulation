package controller;

import java.util.ArrayList;
import java.util.List;

import model.TrafficLight;

public class TrafficController {

    private final List<TrafficLight> trafficLights;

    public TrafficController() {
        trafficLights = new ArrayList<>();
    }

    public void addTrafficLight(
            TrafficLight light) {

        if(light != null) {
            trafficLights.add(light);
        }
    }

    public void removeTrafficLight(
            TrafficLight light) {

        trafficLights.remove(light);
    }

    public List<TrafficLight> getTrafficLights() {
        return trafficLights;
    }

    public void updateLights() {

        for(TrafficLight light : trafficLights) {
            light.update();
        }
    }
}