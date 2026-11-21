package tetris.app;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import tetris.core.GamePanel;
import tetris.core.GameState;
import tetris.model.Board;
import tetris.ui.ArcadeButton;
import tetris.service.GameRecord;
import tetris.service.PlayerRecord;
import tetris.service.SqlServerGameRepository;
import tetris.ui.HistoryPanel;
import tetris.ui.MenuPanel;

public final class GameFrame extends JFrame {
    private static final String MENU_SCREEN = "menu";
    private static final String GAME_SCREEN = "game";
    private static final String PAUSED_SCREEN = "paused";
    private static final String GAME_OVER_SCREEN = "game-over";
    private static final String HISTORY_SCREEN = "history";

    private final CardLayout screenLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(screenLayout);
    private GameState gameState = GameState.MENU;
    private final SqlServerGameRepository gameRepository = new SqlServerGameRepository();
    private final GamePanel gamePanel;
    private final MenuPanel menuPanel;
    private final HistoryPanel historyPanel;
    private final JLabel finalScoreLabel = new JLabel("Điểm cuối: 0", SwingConstants.CENTER);
    private PlayerRecord activePlayer;
    private ArcadeButton fastFallButton;
    private ArcadeButton soundButton;
    private boolean databaseReady;
    private long gameRunId;

    public GameFrame() {
        super("BlockFall");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        gamePanel = new GamePanel(new Board(), this::handleGameStateChange,
                gameRepository::getHighScore);
        menuPanel = new MenuPanel(this::showGameScreen, this::requestClose,
                this::showHistoryScreen);
        menuPanel.setDatabaseReady(false, "Đang kết nối SQL Server...");
        menuPanel.setLeaderboard(List.of());
        historyPanel = new HistoryPanel(this::returnToMenu);
        contentPanel.add(menuPanel, MENU_SCREEN);
        contentPanel.add(createGameScreen(), GAME_SCREEN);
        contentPanel.add(createStateScreen("TẠM DỪNG", "Trò chơi đang tạm dừng.", null,
                "Tiếp tục", this::resumeGame), PAUSED_SCREEN);
        contentPanel.add(createGameOverScreen(), GAME_OVER_SCREEN);
        contentPanel.add(historyPanel, HISTORY_SCREEN);
        setContentPane(contentPanel);
        bindGlobalControls();

        setSize(900, 650);
        setLocationRelativeTo(null);
        initializeDatabase();
    }

