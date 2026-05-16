package game;

public class BitMove {
    public static final int PROMOTION_QUEEN = 0;
    public static final int PROMOTION_ROOK = 1;
    public static final int PROMOTION_BISHOP = 2;
    public static final int PROMOTION_KNIGHT = 3;
    public static final int FLAG_NONE = 0;
    public static final int FLAG_EN_PASSANT = 1;
    public static final int FLAG_CASTLING = 2;

    private final int from;
    private final int to;
    private final int promotion;
    private final int flag;
    private final int movedPiece;
    private final int capturedPiece;
    private final int prevCastlingRights;
    private final int prevEnPassantSquare;
    private final int prevHalfMoveClock;
    private final long prevHash;
    private final boolean wasEnPassantCapture;
    private final int enPassantCapturedSquare;
    private final boolean wasCastling;
    private final int rookFrom;
    private final int rookTo;

    public BitMove(int from, int to, int promotion, int flag,
                   int movedPiece, int capturedPiece,
                   int prevCastlingRights, int prevEnPassantSquare,
                   int prevHalfMoveClock, long prevHash,
                   boolean wasEnPassantCapture, int enPassantCapturedSquare,
                   boolean wasCastling, int rookFrom, int rookTo) {
        this.from = from;
        this.to = to;
        this.promotion = promotion;
        this.flag = flag;
        this.movedPiece = movedPiece;
        this.capturedPiece = capturedPiece;
        this.prevCastlingRights = prevCastlingRights;
        this.prevEnPassantSquare = prevEnPassantSquare;
        this.prevHalfMoveClock = prevHalfMoveClock;
        this.prevHash = prevHash;
        this.wasEnPassantCapture = wasEnPassantCapture;
        this.enPassantCapturedSquare = enPassantCapturedSquare;
        this.wasCastling = wasCastling;
        this.rookFrom = rookFrom;
        this.rookTo = rookTo;
    }

    public int from() { return from; }
    public int to() { return to; }
    public int promotion() { return promotion; }
    public int flag() { return flag; }
    public int movedPiece() { return movedPiece; }
    public int capturedPiece() { return capturedPiece; }
    public int prevCastlingRights() { return prevCastlingRights; }
    public int prevEnPassantSquare() { return prevEnPassantSquare; }
    public int prevHalfMoveClock() { return prevHalfMoveClock; }
    public long prevHash() { return prevHash; }
    public boolean wasEnPassantCapture() { return wasEnPassantCapture; }
    public int enPassantCapturedSquare() { return enPassantCapturedSquare; }
    public boolean wasCastling() { return wasCastling; }
    public int rookFrom() { return rookFrom; }
    public int rookTo() { return rookTo; }

    public int fromRow() { return from / 8; }
    public int fromCol() { return from % 8; }
    public int toRow() { return to / 8; }
    public int toCol() { return to % 8; }

    @Override
    public String toString() {
        char fromFile = (char)('a' + fromCol());
        char fromRank = (char)('1' + fromRow());
        char toFile = (char)('a' + toCol());
        char toRank = (char)('1' + toRow());
        String promo = "";
        if (flag == FLAG_NONE && promotion >= 0) {
            promo = switch (promotion) {
                case PROMOTION_QUEEN -> "q";
                case PROMOTION_ROOK -> "r";
                case PROMOTION_BISHOP -> "b";
                case PROMOTION_KNIGHT -> "n";
                default -> "";
            };
        }
        return "" + fromFile + fromRank + toFile + toRank + promo;
    }
}
