package jraffic;

/**
 * Identifies the side of the map from which a vehicle approaches the junction.
 */
public enum Direction {
    NORTH("North", "Down arrow"),
    EAST("East", "Left arrow"),
    SOUTH("South", "Up arrow"),
    WEST("West", "Right arrow");

    private final String label;
    private final String control;

    /** Creates a direction with its display name and keyboard-control description. */
    Direction(String label, String control) {
        this.label = label;
        this.control = control;
    }

    /** Returns the human-readable compass name shown in the dashboard. */
    public String label() {
        return label;
    }

    /** Returns the key description associated with this approach. */
    public String control() {
        return control;
    }
}
