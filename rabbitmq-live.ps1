# rabbitmq-live.ps1
# Muestra en tiempo real, en una sola consola, todo lo que pasa por RabbitMQ en los
# microservicios: publicacion de eventos, confirmacion del broker, consumo, ACK/NACK,
# reintentos, DLQ, RPC, alertas del monitor de colas y administracion de colas.
# start-all.bat lo ejecuta al final; tambien se puede lanzar solo:
#   powershell -ExecutionPolicy Bypass -File rabbitmq-live.ps1

param(
    [string]$LogDir = (Join-Path $PSScriptRoot 'logs'),
    [switch]$DesdeElInicio   # muestra tambien lo ya escrito en los logs, no solo lo nuevo
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# Etiquetas que emiten los servicios (ver MENSAJERIA.md) y su color en consola
$colores = [ordered]@{
    '[ALERTA]'  = 'Red'
    '[NACK]'    = 'Red'
    '[RETRY]'   = 'Yellow'
    '[PUBLISH]' = 'Cyan'
    '[CONFIRM]' = 'DarkCyan'
    '[CONSUME]' = 'White'
    '[ACK]'     = 'Green'
    '[RPC]'     = 'Magenta'
    '[ADMIN]'   = 'DarkYellow'
}
$filtro = '\[(PUBLISH|CONFIRM|CONSUME|RETRY|ACK|NACK|RPC|ALERTA|ADMIN|RETURN)\]|Notificaci.n \[|Calificaci.n del t.cnico'

# Formato Spring Boot: "2026-10-24T09:15:21.892-03:00  INFO 19424 --- [ms-x] [hilo] logger : mensaje"
$springLine = '^\d{4}-\d{2}-\d{2}T(\d{2}:\d{2}:\d{2}\.\d{3})\S*\s+(\w+)\s+\d+\s+---\s+\[([^\]]+)\]\s+\[[^\]]*\]\s+\S+\s+:\s(.*)$'

$utf8 = New-Object System.Text.UTF8Encoding($false)
$lectores = @{}   # archivo -> @{ Stream; Reader; Resto }

function Abrir-Log($ruta) {
    $fs = [System.IO.File]::Open($ruta, 'Open', 'Read', 'ReadWrite, Delete')
    if (-not $DesdeElInicio) { $fs.Seek(0, 'End') | Out-Null }
    @{ Stream = $fs; Reader = New-Object System.IO.StreamReader($fs, $utf8); Resto = '' }
}

function Escribir-Linea($servicio, $linea) {
    if ($linea -notmatch $filtro) { return }
    if ($linea -match $springLine) {
        $hora, $nivel, $servicio, $mensaje = $Matches[1], $Matches[2], $Matches[3], $Matches[4]
    } else {
        $hora = (Get-Date).ToString('HH:mm:ss.fff'); $mensaje = $linea.Trim()
    }
    $color = 'Gray'
    foreach ($tag in $colores.Keys) {
        if ($mensaje.Contains($tag)) { $color = $colores[$tag]; break }
    }
    if ($mensaje -match '\[RETURN\]') { $color = 'Red' }
    Write-Host ("{0} " -f $hora) -NoNewline -ForegroundColor DarkGray
    Write-Host ("{0,-16} " -f $servicio) -NoNewline -ForegroundColor DarkGray
    Write-Host $mensaje -ForegroundColor $color
}

Write-Host ''
Write-Host '========================================================' -ForegroundColor Cyan
Write-Host '   RABBITMQ EN VIVO  (Ctrl+C para salir)' -ForegroundColor Cyan
Write-Host '========================================================' -ForegroundColor Cyan
foreach ($tag in $colores.Keys) { Write-Host ("  {0,-10}" -f $tag) -ForegroundColor $colores[$tag] -NoNewline }
Write-Host ''
Write-Host ''

while ($true) {
    foreach ($archivo in Get-ChildItem -Path $LogDir -Filter 'ms-*.log' -ErrorAction SilentlyContinue) {
        $ruta = $archivo.FullName
        if (-not $lectores.ContainsKey($ruta)) {
            try { $lectores[$ruta] = Abrir-Log $ruta } catch { continue }
        }
        $l = $lectores[$ruta]
        # El log se recreo (servicio reiniciado): se vuelve a leer desde el inicio
        if ($l.Stream.Length -lt $l.Stream.Position) {
            $l.Stream.Seek(0, 'Begin') | Out-Null
            $l.Reader.DiscardBufferedData()
            $l.Resto = ''
        }
        $servicio = $archivo.BaseName
        # Solo se procesan lineas completas: la ultima puede estar a medio escribir
        $texto = $l.Resto + $l.Reader.ReadToEnd()
        if (-not $texto) { continue }
        $lineas = $texto -split "`r?`n"
        $l.Resto = $lineas[-1]
        for ($i = 0; $i -lt $lineas.Count - 1; $i++) {
            if ($lineas[$i]) { Escribir-Linea $servicio $lineas[$i] }
        }
    }
    Start-Sleep -Milliseconds 300
}
