# =====================================================================
#  Teleticket - verificar la infraestructura local
#  Fase 1
# =====================================================================
#
#  Comprueba que PostgreSQL (con sus 9 bases), RabbitMQ y MailHog
#  esten levantados y respondiendo. Es la prueba manual de la fase 1.
#
#  USO:  .\verificar-infra.ps1
# =====================================================================

$ErrorActionPreference = "Continue"
$fallos = 0

Write-Host ""
Write-Host "  Teleticket - infraestructura local" -ForegroundColor Cyan
Write-Host "  ===============================================================" -ForegroundColor Cyan

# ---------------------------------------------------------------------
# 1. El daemon de Docker
# ---------------------------------------------------------------------
Write-Host ""
Write-Host "  [1] Docker" -ForegroundColor White

$version = docker info --format "{{.ServerVersion}}" 2>$null
if ($LASTEXITCODE -ne 0 -or -not $version) {
    Write-Host "      X  El daemon no responde." -ForegroundColor Red
    Write-Host "         Abri Docker Desktop y espera a que el icono de la" -ForegroundColor Gray
    Write-Host "         ballena deje de animarse (~1 minuto)." -ForegroundColor Gray
    Write-Host ""
    exit 1
}
Write-Host "      OK  daemon activo (version $version)" -ForegroundColor Green

# ---------------------------------------------------------------------
# 2. Los tres contenedores
# ---------------------------------------------------------------------
Write-Host ""
Write-Host "  [2] Contenedores" -ForegroundColor White

$ESPERADOS = @("teleticket-postgres", "teleticket-rabbitmq", "teleticket-mailhog")

foreach ($nombre in $ESPERADOS) {
    # .State es running/exited; .Health es healthy/unhealthy/starting
    # (vacio si la imagen no define healthcheck, como MailHog).
    $estado = docker inspect --format "{{.State.Status}}" $nombre 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host ("      X  {0,-24} no existe" -f $nombre) -ForegroundColor Red
        $fallos++
        continue
    }

    $salud = docker inspect --format "{{if .State.Health}}{{.State.Health.Status}}{{else}}sin healthcheck{{end}}" $nombre 2>$null

    if ($estado -eq "running" -and ($salud -eq "healthy" -or $salud -eq "sin healthcheck")) {
        Write-Host ("      OK  {0,-24} {1} / {2}" -f $nombre, $estado, $salud) -ForegroundColor Green
    }
    elseif ($estado -eq "running" -and $salud -eq "starting") {
        Write-Host ("      ..  {0,-24} arrancando todavia - espera y repite" -f $nombre) -ForegroundColor Yellow
        $fallos++
    }
    else {
        Write-Host ("      X  {0,-24} {1} / {2}" -f $nombre, $estado, $salud) -ForegroundColor Red
        Write-Host ("         Mira el log:  docker compose logs {0}" -f ($nombre -replace "teleticket-","")) -ForegroundColor Gray
        $fallos++
    }
}

# ---------------------------------------------------------------------
# 3. Las 9 bases de datos
# ---------------------------------------------------------------------
Write-Host ""
Write-Host "  [3] Bases de datos en PostgreSQL (puerto 5433)" -ForegroundColor White

$BASES = @("db_usuarios", "db_eventos", "db_cola", "db_inventario", "db_ordenes",
           "db_pagos", "db_tickets", "db_notificaciones", "db_reclamos")

# -t = sin cabeceras, -A = sin alineacion: salida limpia para comparar.
$listado = docker exec teleticket-postgres psql -U teleticket -d postgres -tA -c "SELECT datname FROM pg_database;" 2>$null

if ($LASTEXITCODE -ne 0) {
    Write-Host "      X  No se pudo consultar PostgreSQL" -ForegroundColor Red
    $fallos++
} else {
    $existentes = $listado -split "`n" | ForEach-Object { $_.Trim() }
    $encontradas = 0
    foreach ($base in $BASES) {
        if ($existentes -contains $base) {
            Write-Host ("      OK  {0}" -f $base) -ForegroundColor Green
            $encontradas++
        } else {
            Write-Host ("      X  {0}  NO EXISTE" -f $base) -ForegroundColor Red
            $fallos++
        }
    }
    if ($encontradas -eq 0) {
        Write-Host "         El volumen ya existia antes de agregar el script." -ForegroundColor Gray
        Write-Host "         Solucion:  docker compose down -v ; docker compose up -d" -ForegroundColor Gray
    }
}

# ---------------------------------------------------------------------
# 4. Las consolas web
# ---------------------------------------------------------------------
Write-Host ""
Write-Host "  [4] Consolas web" -ForegroundColor White

$WEBS = @(
    @{ Nombre = "RabbitMQ"; Url = "http://localhost:15672" },
    @{ Nombre = "MailHog";  Url = "http://localhost:8025" }
)

foreach ($web in $WEBS) {
    try {
        $resp = Invoke-WebRequest -Uri $web.Url -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
        if ($resp.StatusCode -eq 200) {
            Write-Host ("      OK  {0,-10} {1}" -f $web.Nombre, $web.Url) -ForegroundColor Green
        } else {
            Write-Host ("      X  {0,-10} codigo {1}" -f $web.Nombre, $resp.StatusCode) -ForegroundColor Red
            $fallos++
        }
    } catch {
        Write-Host ("      X  {0,-10} sin respuesta en {1}" -f $web.Nombre, $web.Url) -ForegroundColor Red
        $fallos++
    }
}

# ---------------------------------------------------------------------
# Resumen
# ---------------------------------------------------------------------
Write-Host ""
Write-Host "  ===============================================================" -ForegroundColor Cyan
if ($fallos -eq 0) {
    Write-Host "  Todo correcto. Fase 1 verificada." -ForegroundColor Green
    Write-Host ""
    Write-Host "  Abri en el navegador:" -ForegroundColor Gray
    Write-Host "      http://localhost:15672   RabbitMQ  (guest / guest)" -ForegroundColor White
    Write-Host "      http://localhost:8025    MailHog" -ForegroundColor White
} else {
    Write-Host "  $fallos comprobacion(es) fallaron." -ForegroundColor Yellow
    Write-Host "  Revisa infra\README.md, seccion 'Si algo sale mal'." -ForegroundColor Gray
}
Write-Host ""
