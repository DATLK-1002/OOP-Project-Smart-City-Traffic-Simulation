package model.vehicle;

import util.Direction;
import util.Vector2D;

public class Ambulance extends Vehicle {

    private boolean sirenOn;

    public Ambulance(String id,
                     Vector2D position,
                     Direction direction) {

        super(id,
              position,
              8,
              0.4,
              direction);

        sirenOn = true;
    }

    public boolean isSirenOn() {
        return sirenOn;
    }

    @Override
    public void update() {

        if(strategy != null)
            strategy.drive(this);

        move();
    }
}