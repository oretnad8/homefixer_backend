# start-all.ps1
Write-Host "Cargando variables de entorno desde .env..." -ForegroundColor Cyan
Get-Content .env | Where-Object { $_ -match '=' } | ForEach-Object {
    $parts = $_.Split('=', 2)
    [Environment]::SetEnvironmentVariable($parts[0], $parts[1], "Process")
}

$modules = @(
    "eureka-server",
    "api-gateway",
    "ms-usuarios",
    "ms-autenticacion",
    "ms-solicitudes",
    "ms-ubicacion",
    "ms-pagos",
    "ms-valoraciones",
    "ms-maestria",
    "ms-notificacion",
    "ms-promociones",
    "ms-reportes"
)

Write-Host "Iniciando todos los microservicios en ventanas separadas..." -ForegroundColor Cyan

foreach ($mod in $modules) {
    Write-Host "Lanzando $mod..." -ForegroundColor Green
    # Inicia cada microservicio en una nueva ventana de consola
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c title $mod && .\mvnw.cmd spring-boot:run -pl $mod"
    
    # Pausa de 8 segundos entre cada servicio para no colapsar la CPU y RAM de tu PC al arrancar todo junto
    Start-Sleep -Seconds 8 
}

Write-Host "========================================================" -ForegroundColor Yellow
Write-Host "¡Todos los servicios están arrancando!" -ForegroundColor Yellow
Write-Host "Para exponer tu API Gateway a Internet con Cloudflare, ejecuta en otra terminal:" -ForegroundColor Yellow
Write-Host "cloudflared tunnel --url http://localhost:8080" -ForegroundColor White
Write-Host "========================================================" -ForegroundColor Yellow
