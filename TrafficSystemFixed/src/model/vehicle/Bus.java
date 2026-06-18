package model.vehicle;

import util.Direction;
import util.Vector2D;

public class Bus extends Vehicle {

    public Bus(String id,
               Vector2D position,
               Direction direction) {

        super(id,
              position,
              5.9,
              0.30,
              direction);
    }

    @Override
    public void update() {

        if(strategy != null)
            strategy.drive(this);

        move();
    }
}
