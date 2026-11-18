package tetris.service;

public final class ScoreManager {
    private static final int BASE_GRAVITY_DELAY_MS = 420;
    private static final int GRAVITY_STEP_MS = 40;
    private static final int MIN_GRAVITY_DELAY_MS = 70;
    private static final int LINES_PER_LEVEL = 5;
    private static final int[] LINE_SCORES = {0, 100, 300, 500, 800};

    private long score;
    private int lines;
    private int level = 1;

    public long getScore() {
        return score;
    }

    public int getLines() {
        return lines;
    }

    public int getLevel() {
        return level;
    }

    public int getGravityDelayMillis() {
        return (int) Math.max(MIN_GRAVITY_DELAY_MS,
                BASE_GRAVITY_DELAY_MS - (long) (level - 1) * GRAVITY_STEP_MS);
    }

    public void addClearedLines(int clearedLines) {
        if (clearedLines < 0 || clearedLines >= LINE_SCORES.length) {
            throw new IllegalArgumentException("Cleared lines must be between 0 and 4");
        }
        score += (long) LINE_SCORES[clearedLines] * level;
        lines += clearedLines;
        level = lines / LINES_PER_LEVEL + 1;
    }

    public void addHardDropDistance(int droppedCells) {
        if (droppedCells < 0) {
            throw new IllegalArgumentException("Dropped cells cannot be negative");
        }
        score += 2L * droppedCells;
    }

    public void reset() {
        score = 0;
        lines = 0;
        level = 1;
    }
}
