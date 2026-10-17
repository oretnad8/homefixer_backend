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
    3000  # fronend-home (next dev)
)

$procesos = @()

# Procesos escuchando en los puertos del proyecto
$procesos += Get-NetTCPConnection -State Listen -LocalPort $puertos -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess

# Maven (spring-boot:run) y Next.js que pudieran quedar sin puerto (p. ej. aún arrancando)
$procesos += Get-CimInstance Win32_Process | Where-Object {
    ($_.Name -eq 'java.exe' -and $_.CommandLine -like '*spring-boot:run*') -or
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
