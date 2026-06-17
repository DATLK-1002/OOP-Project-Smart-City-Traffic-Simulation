package model.vehicle;


import strategy.DrivingStrategy;
import util.Direction;
import util.Vector2D;

public abstract class Vehicle {

    protected String id;
    protected Vector2D position;

    protected double speed;
    protected double maxSpeed;
    protected double acceleration;

    protected Direction direction;
    protected VehicleState state;

    protected DrivingStrategy strategy;

    public Vehicle(String id,
                   Vector2D position,
                   double maxSpeed,
                   double acceleration,
                   Direction direction) {

        this.id = id;
        this.position = position;
        this.maxSpeed = maxSpeed;
        this.acceleration = acceleration;
        this.direction = direction;

        this.speed = 0;
        this.state = VehicleState.STOPPED;
    }

    public void move() {

        switch (direction) {

            case NORTH:
                position.setY(position.getY() - speed);
                break;

            case SOUTH:
                position.setY(position.getY() + speed);
                break;

            case EAST:
                position.setX(position.getX() + speed);
                break;

            case WEST:
                position.setX(position.getX() - speed);
                break;
        }
    }

    public void accelerate() {
        speed = Math.min(maxSpeed, speed + acceleration);
    }

    public void brake() {
        speed = Math.max(0, speed - acceleration);
    }

    public void stop() {
        speed = 0;
        state = VehicleState.STOPPED;
    }

    public abstract void update();

    public String getId() {
        return id;
    }

    public Vector2D getPosition() {
        return position;
    }

    public double getSpeed() {
        return speed;
    }

    public Direction getDirection() {
        return direction;
    }

    public VehicleState getState() {
        return state;
    }

    public void setStrategy(DrivingStrategy strategy) {
        this.strategy = strategy;
    }
    public void setState(VehicleState state) {
    this.state = state;
    }
    public void setDirection(Direction direction) {
    this.direction = direction;
    }
    public void setPosition(Vector2D position) {
    this.position = position;
    }
}