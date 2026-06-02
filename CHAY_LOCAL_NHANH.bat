@echo off
title Auctra - Chay Che Do Local
color 0B
echo =======================================================
echo              AUCTRA - HE THONG DAU GIA
echo         Dang khoi dong ung dung (Che do LOCAL)...
echo =======================================================
echo.

cd /d "%~dp0"

echo [1/3] Kiem tra va build du an...
call mvn install -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo [LOI] Khong the build du an. Vui long kiem tra lai moi truong Java/Maven.
    pause
    exit /b %ERRORLEVEL%
)

echo [2/3] Da build thanh cong!
echo [3/3] Dang chay giao dien Auctra...
cd auction-client
call mvn javafx:run

echo.
echo Da dong ung dung.
pause
