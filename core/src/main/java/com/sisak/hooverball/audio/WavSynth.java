package com.sisak.hooverball.audio;

import com.badlogic.gdx.math.MathUtils;

import java.util.Random;

/**
 * Synthesizes short sound effects and a looping background track as raw PCM
 * WAV data, entirely in code. The project ships no audio assets, so every
 * sound GameAudio plays is generated once at startup by this class.
 */
public final class WavSynth {
    private static final int SAMPLE_RATE = 22050;

    private WavSynth() {
    }

    private interface Gen {
        float sample(float t);
    }

    public static byte[] beep(float freq, float duration) {
        return render(t -> {
            float env = fadeEnvelope(t, duration, 0.01f, 0.03f);
            return (float) Math.sin(2 * Math.PI * freq * t) * env * 0.5f;
        }, duration);
    }

    /** A short two-tone descending cue for the final whistle. */
    public static byte[] endBeep() {
        float d1 = 0.15f;
        float d2 = 0.18f;
        float total = d1 + d2;
        return render(t -> {
            if (t < d1) {
                float env = fadeEnvelope(t, d1, 0.01f, 0.03f);
                return (float) Math.sin(2 * Math.PI * 660f * t) * env * 0.5f;
            } else {
                float t2 = t - d1;
                float env = fadeEnvelope(t2, d2, 0.01f, 0.05f);
                return (float) Math.sin(2 * Math.PI * 440f * t2) * env * 0.5f;
            }
        }, total);
    }

    /** A quick pitch-down thump with a little noise, for vehicle impacts. */
    public static byte[] collisionThud() {
        float duration = 0.14f;
        Random rnd = new Random(12345);
        return render(t -> {
            float freq = MathUtils.lerp(170f, 55f, t / duration);
            float tone = (float) Math.sin(2 * Math.PI * freq * t);
            float noise = rnd.nextFloat() * 2f - 1f;
            float env = fadeEnvelope(t, duration, 0.002f, duration * 0.85f);
            return (tone * 0.6f + noise * 0.4f) * env * 0.6f;
        }, duration);
    }

    /** A bright three-note ascending chime for "booster ready". */
    public static byte[] boostChime() {
        float[] freqs = {1046.5f, 1318.5f, 1568f};
        float noteDur = 0.09f;
        float total = noteDur * freqs.length;
        return render(t -> {
            int idx = Math.min(freqs.length - 1, (int) (t / noteDur));
            float tn = t - idx * noteDur;
            float env = fadeEnvelope(tn, noteDur, 0.005f, noteDur * 0.6f);
            return (float) Math.sin(2 * Math.PI * freqs[idx] * t) * env * 0.45f;
        }, total);
    }

    /**
     * A quiet instrumental rendition of the Hungarian hymn "Hinni tanits,
     * Uram" (F major, 3/4), transcribed by ear from its four-line printed
     * melody, and played back at triple speed (as requested) so it reads as
     * driving background energy rather than a slow chorale. Four musical
     * phrases (one per printed system/refrain line), each a bar-for-bar
     * transcription of that line's soprano part, looped back to back.
     */
    public static byte[] hinniTanits() {
        // {note name, duration in beats}
        Object[][] melodyNotes = {
                // --- Phrase A: "Hinni tanits, Uram, kerni tanits!" ---
                {"F4", 1f}, {"F4", 1f}, {"F4", 1f},
                {"F4", 1.5f}, {"G4", 0.5f}, {"E4", 1f},
                {"D4", 1f}, {"F4", 1f}, {"A4", 1f},
                {"F4", 3f},
                // --- Phrase B: "...nagy hitet kerni tanits!" ---
                {"C5", 1f}, {"Bb4", 1f}, {"A4", 1f},
                {"A4", 1.5f}, {"B4", 0.5f}, {"A4", 1f},
                {"G4", 1f}, {"A4", 1f}, {"C5", 1f},
                {"D5", 2f}, {"C5", 1f},
                // --- Phrase C: "Inditsd fel szivemet, buzduljon fel, neked" ---
                {"C5", 1f}, {"Bb4", 1f}, {"A4", 0.5f}, {"G4", 0.5f},
                {"A4", 1.5f}, {"Bb4", 0.5f}, {"G4", 1f},
                {"G4", 1f}, {"F4", 1f}, {"E4", 0.5f}, {"D4", 0.5f},
                {"C5", 1.5f}, {"D5", 0.5f}, {"C5", 1f},
                // --- Phrase D: "Gyujteni lelkeket, kerni tanits!" (final, fermata) ---
                {"C5", 1f}, {"Bb4", 1f}, {"A4", 0.5f}, {"G4", 0.5f},
                {"A4", 1.5f}, {"Bb4", 0.5f}, {"G4", 1f},
                {"G4", 1f}, {"A4", 1f}, {"C5", 1f},
                {"F4", 3.5f},
        };
        // The hymn's natural pace, played back twice as fast (per request -
        // triple speed was too fast).
        float secondsPerBeat = 0.55f / 2f;
        float[][] melody = new float[melodyNotes.length][2];
        float total = 0f;
        for (int i = 0; i < melodyNotes.length; i++) {
            float freq = noteFrequency((String) melodyNotes[i][0]);
            float dur = (Float) melodyNotes[i][1] * secondsPerBeat;
            melody[i][0] = freq;
            melody[i][1] = dur;
            total += dur;
        }
        final float totalDuration = total;
        // Simple F major tonic/dominant drone, root per 3-beat bar.
        final float barBeats = 3f * secondsPerBeat;

        return render(t -> {
            float acc = 0f;
            float freq = 0f;
            float noteStart = 0f;
            float noteDur = 0.0001f;
            for (float[] n : melody) {
                if (t < acc + n[1]) {
                    freq = n[0];
                    noteStart = acc;
                    noteDur = n[1];
                    break;
                }
                acc += n[1];
            }
            float tn = t - noteStart;
            float env = fadeEnvelope(tn, noteDur, 0.01f, noteDur * 0.4f);
            float melodySample = (float) Math.sin(2 * Math.PI * freq * tn) * env * 0.2f;
            // A close harmony voice a third below (as printed), and a third
            // below that again, so the hymn's close-position chords read as
            // more than a single bare line.
            float thirdSample = (float) Math.sin(2 * Math.PI * (freq / 1.1892f) * tn) * env * 0.12f;
            float sixthSample = (float) Math.sin(2 * Math.PI * (freq / 1.6818f) * tn) * env * 0.09f;

            // A soft F2/C3 tonic-dominant drone, one change per bar.
            float tb = t % barBeats;
            int barIndex = (int) (t / barBeats);
            float bassFreq = (barIndex % 2 == 0) ? 87.31f : 130.81f; // F2 / C3
            float bEnv = fadeEnvelope(tb, barBeats, 0.02f, barBeats * 0.6f);
            float bassSample = (float) Math.sin(2 * Math.PI * bassFreq * tb) * bEnv * 0.1f;

            return melodySample + thirdSample + sixthSample + bassSample;
        }, totalDuration);
    }

