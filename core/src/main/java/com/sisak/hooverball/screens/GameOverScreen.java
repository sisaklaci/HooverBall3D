package com.sisak.hooverball.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.sisak.hooverball.GameState;
import com.sisak.hooverball.HooverBallGame;
import com.sisak.hooverball.graphics.Fonts;
import com.sisak.hooverball.graphics.UiButton;

/** Final score screen shown once a match reaches its score or time limit. */
public class GameOverScreen implements Screen {
    private final HooverBallGame game;
    private final GameState state;

    private SpriteBatch batch;
    private BitmapFont titleFont;
    private BitmapFont font;
    private ShapeRenderer shapes;
    private OrthographicCamera camera;
    private GlyphLayout layout;

    private final UiButton menuButton = new UiButton("VISSZA A MENÜBE");
    private boolean menuRequested;

    public GameOverScreen(HooverBallGame game, GameState state) {
        this.game = game;
        this.state = state;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        camera = new OrthographicCamera();
        layout = new GlyphLayout();

        titleFont = Fonts.create(58);
        font = Fonts.create(30);

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void render(float delta) {
        handleInput();
        if (menuRequested) {
            // Switching screens disposes this screen's GL resources, so we must
            // not touch them again this frame - skip drawing and return.
            game.setScreen(new MenuScreen(game));
            dispose();
            return;
        }

        Gdx.gl.glClearColor(0.05f, 0.08f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.2f, 0.45f, 0.75f, 1f);
        shapes.rect(menuButton.rect.x, menuButton.rect.y, menuButton.rect.width, menuButton.rect.height);
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawCentered(titleFont, resultText(), w / 2f, h * 0.72f);
        String scoreText = "Végeredmény:  Játékos " + state.playerScore + " : " + state.aiScore + " Gép";
        drawCentered(font, scoreText, w / 2f, h * 0.58f);
        drawButtonLabel(menuButton);
        batch.end();
    }

    private String resultText() {
        switch (state.winner) {
            case PLAYER: return "GYŐZTÉL!";
            case AI: return "A GÉP GYŐZÖTT";
            default: return "DÖNTETLEN";
        }
    }

    private void drawCentered(BitmapFont f, String text, float cx, float cy) {
        layout.setText(f, text);
        f.draw(batch, text, cx - layout.width / 2f, cy + layout.height / 2f);
    }

    private void drawButtonLabel(UiButton b) {
        layout.setText(font, b.label);
        font.draw(batch, b.label, b.rect.x + b.rect.width / 2f - layout.width / 2f,
                b.rect.y + b.rect.height / 2f + layout.height / 2f);
    }

    private void handleInput() {
        if (!Gdx.input.justTouched()) {
            return;
        }
        float x = Gdx.input.getX();
        float y = Gdx.graphics.getHeight() - Gdx.input.getY();
        if (menuButton.hit(x, y)) {
            menuRequested = true;
        }
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
        menuButton.rect.set(width / 2f - 220, height * 0.32f, 440, 90);
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
    }
}
