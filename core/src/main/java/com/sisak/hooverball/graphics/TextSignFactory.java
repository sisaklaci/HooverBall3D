package com.sisak.hooverball.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Bakes a short line of text onto a solid background into a standalone
 * {@link Texture}, so advertising signs can be built without shipping any
 * external image or font assets. Rendered once at load time via an offscreen
 * framebuffer, not every frame.
 */
public final class TextSignFactory {
    private TextSignFactory() {
    }

    private static final int MIN_FONT_PIXEL_SIZE = 14;

    public static Texture create(String text, int width, int height, Color background, Color foreground, int maxFontPixelSize) {
        FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
        OrthographicCamera camera = new OrthographicCamera();
        camera.setToOrtho(false, width, height);

        float border = height * 0.07f;
        // Text length varies a lot between sponsors ("refsztg.hu" vs.
        // "helmetsoft.com"), so the requested size is only an upper bound:
        // shrink the font until the whole line actually fits inside the
        // sign, instead of letting long names run off either edge.
        float availableWidth = width - 2f * border - width * 0.05f;
        int fontPixelSize = maxFontPixelSize;
        BitmapFont font = Fonts.create(fontPixelSize);
        GlyphLayout layout = new GlyphLayout(font, text);
        while (layout.width > availableWidth && fontPixelSize > MIN_FONT_PIXEL_SIZE) {
            font.dispose();
            int shrunk = (int) (fontPixelSize * (availableWidth / layout.width) * 0.95f);
            fontPixelSize = Math.max(MIN_FONT_PIXEL_SIZE, Math.min(shrunk, fontPixelSize - 1));
            font = Fonts.create(fontPixelSize);
            layout = new GlyphLayout(font, text);
        }
        font.setColor(foreground);
        SpriteBatch batch = new SpriteBatch();
        batch.setProjectionMatrix(camera.combined);

        ShapeRenderer shapes = new ShapeRenderer();
        shapes.setProjectionMatrix(camera.combined);

        fbo.begin();
        Gdx.gl.glClearColor(background.r, background.g, background.b, background.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(foreground);
        shapes.rect(0, 0, width, border);
        shapes.rect(0, height - border, width, border);
        shapes.rect(0, 0, border, height);
        shapes.rect(width - border, 0, border, height);
        shapes.end();
        batch.begin();
        font.draw(batch, text, (width - layout.width) / 2f, (height + layout.height) / 2f);
        batch.end();
        Pixmap raw = ScreenUtils.getFrameBufferPixmap(0, 0, width, height);
        fbo.end();

        // The framebuffer read comes back bottom-up; flip it so the text sits
        // the right way up once mapped onto a 3D plane.
        Pixmap flipped = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        for (int y = 0; y < height; y++) {
            flipped.drawPixmap(raw, 0, height - 1 - y, width, 1, 0, y, width, 1);
        }
        raw.dispose();

        Texture texture = new Texture(flipped);
        texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        flipped.dispose();
        font.dispose();
        batch.dispose();
        fbo.dispose();
        return texture;
    }
}
