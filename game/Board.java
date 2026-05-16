package game;
import pieces.*;
import java.util.*;
public class Board{
    private Piece[][] board;
    Stack<Move> st;
    public int enPassantRow = -1;
    public int enPassantCol = -1;
    private long hash;
    public Board() throws Exception{
        board = new Piece[8][8];
        st = new Stack<>();
        initializeBoard();
        hash = Zobrist.computeHash(this,"white");
    }
    public boolean isInsideBoard(int row,int col)
    {
        return row >= 0 && col >= 0 && row < 8 && col < 8;
    }
    public Piece getPiece(int row,int col)
    {
        return board[row][col];
    }
    public void setPiece(int row,int col,Piece piece)
    {
        board[row][col] = piece;
    }
    public long getHash(){ return hash; }
    public boolean isPathClear(int presentRow , int presentCol , int destRow , int destCol)
    {
        if((presentRow == destRow && presentCol != destCol)||(presentRow != destRow && presentCol == destCol))
        {
            return rookIsPathClear(presentRow,presentCol,destRow,destCol);
        }
        else if(Math.abs(presentRow - destRow) == Math.abs(presentCol - destCol))
        {
            return bishopIsPathClear(presentRow,presentCol,destRow,destCol);
        }
        return false;
    }
    public boolean bishopIsPathClear(int presentRow,int presentCol,int destRow,int destCol)
    {
        int rowStep = 0;
        int colStep = 0;
        if(destRow > presentRow)rowStep = 1;
        else if(destRow < presentRow)rowStep = -1;
        if(destCol > presentCol)colStep = 1;
        else if(destCol < presentCol)colStep = -1;
        int currentRow = presentRow + rowStep;
        int currentCol = presentCol + colStep;
        while(currentRow != destRow && currentCol != destCol)
        {
            if(board[currentRow][currentCol] != null)return false;
            currentRow = currentRow + rowStep;
            currentCol = currentCol + colStep;
        }
        return true;
    }
    public boolean rookIsPathClear(int presentRow,int presentCol,int destRow,int destCol)
    {
        int rowStep = 0;
        int colStep = 0;
        if(destRow > presentRow && destCol == presentCol)
        {
            rowStep = 1;
        }
        else if(destRow < presentRow && destCol == presentCol)
        {
            rowStep = -1;
        }
        else if(destRow == presentRow && destCol > presentCol)
        {
            colStep = 1;
        }
        else if(destRow == presentRow && destCol < presentCol)
        {
            colStep = -1;
        }
        int currentRow = presentRow + rowStep;
        int currentCol = presentCol + colStep;
        while(currentRow != destRow || currentCol != destCol)
        {
            if(board[currentRow][currentCol] != null)return false;
            currentRow = currentRow + rowStep;
            currentCol = currentCol + colStep;
        }
        return true;
    }
    public void initializeBoard() throws Exception
    {
        Class<? extends Piece>allPieces[] = new Class[]{
                Rook.class,
                Knight.class,
                Bishop.class,
                Queen.class,
                King.class,
                Bishop.class,
                Knight.class,
                Rook.class
        };
        for(int column = 0 ; column < 8 ; column++)
        {
            Class<? extends Piece> pieceClass = allPieces[column];
            Piece black = pieceClass.getConstructor(String.class,int.class,int.class).newInstance("black",7,column);
            setPiece(7,column,black);
            setPiece(6, column, new Pawn("black",6,column));
            Piece white = pieceClass.getConstructor(String.class,int.class,int.class).newInstance("white",0,column);
            setPiece(0,column,white);
            setPiece(1, column, new Pawn("white", 1, column));
        }
    }
    public void printBoard()
    {
        for(int r = 7 ; r >= 0 ; r--)
        {
            System.out.print((r+1)+" ");
            for(int c = 0 ; c < 8 ; c++)
            {
                Piece p = board[r][c];
                if(p!=null)
                {
                    System.out.print(p.getSymbol()+" ");
                }
                else
                {
                    if((r+c) % 2 == 0)
                    {
                        System.out.print(". ");
                    }
                    else
                    {
                        System.out.print("# ");
                    }
                }
            }
            System.out.println(" "+(r+1));
        }
        System.out.println("  a b c d e f g h");
    }
    public void makeMove(Piece piece , int toRow , int toCol,String player)
    {
        int fromRow = piece.row;
        int fromCol = piece.col;
        Piece capturedPiece = getPiece(toRow,toCol);
        boolean hasMovedBefore = piece.isHasMoved();
        int prevEnPassantRow = this.enPassantRow;
        int prevEnPassantCol = this.enPassantCol;
        boolean wasEnPassantCapture = false;
        int enPassantCapturedRow = -1;
        int enPassantCapturedCol = -1;
        boolean wasCastling = false;
        Piece castlingRook = null;
        int castlingRookFromRow = -1;
        int castlingRookFromCol = -1;
        int castlingRookToRow = -1;
        int castlingRookToCol = -1;
        boolean castlingRookHasMovedBefore = false;
        int castlingBefore = Zobrist.computeCastlingIndex(this);
        if(piece instanceof Pawn && Math.abs(toRow - fromRow) == 2)
        {
            this.enPassantRow = (fromRow + toRow) / 2;
            this.enPassantCol = fromCol;
        }
        else if(piece instanceof Pawn && toRow == this.enPassantRow && toCol == this.enPassantCol)
        {
            wasEnPassantCapture = true;
            enPassantCapturedRow = (fromRow + toRow) / 2;
            enPassantCapturedCol = toCol;
            capturedPiece = getPiece(enPassantCapturedRow,enPassantCapturedCol);
            setPiece(fromRow,toCol,null);
            this.enPassantRow = -1;
            this.enPassantCol = -1;
        }
        else if(piece instanceof King && Math.abs(toCol - fromCol) == 2 && fromRow == toRow)
        {
            wasCastling = true;
            this.enPassantRow = -1;
            this.enPassantCol = -1;
            boolean isKingside = (toCol == 6);
            castlingRook = getPiece(fromRow,isKingside ? 7 : 0);
            castlingRookFromRow = fromRow;
            castlingRookFromCol = isKingside ? 7 : 0;
            castlingRookToRow = fromRow;
            castlingRookToCol = isKingside ? 5 : 3;
            castlingRookHasMovedBefore = castlingRook.isHasMoved();
            setPiece(castlingRookFromRow,castlingRookFromCol,null);
            setPiece(castlingRookToRow,castlingRookToCol,castlingRook);
            castlingRook.row = castlingRookToRow;
            castlingRook.col = castlingRookToCol;
            castlingRook.hasMoved = true;
        }
        else
        {
            this.enPassantRow = -1;
            this.enPassantCol = -1;
        }
        setPiece(fromRow,fromCol,null);
        setPiece(toRow,toCol,piece);
        piece.row = toRow;
        piece.col = toCol;
        piece.hasMoved = true;
        int castlingAfter = Zobrist.computeCastlingIndex(this);
        boolean sideChanged = true;
        long hashBefore = this.hash;
        this.hash = Zobrist.updateHash(this,this.hash,piece,fromRow,fromCol,toRow,toCol,
                capturedPiece,wasEnPassantCapture,prevEnPassantCol,this.enPassantCol,
                castlingBefore,castlingAfter,sideChanged,
                wasCastling,castlingRook,castlingRookFromCol,castlingRookToCol);
        Move move = new Move(fromRow,fromCol,toRow,toCol,piece,capturedPiece,hasMovedBefore,player,
                prevEnPassantRow,prevEnPassantCol,
                wasEnPassantCapture,enPassantCapturedRow,enPassantCapturedCol,
                wasCastling,castlingRook,
                castlingRookFromRow,castlingRookFromCol,
                castlingRookToRow,castlingRookToCol,
                castlingRookHasMovedBefore,hashBefore);
        st.push(move);
    }
    public String undoMove()
    {
        if(st.isEmpty())
        {
            System.out.println("There are no moves for undo");
            return null;
        }
        Move move = st.pop();
        int fromRow = move.fromRow;
        int fromCol = move.fromCol;
        int toRow = move.toRow;
        int toCol = move.toCol;
        Piece capturedPiece = move.capturedPiece;
        boolean hasMovedBefore = move.hasMovedBefore;
        Piece movedPiece = move.movedPiece;
        this.enPassantRow = move.prevEnPassantRow;
        this.enPassantCol = move.prevEnPassantCol;
        this.hash = move.prevHash;
        if(move.wasEnPassantCapture)
        {
            setPiece(move.enPassantCapturedRow,move.enPassantCapturedCol,capturedPiece);
            if(capturedPiece != null)
            {
                capturedPiece.row = move.enPassantCapturedRow;
                capturedPiece.col = move.enPassantCapturedCol;
            }
        }
        else
        {
            setPiece(toRow,toCol,capturedPiece);
            if(capturedPiece != null)
            {
                capturedPiece.row = toRow;
                capturedPiece.col = toCol;
            }
        }
        setPiece(fromRow,fromCol,movedPiece);
        movedPiece.row = fromRow;
        movedPiece.col = fromCol;
        movedPiece.hasMoved = hasMovedBefore;
        if(move.wasCastling)
        {
            setPiece(move.castlingRookFromRow,move.castlingRookFromCol,move.castlingRook);
            if(move.castlingRook != null)
            {
                move.castlingRook.row = move.castlingRookFromRow;
                move.castlingRook.col = move.castlingRookFromCol;
                move.castlingRook.hasMoved = move.castlingRookHasMovedBefore;
            }
            setPiece(move.castlingRookToRow,move.castlingRookToCol,null);
        }
        return move.player;
    }
    public boolean undoLastFullMove()
    {
        if(st.size() < 2)
        {
            System.out.println("Not enough moves to undo");
            return false;
        }
        undoMove();
        undoMove();
        return true;
    }
    public boolean isSquareAttacked(int row,int col,String byColor)
    {
        for(int r = 0 ; r < 8 ; r++)
        {
            for(int c = 0 ; c < 8 ; c++)
            {
                Piece currPiece = getPiece(r,c);
                if(currPiece == null || !currPiece.color.equals(byColor))continue;
                if(currPiece instanceof Pawn)
                {
                    if(pawnAttackSquare((Pawn) currPiece,row,col))
                    {
                        return true;
                    }
                }
                else
                {
                    if(currPiece.isValidMove(row,col,this))return true;
                }
            }
        }
        return false;
    }
    public boolean isKingInCheck(String color)
    {
        String opp = color.equals("white") ? "black" : "white";
        for(int r = 0 ; r < 8 ; r++)
        {
            for(int c = 0 ; c < 8 ; c++)
            {
                Piece currPiece = getPiece(r,c);
                if(currPiece != null && currPiece.color.equals(color) && currPiece instanceof King)
                {
                    return isSquareAttacked(currPiece.row,currPiece.col,opp);
                }
            }
        }
        throw new IllegalStateException("King not found " + color);
    }
    public boolean isCheckmate(String color)
    {
        return isKingInCheck(color) && !hasLegalMoves(color);
    }
    public boolean pawnAttackSquare(Pawn pawn , int targetRow , int targetCol)
    {
        int direction = pawn.color.equals("white") ? 1 : -1;
        int r = pawn.row + direction;
        if(r == targetRow && Math.abs(targetCol - pawn.col) == 1)return true;
        return false;
    }
    public boolean hasLegalMoves(String color)
    {
        for(int r = 0 ; r < 8 ; r++)
        {
            for(int c = 0 ; c < 8 ; c++)
            {
                Piece currPiece = getPiece(r,c);
                if(currPiece != null && currPiece.color.equals(color))
                {
                    for(int destR = 0 ; destR < 8 ; destR++)
                    {
                        for(int destC = 0 ; destC < 8 ; destC++)
                        {
                            if(currPiece.isValidMove(destR,destC,this))
                            {
                                this.makeMove(currPiece,destR,destC,color);
                                boolean isKingInCheck = isKingInCheck(color);
                                undoMove();
                                if(!isKingInCheck)return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
    public boolean isStalemate(String color)
    {
        if(isKingInCheck(color))return false;
        return !hasLegalMoves(color);
    }
}