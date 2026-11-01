package com.sisak.hooverball.entities;

import com.sisak.hooverball.Arena;

/**
 * A small plane towing an advertising banner back and forth along one edge
 * of the pitch, or straight across the middle of it. Purely cosmetic - it
 * never interacts with gameplay physics. Multiple instances fly at once,
 * from different sides and overhead (see {@link #all()}).
 */
public class SkyBanner {
    /** Which way this banner travels; the other coordinate stays fixed. */
    public enum Axis { X, Z }

    public static final float SPEED = 7f;
    // Flies low and close to the wall so the plane and banner are clearly
    // visible during play, not a speck in the sky.
    public static final float EDGE_HEIGHT = Arena.WALL_HEIGHT + 3f;
    // The one plane crossing directly over the pitch flies higher, clear of
    // the action below it.
    public static final float OVERFLIGHT_HEIGHT = Arena.WALL_HEIGHT + 9f;
    // Far enough ahead of the banner that the two shapes never overlap -
    // the plane is towing it, not sitting inside it.
    public static final float TOW_PLANE_GAP = 9.5f;

    private final Axis axis;
    private final float crossOffset;
    private final float height;
    private final float travelMin;
    private final float travelMax;
    private float pos;

    public SkyBanner(Axis axis, float crossOffset, float height, float startPos) {
        this.axis = axis;
        this.crossOffset = crossOffset;
        this.height = height;
        float half = (axis == Axis.Z ? Arena.HALF_LENGTH : Arena.HALF_WIDTH) + 25f;
        this.travelMin = -half;
        this.travelMax = half;
        this.pos = startPos;
    }

    /** The planes used by the game: one approaching from each side of the pitch, plus one crossing straight over it. */
    public static SkyBanner[] all() {
        float sideX = Arena.HALF_WIDTH + 4f;
        float sideZ = Arena.HALF_LENGTH + 6f;
        return new SkyBanner[] {
                new SkyBanner(Axis.Z, sideX, EDGE_HEIGHT, -Arena.HALF_LENGTH - 25f),
                new SkyBanner(Axis.Z, -sideX, EDGE_HEIGHT, Arena.HALF_LENGTH + 25f),
                new SkyBanner(Axis.X, sideZ, EDGE_HEIGHT, -Arena.HALF_WIDTH - 25f),
                new SkyBanner(Axis.X, -sideZ, EDGE_HEIGHT, Arena.HALF_WIDTH + 25f),
                new SkyBanner(Axis.X, 0f, OVERFLIGHT_HEIGHT, -Arena.HALF_WIDTH - 25f),
        };
    }

    public void update(float dt) {
        pos += SPEED * dt;
        if (pos > travelMax) {
            pos = travelMin;
        }
    }

    public Axis getAxis() {
        return axis;
    }

    public float getCrossOffset() {
        return crossOffset;
    }

    public float getHeight() {
        return height;
    }

    public float getBannerPos() {
        return pos;
    }

    public float getTowPlanePos() {
        return pos + TOW_PLANE_GAP;
    }

    public float getBannerX() {
        return axis == Axis.Z ? crossOffset : pos;
    }

    public float getBannerZ() {
        return axis == Axis.Z ? pos : crossOffset;
    }

    public float getTowPlaneX() {
        return axis == Axis.Z ? crossOffset : getTowPlanePos();
    }

    public float getTowPlaneZ() {
        return axis == Axis.Z ? getTowPlanePos() : crossOffset;
    }
}
