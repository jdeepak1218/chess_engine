package game;
import pieces.*;
import java.util.Random;
public class Zobrist{
    public static final long[][][] pieceKeys = new long[2][6][64];
    public static final long[] enPassantKeys = new long[8];
    public static final long[] castlingKeys = new long[16];
    public static final long sideKey;
    static{
        Random random = new Random(42);
        for(int color = 0 ; color < 2 ; color++){
            for(int pieceType = 0 ; pieceType < 6 ; pieceType++){
                for(int square = 0 ; square < 64 ; square++){
                    pieceKeys[color][pieceType][square] = random.nextLong();
                }
            }
        }
        for(int file = 0 ; file < 8 ; file++){
            enPassantKeys[file] = random.nextLong();
        }
        for(int i = 0 ; i < 16 ; i++){
            castlingKeys[i] = random.nextLong();
        }
        sideKey = random.nextLong();
    }
    public static int pieceTypeIndex(Piece piece){
        if(piece instanceof Pawn)return 0;
        if(piece instanceof Knight)return 1;
        if(piece instanceof Bishop)return 2;
        if(piece instanceof Rook)return 3;
        if(piece instanceof Queen)return 4;
        if(piece instanceof King)return 5;
        throw new IllegalArgumentException("Unknown piece type");
    }
    public static int colorIndex(String color){
        return color.equals("white") ? 0 : 1;
    }
    public static long computeHash(Board board,String currentPlayer){
        long hash = 0;
        for(int r = 0 ; r < 8 ; r++){
            for(int c = 0 ; c < 8 ; c++){
                Piece piece = board.getPiece(r,c);
                if(piece != null){
                    int colorIdx = colorIndex(piece.color);
                    int pieceIdx = pieceTypeIndex(piece);
                    int square = r * 8 + c;
                    hash ^= pieceKeys[colorIdx][pieceIdx][square];
                }
            }
        }
        if(board.enPassantCol >= 0 && board.enPassantCol < 8){
            hash ^= enPassantKeys[board.enPassantCol];
        }
        int castleIndex = computeCastlingIndex(board);
        hash ^= castlingKeys[castleIndex];
        if(currentPlayer.equals("black")){
            hash ^= sideKey;
        }
        return hash;
    }
    public static long computeHash(BitboardBoard board, String currentPlayer){
        long hash = 0;
        for(int color = 0; color < 2; color++){
            for(int type = 0; type < 6; type++){
                long bb = board.pieces[color][type];
                while(bb != 0){
                    int sq = Long.numberOfTrailingZeros(bb);
                    hash ^= pieceKeys[color][type][sq];
                    bb &= bb - 1;
                }
            }
        }
        if(board.enPassantSquare >= 0 && board.enPassantSquare < 64){
            hash ^= enPassantKeys[board.enPassantSquare % 8];
        }
        hash ^= castlingKeys[board.castlingRights];
        if(currentPlayer.equals("black")){
            hash ^= sideKey;
        }
        return hash;
    }
    public static long updateHash(Board board,long hash,Piece piece,int fromRow,int fromCol,int toRow,int toCol,
                                   Piece capturedPiece,boolean wasEnPassant,int enPassantColBefore,int enPassantColAfter,
                                   int castlingBefore,int castlingAfter,boolean sideChanged,
                                   boolean wasCastling,Piece castlingRook,int castlingRookFromCol,int castlingRookToCol){
        int colorIdx = colorIndex(piece.color);
        int pieceIdx = pieceTypeIndex(piece);
        int fromSquare = fromRow * 8 + fromCol;
        int toSquare = toRow * 8 + toCol;
        hash ^= pieceKeys[colorIdx][pieceIdx][fromSquare];
        hash ^= pieceKeys[colorIdx][pieceIdx][toSquare];
        if(capturedPiece != null && !wasEnPassant){
            int capColorIdx = colorIndex(capturedPiece.color);
            int capPieceIdx = pieceTypeIndex(capturedPiece);
            int capSquare = toRow * 8 + toCol;
            hash ^= pieceKeys[capColorIdx][capPieceIdx][capSquare];
        }
        if(wasEnPassant){
            int capturedPawnRow = (piece.color.equals("white")) ? toRow - 1 : toRow + 1;
            int capturedPawnSquare = capturedPawnRow * 8 + toCol;
            int oppColorIdx = colorIndex(piece.color.equals("white") ? "black" : "white");
            hash ^= pieceKeys[oppColorIdx][0][capturedPawnSquare];
        }
        if(enPassantColBefore >= 0){
            hash ^= enPassantKeys[enPassantColBefore];
        }
        if(piece instanceof Pawn && Math.abs(toRow - fromRow) == 2){
            hash ^= enPassantKeys[fromCol];
        }
        if(wasCastling && castlingRook != null){
            int rookColorIdx = colorIndex(castlingRook.color);
            int rookPieceIdx = pieceTypeIndex(castlingRook);
            int rookFromSquare = fromRow * 8 + castlingRookFromCol;
            int rookToSquare = fromRow * 8 + castlingRookToCol;
            hash ^= pieceKeys[rookColorIdx][rookPieceIdx][rookFromSquare];
            hash ^= pieceKeys[rookColorIdx][rookPieceIdx][rookToSquare];
        }
        hash ^= castlingKeys[castlingBefore];
        hash ^= castlingKeys[castlingAfter];
        if(sideChanged){
            hash ^= sideKey;
        }
        return hash;
    }
    public static int computeCastlingIndex(Board board){
        int index = 0;
        Piece wKing = findPiece(board,"white",King.class);
        Piece bKing = findPiece(board,"black",King.class);
        Piece wRookK = board.getPiece(0,7);
        Piece wRookQ = board.getPiece(0,0);
        Piece bRookK = board.getPiece(7,7);
        Piece bRookQ = board.getPiece(7,0);
        if(wKing != null && !wKing.isHasMoved()){
            if(wRookK instanceof Rook && wRookK.color.equals("white") && !wRookK.isHasMoved()) index |= 1;
            if(wRookQ instanceof Rook && wRookQ.color.equals("white") && !wRookQ.isHasMoved()) index |= 2;
        }
        if(bKing != null && !bKing.isHasMoved()){
            if(bRookK instanceof Rook && bRookK.color.equals("black") && !bRookK.isHasMoved()) index |= 4;
            if(bRookQ instanceof Rook && bRookQ.color.equals("black") && !bRookQ.isHasMoved()) index |= 8;
        }
        return index;
    }
    private static Piece findPiece(Board board,String color,Class<? extends Piece> type){
        for(int r = 0 ; r < 8 ; r++){
            for(int c = 0 ; c < 8 ; c++){
                Piece p = board.getPiece(r,c);
                if(p != null && p.color.equals(color) && type.isInstance(p)){
                    return p;
                }
            }
        }
        return null;
    }
}
