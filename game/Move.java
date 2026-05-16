package game;
import pieces.*;
public class Move{
    int fromRow;
    int fromCol;
    int toRow;
    int toCol;
    Piece capturedPiece;
    Piece movedPiece;
    boolean hasMovedBefore;
    String player;
    int prevEnPassantRow;
    int prevEnPassantCol;
    boolean wasEnPassantCapture;
    int enPassantCapturedRow;
    int enPassantCapturedCol;
    boolean wasCastling;
    Piece castlingRook;
    int castlingRookFromRow;
    int castlingRookFromCol;
    int castlingRookToRow;
    int castlingRookToCol;
    boolean castlingRookHasMovedBefore;
    long prevHash;
    public Move(int fromRow,int fromCol,int toRow,int toCol,Piece movedPiece,Piece capturedPiece,
                boolean hasMovedBefore,String player,
                int prevEnPassantRow,int prevEnPassantCol,
                boolean wasEnPassantCapture,int enPassantCapturedRow,int enPassantCapturedCol,
                boolean wasCastling,Piece castlingRook,
                int castlingRookFromRow,int castlingRookFromCol,
                int castlingRookToRow,int castlingRookToCol,
                boolean castlingRookHasMovedBefore,long prevHash)
    {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.movedPiece = movedPiece;
        this.capturedPiece = capturedPiece;
        this.hasMovedBefore = hasMovedBefore;
        this.player = player;
        this.prevEnPassantRow = prevEnPassantRow;
        this.prevEnPassantCol = prevEnPassantCol;
        this.wasEnPassantCapture = wasEnPassantCapture;
        this.enPassantCapturedRow = enPassantCapturedRow;
        this.enPassantCapturedCol = enPassantCapturedCol;
        this.wasCastling = wasCastling;
        this.castlingRook = castlingRook;
        this.castlingRookFromRow = castlingRookFromRow;
        this.castlingRookFromCol = castlingRookFromCol;
        this.castlingRookToRow = castlingRookToRow;
        this.castlingRookToCol = castlingRookToCol;
        this.castlingRookHasMovedBefore = castlingRookHasMovedBefore;
        this.prevHash = prevHash;
    }
}