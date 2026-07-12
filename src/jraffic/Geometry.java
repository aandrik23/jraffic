package jraffic;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the logical map dimensions and builds paths through the intersection.
 * All coordinates are independent of the window size and are scaled when drawn.
 */
final class Geometry {
    static final double WORLD_SIZE = 900.0;
    static final double ROAD_MIN = 380.0;
    static final double ROAD_MAX = 520.0;
    static final double NORTH_LANE_X = 415.0;
    static final double SOUTH_LANE_X = 485.0;
    static final double WEST_LANE_Y = 485.0;
    static final double EAST_LANE_Y = 415.0;

    /** Prevents creation of this static geometry utility class. */
    private Geometry() {
    }

    /** Represents one immutable point in logical simulation coordinates. */
    record Point(double x, double y) {
    }

    /**
     * Builds the ordered path for one approach and maneuver.
     * The first segment always runs from the spawn point to the stop line.
     */
    static List<Point> path(Direction direction, Route route) {
        List<Point> points = new ArrayList<>();
        switch (direction) {
            case NORTH -> {
                points.add(new Point(NORTH_LANE_X, 40));
                points.add(new Point(NORTH_LANE_X, 370));
                appendNorth(points, route);
            }
            case SOUTH -> {
                points.add(new Point(SOUTH_LANE_X, 860));
                points.add(new Point(SOUTH_LANE_X, 530));
                appendSouth(points, route);
            }
            case WEST -> {
                points.add(new Point(40, WEST_LANE_Y));
                points.add(new Point(370, WEST_LANE_Y));
                appendWest(points, route);
            }
            case EAST -> {
                points.add(new Point(860, EAST_LANE_Y));
                points.add(new Point(530, EAST_LANE_Y));
                appendEast(points, route);
            }
        }
        return List.copyOf(points);
    }

    /** Appends the intersection and exit points for traffic arriving from north. */
    private static void appendNorth(List<Point> points, Route route) {
        switch (route) {
            case STRAIGHT -> points.add(new Point(NORTH_LANE_X, 940));
            case RIGHT -> points.addAll(List.of(
                    new Point(410, 390), new Point(400, 405),
                    new Point(380, EAST_LANE_Y), new Point(-40, EAST_LANE_Y)));
            case LEFT -> points.addAll(List.of(
                    new Point(415, 435), new Point(430, 470),
                    new Point(465, WEST_LANE_Y), new Point(940, WEST_LANE_Y)));
        }
    }

    /** Appends the intersection and exit points for traffic arriving from south. */
    private static void appendSouth(List<Point> points, Route route) {
        switch (route) {
            case STRAIGHT -> points.add(new Point(SOUTH_LANE_X, -40));
            case RIGHT -> points.addAll(List.of(
                    new Point(490, 510), new Point(500, 495),
                    new Point(520, WEST_LANE_Y), new Point(940, WEST_LANE_Y)));
            case LEFT -> points.addAll(List.of(
                    new Point(485, 465), new Point(470, 430),
                    new Point(435, EAST_LANE_Y), new Point(-40, EAST_LANE_Y)));
        }
    }

    /** Appends the intersection and exit points for traffic arriving from west. */
    private static void appendWest(List<Point> points, Route route) {
        switch (route) {
            case STRAIGHT -> points.add(new Point(940, WEST_LANE_Y));
            case RIGHT -> points.addAll(List.of(
                    new Point(390, 490), new Point(405, 500),
                    new Point(NORTH_LANE_X, 520), new Point(NORTH_LANE_X, 940)));
            case LEFT -> points.addAll(List.of(
                    new Point(435, 485), new Point(470, 470),
                    new Point(SOUTH_LANE_X, 435), new Point(SOUTH_LANE_X, -40)));
        }
    }

    /** Appends the intersection and exit points for traffic arriving from east. */
    private static void appendEast(List<Point> points, Route route) {
        switch (route) {
            case STRAIGHT -> points.add(new Point(-40, EAST_LANE_Y));
            case RIGHT -> points.addAll(List.of(
                    new Point(510, 410), new Point(495, 400),
                    new Point(SOUTH_LANE_X, 380), new Point(SOUTH_LANE_X, -40)));
            case LEFT -> points.addAll(List.of(
                    new Point(465, 415), new Point(430, 430),
                    new Point(NORTH_LANE_X, 465), new Point(NORTH_LANE_X, 940)));
        }
    }

    /**
     * Calculates the total distance reached at every point in a path.
     * These values let vehicles move at a fixed speed regardless of segment length.
     */
    static double[] cumulativeLengths(List<Point> path) {
        double[] lengths = new double[path.size()];
        for (int i = 1; i < path.size(); i++) {
            lengths[i] = lengths[i - 1] + distance(path.get(i - 1), path.get(i));
        }
        return lengths;
    }

    /** Returns the interpolated map position at the requested path distance. */
    static Point pointAt(List<Point> path, double[] lengths, double progress) {
        int segment = segmentAt(lengths, progress);
        Point start = path.get(segment);
        Point end = path.get(segment + 1);
        double segmentLength = lengths[segment + 1] - lengths[segment];
        double ratio = segmentLength == 0 ? 0 : (progress - lengths[segment]) / segmentLength;
        ratio = Math.max(0, Math.min(1, ratio));
        return new Point(start.x() + (end.x() - start.x()) * ratio,
                start.y() + (end.y() - start.y()) * ratio);
    }

    /** Returns the angle of the path segment at the requested distance. */
    static double headingAt(List<Point> path, double[] lengths, double progress) {
        int segment = segmentAt(lengths, progress);
        Point start = path.get(segment);
        Point end = path.get(segment + 1);
        return Math.atan2(end.y() - start.y(), end.x() - start.x());
    }

    /** Finds the path segment containing the supplied cumulative distance. */
    private static int segmentAt(double[] lengths, double progress) {
        for (int i = 1; i < lengths.length; i++) {
            if (progress <= lengths[i]) {
                return i - 1;
            }
        }
        return lengths.length - 2;
    }

    /** Calculates the straight-line distance between two logical points. */
    private static double distance(Point first, Point second) {
        return Math.hypot(second.x() - first.x(), second.y() - first.y());
    }
}
