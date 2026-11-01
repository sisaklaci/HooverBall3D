package com.sisak.hooverball.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.sisak.hooverball.Arena;
import com.sisak.hooverball.entities.SkyBanner;

/**
 * All 3D models and textures are generated procedurally so the game ships
 * with zero external art assets.
 */
public class GameAssets implements Disposable {
    public final Texture ballTexture;
    public final Model ballModel;
    public final Model vehicleBodyModelRed;
    public final Model vehicleBodyModelBlue;
    public final Model vehicleNoseModelRed;
    public final Model vehicleNoseModelBlue;
    public final Model vehicleDomeModelRed;
    public final Model vehicleDomeModelBlue;
    public final Model vehicleRimModelRed;
    public final Model vehicleRimModelBlue;
    public final Model vehicleFinModelRed;
    public final Model vehicleFinModelBlue;
    /** One banner model per plane from {@link SkyBanner#all()} - each advertises a single sponsor, facing the right way for its side. */
    public final Model[] skyBannerModels;
    private final Texture[] skyBannerTextures;
    /** A plain 1x1x1 box, reused at different scales/positions for the tow plane's wings, tail, stabilizers and tow rope. */
    public final Model skyTowUnitBoxModel;
    /** A smooth unit capsule (rounded nose and tail) reused and rotated for the tow plane's fuselage, in place of a boxy cube. */
    public final Model skyTowFuselageModel;
    /** A small dark sphere marking the cockpit canopy at the nose of the fuselage. */
    public final Model skyTowCockpitModel;

    public GameAssets() {
        ballTexture = createCheckerTexture();
        ModelBuilder mb = new ModelBuilder();

        Material ballMaterial = new Material(
                TextureAttribute.createDiffuse(ballTexture),
                ColorAttribute.createSpecular(Color.WHITE),
                FloatAttribute.createShininess(28f));
        // A flattened disc rather than a perfect sphere, fitting the hoverball theme.
        ballModel = mb.createSphere(0.82f, 0.3f, 0.82f, 32, 32, ballMaterial, Usage.Position | Usage.Normal | Usage.TextureCoordinates);

        vehicleBodyModelRed = createVehicleBody(mb, Color.valueOf("B71C1C"));
        vehicleBodyModelBlue = createVehicleBody(mb, Color.valueOf("0D47A1"));
        vehicleNoseModelRed = createVehicleNose(mb, Color.valueOf("D32F2F"));
        vehicleNoseModelBlue = createVehicleNose(mb, Color.valueOf("1565C0"));
        vehicleDomeModelRed = createVehicleDome(mb, Color.valueOf("FF8A65"));
        vehicleDomeModelBlue = createVehicleDome(mb, Color.valueOf("82B1FF"));
        vehicleRimModelRed = createVehicleRim(mb, Color.valueOf("FF5252"));
        vehicleRimModelBlue = createVehicleRim(mb, Color.valueOf("448AFF"));
        vehicleFinModelRed = createVehicleFins(mb, Color.valueOf("8D1313"));
        vehicleFinModelBlue = createVehicleFins(mb, Color.valueOf("0A3880"));

        String[] sponsors = {Arena.SPONSOR_A, Arena.SPONSOR_B};
        SkyBanner[] planeConfigs = SkyBanner.all();
        skyBannerModels = new Model[planeConfigs.length];
        skyBannerTextures = new Texture[planeConfigs.length];
        for (int i = 0; i < planeConfigs.length; i++) {
            String sponsor = sponsors[i % sponsors.length].toUpperCase();
            Texture tex = TextSignFactory.create(sponsor, 900, 220, Color.valueOf("B71C1C"), Color.WHITE, 128);
            skyBannerTextures[i] = tex;
            boolean alongX = planeConfigs[i].getAxis() == SkyBanner.Axis.X;
            boolean positive = planeConfigs[i].getCrossOffset() < 0;
            skyBannerModels[i] = createSkyBanner(mb, tex, alongX, positive);
        }

        Material towPlaneMaterial = new Material(
                ColorAttribute.createDiffuse(Color.valueOf("F5F6F8")),
                ColorAttribute.createSpecular(Color.WHITE),
                FloatAttribute.createShininess(48f));
        skyTowUnitBoxModel = mb.createBox(1f, 1f, 1f, towPlaneMaterial, Usage.Position | Usage.Normal);
        // Unit length/radius; the caller scales and rotates this to the needed
        // fuselage length and orientation per banner side.
        skyTowFuselageModel = mb.createCapsule(0.5f, 1f, 16, towPlaneMaterial, Usage.Position | Usage.Normal);
        Material cockpitMaterial = new Material(
                ColorAttribute.createDiffuse(Color.valueOf("263238")),
                ColorAttribute.createSpecular(Color.WHITE),
                FloatAttribute.createShininess(80f));
        skyTowCockpitModel = mb.createSphere(0.5f, 0.42f, 0.5f, 14, 14, cockpitMaterial, Usage.Position | Usage.Normal);
    }

