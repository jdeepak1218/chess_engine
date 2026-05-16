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
echo DeepakChess running in UCI mode
echo Connect with Arena, CuteChess, or any UCI-compatible GUI
echo Or type UCI commands manually (type "quit" to exit)
echo.
java -cp out game.UCI
