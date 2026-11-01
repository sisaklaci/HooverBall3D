package com.sisak.hooverball.entities;

import com.badlogic.gdx.math.MathUtils;
import com.sisak.hooverball.Arena;

/**
 * The floating hooverball. Like {@link Vehicle} it only moves on the X/Z
 * plane for gameplay purposes; its height and spin are purely cosmetic.
 */
public class Ball {
    public static final float RADIUS = 0.4f;
    // Flattened disc 0.3 tall (see GameAssets); hover ~2cm above the floor.
    public static final float BASE_HEIGHT = 0.17f;
    public static final float BOB_AMPLITUDE = 0.015f;
    public static final float BOB_SPEED = 2.6f;
    private static final float WALL_RESTITUTION = 0.75f;
    private static final float DRAG = 0.35f; // fraction of speed lost per second
    private static final float MIN_SPEED = 0.05f;
    // Matches the arena's own rounded corner walls (inset by the ball's own
    // radius), so the ball can't wedge into a corner either.
    private static final float CORNER_RADIUS = Arena.CORNER_RADIUS - RADIUS;

    /** Result of a single wall/goal check this frame. */
    public enum GoalResult { NONE, PLAYER_SCORED, AI_SCORED }

    public float x, z;
    public float vx, vz;
    public float spinAngle;
    private float bobTime;

    public Ball(float x, float z) {
        this.x = x;
        this.z = z;
    }

    public void applyImpulse(float ix, float iz) {
        vx += ix;
        vz += iz;
    }

    public GoalResult update(float dt) {
        // Drag.
        float dragFactor = Math.max(0f, 1f - DRAG * dt);
        vx *= dragFactor;
        vz *= dragFactor;
        if (Math.abs(vx) < MIN_SPEED) vx = 0;
        if (Math.abs(vz) < MIN_SPEED) vz = 0;

        x += vx * dt;
        z += vz * dt;
        bobTime += dt;
        float speed = (float) Math.sqrt(vx * vx + vz * vz);
        spinAngle += speed * dt * 60f;

        return resolveWalls();
    }

    private GoalResult resolveWalls() {
        float minX = -Arena.HALF_WIDTH + RADIUS;
        float maxX = Arena.HALF_WIDTH - RADIUS;
        if (x < minX) {
            x = minX;
            vx = -vx * WALL_RESTITUTION;
        } else if (x > maxX) {
            x = maxX;
            vx = -vx * WALL_RESTITUTION;
        }

        boolean inGoalGap = Math.abs(x) < Arena.GOAL_HALF_WIDTH - RADIUS * 0.3f;
        float minZ = -Arena.HALF_LENGTH + RADIUS;
        float maxZ = Arena.HALF_LENGTH - RADIUS;

        // Scores the instant the whole ball has crossed the goal line, like a
        // real goal line - it doesn't need to travel further into the net.
        // Requiring that extra distance let a slowing ball stall in the gap,
        // out of the attacker's reach (vehicles can't follow it past the
        // line), which read as the ball "stopping in front of the goal".
        if (z < minZ) {
            if (inGoalGap) {
                if (z < -Arena.HALF_LENGTH - RADIUS) {
                    return GoalResult.AI_SCORED;
                }
            } else {
                z = minZ;
                vz = -vz * WALL_RESTITUTION;
            }
        } else if (z > maxZ) {
            if (inGoalGap) {
                if (z > Arena.HALF_LENGTH + RADIUS) {
                    return GoalResult.PLAYER_SCORED;
                }
            } else {
                z = maxZ;
                vz = -vz * WALL_RESTITUTION;
            }
        }

        clampCorner(maxX, maxZ);
        return GoalResult.NONE;
    }

    /** Rounds off the arena's corners with a quarter-circle so the ball can't wedge into them either. */
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
        return BASE_HEIGHT + MathUtils.sin(bobTime * BOB_SPEED) * BOB_AMPLITUDE;
    }

    public void reset(float nx, float nz) {
        x = nx;
        z = nz;
        vx = 0;
        vz = 0;
    }
}
