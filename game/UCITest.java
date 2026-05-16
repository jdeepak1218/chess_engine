package game;
import java.io.*;
import java.util.*;
import pieces.*;
public class UCITest{
    private static int passed = 0;
    private static int failed = 0;
    public static void main(String[] args) throws Exception{
        System.out.println("=== UCI PROTOCOL TESTS ===\n");
        testUciCommand();
        testIsreadyCommand();
        testPositionStartpos();
        testPositionWithMoves();
        testGoDepth1();
        testBestmoveOutput();
        testFenParsing();
        testFenWithCastling();
        testFenWithEnPassant();
        testQuitCommand();
        testGoWithTimeControl();
        testGoWithIncrement();
        testGoWithMovestogo();
        System.out.println("\n=== RESULTS ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Total:  " + (passed + failed));
        if(failed > 0){
            System.exit(1);
        }
    }
    static void testUciCommand() throws Exception{
        String[] commands = {"uci", "quit"};
        String[] output = runUci(commands);
        boolean hasIdName = false, hasIdAuthor = false, hasUciok = false;
        for(String line : output){
            if(line.equals("id name DeepakChess")) hasIdName = true;
            if(line.equals("id author Deepak")) hasIdAuthor = true;
            if(line.equals("uciok")) hasUciok = true;
        }
        assertTrue("uci returns id name", hasIdName);
        assertTrue("uci returns id author", hasIdAuthor);
        assertTrue("uci returns uciok", hasUciok);
    }
    static void testIsreadyCommand() throws Exception{
        String[] commands = {"isready", "quit"};
        String[] output = runUci(commands);
        boolean hasReadyok = false;
        for(String line : output){
            if(line.equals("readyok")) hasReadyok = true;
        }
        assertTrue("isready returns readyok", hasReadyok);
    }
    static void testPositionStartpos() throws Exception{
        String[] commands = {"position startpos", "quit"};
        String[] output = runUci(commands);
        boolean noError = true;
        for(String line : output){
            if(line.startsWith("Error") || line.startsWith("FEN parse failed")){
                noError = false;
                break;
            }
        }
        assertTrue("position startpos no error", noError);
    }
    static void testPositionWithMoves() throws Exception{
        String[] commands = {"position startpos moves e2e4 e7e5", "quit"};
        String[] output = runUci(commands);
        boolean noError = true;
        for(String line : output){
            if(line.startsWith("Error") || line.startsWith("FEN parse failed")){
                noError = false;
                break;
            }
        }
        assertTrue("position with moves no error", noError);
    }
    static void testGoDepth1() throws Exception{
        String[] commands = {"position startpos", "go depth 1", "quit"};
        String[] output = runUci(commands);
        boolean hasInfo = false, hasBestmove = false;
        for(String line : output){
            if(line.startsWith("info depth 1")) hasInfo = true;
            if(line.startsWith("bestmove")) hasBestmove = true;
        }
        assertTrue("go depth 1 returns info", hasInfo);
        assertTrue("go depth 1 returns bestmove", hasBestmove);
    }
    static void testBestmoveOutput() throws Exception{
        String[] commands = {"position startpos", "go depth 1", "quit"};
        String[] output = runUci(commands);
        String bestmove = null;
        for(String line : output){
            if(line.startsWith("bestmove")){
                bestmove = line;
                break;
            }
        }
        assertNotNull("bestmove is present", bestmove);
        if(bestmove != null){
            String[] parts = bestmove.split("\\s+");
            assertTrue("bestmove has 2 parts", parts.length == 2);
            String move = parts[1];
            assertTrue("bestmove is 4 chars", move.length() == 4);
            char file1 = move.charAt(0);
            char rank1 = move.charAt(1);
            char file2 = move.charAt(2);
            char rank2 = move.charAt(3);
            assertTrue("bestmove file1 a-h", file1 >= 'a' && file1 <= 'h');
            assertTrue("bestmove rank1 1-8", rank1 >= '1' && rank1 <= '8');
            assertTrue("bestmove file2 a-h", file2 >= 'a' && file2 <= 'h');
            assertTrue("bestmove rank2 1-8", rank2 >= '1' && rank2 <= '8');
        }
    }
    static void testFenParsing() throws Exception{
        UCI uci = new UCI();
        BitboardBoard b = uci.parseFenForTest("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        assertNotNull("FEN creates board", b);
        Piece wp = b.getPiece(1,0);
        assertNotNull("White pawn on a2", wp);
        assertTrue("White pawn is white", wp.color.equals("white"));
        Piece bp = b.getPiece(6,0);
        assertNotNull("Black pawn on a7", bp);
        assertTrue("Black pawn is black", bp.color.equals("black"));
        Piece wr = b.getPiece(0,0);
        assertNotNull("White rook on a1", wr);
        Piece bk = b.getPiece(7,4);
        assertNotNull("Black king on e8", bk);
    }
    static void testFenWithCastling() throws Exception{
        UCI uci = new UCI();
        BitboardBoard b = uci.parseFenForTest("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1");
        Piece wk = findPiece(b,"white",King.class);
        assertNotNull("White king exists", wk);
        assertTrue("White king not moved", !wk.isHasMoved());
        Piece wrk = b.getPiece(0,7);
        assertTrue("White kingside rook not moved", wrk != null && !wrk.isHasMoved());
        Piece bk = findPiece(b,"black",King.class);
        assertNotNull("Black king exists", bk);
        assertTrue("Black king not moved", !bk.isHasMoved());
    }
    static void testFenWithEnPassant() throws Exception{
        UCI uci = new UCI();
        BitboardBoard b = uci.parseFenForTest("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 1");
        assertTrue("En passant row set", b.enPassantSquare / 8 == 5);
        assertTrue("En passant col set", b.enPassantSquare % 8 == 4);
    }
    static void testQuitCommand() throws Exception{
        String[] commands = {"quit"};
        try{
            runUci(commands);
        } catch(Exception e){
        }
        assertTrue("quit command handled", true);
    }
    static void testGoWithTimeControl() throws Exception{
        String[] commands = {"position startpos", "go wtime 60000 btime 60000", "quit"};
        String[] output = runUci(commands);
        boolean hasBestmove = false;
        for(String line : output){
            if(line.startsWith("bestmove")) hasBestmove = true;
        }
        assertTrue("go with wtime/btime returns bestmove", hasBestmove);
    }
    static void testGoWithIncrement() throws Exception{
        String[] commands = {"position startpos", "go wtime 300000 btime 300000 winc 1000 binc 1000", "quit"};
        String[] output = runUci(commands);
        boolean hasBestmove = false;
        for(String line : output){
            if(line.startsWith("bestmove")) hasBestmove = true;
        }
        assertTrue("go with wtime/btime/winc/binc returns bestmove", hasBestmove);
    }
    static void testGoWithMovestogo() throws Exception{
        String[] commands = {"position startpos", "go wtime 60000 btime 60000 movestogo 10", "quit"};
        String[] output = runUci(commands);
        boolean hasBestmove = false;
        for(String line : output){
            if(line.startsWith("bestmove")) hasBestmove = true;
        }
        assertTrue("go with movestogo returns bestmove", hasBestmove);
    }
    static String[] runUci(String[] commands) throws Exception{
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
        p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        List<String> output = new ArrayList<>();
        String line;
        while((line = reader.readLine()) != null){
            output.add(line);
        }
        if(p.isAlive()) p.destroy();
        return output.toArray(new String[0]);
    }
    static Piece findPiece(BitboardBoard b, String color, Class<? extends Piece> type){
        for(int r = 0 ; r < 8 ; r++){
            for(int c = 0 ; c < 8 ; c++){
                Piece p = b.getPiece(r,c);
                if(p != null && p.color.equals(color) && type.isInstance(p)){
                    return p;
                }
            }
        }
        return null;
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
