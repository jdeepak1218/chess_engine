package pieces;
import game.Board;
public class Pawn extends Piece{
    public Pawn(String color,int row,int col)
    {
        super(color,row,col);
    }
    public String getSymbol()
    {
        return (color.equals("white")) ? "\u2659" : "\u265F";
    }
    public boolean isValidMove(int row,int col,Board board)
    {
        if(!board.isInsideBoard(row,col))return false;
        int presentRow = this.row;
        int presentCol = this.col;
        String color = this.color;
        int direction = color.equals("white") ? 1 : -1;
        int oneStep = direction + presentRow;
        int twoStep = (2 * direction) + presentRow;
        Piece destPiece = board.getPiece(row,col);
        if(oneStep == row && presentCol == col)
        {
            return destPiece == null;
        }
        boolean isStartingRow =
                (color.equals("white") && presentRow == 1) ||
                        (color.equals("black") && presentRow == 6);

        if(twoStep == row && presentCol == col && !isHasMoved() && isStartingRow)
        {
            Piece middleOne = board.getPiece(oneStep, presentCol);
            return destPiece == null && middleOne == null;
        }

        if(oneStep == row && Math.abs(presentCol - col) == 1)
        {
            if(destPiece != null && !destPiece.color.equals(color))return true;
            if(destPiece == null && board.enPassantRow == row && board.enPassantCol == col)
            {
                Piece capturedPawn = board.getPiece(presentRow,col);
                return capturedPawn != null && capturedPawn instanceof Pawn && !capturedPawn.color.equals(color);
            }
        }
        return false;
    }
    public void move(int row,int col,Board board)
    {
        //to do
        //empty
    }
}