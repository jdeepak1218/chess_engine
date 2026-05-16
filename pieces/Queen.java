package pieces;
import game.Board;
public class Queen extends Piece{
    public Queen(String color,int row,int col)
    {
        super(color,row,col);
    }
    public String getSymbol()
    {
        return (color.equals("white")) ? "\u2655" : "\u265B";
    }
    public boolean isValidMove(int row,int col,Board board)
    {
        //to do
        if(!board.isInsideBoard(row,col))return false;
        int presentRow = this.row;
        int presentCol = this.col;
        if(presentCol == col && presentRow == row)return false;
        int dRow = Math.abs(presentRow - row);
        int dCol = Math.abs(presentCol - col);
        boolean isDiagonal = (dRow == dCol);
        boolean isStraight = (presentRow == row || presentCol == col);
        if(!isDiagonal && !isStraight)return false;
        Piece destPiece = board.getPiece(row,col);
        if(destPiece != null && this.color.equals(destPiece.color))return false;
        return board.isPathClear(presentRow,presentCol,row,col);
    }
    public void move(int row,int col,Board board)
    {
        //to do
        //empty
    }
}