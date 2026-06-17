package model.trafficlight;

import java.util.ArrayList;
import java.util.List;

import observer.TrafficObserver;
import observer.TrafficSubject;

public abstract class TrafficLight implements TrafficSubject {

    public enum LightState {
        RED,
        YELLOW,
        GREEN
    }

    protected String id;

    protected LightState currentState;

    protected int timer;

    protected List<TrafficObserver> observers;

    public TrafficLight(String id) {

        this.id = id;

        this.currentState = LightState.RED;

        this.timer = 0;

        this.observers = new ArrayList<>();
    }

    public abstract void update();

    public void switchState() {

        switch(currentState) {

            case RED:
                currentState = LightState.GREEN;
                break;

            case GREEN:
                currentState = LightState.YELLOW;
                break;

            case YELLOW:
                currentState = LightState.RED;
                break;
        }

        notifyObservers();
    }

    @Override
    public void addObserver(
            TrafficObserver observer) {

        if(observer != null &&
                !observers.contains(observer)) {

            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(
            TrafficObserver observer) {

        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {

        for(TrafficObserver observer
                : observers) {

            observer.update(this);
        }
    }

    public String getId() {
        return id;
    }

    public LightState getCurrentState() {
        return currentState;
    }

    public boolean isRed() {
        return currentState ==
                LightState.RED;
    }

    public boolean isGreen() {
        return currentState ==
                LightState.GREEN;
    }

    public boolean isYellow() {
        return currentState ==
                LightState.YELLOW;
    }

    public int getTimer() {
        return timer;
    }
}