    /** A low, angular hull - the main chassis of a sleek hover-pod instead of a flattened sphere. */
    private Model createVehicleBody(ModelBuilder mb, Color color) {
        Material mat = new Material(
                ColorAttribute.createDiffuse(color),
                ColorAttribute.createSpecular(Color.WHITE),
                FloatAttribute.createShininess(32f));
        return mb.createBox(1.7f, 0.42f, 2.1f, mat, Usage.Position | Usage.Normal);
    }

    /** A faceted, low-poly nose cone mounted at the front of the hull - the vehicle's forward-pointing wedge tip. */
    private Model createVehicleNose(ModelBuilder mb, Color color) {
        Material mat = new Material(
                ColorAttribute.createDiffuse(color),
                ColorAttribute.createSpecular(Color.WHITE),
                FloatAttribute.createShininess(32f));
        // Built as an upright cone (apex on +Y); the caller rotates it 90 degrees
        // around X so the apex points along local +Z - forward - instead.
        return mb.createCone(1.5f, 1.1f, 0.42f, 6, mat, Usage.Position | Usage.Normal);
    }

    /** A low, forward-swept cockpit canopy, embedded into the hull's silhouette rather than perched on top of it. */
    private Model createVehicleDome(ModelBuilder mb, Color color) {
        Material mat = new Material(
                ColorAttribute.createDiffuse(color),
                ColorAttribute.createEmissive(color.r * 0.25f, color.g * 0.25f, color.b * 0.25f, 1f),
                ColorAttribute.createSpecular(Color.WHITE),
                FloatAttribute.createShininess(50f));
        return mb.createSphere(0.95f, 0.3f, 1.1f, 20, 20, mat, Usage.Position | Usage.Normal);
    }

    /** A thin glowing underskirt ring, like a hovercraft's lift fan - adds silhouette and a neon accent. */
    private Model createVehicleRim(ModelBuilder mb, Color glowColor) {
        Material mat = new Material(
                ColorAttribute.createDiffuse(glowColor),
                ColorAttribute.createEmissive(glowColor));
        return mb.createCylinder(2.1f, 0.08f, 2.5f, 28, mat, Usage.Position | Usage.Normal);
    }

    /** A pair of swept rear stabilizer fins, angled outward - combined into one model so only one instance is needed per vehicle. */
    private Model createVehicleFins(ModelBuilder mb, Color color) {
        Material mat = new Material(
                ColorAttribute.createDiffuse(color),
                ColorAttribute.createSpecular(Color.WHITE),
                FloatAttribute.createShininess(20f));
        mb.begin();
        MeshPartBuilder part = mb.part("fins", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, mat);
        float finW = 0.12f;
        float finH = 0.34f;
        float finD = 0.6f;
        float xOffset = 0.78f;
        float zOffset = -0.55f;
        Matrix4 left = new Matrix4().setToTranslation(-xOffset, 0f, zOffset).rotate(Vector3.Y, 22f).scale(finW, finH, finD);
        Matrix4 right = new Matrix4().setToTranslation(xOffset, 0f, zOffset).rotate(Vector3.Y, -22f).scale(finW, finH, finD);
        part.box(left);
        part.box(right);
        return mb.end();
    }

