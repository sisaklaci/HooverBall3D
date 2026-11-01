package com.sisak.hooverball.screens;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.sisak.hooverball.Arena;
import com.sisak.hooverball.GameConfig;
import com.sisak.hooverball.GameState;
import com.sisak.hooverball.HooverBallGame;
import com.sisak.hooverball.ai.AIController;
import com.sisak.hooverball.entities.Ball;
import com.sisak.hooverball.entities.SkyBanner;
import com.sisak.hooverball.entities.Vehicle;
import com.sisak.hooverball.graphics.Fonts;
import com.sisak.hooverball.graphics.GameAssets;
import com.sisak.hooverball.graphics.UiButton;
import com.sisak.hooverball.input.AndroidPlayerInput;
import com.sisak.hooverball.input.DesktopPlayerInput;
import com.sisak.hooverball.input.PlayerInput;
import com.sisak.hooverball.physics.Collisions;

/** The 3D match itself: FPS view from the player's hovercraft. */
public class GameScreen implements Screen {
    private static final float GOAL_FREEZE_SECONDS = 1.5f;
    private static final float PITCH_LIMIT = 1.2f; // radians, ~69 degrees
    private static final float EYE_HEIGHT = 1.6f;
    // A clear daytime sky blue, with clouds (see Arena) scattered across it.
    private static final Color SKY_COLOR = new Color(0.40f, 0.71f, 0.96f, 1f);

    private final HooverBallGame game;
    private final GameConfig config;
    private final GameState state;

    private Arena arena;
    private GameAssets assets;
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;

    private Vehicle player;
    private Vehicle ai;
    private Ball ball;
    private AIController aiController;
    private PlayerInput input;

    private ModelInstance aiBodyInstance;
    private ModelInstance aiNoseInstance;
    private ModelInstance aiDomeInstance;
    private ModelInstance aiRimInstance;
    private ModelInstance aiFinInstance;
    private ModelInstance ballInstance;
    private SkyRig[] skyRigs;

    /** A single tow-plane-and-banner pair: its motion state plus every model instance it needs to render. */
    private static final class SkyRig {
        final SkyBanner banner;
        final ModelInstance bannerInstance;
        final ModelInstance fuselage;
        final ModelInstance cockpit;
        final ModelInstance wings;
        final ModelInstance stabilizer;
        final ModelInstance tail;
        final ModelInstance rope;

        SkyRig(SkyBanner banner, ModelInstance bannerInstance, ModelInstance fuselage, ModelInstance cockpit,
               ModelInstance wings, ModelInstance stabilizer, ModelInstance tail, ModelInstance rope) {
            this.banner = banner;
            this.bannerInstance = bannerInstance;
            this.fuselage = fuselage;
            this.cockpit = cockpit;
            this.wings = wings;
            this.stabilizer = stabilizer;
            this.tail = tail;
            this.rope = rope;
        }
    }

    private float yaw;
    private float pitch;
    private float goalPauseTimer;
    private boolean paused;
    private boolean quitRequested;
    private boolean finishedSoundPlayed;
    private boolean boostWasFull = true;

    private SpriteBatch hudBatch;
    private ShapeRenderer hudShapes;
    private BitmapFont hudFont;
    private GlyphLayout layout;
    private OrthographicCamera uiCamera;

    private final UiButton resumeButton = new UiButton("FOLYTATÁS");
    private final UiButton quitButton = new UiButton("KILÉPÉS");

    public GameScreen(HooverBallGame game, GameConfig config) {
        this.game = game;
        this.config = config;
        this.state = new GameState(config);
    }

    @Override
    public void show() {
        arena = new Arena();
        assets = new GameAssets();

        camera = new PerspectiveCamera(70, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 180f;
        camera.up.set(0, 1, 0);

        modelBatch = new ModelBatch();
        environment = new Environment();
        // A bright daytime ambient plus two lights (a warm key and a cool
        // fill from the opposite side) read as far less flat than a single
        // plain grey wash - and the fog ties distant geometry into the sky
        // colour instead of cutting off hard at the far plane.
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.58f, 0.6f, 0.64f, 1f));
        environment.set(ColorAttribute.createFog(SKY_COLOR));
        DirectionalLight keyLight = new DirectionalLight();
        keyLight.set(0.95f, 0.88f, 0.78f, -0.5f, -1f, -0.3f);
        environment.add(keyLight);
        DirectionalLight fillLight = new DirectionalLight();
        fillLight.set(0.22f, 0.28f, 0.42f, 0.6f, -0.4f, 0.55f);
        environment.add(fillLight);

