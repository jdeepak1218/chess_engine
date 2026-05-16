 A Bitboard-Based UCI Chess Engine

================================================================================

What is this?

This is a chess engine written from scratch in Java. It plays chess by searching
through possible moves, evaluating positions, and picking the best one. It
speaks the Universal Chess Interface (UCI) protocol, which means you can
connect it to any UCI-compatible chess GUI like Arena, CuteChess, or Lichess to
play against it or have it analyze positions.

The engine is called DeepakChess. It uses bitboards internally for speed, and
implements many of the standard search and evaluation techniques found in
modern chess engines.

================================================================================

Project Structure

chess_project/
  game/
     UCI.java               - Main engine loop, search, evaluation, UCI protocol, time management
    BitboardBoard.java     - Board representation using bitboards
    BitMove.java           - Move data structure
    TranspositionTable.java - Caches previously searched positions
    Zobrist.java           - Zobrist hashing for position identification
    Benchmark.java         - Performance benchmarking
    UCITest.java           - Tests for UCI protocol correctness
    ZobristTest.java       - Tests for Zobrist hashing correctness
    TranspositionTableTest.java - Tests for transposition table
    Phase1Test.java        - Additional tests
    StockfishBenchmark.java - Compare speed against Stockfish
    Move.java              - Initial move class (legacy)
    Board.java             - Initial board class (legacy)
    Game.java              - Initial game logic (legacy)
    Main.java              - Entry point for local play (legacy)
  pieces/
    Piece.java             - Abstract piece class (legacy)
    Pawn.java, King.java, etc. - Individual piece classes (legacy)
  run.bat                  - Compile and run locally
  run_uci.bat              - Compile and start in UCI mode
  run_tests.bat            - Compile and run all tests
  run_benchmark.bat        - Run performance benchmark

================================================================================

What was implemented and why

This section explains each component of the engine, what it does, and the
reasoning behind implementing it that way.

1. Bitboard Board Representation (BitboardBoard.java)

A bitboard is a 64-bit integer where each bit represents one square on the
chessboard. For example, bit 0 might represent a1, bit 63 might represent h8.
Instead of looping through an 8x8 array to check if a square has a piece, you
can use fast bitwise operations (AND, OR, XOR, shifts) to compute attacks,
check for blockers, and generate moves.

Why bitboards over an array board:
- Performance. Operations like "find all squares attacked by this bishop" can
  be computed with a few bitwise operations instead of looping through squares.
- Modern CPUs handle 64-bit integers in a single instruction, so operations
  that would need loops over an 8x8 board can be done in constant time.
- Move generation, checking for checks, and evaluating the board all become
  much faster.

The engine stores six piece bitboards (pawn, knight, bishop, rook, queen, king)
for each color (white and black), plus occupancy bitboards for all white pieces,
all black pieces, and all pieces combined. Incremental updates mean that when a
piece moves, only the relevant bitboards are updated rather than recomputing
everything from scratch.

2. Move Data Structure (BitMove.java)

Each move stores more than just "from square" and "to square." It also stores:
- The piece that moved and the piece that was captured
- Whether the move was a castling, en passant, or promotion
- The previous castling rights, en passant square, and half-move clock
- The previous Zobrist hash value

Why so much information: A chess engine must be able to undo moves during
search. By storing the previous state in the move itself, the engine can
revert the board to its exact prior state without maintaining a separate stack
of states. This is called "make move, undo move" and is how virtually all
chess engines work.

3. Move Generation (BitboardBoard.java)

The engine generates pseudo-legal moves (moves that are legal except possibly
leaving the king in check) and then filters out the illegal ones. For sliding
pieces (bishops, rooks, queens), it uses bitboard techniques with magic
bitboards or similar to compute ray attacks quickly.

Why pseudo-legal first and then filter: Generating only fully legal moves is
complicated. It is easier and faster to generate all moves that look legal and
then test each one by making the move and checking if the king is in check.

