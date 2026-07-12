package jraffic;

import java.util.List;

/**
 * Models one car moving along a route chosen at spawn time.
 * Progress is measured as distance along the route, which keeps moving speed fixed.
 */
public final class Vehicle {
    public static final double LENGTH = 28.0;
    public static final double WIDTH = 15.0;
    public static final double SPEED = 82.0;

    private final long id;
    private final Direction direction;
    private final Route route;
    private final List<Geometry.Point> path;
    private final double[] cumulativeLengths;
    private final double stopProgress;
    private double progress;

    /** Creates a stationary vehicle at the start of its selected approach and route. */
    Vehicle(long id, Direction direction, Route route) {
        this.id = id;
        this.direction = direction;
        this.route = route;
        this.path = Geometry.path(direction, route);
        this.cumulativeLengths = Geometry.cumulativeLengths(path);
        this.stopProgress = cumulativeLengths[1];
    }

    /** Advances along the path without allowing progress beyond the route endpoint. */
    void move(double distance) {
        progress = Math.min(totalLength(), progress + Math.max(0, distance));
    }

    /** Returns the unique simulation identifier for this vehicle. */
    public long id() {
        return id;
    }

    /** Returns the approach from which this vehicle entered the map. */
    public Direction direction() {
        return direction;
    }

    /** Returns the immutable maneuver selected when this vehicle spawned. */
    public Route route() {
        return route;
    }

    /** Returns the distance already travelled along the route. */
    public double progress() {
        return progress;
    }

    /** Returns the path distance at which the vehicle must wait for a red light. */
    public double stopProgress() {
        return stopProgress;
    }

    /** Returns whether the vehicle's center has crossed its incoming stop line. */
    public boolean hasEnteredIntersection() {
        return progress > stopProgress + 0.01;
    }

    /** Returns whether the vehicle has reached the route endpoint and can be removed. */
    public boolean isFinished() {
        return progress >= totalLength() - 0.01;
    }

    /** Converts current path progress into a logical screen position. */
    public Geometry.Point position() {
        return Geometry.pointAt(path, cumulativeLengths, progress);
    }

    /** Returns the angle used to rotate the vehicle to follow its current segment. */
    public double heading() {
        return Geometry.headingAt(path, cumulativeLengths, progress);
    }

    /** Returns whether any part of an entered vehicle still occupies the junction. */
    boolean isInsideIntersection() {
        if (!hasEnteredIntersection()) {
            return false;
        }
        Geometry.Point point = position();
        double margin = LENGTH / 2.0;
        return point.x() >= Geometry.ROAD_MIN - margin
                && point.x() <= Geometry.ROAD_MAX + margin
                && point.y() >= Geometry.ROAD_MIN - margin
                && point.y() <= Geometry.ROAD_MAX + margin;
    }

    /** Returns the complete distance of this vehicle's selected path. */
    private double totalLength() {
        return cumulativeLengths[cumulativeLengths.length - 1];
    }
}
