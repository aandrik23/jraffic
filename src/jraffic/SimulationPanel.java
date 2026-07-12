package jraffic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * Draws the intersection and dashboard and maps keyboard input to spawn actions.
 * Rendering uses a fixed design coordinate system that scales with the window.
 */
@SuppressWarnings("serial")
public final class SimulationPanel extends JPanel {
    private static final int DESIGN_WIDTH = 1180;
    private static final int DESIGN_HEIGHT = 900;
    private static final Color GRASS = new Color(31, 72, 61);
    private static final Color ROAD = new Color(48, 52, 58);
    private static final Color ROAD_EDGE = new Color(207, 211, 204);
    private static final Color LANE_MARK = new Color(235, 195, 73);
    private static final Color PANEL = new Color(20, 28, 34);
    private static final Color TEXT = new Color(235, 239, 237);
    private static final Color MUTED_TEXT = new Color(159, 173, 170);
    private static final Color RED = new Color(222, 61, 65);
    private static final Color GREEN = new Color(59, 205, 115);

    private final Simulation simulation;

    /** Creates a panel connected to the supplied simulation state. */
    public SimulationPanel(Simulation simulation) {
        this.simulation = simulation;
        setPreferredSize(new Dimension(DESIGN_WIDTH, DESIGN_HEIGHT));
        setBackground(PANEL);
        setFocusable(true);
        installControls();
    }

    /** Installs window-level key bindings for all required simulation commands. */
    private void installControls() {
        bind("UP", "spawnSouth", () -> simulation.spawn(Direction.SOUTH));
        bind("DOWN", "spawnNorth", () -> simulation.spawn(Direction.NORTH));
        bind("RIGHT", "spawnWest", () -> simulation.spawn(Direction.WEST));
        bind("LEFT", "spawnEast", () -> simulation.spawn(Direction.EAST));
        bind("R", "spawnRandom", simulation::spawnRandomDirection);
        bind("ESCAPE", "exit", () -> System.exit(0));
    }

