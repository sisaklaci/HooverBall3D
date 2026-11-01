package com.sisak.hooverball.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

/**
 * Loads the procedurally generated sound effects and background music once
 * at startup and exposes simple play/pause calls. Owned by {@link
 * com.sisak.hooverball.HooverBallGame} so it survives screen transitions.
 */
public class GameAudio implements Disposable {
    private static final float MUSIC_VOLUME = 0.16f; // quiet background, per request

    private final Sound startBeep;
    private final Sound endBeep;
    private final Sound collisionThud;
    private final Sound boostReadyChime;
    private final Music music;

    public GameAudio() {
        startBeep = loadSound("start.wav", WavSynth.beep(880f, 0.16f));
        endBeep = loadSound("end.wav", WavSynth.endBeep());
        collisionThud = loadSound("hit.wav", WavSynth.collisionThud());
        boostReadyChime = loadSound("boost.wav", WavSynth.boostChime());
        music = loadMusic("music.wav", WavSynth.hinniTanits());
        music.setLooping(true);
        music.setVolume(MUSIC_VOLUME);
    }

    private Sound loadSound(String fileName, byte[] wavBytes) {
        FileHandle file = Gdx.files.local("generated-audio/" + fileName);
        file.writeBytes(wavBytes, false);
        return Gdx.audio.newSound(file);
    }

    private Music loadMusic(String fileName, byte[] wavBytes) {
        FileHandle file = Gdx.files.local("generated-audio/" + fileName);
        file.writeBytes(wavBytes, false);
        return Gdx.audio.newMusic(file);
    }

    public void playStart() {
        startBeep.play(0.7f);
    }

    public void playEnd() {
        endBeep.play(0.8f);
    }

    public void playCollision(float intensity) {
        collisionThud.play(MathUtils.clamp(intensity, 0.15f, 1f));
    }

    public void playBoostReady() {
        boostReadyChime.play(0.6f);
    }

    public void startMusic() {
        if (!music.isPlaying()) {
            music.play();
        }
    }

    public void stopMusic() {
        music.stop();
    }

    public void setMusicPaused(boolean paused) {
        if (paused) {
            music.pause();
        } else if (!music.isPlaying()) {
            music.play();
        }
    }

    @Override
    public void dispose() {
        startBeep.dispose();
        endBeep.dispose();
        collisionThud.dispose();
        boostReadyChime.dispose();
        music.dispose();
    }
}
