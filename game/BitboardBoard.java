package game;

import java.util.ArrayList;
import java.util.List;
import pieces.*;

public class BitboardBoard {
    public static final int PAWN = 0;
    public static final int KNIGHT = 1;
    public static final int BISHOP = 2;
    public static final int ROOK = 3;
    public static final int QUEEN = 4;
    public static final int KING = 5;

    public static final int WHITE = 0;
    public static final int BLACK = 1;

    long[][] pieces = new long[2][6];
    long whiteOccupancy;
    long blackOccupancy;
    long allOccupancy;

    public int sideToMove;
    public int castlingRights;
    public int enPassantSquare;
    public int halfMoveClock;
    public int fullMoveNumber;

    long hash;

    private BitMove[] history = new BitMove[2048];
    private int historyIndex = 0;

    private static final long[] knightAttacks = new long[64];
    private static final long[] kingAttacks = new long[64];
    private static final long[][] pawnAttacks = new long[2][64];

    static {
        initKnightAttacks();
        initKingAttacks();
        initPawnAttacks();
    }

    public BitboardBoard() {
        for (int c = 0; c < 2; c++) {
            for (int p = 0; p < 6; p++) {
                pieces[c][p] = 0L;
            }
        }
        whiteOccupancy = 0L;
        blackOccupancy = 0L;
        allOccupancy = 0L;
        sideToMove = WHITE;
        castlingRights = 0;
        enPassantSquare = -1;
        halfMoveClock = 0;
        fullMoveNumber = 1;
        hash = 0L;
        historyIndex = 0;
        for (int i = 0; i < history.length; i++) history[i] = null;
    }