4. Zobrist Hashing (Zobrist.java)

Zobrist hashing assigns a random 64-bit number to each piece on each square,
plus numbers for side to move, en passant squares, and castling rights. The
hash of a position is the XOR of all these random numbers for the pieces and
state present in the position.

Why Zobrist hashing:
- It is incremental: when a piece moves, you XOR out the old piece-square key,
  XOR in the new piece-square key, toggle the side-to-move key, and update the
  castling/en-passant keys as needed. This takes O(1) time instead of
  recomputing from scratch.
- The XOR operation means that the same position reached by different move
  sequences will always have the same hash, as long as the board state is
  identical. This is crucial for the transposition table.
- 64-bit keys have an astronomically low chance of collision, making them
  effectively unique.

5. Transposition Table (TranspositionTable.java)

The transposition table is a large hash table (default 2^20 = 1,048,576
entries) that stores previously searched positions along with their scores,
search depth, and the best move found.

Why a transposition table:
- Chess has many transpositions: the same position can be reached through
  different move orders (e.g., 1.e4 e5 2.Nf3 and 1.Nf3 e5 2.e4 lead to the
  same position). Without a transposition table, the engine would search the
  same position multiple times, wasting time.
- The table stores the score at a given depth. On a hash hit, if the stored
  depth is at least the requested depth, the engine can skip searching the
  position entirely and just return the stored score.
- The table also stores the best move from the previous search. This is used
  in move ordering (see below).
- Three types of entries are stored: EXACT (the exact score), LOWERBOUND
  (score is at least this value, from a beta cutoff), and UPPERBOUND (score is
  at most this value, from an alpha cutoff). This lets the engine use the
  information correctly in alpha-beta pruning.

The table uses a simple replacement scheme: when a new entry wants to write to
a slot, if it has a greater or equal depth than the existing entry, it
overwrites it. This tends to keep deeper (more valuable) searches in the table.

6. Search Algorithm: Negamax with Alpha-Beta Pruning (UCI.java)

The engine uses the negamax formulation of minimax search. Negamax exploits the
fact that a position's score from one player's perspective is the negative of
the score from the opponent's perspective. This simplifies the code: instead of
alternating between maximizing and minimizing, the search always maximizes and
negates the result.

Alpha-beta pruning is the core optimization. The engine maintains a search
window (alpha, beta) that represents the range of possible scores. If a move
is found that proves the position is at least beta (too good for the current
player), the search stops and returns beta (a beta cutoff). If a move is worse
than alpha, it is skipped. This prunes away large portions of the search tree
without affecting the result.

Why this is necessary: Without alpha-beta, searching every possible move at
every node results in O(b^d) nodes where b is the branching factor (~35 in
chess) and d is depth. With perfect move ordering, alpha-beta reduces this to
O(b^(d/2)), which is the square root. That means you can search twice as deep
in the same time.

7. Iterative Deepening (UCI.java)

Instead of searching directly to a target depth, the engine starts at depth 1,
then 2, then 3, and so on until it reaches the target depth or runs out of
time.

Why iterative deepening:
- It seems wasteful to search depth 1, then throw away the result and search
  depth 2. But the time spent at lower depths is negligible compared to higher
  depths (since the tree grows exponentially).
- The main benefit is that the transposition table gets populated with good
  moves from shallow searches, which then serve as the TT best move for deeper
  searches. This dramatically improves move ordering.
- It also allows the engine to be stopped at any time and still have a move
  ready (the best move from the last completed depth).

8. Move Ordering (UCI.java, Benchmark.java)

In alpha-beta search, the order in which moves are searched matters
enormously. If the best move is searched first, many other moves can be pruned
away. If the worst move is searched first, every move must be searched fully.

