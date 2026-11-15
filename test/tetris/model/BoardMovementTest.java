package tetris.model;

public final class BoardMovementTest {
    private BoardMovementTest() {
    }

    public static void main(String[] args) {
        Board board = new Board();

        assertValid(board, new Tetromino(TetrominoType.O, -1, 0), true);
        assertValid(board, new Tetromino(TetrominoType.O, -2, 0), false);
        assertValid(board, new Tetromino(TetrominoType.O, 7, 0), true);
        assertValid(board, new Tetromino(TetrominoType.O, 8, 0), false);
        assertValid(board, new Tetromino(TetrominoType.O, 3, 18), true);
        assertValid(board, new Tetromino(TetrominoType.O, 3, 19), false);

        Tetromino collidingPiece = new Tetromino(TetrominoType.T, 3, 3);
        board.setOccupied(4, 4, true);
        assertValid(board, collidingPiece, false);

        Tetromino movablePiece = new Tetromino(TetrominoType.T, 3, 3);
        movablePiece.moveBy(1, 1);
        if (movablePiece.getColumn() != 4 || movablePiece.getRow() != 4) {
            throw new AssertionError("Piece movement did not update its grid position");
        }

        Board stack = new Board();
        stack.lock(new Tetromino(TetrominoType.O, 3, 18));
        Tetromino pieceAboveStack = new Tetromino(TetrominoType.O, 3, 16);
        pieceAboveStack.moveBy(0, 1);
        if (stack.isValidPosition(pieceAboveStack)) {
            throw new AssertionError("A piece should not move through a locked piece");
        }

        System.out.println("Board movement checks passed.");
    }

    private static void assertValid(Board board, Tetromino piece, boolean expected) {
        if (board.isValidPosition(piece) != expected) {
            throw new AssertionError("Unexpected validity for " + piece.getType()
                    + " at column=" + piece.getColumn() + ", row=" + piece.getRow());
        }
    }
}
