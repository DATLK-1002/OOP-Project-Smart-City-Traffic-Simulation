package observer;

import model.trafficlight.TrafficLight;

public interface TrafficObserver {

    void update(TrafficLight light);

}