The engine uses four layers of move ordering, from highest priority to lowest:

  1. Transposition Table Best Move: The move that was best when this position
     was last searched. This is almost always the best move to try first.
     Score: 10,000,000.

  2. Captures ordered by MVV-LVA (Most Valuable Victim - Least Valuable
     Attacker): Captures are scored as victim_value * 100 - attacker_value.
     This means capturing a queen with a pawn (high victim, low attacker)
     scores higher than capturing a pawn with a queen (low victim, high
     attacker). The formula ensures the engine tries winning captures before
     losing ones. Score: 1,000,000 + MVV-LVA value.

  3. Killer Moves: Quiet moves (non-captures) that caused a beta cutoff at the
     same search depth in sibling nodes. Two killer moves are stored per
     depth. If a quiet move caused a cutoff once, it is likely to do so again
     in similar positions. Score: 500,000 - killer_index * 1000.

  4. History Heuristic: A 64x64 table that tracks how successful each quiet
     move (from square x to square y) has been at causing beta cutoffs across
     the entire search. Each time a quiet move causes a cutoff, its history
     value is incremented by depth * depth (giving more weight to deeper
     cutoffs). Score: history value.

Why move ordering matters so much: With random ordering, alpha-beta degenerates
to brute-force search, exploring O(b^d) nodes. With perfect ordering, it
explores O(b^(d/2)) nodes. Real-world ordering gets close to perfect,
typically reducing node counts by 90-99% compared to random ordering.

9. Piece-Square Tables (PSQT) (UCI.java, Benchmark.java)

Piece-square tables add positional bonuses to the evaluation based on where
each piece sits. A knight on e5 controls many central squares and should score
higher than a knight on a1 that controls nothing.

The engine has six piece-square tables (pawn, knight, bishop, rook, queen,
king) encoded as 64-element arrays. Each element holds a centipawn bonus for
that piece on that square. For black pieces, the table is mirrored vertically
by XORing the square index with 56 (which flips the row).

What each table rewards:
- Pawns: Center control and advancement. A pawn on d4 or e4 gets a bonus.
  A pawn on the 7th rank (about to promote) gets a large bonus.
- Knights: Central squares. A knight on e5, d5, e4, or d4 gets +20. Edge
  squares get -50.
- Bishops: Center and long diagonals. Central squares get +10, corners get
  -20.
- Rooks: Center files and the 7th rank (where they attack the opponent's
  pawns before they move).
- Queens: Central squares get +5. Corners get a penalty.
- King: Safety. The king is rewarded for castling (corners get +30) and
  penalized for being exposed in the center (-50).

Why PSQT over pure material: Two engines with the same material can have very
different winning chances based on piece placement. PSQT captures basic
positional concepts like "knights on the rim are dim" and "control the center"
without needing complex evaluation code. PSQT alone typically adds 200-300 Elo
compared to a purely material-based evaluation.

10. Null-Move Pruning (UCI.java, Benchmark.java)

Null-move pruning is based on the observation that in most positions, having
the move is an advantage. If after making a "null move" (passing the turn to
the opponent) and searching at a reduced depth, the position still scores above
beta, then the original position is too good for the opponent and the search
can be pruned.

Implementation: The engine saves the current en passant square and side to
move, toggles the side to move (updating the Zobrist hash), and searches at
depth - 3 (a reduction of 3 ply) with a null window of (-beta, -beta + 1). If
the result is >= beta, the search returns beta without searching any moves.

