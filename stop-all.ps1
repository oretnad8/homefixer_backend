# stop-all.ps1
# Cierra las instancias previas de HomeFixer: los microservicios (por su puerto),
# los "mvnw spring-boot:run" que los lanzaron y el "next dev" del frontend.
# start-all.bat lo ejecuta al comienzo; también se puede usar solo para apagar todo.

$puertos = @(
    8761, # eureka-server
    8080, # api-gateway
    8081, # ms-usuarios
    8082, # ms-solicitudes
    8083, # ms-autenticacion
    8084, # ms-maestria
    8085, # ms-ubicacion
    8086, # ms-notificacion
    8087, # ms-pagos
    8088, # ms-valoraciones
    8089, # ms-promociones
    8090, # ms-reportes
    8091, # ms-kafka-admin
    3000  # fronend-home (next dev)
)

$procesos = @()

# Procesos que ocupan los puertos del proyecto, en CUALQUIER estado (no solo Listen):
# un servicio colgado a mitad de arranque deja el socket en estado "Bound" y, si no se
# mata, el siguiente intento falla con "Port XXXX was already in use".
$procesos += Get-NetTCPConnection -LocalPort $puertos -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess

# Maven (spring-boot:run), servicios lanzados a mano / desde el IDE (java con las clases
# compiladas de este proyecto en el classpath) y Next.js que pudieran quedar sin puerto
$procesos += Get-CimInstance Win32_Process | Where-Object {
    ($_.Name -eq 'java.exe' -and $_.CommandLine -like '*spring-boot:run*') -or
    ($_.Name -eq 'java.exe' -and $_.CommandLine -like '*cloud.native\homefixer\*\target\classes*') -or
    ($_.Name -eq 'node.exe' -and $_.CommandLine -like '*fronend-home*')
} | Select-Object -ExpandProperty ProcessId

$procesos = $procesos | Where-Object { $_ -gt 4 } | Sort-Object -Unique

if (-not $procesos) {
    Write-Host "No habia procesos previos de HomeFixer."
    exit 0
}

foreach ($id in $procesos) {
    $p = Get-Process -Id $id -ErrorAction SilentlyContinue
    if ($p) {
        Write-Host "Cerrando $($p.ProcessName) (PID $id)"
        Stop-Process -Id $id -Force -ErrorAction SilentlyContinue
    }
}

# Espera a que los puertos queden libres (máx. ~15 s)
for ($i = 0; $i -lt 15; $i++) {
    if (-not (Get-NetTCPConnection -State Listen -LocalPort $puertos -ErrorAction SilentlyContinue)) { break }
    Start-Sleep -Seconds 1
}
Write-Host "Procesos previos cerrados."
