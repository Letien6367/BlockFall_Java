package tetris.core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.util.EnumMap;
import java.util.Objects;
import java.util.Random;
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
import tetris.model.TetrominoBag;
import tetris.model.TetrominoType;
import tetris.service.ScoreManager;
import tetris.service.SoundManager;

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
    private static final Color INFO_PANEL_COLOR = new Color(24, 33, 59);
    private static final Color PRIMARY_TEXT_COLOR = new Color(239, 244, 255);
    private static final Color SECONDARY_TEXT_COLOR = new Color(157, 174, 207);
    private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 24);
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    private static final Font VALUE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 23);

    private final Board board;
    private final TetrominoBag tetrominoBag = new TetrominoBag();
    private final ScoreManager scoreManager = new ScoreManager();
    private final SoundManager soundManager = new SoundManager();
    private final Timer gravityTimer;
    private final Consumer<GameState> stateChangeHandler;
    private final LongSupplier highScoreSupplier;
    private final Timer lineClearEffectTimer;
    private final Random paletteRandom = new Random();
    private final EnumMap<TetrominoType, Color> piecePalette = new EnumMap<>(TetrominoType.class);
    private Tetromino activeTetromino;
    private TetrominoType nextType;
    private TetrominoType heldType;
    private boolean holdAvailable = true;
    private boolean fastFallEnabled;
    private int[] lineClearEffectRows = new int[0];
    private int lineClearEffectPhase;

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
        lineClearEffectTimer = new Timer(45, event -> advanceLineClearEffect());
        generateRandomPalette();
        bindMovementKeys();
        nextType = tetrominoBag.next();
        spawnTetromino();
    }

    public void startGame() {
        gravityTimer.start();
        requestFocusInWindow();
    }

    public void pauseGame() {
        gravityTimer.stop();
        lineClearEffectTimer.stop();
    }

    public void resumeGame() {
        if (activeTetromino != null) {
            gravityTimer.start();
            if (isLineClearEffectActive()) {
                lineClearEffectTimer.restart();
            }
            requestFocusInWindow();
        }
    }

    public void resetGame() {
        gravityTimer.stop();
        lineClearEffectTimer.stop();
        lineClearEffectRows = new int[0];
        lineClearEffectPhase = 0;
        board.clear();
        scoreManager.reset();
        generateRandomPalette();
        fastFallEnabled = false;
        updateGravityDelay();
        heldType = null;
        holdAvailable = true;
        tetrominoBag.reset();
        nextType = tetrominoBag.next();
        spawnTetromino();
        repaint();
    }

    public boolean toggleFastFall() {
        fastFallEnabled = !fastFallEnabled;
        updateGravityDelay();
        return fastFallEnabled;
    }

    public boolean isFastFallEnabled() {
        return fastFallEnabled;
    }

    public boolean toggleSound() {
        return soundManager.toggleEnabled();
    }

    public boolean isSoundEnabled() {
        return soundManager.isEnabled();
    }

    public long getScore() {
        return scoreManager.getScore();
    }

    public int getLevel() {
        return scoreManager.getLevel();
    }

    boolean isGravityTimerRunning() {
        return gravityTimer.isRunning();
    }

    Tetromino getActiveTetromino() {
        return activeTetromino;
    }

    TetrominoType getHeldType() {
        return heldType;
    }

    int getGravityDelayMillis() {
        int levelDelay = scoreManager.getGravityDelayMillis();
        return fastFallEnabled ? Math.max(35, levelDelay / 2) : levelDelay;
    }

    boolean isHoldAvailable() {
        return holdAvailable;
    }

    Tetromino getGhostTetromino() {
        if (activeTetromino == null) {
            return null;
        }
        Tetromino ghost = new Tetromino(activeTetromino.getType(),
                activeTetromino.getColumn(), activeTetromino.getRow());
        ghost.setRotationState(activeTetromino.getRotationState());
        while (true) {
            ghost.moveBy(0, 1);
            if (!board.isValidPosition(ghost)) {
                ghost.moveBy(0, -1);
                return ghost;
            }
        }
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
        inputMap.put(KeyStroke.getKeyStroke("pressed C"), "hold-piece");
        actionMap.put("hold-piece", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                holdActiveTetromino();
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
                    soundManager.play(SoundManager.Effect.ROTATE);
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
                } else if (rows > 0) {
                    soundManager.play(SoundManager.Effect.SOFT_DROP);
                } else if (columns != 0) {
                    soundManager.play(SoundManager.Effect.MOVE);
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
        nextType = tetrominoBag.next();
        activateTetromino(currentType);
    }

    private void activateTetromino(TetrominoType type) {
        activeTetromino = new Tetromino(type, (Board.COLUMNS - 4) / 2, 0);
        if (!board.isValidPosition(activeTetromino)) {
            activeTetromino = null;
            gravityTimer.stop();
            lineClearEffectTimer.stop();
            soundManager.play(SoundManager.Effect.GAME_OVER);
            stateChangeHandler.accept(GameState.GAME_OVER);
        }
    }

    private void holdActiveTetromino() {
        if (activeTetromino == null || !holdAvailable) {
            return;
        }

        TetrominoType currentType = activeTetromino.getType();
        holdAvailable = false;
        if (heldType == null) {
            heldType = currentType;
            spawnTetromino();
        } else {
            TetrominoType replacementType = heldType;
            heldType = currentType;
            activateTetromino(replacementType);
        }
        soundManager.play(SoundManager.Effect.HOLD);
        repaint();
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
        soundManager.play(SoundManager.Effect.HARD_DROP);
        lockAndSpawnNext();
    }

    private void lockAndSpawnNext() {
        if (activeTetromino == null) {
            return;
        }
        board.lock(activeTetromino);
        int[] clearedRows = board.getFullRowIndexes();
        int clearedLineCount = board.clearFullRows();
        int previousLevel = scoreManager.getLevel();
        scoreManager.addClearedLines(clearedLineCount);
        if (scoreManager.getLevel() > previousLevel) {
            generateRandomPalette();
        }
        if (clearedLineCount > 0) {
            startLineClearEffect(clearedRows);
            soundManager.play(clearedLineCount == 1
                    ? SoundManager.Effect.CLEAR_ONE
                    : SoundManager.Effect.CLEAR_MULTI);
        } else {
            soundManager.play(SoundManager.Effect.LOCK);
        }
        updateGravityDelay();
        holdAvailable = true;
        spawnTetromino();
        repaint();
    }

    void startLineClearEffect(int[] clearedRows) {
        lineClearEffectRows = clearedRows.clone();
        lineClearEffectPhase = 0;
        lineClearEffectTimer.restart();
    }

    private void advanceLineClearEffect() {
        lineClearEffectPhase++;
        repaint();
        if (lineClearEffectPhase >= 8) {
            lineClearEffectTimer.stop();
            lineClearEffectRows = new int[0];
            lineClearEffectPhase = 0;
        }
    }

    boolean isLineClearEffectActive() {
        return lineClearEffectRows.length > 0;
    }

    private void updateGravityDelay() {
        int delay = getGravityDelayMillis();
        gravityTimer.setDelay(delay);
        gravityTimer.setInitialDelay(delay);
    }

    private void generateRandomPalette() {
        piecePalette.clear();
        float baseHue = paletteRandom.nextFloat();
        float hueStep = 0.12f + paletteRandom.nextFloat() * 0.06f;
        for (TetrominoType type : TetrominoType.values()) {
            float hue = (baseHue + hueStep * type.ordinal()
                    + (paletteRandom.nextFloat() - 0.5f) * 0.035f) % 1f;
            float saturation = 0.72f + paletteRandom.nextFloat() * 0.18f;
            float brightness = 0.78f + paletteRandom.nextFloat() * 0.18f;
            piecePalette.put(type, Color.getHSBColor(hue, saturation, brightness));
        }
    }

    Color getPieceColor(TetrominoType type) {
        return piecePalette.getOrDefault(type, TetrominoType.I.getColor());
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
            drawLineClearEffect(g2, boardX, boardY, cellSize);
            if (activeTetromino != null) {
                drawGhostTetromino(g2, boardX, boardY, cellSize);
                drawActiveTetromino(g2, boardX, boardY, cellSize);
            } else {
                drawGameOver(g2, boardX, boardY, boardWidth, boardHeight);
            }
            drawInfoPanel(g2, boardX + boardWidth + PANEL_GAP, boardY + 36);
        } finally {
            g2.dispose();
        }
    }

    private void drawGhostTetromino(Graphics2D g2, int boardX, int boardY, int cellSize) {
        Tetromino ghost = getGhostTetromino();
        Color color = getPieceColor(ghost.getType());
        Color ghostColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 85);
        for (Tetromino.GridCell cell : ghost.getOccupiedCells()) {
            int cellX = boardX + cell.column() * cellSize;
            int cellY = boardY + cell.row() * cellSize;
            drawBlock(g2, cellX, cellY, cellSize, ghostColor);
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
                    drawBlock(g2, cellX, cellY, cellSize, getPieceColor(TetrominoType.I));
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

    private void drawLineClearEffect(Graphics2D g2, int boardX, int boardY, int cellSize) {
        if (lineClearEffectRows.length == 0) {
            return;
        }
        float progress = lineClearEffectPhase / 8f;
        int flashAlpha = Math.max(0, (int) (230 * (1f - progress)));

        g2.setComposite(java.awt.AlphaComposite.SrcOver.derive(
                Math.max(0.05f, 1f - progress)));
        g2.setColor(new Color(255, 255, 255, flashAlpha));
        for (int row : lineClearEffectRows) {
            g2.fillRect(boardX + 1, boardY + row * cellSize + 1,
                    Board.COLUMNS * cellSize - 2, cellSize - 2);
        }

        g2.setComposite(java.awt.AlphaComposite.SrcOver);
        int particleSize = Math.max(3, cellSize / 5);
        int spread = (int) (cellSize * (0.8f + progress * 2.2f));
        for (int row : lineClearEffectRows) {
            int centerY = boardY + row * cellSize + cellSize / 2;
            for (int column = 0; column < Board.COLUMNS; column++) {
                int originX = boardX + column * cellSize + cellSize / 2;
                int direction = column % 2 == 0 ? -1 : 1;
                int particleX = originX + direction * spread * (1 + column % 3) / 3;
                int particleY = centerY + ((column % 4) - 1) * (int) (progress * cellSize);

                int particleAlpha = Math.max(0, (int) (255 * (1f - progress)));
                g2.setColor(new Color(255, 240, 130, particleAlpha));
                g2.fillOval(particleX - particleSize / 2,
                        particleY - particleSize / 2,
                        particleSize, particleSize);

                int sparkX = originX - direction * spread / 2;
                int sparkY = centerY - (1 + column % 3) * (int) (progress * cellSize);
                g2.setColor(new Color(92, 245, 220, particleAlpha));
                g2.fillRect(sparkX - 2, sparkY - 2,
                        particleSize + 1, particleSize + 1);
            }

            int ringSize = (int) (Board.COLUMNS * cellSize * progress);
            int ringAlpha = Math.max(0, (int) (180 * (1f - progress)));
            g2.setColor(new Color(255, 255, 255, ringAlpha));
            g2.drawOval(boardX + Board.COLUMNS * cellSize / 2 - ringSize / 2,
                    centerY - ringSize / 4,
                    ringSize,
                    ringSize / 2);
        }
    }

    private void drawActiveTetromino(Graphics2D g2, int boardX, int boardY, int cellSize) {
        for (Tetromino.GridCell cell : activeTetromino.getOccupiedCells()) {
            int cellX = boardX + cell.column() * cellSize;
            int cellY = boardY + cell.row() * cellSize;
            drawBlock(g2, cellX, cellY, cellSize,
                    getPieceColor(activeTetromino.getType()));
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
        String text = "KẾT THÚC";
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

        drawStat(g2, "ĐIỂM", Long.toString(scoreManager.getScore()), x + 20, y + 83);
        drawStat(g2, "CAO NHẤT", Long.toString(highScoreSupplier.getAsLong()), x + 20, y + 145);
        drawStat(g2, "CẤP ĐỘ", Integer.toString(scoreManager.getLevel()), x + 20, y + 207);
        drawStat(g2, "DÒNG", Integer.toString(scoreManager.getLines()), x + 20, y + 269);

        g2.setColor(SECONDARY_TEXT_COLOR);
        g2.setFont(LABEL_FONT);
        g2.drawString("TIẾP THEO", x + 20, y + 322);
        g2.drawString("GIỮ", x + 120, y + 322);
        drawPiecePreview(g2, nextType, x + 20, y + 334);
        drawPiecePreview(g2, heldType, x + 120, y + 334);
    }

    private void drawPiecePreview(Graphics2D g2, TetrominoType type, int x, int y) {
        if (type == null) {
            return;
        }
        int previewCellSize = 17;
        for (TetrominoType.Cell cell : type.getCells()) {
            int cellX = x + cell.column() * previewCellSize;
            int cellY = y + cell.row() * previewCellSize;
            drawBlock(g2, cellX, cellY, previewCellSize, getPieceColor(type));
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
