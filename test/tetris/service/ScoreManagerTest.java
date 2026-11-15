package tetris.service;

public final class ScoreManagerTest {
    private ScoreManagerTest() {
    }

    public static void main(String[] args) {
        verifyLineScoreTable();
        verifyLevelAndGravityProgression();
        verifyGravityDelayHasMinimum();
        verifyReset();
        System.out.println("Score manager checks passed.");
    }

    private static void verifyLineScoreTable() {
        int[] scores = {0, 100, 300, 500, 800};
        for (int lines = 1; lines <= 4; lines++) {
            ScoreManager manager = new ScoreManager();
            manager.addClearedLines(lines);
            if (manager.getScore() != scores[lines] || manager.getLines() != lines) {
                throw new AssertionError("Unexpected score for " + lines + " cleared lines");
            }
        }
    }

    private static void verifyLevelAndGravityProgression() {
        ScoreManager manager = new ScoreManager();
        manager.addClearedLines(4);
        manager.addClearedLines(4);
        manager.addClearedLines(1);
        if (manager.getLevel() != 1 || manager.getGravityDelayMillis() != 600) {
            throw new AssertionError("Level should stay at one before ten lines");
        }

        manager.addClearedLines(1);
        if (manager.getLines() != 10 || manager.getLevel() != 2
                || manager.getGravityDelayMillis() != 550) {
            throw new AssertionError("Level and gravity should advance at ten lines");
        }
        manager.addClearedLines(2);
        if (manager.getScore() != 2400) {
            throw new AssertionError("Line score should use the level before the clear");
        }
        manager.addHardDropDistance(3);
        if (manager.getScore() != 2406) {
            throw new AssertionError("Hard drop should award two points per cell");
        }
    }

    private static void verifyGravityDelayHasMinimum() {
        ScoreManager manager = new ScoreManager();
        for (int i = 0; i < 30; i++) {
            manager.addClearedLines(4);
        }
        if (manager.getGravityDelayMillis() != 100) {
            throw new AssertionError("Gravity delay should not fall below 100ms");
        }
    }

    private static void verifyReset() {
        ScoreManager manager = new ScoreManager();
        manager.addClearedLines(4);
        manager.reset();
        if (manager.getScore() != 0 || manager.getLines() != 0 || manager.getLevel() != 1
                || manager.getGravityDelayMillis() != 600) {
            throw new AssertionError("Score reset should restore initial values");
        }
    }
}
