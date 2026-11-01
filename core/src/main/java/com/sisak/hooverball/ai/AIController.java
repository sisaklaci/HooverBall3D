package com.sisak.hooverball.ai;

import com.badlogic.gdx.math.MathUtils;
import com.sisak.hooverball.Arena;
import com.sisak.hooverball.entities.Ball;
import com.sisak.hooverball.entities.Vehicle;

/**
 * Drives the opponent vehicle. The heuristic is the classic simple-soccer
 * trick: always approach the ball from the side closer to your own goal, so
 * that bumping into it pushes the ball towards the opponent's goal.
 */
public class AIController {
    private static final float APPROACH_OFFSET = 1.3f;
    private static final float SPEED_FACTOR = 1.05f;
    private static final float WOBBLE_AMOUNT = 0.12f;
    private static final float WOBBLE_SPEED = 1.3f;
    // The AI boosts readily to keep up an aggressive, goal-hungry chase.
    private static final float BOOST_DISTANCE_THRESHOLD = 4f;

    private final float aiGoalX = 0f;
    private final float aiGoalZ;
    private float time;

    public AIController() {
        this.aiGoalZ = Arena.HALF_LENGTH;
    }

    public void update(Vehicle ai, Ball ball, float dt) {
        time += dt;

        float toGoalX = aiGoalX - ball.x;
        float toGoalZ = aiGoalZ - ball.z;
        float len = (float) Math.sqrt(toGoalX * toGoalX + toGoalZ * toGoalZ);
        if (len < 0.001f) {
            len = 1f;
        }
        float wobble = MathUtils.sin(time * WOBBLE_SPEED) * WOBBLE_AMOUNT;
        float targetX = ball.x + (toGoalX / len) * APPROACH_OFFSET + wobble;
        float targetZ = ball.z + (toGoalZ / len) * APPROACH_OFFSET;

        float dx = targetX - ai.x;
        float dz = targetZ - ai.z;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        ai.updateBoost(dist > BOOST_DISTANCE_THRESHOLD, dt);

        float desiredVx = 0f;
        float desiredVz = 0f;
        if (dist > 0.05f) {
            float maxSpeed = ai.getMaxSpeed() * SPEED_FACTOR;
            desiredVx = dx / dist * maxSpeed;
            desiredVz = dz / dist * maxSpeed;
        }
        ai.seek(desiredVx, desiredVz, dt);
        ai.integrate(dt);

        float faceDx = ball.x - ai.x;
        float faceDz = ball.z - ai.z;
        if (Math.abs(faceDx) > 0.01f || Math.abs(faceDz) > 0.01f) {
            ai.yaw = MathUtils.atan2(faceDx, faceDz);
        }
    }
}
