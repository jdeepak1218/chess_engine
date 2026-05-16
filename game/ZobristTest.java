package game;
import pieces.*;
public class ZobristTest{
    private static int passed = 0;
    private static int failed = 0;
    public static void main(String[] args) throws Exception{
        System.out.println("=== ZOBRIST HASHING TESTS ===\n");
        testInitialHashConsistency();
        testHashChangesAfterMove();
        testIncrementalVsFullRecompute();
        testHashAfterUndo();
        testDifferentPositionsDifferentHashes();
        testEnPassantHash();
        testCastlingRightsHash();
        testSideToMoveHash();
        testPawnMoveHash();
        testCaptureHash();
        testSymmetricPositions();
        System.out.println("\n=== RESULTS ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Total:  " + (passed + failed));
        if(failed > 0){
            System.exit(1);
        }
    }
    static void testInitialHashConsistency() throws Exception{
        BitboardBoard b1 = new BitboardBoard();
        b1.initializeBoard();
        BitboardBoard b2 = new BitboardBoard();
        b2.initializeBoard();
        assertEqual("Initial hash consistency", b1.getHash(), b2.getHash());
    }
    static void testHashChangesAfterMove() throws Exception{
        BitboardBoard board = new BitboardBoard();
        board.initializeBoard();
        long hashBefore = board.getHash();
        board.makeMove(12, 28, -1, "white");
        assertNotEqual("Hash changes after e2-e4", hashBefore, board.getHash());
    }
    static void testIncrementalVsFullRecompute() throws Exception{
        BitboardBoard board = new BitboardBoard();
        board.initializeBoard();
        String[][] moves = {
            {"e2","e4"},{"e7","e5"},{"g1","f3"},{"b8","c6"},
            {"f1","b5"},{"g8","f6"},{"e1","g1"},{"f8","e7"}
        };
        String player = "white";
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
            long incrementalHash = board.getHash();
            String nextPlayer = player.equals("white") ? "black" : "white";
            long recomputedHash = Zobrist.computeHash(board, nextPlayer);
            assertEqual("Incremental vs recompute after " + move[0] + move[1], incrementalHash, recomputedHash);
            player = nextPlayer;
        }
    }
    static void testHashAfterUndo() throws Exception{
        BitboardBoard board = new BitboardBoard();
        board.initializeBoard();
        long initialHash = board.getHash();
        board.makeMove(12, 28, -1, "white");
        board.undoMove();
        assertEqual("Hash restored after undo", initialHash, board.getHash());
    }
    static void testDifferentPositionsDifferentHashes() throws Exception{
        BitboardBoard b1 = new BitboardBoard();
        b1.initializeBoard();
        b1.makeMove(12, 28, -1, "white");
        BitboardBoard b2 = new BitboardBoard();
        b2.initializeBoard();
        b2.makeMove(11, 27, -1, "white");
        assertNotEqual("e2-e4 vs d2-d4 produce different hashes", b1.getHash(), b2.getHash());
    }
    static void testEnPassantHash() throws Exception{
        BitboardBoard board = new BitboardBoard();
        board.initializeBoard();
        board.makeMove(12, 28, -1, "white");
        long hashBeforeEP = board.getHash();
        board.makeMove(52, 36, -1, "black");
        long hashAfterEP = board.getHash();
        assertNotEqual("En passant opportunity changes hash", hashBeforeEP, hashAfterEP);
        BitboardBoard board2 = new BitboardBoard();
        board2.initializeBoard();
        board2.makeMove(12, 28, -1, "white");
        board2.makeMove(51, 35, -1, "black");
        assertNotEqual("EP on different files produces different hash", hashAfterEP, board2.getHash());
    }
    static void testCastlingRightsHash() throws Exception{
        BitboardBoard board = new BitboardBoard();
        board.initializeBoard();
        long initialHash = board.getHash();
        board.makeMove(4, 5, -1, "white");
        board.undoMove();
        long afterUndoHash = board.getHash();
        assertEqual("Castling rights preserved after king move+undo", initialHash, afterUndoHash);
    }
    static void testSideToMoveHash() throws Exception{
        BitboardBoard b1 = new BitboardBoard();
        b1.initializeBoard();
        BitboardBoard b2 = new BitboardBoard();
        b2.initializeBoard();
        b2.makeMove(12, 28, -1, "white");
        assertNotEqual("Side to move affects hash", b1.getHash(), b2.getHash());
    }
    static void testPawnMoveHash() throws Exception{
        BitboardBoard board = new BitboardBoard();
        board.initializeBoard();
        long h1 = board.getHash();
        board.makeMove(12, 20, -1, "white");
        long h2 = board.getHash();
        board.undoMove();
        board.makeMove(12, 28, -1, "white");
        long h3 = board.getHash();
        assertNotEqual("One step vs two step pawn move different hash", h2, h3);
    }
    static void testCaptureHash() throws Exception{
        BitboardBoard board = new BitboardBoard();
        board.initializeBoard();
        board.makeMove(12, 28, -1, "white");
        board.makeMove(51, 35, -1, "black");
        board.makeMove(28, 35, -1, "white");
        long captureHash = board.getHash();
        BitboardBoard board2 = new BitboardBoard();
        board2.initializeBoard();
        board2.makeMove(12, 28, -1, "white");
        board2.makeMove(51, 34, -1, "black");
        board2.makeMove(28, 35, -1, "white");
        assertNotEqual("Capture vs non-capture different hash", captureHash, board2.getHash());
    }
    static void testSymmetricPositions() throws Exception{
        BitboardBoard b1 = new BitboardBoard();
        b1.initializeBoard();
        b1.makeMove(12, 28, -1, "white");
        BitboardBoard b2 = new BitboardBoard();
        b2.initializeBoard();
        b2.makeMove(52, 36, -1, "black");
        assertNotEqual("Symmetric pawn pushes produce different hashes", b1.getHash(), b2.getHash());
    }
    static void assertEqual(String name,long expected,long actual){
        if(expected == actual){
            System.out.println("[PASS] " + name);
            passed++;
        } else {
            System.out.println("[FAIL] " + name);
            System.out.println("  Expected: " + Long.toHexString(expected));
            System.out.println("  Actual:   " + Long.toHexString(actual));
            failed++;
        }
    }
    static void assertNotEqual(String name,long a,long b){
        if(a != b){
            System.out.println("[PASS] " + name);
            passed++;
        } else {
            System.out.println("[FAIL] " + name + " (values were equal)");
            failed++;
        }
    }
}
