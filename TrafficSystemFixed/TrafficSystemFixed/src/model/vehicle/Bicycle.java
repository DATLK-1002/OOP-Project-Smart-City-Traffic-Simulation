package model.vehicle;

import util.Direction;
import util.Vector2D;

public class Bicycle extends Vehicle {

    public Bicycle(String id,
                   Vector2D position,
                   Direction direction) {

        super(id,
              position,
              2,
              0.1,
              direction);
    }

    @Override
    public void update() {

        if(strategy != null)
            strategy.drive(this);

        move();
    }
}