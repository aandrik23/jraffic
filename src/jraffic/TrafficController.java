package jraffic;

import java.util.EnumMap;
import java.util.Map;

/**
 * Selects one safe green approach using queue pressure and accumulated waiting time.
 * Signal changes pass through an all-red state until the intersection is empty.
 */
public final class TrafficController {
    static final double MIN_GREEN_SECONDS = 3.0;
    static final double BASE_GREEN_SECONDS = 5.0;
    static final double MAX_GREEN_SECONDS = 12.0;
    static final double CLEARANCE_SECONDS = 1.0;

    private final EnumMap<Direction, Double> waitingSeconds = new EnumMap<>(Direction.class);
    private Direction greenDirection;
    private double phaseSeconds;
    private boolean clearing = true;

    /** Creates a controller in its initial all-red clearance state. */
    TrafficController() {
        for (Direction direction : Direction.values()) {
            waitingSeconds.put(direction, 0.0);
        }
    }

    /**
     * Advances signal timing and changes phase when its safety and timing rules allow.
     * A fuller current queue produces a longer target green, up to the maximum.
     */
    void update(double deltaSeconds, Map<Direction, Integer> queues, int capacity,
            boolean intersectionOccupied) {
        updateWaitingTimes(deltaSeconds, queues);
        phaseSeconds += deltaSeconds;

        if (clearing) {
            if (phaseSeconds >= CLEARANCE_SECONDS && !intersectionOccupied) {
                greenDirection = selectNext(queues, capacity);
                if (greenDirection != null) {
                    clearing = false;
                    phaseSeconds = 0;
                    waitingSeconds.put(greenDirection, 0.0);
                }
            }
            return;
        }

        int currentQueue = queues.getOrDefault(greenDirection, 0);
        double congestion = Math.min(1.0, currentQueue / (double) capacity);
        double desiredGreen = BASE_GREEN_SECONDS
                + congestion * (MAX_GREEN_SECONDS - BASE_GREEN_SECONDS);
        boolean noCurrentTraffic = currentQueue == 0 && phaseSeconds >= MIN_GREEN_SECONDS;
        if (noCurrentTraffic || phaseSeconds >= desiredGreen || phaseSeconds >= MAX_GREEN_SECONDS) {
            greenDirection = null;
            clearing = true;
            phaseSeconds = 0;
        }
    }

    /** Accumulates starvation-prevention time for queued approaches that are not green. */
    private void updateWaitingTimes(double deltaSeconds, Map<Direction, Integer> queues) {
        for (Direction direction : Direction.values()) {
            if (queues.getOrDefault(direction, 0) > 0 && direction != greenDirection) {
                waitingSeconds.merge(direction, deltaSeconds, Double::sum);
            } else if (queues.getOrDefault(direction, 0) == 0) {
                waitingSeconds.put(direction, 0.0);
            }
        }
    }

    /**
     * Chooses the highest-scoring queued direction.
     * Capacity-level queues receive an immediate priority bonus.
     */
    private Direction selectNext(Map<Direction, Integer> queues, int capacity) {
        Direction selected = null;
        double highestScore = Double.NEGATIVE_INFINITY;
        for (Direction direction : Direction.values()) {
            int queue = queues.getOrDefault(direction, 0);
            if (queue == 0) {
                continue;
            }
            double capacityBonus = queue >= capacity ? 1000.0 : 0.0;
            double score = capacityBonus + queue * 10.0 + waitingSeconds.get(direction);
            if (score > highestScore) {
                selected = direction;
                highestScore = score;
            }
        }
        return selected;
    }

    /** Returns the current green approach, or {@code null} during all-red clearance. */
    public Direction greenDirection() {
        return greenDirection;
    }

    /** Returns whether the specified approach may currently enter the intersection. */
    public boolean isGreen(Direction direction) {
        return !clearing && greenDirection == direction;
    }

    /** Returns whether every light is red while the intersection clears. */
    public boolean isClearing() {
        return clearing;
    }

    /** Returns elapsed time in the current green or clearance phase. */
    public double phaseSeconds() {
        return phaseSeconds;
    }

    /** Returns how long a queued direction has waited without a green signal. */
    double waitingSeconds(Direction direction) {
        return waitingSeconds.get(direction);
    }
}
