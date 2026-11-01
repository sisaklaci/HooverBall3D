package com.sisak.hooverball.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Disposable;

/**
 * Generates crisp, correctly-sized fonts (not the built-in low-res bitmap
 * font stretched with a scale factor, which both looks blocky and is
 * missing accented Hungarian letters) from a bundled TTF with full Latin
 * Extended-A coverage, so every "á/é/í/ó/ö/ő/ú/ü/ű" in the game's Hungarian
 * text actually renders.
 */
public final class Fonts implements Disposable {
    private static final String FONT_PATH = "fonts/Roboto-Regular.ttf";
    // Default FreeType charset plus the accented letters used in Hungarian.
    private static final String CHARS = FreeTypeFontGenerator.DEFAULT_CHARS
            + "áéíóöőúüűÁÉÍÓÖŐÚÜŰ";

    private static Fonts instance;

    private final FreeTypeFontGenerator generator;

    private Fonts() {
        generator = new FreeTypeFontGenerator(Gdx.files.classpath(FONT_PATH));
    }

    private static Fonts get() {
        if (instance == null) {
            instance = new Fonts();
        }
        return instance;
    }

    /** A font rendered at exactly {@code pixelSize}, not scaled after the fact, so it always stays sharp. */
    public static BitmapFont create(int pixelSize) {
        FreeTypeFontParameter param = new FreeTypeFontParameter();
        param.size = pixelSize;
        param.color = Color.WHITE;
        param.characters = CHARS;
        param.minFilter = TextureFilter.Linear;
        param.magFilter = TextureFilter.Linear;
        param.borderWidth = pixelSize * 0.04f;
        param.borderColor = new Color(0f, 0f, 0f, 0.55f);
        return get().generator.generateFont(param);
    }

    /** Releases the shared FreeType generator; call once when the whole game shuts down. */
    public static void disposeShared() {
        if (instance != null) {
            instance.dispose();
            instance = null;
        }
    }

    @Override
    public void dispose() {
        generator.dispose();
    }
}
