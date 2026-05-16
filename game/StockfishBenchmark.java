package game;
import java.io.*;
import java.util.*;

public class StockfishBenchmark {
    private static final String STOCKFISH_PATH = "stockfish";

    public static void main(String[] args) throws Exception {
        System.out.println("=== DEEPKCHESS vs STOCKFISH BENCHMARK ===\n");

        String[] testPositions = {
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 0 1",
            "rnbqkb1r/pp1p1ppp/2p5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 0 1",
            "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10"
        };

        String[] positionNames = {
            "Starting position",
            "Kiwipete (complex middlegame)",
            "Italian Game opening",
            "Italian Game variation",
            "Middlegame position"
        };

        int[] depths = {3, 4, 5};

        System.out.println("Testing DeepakChess (bitboard engine):");
        System.out.println("======================================");
        for (int i = 0; i < testPositions.length; i++) {
            System.out.println("\nPosition: " + positionNames[i]);
            System.out.println("FEN: " + testPositions[i]);
            benchmarkDeepakChess(testPositions[i], depths);
        }

        System.out.println("\n\nTesting Stockfish:");
        System.out.println("==================");
        for (int i = 0; i < testPositions.length; i++) {
            System.out.println("\nPosition: " + positionNames[i]);
            System.out.println("FEN: " + testPositions[i]);
            benchmarkStockfish(testPositions[i], depths);
        }

        System.out.println("\n\n=== COMPARISON SUMMARY ===");
        System.out.println("Position                | Depth | DeepakChess (ms) | Stockfish (ms) | Speed Ratio");
        System.out.println("------------------------|-------|------------------|----------------|------------");
        for (int i = 0; i < testPositions.length; i++) {
            for (int depth : depths) {
                long dcTime = benchmarkDeepakChessTime(testPositions[i], depth);
                long sfTime = benchmarkStockfishTime(testPositions[i], depth);
                String ratio = sfTime > 0 ? String.format("%.1fx", (double) dcTime / sfTime) : "N/A";
                System.out.printf("%-24s|   %d   | %16d | %14d | %s%n",
                        positionNames[i].length() > 24 ? positionNames[i].substring(0, 24) : positionNames[i],
                        depth, dcTime, sfTime, ratio);
            }
        }
    }

    static void benchmarkDeepakChess(String fen, int[] depths) throws Exception {
        for (int depth : depths) {
            UCI uci = new UCI();
            BitboardBoard board = uci.parseFenForTest(fen);
            String color = fen.split("\\s+")[1].equals("w") ? "white" : "black";

            long start = System.nanoTime();
            uci.negamaxForTest(board, depth, color, Integer.MIN_VALUE, Integer.MAX_VALUE);
            long end = System.nanoTime();

            long elapsedMs = (end - start) / 1_000_000;
            double elapsedSec = elapsedMs / 1000.0;
            int occupancy = uci.getTT().occupancy();

            System.out.printf("  Depth %-2d | Time: %8d ms | TT entries: %d%n", depth, elapsedMs, occupancy);
        }
    }

    static long benchmarkDeepakChessTime(String fen, int depth) throws Exception {
        UCI uci = new UCI();
        BitboardBoard board = uci.parseFenForTest(fen);
        String color = fen.split("\\s+")[1].equals("w") ? "white" : "black";

        long start = System.nanoTime();
        uci.negamaxForTest(board, depth, color, Integer.MIN_VALUE, Integer.MAX_VALUE);
        long end = System.nanoTime();

        return (end - start) / 1_000_000;
    }

    static void benchmarkStockfish(String fen, int[] depths) throws Exception {
        if (!isStockfishAvailable()) {
            System.out.println("  Stockfish not found. Install it or set STOCKFISH_PATH.");
            return;
        }

        for (int depth : depths) {
            ProcessBuilder pb = new ProcessBuilder(STOCKFISH_PATH);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));

            writer.write("uci");
            writer.newLine();
            writer.flush();
            waitForOutput(reader, "uciok", 2000);

            writer.write("position fen " + fen);
            writer.newLine();
            writer.flush();

            writer.write("go depth " + depth);
            writer.newLine();
            writer.flush();

            long start = System.currentTimeMillis();
            String bestmove = waitForBestmove(reader, 30000);
            long end = System.currentTimeMillis();

            long elapsedMs = end - start;

            System.out.printf("  Depth %-2d | Time: %8d ms | Bestmove: %s%n", depth, elapsedMs, bestmove);

            writer.write("quit");
            writer.newLine();
            writer.flush();
            p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
            p.destroy();
        }
    }

    static long benchmarkStockfishTime(String fen, int depth) throws Exception {
        if (!isStockfishAvailable()) return -1;

        ProcessBuilder pb = new ProcessBuilder(STOCKFISH_PATH);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));

        writer.write("uci");
        writer.newLine();
        writer.flush();
        waitForOutput(reader, "uciok", 2000);

        writer.write("position fen " + fen);
        writer.newLine();
        writer.flush();

        writer.write("go depth " + depth);
        writer.newLine();
        writer.flush();

        long start = System.currentTimeMillis();
        waitForBestmove(reader, 30000);
        long end = System.currentTimeMillis();

        writer.write("quit");
        writer.newLine();
        writer.flush();
        p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
        p.destroy();

        return end - start;
    }

    static boolean isStockfishAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(STOCKFISH_PATH);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));

            writer.write("uci");
            writer.newLine();
            writer.flush();

            boolean found = waitForOutput(reader, "uciok", 2000);

            writer.write("quit");
            writer.newLine();
            writer.flush();
            p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
            p.destroy();

            return found;
        } catch (Exception e) {
            return false;
        }
    }

    static boolean waitForOutput(BufferedReader reader, String target, long timeoutMs) throws Exception {
        long start = System.currentTimeMillis();
        String line;
        while ((System.currentTimeMillis() - start) < timeoutMs) {
            if (reader.ready()) {
                line = reader.readLine();
                if (line != null && line.contains(target)) {
                    return true;
                }
            } else {
                Thread.sleep(10);
            }
        }
        return false;
    }

    static String waitForBestmove(BufferedReader reader, long timeoutMs) throws Exception {
        long start = System.currentTimeMillis();
        String line;
        while ((System.currentTimeMillis() - start) < timeoutMs) {
            if (reader.ready()) {
                line = reader.readLine();
                if (line != null && line.startsWith("bestmove")) {
                    String[] parts = line.split("\\s+");
                    return parts.length > 1 ? parts[1] : "none";
                }
            } else {
                Thread.sleep(10);
            }
        }
        return "timeout";
    }
}
