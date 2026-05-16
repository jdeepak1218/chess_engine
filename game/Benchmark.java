package game;
import pieces.*;
import java.util.*;
public class Benchmark{
    public static long nodesSearched;
    public static long ttHits;
    public static long ttMisses;

    // Move ordering tables for without-TT search
    public static int[][] killerNoTT = new int[100][2];
    public static int[][] historyNoTT = new int[64][64];

    // Piece-Square Tables
    public static final int[] PAWN_PSQT = {
          0,  0,  0,  0,  0,  0,  0,  0,
          5, 10, 10,-20,-20, 10, 10,  5,
          5, -5,-10,  0,  0,-10, -5,  5,
          0,  0,  0, 20, 20,  0,  0,  0,
          5,  5, 10, 25, 25, 10,  5,  5,
         10, 10, 20, 30, 30, 20, 10, 10,
         50, 50, 50, 50, 50, 50, 50, 50,
          0,  0,  0,  0,  0,  0,  0,  0
    };
    public static final int[] KNIGHT_PSQT = {
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    };
    public static final int[] BISHOP_PSQT = {
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    };
    public static final int[] ROOK_PSQT = {
          0,  0,  0,  0,  0,  0,  0,  0,
          5, 10, 10, 10, 10, 10, 10,  5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
          0,  0,  0,  5,  5,  0,  0,  0
    };
    public static final int[] QUEEN_PSQT = {
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
         -5,  0,  5,  5,  5,  5,  0, -5,
          0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    };
    public static final int[] KING_PSQT = {
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
         20, 20,  0,  0,  0,  0, 20, 20,
         20, 30, 10,  0,  0, 10, 30, 20
    };

    // Move ordering tables for with-TT search
    public static int[][] killerWithTT = new int[100][2];
    public static int[][] historyWithTT = new int[64][64];