        player = new Vehicle(0, -Arena.HALF_LENGTH + 2.5f, 0f);
        ai = new Vehicle(0, Arena.HALF_LENGTH - 2.5f, MathUtils.PI);
        ball = new Ball(0, 0);
        aiController = new AIController();

        aiBodyInstance = new ModelInstance(assets.vehicleBodyModelRed);
        aiNoseInstance = new ModelInstance(assets.vehicleNoseModelRed);
        aiDomeInstance = new ModelInstance(assets.vehicleDomeModelRed);
        aiRimInstance = new ModelInstance(assets.vehicleRimModelRed);
        aiFinInstance = new ModelInstance(assets.vehicleFinModelRed);
        ballInstance = new ModelInstance(assets.ballModel);
        skyRigs = createSkyRigs();

        input = Gdx.app.getType() == ApplicationType.Android ? new AndroidPlayerInput() : new DesktopPlayerInput();

        hudBatch = new SpriteBatch();
        hudShapes = new ShapeRenderer();
        uiCamera = new OrthographicCamera();
        layout = new GlyphLayout();
        hudFont = Fonts.create(30);

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        game.getAudio().playStart();
        game.getAudio().startMusic();
    }

    private SkyRig[] createSkyRigs() {
        SkyBanner[] banners = SkyBanner.all();
        SkyRig[] rigs = new SkyRig[banners.length];
        for (int i = 0; i < banners.length; i++) {
            rigs[i] = new SkyRig(banners[i],
                    new ModelInstance(assets.skyBannerModels[i]),
                    new ModelInstance(assets.skyTowFuselageModel),
                    new ModelInstance(assets.skyTowCockpitModel),
                    new ModelInstance(assets.skyTowUnitBoxModel),
                    new ModelInstance(assets.skyTowUnitBoxModel),
                    new ModelInstance(assets.skyTowUnitBoxModel),
                    new ModelInstance(assets.skyTowUnitBoxModel));
        }
        return rigs;
    }

    @Override
    public void render(float delta) {
        input.update(delta);
        if (!state.isFinished() && input.consumePauseToggle()) {
            setPaused(!paused);
        }

        if (paused) {
            handlePauseInput();
        } else if (!state.isFinished()) {
            if (goalPauseTimer > 0f) {
                goalPauseTimer -= delta;
            } else {
                updateWorld(delta);
            }
            state.update(delta);
        }

        if (!paused) {
            for (SkyRig rig : skyRigs) {
                rig.banner.update(delta);
            }
        }

        if (state.isFinished() && !finishedSoundPlayed) {
            finishedSoundPlayed = true;
            game.getAudio().playEnd();
            game.getAudio().stopMusic();
        }

        draw();

        // Transitions must happen after draw(): switching screens disposes this
        // screen's GL resources, which would crash if touched again this frame.
        if (quitRequested) {
            game.getAudio().stopMusic();
            game.setScreen(new MenuScreen(game));
            dispose();
        } else if (state.isFinished()) {
            game.setScreen(new GameOverScreen(game, state));
            dispose();
        }
    }

    private void updateWorld(float delta) {
        yaw += input.getYawDelta();
        yaw = yaw % MathUtils.PI2;
        pitch = MathUtils.clamp(pitch + input.getPitchDelta(), -PITCH_LIMIT, PITCH_LIMIT);
        player.yaw = yaw;

        player.updateBoost(input.isBoostPressed(), delta);
        boolean boostFullNow = player.getBoostEnergyRatio() >= 0.999f;
        if (boostFullNow && !boostWasFull) {
            game.getAudio().playBoostReady();
        }
        boostWasFull = boostFullNow;

        float forwardInput = input.getForward();
        float strafeInput = input.getStrafe();
        float fx = MathUtils.sin(yaw);
        float fz = MathUtils.cos(yaw);
        float rx = fz;
        float rz = -fx;
        float mx = fx * forwardInput + rx * strafeInput;
        float mz = fz * forwardInput + rz * strafeInput;
        float len = (float) Math.sqrt(mx * mx + mz * mz);
        float desiredVx = 0f;
        float desiredVz = 0f;
        if (len > 0.0001f) {
            float maxSpeed = player.getMaxSpeed();
            desiredVx = mx / len * maxSpeed;
            desiredVz = mz / len * maxSpeed;
        }
        player.seek(desiredVx, desiredVz, delta);
        player.integrate(delta);

        aiController.update(ai, ball, delta);

        playImpactSound(Collisions.resolveVehicleVehicle(player, ai));
        Collisions.resolveVehicleBall(player, ball);
        Collisions.resolveVehicleBall(ai, ball);

        Ball.GoalResult result = ball.update(delta);
        if (result == Ball.GoalResult.PLAYER_SCORED) {
            state.onPlayerGoal();
            resetAfterGoal();
        } else if (result == Ball.GoalResult.AI_SCORED) {
            state.onAiGoal();
            resetAfterGoal();
        }
    }

    private void resetAfterGoal() {
        ball.reset(0, 0);
        player.reset(0, -Arena.HALF_LENGTH + 2.5f, yaw);
        ai.reset(0, Arena.HALF_LENGTH - 2.5f, MathUtils.PI);
        goalPauseTimer = GOAL_FREEZE_SECONDS;
    }

    private void playImpactSound(float impactSpeed) {
        if (impactSpeed > 1.5f) {
            game.getAudio().playCollision(impactSpeed / 14f);
        }
    }

    private void setPaused(boolean value) {
        paused = value;
        if (input instanceof DesktopPlayerInput) {
            ((DesktopPlayerInput) input).setCursorCaught(!value);
        }
        game.getAudio().setMusicPaused(value);
    }

    private void handlePauseInput() {
        if (!Gdx.input.justTouched()) {
            return;
        }
        float x = Gdx.input.getX();
        float y = Gdx.graphics.getHeight() - Gdx.input.getY();
        if (resumeButton.hit(x, y)) {
            setPaused(false);
        } else if (quitButton.hit(x, y)) {
            quitRequested = true;
        }
    }

    private void draw() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(SKY_COLOR.r, SKY_COLOR.g, SKY_COLOR.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        updateCamera();
        updateAiInstance();
        updateBallInstance();
        updateSkyBannerInstances();

        modelBatch.begin(camera);
        modelBatch.render(arena.getInstances(), environment);
        modelBatch.render(aiBodyInstance, environment);
        modelBatch.render(aiNoseInstance, environment);
        modelBatch.render(aiDomeInstance, environment);
        modelBatch.render(aiRimInstance, environment);
        modelBatch.render(aiFinInstance, environment);
        modelBatch.render(ballInstance, environment);
        for (SkyRig rig : skyRigs) {
            modelBatch.render(rig.bannerInstance, environment);
            modelBatch.render(rig.fuselage, environment);
            modelBatch.render(rig.cockpit, environment);
            modelBatch.render(rig.wings, environment);
            modelBatch.render(rig.stabilizer, environment);
            modelBatch.render(rig.tail, environment);
            modelBatch.render(rig.rope, environment);
        }
        modelBatch.end();

        drawHud();
        if (paused) {
            drawPauseOverlay();
        }
    }

    private void updateCamera() {
        // The craft itself hovers just above the floor, but the pilot's eyes sit
        // at a believable cockpit height; only the small cosmetic bob carries over.
        float eyeY = EYE_HEIGHT + (player.getRenderY() - Vehicle.BASE_HEIGHT);
        camera.position.set(player.x, eyeY, player.z);
        float cosPitch = MathUtils.cos(pitch);
        camera.direction.set(
                MathUtils.sin(yaw) * cosPitch,
                MathUtils.sin(pitch),
                MathUtils.cos(yaw) * cosPitch).nor();
        camera.up.set(0, 1, 0);
        camera.update();
    }

    private void updateAiInstance() {
        float y = ai.getRenderY();
        float degrees = ai.yaw * MathUtils.radiansToDegrees;
        aiBodyInstance.transform.setToTranslation(ai.x, y, ai.z).rotate(Vector3.Y, degrees);
        aiNoseInstance.transform.setToTranslation(ai.x, y, ai.z).rotate(Vector3.Y, degrees)
                .translate(0, 0, 1.25f).rotate(Vector3.X, 90f);
        aiDomeInstance.transform.setToTranslation(ai.x, y + 0.2f, ai.z).rotate(Vector3.Y, degrees)
                .translate(0, 0, 0.3f);
        aiRimInstance.transform.setToTranslation(ai.x, y - 0.2f, ai.z).rotate(Vector3.Y, degrees);
        aiFinInstance.transform.setToTranslation(ai.x, y, ai.z).rotate(Vector3.Y, degrees);
    }

    private void updateBallInstance() {
        float y = ball.getRenderY();
        ballInstance.transform.setToTranslation(ball.x, y, ball.z).rotate(Vector3.Y, ball.spinAngle);
    }

    private void updateSkyBannerInstances() {
        float fuselageLen = 4.5f;
        float fuselageHalf = fuselageLen / 2f;
        float wingSpan = 6f;
        float bannerHalfLen = 5f;
        // The unit capsule model is radius 0.5 and exactly 1 unit long overall
        // (createCapsule's height parameter is the total span, caps included);
        // these factors scale it to the desired fuselage radius and length.
        float fuselageRadiusScale = 0.32f / 0.5f;
        float fuselageLengthScale = fuselageLen;
        float tailOffset = fuselageHalf - 0.35f;

        for (SkyRig rig : skyRigs) {
            SkyBanner banner = rig.banner;
            boolean alongX = banner.getAxis() == SkyBanner.Axis.X;
            float y = banner.getHeight();

            rig.bannerInstance.transform.setToTranslation(banner.getBannerX(), y, banner.getBannerZ());

            float planeX = banner.getTowPlaneX();
            float planeZ = banner.getTowPlaneZ();
            if (!alongX) {
                rig.fuselage.transform.setToTranslation(planeX, y, planeZ)
                        .scale(fuselageRadiusScale, fuselageRadiusScale, fuselageLengthScale)
                        .rotate(Vector3.X, 90f);
                rig.cockpit.transform.setToTranslation(planeX, y, planeZ + fuselageHalf - 0.3f);
                rig.wings.transform.setToTranslation(planeX, y, planeZ).scale(wingSpan, 0.14f, 1f);
                rig.stabilizer.transform.setToTranslation(planeX, y + 0.5f, planeZ - tailOffset).scale(2f, 0.1f, 0.5f);
                rig.tail.transform.setToTranslation(planeX, y + 0.75f, planeZ - tailOffset).scale(0.1f, 1f, 0.65f);
            } else {
                rig.fuselage.transform.setToTranslation(planeX, y, planeZ)
                        .scale(fuselageLengthScale, fuselageRadiusScale, fuselageRadiusScale)
                        .rotate(Vector3.Z, -90f);
                rig.cockpit.transform.setToTranslation(planeX + fuselageHalf - 0.3f, y, planeZ);
                rig.wings.transform.setToTranslation(planeX, y, planeZ).scale(1f, 0.14f, wingSpan);
                rig.stabilizer.transform.setToTranslation(planeX - tailOffset, y + 0.5f, planeZ).scale(0.5f, 0.1f, 2f);
                rig.tail.transform.setToTranslation(planeX - tailOffset, y + 0.75f, planeZ).scale(0.65f, 1f, 0.1f);
            }

            // A short tow rope linking the banner's leading edge to the plane's tail.
            float ropeFrom = banner.getBannerPos() + bannerHalfLen;
            float ropeTo = banner.getTowPlanePos() - fuselageHalf;
            float ropeLen = Math.max(0.3f, ropeTo - ropeFrom);
            float ropeCenter = (ropeFrom + ropeTo) / 2f;
            if (!alongX) {
                rig.rope.transform.setToTranslation(banner.getCrossOffset(), y, ropeCenter).scale(0.08f, 0.08f, ropeLen);
            } else {
                rig.rope.transform.setToTranslation(ropeCenter, y, banner.getCrossOffset()).scale(ropeLen, 0.08f, 0.08f);
            }
        }
    }

    private void drawHud() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        hudBatch.setProjectionMatrix(uiCamera.combined);
        hudBatch.begin();
        String score = "Játékos " + state.playerScore + " : " + state.aiScore + " Gép";
        drawCentered(score, w / 2f, h - 20f);

        String timeStr = config.mode == GameConfig.Mode.TIME_LIMIT
                ? formatTime(state.getRemainingSeconds())
                : formatTime(state.elapsedSeconds);
        drawCentered(timeStr, w / 2f, h - 55f);

        if (goalPauseTimer > 0f) {
            drawCentered("GÓL!", w / 2f, h * 0.6f);
        }
        hudBatch.end();

        hudShapes.setProjectionMatrix(uiCamera.combined);
        hudShapes.begin(ShapeType.Line);
        hudShapes.setColor(Color.WHITE);
        float cx = w / 2f;
        float cy = h / 2f;
        hudShapes.line(cx - 10, cy, cx + 10, cy);
        hudShapes.line(cx, cy - 10, cx, cy + 10);
        hudShapes.end();

        drawBoostBar();

        if (input instanceof AndroidPlayerInput) {
            drawTouchControlsOverlay((AndroidPlayerInput) input);
        }
    }

    private void drawBoostBar() {
        float barWidth = 220f;
        float barHeight = 22f;
        float x = 24f;
        float y = 24f;

        hudShapes.begin(ShapeType.Filled);
        hudShapes.setColor(0f, 0f, 0f, 0.4f);
        hudShapes.rect(x, y, barWidth, barHeight);
        float ratio = player.getBoostEnergyRatio();
        hudShapes.setColor(player.isBoosting() ? new Color(1f, 0.65f, 0.1f, 1f) : new Color(0.2f, 0.75f, 0.95f, 1f));
        hudShapes.rect(x, y, barWidth * ratio, barHeight);
        hudShapes.end();

        hudBatch.begin();
        hudFont.draw(hudBatch, "BOOSTER", x, y + barHeight + 28f);
        hudBatch.end();
    }

    private void drawTouchControlsOverlay(AndroidPlayerInput touchInput) {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        hudShapes.begin(ShapeType.Filled);
        hudShapes.setColor(1f, 1f, 1f, 0.22f);
        hudShapes.rect(
                w - AndroidPlayerInput.PAUSE_BUTTON_MARGIN - AndroidPlayerInput.PAUSE_BUTTON_SIZE,
                h - AndroidPlayerInput.PAUSE_BUTTON_MARGIN - AndroidPlayerInput.PAUSE_BUTTON_SIZE,
                AndroidPlayerInput.PAUSE_BUTTON_SIZE, AndroidPlayerInput.PAUSE_BUTTON_SIZE);
        hudShapes.end();

        if (touchInput.isJoystickActive()) {
            hudShapes.begin(ShapeType.Line);
            hudShapes.setColor(1f, 1f, 1f, 0.6f);
            hudShapes.circle(touchInput.getJoystickOriginX(), h - touchInput.getJoystickOriginY(),
                    AndroidPlayerInput.JOYSTICK_RADIUS, 28);
            hudShapes.end();
            hudShapes.begin(ShapeType.Filled);
            hudShapes.setColor(1f, 1f, 1f, 0.5f);
            hudShapes.circle(touchInput.getJoystickCurrentX(), h - touchInput.getJoystickCurrentY(), 28, 20);
            hudShapes.end();
        }

        float boostCx = touchInput.getBoostButtonX();
        float boostCy = h - touchInput.getBoostButtonY();
        hudShapes.begin(ShapeType.Filled);
        hudShapes.setColor(touchInput.isBoostPressed() ? new Color(1f, 0.65f, 0.1f, 0.65f) : new Color(1f, 1f, 1f, 0.25f));
        hudShapes.circle(boostCx, boostCy, AndroidPlayerInput.BOOST_BUTTON_RADIUS, 28);
        hudShapes.end();
        hudBatch.begin();
        layout.setText(hudFont, "BOOST");
        hudFont.draw(hudBatch, "BOOST", boostCx - layout.width / 2f, boostCy + layout.height / 2f);
        hudBatch.end();
    }

    private void drawPauseOverlay() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        hudShapes.begin(ShapeType.Filled);
        hudShapes.setColor(0f, 0f, 0f, 0.6f);
        hudShapes.rect(0, 0, w, h);
        hudShapes.setColor(0.25f, 0.5f, 0.85f, 1f);
        hudShapes.rect(resumeButton.rect.x, resumeButton.rect.y, resumeButton.rect.width, resumeButton.rect.height);
        hudShapes.setColor(0.55f, 0.2f, 0.2f, 1f);
        hudShapes.rect(quitButton.rect.x, quitButton.rect.y, quitButton.rect.width, quitButton.rect.height);
        hudShapes.end();

        hudBatch.begin();
        drawCentered("SZÜNET", w / 2f, h * 0.68f);
        drawButtonLabel(resumeButton);
        drawButtonLabel(quitButton);
        hudBatch.end();
    }

    private void drawCentered(String text, float cx, float cy) {
        layout.setText(hudFont, text);
        hudFont.draw(hudBatch, text, cx - layout.width / 2f, cy + layout.height / 2f);
    }

    private void drawButtonLabel(UiButton b) {
        layout.setText(hudFont, b.label);
        hudFont.draw(hudBatch, b.label, b.rect.x + b.rect.width / 2f - layout.width / 2f,
                b.rect.y + b.rect.height / 2f + layout.height / 2f);
    }

    private String formatTime(float seconds) {
        int total = Math.max(0, (int) seconds);
        int minutes = total / 60;
        int secs = total % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();

        uiCamera.setToOrtho(false, width, height);

        resumeButton.rect.set(width / 2f - 160, height * 0.45f, 320, 85);
        quitButton.rect.set(width / 2f - 160, height * 0.28f, 320, 85);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        arena.dispose();
        assets.dispose();
        hudBatch.dispose();
        hudShapes.dispose();
        hudFont.dispose();
        input.dispose();
    }
}
