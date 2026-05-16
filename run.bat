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
java -Dfile.encoding=UTF-8 -cp out game.Main
pause
