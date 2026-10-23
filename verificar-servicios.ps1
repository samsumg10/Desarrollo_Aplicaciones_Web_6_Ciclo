# =====================================================================
#  Teleticket - verificar que los microservicios estan vivos
#  Fase 0 (cimientos)
# =====================================================================
#
#  Le pega a /ping y a /actuator/health de cada servicio y muestra una
#  tabla con el resultado. Es la prueba manual de la fase 0.
#
#  USO:  .\verificar-servicios.ps1
# =====================================================================

# Ojo: las claves van entre comillas a proposito. Si fueran numeros,
# PowerShell interpretaria $SERVICIOS[8090] como la POSICION 8090 del
# diccionario en vez de la clave, y devolveria $null.
$SERVICIOS = [ordered]@{
    # El gateway usa 8090 y no 8080: ver la desviacion documentada en
    # docs\fases\FASE-01-infraestructura.md
    "8090" = "api-gateway"
    "8081" = "ms-usuarios"
    "8082" = "ms-eventos"
    "8083" = "ms-cola"
    "8084" = "ms-inventario"
    "8085" = "ms-ordenes"
    "8086" = "ms-pagos"
    "8087" = "ms-tickets"
    "8088" = "ms-notificaciones"
}

Write-Host ""
Write-Host "  Teleticket - estado de los servicios" -ForegroundColor Cyan
Write-Host "  ---------------------------------------------------------------" -ForegroundColor Cyan
Write-Host ("  {0,-7} {1,-20} {2,-10} {3}" -f "PUERTO", "ESPERADO", "/health", "/ping") -ForegroundColor Gray

$vivos = 0
foreach ($puerto in $SERVICIOS.Keys) {
    $esperado = $SERVICIOS[$puerto]

    # -UseBasicParsing y un timeout corto para que no se quede colgado
    # si un servicio esta a medio arrancar.
    try {
        $health = Invoke-RestMethod -Uri "http://localhost:$puerto/actuator/health" `
                                    -TimeoutSec 3 -ErrorAction Stop
        $estadoHealth = $health.status
    } catch {
        $estadoHealth = "-"
    }

    try {
        $ping = Invoke-RestMethod -Uri "http://localhost:$puerto/ping" `
                                  -TimeoutSec 3 -ErrorAction Stop
        $nombreReal = $ping.servicio
    } catch {
        $nombreReal = $null
    }

    if ($nombreReal -eq $esperado -and $estadoHealth -eq "UP") {
        Write-Host ("  {0,-7} {1,-20} {2,-10} {3}" -f $puerto, $esperado, $estadoHealth, "OK") -ForegroundColor Green
        $vivos++
    }
    elseif ($nombreReal -and $nombreReal -ne $esperado) {
        # Sintoma tipico de puertos cruzados en el application.properties.
        Write-Host ("  {0,-7} {1,-20} {2,-10} {3}" -f $puerto, $esperado, $estadoHealth, "RESPONDE '$nombreReal'") -ForegroundColor Yellow
    }
    else {
        Write-Host ("  {0,-7} {1,-20} {2,-10} {3}" -f $puerto, $esperado, $estadoHealth, "SIN RESPUESTA") -ForegroundColor Red
    }
}

Write-Host "  ---------------------------------------------------------------" -ForegroundColor Cyan
if ($vivos -eq $SERVICIOS.Count) {
    Write-Host "  $vivos/$($SERVICIOS.Count) servicios vivos. Fase 0 verificada." -ForegroundColor Green
} else {
    Write-Host "  $vivos/$($SERVICIOS.Count) servicios vivos." -ForegroundColor Yellow
    Write-Host "  Si alguno acaba de arrancar, espera unos segundos y repite." -ForegroundColor Gray
}
Write-Host ""
