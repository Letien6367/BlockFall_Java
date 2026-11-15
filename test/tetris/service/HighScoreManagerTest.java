package tetris.service;

import java.nio.file.Files;
import java.nio.file.Path;

public final class HighScoreManagerTest {
    private HighScoreManagerTest() {
    }

    public static void main(String[] args) throws Exception {
        Path temporaryDirectory = Files.createTempDirectory("blockfall-high-score-test");
        try {
            verifyMissingFileAndPersistence(temporaryDirectory.resolve("scores\\high-score.txt"));
            verifyInvalidFileFallback(temporaryDirectory.resolve("invalid.txt"));
            verifyUnreadablePathFallback(temporaryDirectory.resolve("directory"));
        } finally {
            deleteTree(temporaryDirectory);
        }
        System.out.println("High score persistence checks passed.");
    }

    private static void verifyMissingFileAndPersistence(Path scoreFile) {
        HighScoreManager manager = new HighScoreManager(scoreFile);
        if (manager.getHighScore() != 0 || manager.saveIfHigher(100) != 100
                || manager.saveIfHigher(90) != 100 || manager.saveIfHigher(250) != 250) {
            throw new AssertionError("High score should only increase when a new record is set");
        }
        if (new HighScoreManager(scoreFile).getHighScore() != 250) {
            throw new AssertionError("Saved high score should persist across manager instances");
        }
    }

    private static void verifyInvalidFileFallback(Path scoreFile) throws Exception {
        Files.writeString(scoreFile, "not-a-number");
        if (new HighScoreManager(scoreFile).getHighScore() != 0) {
            throw new AssertionError("Invalid high score content should fall back to zero");
        }
    }

    private static void verifyUnreadablePathFallback(Path directoryPath) throws Exception {
        Files.createDirectory(directoryPath);
        HighScoreManager manager = new HighScoreManager(directoryPath);
        if (manager.getHighScore() != 0 || manager.saveIfHigher(50) != 0) {
            throw new AssertionError("File errors should not crash or invent a saved high score");
        }
    }

    private static void deleteTree(Path directory) throws Exception {
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
