package tetris.model;

import java.util.Arrays;
import java.util.Objects;

public final class Board {
    public static final int ROWS = 20;
    public static final int COLUMNS = 10;

    private final boolean[][] occupiedCells = new boolean[ROWS][COLUMNS];

    public boolean isOccupied(int row, int column) {
        checkCoordinates(row, column);
        return occupiedCells[row][column];
    }

    public void setOccupied(int row, int column, boolean occupied) {
        checkCoordinates(row, column);
        occupiedCells[row][column] = occupied;
    }

    public boolean isValidPosition(Tetromino tetromino) {
        Objects.requireNonNull(tetromino, "tetromino");
        for (Tetromino.GridCell cell : tetromino.getOccupiedCells()) {
            if (cell.row() < 0 || cell.row() >= ROWS
                    || cell.column() < 0 || cell.column() >= COLUMNS
                    || occupiedCells[cell.row()][cell.column()]) {
                return false;
            }
        }
        return true;
    }

    public void lock(Tetromino tetromino) {
        if (!isValidPosition(tetromino)) {
            throw new IllegalArgumentException("Cannot lock a piece at an invalid position");
        }
        for (Tetromino.GridCell cell : tetromino.getOccupiedCells()) {
            occupiedCells[cell.row()][cell.column()] = true;
        }
    }

    public int clearFullRows() {
        int clearedRows = 0;
        int destinationRow = ROWS - 1;
        for (int sourceRow = ROWS - 1; sourceRow >= 0; sourceRow--) {
            if (isRowFull(sourceRow)) {
                clearedRows++;
            } else {
                if (destinationRow != sourceRow) {
                    System.arraycopy(occupiedCells[sourceRow], 0,
                            occupiedCells[destinationRow], 0, COLUMNS);
                }
                destinationRow--;
            }
        }
        while (destinationRow >= 0) {
            occupiedCells[destinationRow--] = new boolean[COLUMNS];
        }
        return clearedRows;
    }

    public void clear() {
        for (boolean[] row : occupiedCells) {
            Arrays.fill(row, false);
        }
    }

    public int[] getFullRowIndexes() {
        int count = 0;
        for (int row = 0; row < ROWS; row++) {
            if (isRowFull(row)) {
                count++;
            }
        }
        int[] rows = new int[count];
        int index = 0;
        for (int row = 0; row < ROWS; row++) {
            if (isRowFull(row)) {
                rows[index++] = row;
            }
        }
        return rows;
    }

    private boolean isRowFull(int row) {
        for (int column = 0; column < COLUMNS; column++) {
            if (!occupiedCells[row][column]) {
                return false;
            }
        }
        return true;
    }

    private void checkCoordinates(int row, int column) {
        if (row < 0 || row >= ROWS || column < 0 || column >= COLUMNS) {
            throw new IndexOutOfBoundsException(
                    "Cell outside board: row=" + row + ", column=" + column);
        }
    }
}
