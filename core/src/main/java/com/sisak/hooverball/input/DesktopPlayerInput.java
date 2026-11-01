package com.sisak.hooverball.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

/**
 * Mouse-look + WASD controls. The cursor starts free (not captured) so the
 * player can always drag the window to another monitor or position right
 * when a match begins; clicking into the view engages mouse-look, and ESC
 * (pause) releases it again at any time.
 */
public class DesktopPlayerInput implements PlayerInput {
    private static final float MOUSE_SENSITIVITY = 0.0025f;
    // Radians/second applied while an arrow key is held, so the game is fully
    // playable with the keyboard alone (no mouse needed to turn).
    private static final float ARROW_TURN_SPEED = 2.4f;

    private float forward, strafe, yawDelta, pitchDelta;
    private boolean pauseRequested;
    private boolean boostPressed;
    private boolean cursorCaught;
    // Catching the cursor warps it to the window centre, which would otherwise
    // register as one huge, jarring mouse delta on the very next frame.
    private boolean skipNextDelta;

    @Override
    public void update(float dt) {
        if (!cursorCaught && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            setCursorCaught(true);
        }

        forward = 0f;
        strafe = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) forward += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) forward -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) strafe += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) strafe -= 1f;

        yawDelta = 0f;
        pitchDelta = 0f;
        if (cursorCaught && !skipNextDelta) {
            yawDelta = -Gdx.input.getDeltaX() * MOUSE_SENSITIVITY;
            pitchDelta = -Gdx.input.getDeltaY() * MOUSE_SENSITIVITY;
        }
        skipNextDelta = false;

        // Left/Right arrows turn the vehicle directly, so the game is playable
        // with the keyboard alone, with no mouse required.
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) yawDelta -= ARROW_TURN_SPEED * dt;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) yawDelta += ARROW_TURN_SPEED * dt;

        boostPressed = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)
                || Gdx.input.isKeyPressed(Input.Keys.SPACE);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            // The screen reacts to consumePauseToggle() by calling setCursorCaught(),
            // which is the single place that actually (un)catches the cursor.
            pauseRequested = true;
        }
    }

    @Override
    public float getForward() {
        return forward;
    }

    @Override
    public float getStrafe() {
        return strafe;
    }

    @Override
    public float getYawDelta() {
        return yawDelta;
    }

    @Override
    public float getPitchDelta() {
        return pitchDelta;
    }

    @Override
    public boolean isBoostPressed() {
        return boostPressed;
    }

    @Override
    public boolean consumePauseToggle() {
        boolean result = pauseRequested;
        pauseRequested = false;
        return result;
    }

    public boolean isCursorCaught() {
        return cursorCaught;
    }

    /** Called when leaving the pause menu by other means (e.g. resume button) to keep mouse-look consistent. */
    public void setCursorCaught(boolean caught) {
        cursorCaught = caught;
        if (caught) {
            skipNextDelta = true;
        }
        Gdx.input.setCursorCatched(caught);
    }

    @Override
    public void dispose() {
        Gdx.input.setCursorCatched(false);
    }
}
