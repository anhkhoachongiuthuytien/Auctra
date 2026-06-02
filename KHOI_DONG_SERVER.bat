@echo off
title Auctra - Socket Server
color 0E
echo =======================================================
echo              AUCTRA - SOCKET SERVER
echo         Dang khoi dong Socket Server...
echo =======================================================
echo.

cd /d "%~dp0"

echo [1/2] Kiem tra va build du an...
call mvn install -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo [LOI] Khong the build du an.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [2/2] Khoi dong Socket Server tai cong 9999...
echo Nhan Ctrl+C de dung server.
echo.
java -jar auction-server/target/auction-server.jar

pause
