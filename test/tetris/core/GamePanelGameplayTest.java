package tetris.core;

import java.util.concurrent.atomic.AtomicReference;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import tetris.model.Board;
import tetris.model.Tetromino;

public final class GamePanelGameplayTest {
    private GamePanelGameplayTest() {
    }

    public static void main(String[] args) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                verifyMovementAndDrops();
                verifyBlockedSpawnEndsGame();
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        if (failure.get() != null) {
            throw new AssertionError("Gameplay checks failed", failure.get());
        }
        System.out.println("Game panel gameplay checks passed.");
    }

    private static void verifyMovementAndDrops() {
        Board board = new Board();
        GamePanel panel = new GamePanel(board);
        Action left = panel.getActionMap().get("move-left");
        Action right = panel.getActionMap().get("move-right");
        Action softDrop = panel.getActionMap().get("soft-drop");
        Action hardDrop = panel.getActionMap().get("hard-drop");

        for (int i = 0; i < Board.COLUMNS * 2; i++) {
            left.actionPerformed(null);
        }
        assertPieceWithinBoard(panel);
        int leftmostColumn = getExtremeColumn(panel.getActiveTetromino(), true);
        if (leftmostColumn != 0) {
            throw new AssertionError("Repeated left movement should reach the left edge");
        }
        left.actionPerformed(null);
        if (getExtremeColumn(panel.getActiveTetromino(), true) != 0) {
            throw new AssertionError("Piece moved outside the left edge");
        }

        for (int i = 0; i < Board.COLUMNS * 2; i++) {
            right.actionPerformed(null);
        }
        assertPieceWithinBoard(panel);
        if (getExtremeColumn(panel.getActiveTetromino(), false) != Board.COLUMNS - 1) {
            throw new AssertionError("Repeated right movement should reach the right edge");
        }
        right.actionPerformed(null);
        if (getExtremeColumn(panel.getActiveTetromino(), false) != Board.COLUMNS - 1) {
            throw new AssertionError("Piece moved outside the right edge");
        }

        int previousRow = panel.getActiveTetromino().getRow();
        softDrop.actionPerformed(null);
        if (panel.getActiveTetromino().getRow() != previousRow + 1) {
            throw new AssertionError("Soft drop should move the active piece down one row");
        }

        hardDrop.actionPerformed(null);
        if (countOccupiedCells(board) != 4) {
            throw new AssertionError("Hard drop should lock the piece at the lowest valid row");
        }
        panel.pauseGame();
    }

    private static void verifyBlockedSpawnEndsGame() {
        Board blockedBoard = new Board();
        for (int row = 0; row < Board.ROWS; row++) {
            for (int column = 0; column < Board.COLUMNS; column++) {
                blockedBoard.setOccupied(row, column, true);
            }
        }

        AtomicReference<GameState> state = new AtomicReference<>();
        GamePanel panel = new GamePanel(blockedBoard, state::set);
        if (state.get() != GameState.GAME_OVER || panel.isGravityTimerRunning()
                || panel.getActiveTetromino() != null) {
            throw new AssertionError("A blocked spawn should enter game over without starting gravity");
        }
    }

    private static int getExtremeColumn(Tetromino piece, boolean leftmost) {
        int extreme = leftmost ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        for (Tetromino.GridCell cell : piece.getOccupiedCells()) {
            extreme = leftmost ? Math.min(extreme, cell.column())
                    : Math.max(extreme, cell.column());
        }
        return extreme;
    }

    private static void assertPieceWithinBoard(GamePanel panel) {
        for (Tetromino.GridCell cell : panel.getActiveTetromino().getOccupiedCells()) {
            if (cell.column() < 0 || cell.column() >= Board.COLUMNS) {
                throw new AssertionError("Active piece moved outside the board");
            }
        }
    }

    private static int countOccupiedCells(Board board) {
        int count = 0;
        for (int row = 0; row < Board.ROWS; row++) {
            for (int column = 0; column < Board.COLUMNS; column++) {
                if (board.isOccupied(row, column)) {
                    count++;
                }
            }
        }
        return count;
    }
}
