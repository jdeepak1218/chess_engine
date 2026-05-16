package pieces;
import game.Board;
public class Knight extends Piece{
    public Knight(String color,int row,int col)
    {
        super(color,row,col);
    }
    public String getSymbol()
    {
        return (color.equals("white")) ? "\u2658" : "\u265E";
    }
    public boolean isValidMove(int row,int col,Board board)
    {
        int presentRow = this.row;
        int presentCol = this.col;
        if(!board.isInsideBoard(row,col))return false;
        if(presentCol == col && presentRow == row)return false;
        int dRow = Math.abs(row - presentRow);
        int dCol = Math.abs(col - presentCol);
        if(!((dRow == 2 && dCol == 1) || (dRow == 1 && dCol == 2)))
        {
            return false;
        }
        Piece destPiece = board.getPiece(row,col);
        if(destPiece != null && this.color.equals(destPiece.color)) return false;
        return true;
    }
    public void move(int row,int col,Board board)
    {
        //to do
        //empty
    }
}