    private JPanel createGameScreen() {
        JPanel screen = new JPanel(new BorderLayout());
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 5));
        controls.setBackground(new Color(14, 20, 40));
        fastFallButton = new ArcadeButton("Tua x2", this::toggleFastFall);
        soundButton = new ArcadeButton("Âm thanh: bật", this::toggleSound);
        controls.add(new ArcadeButton("Tạm dừng", this::pauseGame));
        controls.add(fastFallButton);
        controls.add(soundButton);
        screen.add(controls, BorderLayout.NORTH);
        screen.add(gamePanel, BorderLayout.CENTER);
        return screen;
    }

    private void toggleFastFall() {
        boolean enabled = gamePanel.toggleFastFall();
        fastFallButton.setText(enabled ? "Tua 1x" : "Tua x2");
    }

    private void toggleSound() {
        boolean enabled = gamePanel.toggleSound();
        soundButton.setText(enabled ? "Âm thanh: bật" : "Âm thanh: tắt");
    }

    public GameState getGameState() {
        return gameState;
    }

    private void showGameScreen(String playerName) {
        if (!databaseReady) {
            return;
        }
        menuPanel.setBusy(true);
        new SwingWorker<PlayerRecord, Void>() {
            @Override
            protected PlayerRecord doInBackground() throws Exception {
                return gameRepository.findOrCreatePlayer(playerName);
            }

            @Override
            protected void done() {
                menuPanel.setBusy(false);
                try {
                    activePlayer = get();
                    menuPanel.setPlayerName(activePlayer.name());
                    startGameScreen();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showWorkerFailure("Không thể bắt đầu", exception);
                } catch (ExecutionException exception) {
                    showWorkerFailure("Không thể bắt đầu", exception);
                }
            }
        }.execute();
    }

    private void startGameScreen() {
        if (activePlayer == null) {
            return;
        }
        gameRunId++;
        gamePanel.resetGame();
        if (fastFallButton != null) {
            fastFallButton.setText("Tua x2");
        }
        if (soundButton != null) {
            soundButton.setText(gamePanel.isSoundEnabled()
                    ? "Âm thanh: bật" : "Âm thanh: tắt");
        }
        gameState = GameState.PLAYING;
        screenLayout.show(contentPanel, GAME_SCREEN);
        gamePanel.startGame();
    }

    private void handleGameStateChange(GameState state) {
        gameState = state;
        if (state == GameState.GAME_OVER) {
            finalScoreLabel.setText("Đang lưu kết quả...");
            gamePanel.repaint();
            screenLayout.show(contentPanel, GAME_OVER_SCREEN);
            if (activePlayer != null) {
                PlayerRecord playerAtGameOver = activePlayer;
                long score = gamePanel.getScore();
                int level = gamePanel.getLevel();
                int lines = gamePanel.getLines();
                long runId = gameRunId;
                new SwingWorker<SaveResult, Void>() {
                    @Override
                    protected SaveResult doInBackground() throws Exception {
                        PlayerRecord savedPlayer = gameRepository.saveGame(
                                playerAtGameOver, score, level, lines);
                        return new SaveResult(savedPlayer, gameRepository.getTopPlayers());
                    }

                    @Override
                    protected void done() {
                        try {
                            SaveResult result = get();
                            menuPanel.setLeaderboard(result.topPlayers());
                            if (activePlayer != null
                                    && activePlayer.id().equals(playerAtGameOver.id())) {
                                activePlayer = result.player();
                            }
                            if (gameRunId == runId && gameState == GameState.GAME_OVER) {
                                finalScoreLabel.setText("Điểm cuối: " + score
                                        + "  |  Cấp độ: " + level);
                            }
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            reportSaveFailure(exception, runId);
                        } catch (ExecutionException exception) {
                            reportSaveFailure(exception, runId);
                        }
                    }
                }.execute();
            } else {
                finalScoreLabel.setText("Không tìm thấy hồ sơ người chơi.");
            }
        }
    }

    private void initializeDatabase() {
        new SwingWorker<List<PlayerRecord>, Void>() {
            @Override
            protected List<PlayerRecord> doInBackground() throws Exception {
                gameRepository.initialize();
                return gameRepository.getTopPlayers();
            }

            @Override
            protected void done() {
                try {
                    menuPanel.setLeaderboard(get());
                    databaseReady = true;
                    menuPanel.setDatabaseReady(true, "Đã kết nối database.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showDatabaseInitializationFailure(exception);
                } catch (ExecutionException exception) {
                    showDatabaseInitializationFailure(exception);
                }
            }
        }.execute();
    }

    private void showDatabaseInitializationFailure(Exception exception) {
        Throwable cause = exception instanceof ExecutionException && exception.getCause() != null
                ? exception.getCause() : exception;
        String message = cause.getMessage() == null
                ? cause.getClass().getSimpleName() : cause.getMessage();
        menuPanel.setDatabaseReady(false, "Database chưa sẵn sàng: " + message);
        System.err.println("Unable to initialize the game database: " + message);
        JOptionPane.showMessageDialog(this,
                "Không thể khởi tạo database SQL Server.\n" + message
                        + "\nKiểm tra dịch vụ SQL Server, database BlockFall và cấu hình kết nối.",
                "Lỗi kết nối database", JOptionPane.ERROR_MESSAGE);
    }

    private void showHistoryScreen() {
        if (!databaseReady) {
            return;
        }
        historyPanel.setLoading();
        screenLayout.show(contentPanel, HISTORY_SCREEN);
        new SwingWorker<List<GameRecord>, Void>() {
            @Override
            protected List<GameRecord> doInBackground() throws Exception {
                return gameRepository.getRecentGames(SqlServerGameRepository.MAX_HISTORY_RECORDS);
            }

            @Override
            protected void done() {
                try {
                    historyPanel.setHistory(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showHistoryFailure(exception);
                } catch (ExecutionException exception) {
                    showHistoryFailure(exception);
                }
            }
        }.execute();
    }

    private void showHistoryFailure(Exception exception) {
        Throwable cause = exception instanceof ExecutionException && exception.getCause() != null
                ? exception.getCause() : exception;
        String message = cause.getMessage() == null
                ? cause.getClass().getSimpleName() : cause.getMessage();
        historyPanel.setLoadError(message);
        System.err.println("Unable to load game history: " + message);
        JOptionPane.showMessageDialog(this, "Không thể tải lịch sử ván chơi.\n" + message,
                "Lỗi tải lịch sử", JOptionPane.ERROR_MESSAGE);
    }

    private void reportSaveFailure(Exception exception, long runId) {
        Throwable cause = exception instanceof ExecutionException && exception.getCause() != null
                ? exception.getCause() : exception;
        String message = cause.getMessage() == null
                ? cause.getClass().getSimpleName() : cause.getMessage();
        if (gameRunId == runId && gameState == GameState.GAME_OVER) {
            finalScoreLabel.setText("Không thể lưu thành tích.");
        }
        System.err.println("Unable to save game result: " + message);
        JOptionPane.showMessageDialog(this, "Không thể lưu kết quả vào database.\n" + message,
                "Lỗi lưu thành tích", JOptionPane.ERROR_MESSAGE);
    }

    private void showWorkerFailure(String title, Exception exception) {
        Throwable cause = exception instanceof ExecutionException && exception.getCause() != null
                ? exception.getCause() : exception;
        String message = cause.getMessage() == null
                ? cause.getClass().getSimpleName() : cause.getMessage();
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void pauseGame() {
        if (gameState == GameState.PLAYING) {
            gamePanel.pauseGame();
            gameState = GameState.PAUSED;
            screenLayout.show(contentPanel, PAUSED_SCREEN);
        }
    }

    private void resumeGame() {
        if (gameState == GameState.PAUSED) {
            gameState = GameState.PLAYING;
            screenLayout.show(contentPanel, GAME_SCREEN);
            gamePanel.resumeGame();
        }
    }

    private void restartGame() {
        if (gameState != GameState.MENU) {
            startGameScreen();
        }
    }

    private void returnToMenu() {
        gamePanel.pauseGame();
        gameState = GameState.MENU;
        screenLayout.show(contentPanel, MENU_SCREEN);
    }

    private void bindGlobalControls() {
        getRootPane().getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("pressed P"), "toggle-pause");
        getRootPane().getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("pressed ESCAPE"), "toggle-pause");
        getRootPane().getActionMap().put("toggle-pause", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                if (gameState == GameState.PLAYING) {
                    pauseGame();
                } else if (gameState == GameState.PAUSED) {
                    resumeGame();
                }
            }
        });
        getRootPane().getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("pressed R"), "restart-game");
        getRootPane().getActionMap().put("restart-game", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                restartGame();
            }
        });
    }

    private JPanel createGameOverScreen() {
        return createStateScreen("KẾT THÚC", null, finalScoreLabel,
                "Chơi lại", this::restartGame);
    }

    private JPanel createStateScreen(String title, String message, JLabel dynamicMessage,
            String primaryActionText, Runnable primaryAction) {
        JPanel screen = new JPanel(new GridBagLayout());
        screen.setBackground(new Color(14, 20, 40));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel(title, SwingConstants.CENTER);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        heading.setForeground(Color.WHITE);
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 36));
        content.add(heading);
        content.add(Box.createVerticalStrut(18));
        if (message != null) {
            JLabel description = new JLabel(message, SwingConstants.CENTER);
            description.setAlignmentX(Component.CENTER_ALIGNMENT);
            description.setForeground(new Color(157, 174, 207));
            content.add(description);
            content.add(Box.createVerticalStrut(18));
        } else if (dynamicMessage != null) {
            dynamicMessage.setAlignmentX(Component.CENTER_ALIGNMENT);
            dynamicMessage.setForeground(new Color(157, 174, 207));
            dynamicMessage.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            content.add(dynamicMessage);
            content.add(Box.createVerticalStrut(18));
        }

        ArcadeButton primaryButton = new ArcadeButton(primaryActionText, primaryAction);
        ArcadeButton menuButton = new ArcadeButton("Màn hình chính", this::returnToMenu);
        ArcadeButton exitButton = new ArcadeButton("Thoát", this::requestClose);
        JPanel buttons = new JPanel(new GridLayout(3, 1, 0, 10));
        buttons.setOpaque(false);
        buttons.setPreferredSize(new Dimension(220, 160));
        buttons.add(primaryButton);
        buttons.add(menuButton);
        buttons.add(exitButton);
        content.add(buttons);
        screen.add(content);
        return screen;
    }

    private void requestClose() {
        gamePanel.pauseGame();
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private record SaveResult(PlayerRecord player, List<PlayerRecord> topPlayers) {
    }
}
