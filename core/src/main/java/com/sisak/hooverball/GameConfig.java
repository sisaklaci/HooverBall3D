package com.sisak.hooverball;

/** Settings chosen on the menu screen before a match starts. */
public class GameConfig {
    public enum Mode { SCORE_LIMIT, TIME_LIMIT }

    public Mode mode = Mode.SCORE_LIMIT;
    public int scoreLimit = 5;
    public float timeLimitSeconds = 5 * 60f;

    public static GameConfig scoreMode(int limit) {
        GameConfig config = new GameConfig();
        config.mode = Mode.SCORE_LIMIT;
        config.scoreLimit = limit;
        return config;
    }

    public static GameConfig timeMode(float minutes) {
        GameConfig config = new GameConfig();
        config.mode = Mode.TIME_LIMIT;
        config.timeLimitSeconds = minutes * 60f;
        return config;
    }
}
