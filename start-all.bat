@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo         INICIANDO ECOSISTEMA HOMEFIXER (CLOUD NATIVE)
echo ========================================================

echo Cargando variables de entorno desde .env de forma segura...
for /f "delims=" %%i in ('powershell -NoProfile -Command "Get-Content .env | Where-Object { $_ -match '=' }"') do (
    set "%%i"
)

echo Cerrando instancias previas de HomeFixer...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-all.ps1"

if not exist logs mkdir logs

REM Cada servicio corre con "spring-boot:run -pl <modulo>" (sin -am): toma el modulo
REM "shared" del repositorio local ~/.m2, asi que se reinstala para no usar una version vieja.
echo Instalando el modulo shared actualizado...
call .\mvnw.cmd -q -pl shared -am install -DskipTests
if errorlevel 1 (
    echo ERROR: no se pudo compilar el modulo shared. Revisa el error de arriba.
    pause
    exit /b 1
)

REM Verificamos si se solicito levantar el cluster de 2 nodos para la presentacion EP4.
REM Ojo: dentro de un bloque if ( ... ) los parentesis de un echo deben escaparse con ^
REM o cmd cierra el bloque antes de tiempo y la ventana se cierra con error de sintaxis.
REM Ambos modos publican el puerto 5672/15672, asi que se detiene el otro antes de levantar.
if /i "%~1"=="cluster" (
    echo.
    echo Levantando RabbitMQ en MODO CLUSTER ^(2 nodos: rabbitmq1 + rabbitmq2^)...
    echo Requisito EP4: 2 nodos integrados en cluster.
    docker compose stop rabbitmq >nul 2>&1
    docker compose -f docker-compose.cluster.yml up -d --wait
) else (
    echo.
    echo Levantando RabbitMQ en modo estandar ^(1 nodo^)...
    echo Nota: para el cluster de 2 nodos de la presentacion EP4 usa: start-all.bat cluster
    docker compose -f docker-compose.cluster.yml stop >nul 2>&1
    docker compose up -d --wait rabbitmq
)
if errorlevel 1 (
    echo ERROR: RabbitMQ no quedo saludable. Revisa que Docker Desktop este abierto.
    pause
    exit /b 1
)

set MODULES=eureka-server api-gateway ms-usuarios ms-autenticacion ms-solicitudes ms-ubicacion ms-pagos ms-valoraciones ms-maestria ms-notificacion ms-promociones ms-reportes ms-kafka-admin

echo.
echo ========================================================
echo INICIANDO MICROSERVICIOS EN SEGUNDO PLANO
echo (Los logs detallados de cada uno se guardan en \logs)
echo ========================================================
echo.

REM Intentos por servicio: a veces el pooler de Supabase no acepta la conexion a tiempo
REM ("Connect timed out") y el servicio cae al arrancar; se reintenta antes de seguir.
set MAX_ATTEMPTS=3

for %%m in (%MODULES%) do call :start_module %%m

REM El frontend se sirve en modo produccion (build + start): en "next dev" los chunks JS no
REM cambian de nombre y Cloudflare/el navegador del movil seguian ejecutando codigo viejo.
echo [fronend-home] Compilando (npm run build)...
if exist logs\frontend.log del logs\frontend.log
pushd fronend-home
call npm run build > ..\logs\frontend.log 2>&1
if errorlevel 1 (
    popd
    echo [fronend-home] ERROR: fallo el build. Revisa logs\frontend.log
    goto :end
)
popd
echo [fronend-home] Iniciando (npm run start)...
start /b cmd /c "cd fronend-home && npm run start >> ..\logs\frontend.log 2>&1"

goto :end

REM Lanza un servicio en el background (misma ventana) guardando su output en su log.
REM Si cae al iniciar, guarda ese log como <modulo>.fallido.log y lo vuelve a lanzar.
:start_module
set "module=%~1"
set attempt=1
echo [%module%] Iniciando...
:start_attempt
if exist logs\%module%.log del logs\%module%.log
start /b cmd /c ".\mvnw.cmd spring-boot:run -pl %module% > logs\%module%.log 2>&1"
call :wait_for_startup %module%
if not "%startup_result%"=="fallo" exit /b
if %attempt% GEQ %MAX_ATTEMPTS% (
    echo [%module%] ERROR FATAL: fallo tras %MAX_ATTEMPTS% intentos. Revisa logs\%module%.log
    echo.
    exit /b
)
set /a attempt+=1
REM Pausa para que Maven suelte el archivo de log antes de moverlo
timeout /t 5 /nobreak >nul
move /y logs\%module%.log logs\%module%.fallido.log >nul 2>&1
echo [%module%] Fallo al iniciar. Reintentando ^(intento %attempt%/%MAX_ATTEMPTS%^)...
goto start_attempt

:wait_for_startup
set "module=%1"
set "startup_result=ok"
set max_retries=60
set retry=0
:loop
timeout /t 2 /nobreak >nul
findstr /c:"Started " logs\%module%.log >nul 2>&1
if not errorlevel 1 (
    echo [%module%] INICIADO CORRECTAMENTE
    echo.
    exit /b
)
findstr /c:"BUILD FAILURE" logs\%module%.log >nul 2>&1
if not errorlevel 1 (
    set "startup_result=fallo"
    exit /b
)
set /a retry+=1
if %retry% LSS %max_retries% goto loop
REM Sigue arrancando (no fallo): no se relanza para no duplicar el proceso
echo [%module%] ADVERTENCIA: Tardo mas de 2 minutos. Verifica logs\%module%.log
echo.
exit /b

:end
echo ========================================================
echo         TODOS LOS SERVICIOS ESTAN ACTIVOS
echo ========================================================
echo.
echo Accesos Principales:
echo  - Frontend Web:        http://localhost:3000
echo  - API Gateway:         http://localhost:8080
echo  - Eureka Discovery:    http://localhost:8761
echo  - RabbitMQ Management: http://localhost:15672 (guest / guest)
echo.
echo Endpoints Clave para la Evaluacion de Mensajeria (Rubrica 100%%):
echo  - Monitor de Colas:    GET  http://localhost:8080/api/notificaciones/mensajeria/colas
echo  - Admin de Colas:      POST http://localhost:8080/api/admin/queues
echo  - RPC Resumen Sol.:    GET  http://localhost:8080/api/reportes/solicitudes/resumen
echo.
echo Para exponer tu API Gateway a Internet con Cloudflare:
echo  cloudflared tunnel --url http://localhost:8080
echo ========================================================
echo Por favor NO CIERRES esta ventana (Solo minimizala).
echo Para apagar todos los servicios, presiona Ctrl+C varias veces.
echo ========================================================

REM Desde aqui la ventana muestra en vivo la mensajeria de todos los microservicios
REM (publicacion, confirm del broker, consumo, ACK/NACK, reintentos, DLQ, RPC, alertas).
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0rabbitmq-live.ps1"
pause >nul
