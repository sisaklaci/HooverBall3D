package com.sisak.hooverball.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.sisak.hooverball.HooverBallGame;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

/** Launches the game as a native Windows/Linux/Mac desktop application. */
public class DesktopLauncher {
    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 720;

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("HooverBall 3D Soccer");
        config.setWindowedMode(WINDOW_WIDTH, WINDOW_HEIGHT);
        config.setResizable(true);
        config.setDecorated(true);
        // Always open centred on the primary monitor's own real bounds
        // (queried via AWT, which understands the true multi-monitor layout).
        // Without this, a stale remembered position - valid for a different
        // monitor arrangement - can place the whole title bar off-screen,
        // leaving the window impossible to grab and drag back.
        int[] position = computePrimaryMonitorCenteredPosition();
        if (position != null) {
            config.setWindowPosition(position[0], position[1]);
        }
        config.useVsync(true);
        config.setForegroundFPS(60);
        // 4x MSAA smooths out the low-poly geometry's edges.
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);
        new Lwjgl3Application(new HooverBallGame(), config);
    }

    /** Null if AWT can't report display bounds for some reason - the caller then leaves the OS default in place. */
    private static int[] computePrimaryMonitorCenteredPosition() {
        try {
            Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
            int x = bounds.x + Math.max(20, (bounds.width - WINDOW_WIDTH) / 2);
            int y = bounds.y + Math.max(20, (bounds.height - WINDOW_HEIGHT) / 2);
            return new int[] {x, y};
        } catch (Exception e) {
            return null;
        }
    }
}
