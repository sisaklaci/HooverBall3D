package com.sisak.hooverball;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.sisak.hooverball.graphics.TextSignFactory;

import java.util.Random;

/**
 * Fixed dimensions of the futsal-sized court plus the procedural geometry
 * (floor, rounded walls, goal frames and pitch markings) used to render it.
 * All gameplay code treats the court as the rectangle [-HALF_WIDTH,
 * HALF_WIDTH] x [-HALF_LENGTH, HALF_LENGTH] on the X/Z plane, with its four
 * corners rounded off by {@link #CORNER_RADIUS} - both visually (the walls
 * actually curve there) and physically (see Vehicle/Ball's own corner
 * clamping, which uses this same radius so nothing can wedge into a corner).
 */
public class Arena implements Disposable {
    public static final float HALF_WIDTH = 16f;
    public static final float HALF_LENGTH = 30f;
    public static final float GOAL_HALF_WIDTH = 3f;
    public static final float GOAL_HEIGHT = 2.4f;
    public static final float GOAL_DEPTH = 1.6f;
    public static final float WALL_HEIGHT = 4f;
    public static final float CORNER_RADIUS = 6f;
    private static final float WALL_THICKNESS = 0.3f;
    private static final float FLOOR_CELL_SIZE = 1.5f;

    public static final String SPONSOR_A = "helmetsoft.com";
    public static final String SPONSOR_B = "refsztg.hu";

    private final Array<ModelInstance> instances = new Array<ModelInstance>();
    private final Array<Model> ownedModels = new Array<Model>();
    private final Array<Texture> ownedTextures = new Array<Texture>();
    private Texture floorTexture;

    public Arena() {
        ModelBuilder mb = new ModelBuilder();
        buildFloor(mb);
        buildMarkings(mb);
        buildSideWalls(mb);
        buildEndWalls(mb);
        buildCornerWalls(mb);
        buildGoalFrames(mb);
        buildSideWallSigns(mb);
        buildClouds(mb);
    }

    public Array<ModelInstance> getInstances() {
        return instances;
    }

    private Model box(ModelBuilder mb, float sx, float sy, float sz, Color color) {
        return box(mb, sx, sy, sz, color, null);
    }

    /** Boxes get a touch of specular highlight by default - flat, unlit-looking matte surfaces read as "cheap". */
    private Model box(ModelBuilder mb, float sx, float sy, float sz, Color color, Color emissive) {
        Material mat = emissive != null
                ? new Material(ColorAttribute.createDiffuse(color), ColorAttribute.createSpecular(0.25f, 0.25f, 0.28f, 1f),
                        FloatAttribute.createShininess(12f), ColorAttribute.createEmissive(emissive))
                : new Material(ColorAttribute.createDiffuse(color), ColorAttribute.createSpecular(0.25f, 0.25f, 0.28f, 1f),
                        FloatAttribute.createShininess(12f));
        Model model = mb.createBox(sx, sy, sz, mat, Usage.Position | Usage.Normal);
        ownedModels.add(model);
        return model;
    }

    private void addAt(Model model, float x, float y, float z) {
        ModelInstance instance = new ModelInstance(model);
        instance.transform.setToTranslation(x, y, z);
        instances.add(instance);
    }

    private void buildFloor(ModelBuilder mb) {
        floorTexture = createFloorTexture();
        Material floorMaterial = new Material(
                TextureAttribute.createDiffuse(floorTexture),
                ColorAttribute.createSpecular(0.22f, 0.22f, 0.26f, 1f),
                FloatAttribute.createShininess(10f));
        Model floor = mb.createBox(HALF_WIDTH * 2f, 0.2f, HALF_LENGTH * 2f, floorMaterial,
                Usage.Position | Usage.Normal | Usage.TextureCoordinates);
        ownedModels.add(floor);
        addAt(floor, 0, -0.1f, 0);
    }

