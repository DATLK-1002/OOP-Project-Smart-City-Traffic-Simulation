package model.trafficlight;


public class BasicTrafficLight
        extends TrafficLight {

    private static final int CHANGE_TIME = 50;

    public BasicTrafficLight(
            String id) {

        super(id);

        timer = CHANGE_TIME;
    }

    @Override
    public void update() {

        timer--;

        if(timer <= 0) {

            switchState();

            timer = CHANGE_TIME;
        }
    }
}