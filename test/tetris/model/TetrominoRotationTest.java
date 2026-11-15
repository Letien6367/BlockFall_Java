package tetris.model;

import java.util.HashSet;
import java.util.Set;

public final class TetrominoRotationTest {
    private TetrominoRotationTest() {
    }

    public static void main(String[] args) {
        verifyRotationCoordinates();
        verifySquareDoesNotRotate();
        verifyWallKick();
        verifyBlockedRotationIsRejected();
        System.out.println("Tetromino rotation checks passed.");
    }

    private static void verifyRotationCoordinates() {
        Board board = new Board();
        Tetromino piece = new Tetromino(TetrominoType.T, 3, 5);
        Set<Tetromino.GridCell> initialCells = new HashSet<>(piece.getOccupiedCells());

        if (!piece.tryRotate(board, 1) || piece.getRotationState() != 1) {
            throw new AssertionError("T piece did not rotate clockwise");
        }
        if (initialCells.equals(new HashSet<>(piece.getOccupiedCells()))) {
            throw new AssertionError("Rotation did not change the occupied cells");
        }
        if (!piece.tryRotate(board, -1) || piece.getRotationState() != 0
                || !initialCells.equals(new HashSet<>(piece.getOccupiedCells()))) {
            throw new AssertionError("Counterclockwise rotation did not restore the piece");
        }
    }

    private static void verifySquareDoesNotRotate() {
        Board board = new Board();
        Tetromino piece = new Tetromino(TetrominoType.O, 3, 4);
        Set<Tetromino.GridCell> cells = new HashSet<>(piece.getOccupiedCells());

        if (!piece.tryRotate(board, 1) || piece.getRotationState() != 0
                || !cells.equals(new HashSet<>(piece.getOccupiedCells()))) {
            throw new AssertionError("O piece changed when rotated");
        }
    }

    private static void verifyWallKick() {
        Board board = new Board();
        Tetromino piece = new Tetromino(TetrominoType.T, -2, 5);
        piece.setRotationState(1);
        if (!board.isValidPosition(piece)) {
            throw new AssertionError("Wall-kick test piece must start in a valid position");
        }

        if (!piece.tryRotate(board, 1) || piece.getRotationState() != 2
                || piece.getColumn() != -1 || !board.isValidPosition(piece)) {
            throw new AssertionError("Rotation did not use a valid wall kick");
        }
    }

    private static void verifyBlockedRotationIsRejected() {
        Board board = new Board();
        Tetromino piece = new Tetromino(TetrominoType.T, 3, 5);
        for (int row = 0; row < Board.ROWS; row++) {
            for (int column = 0; column < Board.COLUMNS; column++) {
                board.setOccupied(row, column, true);
            }
        }
        for (Tetromino.GridCell cell : piece.getOccupiedCells()) {
            board.setOccupied(cell.row(), cell.column(), false);
        }

        if (piece.tryRotate(board, 1) || piece.getRotationState() != 0
                || piece.getColumn() != 3) {
            throw new AssertionError("Rotation should not overlap occupied cells");
        }
    }
}
