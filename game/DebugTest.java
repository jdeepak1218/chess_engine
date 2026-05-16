package game;
import pieces.*;
import java.util.*;

public class DebugTest {
    public static void main(String[] args) throws Exception {
        BitboardBoard b = new BitboardBoard();
        b.initializeBoard();
        b.printBoard();
        System.out.println("Side to move: " + b.getSideToMoveStr());
        System.out.println("King in check: " + b.isKingInCheck("white"));
        System.out.println("Castling rights: " + b.castlingRights);
        List<BitMove> moves = b.generateLegalMoves("white");
        System.out.println("Number of moves: " + moves.size());
        for (BitMove m : moves) {
            int fromRow = m.from() / 8;
            int fromCol = m.from() % 8;
            int toRow = m.to() / 8;
            int toCol = m.to() % 8;
            char fromFile = (char)('a' + fromCol);
            char fromRank = (char)('1' + fromRow);
            char toFile = (char)('a' + toCol);
            char toRank = (char)('1' + toRow);
            System.out.println("" + fromFile + fromRank + toFile + toRank);
        }
        System.out.println();

        // Now test UCI search
        UCI uci = new UCI();
        uci.processPositionForTest("startpos");
        int q = uci.quiesceForTest(b, "white", Integer.MIN_VALUE, Integer.MAX_VALUE);
        System.out.println("Quiesce score: " + q);
    }
}
