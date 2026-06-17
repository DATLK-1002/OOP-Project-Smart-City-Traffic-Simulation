package model.junction;

import util.Vector2D;

public class FiveWayJunction extends Junction {

    public FiveWayJunction(String id,
                           Vector2D position) {

        super(id, position);
    }

    @Override
    public int getMaxRoadCount() {

        return 5;
    }
}