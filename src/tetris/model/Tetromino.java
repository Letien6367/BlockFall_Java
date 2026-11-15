package tetris.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Tetromino {
    private static final int ROTATION_GRID_SIZE = 4;
    private static final int[] WALL_KICK_OFFSETS = {0, -1, 1, -2, 2};

    private final TetrominoType type;
    private int column;
    private int row;
    private int rotationState;

    public Tetromino(TetrominoType type, int column, int row) {
        this.type = Objects.requireNonNull(type, "type");
        this.column = column;
        this.row = row;
    }

    public TetrominoType getType() {
        return type;
    }

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    public int getRotationState() {
        return rotationState;
    }

    public void moveBy(int columns, int rows) {
        column += columns;
        row += rows;
    }

    public void setRotationState(int rotationState) {
        if (rotationState < 0 || rotationState >= ROTATION_GRID_SIZE) {
            throw new IllegalArgumentException("Rotation state must be between 0 and 3");
        }
        this.rotationState = rotationState;
    }

    public boolean tryRotate(Board board, int direction) {
        Objects.requireNonNull(board, "board");
        if (direction != -1 && direction != 1) {
            throw new IllegalArgumentException("Rotation direction must be -1 or 1");
        }
        if (type == TetrominoType.O) {
            return true;
        }

        int candidateRotation = Math.floorMod(rotationState + direction, ROTATION_GRID_SIZE);
        for (int columnOffset : WALL_KICK_OFFSETS) {
            Tetromino candidate = new Tetromino(type, column + columnOffset, row);
            candidate.rotationState = candidateRotation;
            if (board.isValidPosition(candidate)) {
                column = candidate.column;
                rotationState = candidate.rotationState;
                return true;
            }
        }
        return false;
    }

    public List<GridCell> getOccupiedCells() {
        List<GridCell> occupiedCells = new ArrayList<>(type.getCells().size());
        for (TetrominoType.Cell shapeCell : type.getCells()) {
            int cellColumn = shapeCell.column();
            int cellRow = shapeCell.row();
            for (int turn = 0; turn < rotationState; turn++) {
                int rotatedColumn = ROTATION_GRID_SIZE - 1 - cellRow;
                cellRow = cellColumn;
                cellColumn = rotatedColumn;
            }
            occupiedCells.add(new GridCell(row + cellRow, column + cellColumn));
        }
        return List.copyOf(occupiedCells);
    }

    public record GridCell(int row, int column) {
    }
}
