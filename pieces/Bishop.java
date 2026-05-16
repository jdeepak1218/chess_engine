package pieces;
import game.Board;
public class Bishop extends Piece{
    public Bishop(String color,int row,int col)
    {
        super(color,row,col);
    }
    public String getSymbol()
    {
        return (color.equals("white")) ? "\u2657" : "\u265D";
    }
    public boolean isValidMove(int row,int col,Board board)
    {
        if(!board.isInsideBoard(row,col))return false;
        int presentRow = this.row;
        int presentCol = this.col;
        if(presentCol == col && presentRow == row)return false;
        int dRow = Math.abs(presentRow - row);
        int dCol = Math.abs(presentCol - col);
        if(dRow != dCol)return false;
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