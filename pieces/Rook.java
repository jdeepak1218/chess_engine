package pieces;
import game.Board;
public class Rook extends Piece{
    public Rook(String color,int row,int col)
    {
        super(color,row,col);
    }
    public String getSymbol()
    {
        return (color.equals("white")) ? "\u2656" : "\u265C";
    }
    public boolean isValidMove(int row,int col,Board board)
    {
        //to do
        int presentRow = this.row;
        int presentCol = this.col;
        if(!board.isInsideBoard(row,col))return false;
        if(presentRow == row && presentCol == col)return false;
        if(!((presentRow == row && presentCol != col) || (presentCol == col && presentRow != row)))return false;
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