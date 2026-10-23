# =====================================================================
#  Teleticket - detener todos los microservicios
#  Fase 0 (cimientos)
# =====================================================================
#
#  Busca que proceso esta escuchando en cada puerto del proyecto
#  (los de entorno.ps1: 8081-8088 y 8090 por defecto) y lo termina. Es mas fiable que cerrar las
#  ventanas a mano, porque Maven deja un proceso Java hijo que a veces
#  sobrevive y luego provoca el error "Port already in use".
#
#  SEGURO IMPORTANTE: solo detiene procesos 'java'. Si en un puerto hay
#  otra cosa (un contenedor de Docker, otro programa), avisa y NO lo
#  toca. Sin este seguro, el script podria matar com.docker.backend y
#  tumbar Docker Desktop entero.
#
#  USO:  .\detener-todo.ps1
# =====================================================================

# Los puertos salen de entorno.ps1 (variables del .env). El 8080 no esta en la
# lista por defecto: el gateway usa el 8090. Ver docs\fases\FASE-01-infraestructura.md
. "$PSScriptRoot\entorno.ps1"
$PUERTOS = $CATALOGO | ForEach-Object { $_.Puerto }

# Unicos procesos que este script tiene permitido detener.
$PERMITIDOS = @("java", "javaw")

Write-Host ""
Write-Host "  Teleticket - deteniendo servicios" -ForegroundColor Cyan
Write-Host "  ----------------------------------------" -ForegroundColor Cyan

$detenidos = 0
foreach ($puerto in $PUERTOS) {
    $conexiones = Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue
    if (-not $conexiones) {
        Write-Host ("  [ ] puerto {0}  libre" -f $puerto) -ForegroundColor DarkGray
        continue
    }

    # Ojo: no usar $pid como variable, es una automatica de solo lectura
    # en PowerShell (el PID de la propia consola) y el bucle fallaria.
    foreach ($procId in ($conexiones.OwningProcess | Select-Object -Unique)) {
        try {
            $proc = Get-Process -Id $procId -ErrorAction Stop
        } catch {
            Write-Host ("  [!] puerto {0}  no se pudo consultar el PID {1}" -f $puerto, $procId) -ForegroundColor Red
            continue
        }

        # El seguro: si no es un proceso Java, no es nuestro. Avisar y seguir.
        if ($PERMITIDOS -notcontains $proc.ProcessName) {
            Write-Host ("  [!] puerto {0}  lo ocupa '{1}' (PID {2}), que NO es un microservicio." -f $puerto, $proc.ProcessName, $procId) -ForegroundColor Yellow
            Write-Host     "                  No se toca. Revisa esto a mano si hace falta." -ForegroundColor Gray
            continue
        }

        try {
            Stop-Process -Id $procId -Force -ErrorAction Stop
            Write-Host ("  [X] puerto {0}  detenido {1} (PID {2})" -f $puerto, $proc.ProcessName, $procId) -ForegroundColor Yellow
            $detenidos++
        } catch {
            Write-Host ("  [!] puerto {0}  no se pudo detener el PID {1}" -f $puerto, $procId) -ForegroundColor Red
        }
    }
}

Write-Host "  ----------------------------------------" -ForegroundColor Cyan
Write-Host "  $detenidos proceso(s) detenido(s)." -ForegroundColor Green
Write-Host ""
