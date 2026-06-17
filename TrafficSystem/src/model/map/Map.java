package model.map;

import java.util.ArrayList;
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

        return roads;
    }

    public List<Junction> getJunctions() {

        return junctions;
    }

    public List<SpawnPoint> getSpawnPoints() {

        return spawnPoints;
    }

    public String getName() {

        return name;
    }
}