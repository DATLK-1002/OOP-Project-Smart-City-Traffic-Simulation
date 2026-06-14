package com.smartcity.core;

import java.util.ArrayList;
import java.util.List;

public class SimulationModel {

    private List<Object> vehicles;

    private List<Object> trafficLights;

    private Object roadNetwork;

    public SimulationModel() {

        vehicles = new ArrayList<>();

        trafficLights = new ArrayList<>();
    }

    public void addVehicle(Object vehicle) {
        vehicles.add(vehicle);
    }

    public void removeVehicle(Object vehicle) {
        vehicles.remove(vehicle);
    }

    public void addTrafficLight(Object trafficLight) {
        trafficLights.add(trafficLight);
    }

    public void removeTrafficLight(Object trafficLight) {
        trafficLights.remove(trafficLight);
    }

    public List<Object> getVehicles() {
        return vehicles;
    }

    public List<Object> getTrafficLights() {
        return trafficLights;
    }

    public Object getRoadNetwork() {
        return roadNetwork;
    }

    public void setRoadNetwork(Object roadNetwork) {
        this.roadNetwork = roadNetwork;
    }
}