    /**
     * A flat banner strip, long axis along the direction of travel, facing
     * towards the pitch - one geometry variant per side the plane flies on.
     *
     * @param alongX   true if the plane travels along X (so the banner's long
     *                 axis is X and it lies at a fixed Z); false for travel
     *                 along Z (long axis Z, fixed X).
     * @param positive true if the banner should face the +X/+Z direction
     *                 (i.e. it's mounted on the -X/-Z side looking inward).
     */
    private Model createSkyBanner(ModelBuilder mb, Texture texture, boolean alongX, boolean positive) {
        float halfLen = 5f;
        float halfH = 2f;
        Material material = new Material(TextureAttribute.createDiffuse(texture));
        if (!alongX) {
            float nx = positive ? 1f : -1f;
            if (positive) {
                return mb.createRect(
                        0, -halfH, halfLen, 0, -halfH, -halfLen, 0, halfH, -halfLen, 0, halfH, halfLen,
                        nx, 0, 0, material, Usage.Position | Usage.Normal | Usage.TextureCoordinates);
            } else {
                return mb.createRect(
                        0, -halfH, -halfLen, 0, -halfH, halfLen, 0, halfH, halfLen, 0, halfH, -halfLen,
                        nx, 0, 0, material, Usage.Position | Usage.Normal | Usage.TextureCoordinates);
            }
        } else {
            float nz = positive ? 1f : -1f;
            if (positive) {
                return mb.createRect(
                        -halfLen, -halfH, 0, halfLen, -halfH, 0, halfLen, halfH, 0, -halfLen, halfH, 0,
                        0, 0, nz, material, Usage.Position | Usage.Normal | Usage.TextureCoordinates);
            } else {
                return mb.createRect(
                        halfLen, -halfH, 0, -halfLen, -halfH, 0, -halfLen, halfH, 0, halfLen, halfH, 0,
                        0, 0, nz, material, Usage.Position | Usage.Normal | Usage.TextureCoordinates);
            }
        }
    }

    private Texture createCheckerTexture() {
        int size = 256;
        int cells = 8;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        Color a = Color.valueOf("FFC400");
        Color b = Color.valueOf("FF6D00");
        int cellSize = size / cells;
        for (int cy = 0; cy < cells; cy++) {
            for (int cx = 0; cx < cells; cx++) {
                boolean isA = (cx + cy) % 2 == 0;
                pixmap.setColor(isA ? a : b);
                pixmap.fillRectangle(cx * cellSize, cy * cellSize, cellSize, cellSize);
            }
        }
        Texture texture = new Texture(pixmap, true);
        texture.setFilter(TextureFilter.MipMapLinearLinear, TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void dispose() {
        ballTexture.dispose();
        ballModel.dispose();
        vehicleBodyModelRed.dispose();
        vehicleBodyModelBlue.dispose();
        vehicleNoseModelRed.dispose();
        vehicleNoseModelBlue.dispose();
        vehicleDomeModelRed.dispose();
        vehicleDomeModelBlue.dispose();
        vehicleRimModelRed.dispose();
        vehicleRimModelBlue.dispose();
        vehicleFinModelRed.dispose();
        vehicleFinModelBlue.dispose();
        for (Texture texture : skyBannerTextures) {
            texture.dispose();
        }
        for (Model model : skyBannerModels) {
            model.dispose();
        }
        skyTowUnitBoxModel.dispose();
        skyTowFuselageModel.dispose();
        skyTowCockpitModel.dispose();
    }
}
