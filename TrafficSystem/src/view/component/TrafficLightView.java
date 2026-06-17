package view.component;

import model.trafficlight.TrafficLight;

public class TrafficLightView {

    private TrafficLight trafficLight;

    public TrafficLightView(
            TrafficLight trafficLight) {

        this.trafficLight = trafficLight;
    }

    public void draw() {

        System.out.println(
                "Traffic Light: "
                + trafficLight.getCurrentState()
        );
    }
}