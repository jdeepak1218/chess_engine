<<<<<<< HEAD
 A Bitboard-Based UCI Chess Engine
=======

>>>>>>> 55b2c86 (update readme)

A chess engine written from scratch in Java. It plays chess by searching through possible moves, evaluating positions, and picking the best one. It communicates via the **Universal Chess Interface (UCI)** protocol, making it compatible with any UCI-compatible chess GUI such as Arena, CuteChess, or Lichess.

---

## Table of Contents

- [Project Structure](#project-structure)
- [How to Build and Run](#how-to-build-and-run)
- [Architecture Overview](#architecture-overview)
  - [Board Representation](#1-board-representation)
  - [Move Data Structure](#2-move-data-structure)
  - [Move Generation](#3-move-generation)
  - [Zobrist Hashing](#4-zobrist-hashing)
  - [Transposition Table](#5-transposition-table)
  - [Search Algorithm](#6-search-algorithm-negamax--alpha-beta-pruning)
  - [Iterative Deepening](#7-iterative-deepening)
  - [Move Ordering](#8-move-ordering)
  - [Piece-Square Tables](#9-piece-square-tables-psqt)
  - [Null-Move Pruning](#10-null-move-pruning)
  - [Late Move Reductions](#11-late-move-reductions-lmr)
  - [Quiescence Search](#12-quiescence-search)
  - [Evaluation Function](#13-evaluation-function)
  - [UCI Protocol](#14-uci-protocol)
  - [Time Management](#15-time-management)
- [Playing Strength](#playing-strength)
- [Potential Improvements](#potential-improvements)
- [Legacy Code](#legacy-code)
- [License](#license)

---

## Project Structure

```
chess_project/
├── game/
│   ├── UCI.java                      # Main engine loop, search, evaluation, UCI protocol, time management
│   ├── BitboardBoard.java            # Board representation using bitboards
│   ├── BitMove.java                  # Move data structure
│   ├── TranspositionTable.java       # Caches previously searched positions
│   ├── Zobrist.java                  # Zobrist hashing for position identification
│   ├── Benchmark.java                # Performance benchmarking
│   ├── UCITest.java                  # Tests for UCI protocol correctness
│   ├── ZobristTest.java              # Tests for Zobrist hashing correctness
│   ├── TranspositionTableTest.java   # Tests for transposition table
│   ├── Phase1Test.java               # Additional tests
│   ├── StockfishBenchmark.java       # Compare speed against Stockfish
│   ├── Move.java                     # [Legacy] Initial move class
│   ├── Board.java                    # [Legacy] Initial board class
│   ├── Game.java                     # [Legacy] Initial game logic
│   └── Main.java                     # [Legacy] Entry point for local play
├── pieces/
│   ├── Piece.java                    # [Legacy] Abstract piece class
│   └── Pawn.java, King.java, ...     # [Legacy] Individual piece classes
├── run.bat                           # Compile and run locally
├── run_uci.bat                       # Compile and start in UCI mode
├── run_tests.bat                     # Compile and run all tests
└── run_benchmark.bat                 # Run performance benchmark
```

> **Note:** Files marked `[Legacy]` are from the initial implementation and are not used by the current engine. See [Legacy Code](#legacy-code) for details.

---

## How to Build and Run

### Prerequisites

- Java Development Kit (JDK) 17 or later
- A UCI-compatible chess GUI (optional, for playing against the engine)

### Compilation

```bash
javac -d out game/*.java pieces/*.java
```

### Running

| Mode | Command |
|---|---|
| UCI mode (connect to GUI) | `java -cp out game.UCI` |
| Benchmark | `java -cp out game.Benchmark` |
| All tests | `java -cp out game.UCITest` |
| Local play (legacy UI) | `java -cp out game.Main` |

Alternatively, use the included batch files:

```
run_uci.bat        Compile and start in UCI mode
run_benchmark.bat  Compile and run the benchmark
run_tests.bat      Compile and run all tests
run.bat            Compile and run locally
```

### Connecting to a GUI

To play against the engine in Arena, CuteChess, or any UCI-compatible GUI, add the engine with the command:

```
java -cp out game.UCI
```

Or point the GUI to `run_uci.bat`.

---

## Architecture Overview

### 1. Board Representation

**File:** `BitboardBoard.java`

A **bitboard** is a 64-bit integer where each bit represents one square on the chessboard (bit 0 = a1, bit 63 = h8). Instead of looping through an 8×8 array, attacks, blockers, and move generation are computed with fast bitwise operations (AND, OR, XOR, shifts).

The engine stores six piece bitboards (pawn, knight, bishop, rook, queen, king) for each color, plus occupancy bitboards for white pieces, black pieces, and all pieces combined. When a piece moves, only the relevant bitboards are updated — no full recomputation.

**Why bitboards over an array board:**
- Operations like "find all squares attacked by this bishop" reduce to a few bitwise instructions instead of loops.
- Modern CPUs handle 64-bit integers in a single instruction.
- Move generation, check detection, and evaluation are all significantly faster.

---

### 2. Move Data Structure

**File:** `BitMove.java`

Each move stores more than just source and destination squares. It also encodes:

- The piece that moved and the piece that was captured
- Flags for castling, en passant, and promotion
- The previous castling rights, en passant square, and half-move clock
- The previous Zobrist hash value

This extra data enables **make/undo move**: the engine can revert the board to its exact prior state without a separate state stack, which is standard practice in chess engines.

---

### 3. Move Generation

**File:** `BitboardBoard.java`

The engine generates **pseudo-legal moves** (moves that are legal except they may leave the king in check), then filters out illegal ones. For sliding pieces (bishops, rooks, queens), bitboard ray-attack techniques efficiently compute reachable squares.

Generating pseudo-legal moves first and then filtering is simpler and faster than generating only fully legal moves directly.

---

### 4. Zobrist Hashing

**File:** `Zobrist.java`

Zobrist hashing assigns a random 64-bit number to each (piece, square) combination, plus values for the side to move, en passant squares, and castling rights. The hash of a position is the XOR of all relevant values.

**Why Zobrist hashing:**
- **Incremental updates in O(1):** XOR out the old piece-square key, XOR in the new one, toggle side-to-move and castling/en-passant keys as needed.
- **Order-independent:** The same board position reached via different move sequences always produces the same hash.
- **Near-unique:** 64-bit keys have an astronomically low collision probability, making them effectively unique identifiers for positions.

---

### 5. Transposition Table

**File:** `TranspositionTable.java`

A large hash table (default 2²⁰ = 1,048,576 entries) that caches previously searched positions along with their scores, search depth, and the best move found.

**Why a transposition table:**
- Chess has many **transpositions**: the same position can be reached via different move orders (e.g., 1.e4 e5 2.Nf3 and 1.Nf3 e5 2.e4 reach the same position). Without this table, the engine redundantly searches the same positions.
- On a cache hit, if the stored depth ≥ the requested depth, the engine can return the stored score without searching.
- The stored best move improves move ordering in subsequent searches.

Three entry types are stored:

| Type | Meaning |
|---|---|
| `EXACT` | The exact score for the position |
| `LOWERBOUND` | Score is at least this value (from a beta cutoff) |
| `UPPERBOUND` | Score is at most this value (from an alpha cutoff) |

**Replacement policy:** A new entry overwrites an existing one only if it has greater or equal depth, keeping deeper (more valuable) results in the table.

---

### 6. Search Algorithm: Negamax + Alpha-Beta Pruning

**File:** `UCI.java`

The engine uses **negamax**, which exploits the fact that a position's score from one player's perspective is the negative of the opponent's. This avoids alternating between maximizing and minimizing — the search always maximizes and negates.

**Alpha-beta pruning** maintains a search window (alpha, beta). If a move proves the position is at least beta (too good for the current player), the search prunes the remaining moves and returns beta. This cuts away large parts of the tree without changing the result.

**Impact:**
- Without alpha-beta: O(b^d) nodes, where b ≈ 35 (branching factor) and d is depth.
- With perfect move ordering: O(b^(d/2)) — the square root. This effectively doubles searchable depth for the same time.

---

### 7. Iterative Deepening

**File:** `UCI.java`

Rather than searching directly to the target depth, the engine searches depth 1, then 2, then 3, and so on.

**Why iterative deepening:**
- Time at shallow depths is negligible compared to deeper levels (the tree grows exponentially), so the overhead is minimal.
- Each shallow iteration populates the transposition table with good moves, which dramatically improves move ordering at the next depth.
- A valid best move is always available if the search is stopped early.

---

### 8. Move Ordering

**Files:** `UCI.java`, `Benchmark.java`

Alpha-beta pruning is most effective when the best moves are searched first. The engine uses four layers of ordering (highest to lowest priority):

| Priority | Method | Score |
|---|---|---|
| 1 | **Transposition Table best move** — the move that was best in the last search of this position | 10,000,000 |
| 2 | **MVV-LVA captures** — Most Valuable Victim / Least Valuable Attacker. Formula: `victim_value × 100 − attacker_value`. Captures a queen with a pawn score highest. | 1,000,000 + MVV-LVA |
| 3 | **Killer moves** — quiet moves that caused a beta cutoff at the same depth in sibling nodes (two stored per depth) | 500,000 − killer_index × 1,000 |
| 4 | **History heuristic** — a 64×64 table tracking how often each (from, to) quiet move caused beta cutoffs; incremented by `depth²` per cutoff | history value |

With random ordering, alpha-beta degenerates to brute-force. Good ordering can reduce node counts by 90–99%.

---

### 9. Piece-Square Tables (PSQT)

**Files:** `UCI.java`, `Benchmark.java`

Piece-square tables add positional bonuses to evaluation based on where each piece stands. There are six 64-element tables (one per piece type). For black pieces, the table is mirrored vertically by XORing the square index with 56.

**What each table rewards:**

| Piece | Reward |
|---|---|
| Pawn | Center control and advancement; large bonus near promotion |
| Knight | Central squares (+20 on d4/d5/e4/e5); penalties on edge squares (−50) |
| Bishop | Central and long-diagonal squares (+10 center, −20 corners) |
| Rook | Center files and the 7th rank (attacking unadvanced pawns) |
| Queen | Modest central bonus (+5); corner penalties |
| King | Castled corners (+30); center exposure penalized (−50) |

PSQT alone typically adds 200–300 Elo compared to a purely material-based evaluation.

---

### 10. Null-Move Pruning

**Files:** `UCI.java`, `Benchmark.java`

Based on the observation that in most positions, having the move is an advantage. If after **passing** (a "null move") and searching at a reduced depth the position still scores above beta, the original position is too good and can be pruned.

**Implementation:** Toggle the side to move (updating the Zobrist hash), search at `depth − 3` with a null window `(−beta, −beta + 1)`. If the result ≥ beta, return beta.

**Conditions required:**
- Depth ≥ 3
- The side to move is not in check
- The side to move has at least one non-pawn piece (prevents errors in **zugzwang** positions)

The non-pawn condition disables null-move pruning in king-and-pawn endgames, where passing can be a disadvantage. This is the standard approach used by Stockfish and other top engines.

---

### 11. Late Move Reductions (LMR)

**Files:** `UCI.java`, `Benchmark.java`

After the first few moves in the sorted list, remaining moves are almost certainly bad. Searching them at full depth is wasteful.

**Implementation:** The first 4 moves (indices 0–3) are searched at full depth. After that, quiet moves (non-captures) are searched at a reduced depth:

| Move index | Reduction (R) |
|---|---|
| 4–7 | depth − 2 |
| 8–11 | depth − 3 |
| 12+ | depth − 4 |

If a reduced-depth search beats alpha, the move is re-searched at full depth. Captures are never reduced.

**Why this is safe:** Move ordering ensures the first few moves are the best candidates (TT move, winning captures, killers). Reduced searches quickly confirm the rest are bad, and the re-search recovers any exceptions.

LMR typically reduces node counts by 60–75% and adds roughly 200 Elo.

---

### 12. Quiescence Search

**File:** `UCI.java`

The **horizon effect** occurs when a search stops at a fixed depth, missing a capturing sequence that changes the evaluation entirely. For example, at depth 5 the engine may evaluate a position as equal, but at depth 6 it would see the queen can be captured.

Quiescence search continues past the nominal depth, searching only captures (and sometimes checks). **Delta pruning** stops the search early when even capturing the most valuable opponent piece cannot raise the score to alpha, avoiding time spent on hopeless captures.

Without quiescence search, the engine makes tactical blunders on positions just beyond its search horizon.

---

### 13. Evaluation Function

**Files:** `UCI.java`, `Benchmark.java`

The evaluation assigns a score from White's perspective (positive = White advantage, negative = Black advantage).

**Two components:**

| Component | Detail |
|---|---|
| Material | Pawn = 100, Knight = 320, Bishop = 330, Rook = 500, Queen = 900, King = 20,000 (centipawns) |
| Position | Piece-square table bonuses and penalties per piece placement |

**Total score = Σ (material + positional bonus) for all pieces on the board.**

Centipawns (1/100 of a pawn) provide sufficient evaluation granularity while keeping all arithmetic as integers.

---

### 14. UCI Protocol

**File:** `UCI.java`

The engine implements the Universal Chess Interface, a text-based protocol for GUI communication.

| Command | Behavior |
|---|---|
| `uci` | Announces engine name and available options |
| `isready` | Responds with `readyok` |
| `ucinewgame` | Clears the transposition table |
| `position [fen \| startpos] moves ...` | Sets up a board position |
| `go depth N / wtime / btime / winc / binc / movestogo / movetime / infinite` | Starts searching |
| `stop` | Stops the current search |
| `quit` | Exits the engine |

The search runs on a **separate thread**, allowing the engine to respond to `stop` commands during search. It outputs `info` lines (depth, score, principal variation) and finishes with `bestmove <move>`.

---

### 15. Time Management

**File:** `UCI.java`

Allocates available clock time across moves to avoid time trouble.

**Supported `go` parameters:**

| Parameter | Meaning |
|---|---|
| `wtime` / `btime` | Remaining time for white/black in milliseconds |
| `winc` / `binc` | Fischer increment per move in milliseconds |
| `movestogo` | Moves until next time control |
| `movetime` | Fixed time per move (overrides other controls) |

**Allocation strategy:**

1. If `movetime` is specified, use it directly.
2. Otherwise: divide remaining time by expected remaining moves (default 40, or `movestogo` if given), add the increment, and subtract a safety margin (up to 2,000 ms or 20% of remaining time). Minimum search time is 10 ms.

Time is checked in the iterative deepening loop — if the allocation expires before reaching the target depth, the engine returns the best move from the last completed depth.

---

## Playing Strength

Based on the features implemented, the engine is estimated at approximately **1400–1600 Elo** (standard human rating scale). This corresponds to a strong club-player level — it will consistently beat casual players but will struggle against masters and modern engines.

The primary bottleneck is the evaluation function, which only uses material and piece-square tables. Adding pawn structure evaluation, king safety, and piece mobility would significantly improve strength.

---

## Potential Improvements

The engine has a solid foundation. The most impactful next steps are:

1. **Aspiration Windows** — Search with a narrow window around the previous iteration's score instead of (−∞, +∞). Re-search with a wider window on a miss. Reduces search tree size in stable positions.

2. **Static Exchange Evaluation (SEE)** — Compute whether a capture actually wins or loses material, replacing the simpler MVV-LVA heuristic. Prevents wasting time on losing captures.

3. **Check Extensions** — Extend search depth by one ply when a move gives check. Checks are tactically significant and can be wrongly pruned by LMR or null-move pruning without extensions.

4. **Pawn Structure Evaluation** — Evaluate doubled pawns, isolated pawns, passed pawns, connected pawns, and pawn chains. This is the largest missing piece in the evaluation function.

5. **King Safety** — Evaluate the pawn shield around the king and open files near it. Penalize poorly defended king positions.

6. **Piece Mobility** — Count legal moves available to each piece and add a bonus for higher mobility.

7. **Multi-Threaded Search** — Use multiple threads (e.g., Lazy SMP) to search the tree in parallel. Requires a more complex search architecture but scales with CPU cores.

---

## Legacy Code

The following files are from the initial implementation before the bitboard engine was completed. They are not used by the current engine and are kept for reference only:

- `game/Board.java`, `game/Game.java`, `game/Main.java`, `game/Move.java`
- `pieces/Piece.java`, `pieces/Pawn.java`, `pieces/King.java`, etc.

The current engine uses only `game.UCI` and its supporting classes: `BitboardBoard`, `BitMove`, `TranspositionTable`, and `Zobrist`.

---

## License

This project is for educational purposes. Feel free to study, modify, and learn from the code.