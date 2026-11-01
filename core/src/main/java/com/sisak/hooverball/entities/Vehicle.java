package com.sisak.hooverball.entities;

import com.badlogic.gdx.math.MathUtils;
import com.sisak.hooverball.Arena;

/**
 * A small hovering vehicle (the player's or the AI's). Movement is resolved
 * on the flat X/Z plane only; height is purely a cosmetic bobbing effect so
 * the craft looks like it is skimming just above the floor.
 */
public class Vehicle {
    public static final float RADIUS = 1f;
    public static final float MAX_SPEED = 12.5f;
    public static final float ACCEL = 20f;
    // Body is a flattened sphere 0.56 tall (see GameAssets); hover ~2cm above the floor.
    public static final float BASE_HEIGHT = 0.3f;
    public static final float BOB_AMPLITUDE = 0.02f;
    public static final float BOB_SPEED = 2f;
    private static final float WALL_RESTITUTION = 0.6f;
    // Clamping X and Z independently would make a sharp 90-degree pocket at
    // each corner, where the AI's steering can get pinned fighting both
    // walls at once. This mirrors the arena's own rounded corner walls
    // (inset by this vehicle's own radius) so the invisible boundary exactly
    // matches what's actually drawn there.
    private static final float CORNER_RADIUS = Arena.CORNER_RADIUS - RADIUS;

    public static final float BOOST_SPEED_MULTIPLIER = 1.9f;
    public static final float BOOST_ACCEL_MULTIPLIER = 1.4f;
    public static final float MAX_BOOST_ENERGY = 100f;
    private static final float BOOST_DRAIN_PER_SECOND = 45f;
    private static final float BOOST_RECHARGE_PER_SECOND = 22f;

    public float x, z;
    public float vx, vz;
    public float yaw;
    private final float bobPhase;
    private float bobTime;

    private float boostEnergy = MAX_BOOST_ENERGY;
    private boolean boosting;

    public Vehicle(float x, float z, float yaw) {
        this.x = x;
        this.z = z;
        this.yaw = yaw;
        this.bobPhase = MathUtils.random(0f, MathUtils.PI2);
    }

    /**
     * Drains or recharges the boost energy for this frame and decides whether
     * boost is actually active (it needs energy left, not just the request).
     * Call this once per frame before {@link #seek}.
     */
    public boolean updateBoost(boolean wantsBoost, float dt) {
        boosting = wantsBoost && boostEnergy > 0f;
        if (boosting) {
            boostEnergy = Math.max(0f, boostEnergy - BOOST_DRAIN_PER_SECOND * dt);
        } else {
            boostEnergy = Math.min(MAX_BOOST_ENERGY, boostEnergy + BOOST_RECHARGE_PER_SECOND * dt);
        }
        return boosting;
    }

    public float getMaxSpeed() {
        return boosting ? MAX_SPEED * BOOST_SPEED_MULTIPLIER : MAX_SPEED;
    }

    private float getAccel() {
        return boosting ? ACCEL * BOOST_ACCEL_MULTIPLIER : ACCEL;
    }

    public float getBoostEnergyRatio() {
        return boostEnergy / MAX_BOOST_ENERGY;
    }

    public boolean isBoosting() {
        return boosting;
    }

    /** Steers velocity towards the desired velocity using a simple accelerating seek. */
    public void seek(float desiredVx, float desiredVz, float dt) {
        float t = Math.min(1f, getAccel() * dt);
        vx += (desiredVx - vx) * t;
        vz += (desiredVz - vz) * t;
    }

    public void integrate(float dt) {
        x += vx * dt;
        z += vz * dt;
        bobTime += dt;
        clampToArena();
    }

    private void clampToArena() {
        float minX = -Arena.HALF_WIDTH + RADIUS;
        float maxX = Arena.HALF_WIDTH - RADIUS;
        float minZ = -Arena.HALF_LENGTH + RADIUS;
        float maxZ = Arena.HALF_LENGTH - RADIUS;

        if (x < minX) {
            x = minX;
            vx = -vx * WALL_RESTITUTION;
        } else if (x > maxX) {
            x = maxX;
            vx = -vx * WALL_RESTITUTION;
        }

        if (z < minZ) {
            z = minZ;
            vz = -vz * WALL_RESTITUTION;
        } else if (z > maxZ) {
            z = maxZ;
            vz = -vz * WALL_RESTITUTION;
        }

        clampCorner(maxX, maxZ);
    }

    /** Rounds off the arena's corners with a quarter-circle so nothing can wedge into them. */
    private void clampCorner(float maxX, float maxZ) {
        float innerX = maxX - CORNER_RADIUS;
        float innerZ = maxZ - CORNER_RADIUS;
        if (innerX <= 0f || innerZ <= 0f || Math.abs(x) <= innerX || Math.abs(z) <= innerZ) {
            return;
        }
        float cornerX = Math.signum(x) * innerX;
        float cornerZ = Math.signum(z) * innerZ;
        float dx = x - cornerX;
        float dz = z - cornerZ;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        if (dist > CORNER_RADIUS) {
            float nx = dx / dist;
            float nz = dz / dist;
            x = cornerX + nx * CORNER_RADIUS;
            z = cornerZ + nz * CORNER_RADIUS;
            float vDotN = vx * nx + vz * nz;
            if (vDotN > 0f) {
                float j = (1f + WALL_RESTITUTION) * vDotN;
                vx -= j * nx;
                vz -= j * nz;
            }
        }
    }

    public float getRenderY() {
        return BASE_HEIGHT + MathUtils.sin(bobTime * BOB_SPEED + bobPhase) * BOB_AMPLITUDE;
    }

    public void reset(float nx, float nz, float nyaw) {
        x = nx;
        z = nz;
        yaw = nyaw;
        vx = 0;
        vz = 0;
    }
}
