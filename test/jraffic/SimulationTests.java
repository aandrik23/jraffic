package jraffic;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;

/**
 * Dependency-free regression suite for traffic rules, controller behavior, and rendering.
 */
public final class SimulationTests {
    private int passed;

    /** Creates and executes the complete test suite. */
    public static void main(String[] args) {
        new SimulationTests().run();
    }

    /** Runs every test and prints the final number of successful assertions. */
    private void run() {
        testCapacityCalculation();
        testUnsafeRepeatedSpawnIsRejected();
        testVehicleStopsAtRedLight();
        testFollowingDistanceIsMaintained();
        testCapacityQueueExtendsGreen();
        testClearanceWaitsForIntersection();
        testWaitingLaneIsServed();
        testPanelRendersOffScreen();
        System.out.println("All " + passed + " tests passed.");
    }

    /** Verifies the required capacity formula and invalid-dimension handling. */
    private void testCapacityCalculation() {
        expect(Simulation.capacityFor(330, 28, 16) == 7,
                "capacity uses floor(lane / (vehicle + gap))");
        expectThrows(() -> Simulation.capacityFor(0, 28, 16),
                "invalid lane dimensions are rejected");
    }

    /** Verifies that repeated input cannot overlap vehicles at a spawn point. */
    private void testUnsafeRepeatedSpawnIsRejected() {
        Simulation simulation = new Simulation(new Random(1));
        expect(simulation.spawn(Direction.NORTH, Route.STRAIGHT), "first spawn succeeds");
        expect(!simulation.spawn(Direction.NORTH, Route.RIGHT), "spam spawn is rejected");
        expect(simulation.vehicles().size() == 1, "rejected spawn creates no vehicle");
    }

    /** Verifies that an approach without green cannot cross its stop line. */
    private void testVehicleStopsAtRedLight() {
        Simulation simulation = new Simulation(new Random(2));
        simulation.spawn(Direction.NORTH, Route.STRAIGHT);
        simulation.spawn(Direction.SOUTH, Route.STRAIGHT);
        advance(simulation, 4.3);

        Vehicle southVehicle = vehicleFrom(simulation.vehicles(), Direction.SOUTH);
        expect(southVehicle.progress() <= southVehicle.stopProgress() + 0.001,
                "vehicle cannot cross a red stop line");
    }

    /** Verifies that a following vehicle never enters the configured safety gap. */
    private void testFollowingDistanceIsMaintained() {
        Simulation simulation = new Simulation(new Random(3));
        simulation.spawn(Direction.NORTH, Route.STRAIGHT);
        advance(simulation, 1.0);
        expect(simulation.spawn(Direction.NORTH, Route.LEFT), "spawn succeeds after safe gap opens");
        advance(simulation, 2.0);

        List<Vehicle> northbound = simulation.vehicles().stream()
                .filter(vehicle -> vehicle.direction() == Direction.NORTH)
                .sorted((first, second) -> Double.compare(second.progress(), first.progress()))
                .toList();
        double gap = northbound.get(0).progress() - northbound.get(1).progress() - Vehicle.LENGTH;
        expect(gap + 0.001 >= Simulation.SAFETY_GAP, "following vehicle preserves the safety gap");
    }

    /** Verifies priority and extended green time for a capacity-level queue. */
    private void testCapacityQueueExtendsGreen() {
        TrafficController controller = new TrafficController();
        EnumMap<Direction, Integer> queues = emptyQueues();
        queues.put(Direction.EAST, Simulation.LANE_CAPACITY);
        advance(controller, queues, 1.1, false);
        expect(controller.isGreen(Direction.EAST), "capacity lane receives green priority");
        advance(controller, queues, 9.0, false);
        expect(controller.isGreen(Direction.EAST), "capacity lane green is dynamically extended");
        advance(controller, queues, 3.0, false);
        expect(controller.isClearing(), "extended green still observes a maximum duration");
    }

    /** Verifies that all lights remain red while a vehicle occupies the junction. */
    private void testClearanceWaitsForIntersection() {
        TrafficController controller = new TrafficController();
        EnumMap<Direction, Integer> queues = emptyQueues();
        queues.put(Direction.WEST, 1);
        advance(controller, queues, 1.1, false);
        expect(controller.isGreen(Direction.WEST), "queued lane gets a green signal");
        advance(controller, queues, 7.0, true);
        expect(controller.isClearing(), "controller enters all-red clearance");
        advance(controller, queues, 2.0, true);
        expect(controller.greenDirection() == null,
                "no new green starts while a vehicle occupies the intersection");
    }

    /** Verifies that accumulated waiting time prevents signal starvation. */
    private void testWaitingLaneIsServed() {
        TrafficController controller = new TrafficController();
        EnumMap<Direction, Integer> queues = emptyQueues();
        queues.put(Direction.NORTH, 1);
        queues.put(Direction.SOUTH, 1);
        advance(controller, queues, 1.1, false);
        expect(controller.isGreen(Direction.NORTH), "deterministic first tied lane is selected");
        advance(controller, queues, 7.0, false);
        advance(controller, queues, 1.1, false);
        expect(controller.isGreen(Direction.SOUTH), "longer-waiting lane is served next");
    }

    /** Verifies that painting the complete interface does not throw an exception. */
    private void testPanelRendersOffScreen() {
        Simulation simulation = new Simulation(new Random(4));
        simulation.spawn(Direction.WEST, Route.RIGHT);
        SimulationPanel panel = new SimulationPanel(simulation);
        panel.setSize(1180, 900);
        BufferedImage image = new BufferedImage(1180, 900, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        panel.paint(graphics);
        graphics.dispose();
        expect(image.getRGB(450, 450) != 0, "simulation panel renders the intersection and dashboard");
    }

    /** Creates a controller input map initialized with zero vehicles in every lane. */
    private static EnumMap<Direction, Integer> emptyQueues() {
        EnumMap<Direction, Integer> queues = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            queues.put(direction, 0);
        }
        return queues;
    }

    /** Finds the first test vehicle that originated from the requested direction. */
    private static Vehicle vehicleFrom(List<Vehicle> vehicles, Direction direction) {
        return vehicles.stream()
                .filter(vehicle -> vehicle.direction() == direction)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing vehicle from " + direction));
    }

    /** Advances a simulation in the same bounded increments used by normal updates. */
    private static void advance(Simulation simulation, double seconds) {
        int steps = (int) Math.ceil(seconds / 0.05);
        for (int step = 0; step < steps; step++) {
            simulation.update(0.05);
        }
    }

    /** Advances a controller directly with fixed queue and occupancy inputs. */
    private static void advance(TrafficController controller,
            EnumMap<Direction, Integer> queues, double seconds, boolean occupied) {
        int steps = (int) Math.ceil(seconds / 0.05);
        for (int step = 0; step < steps; step++) {
            controller.update(0.05, queues, Simulation.LANE_CAPACITY, occupied);
        }
    }

    /** Records a successful assertion or fails with its behavioral description. */
    private void expect(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError("FAILED: " + description);
        }
        passed++;
    }

    /** Records success only when the supplied invalid operation is rejected. */
    private void expectThrows(Runnable action, String description) {
        try {
            action.run();
            throw new AssertionError("FAILED: " + description);
        } catch (IllegalArgumentException expected) {
            passed++;
        }
    }
}