    /** Associates one keystroke with a named action and repaints after it runs. */
    private void bind(String key, String name, Runnable action) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
        getActionMap().put(name, new AbstractAction() {
            /** Runs the action represented by this key binding. */
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
                repaint();
            }
        });
    }

    /** Paints the complete scaled simulation from the latest model snapshot. */
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        double scale = Math.min(getWidth() / (double) DESIGN_WIDTH, getHeight() / (double) DESIGN_HEIGHT);
        double offsetX = (getWidth() - DESIGN_WIDTH * scale) / 2.0;
        double offsetY = (getHeight() - DESIGN_HEIGHT * scale) / 2.0;
        g.translate(offsetX, offsetY);
        g.scale(scale, scale);

        drawIntersection(g);
        drawLights(g);
        drawVehicles(g);
        drawDashboard(g);
        g.dispose();
    }

    /** Draws roads, lane dividers, stop lines, road edges, and compass labels. */
    private void drawIntersection(Graphics2D g) {
        g.setColor(GRASS);
        g.fillRect(0, 0, 900, 900);
        g.setColor(ROAD);
        g.fillRect((int) Geometry.ROAD_MIN, 0,
                (int) (Geometry.ROAD_MAX - Geometry.ROAD_MIN), 900);
        g.fillRect(0, (int) Geometry.ROAD_MIN, 900,
                (int) (Geometry.ROAD_MAX - Geometry.ROAD_MIN));

        g.setStroke(new BasicStroke(3));
        g.setColor(ROAD_EDGE);
        g.drawLine((int) Geometry.ROAD_MIN, 0, (int) Geometry.ROAD_MIN, (int) Geometry.ROAD_MIN);
        g.drawLine((int) Geometry.ROAD_MAX, 0, (int) Geometry.ROAD_MAX, (int) Geometry.ROAD_MIN);
        g.drawLine((int) Geometry.ROAD_MIN, (int) Geometry.ROAD_MAX,
                (int) Geometry.ROAD_MIN, 900);
        g.drawLine((int) Geometry.ROAD_MAX, (int) Geometry.ROAD_MAX,
                (int) Geometry.ROAD_MAX, 900);
        g.drawLine(0, (int) Geometry.ROAD_MIN, (int) Geometry.ROAD_MIN, (int) Geometry.ROAD_MIN);
        g.drawLine(0, (int) Geometry.ROAD_MAX, (int) Geometry.ROAD_MIN, (int) Geometry.ROAD_MAX);
        g.drawLine((int) Geometry.ROAD_MAX, (int) Geometry.ROAD_MIN, 900, (int) Geometry.ROAD_MIN);
        g.drawLine((int) Geometry.ROAD_MAX, (int) Geometry.ROAD_MAX, 900, (int) Geometry.ROAD_MAX);

        g.setColor(LANE_MARK);
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10,
                new float[]{14, 12}, 0));
        g.drawLine(450, 0, 450, (int) Geometry.ROAD_MIN);
        g.drawLine(450, (int) Geometry.ROAD_MAX, 450, 900);
        g.drawLine(0, 450, (int) Geometry.ROAD_MIN, 450);
        g.drawLine((int) Geometry.ROAD_MAX, 450, 900, 450);

        g.setStroke(new BasicStroke(5));
        g.setColor(Color.WHITE);
        g.drawLine(385, 370, 445, 370);
        g.drawLine(455, 530, 515, 530);
        g.drawLine(370, 455, 370, 515);
        g.drawLine(530, 385, 530, 445);

        drawCompassLabel(g, "NORTH", 450, 24);
        drawCompassLabel(g, "SOUTH", 450, 884);
        drawCompassLabel(g, "WEST", 35, 365);
        drawCompassLabel(g, "EAST", 865, 365);
    }

    /** Draws a horizontally centered compass label at a map edge. */
    private void drawCompassLabel(Graphics2D g, String text, int centerX, int y) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        FontMetrics metrics = g.getFontMetrics();
        g.setColor(new Color(234, 240, 230));
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, y);
    }

    /** Draws one traffic-light housing beside each incoming stop line. */
    private void drawLights(Graphics2D g) {
        drawLight(g, 365, 345, Direction.NORTH);
        drawLight(g, 510, 535, Direction.SOUTH);
        drawLight(g, 345, 510, Direction.WEST);
        drawLight(g, 535, 365, Direction.EAST);
    }

    /** Draws a red or green lamp based on the controller state for one approach. */
    private void drawLight(Graphics2D g, int x, int y, Direction direction) {
        g.setColor(new Color(12, 15, 17));
        g.fillRoundRect(x, y, 28, 28, 8, 8);
        g.setColor(simulation.controller().isGreen(direction) ? GREEN : RED);
        g.fillOval(x + 5, y + 5, 18, 18);
        g.setColor(new Color(255, 255, 255, 100));
        g.fillOval(x + 9, y + 8, 5, 5);
    }

    /** Draws every vehicle rotated to its current route heading and route color. */
    private void drawVehicles(Graphics2D g) {
        for (Vehicle vehicle : simulation.vehicles()) {
            Geometry.Point point = vehicle.position();
            AffineTransform original = g.getTransform();
            g.translate(point.x(), point.y());
            g.rotate(vehicle.heading());

            Shape body = new Rectangle2D.Double(-Vehicle.LENGTH / 2, -Vehicle.WIDTH / 2,
                    Vehicle.LENGTH, Vehicle.WIDTH);
            g.setColor(new Color(0, 0, 0, 80));
            g.translate(2, 2);
            g.fill(body);
            g.translate(-2, -2);
            g.setColor(vehicle.route().color());
            g.fillRoundRect((int) (-Vehicle.LENGTH / 2), (int) (-Vehicle.WIDTH / 2),
                    (int) Vehicle.LENGTH, (int) Vehicle.WIDTH, 7, 7);
            g.setColor(new Color(202, 229, 235));
            g.fillRoundRect(-3, (int) (-Vehicle.WIDTH / 2 + 2), 9,
                    (int) Vehicle.WIDTH - 4, 3, 3);
            g.setColor(new Color(20, 25, 28));
            g.fillRect(-9, (int) (-Vehicle.WIDTH / 2 - 1), 5, 2);
            g.fillRect(5, (int) (-Vehicle.WIDTH / 2 - 1), 5, 2);
            g.fillRect(-9, (int) (Vehicle.WIDTH / 2 - 1), 5, 2);
            g.fillRect(5, (int) (Vehicle.WIDTH / 2 - 1), 5, 2);
            g.setTransform(original);
        }
    }

    /** Draws live signal state, queue loads, route legend, controls, and status text. */
    private void drawDashboard(Graphics2D g) {
        g.setColor(PANEL);
        g.fillRect(900, 0, DESIGN_WIDTH - 900, DESIGN_HEIGHT);
        g.setColor(TEXT);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        g.drawString("JRAFFIC", 930, 55);
        g.setColor(MUTED_TEXT);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        g.drawString("ADAPTIVE JUNCTION CONTROL", 930, 78);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.setColor(TEXT);
        g.drawString("SIGNAL", 930, 125);
        Direction green = simulation.controller().greenDirection();
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g.setColor(green == null ? RED : GREEN);
        g.drawString(green == null ? "ALL RED / CLEARING" : green.label().toUpperCase() + " GREEN",
                930, 153);

        g.setColor(TEXT);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.drawString("LANE LOAD", 930, 205);
        Map<Direction, Integer> queues = simulation.queueCounts();
        int y = 237;
        for (Direction direction : Direction.values()) {
            drawQueue(g, direction, queues.get(direction), y);
            y += 53;
        }

        g.setColor(TEXT);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.drawString("ROUTE COLORS", 930, 480);
        y = 510;
        for (Route route : Route.values()) {
            g.setColor(route.color());
            g.fillRoundRect(930, y - 12, 28, 14, 6, 6);
            g.setColor(TEXT);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            g.drawString(route.label(), 970, y);
            y += 31;
        }

        g.setColor(TEXT);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.drawString("CONTROLS", 930, 640);
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        g.setColor(MUTED_TEXT);
        g.drawString("UP     from South", 930, 670);
        g.drawString("DOWN   from North", 930, 693);
        g.drawString("RIGHT  from West", 930, 716);
        g.drawString("LEFT   from East", 930, 739);
        g.drawString("R      random", 930, 762);
        g.drawString("ESC    exit", 930, 785);

        g.setColor(new Color(32, 43, 50));
        g.fillRoundRect(920, 820, 240, 55, 10, 10);
        g.setColor(TEXT);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        drawCenteredWrapped(g, simulation.statusMessage(), 1040, 842, 220);
    }

    /** Draws one lane's queue count and capacity-relative load bar. */
    private void drawQueue(Graphics2D g, Direction direction, int queue, int y) {
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        g.setColor(TEXT);
        g.drawString(direction.label(), 930, y);
        String count = queue + " / " + Simulation.LANE_CAPACITY;
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(count, 1145 - metrics.stringWidth(count), y);
        g.setColor(new Color(50, 61, 67));
        g.fillRoundRect(930, y + 10, 215, 8, 8, 8);
        double ratio = queue / (double) Simulation.LANE_CAPACITY;
        g.setColor(ratio >= 1 ? RED : ratio >= 0.7 ? Route.RIGHT.color() : GREEN);
        g.fillRoundRect(930, y + 10, (int) (215 * ratio), 8, 8, 8);
    }

    /** Centers status text and splits it over two lines when it exceeds the panel width. */
    private void drawCenteredWrapped(Graphics2D g, String text, int centerX, int y, int maxWidth) {
        FontMetrics metrics = g.getFontMetrics();
        if (metrics.stringWidth(text) <= maxWidth) {
            g.drawString(text, centerX - metrics.stringWidth(text) / 2, y + 10);
            return;
        }
        int split = text.lastIndexOf(' ', text.length() / 2);
        if (split < 0) {
            split = text.length() / 2;
        }
        String first = text.substring(0, split);
        String second = text.substring(Math.min(text.length(), split + 1));
        g.drawString(first, centerX - metrics.stringWidth(first) / 2, y);
        g.drawString(second, centerX - metrics.stringWidth(second) / 2, y + 17);
    }
}
