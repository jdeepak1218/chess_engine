package pieces;
import game.Board;
public abstract class Piece{
    public String color;
    public int row;
    public int col;
    public boolean hasMoved;
    public Piece(String color,int row,int col)
    {
        this.color = color;
        this.row = row;
        this.col = col;
        hasMoved = false;
    }
    public abstract String getSymbol();
    public abstract boolean isValidMove(int row,int col,Board board);
    public abstract void move(int row,int col,Board board);
    public boolean isHasMoved() {
        return hasMoved;
    }
}
