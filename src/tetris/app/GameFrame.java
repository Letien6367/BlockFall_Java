package tetris.app;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import tetris.core.GamePanel;
import tetris.core.GameState;
import tetris.model.Board;
import tetris.ui.ArcadeButton;
import tetris.service.HighScoreManager;
import tetris.ui.MenuPanel;

public final class GameFrame extends JFrame {
    private static final String MENU_SCREEN = "menu";
    private static final String GAME_SCREEN = "game";
    private static final String PAUSED_SCREEN = "paused";
    private static final String GAME_OVER_SCREEN = "game-over";

    private final CardLayout screenLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(screenLayout);
    private GameState gameState = GameState.MENU;
    private final HighScoreManager highScoreManager = new HighScoreManager();
    private final GamePanel gamePanel;
    private final MenuPanel menuPanel;
    private final JLabel finalScoreLabel = new JLabel("Final score: 0", SwingConstants.CENTER);

    public GameFrame() {
        super("BlockFall Desktop");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        gamePanel = new GamePanel(new Board(), this::handleGameStateChange,
                highScoreManager::getHighScore);
        menuPanel = new MenuPanel(this::showGameScreen, this::requestClose);
        menuPanel.setBestScore(highScoreManager.getHighScore());
        contentPanel.add(menuPanel, MENU_SCREEN);
        contentPanel.add(gamePanel, GAME_SCREEN);
        contentPanel.add(createStateScreen("PAUSED", "The game is paused.", null,
                "Resume", this::resumeGame), PAUSED_SCREEN);
        contentPanel.add(createGameOverScreen(), GAME_OVER_SCREEN);
        setContentPane(contentPanel);
        bindGlobalControls();

        setSize(900, 650);
        setLocationRelativeTo(null);
    }

    public GameState getGameState() {
        return gameState;
    }

    private void showGameScreen() {
        gamePanel.resetGame();
        gameState = GameState.PLAYING;
        screenLayout.show(contentPanel, GAME_SCREEN);
        gamePanel.startGame();
    }

    private void handleGameStateChange(GameState state) {
        gameState = state;
        if (state == GameState.GAME_OVER) {
            long bestScore = highScoreManager.saveIfHigher(gamePanel.getScore());
            menuPanel.setBestScore(bestScore);
            finalScoreLabel.setText("Final score: " + gamePanel.getScore());
            gamePanel.repaint();
            screenLayout.show(contentPanel, GAME_OVER_SCREEN);
        }
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
            showGameScreen();
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
        return createStateScreen("GAME OVER", null, finalScoreLabel,
                "Play Again", this::showGameScreen);
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
        ArcadeButton menuButton = new ArcadeButton("Main Menu", this::returnToMenu);
        ArcadeButton exitButton = new ArcadeButton("Exit", this::requestClose);
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
}
