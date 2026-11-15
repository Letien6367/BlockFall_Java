package tetris.core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import tetris.model.Board;
import tetris.model.Tetromino;
import tetris.model.TetrominoType;
import tetris.service.ScoreManager;

public final class GamePanel extends JPanel {
    private static final int CELL_SIZE = 26;
    private static final int HORIZONTAL_MARGIN = 35;
    private static final int VERTICAL_MARGIN = 52;
    private static final int INFO_PANEL_WIDTH = 220;
    private static final int PANEL_GAP = 55;
    private static final Color BACKGROUND_COLOR = new Color(14, 20, 40);
    private static final Color BOARD_COLOR = new Color(8, 12, 27);
    private static final Color GRID_COLOR = new Color(37, 47, 72);
    private static final Color BORDER_COLOR = new Color(82, 101, 142);
    private static final Color SETTLED_BLOCK_COLOR = new Color(70, 207, 190);
    private static final Color INFO_PANEL_COLOR = new Color(24, 33, 59);
    private static final Color PRIMARY_TEXT_COLOR = new Color(239, 244, 255);
    private static final Color SECONDARY_TEXT_COLOR = new Color(157, 174, 207);
    private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 24);
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    private static final Font VALUE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 23);

    private final Board board;
    private final ScoreManager scoreManager = new ScoreManager();
    private final Timer gravityTimer;
    private final Consumer<GameState> stateChangeHandler;
    private final LongSupplier highScoreSupplier;
    private Tetromino activeTetromino;
    private TetrominoType nextType;

    public GamePanel(Board board) {
        this(board, state -> {
        }, () -> 0);
    }

    public GamePanel(Board board, Consumer<GameState> stateChangeHandler) {
        this(board, stateChangeHandler, () -> 0);
    }

    public GamePanel(Board board, Consumer<GameState> stateChangeHandler,
            LongSupplier highScoreSupplier) {
        this.board = Objects.requireNonNull(board, "board");
        this.stateChangeHandler = Objects.requireNonNull(stateChangeHandler, "stateChangeHandler");
        this.highScoreSupplier = Objects.requireNonNull(highScoreSupplier, "highScoreSupplier");
        setBackground(BACKGROUND_COLOR);
        setPreferredSize(new Dimension(900, 650));
        setMinimumSize(new Dimension(360, 300));
        setFocusable(true);
        gravityTimer = new Timer(scoreManager.getGravityDelayMillis(), event -> gravityTick());
        bindMovementKeys();
        nextType = randomTetrominoType();
        spawnTetromino();
    }

    public void startGame() {
        gravityTimer.start();
        requestFocusInWindow();
    }

    public void pauseGame() {
        gravityTimer.stop();
    }

    public void resumeGame() {
        if (activeTetromino != null) {
            gravityTimer.start();
            requestFocusInWindow();
        }
    }

    public void resetGame() {
        gravityTimer.stop();
        board.clear();
        scoreManager.reset();
        gravityTimer.setDelay(scoreManager.getGravityDelayMillis());
        gravityTimer.setInitialDelay(scoreManager.getGravityDelayMillis());
        nextType = randomTetrominoType();
        spawnTetromino();
        repaint();
    }

    public long getScore() {
        return scoreManager.getScore();
    }

    boolean isGravityTimerRunning() {
        return gravityTimer.isRunning();
    }

    Tetromino getActiveTetromino() {
        return activeTetromino;
    }

    private void bindMovementKeys() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        bindKey(inputMap, actionMap, "move-left", KeyStroke.getKeyStroke("pressed LEFT"),
                KeyStroke.getKeyStroke("pressed A"), -1, 0);
        bindKey(inputMap, actionMap, "move-right", KeyStroke.getKeyStroke("pressed RIGHT"),
                KeyStroke.getKeyStroke("pressed D"), 1, 0);
        bindKey(inputMap, actionMap, "soft-drop", KeyStroke.getKeyStroke("pressed DOWN"),
                KeyStroke.getKeyStroke("pressed S"), 0, 1);
        bindRotationKey(inputMap, actionMap, "rotate-clockwise",
                KeyStroke.getKeyStroke("pressed UP"), KeyStroke.getKeyStroke("pressed X"), 1);
        bindRotationKey(inputMap, actionMap, "rotate-counterclockwise",
                KeyStroke.getKeyStroke("pressed Z"), null, -1);
        inputMap.put(KeyStroke.getKeyStroke("pressed SPACE"), "hard-drop");
        actionMap.put("hard-drop", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                hardDropActiveTetromino();
            }
        });
    }

    private void gravityTick() {
        if (!tryMoveActiveTetromino(0, 1)) {
            lockAndSpawnNext();
        }
    }

    private void bindRotationKey(InputMap inputMap, ActionMap actionMap, String actionName,
            KeyStroke primaryKey, KeyStroke alternateKey, int direction) {
        AbstractAction action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (activeTetromino.tryRotate(board, direction)) {
                    repaint();
                }
            }
        };
        inputMap.put(primaryKey, actionName);
        if (alternateKey != null) {
            inputMap.put(alternateKey, actionName);
        }
        actionMap.put(actionName, action);
    }

    private void bindKey(InputMap inputMap, ActionMap actionMap, String actionName,
            KeyStroke primaryKey, KeyStroke alternateKey, int columns, int rows) {
        AbstractAction action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (!tryMoveActiveTetromino(columns, rows) && rows > 0) {
                    lockAndSpawnNext();
                }
            }
        };
        inputMap.put(primaryKey, actionName);
        inputMap.put(alternateKey, actionName);
        actionMap.put(actionName, action);
    }

    boolean tryMoveActiveTetromino(int columns, int rows) {
        if (activeTetromino == null) {
            return false;
        }
        activeTetromino.moveBy(columns, rows);
        if (!board.isValidPosition(activeTetromino)) {
            activeTetromino.moveBy(-columns, -rows);
            return false;
        }
        repaint();
        return true;
    }

    private void spawnTetromino() {
        TetrominoType currentType = nextType;
        nextType = randomTetrominoType();
        activeTetromino = new Tetromino(currentType, (Board.COLUMNS - 4) / 2, 0);
        if (!board.isValidPosition(activeTetromino)) {
            activeTetromino = null;
            gravityTimer.stop();
            stateChangeHandler.accept(GameState.GAME_OVER);
        }
    }

    private TetrominoType randomTetrominoType() {
        TetrominoType[] types = TetrominoType.values();
        return types[ThreadLocalRandom.current().nextInt(types.length)];
    }

    private void hardDropActiveTetromino() {
        if (activeTetromino == null) {
            return;
        }
        int droppedCells = 0;
        while (tryMoveActiveTetromino(0, 1)) {
            droppedCells++;
        }
        scoreManager.addHardDropDistance(droppedCells);
        lockAndSpawnNext();
    }

    private void lockAndSpawnNext() {
        if (activeTetromino == null) {
            return;
        }
        board.lock(activeTetromino);
        scoreManager.addClearedLines(board.clearFullRows());
        gravityTimer.setDelay(scoreManager.getGravityDelayMillis());
        gravityTimer.setInitialDelay(scoreManager.getGravityDelayMillis());
        spawnTetromino();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cellSize = getCellSize();
            int boardWidth = Board.COLUMNS * cellSize;
            int boardHeight = Board.ROWS * cellSize;
            int compositionWidth = boardWidth + PANEL_GAP + INFO_PANEL_WIDTH;
            int boardX = Math.max(HORIZONTAL_MARGIN, (getWidth() - compositionWidth) / 2);
            int boardY = Math.max(VERTICAL_MARGIN, (getHeight() - boardHeight) / 2);

            drawBoard(g2, boardX, boardY, cellSize, boardWidth, boardHeight);
            if (activeTetromino != null) {
                drawActiveTetromino(g2, boardX, boardY, cellSize);
            } else {
                drawGameOver(g2, boardX, boardY, boardWidth, boardHeight);
            }
            drawInfoPanel(g2, boardX + boardWidth + PANEL_GAP, boardY + 36);
        } finally {
            g2.dispose();
        }
    }

    private int getCellSize() {
        int heightLimitedSize = Math.max(1, (getHeight() - 2 * VERTICAL_MARGIN) / Board.ROWS);
        int widthAvailable = getWidth() - 2 * HORIZONTAL_MARGIN - PANEL_GAP - INFO_PANEL_WIDTH;
        int widthLimitedSize = Math.max(1, widthAvailable / Board.COLUMNS);
        return Math.max(1, Math.min(CELL_SIZE, Math.min(heightLimitedSize, widthLimitedSize)));
    }

    private void drawBoard(Graphics2D g2, int x, int y, int cellSize, int width, int height) {
        g2.setColor(BOARD_COLOR);
        g2.fillRect(x, y, width, height);

        for (int row = 0; row < Board.ROWS; row++) {
            for (int column = 0; column < Board.COLUMNS; column++) {
                int cellX = x + column * cellSize;
                int cellY = y + row * cellSize;
                if (board.isOccupied(row, column)) {
                    drawBlock(g2, cellX, cellY, cellSize, SETTLED_BLOCK_COLOR);
                }
            }
        }

        g2.setColor(GRID_COLOR);
        for (int column = 0; column <= Board.COLUMNS; column++) {
            int lineX = x + column * cellSize;
            g2.drawLine(lineX, y, lineX, y + height);
        }
        for (int row = 0; row <= Board.ROWS; row++) {
            int lineY = y + row * cellSize;
            g2.drawLine(x, lineY, x + width, lineY);
        }

        g2.setColor(BORDER_COLOR);
        g2.drawRect(x, y, width, height);
    }

    private void drawActiveTetromino(Graphics2D g2, int boardX, int boardY, int cellSize) {
        for (Tetromino.GridCell cell : activeTetromino.getOccupiedCells()) {
            int cellX = boardX + cell.column() * cellSize;
            int cellY = boardY + cell.row() * cellSize;
            drawBlock(g2, cellX, cellY, cellSize, activeTetromino.getType().getColor());
        }
    }

    private void drawBlock(Graphics2D g2, int x, int y, int cellSize, Color color) {
        g2.setColor(color);
        g2.fillRect(x + 1, y + 1, cellSize - 1, cellSize - 1);
    }

    private void drawGameOver(Graphics2D g2, int x, int y, int width, int height) {
        g2.setColor(new Color(8, 12, 27, 190));
        g2.fillRect(x, y, width, height);
        g2.setColor(PRIMARY_TEXT_COLOR);
        g2.setFont(TITLE_FONT);
        String text = "GAME OVER";
        int textWidth = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, x + (width - textWidth) / 2, y + height / 2);
    }

    private void drawInfoPanel(Graphics2D g2, int x, int y) {
        int panelHeight = 430;
        g2.setColor(INFO_PANEL_COLOR);
        g2.fillRoundRect(x, y, INFO_PANEL_WIDTH, panelHeight, 18, 18);

        g2.setColor(PRIMARY_TEXT_COLOR);
        g2.setFont(TITLE_FONT);
        g2.drawString("BLOCKFALL", x + 20, y + 38);

        drawStat(g2, "SCORE", Long.toString(scoreManager.getScore()), x + 20, y + 83);
        drawStat(g2, "BEST", Long.toString(highScoreSupplier.getAsLong()), x + 20, y + 145);
        drawStat(g2, "LEVEL", Integer.toString(scoreManager.getLevel()), x + 20, y + 207);
        drawStat(g2, "LINES", Integer.toString(scoreManager.getLines()), x + 20, y + 269);

        g2.setColor(SECONDARY_TEXT_COLOR);
        g2.setFont(LABEL_FONT);
        g2.drawString("NEXT", x + 20, y + 322);
        drawNextPiece(g2, x + 20, y + 334);
    }

    private void drawNextPiece(Graphics2D g2, int x, int y) {
        int previewCellSize = 17;
        for (TetrominoType.Cell cell : nextType.getCells()) {
            int cellX = x + cell.column() * previewCellSize;
            int cellY = y + cell.row() * previewCellSize;
            drawBlock(g2, cellX, cellY, previewCellSize, nextType.getColor());
        }
    }

    private void drawStat(Graphics2D g2, String label, String value, int x, int y) {
        g2.setColor(SECONDARY_TEXT_COLOR);
        g2.setFont(LABEL_FONT);
        g2.drawString(label, x, y);

        g2.setColor(PRIMARY_TEXT_COLOR);
        Font valueFont = VALUE_FONT;
        int maxValueWidth = INFO_PANEL_WIDTH - 40;
        while (valueFont.getSize() > 12
                && g2.getFontMetrics(valueFont).stringWidth(value) > maxValueWidth) {
            valueFont = valueFont.deriveFont((float) valueFont.getSize() - 1);
        }
        g2.setFont(valueFont);
        g2.drawString(value, x, y + 29);
    }
}
