package tetris.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public final class SoundManager {
    public enum Effect {
        MOVE,
        ROTATE,
        SOFT_DROP,
        HARD_DROP,
        LOCK,
        CLEAR_ONE,
        CLEAR_MULTI,
        HOLD,
        GAME_OVER
    }

    private static final float SAMPLE_RATE = 44100f;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "blockfall-sound");
        thread.setDaemon(true);
        return thread;
    });
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean toggleEnabled() {
        enabled = !enabled;
        return enabled;
    }

    public void play(Effect effect) {
        if (!enabled) {
            return;
        }
        executor.execute(() -> playEffect(effect));
    }

    private void playEffect(Effect effect) {
        switch (effect) {
            case MOVE -> playTone(240, 45, 0.10);
            case ROTATE -> playTone(420, 55, 0.12);
            case SOFT_DROP -> playTone(180, 35, 0.08);
            case HARD_DROP -> playTwoNotes(110, 220, 55, 0.15);
            case LOCK -> playTwoNotes(150, 190, 45, 0.10);
            case CLEAR_ONE -> playThreeNotes(523, 659, 784, 70);
            case CLEAR_MULTI -> playThreeNotes(523, 784, 1047, 85);
            case HOLD -> playTwoNotes(330, 440, 55, 0.10);
            case GAME_OVER -> playThreeNotes(330, 220, 110, 130);
        }
    }

    private void playTwoNotes(int firstFrequency, int secondFrequency,
            int durationMillis, double volume) {
        playTone(firstFrequency, durationMillis, volume);
        playTone(secondFrequency, durationMillis, volume);
    }

    private void playThreeNotes(int firstFrequency, int secondFrequency,
            int thirdFrequency, int durationMillis) {
        playTone(firstFrequency, durationMillis, 0.16);
        playTone(secondFrequency, durationMillis, 0.16);
        playTone(thirdFrequency, durationMillis, 0.18);
    }

    private void playTone(int frequency, int durationMillis, double volume) {
        int sampleCount = (int) (SAMPLE_RATE * durationMillis / 1000f);
        byte[] samples = new byte[sampleCount * 2];
        for (int i = 0; i < sampleCount; i++) {
            double time = i / SAMPLE_RATE;
            double envelope = 1f - (double) i / sampleCount;
            double sine = Math.sin(2d * Math.PI * frequency * time);
            short value = (short) (sine * envelope * volume * Short.MAX_VALUE);
            samples[i * 2] = (byte) (value & 0xff);
            samples[i * 2 + 1] = (byte) ((value >> 8) & 0xff);
        }

        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        try (SourceDataLine line = (SourceDataLine) AudioSystem.getSourceDataLine(format)) {
            line.open(format);
            line.start();
            line.write(samples, 0, samples.length);
            line.drain();
        } catch (LineUnavailableException | SecurityException exception) {
            // Keep gameplay running if the device or audio permission is unavailable.
        }
    }
}
