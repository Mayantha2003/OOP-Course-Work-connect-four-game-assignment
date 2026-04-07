package lk.ijse.dep.service;

public class BoardImpl implements Board{

    private final Piece[][] pieces ;
    private final BoardUI boardUI;

    public BoardImpl(BoardUI boardUI){
        this.boardUI = boardUI;
        this.pieces = new Piece[NUM_OF_COLS][NUM_OF_ROWS];
        for(int col = 0; col < NUM_OF_COLS ; col++){
            for(int row = 0; row < NUM_OF_ROWS ; row++){
                pieces[col][row] = Piece.EMPTY;
            }
        }
    }
    public void updateMove(int col, int row, Piece move) {
        pieces[col][row] = move;
    }

    public Piece[][] getPieces() {
        Piece[][] copy = new Piece[NUM_OF_COLS][NUM_OF_ROWS];
        for (int i = 0; i < NUM_OF_COLS; i++) {
            System.arraycopy(pieces[i], 0, copy[i], 0, NUM_OF_ROWS);
        }
        return copy;
    }

    @Override
    public BoardUI getBoardUI() {
        return boardUI;
    }

    @Override
    public int findNextAvailableSpot(int col){
        for(int row = 0; row < NUM_OF_ROWS; row++){
            if (pieces[col][row] == Piece.EMPTY){
                return row;
            }
        }
        return  -1;
    }
    @Override
    public boolean isLegalMove(int col){
        return findNextAvailableSpot(col) != -1;
    }

    @Override
    public boolean existLegalMoves(){
        for(int col = 0;col < NUM_OF_COLS; col++){
            if(isLegalMove(col)){
                return false;
            }
        }
        return true;
    }
    @Override
    public void updateMove(int col,Piece move){
        int row = findNextAvailableSpot(col);
        if(row != -1){
            pieces[col][row] = move;
        }
    }
    @Override
    public Winner findWinner() {
        // Horizontal checks
        for (int row = 0; row < NUM_OF_ROWS; row++) {
            for (int col = 0; col < NUM_OF_COLS - 3; col++) {
                Piece p = pieces[col][row];
                if (p != Piece.EMPTY && p == pieces[col + 1][row] && p == pieces[col + 2][row] && p == pieces[col + 3][row]) {
                    return new Winner(p, col, row, col + 3, row);
                }
            }
        }

        // Vertical checks
        for (int col = 0; col < NUM_OF_COLS; col++) {
            for (int row = 0; row < NUM_OF_ROWS - 3; row++) {
                Piece p = pieces[col][row];
                if (p != Piece.EMPTY && p == pieces[col][row + 1] && p == pieces[col][row + 2] && p == pieces[col][row + 3]) {
                    return new Winner(p, col, row, col, row + 3);
                }
            }
        }

        return new Winner(Piece.EMPTY);
    }
}

