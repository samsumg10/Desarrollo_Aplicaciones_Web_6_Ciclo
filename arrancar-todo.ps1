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

# Carga el .env de la raiz y la tabla de servicios con sus puertos.
. "$PSScriptRoot\entorno.ps1"
$raiz = $RAIZ

# Orden de arranque y puertos: ver entorno.ps1 (los puertos salen del .env).
$TODOS = [ordered]@{}
foreach ($s in $CATALOGO) { $TODOS[$s.Carpeta] = $s.Puerto }

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

# La libreria de mensajeria se lee de ~/.m2: en un clon nuevo no esta y los
# servicios que la usan no compilan. Si falta, se instala aqui (necesita red).
$libInstalada = Join-Path $HOME ".m2\repository\com\teleticket\mensajeria-comun"
if (-not (Test-Path $libInstalada)) {
    Write-Host ""
    Write-Host "  [i] libs\mensajeria-comun no esta instalada en ~/.m2. Instalandola..." -ForegroundColor Yellow
    Push-Location (Join-Path $raiz "libs\mensajeria-comun")
    try {
        & .\mvnw.cmd -q clean install -DskipTests
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  [X] No se pudo instalar libs\mensajeria-comun. Revisa el error de arriba." -ForegroundColor Red
            exit 1
        }
    } finally {
        Pop-Location
    }
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
