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

# Puertos y nombres salen de entorno.ps1 (variables del .env).
. "$PSScriptRoot\entorno.ps1"

# Ojo: las claves van entre comillas a proposito. Si fueran numeros,
# PowerShell interpretaria $ESPERADOS[8090] como la POSICION 8090 del
# diccionario en vez de la clave, y devolveria $null.
# El gateway va primero porque es la puerta de entrada.
$ESPERADOS = [ordered]@{}
$gateway = $CATALOGO | Where-Object { $_.Carpeta -eq "ms-api-gateway" }
$ESPERADOS["$($gateway.Puerto)"] = $gateway.Nombre
foreach ($s in ($CATALOGO | Where-Object { $_.Carpeta -ne "ms-api-gateway" })) {
    $ESPERADOS["$($s.Puerto)"] = $s.Nombre
}

Write-Host ""
Write-Host "  Teleticket - estado de los servicios" -ForegroundColor Cyan
Write-Host "  ---------------------------------------------------------------" -ForegroundColor Cyan
Write-Host ("  {0,-7} {1,-20} {2,-10} {3}" -f "PUERTO", "ESPERADO", "/health", "/ping") -ForegroundColor Gray

$vivos = 0
foreach ($puerto in $ESPERADOS.Keys) {
    $esperado = $ESPERADOS[$puerto]

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
if ($vivos -eq $ESPERADOS.Count) {
    Write-Host "  $vivos/$($ESPERADOS.Count) servicios vivos. Fase 0 verificada." -ForegroundColor Green
} else {
    Write-Host "  $vivos/$($ESPERADOS.Count) servicios vivos." -ForegroundColor Yellow
    Write-Host "  Si alguno acaba de arrancar, espera unos segundos y repite." -ForegroundColor Gray
}
Write-Host ""
