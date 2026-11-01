package com.sisak.hooverball.physics;

import com.sisak.hooverball.entities.Ball;
import com.sisak.hooverball.entities.Vehicle;

/** Simple circle/circle collision resolution on the X/Z plane. */
public final class Collisions {
    private static final float VEHICLE_RESTITUTION = 0.85f;
    private static final float MIN_SEPARATION_SPEED = 1.5f;
    private static final float BALL_KICK_STRENGTH = 5.4f;
    private static final float BALL_CARRY_FACTOR = 0.9f;

    private Collisions() {
    }

    /**
     * Equal-mass elastic bounce, so ramming the other car actually feels like
     * an impact. Returns the impact speed (0 if no real collision happened),
     * so callers can trigger an impact sound scaled to how hard it was.
     */
    public static float resolveVehicleVehicle(Vehicle a, Vehicle b) {
        float dx = b.x - a.x;
        float dz = b.z - a.z;
        float distSq = dx * dx + dz * dz;
        float minDist = Vehicle.RADIUS * 2f;
        if (distSq >= minDist * minDist || distSq < 0.0001f) {
            return 0f;
        }
        float dist = (float) Math.sqrt(distSq);
        float overlap = minDist - dist;
        float nx = dx / dist;
        float nz = dz / dist;
        float push = overlap / 2f;
        a.x -= nx * push;
        a.z -= nz * push;
        b.x += nx * push;
        b.z += nz * push;

        float relVx = b.vx - a.vx;
        float relVz = b.vz - a.vz;
        float closingSpeed = relVx * nx + relVz * nz;
        if (closingSpeed < 0f) {
            // Even a slow, barely-overlapping approach gets a minimum kick apart,
            // so vehicles never feel "sticky"; a fast ram keeps its real impact speed.
            float effectiveSpeed = Math.min(closingSpeed, -MIN_SEPARATION_SPEED);
            float impulse = -(1f + VEHICLE_RESTITUTION) * effectiveSpeed / 2f;
            a.vx -= impulse * nx;
            a.vz -= impulse * nz;
            b.vx += impulse * nx;
            b.vz += impulse * nz;
            return -effectiveSpeed;
        }
        return 0f;
    }

    /** Pushes the ball away from a vehicle that has driven into it. */
    public static void resolveVehicleBall(Vehicle v, Ball ball) {
        float dx = ball.x - v.x;
        float dz = ball.z - v.z;
        float distSq = dx * dx + dz * dz;
        float minDist = Vehicle.RADIUS + Ball.RADIUS;
        if (distSq >= minDist * minDist) {
            return;
        }
        float dist = (float) Math.sqrt(distSq);
        if (dist < 0.0001f) {
            dist = 0.0001f;
            dx = minDist;
            dz = 0;
        }
        float overlap = minDist - dist;
        float nx = dx / dist;
        float nz = dz / dist;

        ball.x += nx * overlap;
        ball.z += nz * overlap;
        ball.applyImpulse(
                nx * BALL_KICK_STRENGTH + v.vx * BALL_CARRY_FACTOR,
                nz * BALL_KICK_STRENGTH + v.vz * BALL_CARRY_FACTOR);
    }
}
