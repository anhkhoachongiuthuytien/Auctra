@echo off
title Auctra - Client Socket
color 0A
echo =======================================================
echo              AUCTRA - SOCKET CLIENT
echo         Dang ket noi toi localhost cong 9999...
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
echo [2/2] Dang chay giao dien client ket noi toi localhost:9999...
cd auction-client
call mvn javafx:run "-Djavafx.args=--socket localhost 9999"

echo.
echo Da dong ung dung.
pause
