package com.sisak.hooverball;

/** Running score/clock for the current match, plus the win condition check. */
public class GameState {
    public enum Winner { NONE, PLAYER, AI, DRAW }

    private final GameConfig config;
    public int playerScore;
    public int aiScore;
    public float elapsedSeconds;
    public Winner winner = Winner.NONE;

    public GameState(GameConfig config) {
        this.config = config;
    }

    public void update(float dt) {
        if (winner != Winner.NONE) {
            return;
        }
        elapsedSeconds += dt;

        if (config.mode == GameConfig.Mode.TIME_LIMIT && elapsedSeconds >= config.timeLimitSeconds) {
            if (playerScore > aiScore) {
                winner = Winner.PLAYER;
            } else if (aiScore > playerScore) {
                winner = Winner.AI;
            } else {
                winner = Winner.DRAW;
            }
        }
    }

    public void onPlayerGoal() {
        playerScore++;
        checkScoreLimit();
    }

    public void onAiGoal() {
        aiScore++;
        checkScoreLimit();
    }

    private void checkScoreLimit() {
        if (config.mode != GameConfig.Mode.SCORE_LIMIT) {
            return;
        }
        if (playerScore >= config.scoreLimit) {
            winner = Winner.PLAYER;
        } else if (aiScore >= config.scoreLimit) {
            winner = Winner.AI;
        }
    }

    public boolean isFinished() {
        return winner != Winner.NONE;
    }

    public float getRemainingSeconds() {
        if (config.mode != GameConfig.Mode.TIME_LIMIT) {
            return -1f;
        }
        return Math.max(0f, config.timeLimitSeconds - elapsedSeconds);
    }

    public GameConfig getConfig() {
        return config;
    }
}
