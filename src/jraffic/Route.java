package jraffic;

import java.awt.Color;

/**
 * Defines the immutable maneuver selected when a vehicle is created.
 */
public enum Route {
    LEFT("Left turn", new Color(218, 83, 146)),
    STRAIGHT("Straight", new Color(43, 145, 216)),
    RIGHT("Right turn", new Color(244, 183, 64));

    private final String label;
    private final Color color;

    /** Creates a route with the label and vehicle color used by the renderer. */
    Route(String label, Color color) {
        this.label = label;
        this.color = color;
    }

    /** Returns the route name displayed in status messages and the legend. */
    public String label() {
        return label;
    }

    /** Returns the color that visually identifies this route. */
    public Color color() {
        return color;
    }
}
