@echo off
setlocal enabledelayedexpansion

echo Cargando variables de entorno desde .env de forma segura...
for /f "delims=" %%i in ('powershell -NoProfile -Command "Get-Content .env | Where-Object { $_ -match '=' }"') do (
    set "%%i"
)

if not exist logs mkdir logs

set MODULES=eureka-server api-gateway ms-usuarios ms-autenticacion ms-solicitudes ms-ubicacion ms-pagos ms-valoraciones ms-maestria ms-notificacion ms-promociones ms-reportes

echo.
echo ========================================================
echo INICIANDO MICROSERVICIOS EN SEGUNDO PLANO
echo (Los logs detallados de cada uno se guardan en \logs)
echo ========================================================
echo.

for %%m in (%MODULES%) do (
    echo [%%m] Iniciando...
    if exist logs\%%m.log del logs\%%m.log
    
    REM Lanza el servicio en el background (misma ventana) y guarda el output en su log
    start /b cmd /c ".\mvnw.cmd spring-boot:run -pl %%m > logs\%%m.log 2>&1"
    
    call :wait_for_startup %%m
)
goto :end

:wait_for_startup
set "module=%1"
set max_retries=60
set retry=0
:loop
timeout /t 2 /nobreak >nul
findstr /c:"Started " logs\%module%.log >nul 2>&1
if not errorlevel 1 (
    echo [%module%] INICIADO CORRECTAMENTE!
    echo.
    exit /b
)
findstr /c:"BUILD FAILURE" logs\%module%.log >nul 2>&1
if not errorlevel 1 (
    echo [%module%] ERROR FATAL: El servicio fallo al compilar o iniciar. Revisa logs\%module%.log
    echo.
    exit /b
)
set /a retry+=1
if %retry% LSS %max_retries% goto loop
echo [%module%] ADVERTENCIA: Tardo mas de 2 minutos. Verifica logs\%module%.log
echo.
exit /b

:end
echo ========================================================
echo TODOS LOS SERVICIOS ESTAN CORRIENDO EN ESTA VENTANA.
echo ¡Por favor NO CIERRES esta ventana negra! (Solo minimizala).
echo Para apagarlos, presiona Ctrl+C varias veces.
echo.
echo Para exponer tu API Gateway a Internet con Cloudflare:
echo cloudflared tunnel --url http://localhost:8080
echo ========================================================
pause >nul
