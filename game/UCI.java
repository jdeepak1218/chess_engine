package game;
import pieces.*;
import java.util.*;
import java.io.*;
public class UCI{
    private BitboardBoard board;
    private TranspositionTable tt;
    private volatile boolean searchRunning;
    private Thread searchThread;
    private String sideToMove;
    private int[][] killerMoves;
    private int[][] history;

    // Piece-Square Tables
    private static final int[] PAWN_PSQT = {
          0,  0,  0,  0,  0,  0,  0,  0,
          5, 10, 10,-20,-20, 10, 10,  5,
          5, -5,-10,  0,  0,-10, -5,  5,
          0,  0,  0, 20, 20,  0,  0,  0,
          5,  5, 10, 25, 25, 10,  5,  5,
         10, 10, 20, 30, 30, 20, 10, 10,
         50, 50, 50, 50, 50, 50, 50, 50,
          0,  0,  0,  0,  0,  0,  0,  0
    };
    private static final int[] KNIGHT_PSQT = {
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    };
    private static final int[] BISHOP_PSQT = {
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    };
    private static final int[] ROOK_PSQT = {
          0,  0,  0,  0,  0,  0,  0,  0,
          5, 10, 10, 10, 10, 10, 10,  5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
          0,  0,  0,  5,  5,  0,  0,  0
    };
    private static final int[] QUEEN_PSQT = {
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
         -5,  0,  5,  5,  5,  5,  0, -5,
          0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    };
    private static final int[] KING_PSQT = {
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
         20, 20,  0,  0,  0,  0, 20, 20,
         20, 30, 10,  0,  0, 10, 30, 20
    };
    public UCI(){
        board = null;
        tt = new TranspositionTable(1 << 20);
        searchRunning = false;
        sideToMove = "white";
        killerMoves = new int[100][2];
        history = new int[64][64];
    }
    public static void main(String[] args){
        new UCI().loop();
    }
    public void loop(){
        Scanner sc = new Scanner(System.in);
        while(sc.hasNextLine()){
            String line = sc.nextLine().trim();
            if(line.isEmpty())continue;
            try{
                handleCommand(line);
            } catch(Exception e){
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
    private void handleCommand(String line){
        String[] parts = line.split("\\s+");
        String cmd = parts[0];
        switch(cmd){
            case "uci":
                System.out.println("id name DeepakChess");
                System.out.println("id author Deepak");
                System.out.println("uciok");
                System.out.flush();
                break;
            case "isready":
                System.out.println("readyok");
                System.out.flush();
                break;
            case "ucinewgame":
                tt.clear();
                break;
            case "position":
                handlePosition(parts);
                break;
            case "go":
                handleGo(parts);
                break;
            case "stop":
                searchRunning = false;
                break;
            case "quit":
                searchRunning = false;
                System.exit(0);
                break;
        }
    }
    private void handlePosition(String[] parts){
        int idx = 1;
        if(idx >= parts.length) return;
        if(parts[idx].equals("startpos")){
            try{
                board = new BitboardBoard();
                board.initializeBoard();
                sideToMove = "white";
                tt.clear();
            } catch(Exception e){
                System.err.println("Board init failed: " + e.getMessage());
                return;
            }
            idx++;
        } else if(parts[idx].equals("fen")){
            idx++;
            StringBuilder fen = new StringBuilder();
            while(idx < parts.length && !parts[idx].equals("moves")){
                fen.append(parts[idx]).append(" ");
                idx++;
            }
            try{
                board = parseFen(fen.toString().trim());
                String[] fenParts = fen.toString().trim().split("\\s+");
                sideToMove = fenParts.length > 1 && fenParts[1].equals("b") ? "black" : "white";
                tt.clear();
            } catch(Exception e){
                System.err.println("FEN parse failed: " + e.getMessage());
                return;
            }
        } else {
            return;
        }
        if(idx < parts.length && parts[idx].equals("moves")){
            idx++;
            while(idx < parts.length){
                String moveStr = parts[idx];
                idx++;
                int fromCol = moveStr.charAt(0) - 'a';
                int fromRow = moveStr.charAt(1) - '1';
                int toCol = moveStr.charAt(2) - 'a';
                int toRow = moveStr.charAt(3) - '1';
                int from = fromRow * 8 + fromCol;
                int to = toRow * 8 + toCol;
                if(board == null) return;
                int promotion = -1;
                if(moveStr.length() == 5){
                    char promoChar = moveStr.charAt(4);
                    promotion = switch(promoChar){
                        case 'q' -> BitboardBoard.QUEEN;
                        case 'r' -> BitboardBoard.ROOK;
                        case 'b' -> BitboardBoard.BISHOP;
                        case 'n' -> BitboardBoard.KNIGHT;
                        default -> BitboardBoard.QUEEN;
                    };
                }
                board.makeMove(from, to, promotion, sideToMove);
                sideToMove = sideToMove.equals("white") ? "black" : "white";
            }
        }
    }
    private void handleGo(String[] parts){
        if(board == null) return;
        int depth = 1;
        int movetime = 0;
        int wtime = -1, btime = -1, winc = 0, binc = 0, movestogo = 0;
        for(int i = 1 ; i < parts.length ; i++){
            if(parts[i].equals("depth") && i + 1 < parts.length){
                depth = Integer.parseInt(parts[++i]);
            } else if(parts[i].equals("movetime") && i + 1 < parts.length){
                movetime = Integer.parseInt(parts[++i]);
            } else if(parts[i].equals("wtime") && i + 1 < parts.length){
                wtime = Integer.parseInt(parts[++i]);
            } else if(parts[i].equals("btime") && i + 1 < parts.length){
                btime = Integer.parseInt(parts[++i]);
            } else if(parts[i].equals("winc") && i + 1 < parts.length){
                winc = Integer.parseInt(parts[++i]);
            } else if(parts[i].equals("binc") && i + 1 < parts.length){
                binc = Integer.parseInt(parts[++i]);
            } else if(parts[i].equals("movestogo") && i + 1 < parts.length){
                movestogo = Integer.parseInt(parts[++i]);
            } else if(parts[i].equals("infinite")){
                depth = 99;
            }
        }
        if(movetime == 0){
            movetime = calculateMoveTime(wtime, btime, winc, binc, movestogo);
        }
        final int searchDepth = depth;
        final int searchTime = movetime;
        searchRunning = true;
        searchThread = new Thread(() -> search(searchDepth, searchTime));
        searchThread.start();
    }
    private int calculateMoveTime(int wtime, int btime, int winc, int binc, int movestogo){
        int myTime = sideToMove.equals("white") ? wtime : btime;
        int myInc = sideToMove.equals("white") ? winc : binc;
        if(myTime < 0) return 0;
        int movesRemaining = movestogo > 0 ? movestogo : 40;
        int targetTime = myTime / movesRemaining + myInc;
        int safetyMargin = Math.min(2000, myTime / 5);
        targetTime = Math.min(targetTime, myTime - safetyMargin);
        return Math.max(10, targetTime);
    }
    private void search(int maxDepth, int movetime){
        long startTime = System.currentTimeMillis();
        String color = sideToMove;
        int bestFrom = -1, bestTo = -1;
        int bestScore = Integer.MIN_VALUE;            long nodes = 0;
        for(int i = 0; i < killerMoves.length; i++){ killerMoves[i][0] = 0; killerMoves[i][1] = 0; }
        for(int i = 0; i < 64; i++){ Arrays.fill(history[i], 0); }
        for(int depth = 1 ; depth <= maxDepth ; depth++){
            if(!searchRunning) break;
            if(movetime > 0 && (System.currentTimeMillis() - startTime) > movetime) break;
            int alpha = -Integer.MAX_VALUE;
            int beta = Integer.MAX_VALUE;
            int depthBestScore = Integer.MIN_VALUE;
            int dFrom = -1, dTo = -1;
            long depthNodes = 0;
            TranspositionTable.TTEntry ttEntry = tt.probe(board.getHash());
            boolean hasTTBestMove = ttEntry != null && ttEntry.hasBestMove;
            int ttFrom = -1, ttTo = -1;
            if(hasTTBestMove){
                ttFrom = ttEntry.fromRow * 8 + ttEntry.fromCol;
                ttTo = ttEntry.toRow * 8 + ttEntry.toCol;
            }
            List<BitMove> moves = board.generateLegalMoves(color);
            sortMoves(moves, 0, hasTTBestMove, ttFrom, ttTo);
            for(BitMove move : moves){
                if(!searchRunning) break;
                board.makeMove(move.from(), move.to(), move.promotion(), color);
                String opp = color.equals("white") ? "black" : "white";
                int score = -negamax(board, depth - 1, opp, -beta, -alpha);
                board.undoMove();
                depthNodes++;
                if(score > depthBestScore){
                    depthBestScore = score;
                    dFrom = move.from();
                    dTo = move.to();
                }
                alpha = Math.max(alpha, score);
            }
            nodes += depthNodes;
            long elapsed = System.currentTimeMillis() - startTime;
            if(dFrom >= 0){
                bestFrom = dFrom;
                bestTo = dTo;
                bestScore = depthBestScore;
            }
            int flag = TranspositionTable.EXACT;
            if(bestScore <= alpha) flag = TranspositionTable.UPPERBOUND;
            else if(bestScore >= beta) flag = TranspositionTable.LOWERBOUND;
            tt.store(board.getHash(), bestScore, depth, flag, bestFrom / 8, bestFrom % 8, bestTo / 8, bestTo % 8);
            System.out.println("info depth " + depth + " score cp " + bestScore + " nodes " + nodes +
                    " time " + elapsed + " pv " + toAlgebraic(bestFrom, bestTo));
            System.out.flush();
        }
        if(bestFrom >= 0){
            System.out.println("bestmove " + toAlgebraic(bestFrom, bestTo));
        } else {
            System.out.println("bestmove 0000");
        }
        System.out.flush();
    }
    private int negamax(BitboardBoard b, int depth, String color, int alpha, int beta){
        if(!searchRunning) return 0;
        TranspositionTable.TTEntry ttEntry = tt.probe(b.getHash());
        if(ttEntry != null && ttEntry.depth >= depth){
            if(ttEntry.flag == TranspositionTable.EXACT) return ttEntry.score;
            if(ttEntry.flag == TranspositionTable.LOWERBOUND && ttEntry.score > alpha) alpha = ttEntry.score;
            if(ttEntry.flag == TranspositionTable.UPPERBOUND && ttEntry.score < beta) beta = ttEntry.score;
            if(alpha >= beta) return ttEntry.score;
        }
        if(b.isCheckmate(color)){
            return -99999 + (100 - depth);
        }
        if(b.isStalemate(color)){
            return 0;
        }
        if(depth == 0){
            return quiesce(b, color, alpha, beta);
        }
        // Null move pruning — skip in zugzwang-prone positions (only pawns + king)
        if(depth >= 3 && !b.isKingInCheck(color) && hasNonPawnMaterial(b, color)){
            String opp = color.equals("white") ? "black" : "white";
            int savedEp = b.enPassantSquare;
            int savedSide = b.sideToMove;
            int oppColorInt = savedSide ^ 1;
            if(savedEp >= 0){
                b.hash ^= Zobrist.enPassantKeys[savedEp % 8];
            }
            b.enPassantSquare = -1;
            b.hash ^= Zobrist.sideKey;
            b.sideToMove = oppColorInt;
            int nullScore = -negamax(b, depth - 3, opp, -beta, -beta + 1);
            b.sideToMove = savedSide;
            b.hash ^= Zobrist.sideKey;
            b.enPassantSquare = savedEp;
            if(savedEp >= 0){
                b.hash ^= Zobrist.enPassantKeys[savedEp % 8];
            }
            if(nullScore >= beta){
                return beta;
            }
        }
        int bestScore = Integer.MIN_VALUE;
        int origAlpha = alpha;
        List<BitMove> moves = b.generateLegalMoves(color);
        boolean hasTTBestMove = ttEntry != null && ttEntry.hasBestMove;
        int ttFrom = -1, ttTo = -1;
        if(hasTTBestMove){
            ttFrom = ttEntry.fromRow * 8 + ttEntry.fromCol;
            ttTo = ttEntry.toRow * 8 + ttEntry.toCol;
        }
        sortMoves(moves, depth, hasTTBestMove, ttFrom, ttTo);
        for(int i = 0; i < moves.size(); i++){
            BitMove move = moves.get(i);
            if(!searchRunning) return alpha;
            b.makeMove(move.from(), move.to(), move.promotion(), color);
            String opp = color.equals("white") ? "black" : "white";
            int score;
            // Late Move Reduction: reduce depth for late quiet moves (not captures, past first 4)
            if(depth >= 3 && i >= 4 && move.capturedPiece() < 0){
                int R = 1 + Math.min((i - 4) / 4, 2);
                score = -negamax(b, depth - 1 - R, opp, -beta, -alpha);
                if(score > alpha){
                    score = -negamax(b, depth - 1, opp, -beta, -alpha);
                }
            } else {
                score = -negamax(b, depth - 1, opp, -beta, -alpha);
            }
            b.undoMove();
            bestScore = Math.max(bestScore, score);
            alpha = Math.max(alpha, bestScore);
            if(alpha >= beta){
                if(move.capturedPiece() < 0 && depth < killerMoves.length){
                    int moveKey = move.from() * 64 + move.to();
                    if(killerMoves[depth][0] != moveKey){
                        killerMoves[depth][1] = killerMoves[depth][0];
                        killerMoves[depth][0] = moveKey;
                    }
                    history[move.from()][move.to()] += depth * depth;
                }
                break;
            }
        }
        int flag = TranspositionTable.EXACT;
        if(bestScore <= origAlpha) flag = TranspositionTable.UPPERBOUND;
        else if(bestScore >= beta) flag = TranspositionTable.LOWERBOUND;
        tt.store(b.getHash(), bestScore, depth, flag);
        return bestScore;
    }
    private int quiesce(BitboardBoard b, String color, int alpha, int beta){
        if(!searchRunning) return 0;
        int standPat = evaluate(b, color);
        if(standPat >= beta) return beta;
        if(standPat > alpha) alpha = standPat;
        int c = color.equals("white") ? BitboardBoard.WHITE : BitboardBoard.BLACK;
        int opp = c ^ 1;
        long captures = 0L;
        long pawns = b.pieces[c][BitboardBoard.PAWN];
        while(pawns != 0){
            int from = Long.numberOfTrailingZeros(pawns);
            pawns &= pawns - 1;
            captures |= BitboardBoard.getPawnAttacksStatic(c, from) & b.allOccupancy(opp);
        }
        long knights = b.pieces[c][BitboardBoard.KNIGHT];
        while(knights != 0){
            int from = Long.numberOfTrailingZeros(knights);
            knights &= knights - 1;
            captures |= BitboardBoard.getKnightAttacksStatic(from) & b.allOccupancy(opp);
        }
        long bishops = b.pieces[c][BitboardBoard.BISHOP];
        while(bishops != 0){
            int from = Long.numberOfTrailingZeros(bishops);
            bishops &= bishops - 1;
            captures |= BitboardBoard.getBishopAttacks(from, b.allOccupancy) & b.allOccupancy(opp);
        }
        long rooks = b.pieces[c][BitboardBoard.ROOK];
        while(rooks != 0){
            int from = Long.numberOfTrailingZeros(rooks);
            rooks &= rooks - 1;
            captures |= BitboardBoard.getRookAttacks(from, b.allOccupancy) & b.allOccupancy(opp);
        }
        long queens = b.pieces[c][BitboardBoard.QUEEN];
        while(queens != 0){
            int from = Long.numberOfTrailingZeros(queens);
            queens &= queens - 1;
            captures |= BitboardBoard.getQueenAttacks(from, b.allOccupancy) & b.allOccupancy(opp);
        }
        long king = b.pieces[c][BitboardBoard.KING];
        if(king != 0){
            int from = Long.numberOfTrailingZeros(king);
            captures |= BitboardBoard.getKingAttacksStatic(from) & b.allOccupancy(opp);
        }
        while(captures != 0){
            int to = Long.numberOfTrailingZeros(captures);
            captures &= captures - 1;
            for(int from = 0; from < 64; from++){
                long fromBit = 1L << from;
                if((b.allOccupancy(c) & fromBit) == 0) continue;
                int movedType = b.getPieceTypeAt(from);
                if(movedType < 0) continue;
                boolean isCapture = false;
                if(movedType == BitboardBoard.PAWN){
                    long pawnAttacks = BitboardBoard.getPawnAttacksStatic(c, from);
                    if((pawnAttacks & (1L << to)) != 0) isCapture = true;
                } else if(movedType == BitboardBoard.KNIGHT){
                    if((BitboardBoard.getKnightAttacksStatic(from) & (1L << to)) != 0) isCapture = true;
                } else if(movedType == BitboardBoard.BISHOP){
                    if((BitboardBoard.getBishopAttacks(from, b.allOccupancy) & (1L << to)) != 0) isCapture = true;
                } else if(movedType == BitboardBoard.ROOK){
                    if((BitboardBoard.getRookAttacks(from, b.allOccupancy) & (1L << to)) != 0) isCapture = true;
                } else if(movedType == BitboardBoard.QUEEN){
                    if((BitboardBoard.getQueenAttacks(from, b.allOccupancy) & (1L << to)) != 0) isCapture = true;
                } else if(movedType == BitboardBoard.KING){
                    if((BitboardBoard.getKingAttacksStatic(from) & (1L << to)) != 0) isCapture = true;
                }
                if(!isCapture) continue;
                int promo = -1;
                if(movedType == BitboardBoard.PAWN){
                    int toRow = to / 8;
                    if(toRow == 0 || toRow == 7) promo = BitboardBoard.QUEEN;
                }
                b.makeMove(from, to, promo, color);
                boolean inCheck = b.isKingInCheck(color);
                if(!inCheck){
                    String oppColor = color.equals("white") ? "black" : "white";
                    int score = -quiesce(b, oppColor, -beta, -alpha);
                    b.undoMove();
                    if(score >= beta) return beta;
                    if(score > alpha) alpha = score;
                } else {
                    b.undoMove();
                }
            }
        }
        return alpha;
    }
    private int evaluate(BitboardBoard b, String color){
        int score = 0;
        for(int c = 0; c < 2; c++){
            for(int type = 0; type < 6; type++){
                long bb = b.pieces[c][type];
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
        return color.equals("white") ? score : -score;
    }
    private int getPieceValue(int type){
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
    private boolean hasNonPawnMaterial(BitboardBoard b, String color){
        int c = color.equals("white") ? BitboardBoard.WHITE : BitboardBoard.BLACK;
        long nonPawn = b.pieces[c][BitboardBoard.KNIGHT]
                     | b.pieces[c][BitboardBoard.BISHOP]
                     | b.pieces[c][BitboardBoard.ROOK]
                     | b.pieces[c][BitboardBoard.QUEEN];
        return nonPawn != 0;
    }
    private int getPSQT(int type, int sq, int color){
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
    private BitboardBoard parseFen(String fen) throws Exception{
        BitboardBoard b = new BitboardBoard();
        for(int c = 0; c < 2; c++){
            for(int p = 0; p < 6; p++){
                b.pieces[c][p] = 0L;
            }
        }
        b.whiteOccupancy = 0L;
        b.blackOccupancy = 0L;
        b.allOccupancy = 0L;
        b.castlingRights = 0;
        b.enPassantSquare = -1;
        b.sideToMove = BitboardBoard.WHITE;
        String[] parts = fen.split("\\s+");
        String placement = parts[0];
        String activeColor = parts.length > 1 ? parts[1] : "w";
        String castling = parts.length > 2 ? parts[2] : "-";
        String epSquare = parts.length > 3 ? parts[3] : "-";
        int row = 7;
        int col = 0;
        for(int i = 0 ; i < placement.length() ; i++){
            char ch = placement.charAt(i);
            if(ch == '/'){
                row--;
                col = 0;
            } else if(Character.isDigit(ch)){
                col += ch - '0';
            } else {
                int color = Character.isUpperCase(ch) ? BitboardBoard.WHITE : BitboardBoard.BLACK;
                int type = switch(Character.toLowerCase(ch)){
                    case 'p' -> BitboardBoard.PAWN;
                    case 'r' -> BitboardBoard.ROOK;
                    case 'n' -> BitboardBoard.KNIGHT;
                    case 'b' -> BitboardBoard.BISHOP;
                    case 'q' -> BitboardBoard.QUEEN;
                    case 'k' -> BitboardBoard.KING;
                    default -> throw new IllegalArgumentException("Unknown piece: " + ch);
                };
                int sq = row * 8 + col;
                long bit = 1L << sq;
                b.pieces[color][type] |= bit;
                if(color == BitboardBoard.WHITE) b.whiteOccupancy |= bit;
                else b.blackOccupancy |= bit;
                b.allOccupancy |= bit;
                col++;
            }
        }
        if(!castling.equals("-")){
            for(int i = 0 ; i < castling.length() ; i++){
                char ch = castling.charAt(i);
                if(ch == 'K') b.castlingRights |= 1;
                if(ch == 'Q') b.castlingRights |= 2;
                if(ch == 'k') b.castlingRights |= 4;
                if(ch == 'q') b.castlingRights |= 8;
            }
        }
        if(!epSquare.equals("-")){
            int epCol = epSquare.charAt(0) - 'a';
            int epRow = epSquare.charAt(1) - '1';
            b.enPassantSquare = epRow * 8 + epCol;
        }
        if(activeColor.equals("b")){
            b.sideToMove = BitboardBoard.BLACK;
        }
        b.hash = Zobrist.computeHash(b, b.sideToMove == BitboardBoard.WHITE ? "white" : "black");
        return b;
    }
    private String toAlgebraic(int from, int to){
        char fromFile = (char)('a' + (from % 8));
        char fromRank = (char)('1' + (from / 8));
        char toFile = (char)('a' + (to % 8));
        char toRank = (char)('1' + (to / 8));
        return "" + fromFile + fromRank + toFile + toRank;
    }
    private void sortMoves(List<BitMove> moves, int depth, boolean hasTTBestMove, int ttFrom, int ttTo){
        int[] scores = new int[moves.size()];
        for(int i = 0; i < moves.size(); i++){
            scores[i] = scoreMove(moves.get(i), depth, hasTTBestMove, ttFrom, ttTo);
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
    private int scoreMove(BitMove move, int depth, boolean hasTTBestMove, int ttFrom, int ttTo){
        if(hasTTBestMove && move.from() == ttFrom && move.to() == ttTo){
            return 10_000_000;
        }
        int captured = move.capturedPiece();
        if(captured >= 0){
            return 1_000_000 + getPieceValue(captured) * 100 - getPieceValue(move.movedPiece());
        }
        int moveKey = move.from() * 64 + move.to();
        if(depth < killerMoves.length){
            for(int i = 0; i < 2; i++){
                if(killerMoves[depth][i] == moveKey){
                    return 500_000 - i * 1000;
                }
            }
        }
        return history[move.from()][move.to()];
    }
    public BitboardBoard parseFenForTest(String fen) throws Exception{
        return parseFen(fen);
    }
    public TranspositionTable getTT(){ return tt; }
    public int quiesceForTest(BitboardBoard b, String color, int alpha, int beta){
        boolean prevSearchRunning = searchRunning;
        searchRunning = true;
        int result = quiesce(b, color, alpha, beta);
        searchRunning = prevSearchRunning;
        return result;
    }
    public int evaluateForTest(BitboardBoard b, String color){
        return evaluate(b, color);
    }
    public int negamaxForTest(BitboardBoard b, int depth, String color, int alpha, int beta){
        boolean prevSearchRunning = searchRunning;
        searchRunning = true;
        int result = negamax(b, depth, color, alpha, beta);
        searchRunning = prevSearchRunning;
        return result;
    }
    public String getSideToMove(){ return sideToMove; }
    public void processPositionForTest(String positionStr){
        String[] parts = ("position " + positionStr).split("\\s+");
        handlePosition(parts);
    }
}
