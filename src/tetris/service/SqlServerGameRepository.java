package tetris.service;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

public final class SqlServerGameRepository {
    public static final int MAX_HISTORY_RECORDS = 100;
    private static final int MAX_PLAYERS_DISPLAYED = 10;
    private static final String DEFAULT_DATABASE_URL =
            "jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;"
                    + "databaseName=BlockFall;encrypt=true;trustServerCertificate=true";
    private static final String LEGACY_MIGRATION_KEY = "legacy-text-files-v1";
    private static final String GLOBAL_HIGH_SCORE_KEY = "global_high_score";
    private static final String GLOBAL_HIGH_LEVEL_KEY = "global_high_level";

    private final String databaseUrl;
    private final String databaseUser;
    private final String databasePassword;
    private final Path legacyDataDirectory;
    private volatile long highScore;
    private volatile boolean initialized;

    public SqlServerGameRepository() {
        this(System.getenv("BLOCKFALL_DB_URL"), System.getenv("BLOCKFALL_DB_USER"),
                System.getenv("BLOCKFALL_DB_PASSWORD"),
                Path.of(System.getProperty("user.home"), ".blockfall"));
    }

    SqlServerGameRepository(String databaseUrl, String databaseUser, String databasePassword,
            Path legacyDataDirectory) {
        this.databaseUrl = databaseUrl == null || databaseUrl.isBlank()
                ? DEFAULT_DATABASE_URL : databaseUrl.strip();
        this.databaseUser = databaseUser;
        this.databasePassword = databasePassword;
        this.legacyDataDirectory = Objects.requireNonNull(
                legacyDataDirectory, "legacyDataDirectory");
    }

