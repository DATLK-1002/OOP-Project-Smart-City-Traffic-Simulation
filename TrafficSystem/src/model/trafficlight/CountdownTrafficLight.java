package model.trafficlight;


public class CountdownTrafficLight
        extends TrafficLight {

    private int countdown;

    private static final int CHANGE_TIME = 50;

    public CountdownTrafficLight(
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

    public int getCountdown() {

        return countdown;
    }
}