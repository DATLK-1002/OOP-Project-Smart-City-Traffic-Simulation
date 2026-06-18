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

    // Angle used only by the JavaFX renderer. It lets a vehicle rotate gradually while turning.
    protected double visualHeadingDegrees;

    // Indicator state belongs to the model; JavaFX only reads and renders it.
    protected boolean leftIndicatorOn;
    protected boolean rightIndicatorOn;

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
        this.visualHeadingDegrees = headingForDirection(direction);
        this.leftIndicatorOn = false;
        this.rightIndicatorOn = false;
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

        if (speed > 0) {
            state = VehicleState.MOVING;
        }
    }

    public void brake() {
        speed = Math.max(0, speed - acceleration);

        if (speed == 0) {
            state = VehicleState.STOPPED;
        }
    }

    public void stop() {
        speed = 0;
        state = VehicleState.STOPPED;
    }

    public void nudgeAside(double distance) {
        switch (direction) {
            case NORTH:
            case SOUTH:
                position.setX(position.getX() + distance);
                break;
            case EAST:
            case WEST:
                position.setY(position.getY() + distance);
                break;
        }
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

    public void setSpeed(double speed) {
        this.speed = Math.max(0, Math.min(maxSpeed, speed));
        this.state = this.speed > 0 ? VehicleState.MOVING : VehicleState.STOPPED;
    }

    public double getMaxSpeed() {
        return maxSpeed;
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
    this.visualHeadingDegrees = headingForDirection(direction);
    }
    public void setPosition(Vector2D position) {
    this.position = position;
    }

    public double getVisualHeadingDegrees() {
        return visualHeadingDegrees;
    }

    public void setVisualHeadingDegrees(double visualHeadingDegrees) {
        this.visualHeadingDegrees = visualHeadingDegrees;
    }

    public boolean isLeftIndicatorOn() {
        return leftIndicatorOn;
    }

    public boolean isRightIndicatorOn() {
        return rightIndicatorOn;
    }

    public void setLeftIndicatorOn(boolean leftIndicatorOn) {
        this.leftIndicatorOn = leftIndicatorOn;
    }

    public void setRightIndicatorOn(boolean rightIndicatorOn) {
        this.rightIndicatorOn = rightIndicatorOn;
    }

    public void clearIndicators() {
        leftIndicatorOn = false;
        rightIndicatorOn = false;
    }

    private double headingForDirection(Direction direction) {
        switch (direction) {
            case SOUTH:
                return 90;
            case WEST:
                return 180;
            case NORTH:
                return 270;
            case EAST:
            default:
                return 0;
        }
    }
}