package game;
public class TranspositionTableTest{
    private static int passed = 0;
    private static int failed = 0;
    public static void main(String[] args){
        System.out.println("=== TRANSPOSITION TABLE TESTS ===\n");
        testStoreAndRetrieve();
        testMissOnUnknownKey();
        testOverwriteDeeperEntry();
        testKeepShallowerEntry();
        testClear();
        testHitRate();
        testCollisionHandling();
        testStatsCounters();
        testStoreWithBestMove();
        testLargeTable();
        System.out.println("\n=== RESULTS ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Total:  " + (passed + failed));
        if(failed > 0){
            System.exit(1);
        }
    }
    static void testStoreAndRetrieve(){
        TranspositionTable tt = new TranspositionTable(1024);
        long key = 0x1234567890ABCDEFL;
        tt.store(key,150,4,TranspositionTable.EXACT,1,4,3,4);
        TranspositionTable.TTEntry entry = tt.probe(key);
        assertNotNull("Store and retrieve", entry);
        assertEqual("Score", 150, entry.score);
        assertEqual("Depth", 4, entry.depth);
        assertEqual("Flag", TranspositionTable.EXACT, entry.flag);
        assertEqual("FromRow", 1, entry.fromRow);
        assertEqual("FromCol", 4, entry.fromCol);
        assertEqual("ToRow", 3, entry.toRow);
        assertEqual("ToCol", 4, entry.toCol);
    }
    static void testMissOnUnknownKey(){
        TranspositionTable tt = new TranspositionTable(1024);
        TranspositionTable.TTEntry entry = tt.probe(0xDEADBEEFL);
        assertNull("Miss on unknown key", entry);
    }
    static void testOverwriteDeeperEntry(){
        TranspositionTable tt = new TranspositionTable(1024);
        long key = 0xCAFEBABEL;
        tt.store(key,100,3,TranspositionTable.EXACT);
        tt.store(key,200,5,TranspositionTable.LOWERBOUND);
        TranspositionTable.TTEntry entry = tt.probe(key);
        assertEqual("Overwrites with deeper depth - score", 200, entry.score);
        assertEqual("Overwrites with deeper depth - depth", 5, entry.depth);
        assertEqual("Overwrites with deeper depth - flag", TranspositionTable.LOWERBOUND, entry.flag);
    }
    static void testKeepShallowerEntry(){
        TranspositionTable tt = new TranspositionTable(1024);
        long key = 0xFEEDFACEL;
        tt.store(key,300,6,TranspositionTable.UPPERBOUND);
        tt.store(key,100,2,TranspositionTable.EXACT);
        TranspositionTable.TTEntry entry = tt.probe(key);
        assertEqual("Keeps shallower entry - score", 300, entry.score);
        assertEqual("Keeps shallower entry - depth", 6, entry.depth);
    }
    static void testClear(){
        TranspositionTable tt = new TranspositionTable(1024);
        tt.store(0x1111L,50,3,TranspositionTable.EXACT);
        tt.store(0x2222L,75,4,TranspositionTable.LOWERBOUND);
        tt.probe(0x1111L);
        tt.probe(0x9999L);
        assertEqual("Hits before clear", 1, tt.getHits());
        assertEqual("Misses before clear", 1, tt.getMisses());
        tt.clear();
        assertEqual("Clear resets hits", 0, tt.getHits());
        assertEqual("Clear resets misses", 0, tt.getMisses());
        assertTrue("Clear removes entry 1", tt.occupancy() == 0);
    }
    static void testHitRate(){
        TranspositionTable tt = new TranspositionTable(1024);
        long key = 0xABCDEFL;
        tt.store(key,100,3,TranspositionTable.EXACT);
        tt.probe(key);
        tt.probe(key);
        tt.probe(0x123456L);
        assertEqual("Hits", 2, tt.getHits());
        assertEqual("Misses", 1, tt.getMisses());
        double rate = tt.getHitRate();
        assertTrue("Hit rate is 0.666", Math.abs(rate - 2.0/3.0) < 0.001);
    }
    static void testCollisionHandling(){
        TranspositionTable tt = new TranspositionTable(16);
        long key1 = 0x1L;
        long key2 = 0x11L;
        tt.store(key1,100,3,TranspositionTable.EXACT);
        tt.store(key2,200,5,TranspositionTable.EXACT);
        TranspositionTable.TTEntry entry = tt.probe(key2);
        assertNotNull("Collision - entry exists", entry);
        assertEqual("Collision - deeper entry stored", 200, entry.score);
    }
    static void testStatsCounters(){
        TranspositionTable tt = new TranspositionTable(1024);
        tt.store(0xAAL,10,2,TranspositionTable.EXACT);
        tt.probe(0xAAL);
        tt.probe(0xBBL);
        tt.probe(0xAAL);
        tt.probe(0xCCL);
        assertEqual("Stats - hits", 2, tt.getHits());
        assertEqual("Stats - misses", 2, tt.getMisses());
    }
    static void testStoreWithBestMove(){
        TranspositionTable tt = new TranspositionTable(1024);
        long key = 0x999999L;
        tt.store(key,-50,4,TranspositionTable.EXACT,6,3,4,3);
        TranspositionTable.TTEntry entry = tt.probe(key);
        assertTrue("Has best move", entry.hasBestMove);
        assertEqual("Best move fromRow", 6, entry.fromRow);
        assertEqual("Best move fromCol", 3, entry.fromCol);
        assertEqual("Best move toRow", 4, entry.toRow);
        assertEqual("Best move toCol", 3, entry.toCol);
    }
    static void testLargeTable(){
        TranspositionTable tt = new TranspositionTable(1 << 20);
        assertEqual("Default size is 1M", 1 << 20, tt.getSize());
        assertEqual("Initial occupancy", 0, tt.occupancy());
        for(long i = 0 ; i < 10000 ; i++){
            tt.store(i * 0x123456789ABCDEFL,(int)(i % 2000) - 1000,(int)(i % 8),TranspositionTable.EXACT);
        }
        assertTrue("Occupancy > 0 after inserts", tt.occupancy() > 0);
    }
    static void assertEqual(String name,long expected,long actual){
        if(expected == actual){
            System.out.println("[PASS] " + name);
            passed++;
        } else {
            System.out.println("[FAIL] " + name + " (expected " + expected + ", got " + actual + ")");
            failed++;
        }
    }
    static void assertNotNull(String name,Object obj){
        if(obj != null){
            System.out.println("[PASS] " + name);
            passed++;
        } else {
            System.out.println("[FAIL] " + name + " (was null)");
            failed++;
        }
    }
    static void assertNull(String name,Object obj){
        if(obj == null){
            System.out.println("[PASS] " + name);
            passed++;
        } else {
            System.out.println("[FAIL] " + name + " (was not null)");
            failed++;
        }
    }
    static void assertTrue(String name,boolean condition){
        if(condition){
            System.out.println("[PASS] " + name);
            passed++;
        } else {
            System.out.println("[FAIL] " + name);
            failed++;
        }
    }
}
