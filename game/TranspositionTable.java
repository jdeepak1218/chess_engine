package game;
public class TranspositionTable{
    public static final int EXACT = 0;
    public static final int LOWERBOUND = 1;
    public static final int UPPERBOUND = 2;
    static class TTEntry{
        long key;
        int score;
        int depth;
        int flag;
        int fromRow,fromCol,toRow,toCol;
        boolean hasBestMove;
        TTEntry(long key,int score,int depth,int flag,int fromRow,int fromCol,int toRow,int toCol){
            this.key = key;
            this.score = score;
            this.depth = depth;
            this.flag = flag;
            this.fromRow = fromRow;
            this.fromCol = fromCol;
            this.toRow = toRow;
            this.toCol = toCol;
            this.hasBestMove = true;
        }
    }
    private final TTEntry[] table;
    private final int size;
    private int hits;
    private int misses;
    private int collisions;
    public TranspositionTable(int size){
        this.size = size;
        this.table = new TTEntry[size];
        this.hits = 0;
        this.misses = 0;
        this.collisions = 0;
    }
    public TranspositionTable(){
        this(1 << 20);
    }
    public TTEntry probe(long key){
        int index = (int)(key & (size - 1));
        TTEntry entry = table[index];
        if(entry != null && entry.key == key){
            hits++;
            return entry;
        }
        misses++;
        return null;
    }
    public void store(long key,int score,int depth,int flag,int fromRow,int fromCol,int toRow,int toCol){
        int index = (int)(key & (size - 1));
        TTEntry existing = table[index];
        if(existing != null && existing.key != key){
            collisions++;
        }
        if(existing == null || depth >= existing.depth){
            table[index] = new TTEntry(key,score,depth,flag,fromRow,fromCol,toRow,toCol);
        }
    }
    public void store(long key,int score,int depth,int flag){
        store(key,score,depth,flag,-1,-1,-1,-1);
    }
    public void clear(){
        for(int i = 0 ; i < size ; i++){
            table[i] = null;
        }
        hits = 0;
        misses = 0;
        collisions = 0;
    }
    public int getHits(){ return hits; }
    public int getMisses(){ return misses; }
    public int getCollisions(){ return collisions; }
    public int getSize(){ return size; }
    public double getHitRate(){
        int total = hits + misses;
        return total == 0 ? 0.0 : (double)hits / total;
    }
    public int occupancy(){
        int count = 0;
        for(TTEntry entry : table){
            if(entry != null) count++;
        }
        return count;
    }
}