Conditions for null-move pruning:
- Depth must be at least 3 (so the reduction doesn't go below 0).
- The side to move must not be in check (you cannot pass when in check).
- The side to move must have at least one non-pawn piece (to avoid zugzwang).

The third condition is the zugzwang fix. In pawn-only endgames, having the
move can be a disadvantage (zugzwang), and null-move pruning would mistakenly
think passing is harmless. By disabling null moves when only pawns and king
remain, the engine avoids this class of error. This is the standard heuristic
used by Stockfish and other top engines.

11. Late Move Reductions (LMR) (UCI.java, Benchmark.java)

LMR is based on the observation that after the first few moves in a sorted move
list, the remaining moves are almost certainly bad. Searching them at full
depth is wasteful.

Implementation: The sorted move list is searched with an indexed loop. The
first 4 moves (indices 0-3) are searched at full depth. After that, for quiet
moves (non-captures), the search depth is reduced by R where
R = 1 + min((i - 4) / 4, 2). This means:
- Moves 4-7: searched at depth - 2 (reduction of 1)
- Moves 8-11: searched at depth - 3 (reduction of 2)
- Moves 12+: searched at depth - 4 (reduction of 3)

If the reduced-depth search produces a score that beats alpha, the move is
re-searched at full depth to get an accurate evaluation. Captures are never
reduced because they often change the evaluation significantly.

Why LMR is safe: The move ordering ensures that the first few moves are the
most promising ones (TT best move, winning captures, killers). The remaining
moves are almost always bad. The reduced-depth search is a quick check that
confirms they are bad. In the rare case where a reduced move turns out to be
good, the re-search catches it.

LMR is the single biggest search optimization after alpha-beta itself,
typically reducing node counts by 60-75% and adding about 200 Elo.

12. Quiescence Search (UCI.java)

The horizon effect occurs when a search stops at a certain depth, missing a
capturing sequence that changes the evaluation. For example, at depth 5, the
engine might evaluate the position as equal, but at depth 6, it would see that
the queen can be captured.

Quiescence search continues searching only captures (and sometimes checks) at
"leaf" nodes beyond the nominal search depth. It uses a delta-pruning
technique: if even capturing the most valuable opponent piece cannot bring the
score up to alpha, the search stops. This prevents spending time on hopeless
captures.

Why quiescence is needed: Without it, the engine would make tactical blunders
because it couldn't "see" captures that happen just beyond the search depth.

13. Evaluation Function (UCI.java, Benchmark.java)

The evaluation function assigns a score to a position from White's perspective.
Positive scores favor White, negative scores favor Black.

The evaluation has two components:
  1. Material: Each piece has a value in centipawns (1/100 of a pawn):
     Pawn = 100, Knight = 320, Bishop = 330, Rook = 500, Queen = 900,
     King = 20000.
  2. Position: From the piece-square tables, adding bonuses for good piece
     placement and penalties for bad placement.

The total score is material + positional bonus for each piece on the board.

Why centipawns: Using 1/100 of a pawn as the unit gives enough granularity
for evaluation while keeping values as integers (avoiding floating-point
arithmetic).

14. UCI Protocol (UCI.java)

The Universal Chess Interface is a text-based protocol that lets chess GUIs
communicate with chess engines. The engine implements the following commands:

- uci: Announces the engine name and available options.
- isready: Responds with "readyok".
- ucinewgame: Clears the transposition table for a new game.
- position [fen | startpos] moves ...: Sets up a position on the board.
- go depth N / wtime / btime / winc / binc / movestogo / movetime / infinite: Starts searching.
- stop: Stops the current search.
- quit: Exits the engine.

The engine runs the search on a separate thread so it can respond to stop
commands even while searching. The search outputs "info" lines with depth,
score, and principal variation, and finishes with "bestmove <move>".
The engine includes time management (see section 15) to allocate time
across moves when clock times are provided.

15. Time Management (UCI.java)

Time management allocates the available clock time across moves during a
game. The engine implements these UCI go command parameters:

- wtime/btime: Remaining time for white/black in milliseconds.
- winc/binc: Fischer increment per move for white/black in milliseconds.
- movestogo: Number of moves until the next time control.
- movetime: Fixed time per move in milliseconds (overrides other controls).

The time allocation strategy:

1. If movetime is specified by the GUI, use it directly.
2. If time controls (wtime/btime) are specified, calculate a target:
   - Divide remaining time by expected remaining moves (default 40, or
     movestogo if provided).
   - Add the per-move increment.
   - Reserve a safety margin (up to 2000ms or 20% of remaining time) to
     avoid losing on time.
   - Minimum 10ms per search.

This ensures the engine uses most of its available time without risking
loss on time. The time management is applied in the iterative deepening
loop: if the allocated time expires before the target depth is reached,
the search stops and returns the best move from the last completed depth.

Why this basic approach: More sophisticated systems consider position
complexity, move count, or phase of the game. This simple approach
ensures the engine avoids time trouble while using its available time
effectively, which is sufficient for playing under standard tournament
time controls.

16. Benchmark and Tests

Benchmark.java measures the engine's performance by searching the starting
position at various depths with and without the transposition table. It
reports node counts and search times, allowing you to see the effect of each
optimization.

UCITest.java runs 30 test cases covering:
- FEN parsing correctness
- Perft tests (number of positions at each depth from various starting
  positions)
- UCI protocol command handling
- Move validation

17. Legacy Code

The files Board.java, Game.java, Main.java, Move.java, and the piece classes
in the pieces/ directory are from the initial implementation before the
bitboard-based engine was completed. They are not used by the current engine
but have been kept in the project. The current engine uses only the
game.UCI class and its supporting classes (BitboardBoard, BitMove,
TranspositionTable, Zobrist).

================================================================================

How to Build and Run

Prerequisites:
- Java Development Kit (JDK) 17 or later
- Any UCI-compatible chess GUI (optional, for playing against the engine)

Compilation:
  javac -d out game/*.java pieces/*.java

Running in UCI mode (for connecting to a GUI):
  java -cp out game.UCI

Running the benchmark:
  java -cp out game.Benchmark

Running all tests:
  java -cp out game.UCITest

Playing locally (uses the legacy UI):
  java -cp out game.Main

Alternatively, use the included batch files:
  run_uci.bat       - Compile and start in UCI mode
  run_benchmark.bat - Compile and run the benchmark
  run_tests.bat     - Compile and run all tests
  run.bat           - Compile and run locally

To play against the engine, connect it to Arena, CuteChess, or any
UCI-compatible chess GUI. In the GUI, add the engine as:
  java -cp out game.UCI
or point to the run_uci.bat file.

================================================================================

Current Estimated Playing Strength

Based on the features implemented, the engine is estimated at approximately
1400-1600 Elo (using standard human rating scales). This means it plays chess
at a strong club-player level. It will consistently beat casual players but
will struggle against masters and most modern chess engines.

The main bottleneck is the evaluation function, which only uses material and
piece-square tables. Adding pawn structure evaluation (doubled pawns, isolated
pawns, passed pawns), king safety, and piece mobility would significantly
improve strength.

================================================================================

What Can Be Improved

The engine has a solid foundation with all the standard search optimizations.
Here are the most impactful improvements that could be made:

1. Aspiration Windows: Instead of searching with a wide window of
   (-infinity, +infinity), use a narrow window around the previous iteration's
   score. If the score falls outside the window, re-search with a wider window.

2. Static Exchange Evaluation (SEE): Instead of the simple MVV-LVA capture
   ordering, compute whether a capture actually wins or loses material. This
   prevents the engine from wasting time searching losing captures.

3. Check Extensions: Extend the search depth by one ply when a move gives
   check. Chess tactics often involve checks, and without extensions, they
   can be pruned away by LMR or null-move pruning.

4. Pawn Structure Evaluation: Evaluate doubled pawns, isolated pawns, passed
   pawns, connected pawns, and pawn chains. This is the biggest missing piece
   in the evaluation.

5. King Safety: Evaluate the pawn shield around the king and open files near
   the king. Punish the opponent for attacking a poorly defended king.

6. Piece Mobility: Count the number of legal moves available to each piece and
   add a bonus for higher mobility.

7. Multi-Threaded Search: Use multiple threads to search the tree in parallel.
   This requires a more complex search architecture but can give a significant
   speedup on modern CPUs.

================================================================================

License

This project is for educational purposes. Feel free to study, modify, and
learn from the code.
