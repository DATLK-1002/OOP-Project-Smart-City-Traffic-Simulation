package model.vehicle;

import util.Direction;
import util.Vector2D;

public class FireTruck extends Vehicle {

    public FireTruck(String id,
                     Vector2D position,
                     Direction direction) {

        super(id,
              position,
              8.6,
              0.46,
              direction);
    }

    @Override
    public void update() {

        if(strategy != null)
            strategy.drive(this);

        move();
    }
}