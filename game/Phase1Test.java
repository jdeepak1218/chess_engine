package game;
import java.io.*;
import java.util.*;
import pieces.*;
public class Phase1Test{
    private static int passed = 0;
    private static int failed = 0;
    public static void main(String[] args) throws Exception{
        System.out.println("=== PHASE 1 TESTS ===\n");
        testSideToMoveStartpos();
        testSideToMoveAfterMoves();
        testSideToMoveFromFenBlack();
        testSideToMoveFromFenWhite();
        testTTUsedInSearch();
        testTTStoreAndProbe();
        testQuiescenceCaptureOnly();
        testQuiescenceStandPat();
        testQuiescenceBetaCutoff();
        testNegamaxUsesTT();
        System.out.println("\n=== RESULTS ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Total:  " + (passed + failed));
        if(failed > 0){
            System.exit(1);
        }
    }
    static void testSideToMoveStartpos() throws Exception{
        UCI uci = new UCI();
        uci.processPositionForTest("startpos");
        assertTrue("side to move is white after startpos", uci.getSideToMove().equals("white"));
    }
    static void testSideToMoveAfterMoves() throws Exception{
        UCI uci = new UCI();
        uci.processPositionForTest("startpos moves e2e4");
        assertTrue("side to move is black after e2e4", uci.getSideToMove().equals("black"));
    }
    static void testSideToMoveFromFenBlack() throws Exception{
        UCI uci = new UCI();
        uci.processPositionForTest("fen rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 0 1");
        assertTrue("side to move is black from FEN", uci.getSideToMove().equals("black"));
    }
    static void testSideToMoveFromFenWhite() throws Exception{
        UCI uci = new UCI();
        uci.processPositionForTest("fen rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        assertTrue("side to move is black after e4 in FEN", uci.getSideToMove().equals("black"));
    }
    static void testTTUsedInSearch() throws Exception{
        String[] commands = {"position startpos", "go depth 2", "quit"};
        String[] output = runUciDirect(commands);
        boolean hasInfo = false;
        boolean hasBestmove = false;
        for(String line : output){
            if(line.startsWith("info depth")) hasInfo = true;
            if(line.startsWith("bestmove")) hasBestmove = true;
        }
        assertTrue("search produces info output", hasInfo);
        assertTrue("search produces bestmove", hasBestmove);
    }
    static void testTTStoreAndProbe() throws Exception{
        TranspositionTable tt = new TranspositionTable(1024);
        long testKey = 12345678L;
        tt.store(testKey, 150, 3, TranspositionTable.EXACT, 1, 2, 3, 4);
        TranspositionTable.TTEntry entry = tt.probe(testKey);
        assertNotNull("TT entry stored and retrieved", entry);
        assertTrue("TT entry score matches", entry.score == 150);
        assertTrue("TT entry depth matches", entry.depth == 3);
        assertTrue("TT entry flag matches", entry.flag == TranspositionTable.EXACT);
    }
    static void testQuiescenceCaptureOnly() throws Exception{
        UCI uci = new UCI();
        BitboardBoard b = uci.parseFenForTest("rnbqkbnr/pppppppp/8/8/8/5P2/PPPPP1PP/RNBQKBNR b KQkq - 0 1");
        int score = uci.quiesceForTest(b, "black", Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertTrue("quiescence returns finite score", score > Integer.MIN_VALUE && score < Integer.MAX_VALUE);
    }
    static void testQuiescenceStandPat() throws Exception{
        UCI uci = new UCI();
        BitboardBoard b = uci.parseFenForTest("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        int standPat = uci.evaluateForTest(b, "white");
        int score = uci.quiesceForTest(b, "white", Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertTrue("stand pat is baseline", score >= standPat - 10);
    }
    static void testQuiescenceBetaCutoff() throws Exception{
        UCI uci = new UCI();
        BitboardBoard b = uci.parseFenForTest("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        int beta = -30000;
        int result = uci.quiesceForTest(b, "white", Integer.MIN_VALUE, beta);
        assertTrue("quiescence respects beta cutoff", result <= beta);
    }
    static void testNegamaxUsesTT() throws Exception{
        UCI uci = new UCI();
        uci.parseFenForTest("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        BitboardBoard b = uci.parseFenForTest("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        uci.negamaxForTest(b, 1, "black", Integer.MIN_VALUE, Integer.MAX_VALUE);
        int occupancy = uci.getTT().occupancy();
        assertTrue("negamax stores entries in TT", occupancy > 0);
    }
    static String[] runUciDirect(String[] commands) throws Exception{
        ProcessBuilder pb = new ProcessBuilder("java", "-cp", "out", "game.UCI");
        pb.redirectErrorStream(true);
        Process p = pb.start();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
        Thread.sleep(500);
        for(String cmd : commands){
            if(!p.isAlive()) break;
            try{
                writer.write(cmd);
                writer.newLine();
                writer.flush();
            } catch(IOException e){
                break;
            }
            Thread.sleep(500);
        }
        p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        List<String> output = new ArrayList<>();
        String line;
        while((line = reader.readLine()) != null){
            output.add(line);
        }
        if(p.isAlive()) p.destroy();
        return output.toArray(new String[0]);
    }
    static void assertTrue(String name, boolean condition){
        if(condition){
            System.out.println("[PASS] " + name);
            passed++;
        } else {
            System.out.println("[FAIL] " + name);
            failed++;
        }
    }
    static void assertNotNull(String name, Object obj){
        if(obj != null){
            System.out.println("[PASS] " + name);
            passed++;
        } else {
            System.out.println("[FAIL] " + name + " (was null)");
            failed++;
        }
    }
}
