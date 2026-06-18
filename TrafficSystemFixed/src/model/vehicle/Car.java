package model.vehicle;

import util.Direction;
import util.Vector2D;

public class Car extends Vehicle {

    public Car(String id,
               Vector2D position,
               Direction direction) {

        super(id,
              position,
              6.6,
              0.34,
              direction);
    }

    @Override
    public void update() {

        if(strategy != null)
            strategy.drive(this);

        move();
    }
}