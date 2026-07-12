package jraffic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;

/**
 * Owns all mutable simulation state and applies movement, signal, and spawn rules.
 * The class contains no Swing code, allowing deterministic headless tests.
 */
public final class Simulation {
    public static final double LANE_LENGTH = 330.0;
    public static final double SAFETY_GAP = 16.0;
    public static final int LANE_CAPACITY = capacityFor(LANE_LENGTH, Vehicle.LENGTH, SAFETY_GAP);

    private final List<Vehicle> vehicles = new ArrayList<>();
    private final TrafficController controller = new TrafficController();
    private final Random random;
    private long nextVehicleId = 1;
    private String statusMessage = "Use arrow keys or R to add traffic";
    private double statusSeconds;

    /** Creates a simulation using nondeterministic random route selection. */
    public Simulation() {
        this(new Random());
    }

    /** Creates a simulation with an injectable random source for repeatable tests. */
    Simulation(Random random) {
        this.random = random;
    }

    /**
     * Advances the controller and every vehicle by one bounded time step.
     * Bounding avoids large jumps that could pass through stop lines after a pause.
     */
    public void update(double deltaSeconds) {
        double boundedDelta = Math.min(0.05, Math.max(0, deltaSeconds));
        EnumMap<Direction, Integer> queues = queueCounts();
        controller.update(boundedDelta, queues, LANE_CAPACITY, isIntersectionOccupied());

        List<Vehicle> ordered = new ArrayList<>(vehicles);
        ordered.sort(Comparator.comparingDouble(Vehicle::progress).reversed());
        for (Vehicle vehicle : ordered) {
            double movement = Vehicle.SPEED * boundedDelta;
            movement = limitForTrafficLight(vehicle, movement);
            movement = limitForLeader(vehicle, movement);
            vehicle.move(movement);
        }
        vehicles.removeIf(Vehicle::isFinished);

        if (statusSeconds > 0) {
            statusSeconds -= boundedDelta;
            if (statusSeconds <= 0) {
                statusMessage = "Use arrow keys or R to add traffic";
            }
        }
    }

    /** Attempts to spawn a vehicle with a random route on the requested approach. */
    public boolean spawn(Direction direction) {
        Route[] routes = Route.values();
        return spawn(direction, routes[random.nextInt(routes.length)]);
    }

    /**
     * Attempts to spawn a specific route, rejecting full lanes and unsafe spawn gaps.
     */
    boolean spawn(Direction direction, Route route) {
        int laneVehicles = queueCount(direction);
        if (laneVehicles >= LANE_CAPACITY) {
            setStatus(direction.label() + " lane is at capacity");
            return false;
        }

        double nearestProgress = vehicles.stream()
                .filter(vehicle -> vehicle.direction() == direction)
                .filter(vehicle -> !vehicle.hasEnteredIntersection())
                .mapToDouble(Vehicle::progress)
                .min()
                .orElse(Double.POSITIVE_INFINITY);
        if (nearestProgress < Vehicle.LENGTH + SAFETY_GAP) {
            setStatus("Safe spawn gap not available on " + direction.label());
            return false;
        }

        vehicles.add(new Vehicle(nextVehicleId++, direction, route));
        setStatus(route.label() + " vehicle added from " + direction.label());
        return true;
    }

    /**
     * Tries each approach from a random starting point until one accepts a vehicle.
     */
    public boolean spawnRandomDirection() {
        Direction[] directions = Direction.values();
        int start = random.nextInt(directions.length);
        for (int offset = 0; offset < directions.length; offset++) {
            if (spawn(directions[(start + offset) % directions.length])) {
                return true;
            }
        }
        setStatus("No direction currently has a safe spawn gap");
        return false;
    }

    /** Clamps movement at the stop line unless this approach currently has green. */
    private double limitForTrafficLight(Vehicle vehicle, double movement) {
        if (vehicle.hasEnteredIntersection() || controller.isGreen(vehicle.direction())) {
            return movement;
        }
        double remaining = vehicle.stopProgress() - vehicle.progress();
        return Math.max(0, Math.min(movement, remaining));
    }

    /** Clamps movement to preserve vehicle length plus the configured safety gap. */
    private double limitForLeader(Vehicle vehicle, double movement) {
        if (vehicle.hasEnteredIntersection()) {
            return movement;
        }
        Vehicle leader = vehicles.stream()
                .filter(other -> other != vehicle)
                .filter(other -> other.direction() == vehicle.direction())
                .filter(other -> !other.hasEnteredIntersection())
                .filter(other -> other.progress() > vehicle.progress())
                .min(Comparator.comparingDouble(Vehicle::progress))
                .orElse(null);
        if (leader == null) {
            return movement;
        }
        double available = leader.progress() - vehicle.progress() - Vehicle.LENGTH - SAFETY_GAP;
        return Math.max(0, Math.min(movement, available));
    }

    /** Reports whether a vehicle must clear before another approach receives green. */
    private boolean isIntersectionOccupied() {
        return vehicles.stream().anyMatch(Vehicle::isInsideIntersection);
    }

    /** Returns a fresh map containing the current incoming queue for every approach. */
    public EnumMap<Direction, Integer> queueCounts() {
        EnumMap<Direction, Integer> counts = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            counts.put(direction, queueCount(direction));
        }
        return counts;
    }

    /** Counts vehicles on one incoming lane that have not crossed the stop line. */
    public int queueCount(Direction direction) {
        return (int) vehicles.stream()
                .filter(vehicle -> vehicle.direction() == direction)
                .filter(vehicle -> !vehicle.hasEnteredIntersection())
                .count();
    }

    /** Returns an immutable snapshot of vehicles for rendering and inspection. */
    public List<Vehicle> vehicles() {
        return List.copyOf(vehicles);
    }

    /** Returns the signal controller used by the dashboard and traffic lights. */
    public TrafficController controller() {
        return controller;
    }

    /** Returns the short user-facing result of the most recent spawn action. */
    public String statusMessage() {
        return statusMessage;
    }

    /** Displays a temporary status message before restoring the control hint. */
    private void setStatus(String message) {
        statusMessage = message;
        statusSeconds = 2.5;
    }

    /**
     * Calculates physical queue capacity using floor(lane / (vehicle + gap)).
     *
     * @throws IllegalArgumentException when dimensions cannot describe a valid lane
     */
    public static int capacityFor(double laneLength, double vehicleLength, double safetyGap) {
        if (laneLength <= 0 || vehicleLength <= 0 || safetyGap < 0) {
            throw new IllegalArgumentException("Lengths must be positive and gap cannot be negative");
        }
        return (int) Math.floor(laneLength / (vehicleLength + safetyGap));
    }
}
