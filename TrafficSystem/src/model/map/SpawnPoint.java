package model.map;

import util.Direction;
import util.Vector2D;

public class SpawnPoint {

    private String id;

    private Vector2D position;

    private Direction direction;

    public SpawnPoint(String id,
                      Vector2D position,
                      Direction direction) {

        this.id = id;
        this.position = position;
        this.direction = direction;
    }

    public String getId() {
        return id;
    }

    public Vector2D getPosition() {
        return position;
    }

    public Direction getDirection() {
        return direction;
    }
}