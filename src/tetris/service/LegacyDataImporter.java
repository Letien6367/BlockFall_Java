package tetris.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class LegacyDataImporter {
    private LegacyDataImporter() {
    }

    static Snapshot read(Path dataDirectory) throws IOException {
        List<PlayerRecord> players = readPlayers(dataDirectory.resolve("leaderboard.txt"));
        long highScore = readLong(dataDirectory.resolve("high-score.txt"), 0);
        int highLevel = readLevel(dataDirectory.resolve("high-level.txt"));
        return new Snapshot(players, highScore, highLevel);
    }

    private static List<PlayerRecord> readPlayers(Path leaderboardFile) throws IOException {
        List<String> lines;
        try {
            lines = Files.readAllLines(leaderboardFile, StandardCharsets.UTF_8);
        } catch (NoSuchFileException exception) {
            return List.of();
        }

        Map<String, PlayerRecord> playersByName = new LinkedHashMap<>();
        Set<UUID> playerIds = new HashSet<>();
        for (String line : lines) {
            try {
                String[] fields = line.split("\t", -1);
                if (fields.length != 3) {
                    throw new IllegalArgumentException("Invalid field count");
                }

                UUID id = UUID.fromString(fields[0]);
                String name = PlayerRecord.normalizeName(new String(
                        Base64.getDecoder().decode(fields[1]), StandardCharsets.UTF_8));
                long highScore = Long.parseLong(fields[2]);
                if (highScore < 0 || playersByName.containsKey(name) || !playerIds.add(id)) {
                    throw new IllegalArgumentException("Invalid or duplicate player record");
                }
                playersByName.put(name, new PlayerRecord(id, name, highScore));
            } catch (IllegalArgumentException exception) {
                System.err.println("Ignoring invalid legacy leaderboard entry in "
                        + leaderboardFile);
            }
        }
        return List.copyOf(playersByName.values());
    }

    private static long readLong(Path file, long defaultValue) throws IOException {
        final String contents;
        try {
            contents = Files.readString(file, StandardCharsets.UTF_8).trim();
        } catch (NoSuchFileException exception) {
            return defaultValue;
        }

        try {
            long value = Long.parseLong(contents);
            if (value >= 0) {
                return value;
            }
        } catch (NumberFormatException exception) {
            System.err.println("Ignoring invalid legacy value in " + file);
            return defaultValue;
        }
        System.err.println("Ignoring negative legacy value in " + file);
        return defaultValue;
    }

    private static int readLevel(Path file) throws IOException {
        long level = readLong(file, 1);
        if (level < 1 || level > Integer.MAX_VALUE) {
            System.err.println("Ignoring invalid legacy level in " + file);
            return 1;
        }
        return (int) level;
    }

    record Snapshot(List<PlayerRecord> players, long highScore, int highLevel) {
        Snapshot {
            players = List.copyOf(players);
        }
    }
}