    public void initialize() throws SQLException, IOException {
        try (Connection connection = openConnection()) {
            createSchema(connection);
        }
        migrateLegacyData();
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT stat_value FROM dbo.BlockFallStats WHERE stat_key = ?")) {
            statement.setString(1, GLOBAL_HIGH_SCORE_KEY);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Database is missing the global high-score value.");
                }
                highScore = result.getLong(1);
            }
        }
        initialized = true;
    }

    public long getHighScore() {
        return highScore;
    }

    public List<PlayerRecord> getTopPlayers() throws SQLException {
        requireInitialized();
        String query = "SELECT TOP (" + MAX_PLAYERS_DISPLAYED + ") player_id, player_name, "
                + "high_score FROM dbo.BlockFallPlayers "
                + "ORDER BY high_score DESC, player_name ASC, "
                + "CONVERT(char(36), player_id) COLLATE Latin1_General_100_BIN2 ASC";
        try (Connection connection = openConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(query)) {
            List<PlayerRecord> players = new ArrayList<>();
            while (result.next()) {
                players.add(new PlayerRecord(result.getObject("player_id", UUID.class),
                        result.getString("player_name"), result.getLong("high_score")));
            }
            return List.copyOf(players);
        }
    }

    public PlayerRecord findOrCreatePlayer(String name) throws SQLException {
        requireInitialized();
        String normalizedName = PlayerRecord.normalizeName(name);
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                PlayerRecord existing = findPlayerByName(connection, normalizedName, true);
                if (existing != null) {
                    connection.commit();
                    return existing;
                }

                PlayerRecord created = new PlayerRecord(UUID.randomUUID(), normalizedName, 0);
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO dbo.BlockFallPlayers (player_id, player_name, high_score) "
                                + "VALUES (?, ?, 0)")) {
                    statement.setObject(1, created.id());
                    statement.setString(2, created.name());
                    statement.executeUpdate();
                }
                connection.commit();
                return created;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public PlayerRecord saveGame(PlayerRecord player, long score, int level, int lines)
            throws SQLException {
        requireInitialized();
        Objects.requireNonNull(player, "player");
        if (score < 0 || level < 1 || lines < 0) {
            throw new IllegalArgumentException("Invalid game statistics");
        }

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO dbo.BlockFallGameRecords "
                                + "(player_id, score, level, lines_cleared, completed_at) "
                                    + "VALUES (?, ?, ?, ?, TODATETIMEOFFSET("
                                    + "SYSUTCDATETIME(), '+00:00'))")) {
                    statement.setObject(1, player.id());
                    statement.setLong(2, score);
                    statement.setInt(3, level);
                    statement.setInt(4, lines);
                    statement.executeUpdate();
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE dbo.BlockFallPlayers SET "
                                + "high_score = CASE WHEN high_score < ? THEN ? ELSE high_score END, "
                                + "high_level = CASE WHEN high_level < ? THEN ? ELSE high_level END "
                                + "WHERE player_id = ?")) {
                    statement.setLong(1, score);
                    statement.setLong(2, score);
                    statement.setInt(3, level);
                    statement.setInt(4, level);
                    statement.setObject(5, player.id());
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Cannot save a game for an unknown player.");
                    }
                }
                updateStatistic(connection, GLOBAL_HIGH_SCORE_KEY, score);
                updateStatistic(connection, GLOBAL_HIGH_LEVEL_KEY, level);
                PlayerRecord updatedPlayer = findPlayerById(connection, player.id());
                connection.commit();
                highScore = Math.max(highScore, score);
                return updatedPlayer;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public List<GameRecord> getRecentGames(int limit) throws SQLException {
        requireInitialized();
        if (limit < 1 || limit > MAX_HISTORY_RECORDS) {
            throw new IllegalArgumentException("History limit must be between 1 and "
                    + MAX_HISTORY_RECORDS);
        }

        String query = "SELECT TOP (?) p.player_name, r.score, r.level, r.lines_cleared, "
                + "r.completed_at FROM dbo.BlockFallGameRecords r "
                + "INNER JOIN dbo.BlockFallPlayers p ON p.player_id = r.player_id "
                + "ORDER BY r.completed_at DESC, r.record_id DESC";
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, limit);
            try (ResultSet result = statement.executeQuery()) {
                List<GameRecord> records = new ArrayList<>();
                while (result.next()) {
                    OffsetDateTime completedAt =
                            result.getObject("completed_at", OffsetDateTime.class);
                    records.add(new GameRecord(result.getString("player_name"),
                            result.getLong("score"), result.getInt("level"),
                            result.getInt("lines_cleared"), completedAt.toInstant()));
                }
                return List.copyOf(records);
            }
        }
    }

    private Connection openConnection() throws SQLException {
        if (!databaseUrl.startsWith("jdbc:sqlserver:")) {
            throw new SQLException("BLOCKFALL_DB_URL must be a SQL Server JDBC URL.");
        }
        if (databaseUser == null || databaseUser.isBlank()
                || databasePassword == null || databasePassword.isEmpty()) {
            throw new SQLException("Set BLOCKFALL_DB_USER and BLOCKFALL_DB_PASSWORD for "
                    + "the SQL Server application login.");
        }

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException exception) {
            throw new SQLException("Microsoft SQL Server JDBC driver is missing from the "
                    + "application package.", exception);
        }

        Properties properties = new Properties();
        properties.setProperty("user", databaseUser);
        properties.setProperty("password", databasePassword);
        return DriverManager.getConnection(databaseUrl, properties);
    }

    private static void createSchema(Connection connection) throws SQLException {
        String[] statements = {
            "IF OBJECT_ID(N'dbo.BlockFallPlayers', N'U') IS NULL "
                    + "CREATE TABLE dbo.BlockFallPlayers ("
                    + "player_id uniqueidentifier NOT NULL PRIMARY KEY, "
                    + "player_name nvarchar(24) COLLATE Latin1_General_100_BIN2 NOT NULL "
                    + "UNIQUE, high_score bigint NOT NULL DEFAULT 0 CHECK (high_score >= 0), "
                    + "high_level int NOT NULL DEFAULT 1 CHECK (high_level >= 1), "
                    + "created_at datetime2(0) NOT NULL DEFAULT SYSUTCDATETIME())",
            "IF OBJECT_ID(N'dbo.BlockFallGameRecords', N'U') IS NULL "
                    + "CREATE TABLE dbo.BlockFallGameRecords ("
                    + "record_id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY, "
                    + "player_id uniqueidentifier NOT NULL REFERENCES dbo.BlockFallPlayers "
                    + "(player_id), score bigint NOT NULL CHECK (score >= 0), "
                    + "level int NOT NULL CHECK (level >= 1), "
                    + "lines_cleared int NOT NULL CHECK (lines_cleared >= 0), "
                    + "completed_at datetimeoffset(0) NOT NULL)",
            "IF OBJECT_ID(N'dbo.BlockFallStats', N'U') IS NULL "
                    + "CREATE TABLE dbo.BlockFallStats ("
                    + "stat_key varchar(64) NOT NULL PRIMARY KEY, stat_value bigint NOT NULL)",
            "IF OBJECT_ID(N'dbo.BlockFallMigrations', N'U') IS NULL "
                    + "CREATE TABLE dbo.BlockFallMigrations ("
                    + "migration_key nvarchar(100) COLLATE Latin1_General_100_BIN2 "
                    + "NOT NULL PRIMARY KEY, applied_at datetime2(0) NOT NULL "
                    + "DEFAULT SYSUTCDATETIME())",
            "IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = "
                    + "N'IX_BlockFallGameRecords_CompletedAt' AND object_id = "
                    + "OBJECT_ID(N'dbo.BlockFallGameRecords')) "
                    + "CREATE INDEX IX_BlockFallGameRecords_CompletedAt "
                    + "ON dbo.BlockFallGameRecords (completed_at DESC, record_id DESC) "
                    + "INCLUDE (player_id, score, level, lines_cleared)",
            "IF NOT EXISTS (SELECT 1 FROM dbo.BlockFallStats WHERE stat_key = '"
                    + GLOBAL_HIGH_SCORE_KEY + "') "
                    + "INSERT INTO dbo.BlockFallStats (stat_key, stat_value) VALUES ('"
                    + GLOBAL_HIGH_SCORE_KEY + "', 0)",
            "IF NOT EXISTS (SELECT 1 FROM dbo.BlockFallStats WHERE stat_key = '"
                    + GLOBAL_HIGH_LEVEL_KEY + "') "
                    + "INSERT INTO dbo.BlockFallStats (stat_key, stat_value) VALUES ('"
                    + GLOBAL_HIGH_LEVEL_KEY + "', 1)"
        };
        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }

    private void migrateLegacyData() throws SQLException, IOException {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                if (migrationAlreadyApplied(connection)) {
                    connection.commit();
                    return;
                }

                LegacyDataImporter.Snapshot legacy =
                        LegacyDataImporter.read(legacyDataDirectory);
                for (PlayerRecord player : legacy.players()) {
                    importPlayer(connection, player);
                }
                updateStatistic(connection, GLOBAL_HIGH_SCORE_KEY, legacy.highScore());
                updateStatistic(connection, GLOBAL_HIGH_LEVEL_KEY, legacy.highLevel());
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO dbo.BlockFallMigrations (migration_key) VALUES (?)")) {
                    statement.setString(1, LEGACY_MIGRATION_KEY);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | IOException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    private static boolean migrationAlreadyApplied(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT migration_key FROM dbo.BlockFallMigrations "
                        + "WITH (UPDLOCK, HOLDLOCK) WHERE migration_key = ?")) {
            statement.setString(1, LEGACY_MIGRATION_KEY);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static void importPlayer(Connection connection, PlayerRecord legacyPlayer)
            throws SQLException {
        PlayerRecord existing = findPlayerByName(connection, legacyPlayer.name(), true);
        if (existing != null) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE dbo.BlockFallPlayers SET high_score = "
                            + "CASE WHEN high_score < ? THEN ? ELSE high_score END "
                            + "WHERE player_id = ?")) {
                statement.setLong(1, legacyPlayer.highScore());
                statement.setLong(2, legacyPlayer.highScore());
                statement.setObject(3, existing.id());
                statement.executeUpdate();
            }
            return;
        }

        PlayerRecord idCollision = findPlayerById(connection, legacyPlayer.id());
        if (idCollision != null) {
            throw new SQLException("Legacy player ID conflicts with a different database player.");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO dbo.BlockFallPlayers (player_id, player_name, high_score) "
                        + "VALUES (?, ?, ?)")) {
            statement.setObject(1, legacyPlayer.id());
            statement.setString(2, legacyPlayer.name());
            statement.setLong(3, legacyPlayer.highScore());
            statement.executeUpdate();
        }
    }

    private static PlayerRecord findPlayerByName(Connection connection, String name,
            boolean lockForUpdate) throws SQLException {
        String lock = lockForUpdate ? " WITH (UPDLOCK, HOLDLOCK)" : "";
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT player_id, player_name, high_score FROM dbo.BlockFallPlayers"
                        + lock + " WHERE player_name = ?")) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? new PlayerRecord(result.getObject("player_id", UUID.class),
                                result.getString("player_name"), result.getLong("high_score"))
                        : null;
            }
        }
    }

    private static PlayerRecord findPlayerById(Connection connection, UUID id)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT player_id, player_name, high_score FROM dbo.BlockFallPlayers "
                        + "WHERE player_id = ?")) {
            statement.setObject(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? new PlayerRecord(result.getObject("player_id", UUID.class),
                                result.getString("player_name"), result.getLong("high_score"))
                        : null;
            }
        }
    }

    private static void updateStatistic(Connection connection, String key, long value)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE dbo.BlockFallStats SET stat_value = "
                        + "CASE WHEN stat_value < ? THEN ? ELSE stat_value END "
                        + "WHERE stat_key = ?")) {
            statement.setLong(1, value);
            statement.setLong(2, value);
            statement.setString(3, key);
            if (statement.executeUpdate() == 0) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO dbo.BlockFallStats (stat_key, stat_value) VALUES (?, ?)")) {
                    insert.setString(1, key);
                    insert.setLong(2, value);
                    insert.executeUpdate();
                }
            }
        }
    }

    private static void rollback(Connection connection, Exception cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
        }
    }

    private void requireInitialized() throws SQLException {
        if (!initialized) {
            throw new SQLException("SQL Server database has not been initialized.");
        }
    }
}
