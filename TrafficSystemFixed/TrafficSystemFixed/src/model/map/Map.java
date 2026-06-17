package model.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.junction.Junction;
import model.road.Road;

public class Map {

    private String name;

    private List<Road> roads;

    private List<Junction> junctions;

    private List<SpawnPoint> spawnPoints;

    public Map(String name) {

        this.name = name;

        roads = new ArrayList<>();

        junctions = new ArrayList<>();

        spawnPoints = new ArrayList<>();
    }

    public void addRoad(Road road) {

        roads.add(road);
    }

    public void addJunction(
            Junction junction) {

        junctions.add(junction);
    }

    public void addSpawnPoint(
            SpawnPoint spawnPoint) {

        spawnPoints.add(spawnPoint);
    }

    public List<Road> getRoads() {

        return Collections.unmodifiableList(roads);
    }

    public List<Junction> getJunctions() {

        return Collections.unmodifiableList(junctions);
    }

    public List<SpawnPoint> getSpawnPoints() {

        return Collections.unmodifiableList(spawnPoints);
    }

    public String getName() {

        return name;
    }
}