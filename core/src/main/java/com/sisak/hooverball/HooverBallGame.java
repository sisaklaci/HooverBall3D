package com.sisak.hooverball;

import com.badlogic.gdx.Game;
import com.sisak.hooverball.audio.GameAudio;
import com.sisak.hooverball.graphics.Fonts;
import com.sisak.hooverball.screens.MenuScreen;

/** Shared entry point used by both the desktop and Android launchers. */
public class HooverBallGame extends Game {
    private GameAudio audio;

    @Override
    public void create() {
        audio = new GameAudio();
        setScreen(new MenuScreen(this));
    }

    public GameAudio getAudio() {
        return audio;
    }

    @Override
    public void dispose() {
        super.dispose();
        audio.dispose();
        Fonts.disposeShared();
    }
}
