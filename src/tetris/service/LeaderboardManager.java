package tetris.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class LeaderboardManager {
    public static final int MAX_PLAYERS_DISPLAYED = 10;
    public static final int MAX_NAME_LENGTH = 24;

    private static final Comparator<PlayerRecord> RANKING_ORDER =
            Comparator.comparingLong(PlayerRecord::highScore).reversed()
                    .thenComparing(PlayerRecord::name)
                    .thenComparing(record -> record.id().toString());

    private final Path leaderboardFile;
    private Map<String, PlayerRecord> playersByName;

    public LeaderboardManager() {
        this(Path.of(System.getProperty("user.home"), ".blockfall", "leaderboard.txt"));
    }

    public LeaderboardManager(Path leaderboardFile) {
        this.leaderboardFile = Objects.requireNonNull(leaderboardFile, "leaderboardFile")
                .toAbsolutePath().normalize();
        playersByName = readPlayers();
    }

    public PlayerRecord findOrCreatePlayer(String name) throws IOException {
        String normalizedName = normalizeName(name);
        PlayerRecord existing = playersByName.get(normalizedName);
        if (existing != null) {
            return existing;
        }

        PlayerRecord created = new PlayerRecord(UUID.randomUUID(), normalizedName, 0);
        Map<String, PlayerRecord> updatedPlayers = new LinkedHashMap<>(playersByName);
        updatedPlayers.put(normalizedName, created);
        persistAndReplace(updatedPlayers);
        return created;
    }

    public PlayerRecord saveScore(UUID playerId, long score) throws IOException {
        Objects.requireNonNull(playerId, "playerId");
        if (score < 0) {
            throw new IllegalArgumentException("Score cannot be negative");
        }

        PlayerRecord current = playersByName.values().stream()
                .filter(player -> player.id().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown player ID"));
        if (score <= current.highScore()) {
            return current;
        }

        PlayerRecord updated = new PlayerRecord(current.id(), current.name(), score);
        Map<String, PlayerRecord> updatedPlayers = new LinkedHashMap<>(playersByName);
        updatedPlayers.put(updated.name(), updated);
        persistAndReplace(updatedPlayers);
        return updated;
    }

    public List<PlayerRecord> getTopPlayers() {
        return playersByName.values().stream()
                .sorted(RANKING_ORDER)
                .limit(MAX_PLAYERS_DISPLAYED)
                .toList();
    }

    private static String normalizeName(String name) {
        Objects.requireNonNull(name, "name");
        String normalizedName = name.strip().replaceAll("\\s+", " ");
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập tên người chơi.");
        }
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Tên người chơi không được dài quá " + MAX_NAME_LENGTH + " ký tự.");
        }
        return normalizedName;
    }

    private Map<String, PlayerRecord> readPlayers() {
        List<String> lines;
        try {
            lines = Files.readAllLines(leaderboardFile, StandardCharsets.UTF_8);
        } catch (NoSuchFileException exception) {
            return new LinkedHashMap<>();
        } catch (IOException | SecurityException exception) {
            System.err.println("Unable to read leaderboard from " + leaderboardFile + ": "
                    + exception.getMessage());
            return new LinkedHashMap<>();
        }

        Map<String, PlayerRecord> loadedPlayers = new LinkedHashMap<>();
        for (String line : lines) {
            try {
                String[] fields = line.split("\t", -1);
                if (fields.length != 3) {
                    throw new IllegalArgumentException("Invalid field count");
                }

                UUID id = UUID.fromString(fields[0]);
                String name = normalizeName(new String(
                        Base64.getDecoder().decode(fields[1]), StandardCharsets.UTF_8));
                long highScore = Long.parseLong(fields[2]);
                if (highScore < 0 || loadedPlayers.containsKey(name)
                        || containsPlayerId(loadedPlayers, id)) {
                    throw new IllegalArgumentException("Invalid or duplicate player record");
                }
                loadedPlayers.put(name, new PlayerRecord(id, name, highScore));
            } catch (IllegalArgumentException exception) {
                System.err.println("Ignoring invalid leaderboard entry in " + leaderboardFile);
            }
        }
        return loadedPlayers;
    }

    private static boolean containsPlayerId(Map<String, PlayerRecord> players, UUID id) {
        return players.values().stream().anyMatch(player -> player.id().equals(id));
    }

    private void persistAndReplace(Map<String, PlayerRecord> updatedPlayers) throws IOException {
        Path parent = leaderboardFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        List<PlayerRecord> records = new ArrayList<>(updatedPlayers.values());
        records.sort(Comparator.comparing(PlayerRecord::name));
        StringBuilder contents = new StringBuilder();
        for (PlayerRecord player : records) {
            contents.append(player.id()).append('\t')
                    .append(Base64.getEncoder().encodeToString(
                            player.name().getBytes(StandardCharsets.UTF_8)))
                    .append('\t').append(player.highScore()).append(System.lineSeparator());
        }

        Path temporaryFile = Files.createTempFile(parent, "leaderboard-", ".tmp");
        try {
            Files.writeString(temporaryFile, contents, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, leaderboardFile, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, leaderboardFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
        playersByName = updatedPlayers;
    }

    public record PlayerRecord(UUID id, String name, long highScore) {
        public PlayerRecord {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
            if (highScore < 0) {
                throw new IllegalArgumentException("Score cannot be negative");
            }
        }
    }
}
