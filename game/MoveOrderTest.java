package game;
import pieces.*;
import java.util.*;
public class MoveOrderTest{
    public static void main(String[] args) throws Exception{
        // Position where white has a winning capture: Qxd7+ (queen takes undefended knight)
        // FEN: r1bqkb1r/ppppNppp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNBQK2R w KQkq - 0 1
        // White knight on e7 attacks black queen, but let's use a simpler one
        // Where black is up a rook so eval should show it
        
        // Let's test with a position with clear tactical win
        // Scholar's mate position: White can checkmate with Qxf7#
        // After: 1. e4 e5 2. Bc4 Nc6 3. Qh5 Nf6?? 4. Qxf7#
        String fen = "r1bqkb1r/pppp1Qpp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 4";
        
        BitboardBoard board = new BitboardBoard();
        board = parseFen(board, fen);
        
        System.out.println("=== TEST POSITION: White to move with Qxf7# ===");
        System.out.println();
        
        // Without move ordering (original approach, no sorting)
        BenchmarkNoSort.nodesSearched = 0;
        long start = System.nanoTime();
        int scoreNoSort = BenchmarkNoSort.search(board, 4, "white", -Integer.MAX_VALUE, Integer.MAX_VALUE);
        long end = System.nanoTime();
        long timeNoSort = (end - start) / 1_000_000;
        System.out.printf("Without ordering: Score=%d, Nodes=%d, Time=%dms%n", 
            scoreNoSort, BenchmarkNoSort.nodesSearched, timeNoSort);
        
        // With move ordering
        Benchmark.nodesSearched = 0;
        for(int i = 0; i < 64; i++) Arrays.fill(Benchmark.historyNoTT[i], 0);
        for(int i = 0; i < 100; i++){ Benchmark.killerNoTT[i][0] = 0; Benchmark.killerNoTT[i][1] = 0; }
        start = System.nanoTime();
        int scoreOrdered = Benchmark.searchWithoutTT(board, 4, "white", -Integer.MAX_VALUE, Integer.MAX_VALUE);
        end = System.nanoTime();
        long timeOrdered = (end - start) / 1_000_000;
        System.out.printf("With ordering:    Score=%d, Nodes=%d, Time=%dms%n", 
            scoreOrdered, Benchmark.nodesSearched, timeOrdered);
        
        double reduction = (1 - (double)Benchmark.nodesSearched / BenchmarkNoSort.nodesSearched) * 100;
        System.out.printf("Node reduction: %.1f%%%n", reduction);
    }
    
    static BitboardBoard parseFen(BitboardBoard b, String fen) throws Exception{
        for(int c = 0; c < 2; c++) for(int p = 0; p < 6; p++) b.pieces[c][p] = 0L;
        b.whiteOccupancy = 0L; b.blackOccupancy = 0L; b.allOccupancy = 0L;
        b.castlingRights = 0; b.enPassantSquare = -1; b.sideToMove = BitboardBoard.WHITE;
        String[] parts = fen.split(" ");
        String placement = parts[0]; String activeColor = parts.length > 1 ? parts[1] : "w";
        String castling = parts.length > 2 ? parts[2] : "-";
        String epSquare = parts.length > 3 ? parts[3] : "-";
        int row = 7, col = 0;
        for(int i = 0; i < placement.length(); i++){
            char ch = placement.charAt(i);
            if(ch == '/'){ row--; col = 0; }
            else if(Character.isDigit(ch)){ col += ch - '0'; }
            else {
                int color = Character.isUpperCase(ch) ? BitboardBoard.WHITE : BitboardBoard.BLACK;
                int type = switch(Character.toLowerCase(ch)){
                    case 'p' -> BitboardBoard.PAWN; case 'r' -> BitboardBoard.ROOK;
                    case 'n' -> BitboardBoard.KNIGHT; case 'b' -> BitboardBoard.BISHOP;
                    case 'q' -> BitboardBoard.QUEEN; case 'k' -> BitboardBoard.KING;
                    default -> -1;
                };
                int sq = row * 8 + col; long bit = 1L << sq;
                b.pieces[color][type] |= bit;
                if(color == BitboardBoard.WHITE) b.whiteOccupancy |= bit; else b.blackOccupancy |= bit;
                b.allOccupancy |= bit; col++;
            }
        }
        b.hash = Zobrist.computeHash(b, b.sideToMove == BitboardBoard.WHITE ? "white" : "black");
        return b;
    }
}

class BenchmarkNoSort{
    static long nodesSearched;
    static int search(BitboardBoard board, int depth, String color, int alpha, int beta){
        nodesSearched++;
        if(depth == 0) return evaluate(board, color);
        if(board.isCheckmate(color)) return -99999 + (100 - depth);
        if(board.isStalemate(color)) return 0;
        int bestScore = Integer.MIN_VALUE;
        List<BitMove> moves = board.generateLegalMoves(color);
        for(BitMove move : moves){
            board.makeMove(move.from(), move.to(), move.promotion(), color);
            boolean inCheck = board.isKingInCheck(color);
            if(!inCheck){
                String opp = color.equals("white") ? "black" : "white";
                int score = -search(board, depth - 1, opp, -beta, -alpha);
                bestScore = Math.max(bestScore, score);
            }
            board.undoMove();
        }
        return bestScore;
    }
    static int evaluate(BitboardBoard b, String color){
        int score = 0;
        for(int c = 0; c < 2; c++){
            for(int type = 0; type < 6; type++){
                long bb = b.pieces[c][type];
                while(bb != 0){
                    int sq = Long.numberOfTrailingZeros(bb);
                    bb &= bb - 1;
                    int value = switch(type){
                        case BitboardBoard.PAWN -> 100; case BitboardBoard.KNIGHT -> 320;
                        case BitboardBoard.BISHOP -> 330; case BitboardBoard.ROOK -> 500;
                        case BitboardBoard.QUEEN -> 900; case BitboardBoard.KING -> 20000;
                        default -> 0;
                    };
                    if(c == BitboardBoard.WHITE) score += value; else score -= value;
                }
            }
        }
        return color.equals("white") ? score : -score;
    }
}
