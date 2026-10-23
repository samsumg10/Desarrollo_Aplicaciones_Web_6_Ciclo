# =====================================================================
#  Teleticket - arrancar todos los microservicios
#  Fase 0 (cimientos)
# =====================================================================
#
#  Abre una ventana de PowerShell por cada microservicio. Cada ventana
#  queda con su propio log a la vista, que es justo lo que se necesita
#  para seguir una compra pasando por varios servicios.
#
#  USO:
#     .\arrancar-todo.ps1              arranca los 9 servicios
#     .\arrancar-todo.ps1 ms-eventos   arranca solo uno
#     .\arrancar-todo.ps1 ms-eventos,ms-cola
#
#  Para detenerlos:  .\detener-todo.ps1   (o cierra cada ventana)
# =====================================================================

param(
    [string[]] $Servicios
)

$ErrorActionPreference = "Stop"
$raiz = $PSScriptRoot

# Orden de arranque: primero los de negocio, el gateway al final, para que
# cuando el gateway empiece a enrutar (fase 2) sus destinos ya esten vivos.
$TODOS = [ordered]@{
    "ms-usuarios"       = 8081
    "ms-eventos"        = 8082
    "ms-cola"           = 8083
    "ms-inventario"     = 8084
    "ms-ordenes"        = 8085
    "ms-pagos"          = 8086
    "ms-teletickets"    = 8087
    "ms-notificaciones" = 8088
    # El gateway usa 8090 y no 8080: ver la desviacion documentada en
    # docs\fases\FASE-01-infraestructura.md
    "ms-api-gateway"    = 8090
}

if ($Servicios) {
    $aLevantar = [ordered]@{}
    foreach ($s in $Servicios) {
        if (-not $TODOS.Contains($s)) {
            Write-Host "Servicio desconocido: $s" -ForegroundColor Red
            Write-Host "Disponibles: $($TODOS.Keys -join ', ')"
            exit 1
        }
        $aLevantar[$s] = $TODOS[$s]
    }
} else {
    $aLevantar = $TODOS
}

Write-Host ""
Write-Host "  Teleticket - arrancando $($aLevantar.Count) servicio(s)" -ForegroundColor Cyan
Write-Host "  ----------------------------------------------------" -ForegroundColor Cyan

foreach ($servicio in $aLevantar.Keys) {
    $puerto = $aLevantar[$servicio]
    $ruta   = Join-Path $raiz $servicio

    if (-not (Test-Path (Join-Path $ruta "pom.xml"))) {
        Write-Host ("  [X] {0,-20} no se encontro el pom.xml" -f $servicio) -ForegroundColor Red
        continue
    }

    # Aviso si el puerto ya esta ocupado: evita el error confuso de
    # "Port 8082 was already in use" dentro de una ventana que se cierra sola.
    $ocupado = Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue
    if ($ocupado) {
        Write-Host ("  [!] {0,-20} puerto {1} YA OCUPADO - se omite" -f $servicio, $puerto) -ForegroundColor Yellow
        continue
    }

    # -NoExit deja la ventana abierta aunque el servicio falle, para poder
    # leer el error. El titulo ayuda a distinguir 9 ventanas iguales.
    $comando = "`$Host.UI.RawUI.WindowTitle = '$servicio :$puerto'; " +
               "Set-Location '$ruta'; " +
               ".\mvnw.cmd spring-boot:run"

    Start-Process powershell -ArgumentList "-NoExit", "-Command", $comando | Out-Null
    Write-Host ("  [>] {0,-20} puerto {1}" -f $servicio, $puerto) -ForegroundColor Green

    # Pequena pausa para no saturar el disco con 9 compilaciones simultaneas.
    Start-Sleep -Milliseconds 1500
}

Write-Host ""
Write-Host "  La primera vez tarda 1-2 minutos (Maven descarga y compila)." -ForegroundColor Gray
Write-Host "  Cuando terminen, comprueba que esten vivos con:" -ForegroundColor Gray
Write-Host "      .\verificar-servicios.ps1" -ForegroundColor White
Write-Host ""