    public static void main(String[] args) throws Exception{
        System.out.println("=== CHESS ENGINE BENCHMARK (BITBOARD) ===\n");
        System.out.println("Position: Starting position");
        System.out.println("Engine: DeepakChess with bitboard representation");
        System.out.println();
        benchmarkWithoutTT();
        benchmarkWithTT();
        benchmarkHashOverhead();
        benchmarkIncrementalHash();
        System.out.println("\n=== BENCHMARK COMPLETE ===");
    }
    static void benchmarkWithoutTT() throws Exception{
        System.out.println("--- WITHOUT TRANSPOSITION TABLE ---");
        int[] depths = {2, 3, 4, 5};
        for(int depth : depths){
            BitboardBoard board = new BitboardBoard();
            board.initializeBoard();
            nodesSearched = 0;
            // Clear move ordering tables
            for(int i = 0; i < killerNoTT.length; i++){ killerNoTT[i][0] = 0; killerNoTT[i][1] = 0; }
            for(int i = 0; i < 64; i++){ Arrays.fill(historyNoTT[i], 0); }
            long start = System.nanoTime();
            int score = searchWithoutTT(board, depth, "white", -Integer.MAX_VALUE, Integer.MAX_VALUE);
            long end = System.nanoTime();
            long elapsedMs = (end - start) / 1_000_000;
            double elapsedSec = elapsedMs / 1000.0;
            long nps = elapsedMs > 0 ? (long)(nodesSearched / elapsedSec) : nodesSearched;
            System.out.printf("Depth %-2d | Score: %6d | Nodes: %10d | Time: %8.3fs | NPS: %10d%n",
                    depth, score, nodesSearched, elapsedSec, nps);
        }
        System.out.println();
    }
    static void benchmarkWithTT() throws Exception{
        System.out.println("--- WITH TRANSPOSITION TABLE ---");
        int[] depths = {2, 3, 4, 5};
        for(int depth : depths){
            BitboardBoard board = new BitboardBoard();
            board.initializeBoard();
            TranspositionTable tt = new TranspositionTable(1 << 18);
            nodesSearched = 0;
            ttHits = 0;
            ttMisses = 0;
            // Clear move ordering tables
            for(int i = 0; i < killerWithTT.length; i++){ killerWithTT[i][0] = 0; killerWithTT[i][1] = 0; }
            for(int i = 0; i < 64; i++){ Arrays.fill(historyWithTT[i], 0); }
            long start = System.nanoTime();
            int score = searchWithTT(board, depth, "white", -Integer.MAX_VALUE, Integer.MAX_VALUE, tt);
            long end = System.nanoTime();
            long elapsedMs = (end - start) / 1_000_000;
            double elapsedSec = elapsedMs / 1000.0;
            long nps = elapsedMs > 0 ? (long)(nodesSearched / elapsedSec) : nodesSearched;
            double hitRate = (ttHits + ttMisses) > 0 ? (double)ttHits / (ttHits + ttMisses) : 0;
            System.out.printf("Depth %-2d | Score: %6d | Nodes: %10d | TT Hits: %8d | HitRate: %.1f%% | Time: %8.3fs | NPS: %10d%n",
                    depth, score, nodesSearched, ttHits, hitRate * 100, elapsedSec, nps);
        }
        System.out.println();
    }
    static void benchmarkHashOverhead() throws Exception{
        System.out.println("--- HASH OVERHEAD BENCHMARK ---");
        BitboardBoard board = new BitboardBoard();
        board.initializeBoard();
        int iterations = 100000;
        long start = System.nanoTime();
        for(int i = 0 ; i < iterations ; i++){
            Zobrist.computeHash(board, "white");
        }
        long end = System.nanoTime();
        long fullRecomputeMs = (end - start) / 1_000_000;
        double fullRecomputeSec = fullRecomputeMs / 1000.0;
        double recomputePerMs = iterations / (double)fullRecomputeMs;
        System.out.printf("Full hash recompute: %d iterations in %.3fs (%.0f hashes/ms)%n",
                iterations, fullRecomputeSec, recomputePerMs);
        start = System.nanoTime();
        for(int i = 0 ; i < iterations ; i++){
            board.makeMove(12, 28, -1, "white");
            long h = board.getHash();
            board.undoMove();
        }
        end = System.nanoTime();
        long incrementalMs = (end - start) / 1_000_000;
        double incrementalSec = incrementalMs / 1000.0;
        double incrementalPerMs = iterations / (double)incrementalMs;
        System.out.printf("Incremental update:  %d iterations in %.3fs (%.0f updates/ms)%n",
                iterations, incrementalSec, incrementalPerMs);
        double speedup = fullRecomputeMs / (double)incrementalMs;
        System.out.printf("Speedup: %.1fx faster with incremental update%n", speedup);
        System.out.println();
    }
    static void benchmarkIncrementalHash() throws Exception{
        System.out.println("--- INCREMENTAL HASH CORRECTNESS ---");
        BitboardBoard board = new BitboardBoard();
        board.initializeBoard();
        String[][] moves = {
            {"e2","e4"},{"e7","e5"},{"g1","f3"},{"b8","c6"},
            {"f1","c4"},{"g8","f6"},{"d2","d3"},{"f8","c5"},
            {"c1","g5"},{"h7","h6"},{"g5","f6"},{"d8","f6"}
        };
        String player = "white";
        boolean allMatch = true;
        for(String[] move : moves){
            int fromCol = move[0].charAt(0) - 'a';
            int fromRow = move[0].charAt(1) - '1';
            int toCol = move[1].charAt(0) - 'a';
            int toRow = move[1].charAt(1) - '1';
            int from = fromRow * 8 + fromCol;
            int to = toRow * 8 + toCol;
            int promotion = -1;
            Piece p = board.getPiece(fromRow, fromCol);
            if(p instanceof Pawn){
                int promoRow = player.equals("white") ? 7 : 0;
                if(toRow == promoRow) promotion = BitboardBoard.QUEEN;
            }
            board.makeMove(from, to, promotion, player);
            long incremental = board.getHash();
            String nextPlayer = player.equals("white") ? "black" : "white";
            long recomputed = Zobrist.computeHash(board, nextPlayer);
            if(incremental != recomputed){
                System.out.println("[MISMATCH] After " + move[0] + move[1] +
                        " incremental=" + Long.toHexString(incremental) +
                        " recomputed=" + Long.toHexString(recomputed));
                allMatch = false;
            }
            player = nextPlayer;
        }
        if(allMatch){
            System.out.println("All incremental hashes match full recomputation after 12 moves");
        }
        System.out.println();
    }
    static int searchWithoutTT(BitboardBoard board, int depth, String color, int alpha, int beta){
        nodesSearched++;
        if(depth == 0){
            return evaluate(board, color);
        }
        if(board.isCheckmate(color)){
            return -99999 + (100 - depth);
        }
        if(board.isStalemate(color)){
            return 0;
        }
        // Null move pruning — skip in zugzwang-prone positions (only pawns + king)
        if(depth >= 3 && !board.isKingInCheck(color) && hasNonPawnMaterial(board, color)){
            String opp = color.equals("white") ? "black" : "white";
            int savedEp = board.enPassantSquare;
            int savedSide = board.sideToMove;
            int oppColorInt = savedSide ^ 1;
            if(savedEp >= 0){
                board.hash ^= Zobrist.enPassantKeys[savedEp % 8];
            }
            board.enPassantSquare = -1;
            board.hash ^= Zobrist.sideKey;
            board.sideToMove = oppColorInt;
            int nullScore = -searchWithoutTT(board, depth - 3, opp, -beta, -beta + 1);
            board.sideToMove = savedSide;
            board.hash ^= Zobrist.sideKey;
            board.enPassantSquare = savedEp;
            if(savedEp >= 0){
                board.hash ^= Zobrist.enPassantKeys[savedEp % 8];
            }
            if(nullScore >= beta){
                return beta;
            }
        }
        int bestScore = Integer.MIN_VALUE;
        int origAlpha = alpha;
        List<BitMove> moves = board.generateLegalMoves(color);
        sortMoves(moves, depth, false, -1, -1, killerNoTT, historyNoTT);
        for(int i = 0; i < moves.size(); i++){
            BitMove move = moves.get(i);
            board.makeMove(move.from(), move.to(), move.promotion(), color);
            boolean inCheck = board.isKingInCheck(color);
            if(!inCheck){
                String opp = color.equals("white") ? "black" : "white";
                int score;
                if(depth >= 3 && i >= 4 && move.capturedPiece() < 0){
                    int R = 1 + Math.min((i - 4) / 4, 2);
                    score = -searchWithoutTT(board, depth - 1 - R, opp, -beta, -alpha);
                    if(score > alpha){
                        score = -searchWithoutTT(board, depth - 1, opp, -beta, -alpha);
                    }
                } else {
                    score = -searchWithoutTT(board, depth - 1, opp, -beta, -alpha);
                }
                bestScore = Math.max(bestScore, score);
                alpha = Math.max(alpha, bestScore);
                if(alpha >= beta){
                    if(move.capturedPiece() < 0 && depth < killerNoTT.length){
                        int moveKey = move.from() * 64 + move.to();
                        if(killerNoTT[depth][0] != moveKey){
                            killerNoTT[depth][1] = killerNoTT[depth][0];
                            killerNoTT[depth][0] = moveKey;
                        }
                        historyNoTT[move.from()][move.to()] += depth * depth;
                    }
                    board.undoMove();
                    break;
                }
            }
            board.undoMove();
        }
        return bestScore;
    }
    static int searchWithTT(BitboardBoard board, int depth, String color, int alpha, int beta, TranspositionTable tt){
        nodesSearched++;
        long key = board.getHash();
        TranspositionTable.TTEntry entry = tt.probe(key);
        boolean hasTTBestMove = false;
        int ttFrom = -1, ttTo = -1;
        if(entry != null && entry.depth >= depth){
            ttHits++;
            if(entry.flag == TranspositionTable.EXACT){
                return entry.score;
            } else if(entry.flag == TranspositionTable.LOWERBOUND){
                alpha = Math.max(alpha, entry.score);
            } else if(entry.flag == TranspositionTable.UPPERBOUND){
                beta = Math.min(beta, entry.score);
            }
            if(alpha >= beta){
                return entry.score;
            }
            if(entry.hasBestMove){
                hasTTBestMove = true;
                ttFrom = entry.fromRow * 8 + entry.fromCol;
                ttTo = entry.toRow * 8 + entry.toCol;
            }
        } else {
            ttMisses++;
        }
        if(depth == 0){
            return evaluate(board, color);
        }
        if(board.isCheckmate(color)){
            return -99999 + (100 - depth);
        }
        if(board.isStalemate(color)){
            return 0;
        }
        // Null move pruning — skip in zugzwang-prone positions (only pawns + king)
        if(depth >= 3 && !board.isKingInCheck(color) && hasNonPawnMaterial(board, color)){
            String opp = color.equals("white") ? "black" : "white";
            int savedEp = board.enPassantSquare;
            int savedSide = board.sideToMove;
            int oppColorInt = savedSide ^ 1;
            if(savedEp >= 0){
                board.hash ^= Zobrist.enPassantKeys[savedEp % 8];
            }
            board.enPassantSquare = -1;
            board.hash ^= Zobrist.sideKey;
            board.sideToMove = oppColorInt;
            int nullScore = -searchWithTT(board, depth - 3, opp, -beta, -beta + 1, tt);
            board.sideToMove = savedSide;
            board.hash ^= Zobrist.sideKey;
            board.enPassantSquare = savedEp;
            if(savedEp >= 0){
                board.hash ^= Zobrist.enPassantKeys[savedEp % 8];
            }
            if(nullScore >= beta){
                return beta;
            }
        }
        int bestScore = Integer.MIN_VALUE;
        int bestFrom = -1, bestTo = -1;
        boolean hasMoves = false;
        int origAlpha = alpha;
        List<BitMove> moves = board.generateLegalMoves(color);
        sortMoves(moves, depth, hasTTBestMove, ttFrom, ttTo, killerWithTT, historyWithTT);
        for(int i = 0; i < moves.size(); i++){
            BitMove move = moves.get(i);
            board.makeMove(move.from(), move.to(), move.promotion(), color);
            boolean inCheck = board.isKingInCheck(color);
            if(!inCheck){
                hasMoves = true;
                String opp = color.equals("white") ? "black" : "white";
                int score;
                if(depth >= 3 && i >= 4 && move.capturedPiece() < 0){
                    int R = 1 + Math.min((i - 4) / 4, 2);
                    score = -searchWithTT(board, depth - 1 - R, opp, -beta, -alpha, tt);
                    if(score > alpha){
                        score = -searchWithTT(board, depth - 1, opp, -beta, -alpha, tt);
                    }
                } else {
                    score = -searchWithTT(board, depth - 1, opp, -beta, -alpha, tt);
                }
                if(score > bestScore){
                    bestScore = score;
                    bestFrom = move.from();
                    bestTo = move.to();
                }
                alpha = Math.max(alpha, bestScore);
                if(alpha >= beta){
                    if(move.capturedPiece() < 0 && depth < killerWithTT.length){
                        int moveKey = move.from() * 64 + move.to();
                        if(killerWithTT[depth][0] != moveKey){
                            killerWithTT[depth][1] = killerWithTT[depth][0];
                            killerWithTT[depth][0] = moveKey;
                        }
                        historyWithTT[move.from()][move.to()] += depth * depth;
                    }
                    board.undoMove();
                    break;
                }
            }
            board.undoMove();
        }
        if(!hasMoves) return 0;
        int flag = TranspositionTable.EXACT;
        if(bestScore <= origAlpha){
            flag = TranspositionTable.UPPERBOUND;
        } else if(bestScore >= beta){
            flag = TranspositionTable.LOWERBOUND;
        }
        tt.store(key, bestScore, depth, flag, bestFrom / 8, bestFrom % 8, bestTo / 8, bestTo % 8);
        return bestScore;
    }
    static int evaluate(BitboardBoard board){
        int score = 0;
        for(int c = 0; c < 2; c++){
            for(int type = 0; type < 6; type++){
                long bb = board.pieces[c][type];
                while(bb != 0){
                    int sq = Long.numberOfTrailingZeros(bb);
                    bb &= bb - 1;
                    int value = getPieceValue(type);
                    int bonus = getPSQT(type, sq, c);
                    if(c == BitboardBoard.WHITE) score += value + bonus;
                    else score -= value + bonus;
                }
            }
        }
        return score;
    }
    static int evaluate(BitboardBoard b, String color){
        int score = evaluate(b);
        return color.equals("white") ? score : -score;
    }
    static int getPieceValue(int type){
        return switch(type){
            case BitboardBoard.PAWN -> 100;
            case BitboardBoard.KNIGHT -> 320;
            case BitboardBoard.BISHOP -> 330;
            case BitboardBoard.ROOK -> 500;
            case BitboardBoard.QUEEN -> 900;
            case BitboardBoard.KING -> 20000;
            default -> 0;
        };
    }
    static boolean hasNonPawnMaterial(BitboardBoard b, String color){
        int c = color.equals("white") ? BitboardBoard.WHITE : BitboardBoard.BLACK;
        long nonPawn = b.pieces[c][BitboardBoard.KNIGHT]
                     | b.pieces[c][BitboardBoard.BISHOP]
                     | b.pieces[c][BitboardBoard.ROOK]
                     | b.pieces[c][BitboardBoard.QUEEN];
        return nonPawn != 0;
    }
    public static int getPSQT(int type, int sq, int color){
        int idx = (color == BitboardBoard.WHITE) ? sq : (sq ^ 56);
        return switch(type){
            case BitboardBoard.PAWN -> PAWN_PSQT[idx];
            case BitboardBoard.KNIGHT -> KNIGHT_PSQT[idx];
            case BitboardBoard.BISHOP -> BISHOP_PSQT[idx];
            case BitboardBoard.ROOK -> ROOK_PSQT[idx];
            case BitboardBoard.QUEEN -> QUEEN_PSQT[idx];
            case BitboardBoard.KING -> KING_PSQT[idx];
            default -> 0;
        };
    }
    public static void sortMoves(List<BitMove> moves, int depth, boolean hasTTBestMove, int ttFrom, int ttTo, int[][] killers, int[][] history){
        int[] scores = new int[moves.size()];
        for(int i = 0; i < moves.size(); i++){
            scores[i] = scoreMove(moves.get(i), depth, hasTTBestMove, ttFrom, ttTo, killers, history);
        }
        for(int i = 1; i < moves.size(); i++){
            BitMove key = moves.get(i);
            int keyScore = scores[i];
            int j = i - 1;
            while(j >= 0 && scores[j] < keyScore){
                moves.set(j + 1, moves.get(j));
                scores[j + 1] = scores[j];
                j--;
            }
            moves.set(j + 1, key);
            scores[j + 1] = keyScore;
        }
    }
    public static int scoreMove(BitMove move, int depth, boolean hasTTBestMove, int ttFrom, int ttTo, int[][] killers, int[][] history){
        if(hasTTBestMove && move.from() == ttFrom && move.to() == ttTo){
            return 10_000_000;
        }
        int captured = move.capturedPiece();
        if(captured >= 0){
            return 1_000_000 + getPieceValue(captured) * 100 - getPieceValue(move.movedPiece());
        }
        int moveKey = move.from() * 64 + move.to();
        if(depth < killers.length){
            for(int i = 0; i < 2; i++){
                if(killers[depth][i] == moveKey){
                    return 500_000 - i * 1000;
                }
            }
        }
        return history[move.from()][move.to()];
    }
}
