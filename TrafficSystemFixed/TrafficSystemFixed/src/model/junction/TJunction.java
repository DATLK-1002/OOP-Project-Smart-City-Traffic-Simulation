package model.junction;

import util.Vector2D;

public class TJunction extends Junction {

    public TJunction(String id,
                     Vector2D position) {

        super(id, position);
    }

    @Override
    public int getMaxRoadCount() {

        return 3;
    }
}