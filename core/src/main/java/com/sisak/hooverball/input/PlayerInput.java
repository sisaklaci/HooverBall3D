package com.sisak.hooverball.input;

/** Abstracts desktop (mouse+keyboard) and mobile (touch) controls behind one interface. */
public interface PlayerInput {
    /** Polls the current input state. Call once per frame before reading the getters below. */
    void update(float dt);

    /** -1..1, forward/back intent relative to the vehicle's facing direction. */
    float getForward();

    /** -1..1, strafe left/right intent relative to the vehicle's facing direction. */
    float getStrafe();

    /** Yaw delta in radians to apply this frame (look left/right). */
    float getYawDelta();

    /** Pitch delta in radians to apply this frame (look up/down). */
    float getPitchDelta();

    /** True exactly once on the frame the player asked to pause/resume. */
    boolean consumePauseToggle();

    /** True while the player is holding the boost control. */
    boolean isBoostPressed();

    /** Releases any native resources / input processors this controller registered. */
    void dispose();
}
