package tetris.model;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;

public enum TetrominoType {
    I(List.of(new Cell(0, 1), new Cell(1, 1), new Cell(2, 1), new Cell(3, 1)),
            new Color(38, 198, 218)),
    O(List.of(new Cell(1, 0), new Cell(2, 0), new Cell(1, 1), new Cell(2, 1)),
            new Color(255, 202, 40)),
    T(List.of(new Cell(1, 0), new Cell(0, 1), new Cell(1, 1), new Cell(2, 1)),
            new Color(171, 71, 188)),
    S(List.of(new Cell(1, 0), new Cell(2, 0), new Cell(0, 1), new Cell(1, 1)),
            new Color(102, 187, 106)),
    Z(List.of(new Cell(0, 0), new Cell(1, 0), new Cell(1, 1), new Cell(2, 1)),
            new Color(239, 83, 80)),
    J(List.of(new Cell(0, 0), new Cell(0, 1), new Cell(1, 1), new Cell(2, 1)),
            new Color(66, 165, 245)),
    L(List.of(new Cell(2, 0), new Cell(0, 1), new Cell(1, 1), new Cell(2, 1)),
            new Color(255, 152, 0));

    private final List<Cell> cells;
    private final Color color;

    TetrominoType(List<Cell> cells, Color color) {
        this.cells = List.copyOf(cells);
        if (this.cells.size() != 4 || new HashSet<>(this.cells).size() != 4) {
            throw new IllegalArgumentException("A tetromino must contain four distinct cells");
        }
        this.color = color;
    }

    public List<Cell> getCells() {
        return cells;
    }

    public Color getColor() {
        return color;
    }

    public record Cell(int column, int row) {
        public Cell {
            if (column < 0 || column > 3 || row < 0 || row > 3) {
                throw new IllegalArgumentException("Shape cell must be within its 4x4 grid");
            }
        }
    }
}