    /** A stylish slate/teal checkerboard rather than a flat green pitch. */
    private Texture createFloorTexture() {
        int cellsX = Math.max(2, Math.round(HALF_WIDTH * 2f / FLOOR_CELL_SIZE));
        int cellsZ = Math.max(2, Math.round(HALF_LENGTH * 2f / FLOOR_CELL_SIZE));
        int pixelsPerCell = 16;
        int width = cellsX * pixelsPerCell;
        int height = cellsZ * pixelsPerCell;

        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        Color dark = Color.valueOf("3C5A82");
        Color light = Color.valueOf("5479A8");
        for (int cz = 0; cz < cellsZ; cz++) {
            for (int cx = 0; cx < cellsX; cx++) {
                boolean isDark = (cx + cz) % 2 == 0;
                pixmap.setColor(isDark ? dark : light);
                pixmap.fillRectangle(cx * pixelsPerCell, cz * pixelsPerCell, pixelsPerCell, pixelsPerCell);
            }
        }
        Texture texture = new Texture(pixmap, true);
        texture.setFilter(TextureFilter.MipMapLinearLinear, TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private void buildMarkings(ModelBuilder mb) {
        Color line = Color.valueOf("ECEFF4");
        Color lineGlow = Color.valueOf("4D5566");
        // Halfway line.
        Model halfway = box(mb, HALF_WIDTH * 2f, 0.02f, 0.18f, line, lineGlow);
        addAt(halfway, 0, 0.011f, 0);
        // Centre spot.
        Model spot = box(mb, 0.35f, 0.02f, 0.35f, line, lineGlow);
        addAt(spot, 0, 0.012f, 0);
        // Goal lines.
        Model goalLineFar = box(mb, HALF_WIDTH * 2f, 0.02f, 0.18f, line, lineGlow);
        addAt(goalLineFar, 0, 0.011f, HALF_LENGTH);
        Model goalLineNear = box(mb, HALF_WIDTH * 2f, 0.02f, 0.18f, line, lineGlow);
        addAt(goalLineNear, 0, 0.011f, -HALF_LENGTH);
        // Outer boundary lines.
        Model sideLineA = box(mb, 0.18f, 0.02f, HALF_LENGTH * 2f, line, lineGlow);
        addAt(sideLineA, -HALF_WIDTH, 0.011f, 0);
        Model sideLineB = box(mb, 0.18f, 0.02f, HALF_LENGTH * 2f, line, lineGlow);
        addAt(sideLineB, HALF_WIDTH, 0.011f, 0);
    }

    /** Side walls now stop short of each corner, leaving room for the rounded corner arc. */
    private void buildSideWalls(ModelBuilder mb) {
        Color wallColor = Color.valueOf("222C40");
        float length = HALF_LENGTH * 2f - CORNER_RADIUS * 2f;
        Model wall = box(mb, WALL_THICKNESS, WALL_HEIGHT, length, wallColor);
        addAt(wall, -(HALF_WIDTH + WALL_THICKNESS / 2f), WALL_HEIGHT / 2f, 0);
        addAt(wall, HALF_WIDTH + WALL_THICKNESS / 2f, WALL_HEIGHT / 2f, 0);

        Model trim = buildWallTrim(mb, length, true);
        addAt(trim, -(HALF_WIDTH + WALL_THICKNESS / 2f), WALL_HEIGHT - 0.05f, 0);
        addAt(trim, HALF_WIDTH + WALL_THICKNESS / 2f, WALL_HEIGHT - 0.05f, 0);
    }

    /** End wall segments now stop short of the corners too, on the side away from the goal gap. */
    private void buildEndWalls(ModelBuilder mb) {
        Color wallColor = Color.valueOf("222C40");
        float segWidth = HALF_WIDTH - CORNER_RADIUS - GOAL_HALF_WIDTH;
        Model segment = box(mb, segWidth, WALL_HEIGHT, WALL_THICKNESS, wallColor);
        Model trim = buildWallTrim(mb, segWidth, false);
        float segCenterX = GOAL_HALF_WIDTH + segWidth / 2f;
        for (float z : new float[] {-HALF_LENGTH, HALF_LENGTH}) {
            float zPos = z + Math.signum(z) * WALL_THICKNESS / 2f;
            addAt(segment, -segCenterX, WALL_HEIGHT / 2f, zPos);
            addAt(segment, segCenterX, WALL_HEIGHT / 2f, zPos);
            addAt(trim, -segCenterX, WALL_HEIGHT - 0.05f, zPos);
            addAt(trim, segCenterX, WALL_HEIGHT - 0.05f, zPos);
        }
    }

    /** A thin glowing cyan strip along a wall's top edge - a cheap but effective neon accent. */
    private Model buildWallTrim(ModelBuilder mb, float length, boolean alongZ) {
        Color trimColor = Color.valueOf("26C6DA");
        float sx = alongZ ? WALL_THICKNESS + 0.04f : length;
        float sz = alongZ ? length : WALL_THICKNESS + 0.04f;
        return box(mb, sx, 0.08f, sz, trimColor, trimColor);
    }

    /** The four quarter-circle wall arcs that round off the pitch's corners, each with its own neon trim. */
    private void buildCornerWalls(ModelBuilder mb) {
        Color wallColor = Color.valueOf("222C40");
        Color trimColor = Color.valueOf("26C6DA");
        float half = MathUtils.PI / 2f;
        // {signX, signZ, startAngle, endAngle} - angle 0 points +Z, PI/2 points +X (see buildArc).
        float[][] corners = {
                {1, 1, 0f, half},
                {1, -1, MathUtils.PI, half},
                {-1, 1, 0f, -half},
                {-1, -1, MathUtils.PI, MathUtils.PI + half},
        };
        for (float[] c : corners) {
            float centerX = c[0] * (HALF_WIDTH - CORNER_RADIUS);
            float centerZ = c[1] * (HALF_LENGTH - CORNER_RADIUS);
            instances.add(buildArc(mb, centerX, centerZ, CORNER_RADIUS, c[2], c[3], 0f, WALL_HEIGHT, wallColor, null));
            instances.add(buildArc(mb, centerX, centerZ, CORNER_RADIUS, c[2], c[3],
                    WALL_HEIGHT - 0.08f, WALL_HEIGHT, trimColor, trimColor));
        }
    }

    /**
     * A curved wall strip following an arc of the given radius around
     * (centerX, centerZ), from startAngle to endAngle (0 = +Z, PI/2 = +X),
     * between yLow and yHigh. Its own (world-space) coordinates are baked
     * straight into the mesh, so the returned instance uses an identity
     * transform. Face culling is disabled so it's never accidentally
     * invisible from one side regardless of winding.
     */
    private ModelInstance buildArc(ModelBuilder mb, float centerX, float centerZ, float radius,
            float startAngle, float endAngle, float yLow, float yHigh, Color color, Color emissive) {
        Material material = emissive != null
                ? new Material(ColorAttribute.createDiffuse(color), ColorAttribute.createEmissive(emissive),
                        IntAttribute.createCullFace(GL20.GL_NONE))
                : new Material(ColorAttribute.createDiffuse(color),
                        ColorAttribute.createSpecular(0.25f, 0.25f, 0.28f, 1f), FloatAttribute.createShininess(12f),
                        IntAttribute.createCullFace(GL20.GL_NONE));
        int segments = 10;

        mb.begin();
        MeshPartBuilder part = mb.part("arc", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, material);
        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments;
            float t1 = (float) (i + 1) / segments;
            float a0 = startAngle + (endAngle - startAngle) * t0;
            float a1 = startAngle + (endAngle - startAngle) * t1;

            float sin0 = MathUtils.sin(a0);
            float cos0 = MathUtils.cos(a0);
            float sin1 = MathUtils.sin(a1);
            float cos1 = MathUtils.cos(a1);
            float x0 = centerX + radius * sin0;
            float z0 = centerZ + radius * cos0;
            float x1 = centerX + radius * sin1;
            float z1 = centerZ + radius * cos1;
            // Normals point inward (towards the corner centre), matching a concave wall seen from inside the pitch.
            VertexInfo v00 = new VertexInfo().setPos(x0, yLow, z0).setNor(-sin0, 0, -cos0);
            VertexInfo v10 = new VertexInfo().setPos(x1, yLow, z1).setNor(-sin1, 0, -cos1);
            VertexInfo v11 = new VertexInfo().setPos(x1, yHigh, z1).setNor(-sin1, 0, -cos1);
            VertexInfo v01 = new VertexInfo().setPos(x0, yHigh, z0).setNor(-sin0, 0, -cos0);
            part.rect(v00, v10, v11, v01);
        }
        Model model = mb.end();
        ownedModels.add(model);
        return new ModelInstance(model);
    }

    /** Advertising signs on every other stretch of both side walls, cycling through the sponsors. */
    private void buildSideWallSigns(ModelBuilder mb) {
        String[] sponsors = {SPONSOR_A, SPONSOR_B};
        float signHalfW = 3.2f;
        float signHalfH = 1.4f;
        float signY = WALL_HEIGHT * 0.55f;
        int segments = 6;
        float wallLength = HALF_LENGTH * 2f - CORNER_RADIUS * 2f;
        float segmentLen = wallLength / segments;
        float wallStart = -HALF_LENGTH + CORNER_RADIUS;

        Model[] signsFacePosX = new Model[sponsors.length];
        Model[] signsFaceNegX = new Model[sponsors.length];
        for (int i = 0; i < sponsors.length; i++) {
            Texture texture = signTexture(sponsors[i]);
            signsFacePosX[i] = buildFlatWallSign(mb, texture, signHalfW, signHalfH, true);
            signsFaceNegX[i] = buildFlatWallSign(mb, texture, signHalfW, signHalfH, false);
        }

        int signIndex = 0;
        for (int i = 1; i < segments; i += 2) {
            float z = wallStart + (i + 0.5f) * segmentLen;
            int sponsorIndex = signIndex % sponsors.length;
            addAt(signsFacePosX[sponsorIndex], -HALF_WIDTH, signY, z);
            addAt(signsFaceNegX[sponsorIndex], HALF_WIDTH, signY, z);
            signIndex++;
        }
    }

    /** A flat sign mounted just off a side wall's inner face, facing +X or -X into the pitch. */
    private Model buildFlatWallSign(ModelBuilder mb, Texture texture, float halfW, float halfH, boolean facePositiveX) {
        float offset = 0.08f;
        float x = facePositiveX ? offset : -offset;
        float nx = facePositiveX ? 1f : -1f;
        Material material = new Material(TextureAttribute.createDiffuse(texture));
        if (facePositiveX) {
            return mb.createRect(
                    x, -halfH, halfW, x, -halfH, -halfW, x, halfH, -halfW, x, halfH, halfW,
                    nx, 0, 0, material, Usage.Position | Usage.Normal | Usage.TextureCoordinates);
        } else {
            return mb.createRect(
                    x, -halfH, -halfW, x, -halfH, halfW, x, halfH, halfW, x, halfH, -halfW,
                    nx, 0, 0, material, Usage.Position | Usage.Normal | Usage.TextureCoordinates);
        }
    }

    private void buildGoalFrames(ModelBuilder mb) {
        Color frameColor = Color.WHITE;
        Color frameGlow = Color.valueOf("4A4A55");
        float postThickness = 0.15f;
        Model post = box(mb, postThickness, GOAL_HEIGHT, postThickness, frameColor, frameGlow);
        Model bar = box(mb, GOAL_HALF_WIDTH * 2f + postThickness, postThickness, postThickness, frameColor, frameGlow);
        Model backPost = box(mb, postThickness, GOAL_HEIGHT, GOAL_DEPTH, frameColor, frameGlow);
        for (float z : new float[] {-HALF_LENGTH, HALF_LENGTH}) {
            addAt(post, -GOAL_HALF_WIDTH, GOAL_HEIGHT / 2f, z);
            addAt(post, GOAL_HALF_WIDTH, GOAL_HEIGHT / 2f, z);
            addAt(bar, 0, GOAL_HEIGHT, z);
            float backZ = z + Math.signum(z) * GOAL_DEPTH / 2f;
            addAt(backPost, -GOAL_HALF_WIDTH, GOAL_HEIGHT / 2f, backZ);
            addAt(backPost, GOAL_HALF_WIDTH, GOAL_HEIGHT / 2f, backZ);
        }
    }

    private Texture signTexture(String text) {
        Texture texture = TextSignFactory.create(text, 640, 160, Color.valueOf("B71C1C"), Color.WHITE, 92);
        ownedTextures.add(texture);
        return texture;
    }

    /** A handful of fluffy white clouds scattered high over and around the pitch. */
    private void buildClouds(ModelBuilder mb) {
        Material cloudMaterial = new Material(
                ColorAttribute.createDiffuse(Color.WHITE),
                ColorAttribute.createEmissive(0.18f, 0.18f, 0.18f, 1f));
        Model puff = mb.createSphere(7f, 3.2f, 7f, 12, 12, cloudMaterial, Usage.Position | Usage.Normal);
        ownedModels.add(puff);

        float[][] cloudCenters = {
                {-45, 46, -25}, {35, 52, 8}, {-12, 42, 45}, {55, 50, -35},
                {-65, 48, 15}, {18, 45, -55}, {62, 53, 35}, {-38, 50, 65},
        };
        Random rnd = new Random(7);
        for (float[] center : cloudCenters) {
            int puffs = 3 + rnd.nextInt(2);
            for (int i = 0; i < puffs; i++) {
                float ox = (rnd.nextFloat() - 0.5f) * 11f;
                float oy = (rnd.nextFloat() - 0.5f) * 2f;
                float oz = (rnd.nextFloat() - 0.5f) * 9f;
                float scale = 0.7f + rnd.nextFloat() * 0.6f;
                ModelInstance instance = new ModelInstance(puff);
                instance.transform.setToTranslation(center[0] + ox, center[1] + oy, center[2] + oz)
                        .scale(scale, scale, scale);
                instances.add(instance);
            }
        }
    }

    @Override
    public void dispose() {
        for (Model model : ownedModels) {
            model.dispose();
        }
        for (Texture texture : ownedTextures) {
            texture.dispose();
        }
        floorTexture.dispose();
    }
}
