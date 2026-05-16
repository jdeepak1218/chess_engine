package pieces;
import game.Board;
public class King extends Piece{
    public King(String color,int row,int col)
    {
        super(color,row,col);
    }
    public String getSymbol()
    {
        return (color.equals("white")) ? "\u2654" : "\u265A";
    }
    public boolean isValidMove(int row,int col,Board board)
    {
        if(!board.isInsideBoard(row,col)) return false;
        int presentRow = this.row;
        int presentCol = this.col;
        if(presentRow == row && presentCol == col)return false;
        int dRow = Math.abs(presentRow - row);
        int dCol = Math.abs(presentCol - col);
        if(dRow == 0 && dCol == 2)
        {
            return isValidCastling(row,col,board);
        }
        if(!(dRow <= 1 && dCol <=1))return false;
        Piece destPiece = board.getPiece(row,col);
        if(destPiece != null && this.color.equals(destPiece.color))return false;
        return true;
    }
    public boolean isValidCastling(int targetRow,int targetCol,Board board)
    {
        if(isHasMoved())return false;
        int presentRow = this.row;
        if(board.isKingInCheck(this.color))return false;
        boolean isKingside = (targetCol == 6);
        boolean isQueenside = (targetCol == 2);
        if(!isKingside && !isQueenside)return false;
        int rookCol = isKingside ? 7 : 0;
        int pathStartCol = isKingside ? 5 : 1;
        int pathEndCol = isKingside ? 6 : 3;
        int checkStartCol = isKingside ? 5 : 2;
        int checkEndCol = isKingside ? 6 : 3;
        Piece rook = board.getPiece(presentRow,rookCol);
        if(rook == null || !(rook instanceof Rook) || !rook.color.equals(this.color) || rook.isHasMoved())return false;
        for(int c = pathStartCol ; c <= pathEndCol ; c++)
        {
            if(board.getPiece(presentRow,c) != null)return false;
        }
        String opp = this.color.equals("white") ? "black" : "white";
        for(int c = checkStartCol ; c <= checkEndCol ; c++)
        {
            if(board.isSquareAttacked(presentRow,c,opp))return false;
        }
        return true;
    }
    public void move(int row,int col,Board board)
    {
        //to do
        //empty
    }
}