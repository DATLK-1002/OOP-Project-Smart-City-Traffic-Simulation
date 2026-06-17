package view.component;

import model.road.Road;

public class RoadView {

    private Road road;

    public RoadView(Road road) {

        this.road = road;
    }

    public void draw() {

        System.out.println(
                "Draw Road: "
                + road.getName()
        );
    }
}