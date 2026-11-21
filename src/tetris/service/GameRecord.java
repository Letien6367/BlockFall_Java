package tetris.service;

import java.time.Instant;
import java.util.Objects;

public record GameRecord(String playerName, long score, int level, int lines,
        Instant completedAt) {
    public GameRecord {
        Objects.requireNonNull(playerName, "playerName");
        Objects.requireNonNull(completedAt, "completedAt");
        if (score < 0 || level < 1 || lines < 0) {
            throw new IllegalArgumentException("Invalid game record statistics");
        }
    }
}
