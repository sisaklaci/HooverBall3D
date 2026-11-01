package com.sisak.hooverball.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.sisak.hooverball.GameConfig;
import com.sisak.hooverball.HooverBallGame;
import com.sisak.hooverball.graphics.Fonts;
import com.sisak.hooverball.graphics.UiButton;

/** Lets the player pick score-limit vs. time-limit play before starting a match. */
public class MenuScreen implements Screen {
    private final HooverBallGame game;
    private SpriteBatch batch;
    private BitmapFont titleFont;
    private BitmapFont font;
    private BitmapFont creditFont;
    private ShapeRenderer shapes;
    private OrthographicCamera camera;
    private GlyphLayout layout;

    private GameConfig.Mode selectedMode = GameConfig.Mode.SCORE_LIMIT;
    private int selectedScoreLimit = 5;
    private float selectedMinutes = 5f;
    private boolean startRequested;

    private final UiButton scoreModeButton = new UiButton("Pontszámra");
    private final UiButton timeModeButton = new UiButton("Időre");
    private final Array<UiButton> limitButtons = new Array<UiButton>();
    private final UiButton startButton = new UiButton("INDÍTÁS");

    public MenuScreen(HooverBallGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        camera = new OrthographicCamera();
        layout = new GlyphLayout();

        titleFont = Fonts.create(54);
        font = Fonts.create(28);
        creditFont = Fonts.create(19);
        creditFont.setColor(0.6f, 0.65f, 0.75f, 1f);

        rebuildLimitButtons();
        layoutButtons();
    }

    private void rebuildLimitButtons() {
        limitButtons.clear();
        if (selectedMode == GameConfig.Mode.SCORE_LIMIT) {
            limitButtons.add(new UiButton("3 gól"));
            limitButtons.add(new UiButton("5 gól"));
            limitButtons.add(new UiButton("10 gól"));
        } else {
            limitButtons.add(new UiButton("2 perc"));
            limitButtons.add(new UiButton("5 perc"));
            limitButtons.add(new UiButton("10 perc"));
        }
    }

    private void layoutButtons() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float centerX = w / 2f;

        float modeW = 240, modeH = 70, gap = 30;
        scoreModeButton.rect.set(centerX - modeW - gap / 2f, h * 0.62f, modeW, modeH);
        timeModeButton.rect.set(centerX + gap / 2f, h * 0.62f, modeW, modeH);

        float limitW = 160, limitH = 70, limitGap = 24;
        float totalLimitW = limitButtons.size * limitW + (limitButtons.size - 1) * limitGap;
        float lx = centerX - totalLimitW / 2f;
        for (UiButton b : limitButtons) {
            b.rect.set(lx, h * 0.42f, limitW, limitH);
            lx += limitW + limitGap;
        }

        startButton.rect.set(centerX - 150, h * 0.18f, 300, 90);
    }

    @Override
    public void render(float delta) {
        handleInput();
        if (startRequested) {
            // Switching screens disposes this screen's GL resources, so we must
            // not touch them again this frame - skip drawing and return.
            startMatch();
            return;
        }

        Gdx.gl.glClearColor(0.05f, 0.08f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.setProjectionMatrix(camera.combined);
        shapes.setProjectionMatrix(camera.combined);

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawButton(scoreModeButton, selectedMode == GameConfig.Mode.SCORE_LIMIT);
        drawButton(timeModeButton, selectedMode == GameConfig.Mode.TIME_LIMIT);
        for (int i = 0; i < limitButtons.size; i++) {
            UiButton b = limitButtons.get(i);
            boolean sel = selectedMode == GameConfig.Mode.SCORE_LIMIT
                    ? limitToScore(i) == selectedScoreLimit
                    : limitToMinutes(i) == selectedMinutes;
            drawButton(b, sel);
        }
        shapes.setColor(0.2f, 0.65f, 0.3f, 1f);
        shapes.rect(startButton.rect.x, startButton.rect.y, startButton.rect.width, startButton.rect.height);
        shapes.end();

        batch.begin();
        drawCentered(titleFont, "HooverBall 3D Soccer", w / 2f, h * 0.88f);
        drawCentered(font, "Válassz játékmódot", w / 2f, h * 0.74f);
        drawButtonLabel(scoreModeButton);
        drawButtonLabel(timeModeButton);
        for (UiButton b : limitButtons) {
            drawButtonLabel(b);
        }
        drawButtonLabel(startButton);

        boolean isAndroid = Gdx.app.getType() == ApplicationType.Android;
        String hint = isAndroid
                ? "Bal: joystick (mozgás)   Jobb: húzás (nézés)   Turbo gomb: booster"
                : "WASD / nyilak: mozgás + kanyar   Egér: körülnézés   Shift/Space: booster   ESC: szünet";
        drawCentered(font, hint, w / 2f, h * 0.08f);
        creditFont.draw(batch, "Készítette: helmetsoft.com", w - 230f, 24f);
        batch.end();
    }

    private int limitToScore(int index) {
        switch (index) {
            case 0: return 3;
            case 1: return 5;
            default: return 10;
        }
    }

    private float limitToMinutes(int index) {
        switch (index) {
            case 0: return 2f;
            case 1: return 5f;
            default: return 10f;
        }
    }

    private void drawButton(UiButton b, boolean selected) {
        shapes.setColor(selected ? new Color(0.25f, 0.5f, 0.85f, 1f) : new Color(0.2f, 0.22f, 0.28f, 1f));
        shapes.rect(b.rect.x, b.rect.y, b.rect.width, b.rect.height);
    }

    private void drawButtonLabel(UiButton b) {
        layout.setText(font, b.label);
        font.draw(batch, b.label, b.rect.x + b.rect.width / 2f - layout.width / 2f,
                b.rect.y + b.rect.height / 2f + layout.height / 2f);
    }

    private void drawCentered(BitmapFont f, String text, float cx, float cy) {
        layout.setText(f, text);
        f.draw(batch, text, cx - layout.width / 2f, cy + layout.height / 2f);
    }

    private void handleInput() {
        if (!Gdx.input.justTouched()) {
            return;
        }
        float x = Gdx.input.getX();
        float y = Gdx.graphics.getHeight() - Gdx.input.getY();

        if (scoreModeButton.hit(x, y)) {
            selectedMode = GameConfig.Mode.SCORE_LIMIT;
            rebuildLimitButtons();
            layoutButtons();
        } else if (timeModeButton.hit(x, y)) {
            selectedMode = GameConfig.Mode.TIME_LIMIT;
            rebuildLimitButtons();
            layoutButtons();
        } else if (startButton.hit(x, y)) {
            startRequested = true;
        } else {
            for (int i = 0; i < limitButtons.size; i++) {
                if (limitButtons.get(i).hit(x, y)) {
                    if (selectedMode == GameConfig.Mode.SCORE_LIMIT) {
                        selectedScoreLimit = limitToScore(i);
                    } else {
                        selectedMinutes = limitToMinutes(i);
                    }
                }
            }
        }
    }

    private void startMatch() {
        GameConfig config = selectedMode == GameConfig.Mode.SCORE_LIMIT
                ? GameConfig.scoreMode(selectedScoreLimit)
                : GameConfig.timeMode(selectedMinutes);
        game.setScreen(new GameScreen(game, config));
        dispose();
    }

    @Override
    public void resize(int width, int height) {
        layoutButtons();
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
        batch.dispose();
        shapes.dispose();
        titleFont.dispose();
        font.dispose();
        creditFont.dispose();
    }
}
