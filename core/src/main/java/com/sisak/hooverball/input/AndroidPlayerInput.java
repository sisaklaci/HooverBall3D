package com.sisak.hooverball.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.MathUtils;

/**
 * Touch controls: a virtual joystick on the left half of the screen for
 * movement, and drag-to-look on the right half, matching common mobile FPS
 * conventions. A button in the top-right corner toggles pause.
 */
public class AndroidPlayerInput implements InputProcessor, PlayerInput {
    private static final float TOUCH_LOOK_SENSITIVITY = 0.006f;
    public static final float JOYSTICK_RADIUS = 110f;
    public static final float PAUSE_BUTTON_SIZE = 110f;
    public static final float PAUSE_BUTTON_MARGIN = 20f;
    public static final float BOOST_BUTTON_RADIUS = 80f;
    public static final float BOOST_BUTTON_MARGIN = 40f;

    private int movePointer = -1;
    private int lookPointer = -1;
    private int boostPointer = -1;

    private float joystickOriginX, joystickOriginY;
    private float joystickCurrentX, joystickCurrentY;
    private float lastLookX, lastLookY;

    private float accumYawDelta, accumPitchDelta;
    private float forward, strafe;
    private boolean pauseRequested;

    public AndroidPlayerInput() {
        Gdx.input.setInputProcessor(this);
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
    }

    @Override
    public void update(float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
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
        float d = accumYawDelta;
        accumYawDelta = 0f;
        return d;
    }

    @Override
    public float getPitchDelta() {
        float d = accumPitchDelta;
        accumPitchDelta = 0f;
        return d;
    }

    @Override
    public boolean consumePauseToggle() {
        boolean result = pauseRequested;
        pauseRequested = false;
        return result;
    }

    @Override
    public boolean isBoostPressed() {
        return boostPointer != -1;
    }

    public boolean isJoystickActive() {
        return movePointer != -1;
    }

    public float getJoystickOriginX() {
        return joystickOriginX;
    }

    public float getJoystickOriginY() {
        return joystickOriginY;
    }

    public float getJoystickCurrentX() {
        return joystickCurrentX;
    }

    public float getJoystickCurrentY() {
        return joystickCurrentY;
    }

    public float getBoostButtonX() {
        return Gdx.graphics.getWidth() - BOOST_BUTTON_MARGIN - BOOST_BUTTON_RADIUS;
    }

    public float getBoostButtonY() {
        return Gdx.graphics.getHeight() - BOOST_BUTTON_MARGIN - BOOST_BUTTON_RADIUS;
    }

    private boolean isOnPauseButton(float x, float y) {
        float w = Gdx.graphics.getWidth();
        float left = w - PAUSE_BUTTON_MARGIN - PAUSE_BUTTON_SIZE;
        float top = PAUSE_BUTTON_MARGIN;
        return x >= left && x <= left + PAUSE_BUTTON_SIZE && y >= top && y <= top + PAUSE_BUTTON_SIZE;
    }

    private boolean isOnBoostButton(float x, float y) {
        float cx = Gdx.graphics.getWidth() - BOOST_BUTTON_MARGIN - BOOST_BUTTON_RADIUS;
        float cy = Gdx.graphics.getHeight() - BOOST_BUTTON_MARGIN - BOOST_BUTTON_RADIUS;
        float dx = x - cx;
        float dy = y - cy;
        return dx * dx + dy * dy <= BOOST_BUTTON_RADIUS * BOOST_BUTTON_RADIUS;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (isOnPauseButton(screenX, screenY)) {
            pauseRequested = true;
            return true;
        }
        if (isOnBoostButton(screenX, screenY) && boostPointer == -1) {
            boostPointer = pointer;
            return true;
        }
        float halfWidth = Gdx.graphics.getWidth() / 2f;
        if (screenX < halfWidth && movePointer == -1) {
            movePointer = pointer;
            joystickOriginX = screenX;
            joystickOriginY = screenY;
            joystickCurrentX = screenX;
            joystickCurrentY = screenY;
        } else if (screenX >= halfWidth && lookPointer == -1) {
            lookPointer = pointer;
            lastLookX = screenX;
            lastLookY = screenY;
        }
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (pointer == movePointer) {
            float dx = screenX - joystickOriginX;
            float dy = screenY - joystickOriginY;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > JOYSTICK_RADIUS) {
                dx = dx / len * JOYSTICK_RADIUS;
                dy = dy / len * JOYSTICK_RADIUS;
            }
            joystickCurrentX = joystickOriginX + dx;
            joystickCurrentY = joystickOriginY + dy;
            strafe = MathUtils.clamp(dx / JOYSTICK_RADIUS, -1f, 1f);
            forward = MathUtils.clamp(-dy / JOYSTICK_RADIUS, -1f, 1f);
        } else if (pointer == lookPointer) {
            float dx = screenX - lastLookX;
            float dy = screenY - lastLookY;
            accumYawDelta -= dx * TOUCH_LOOK_SENSITIVITY;
            accumPitchDelta -= dy * TOUCH_LOOK_SENSITIVITY;
            lastLookX = screenX;
            lastLookY = screenY;
        }
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (pointer == movePointer) {
            movePointer = -1;
            forward = 0f;
            strafe = 0f;
        } else if (pointer == lookPointer) {
            lookPointer = -1;
        } else if (pointer == boostPointer) {
            boostPointer = -1;
        }
        return true;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return touchUp(screenX, screenY, pointer, button);
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    @Override
    public void dispose() {
        Gdx.input.setInputProcessor(null);
    }
}
