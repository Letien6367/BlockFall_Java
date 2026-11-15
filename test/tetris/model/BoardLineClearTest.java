package tetris.model;

public final class BoardLineClearTest {
    private BoardLineClearTest() {
    }

    public static void main(String[] args) {
        verifyPieceLocksIntoBoard();
        verifySingleRowClearsAndRowsShift();
        verifyFourRowsClearTogether();
        System.out.println("Board line-clear checks passed.");
    }

    private static void verifyPieceLocksIntoBoard() {
        Board board = new Board();
        Tetromino piece = new Tetromino(TetrominoType.T, 3, 5);
        board.lock(piece);
        for (Tetromino.GridCell cell : piece.getOccupiedCells()) {
            if (!board.isOccupied(cell.row(), cell.column())) {
                throw new AssertionError("Locked piece cell was not stored on the board");
            }
        }
    }

    private static void verifySingleRowClearsAndRowsShift() {
        Board board = new Board();
        board.setOccupied(18, 0, true);
        for (int column = 0; column < Board.COLUMNS; column++) {
            if (column < 3 || column > 6) {
                board.setOccupied(19, column, true);
            }
        }
        board.lock(new Tetromino(TetrominoType.I, 3, 18));

        if (board.clearFullRows() != 1) {
            throw new AssertionError("Expected one full row to clear");
        }
        if (!board.isOccupied(19, 0) || board.isOccupied(18, 0)) {
            throw new AssertionError("Rows above a cleared row did not shift down");
        }
        for (int column = 0; column < Board.COLUMNS; column++) {
            if (board.isOccupied(18, column)) {
                throw new AssertionError("Top row should be empty after shifting");
            }
        }
    }

    private static void verifyFourRowsClearTogether() {
        Board board = new Board();
        for (int row = 16; row < Board.ROWS; row++) {
            for (int column = 0; column < Board.COLUMNS; column++) {
                if (column != 6) {
                    board.setOccupied(row, column, true);
                }
            }
        }
        Tetromino verticalI = new Tetromino(TetrominoType.I, 4, 16);
        verticalI.setRotationState(1);
        board.lock(verticalI);

        if (board.clearFullRows() != 4) {
            throw new AssertionError("Expected four full rows to clear together");
        }
        for (int row = 0; row < Board.ROWS; row++) {
            for (int column = 0; column < Board.COLUMNS; column++) {
                if (board.isOccupied(row, column)) {
                    throw new AssertionError("Board should be empty after clearing four rows");
                }
            }
        }
    }
}
