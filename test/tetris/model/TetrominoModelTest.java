package tetris.model;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TetrominoModelTest {
    private TetrominoModelTest() {
    }

    public static void main(String[] args) {
        TetrominoType[] types = TetrominoType.values();
        if (types.length != 7) {
            throw new AssertionError("Expected seven tetromino types");
        }

        Set<Color> colors = new HashSet<>();
        for (TetrominoType type : types) {
            List<TetrominoType.Cell> cells = type.getCells();
            if (cells.size() != 4 || new HashSet<>(cells).size() != 4) {
                throw new AssertionError(type + " must have four distinct cells");
            }
            if (!colors.add(type.getColor())) {
                throw new AssertionError(type + " must have a unique color");
            }

            Tetromino tetromino = new Tetromino(type, 4, 6);
            List<Tetromino.GridCell> initialCells = tetromino.getOccupiedCells();
            if (initialCells.size() != 4) {
                throw new AssertionError(type + " must occupy four grid cells");
            }
            for (int rotation = 1; rotation < 4; rotation++) {
                tetromino.setRotationState(rotation);
                List<Tetromino.GridCell> rotatedCells = tetromino.getOccupiedCells();
                if (rotatedCells.size() != 4 || new HashSet<>(rotatedCells).size() != 4) {
                    throw new AssertionError(type + " lost cells while rotating");
                }
            }
            tetromino.setRotationState(0);
            if (!initialCells.equals(tetromino.getOccupiedCells())) {
                throw new AssertionError(type + " did not return to its initial orientation");
            }
        }

        System.out.println("Tetromino model checks passed for all seven types.");
    }
}
