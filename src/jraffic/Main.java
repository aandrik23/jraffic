package jraffic;

import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 * Application entry point that creates the Swing window and fixed-rate update timer.
 */
public final class Main {
    /** Prevents construction because application startup is entirely static. */
    private Main() {
    }

    /** Schedules GUI creation on Swing's event-dispatch thread. */
    public static void main(String[] args) {
        EventQueue.invokeLater(Main::start);
    }

    /** Builds the simulation window and starts the approximately 60 FPS update loop. */
    private static void start() {
        UIManager.put("Panel.font", UIManager.getFont("Label.font"));
        Simulation simulation = new Simulation();
        SimulationPanel panel = new SimulationPanel(simulation);

        JFrame frame = new JFrame("Jraffic - Adaptive Traffic Control");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(panel);
        frame.pack();
        frame.setMinimumSize(new java.awt.Dimension(820, 650));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        long[] previousNanos = {System.nanoTime()};
        Timer timer = new Timer(16, event -> {
            long now = System.nanoTime();
            double deltaSeconds = (now - previousNanos[0]) / 1_000_000_000.0;
            previousNanos[0] = now;
            simulation.update(deltaSeconds);
            panel.repaint();
        });
        timer.start();
    }
}
