package tetris.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public final class HighScoreManager {
    private final Path scoreFile;
    private long highScore;

    public HighScoreManager() {
        this(Path.of(System.getProperty("user.home"), ".blockfall", "high-score.txt"));
    }

    public HighScoreManager(Path scoreFile) {
        this.scoreFile = Objects.requireNonNull(scoreFile, "scoreFile").toAbsolutePath().normalize();
        highScore = readHighScore();
    }

    public long getHighScore() {
        return highScore;
    }

    public long saveIfHigher(long score) {
        if (score < 0) {
            throw new IllegalArgumentException("Score cannot be negative");
        }
        if (score <= highScore) {
            return highScore;
        }

        try {
            Path parent = scoreFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(scoreFile, Long.toString(score), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            highScore = score;
        } catch (IOException | SecurityException exception) {
            System.err.println("Unable to save high score to " + scoreFile + ": "
                    + exception.getMessage());
        }
        return highScore;
    }

    private long readHighScore() {
        final String contents;
        try {
            contents = Files.readString(scoreFile, StandardCharsets.UTF_8).trim();
        } catch (NoSuchFileException exception) {
            return 0;
        } catch (IOException | SecurityException exception) {
            System.err.println("Unable to read high score from " + scoreFile + ": "
                    + exception.getMessage());
            return 0;
        }

        try {
            long score = Long.parseLong(contents);
            if (score < 0) {
                System.err.println("Ignoring negative high score in " + scoreFile);
                return 0;
            }
            return score;
        } catch (NumberFormatException exception) {
            System.err.println("Ignoring invalid high score in " + scoreFile);
            return 0;
        }
    }
}
