package util;

import model.vehicle.Ambulance;
import model.vehicle.Bicycle;
import model.vehicle.FireTruck;
import model.vehicle.Motorbike;
import model.vehicle.Vehicle;

public class SoundManager {

    private boolean enabled;

    public SoundManager() {

        enabled = true;
    }

    public void playHorn() {

        play("horn");
    }

    public void playSiren() {

        play("siren");
    }

    public void playTrafficLightChange() {

        play("traffic-light-change");
    }

    public void playTurnSignal() {

        play("turn-signal");
    }

    public void playVehicleSound(Vehicle vehicle) {

        if (vehicle == null || vehicle.getSpeed() <= 0) {
            return;
        }

        if (vehicle instanceof Ambulance || vehicle instanceof FireTruck) {
            playSiren();
        } else if (vehicle instanceof Motorbike) {
            play("motorbike-engine");
        } else if (vehicle instanceof Bicycle) {
            play("bicycle-bell");
        } else {
            play("car-engine");
        }
    }


    public void play(String soundName) {

        if (enabled && soundName != null && !soundName.trim().isEmpty()) {
            System.out.println("[Sound] " + soundName);
        }
    }

    public void mute() {

        enabled = false;
    }

    public void unmute() {

        enabled = true;
    }

    public boolean isEnabled() {

        return enabled;
    }
}