    private static float noteFrequency(String name) {
        int[] semitoneFromC = {0, 2, 4, 5, 7, 9, 11}; // C D E F G A B
        char letter = name.charAt(0);
        int idx = "CDEFGAB".indexOf(letter);
        int semitone = semitoneFromC[idx];
        int pos = 1;
        if (pos < name.length() && name.charAt(pos) == '#') {
            semitone += 1;
            pos++;
        } else if (pos < name.length() && name.charAt(pos) == 'b') {
            semitone -= 1;
            pos++;
        }
        int octave = Integer.parseInt(name.substring(pos));
        int midi = (octave + 1) * 12 + semitone;
        return 440f * (float) Math.pow(2.0, (midi - 69) / 12.0);
    }

    /** Linear fade in over {@code attack}s, sustain, fade out over the last {@code release}s. */
    private static float fadeEnvelope(float t, float total, float attack, float release) {
        if (t < attack) {
            return t / attack;
        }
        if (t > total - release) {
            return Math.max(0f, (total - t) / release);
        }
        return 1f;
    }

    private static byte[] render(Gen gen, float durationSeconds) {
        int numSamples = (int) (durationSeconds * SAMPLE_RATE);
        byte[] data = new byte[44 + numSamples * 2];
        writeWavHeader(data, numSamples);
        for (int i = 0; i < numSamples; i++) {
            float t = i / (float) SAMPLE_RATE;
            float s = MathUtils.clamp(gen.sample(t), -1f, 1f);
            short pcm = (short) (s * 32767f);
            int offset = 44 + i * 2;
            data[offset] = (byte) (pcm & 0xFF);
            data[offset + 1] = (byte) ((pcm >> 8) & 0xFF);
        }
        return data;
    }

    private static void writeWavHeader(byte[] data, int numSamples) {
        int byteRate = SAMPLE_RATE * 2;
        int dataSize = numSamples * 2;
        writeString(data, 0, "RIFF");
        writeInt(data, 4, 36 + dataSize);
        writeString(data, 8, "WAVE");
        writeString(data, 12, "fmt ");
        writeInt(data, 16, 16);
        writeShort(data, 20, (short) 1); // PCM
        writeShort(data, 22, (short) 1); // mono
        writeInt(data, 24, SAMPLE_RATE);
        writeInt(data, 28, byteRate);
        writeShort(data, 32, (short) 2); // block align
        writeShort(data, 34, (short) 16); // bits per sample
        writeString(data, 36, "data");
        writeInt(data, 40, dataSize);
    }

    private static void writeInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
        data[offset + 2] = (byte) ((value >> 16) & 0xFF);
        data[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    private static void writeShort(byte[] data, int offset, short value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    private static void writeString(byte[] data, int offset, String s) {
        for (int i = 0; i < s.length(); i++) {
            data[offset + i] = (byte) s.charAt(i);
        }
    }
}
