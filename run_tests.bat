@echo off
chcp 65001 >nul
echo Compiling chess game with bitboard engine...
javac -d out game\*.java pieces\*.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)
echo Compilation successful!
echo.
echo ========================================
echo RUNNING ZOBRIST HASHING TESTS
echo ========================================
java -cp out game.ZobristTest
echo.
if %errorlevel% neq 0 (
    echo ZOBRIST TESTS FAILED!
    pause
    exit /b %errorlevel%
)
echo ========================================
echo RUNNING TRANSPOSITION TABLE TESTS
echo ========================================
java -cp out game.TranspositionTableTest
echo.
if %errorlevel% neq 0 (
    echo TRANSPOSITION TABLE TESTS FAILED!
    pause
    exit /b %errorlevel%
)
echo ========================================
echo RUNNING UCI PROTOCOL TESTS
echo ========================================
java -cp out game.UCITest
echo.
if %errorlevel% neq 0 (
    echo UCI TESTS FAILED!
    pause
    exit /b %errorlevel%
)
echo ========================================
echo RUNNING PHASE 1 TESTS
echo ========================================
java -cp out game.Phase1Test
echo.
if %errorlevel% neq 0 (
    echo PHASE 1 TESTS FAILED!
    pause
    exit /b %errorlevel%
)
echo ========================================
echo RUNNING BENCHMARK
echo ========================================
java -cp out game.Benchmark
echo.
echo ========================================
echo ALL TESTS PASSED!
echo ========================================
pause
