package model.trafficlight;


public class SmartTrafficLight
        extends TrafficLight {

    private int countdown;

    private static final int CHANGE_TIME = 50;

    public SmartTrafficLight(
            String id) {

        super(id);

        countdown = CHANGE_TIME;

        timer = CHANGE_TIME;
    }

    @Override
    public void update() {

        countdown--;

        timer = countdown;

        if(countdown <= 0) {

            switchState();

            countdown = CHANGE_TIME;

            timer = countdown;
        }
    }

    public boolean showCountdown() {

        return countdown <= 10;
    }

    public int getCountdown() {

        return countdown;
    }
}