package tetris.core;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import tetris.model.Board;

public final class GamePanelControlsTest {
    private GamePanelControlsTest() {
    }

    public static void main(String[] args) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                Board board = new Board();
                GamePanel panel = new GamePanel(board);
                panel.setSize(900, 650);

                Action hardDrop = panel.getActionMap().get("hard-drop");
                if (hardDrop == null) {
                    throw new AssertionError("Space hard-drop action was not bound");
                }
                hardDrop.actionPerformed(null);

                int occupiedCells = 0;
                for (int row = 0; row < Board.ROWS; row++) {
                    for (int column = 0; column < Board.COLUMNS; column++) {
                        if (board.isOccupied(row, column)) {
                            occupiedCells++;
                        }
                    }
                }
                if (occupiedCells != 4) {
                    throw new AssertionError("Hard drop should lock exactly one tetromino");
                }

                BufferedImage image = new BufferedImage(900, 650, BufferedImage.TYPE_INT_ARGB);
                panel.paint(image.getGraphics());
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });
        if (failure.get() != null) {
            throw new AssertionError("Game panel control check failed", failure.get());
        }
        System.out.println("Game panel control checks passed.");
    }
}