    private static void initKnightAttacks() {
        int[][] knightMoves = {{2,1},{2,-1},{-2,1},{-2,-1},{1,2},{1,-2},{-1,2},{-1,-2}};
        for (int sq = 0; sq < 64; sq++) {
            long attacks = 0L;
            int r = sq / 8, c = sq % 8;
            for (int[] move : knightMoves) {
                int nr = r + move[0], nc = c + move[1];
                if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
                    attacks |= (1L << (nr * 8 + nc));
                }
            }
            knightAttacks[sq] = attacks;
        }
    }

    private static void initKingAttacks() {
        for (int sq = 0; sq < 64; sq++) {
            long attacks = 0L;
            int r = sq / 8, c = sq % 8;
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    int nr = r + dr, nc = c + dc;
                    if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
                        attacks |= (1L << (nr * 8 + nc));
                    }
                }
            }
            kingAttacks[sq] = attacks;
        }
    }

    private static void initPawnAttacks() {
        for (int sq = 0; sq < 64; sq++) {
            int r = sq / 8, c = sq % 8;
            long whiteAttack = 0L, blackAttack = 0L;
            if (r + 1 < 8) {
                if (c > 0) whiteAttack |= (1L << ((r + 1) * 8 + (c - 1)));
                if (c + 1 < 8) whiteAttack |= (1L << ((r + 1) * 8 + (c + 1)));
            }
            if (r - 1 >= 0) {
                if (c > 0) blackAttack |= (1L << ((r - 1) * 8 + (c - 1)));
                if (c + 1 < 8) blackAttack |= (1L << ((r - 1) * 8 + (c + 1)));
            }
            pawnAttacks[WHITE][sq] = whiteAttack;
            pawnAttacks[BLACK][sq] = blackAttack;
        }
    }

    public static long getBishopAttacks(int sq, long blockers) {
        long attacks = 0L;
        int r = sq / 8, c = sq % 8;
        int[] dr = {1, 1, -1, -1};
        int[] dc = {1, -1, 1, -1};
        for (int i = 0; i < 4; i++) {
            for (int j = 1; j < 8; j++) {
                int nr = r + dr[i] * j, nc = c + dc[i] * j;
                if (nr < 0 || nr >= 8 || nc < 0 || nc >= 8) break;
                int nsq = nr * 8 + nc;
                attacks |= (1L << nsq);
                if ((blockers & (1L << nsq)) != 0) break;
            }
        }
        return attacks;
    }

    public static long getRookAttacks(int sq, long blockers) {
        long attacks = 0L;
        int r = sq / 8, c = sq % 8;
        int[] dr = {1, -1, 0, 0};
        int[] dc = {0, 0, 1, -1};
        for (int i = 0; i < 4; i++) {
            for (int j = 1; j < 8; j++) {
                int nr = r + dr[i] * j, nc = c + dc[i] * j;
                if (nr < 0 || nr >= 8 || nc < 0 || nc >= 8) break;
                int nsq = nr * 8 + nc;
                attacks |= (1L << nsq);
                if ((blockers & (1L << nsq)) != 0) break;
            }
        }
        return attacks;
    }

    public static long getQueenAttacks(int sq, long blockers) {
        return getBishopAttacks(sq, blockers) | getRookAttacks(sq, blockers);
    }

    public void setPiece(int row, int col, Piece piece) {
        int sq = row * 8 + col;
        long bit = 1L << sq;
        whiteOccupancy &= ~bit;
        blackOccupancy &= ~bit;
        allOccupancy &= ~bit;
        for (int p = 0; p < 6; p++) {
            pieces[WHITE][p] &= ~bit;
            pieces[BLACK][p] &= ~bit;
        }
        if (piece != null) {
            int color = piece.color.equals("white") ? WHITE : BLACK;
            int type = pieceType(piece);
            pieces[color][type] |= bit;
            if (color == WHITE) whiteOccupancy |= bit;
            else blackOccupancy |= bit;
            allOccupancy |= bit;
        }
    }

    public Piece getPiece(int row, int col) {
        int sq = row * 8 + col;
        long bit = 1L << sq;
        for (int color = 0; color < 2; color++) {
            for (int type = 0; type < 6; type++) {
                if ((pieces[color][type] & bit) != 0) {
                    String colorStr = color == WHITE ? "white" : "black";
                    return createPiece(type, colorStr, row, col);
                }
            }
        }
        return null;
    }

    private Piece createPiece(int type, String color, int row, int col) {
        return switch (type) {
            case PAWN -> new Pawn(color, row, col);
            case KNIGHT -> new Knight(color, row, col);
            case BISHOP -> new Bishop(color, row, col);
            case ROOK -> new Rook(color, row, col);
            case QUEEN -> new Queen(color, row, col);
            case KING -> {
                King k = new King(color, row, col);
                int c = color.equals("white") ? WHITE : BLACK;
                k.hasMoved = !hasCastlingRight(c == WHITE ? 0 : 2);
                yield k;
            }
            default -> null;
        };
    }

    private int pieceType(Piece p) {
        if (p instanceof Pawn) return PAWN;
        if (p instanceof Knight) return KNIGHT;
        if (p instanceof Bishop) return BISHOP;
        if (p instanceof Rook) return ROOK;
        if (p instanceof Queen) return QUEEN;
        if (p instanceof King) return KING;
        throw new IllegalArgumentException("Unknown piece");
    }

    public boolean isInsideBoard(int row, int col) {
        return row >= 0 && col >= 0 && row < 8 && col < 8;
    }

    public long getHash() {
        return hash;
    }

    public void initializeBoard() {
        for (int c = 0; c < 2; c++) {
            for (int p = 0; p < 6; p++) {
                pieces[c][p] = 0L;
            }
        }
        whiteOccupancy = 0L;
        blackOccupancy = 0L;
        allOccupancy = 0L;

        pieces[WHITE][PAWN] = 0x000000000000FF00L;
        pieces[WHITE][ROOK] = 0x0000000000000081L;
        pieces[WHITE][KNIGHT] = 0x0000000000000042L;
        pieces[WHITE][BISHOP] = 0x0000000000000024L;
        pieces[WHITE][QUEEN] = 0x0000000000000008L;
        pieces[WHITE][KING] = 0x0000000000000010L;

        pieces[BLACK][PAWN] = 0x00FF000000000000L;
        pieces[BLACK][ROOK] = 0x8100000000000000L;
        pieces[BLACK][KNIGHT] = 0x4200000000000000L;
        pieces[BLACK][BISHOP] = 0x2400000000000000L;
        pieces[BLACK][QUEEN] = 0x0800000000000000L;
        pieces[BLACK][KING] = 0x1000000000000000L;

        whiteOccupancy = 0x000000000000FFFFL;
        blackOccupancy = 0xFFFF000000000000L;
        allOccupancy = whiteOccupancy | blackOccupancy;

        sideToMove = WHITE;
        castlingRights = 0xF;
        enPassantSquare = -1;
        halfMoveClock = 0;
        fullMoveNumber = 1;
        historyIndex = 0;
        for (int i = 0; i < history.length; i++) history[i] = null;

        hash = Zobrist.computeHash(this, sideToMove == WHITE ? "white" : "black");
    }

    public boolean hasCastlingRight(int index) {
        return (castlingRights & (1 << index)) != 0;
    }

    public void makeMove(int from, int to, int promotion, String player) {
        int color = player.equals("white") ? WHITE : BLACK;
        int opp = color ^ 1;

        int movedType = getPieceTypeAt(from);
        int capturedType = getPieceTypeAt(to);

        long prevHash = hash;
        int prevCastlingRights = castlingRights;
        int prevEnPassantSquare = enPassantSquare;
        int prevHalfMoveClock = halfMoveClock;

        boolean wasEnPassantCapture = false;
        int enPassantCapturedSquare = -1;
        boolean wasCastling = false;
        int rookFrom = -1, rookTo = -1;
        int capturedTypeActual = capturedType;

        long fromBit = 1L << from;
        long toBit = 1L << to;

        halfMoveClock++;

        if (movedType == PAWN) {
            halfMoveClock = 0;
            int fromRow = from / 8, toRow = to / 8;
            if (Math.abs(toRow - fromRow) == 2) {
                enPassantSquare = (from + to) / 2;
            } else if (to == enPassantSquare && enPassantSquare >= 0) {
                wasEnPassantCapture = true;
                enPassantCapturedSquare = (color == WHITE) ? to - 8 : to + 8;
                long epBit = 1L << enPassantCapturedSquare;
                pieces[opp][PAWN] &= ~epBit;
                if (color == WHITE) whiteOccupancy &= ~epBit;
                else blackOccupancy &= ~epBit;
                allOccupancy &= ~epBit;
                hash ^= Zobrist.pieceKeys[opp][PAWN][enPassantCapturedSquare];
                capturedTypeActual = PAWN;
            } else {
                enPassantSquare = -1;
            }
        } else {
            enPassantSquare = -1;
        }

        if (movedType == KING && Math.abs((to % 8) - (from % 8)) == 2) {
            wasCastling = true;
            boolean isKingside = (to % 8) == 6;
            rookFrom = isKingside ? from + 3 : from - 4;
            rookTo = isKingside ? from + 1 : from - 1;
            long rookFromBit = 1L << rookFrom;
            long rookToBit = 1L << rookTo;
            pieces[color][ROOK] &= ~rookFromBit;
            pieces[color][ROOK] |= rookToBit;
            if (color == WHITE) {
                whiteOccupancy &= ~rookFromBit;
                whiteOccupancy |= rookToBit;
            } else {
                blackOccupancy &= ~rookFromBit;
                blackOccupancy |= rookToBit;
            }
            allOccupancy &= ~rookFromBit;
            allOccupancy |= rookToBit;
            hash ^= Zobrist.pieceKeys[color][ROOK][rookFrom];
            hash ^= Zobrist.pieceKeys[color][ROOK][rookTo];
        }

        if (movedType == KING) {
            if (color == WHITE) {
                castlingRights &= ~0x3;
            } else {
                castlingRights &= ~0xC;
            }
        }
        if (movedType == ROOK) {
            if (from == 0) castlingRights &= ~0x2;
            else if (from == 7) castlingRights &= ~0x1;
            else if (from == 56) castlingRights &= ~0x8;
            else if (from == 63) castlingRights &= ~0x4;
        }
        if (capturedType >= 0) {
            if (to == 0) castlingRights &= ~0x2;
            else if (to == 7) castlingRights &= ~0x1;
            else if (to == 56) castlingRights &= ~0x8;
            else if (to == 63) castlingRights &= ~0x4;
        }

        hash ^= Zobrist.pieceKeys[color][movedType][from];
        hash ^= Zobrist.pieceKeys[color][movedType][to];

        pieces[color][movedType] &= ~fromBit;
        pieces[color][movedType] |= toBit;
        if (color == WHITE) {
            whiteOccupancy &= ~fromBit;
            whiteOccupancy |= toBit;
        } else {
            blackOccupancy &= ~fromBit;
            blackOccupancy |= toBit;
        }
        allOccupancy &= ~fromBit;
        allOccupancy |= toBit;

        if (capturedType >= 0 && !wasEnPassantCapture) {
            hash ^= Zobrist.pieceKeys[opp][capturedType][to];
            pieces[opp][capturedType] &= ~toBit;
            if (opp == WHITE) whiteOccupancy &= ~toBit;
            else blackOccupancy &= ~toBit;
            allOccupancy &= ~toBit;
        }

        if (promotion >= 0) {
            pieces[color][movedType] &= ~toBit;
            pieces[color][promotion] |= toBit;
            hash ^= Zobrist.pieceKeys[color][movedType][to];
            hash ^= Zobrist.pieceKeys[color][promotion][to];
        }

        if (prevEnPassantSquare >= 0) {
            hash ^= Zobrist.enPassantKeys[prevEnPassantSquare % 8];
        }
        if (enPassantSquare >= 0) {
            hash ^= Zobrist.enPassantKeys[enPassantSquare % 8];
        }

        hash ^= Zobrist.castlingKeys[prevCastlingRights];
        hash ^= Zobrist.castlingKeys[castlingRights];

        hash ^= Zobrist.sideKey;

        int promoFlag = promotion >= 0 ? promotion : 0;
        int flag = wasEnPassantCapture ? BitMove.FLAG_EN_PASSANT : (wasCastling ? BitMove.FLAG_CASTLING : BitMove.FLAG_NONE);

        history[historyIndex++] = new BitMove(from, to, promoFlag, flag,
                movedType, capturedTypeActual,
                prevCastlingRights, prevEnPassantSquare,
                prevHalfMoveClock, prevHash,
                wasEnPassantCapture, enPassantCapturedSquare,
                wasCastling, rookFrom, rookTo);

        sideToMove = opp;
        if (sideToMove == WHITE) fullMoveNumber++;
    }

    public void makeMove(Piece piece, int toRow, int toCol, String player) {
        int from = piece.row * 8 + piece.col;
        int to = toRow * 8 + toCol;
        int promotion = -1;
        if (piece instanceof Pawn) {
            int promoRow = player.equals("white") ? 7 : 0;
            if (toRow == promoRow) {
                promotion = QUEEN;
            }
        }
        makeMove(from, to, promotion, player);
    }

    public void undoMove() {
        if (historyIndex <= 0) return;
        BitMove move = history[--historyIndex];
        history[historyIndex] = null;

        int from = move.from();
        int to = move.to();
        int color = sideToMove ^ 1;
        int opp = sideToMove;
        int movedType = move.movedPiece();
        int capturedType = move.capturedPiece();

        long fromBit = 1L << from;
        long toBit = 1L << to;

        if (move.wasCastling()) {
            long rookFromBit = 1L << move.rookFrom();
            long rookToBit = 1L << move.rookTo();
            pieces[color][ROOK] &= ~rookToBit;
            pieces[color][ROOK] |= rookFromBit;
            if (color == WHITE) {
                whiteOccupancy &= ~rookToBit;
                whiteOccupancy |= rookFromBit;
            } else {
                blackOccupancy &= ~rookToBit;
                blackOccupancy |= rookFromBit;
            }
            allOccupancy &= ~rookToBit;
            allOccupancy |= rookFromBit;
        }

        pieces[color][movedType] &= ~toBit;
        pieces[color][movedType] |= fromBit;
        if (color == WHITE) {
            whiteOccupancy &= ~toBit;
            whiteOccupancy |= fromBit;
        } else {
            blackOccupancy &= ~toBit;
            blackOccupancy |= fromBit;
        }
        allOccupancy &= ~toBit;
        allOccupancy |= fromBit;

        if (move.wasEnPassantCapture()) {
            int epSq = move.enPassantCapturedSquare();
            long epBit = 1L << epSq;
            pieces[opp][PAWN] |= epBit;
            if (opp == WHITE) whiteOccupancy |= epBit;
            else blackOccupancy |= epBit;
            allOccupancy |= epBit;
        } else if (capturedType >= 0) {
            pieces[opp][capturedType] |= toBit;
            if (opp == WHITE) whiteOccupancy |= toBit;
            else blackOccupancy |= toBit;
            allOccupancy |= toBit;
        }

        sideToMove = color;
        castlingRights = move.prevCastlingRights();
        enPassantSquare = move.prevEnPassantSquare();
        halfMoveClock = move.prevHalfMoveClock();
        hash = move.prevHash();
        if (sideToMove == BLACK) fullMoveNumber--;
    }

    public boolean undoLastFullMove() {
        if (historyIndex < 2) return false;
        undoMove();
        undoMove();
        return true;
    }

    int getPieceTypeAt(int sq) {
        long bit = 1L << sq;
        for (int color = 0; color < 2; color++) {
            for (int type = 0; type < 6; type++) {
                if ((pieces[color][type] & bit) != 0) {
                    return type;
                }
            }
        }
        return -1;
    }

    public boolean isSquareAttacked(int sq, int byColor) {
        long bit = 1L << sq;
        if ((pawnAttacks[byColor ^ 1][sq] & pieces[byColor][PAWN]) != 0) return true;
        if ((knightAttacks[sq] & pieces[byColor][KNIGHT]) != 0) return true;
        if ((kingAttacks[sq] & pieces[byColor][KING]) != 0) return true;
        if ((getBishopAttacks(sq, allOccupancy) & (pieces[byColor][BISHOP] | pieces[byColor][QUEEN])) != 0) return true;
        if ((getRookAttacks(sq, allOccupancy) & (pieces[byColor][ROOK] | pieces[byColor][QUEEN])) != 0) return true;
        return false;
    }

    public boolean isKingInCheck(String color) {
        int c = color.equals("white") ? WHITE : BLACK;
        int opp = c ^ 1;
        int kingSq = Long.numberOfTrailingZeros(pieces[c][KING]);
        if (kingSq >= 64) return false;
        return isSquareAttacked(kingSq, opp);
    }

    public boolean isCheckmate(String color) {
        return isKingInCheck(color) && !hasLegalMoves(color);
    }

    public boolean isStalemate(String color) {
        if (isKingInCheck(color)) return false;
        return !hasLegalMoves(color);
    }

    public boolean hasLegalMoves(String color) {
        List<BitMove> moves = generateLegalMoves(color);
        return !moves.isEmpty();
    }

    public List<BitMove> generateLegalMoves(String color) {
        List<BitMove> moves = new ArrayList<>();
        int c = color.equals("white") ? WHITE : BLACK;
        int opp = c ^ 1;
        int kingSq = Long.numberOfTrailingZeros(pieces[c][KING]);

        long pinned = computePinned(c);

        long pawns = pieces[c][PAWN];
        while (pawns != 0) {
            int from = Long.numberOfTrailingZeros(pawns);
            pawns &= pawns - 1;
            generatePawnMoves(from, c, opp, pinned, moves);
        }

        long knights = pieces[c][KNIGHT];
        while (knights != 0) {
            int from = Long.numberOfTrailingZeros(knights);
            knights &= knights - 1;
            if ((pinned & (1L << from)) == 0 || !isPinnedAlongLine(from, kingSq, pinned)) {
                long attacks = knightAttacks[from] & ~allOccupancy(c);
                while (attacks != 0) {
                    int to = Long.numberOfTrailingZeros(attacks);
                    attacks &= attacks - 1;
                    if (isMoveLegal(from, to, c)) {
                        addMove(from, to, c, moves);
                    }
                }
            }
        }

        long bishops = pieces[c][BISHOP];
        while (bishops != 0) {
            int from = Long.numberOfTrailingZeros(bishops);
            bishops &= bishops - 1;
            long attacks = getBishopAttacks(from, allOccupancy) & ~allOccupancy(c);
            if ((pinned & (1L << from)) != 0) {
                attacks &= computePinRay(from, kingSq);
            }
            while (attacks != 0) {
                int to = Long.numberOfTrailingZeros(attacks);
                attacks &= attacks - 1;
                if (isMoveLegal(from, to, c)) {
                    addMove(from, to, c, moves);
                }
            }
        }

        long rooks = pieces[c][ROOK];
        while (rooks != 0) {
            int from = Long.numberOfTrailingZeros(rooks);
            rooks &= rooks - 1;
            long attacks = getRookAttacks(from, allOccupancy) & ~allOccupancy(c);
            if ((pinned & (1L << from)) != 0) {
                attacks &= computePinRay(from, kingSq);
            }
            while (attacks != 0) {
                int to = Long.numberOfTrailingZeros(attacks);
                attacks &= attacks - 1;
                if (isMoveLegal(from, to, c)) {
                    addMove(from, to, c, moves);
                }
            }
        }

        long queens = pieces[c][QUEEN];
        while (queens != 0) {
            int from = Long.numberOfTrailingZeros(queens);
            queens &= queens - 1;
            long attacks = getQueenAttacks(from, allOccupancy) & ~allOccupancy(c);
            if ((pinned & (1L << from)) != 0) {
                attacks &= computePinRay(from, kingSq);
            }
            while (attacks != 0) {
                int to = Long.numberOfTrailingZeros(attacks);
                attacks &= attacks - 1;
                if (isMoveLegal(from, to, c)) {
                    addMove(from, to, c, moves);
                }
            }
        }

        if ((pinned & (1L << kingSq)) == 0) {
            long attacks = kingAttacks[kingSq] & ~allOccupancy(c);
            while (attacks != 0) {
                int to = Long.numberOfTrailingZeros(attacks);
                attacks &= attacks - 1;
                if (!isSquareAttacked(to, opp)) {
                    addMove(kingSq, to, c, moves);
                }
            }
            if (c == WHITE) {
                if (hasCastlingRight(0) && (allOccupancy & 0x60L) == 0 &&
                    !isSquareAttacked(4, opp) && !isSquareAttacked(5, opp) && !isSquareAttacked(6, opp)) {
                    moves.add(new BitMove(4, 6, -1, BitMove.FLAG_CASTLING, KING, -1, castlingRights, enPassantSquare, halfMoveClock, hash, false, -1, true, 7, 5));
                }
                if (hasCastlingRight(1) && (allOccupancy & 0x0EL) == 0 &&
                    !isSquareAttacked(4, opp) && !isSquareAttacked(3, opp) && !isSquareAttacked(2, opp)) {
                    moves.add(new BitMove(4, 2, -1, BitMove.FLAG_CASTLING, KING, -1, castlingRights, enPassantSquare, halfMoveClock, hash, false, -1, true, 0, 3));
                }
            } else {
                if (hasCastlingRight(2) && (allOccupancy & 0x6000000000000000L) == 0 &&
                    !isSquareAttacked(60, opp) && !isSquareAttacked(61, opp) && !isSquareAttacked(62, opp)) {
                    moves.add(new BitMove(60, 62, -1, BitMove.FLAG_CASTLING, KING, -1, castlingRights, enPassantSquare, halfMoveClock, hash, false, -1, true, 63, 61));
                }
                if (hasCastlingRight(3) && (allOccupancy & 0x0E00000000000000L) == 0 &&
                    !isSquareAttacked(60, opp) && !isSquareAttacked(59, opp) && !isSquareAttacked(58, opp)) {
                    moves.add(new BitMove(60, 58, -1, BitMove.FLAG_CASTLING, KING, -1, castlingRights, enPassantSquare, halfMoveClock, hash, false, -1, true, 56, 57));
                }
            }
        }

        return moves;
    }

    long allOccupancy(int color) {
        return color == WHITE ? whiteOccupancy : blackOccupancy;
    }

    private void generatePawnMoves(int from, int color, int opp, long pinned, List<BitMove> moves) {
        int kingSq = Long.numberOfTrailingZeros(pieces[color][KING]);
        long fromBit = 1L << from;
        if ((pinned & fromBit) != 0 && !isPinnedAlongLine(from, kingSq, pinned)) return;

        int dir = color == WHITE ? 8 : -8;
        int startRow = color == WHITE ? 1 : 6;
        int promoRow = color == WHITE ? 7 : 0;
        int to1 = from + dir;
        long to1Bit = 1L << to1;

        long pinRay = (pinned & fromBit) != 0 ? computePinRay(from, kingSq) : ~0L;

        if ((to1Bit & allOccupancy) == 0) {
            if (to1 / 8 == promoRow) {
                if ((pinRay & to1Bit) != 0) {
                    addPromotionMoves(from, to1, color, moves);
                }
            } else {
                if ((pinRay & to1Bit) != 0) {
                    moves.add(new BitMove(from, to1, -1, BitMove.FLAG_NONE, PAWN, -1, castlingRights, enPassantSquare, halfMoveClock, hash, false, -1, false, -1, -1));
                }
                int to2 = from + 2 * dir;
                long to2Bit = 1L << to2;
                if (from / 8 == startRow && (to2Bit & allOccupancy) == 0 && (pinRay & to2Bit) != 0) {
                    moves.add(new BitMove(from, to2, -1, BitMove.FLAG_NONE, PAWN, -1, castlingRights, enPassantSquare, halfMoveClock, hash, false, -1, false, -1, -1));
                }
            }
        }

        long captureTargets = pawnAttacks[color][from] & allOccupancy(opp);
        while (captureTargets != 0) {
            int to = Long.numberOfTrailingZeros(captureTargets);
            captureTargets &= captureTargets - 1;
            if ((pinRay & (1L << to)) == 0) continue;
            if (to / 8 == promoRow) {
                addPromotionMoves(from, to, color, moves);
            } else {
                moves.add(new BitMove(from, to, -1, BitMove.FLAG_NONE, PAWN, getPieceTypeAt(to), castlingRights, enPassantSquare, halfMoveClock, hash, false, -1, false, -1, -1));
            }
        }

        if (enPassantSquare >= 0) {
            long epBit = 1L << enPassantSquare;
            long epCaptures = pawnAttacks[color][from] & epBit;
            while (epCaptures != 0) {
                int to = Long.numberOfTrailingZeros(epCaptures);
                epCaptures &= epCaptures - 1;
                int capturedPawnSq = to - dir;
                long tempAll = allOccupancy & ~(fromBit | (1L << capturedPawnSq)) | (1L << to);
                if (!isSquareAttackedFast(kingSq, opp, tempAll, color)) {
                    moves.add(new BitMove(from, to, -1, BitMove.FLAG_EN_PASSANT, PAWN, PAWN, castlingRights, enPassantSquare, halfMoveClock, hash, true, capturedPawnSq, false, -1, -1));
                }
            }
        }
    }

    private void addPromotionMoves(int from, int to, int color, List<BitMove> moves) {
        int captured = getPieceTypeAt(to);
        moves.add(new BitMove(from, to, BitMove.PROMOTION_QUEEN, BitMove.FLAG_NONE, PAWN, captured, castlingRights, enPassantSquare, halfMoveClock, hash, false, -1, false, -1, -1));
        moves.add(new BitMove(from, to, BitMove.PROMOTION_ROOK, BitMove.FLAG_NONE, PAWN, captured, castlingRights, enPassantSquare, halfMoveClock, hash, false, -1, false, -1, -1));
        moves.add(new BitMove(from, to, BitMove.PROMOTION_BISHOP, BitMove.FLAG_NONE, PAWN, captured, castlingRights, enPassantSquare, halfMoveClock, hash, false, -1, false, -1, -1));
        moves.add(new BitMove(from, to, BitMove.PROMOTION_KNIGHT, BitMove.FLAG_NONE, PAWN, captured, castlingRights, enPassantSquare, halfMoveClock, hash, false, -1, false, -1, -1));
    }

    private void addMove(int from, int to, int color, List<BitMove> moves) {
        int captured = getPieceTypeAt(to);
        int movedType = getPieceTypeAt(from);
        moves.add(new BitMove(from, to, -1, BitMove.FLAG_NONE, movedType, captured, castlingRights, enPassantSquare, halfMoveClock, hash, false, -1, false, -1, -1));
    }

    private boolean isMoveLegal(int from, int to, int color) {
        int kingSq = Long.numberOfTrailingZeros(pieces[color][KING]);
        int opp = color ^ 1;
        long movedPiece = 1L << from;
        long capturedPiece = 1L << to;
        long tempAll = (allOccupancy & ~movedPiece & ~capturedPiece) | (1L << to);

        if (getPieceTypeAt(from) == KING) {
            return !isSquareAttackedFast(to, opp, tempAll, color);
        }

        return !isSquareAttackedFast(kingSq, opp, tempAll, color);
    }

    private boolean isSquareAttackedFast(int sq, int byColor, long tempAll, int myColor) {
        if ((pawnAttacks[byColor ^ 1][sq] & pieces[byColor][PAWN]) != 0) return true;
        if ((knightAttacks[sq] & pieces[byColor][KNIGHT]) != 0) return true;
        if ((kingAttacks[sq] & pieces[byColor][KING]) != 0) return true;

        long bishops = pieces[byColor][BISHOP] | pieces[byColor][QUEEN];
        if (bishops != 0 && (getBishopAttacks(sq, tempAll) & bishops) != 0) return true;

        long rooks = pieces[byColor][ROOK] | pieces[byColor][QUEEN];
        if (rooks != 0 && (getRookAttacks(sq, tempAll) & rooks) != 0) return true;

        return false;
    }

    private long computePinned(int color) {
        long pinned = 0L;
        int kingSq = Long.numberOfTrailingZeros(pieces[color][KING]);
        if (kingSq >= 64) return 0L;
        int opp = color ^ 1;

        long pinners = pieces[opp][ROOK] | pieces[opp][QUEEN];
        long rookAttacks = getRookAttacks(kingSq, allOccupancy);
        long rookPinners = pinners & rookAttacks;
        while (rookPinners != 0) {
            int pinnerSq = Long.numberOfTrailingZeros(rookPinners);
            rookPinners &= rookPinners - 1;
            long between = betweenSquares(kingSq, pinnerSq) & allOccupancy;
            if (Long.bitCount(between) == 1 && (between & allOccupancy(color)) != 0) {
                pinned |= between;
            }
        }

        pinners = pieces[opp][BISHOP] | pieces[opp][QUEEN];
        long bishopAttacks = getBishopAttacks(kingSq, allOccupancy);
        long bishopPinners = pinners & bishopAttacks;
        while (bishopPinners != 0) {
            int pinnerSq = Long.numberOfTrailingZeros(bishopPinners);
            bishopPinners &= bishopPinners - 1;
            long between = betweenSquares(kingSq, pinnerSq) & allOccupancy;
            if (Long.bitCount(between) == 1 && (between & allOccupancy(color)) != 0) {
                pinned |= between;
            }
        }

        return pinned;
    }

    private boolean isPinnedAlongLine(int sq, int kingSq, long pinned) {
        if ((pinned & (1L << sq)) == 0) return false;
        long ray = computePinRay(sq, kingSq);
        return (ray & (1L << sq)) != 0;
    }

    private long computePinRay(int sq, int kingSq) {
        long ray = 0L;
        int dr = (sq / 8) - (kingSq / 8);
        int dc = (sq % 8) - (kingSq % 8);
        if (dr == 0 && dc == 0) return ~0L;
        int stepR = dr == 0 ? 0 : dr / Math.abs(dr);
        int stepC = dc == 0 ? 0 : dc / Math.abs(dc);
        int r = sq / 8 + stepR;
        int c = sq % 8 + stepC;
        while (r >= 0 && r < 8 && c >= 0 && c < 8) {
            ray |= (1L << (r * 8 + c));
            r += stepR;
            c += stepC;
        }
        return ray;
    }

    private long betweenSquares(int sq1, int sq2) {
        long between = 0L;
        int r1 = sq1 / 8, c1 = sq1 % 8;
        int r2 = sq2 / 8, c2 = sq2 % 8;
        int dr = r2 > r1 ? 1 : r2 < r1 ? -1 : 0;
        int dc = c2 > c1 ? 1 : c2 < c1 ? -1 : 0;
        int r = r1 + dr, c = c1 + dc;
        while (r != r2 || c != c2) {
            between |= (1L << (r * 8 + c));
            r += dr;
            c += dc;
        }
        return between;
    }

    public void printBoard() {
        for (int r = 7; r >= 0; r--) {
            System.out.print((r + 1) + " ");
            for (int c = 0; c < 8; c++) {
                int sq = r * 8 + c;
                long bit = 1L << sq;
                Piece p = null;
                for (int color = 0; color < 2; color++) {
                    for (int type = 0; type < 6; type++) {
                        if ((pieces[color][type] & bit) != 0) {
                            String colorStr = color == WHITE ? "white" : "black";
                            p = createPiece(type, colorStr, r, c);
                            break;
                        }
                    }
                    if (p != null) break;
                }
                if (p != null) {
                    System.out.print(p.getSymbol() + " ");
                } else {
                    System.out.print(((r + c) % 2 == 0) ? ". " : "# ");
                }
            }
            System.out.println(" " + (r + 1));
        }
        System.out.println("  a b c d e f g h");
    }

    public String getSideToMoveStr() {
        return sideToMove == WHITE ? "white" : "black";
    }

    public static long getPawnAttacksStatic(int color, int sq) {
        return pawnAttacks[color][sq];
    }

    public static long getKnightAttacksStatic(int sq) {
        return knightAttacks[sq];
    }

    public static long getKingAttacksStatic(int sq) {
        return kingAttacks[sq];
    }
}
