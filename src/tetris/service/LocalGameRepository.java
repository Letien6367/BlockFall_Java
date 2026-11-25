package tetris.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class LocalGameRepository {
    private static final int MAX_PLAYERS_DISPLAYED = 10;
    public static final int MAX_HISTORY_RECORDS = 100;
    private static final String LEADERBOARD_FILE_NAME = "leaderboard.txt";
    private static final String HIGH_SCORE_FILE_NAME = "high-score.txt";
    private static final String GAME_HISTORY_FILE_NAME = "game-history.txt";
    private static final Comparator<PlayerRecord> PLAYER_RANKING =
            Comparator.comparingLong(PlayerRecord::highScore).reversed()
                    .thenComparing(PlayerRecord::name)
                    .thenComparing(player -> player.id().toString());

    private final Path dataDirectory;
    private Map<String, PlayerRecord> playersByName = Map.of();
    private List<GameRecord> recentGames = List.of();
    private volatile long highScore;
    private boolean initialized;

    public LocalGameRepository() {
        this(Path.of(System.getProperty("user.home"), ".blockfall"));
    }

    LocalGameRepository(Path dataDirectory) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
    }

    public void initialize() throws IOException {
        Files.createDirectories(dataDirectory);
        Map<String, PlayerRecord> loadedPlayers = readPlayers();
        List<GameRecord> loadedHistory = readHistory();
        long storedHighScore = readHighScore();
        long bestPlayerScore = loadedPlayers.values().stream()
                .mapToLong(PlayerRecord::highScore)
                .max()
                .orElse(0);
        playersByName = loadedPlayers;
        recentGames = loadedHistory;
        highScore = Math.max(storedHighScore, bestPlayerScore);
        initialized = true;
    }

    public long getHighScore() {
        return highScore;
    }

    public List<PlayerRecord> getTopPlayers() throws IOException {
        requireInitialized();
        return playersByName.values().stream()
                .sorted(PLAYER_RANKING)
                .limit(MAX_PLAYERS_DISPLAYED)
                .toList();
    }

    public List<GameRecord> getRecentGames() throws IOException {
        requireInitialized();
        return recentGames;
    }

    public PlayerRecord findOrCreatePlayer(String name) throws IOException {
        requireInitialized();
        String normalizedName = PlayerRecord.normalizeName(name);
        PlayerRecord existing = playersByName.get(normalizedName);
        if (existing != null) {
            return existing;
        }

        PlayerRecord created = new PlayerRecord(UUID.randomUUID(), normalizedName, 0);
        Map<String, PlayerRecord> updatedPlayers = new LinkedHashMap<>(playersByName);
        updatedPlayers.put(created.name(), created);
        writePlayers(updatedPlayers.values());
        playersByName = updatedPlayers;
        return created;
    }

    public PlayerRecord saveGame(PlayerRecord player, long score, int level, int lines)
            throws IOException {
        requireInitialized();
        Objects.requireNonNull(player, "player");
        if (score < 0 || level < 1 || lines < 0) {
            throw new IllegalArgumentException("Invalid game statistics");
        }

        PlayerRecord savedPlayer = playersByName.get(player.name());
        if (savedPlayer == null || !savedPlayer.id().equals(player.id())) {
            throw new IOException("Cannot save a game for an unknown player.");
        }

        long updatedHighScore = Math.max(savedPlayer.highScore(), score);
        if (updatedHighScore > savedPlayer.highScore()) {
            PlayerRecord updatedPlayer = new PlayerRecord(
                    savedPlayer.id(), savedPlayer.name(), updatedHighScore);
            Map<String, PlayerRecord> updatedPlayers = new LinkedHashMap<>(playersByName);
            updatedPlayers.put(updatedPlayer.name(), updatedPlayer);
            writePlayers(updatedPlayers.values());
            playersByName = updatedPlayers;
            savedPlayer = updatedPlayer;
        }

        List<GameRecord> updatedHistory = new ArrayList<>();
        updatedHistory.add(new GameRecord(player.name(), score, level, lines, Instant.now()));
        updatedHistory.addAll(recentGames);
        if (updatedHistory.size() > MAX_HISTORY_RECORDS) {
            updatedHistory.remove(updatedHistory.size() - 1);
        }
        writeHistory(updatedHistory);
        recentGames = List.copyOf(updatedHistory);

        if (score > highScore) {
            highScore = score;
            writeHighScore(highScore);
        }
        return savedPlayer;
    }

    private Map<String, PlayerRecord> readPlayers() throws IOException {
        Path leaderboardFile = dataDirectory.resolve(LEADERBOARD_FILE_NAME);
        if (!Files.exists(leaderboardFile)) {
            return new LinkedHashMap<>();
        }

        List<String> lines = Files.readAllLines(leaderboardFile, StandardCharsets.UTF_8);
        Map<String, PlayerRecord> loadedPlayers = new LinkedHashMap<>();
        Set<UUID> playerIds = new HashSet<>();
        for (int index = 0; index < lines.size(); index++) {
            String[] fields = lines.get(index).split("\\t", -1);
            try {
                if (fields.length != 3) {
                    throw new IllegalArgumentException("Invalid field count");
                }
                UUID id = UUID.fromString(fields[0]);
                String name = PlayerRecord.normalizeName(new String(
                        Base64.getDecoder().decode(fields[1]), StandardCharsets.UTF_8));
                long score = Long.parseLong(fields[2]);
                if (score < 0 || loadedPlayers.containsKey(name) || !playerIds.add(id)) {
                    throw new IllegalArgumentException("Invalid or duplicate player record");
                }
                loadedPlayers.put(name, new PlayerRecord(id, name, score));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid leaderboard entry at "
                        + leaderboardFile + ":" + (index + 1), exception);
            }
        }
        return loadedPlayers;
    }

    private List<GameRecord> readHistory() throws IOException {
        Path historyFile = dataDirectory.resolve(GAME_HISTORY_FILE_NAME);
        if (!Files.exists(historyFile)) {
            return List.of();
        }

        List<String> lines = Files.readAllLines(historyFile, StandardCharsets.UTF_8);
        List<GameRecord> loadedHistory = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String[] fields = lines.get(index).split("\\t", -1);
            try {
                if (fields.length != 5) {
                    throw new IllegalArgumentException("Invalid field count");
                }
                String name = new String(
                        Base64.getDecoder().decode(fields[0]), StandardCharsets.UTF_8);
                loadedHistory.add(new GameRecord(name, Long.parseLong(fields[1]),
                        Integer.parseInt(fields[2]), Integer.parseInt(fields[3]),
                        Instant.parse(fields[4])));
            } catch (IllegalArgumentException | DateTimeParseException exception) {
                throw new IOException("Invalid game history entry at "
                        + historyFile + ":" + (index + 1), exception);
            }
        }
        if (loadedHistory.size() > MAX_HISTORY_RECORDS) {
            throw new IOException("Game history contains more than "
                    + MAX_HISTORY_RECORDS + " records: " + historyFile);
        }
        return List.copyOf(loadedHistory);
    }

    private long readHighScore() throws IOException {
        Path highScoreFile = dataDirectory.resolve(HIGH_SCORE_FILE_NAME);
        if (!Files.exists(highScoreFile)) {
            return 0;
        }
        String contents = Files.readString(highScoreFile, StandardCharsets.UTF_8).strip();
        try {
            long score = Long.parseLong(contents);
            if (score < 0) {
                throw new NumberFormatException("Score cannot be negative");
            }
            return score;
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid high score in " + highScoreFile, exception);
        }
    }

    private void writePlayers(Iterable<PlayerRecord> players) throws IOException {
        List<String> lines = new ArrayList<>();
        for (PlayerRecord player : players) {
            String encodedName = Base64.getEncoder().encodeToString(
                    player.name().getBytes(StandardCharsets.UTF_8));
            lines.add(player.id() + "\t" + encodedName + "\t" + player.highScore());
        }
        writeAtomically(dataDirectory.resolve(LEADERBOARD_FILE_NAME), lines);
    }

    private void writeHighScore(long score) throws IOException {
        writeAtomically(dataDirectory.resolve(HIGH_SCORE_FILE_NAME), List.of(Long.toString(score)));
    }

    private void writeHistory(List<GameRecord> records) throws IOException {
        List<String> lines = new ArrayList<>();
        for (GameRecord record : records) {
            String encodedName = Base64.getEncoder().encodeToString(
                    record.playerName().getBytes(StandardCharsets.UTF_8));
            lines.add(encodedName + "\t" + record.score() + "\t" + record.level()
                    + "\t" + record.lines() + "\t" + record.completedAt());
        }
        writeAtomically(dataDirectory.resolve(GAME_HISTORY_FILE_NAME), lines);
    }

    private static void writeAtomically(Path destination, List<String> lines) throws IOException {
        Path temporaryFile = Files.createTempFile(
                destination.getParent(), destination.getFileName().toString(), ".tmp");
        try {
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, destination, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private void requireInitialized() throws IOException {
        if (!initialized) {
            throw new IOException("Local player records have not been initialized.");
        }
    }
}
