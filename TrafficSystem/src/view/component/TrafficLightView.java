package view.component;

import model.trafficlight.TrafficLight;

public class TrafficLightView {

    private TrafficLight trafficLight;

    public TrafficLightView(TrafficLight trafficLight) {

        this.trafficLight = trafficLight;
    }

    public void render() {

        System.out.println("TrafficLight [" + trafficLight.getId()
                + "]: " + trafficLight.getCurrentState());
    }

    public TrafficLight getTrafficLight() {

        return trafficLight;
    }
}
