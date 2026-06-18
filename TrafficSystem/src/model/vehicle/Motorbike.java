package model.vehicle;

import util.Direction;
import util.Vector2D;

public class Motorbike extends Vehicle {

    public Motorbike(String id,
                     Vector2D position,
                     Direction direction) {

        super(id,
              position,
              8.0,
              0.48,
              direction);
    }

    @Override
    public void update() {

        if(strategy != null)
            strategy.drive(this);

        move();
    }
}