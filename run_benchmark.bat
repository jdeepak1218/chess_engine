@echo off
chcp 65001 >nul
echo Compiling chess game...
javac -d out game\*.java pieces\*.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)
echo Compilation successful!
echo.
echo ========================================
echo RUNNING BENCHMARKS
echo ========================================
echo This will take 30-120 seconds depending on your CPU...
echo.
java -cp out game.Benchmark
echo.
pause
