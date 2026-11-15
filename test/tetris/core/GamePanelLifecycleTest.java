package tetris.core;

import java.util.concurrent.atomic.AtomicReference;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import tetris.model.Board;

public final class GamePanelLifecycleTest {
    private GamePanelLifecycleTest() {
    }

    public static void main(String[] args) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                Board board = new Board();
                GamePanel panel = new GamePanel(board);
                Action hardDrop = panel.getActionMap().get("hard-drop");
                hardDrop.actionPerformed(null);
                if (panel.getScore() <= 0) {
                    throw new AssertionError("Hard drop should make the score nonzero");
                }

                panel.startGame();
                if (!panel.isGravityTimerRunning()) {
                    throw new AssertionError("Starting a game should start its gravity timer");
                }
                panel.pauseGame();
                if (panel.isGravityTimerRunning()) {
                    throw new AssertionError("Pausing should stop the gravity timer");
                }
                panel.resumeGame();
                if (!panel.isGravityTimerRunning()) {
                    throw new AssertionError("Resuming should restart the gravity timer");
                }

                panel.resetGame();
                if (panel.isGravityTimerRunning() || panel.getScore() != 0
                        || countOccupiedCells(board) != 0) {
                    throw new AssertionError("Reset should stop timer and clear board and score");
                }
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        if (failure.get() != null) {
            throw new AssertionError("Game panel lifecycle check failed", failure.get());
        }
        System.out.println("Game panel lifecycle checks passed.");
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
