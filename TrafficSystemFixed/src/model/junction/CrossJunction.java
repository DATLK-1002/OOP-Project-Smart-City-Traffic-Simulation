package model.junction;

import util.Vector2D;

public class CrossJunction extends Junction {

    public CrossJunction(String id,
                         Vector2D position) {

        super(id, position);
    }

    @Override
    public int getMaxRoadCount() {

        return 4;
    }
}