# =====================================================================
#  Teleticket - configuracion compartida por los scripts
# =====================================================================
#
#  No se ejecuta solo: lo cargan los demas .ps1 con
#      . "$PSScriptRoot\entorno.ps1"
#
#  Hace dos cosas:
#    1. Lee el archivo .env de la raiz (si existe) y deja sus variables en
#       el entorno del proceso. Los servicios que se lancen despues las
#       heredan. Una variable que ya estaba definida en la consola gana.
#    2. Define la tabla de servicios con sus puertos, que salen de las
#       mismas variables que leen los application.properties. Asi el puerto
#       se escribe en un solo sitio (.env) y no en cuatro scripts.
#
#  Solo ASCII: Windows PowerShell 5.1 lee mal los acentos sin BOM.
# =====================================================================

$RAIZ = $PSScriptRoot

function Import-DotEnv {
    param([string] $Ruta)

    if (-not (Test-Path $Ruta)) { return }

    foreach ($linea in Get-Content $Ruta) {
        $texto = $linea.Trim()
        if (-not $texto -or $texto.StartsWith("#")) { continue }

        $igual = $texto.IndexOf("=")
        if ($igual -lt 1) { continue }

        $clave = $texto.Substring(0, $igual).Trim()
        $valor = $texto.Substring($igual + 1).Trim()

        # Quita comillas simples o dobles alrededor del valor.
        if ($valor.Length -ge 2 -and
            (($valor.StartsWith('"') -and $valor.EndsWith('"')) -or
             ($valor.StartsWith("'") -and $valor.EndsWith("'")))) {
            $valor = $valor.Substring(1, $valor.Length - 2)
        }

        # Lo que ya esta en el entorno gana sobre el archivo (igual que docker compose).
        if ([string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable($clave))) {
            [Environment]::SetEnvironmentVariable($clave, $valor, "Process")
        }
    }
}

function Get-Config {
    param([string] $Nombre, [string] $Defecto)

    $valor = [Environment]::GetEnvironmentVariable($Nombre)
    if ([string]::IsNullOrEmpty($valor)) { return $Defecto }
    return $valor
}

Import-DotEnv (Join-Path $RAIZ ".env")

# Orden de arranque: primero los de negocio, el gateway al final, para que
# cuando empiece a enrutar sus destinos ya esten vivos.
#   Carpeta = directorio del proyecto
#   Nombre  = lo que responde /ping (spring.application.name)
#   Variable/Defecto = puerto, igual que en su application.properties
$CATALOGO = @(
    @{ Carpeta = "ms-usuarios";       Nombre = "ms-usuarios";       Variable = "MS_USUARIOS_PORT";       Defecto = 8081 },
    @{ Carpeta = "ms-eventos";        Nombre = "ms-eventos";        Variable = "MS_EVENTOS_PORT";        Defecto = 8082 },
    @{ Carpeta = "ms-cola";           Nombre = "ms-cola";           Variable = "MS_COLA_PORT";           Defecto = 8083 },
    @{ Carpeta = "ms-inventario";     Nombre = "ms-inventario";     Variable = "MS_INVENTARIO_PORT";     Defecto = 8084 },
    @{ Carpeta = "ms-ordenes";        Nombre = "ms-ordenes";        Variable = "MS_ORDENES_PORT";        Defecto = 8085 },
    @{ Carpeta = "ms-pagos";          Nombre = "ms-pagos";          Variable = "MS_PAGOS_PORT";          Defecto = 8086 },
    @{ Carpeta = "ms-teletickets";    Nombre = "ms-tickets";        Variable = "MS_TICKETS_PORT";        Defecto = 8087 },
    @{ Carpeta = "ms-notificaciones"; Nombre = "ms-notificaciones"; Variable = "MS_NOTIFICACIONES_PORT"; Defecto = 8088 },
    # El gateway usa 8090 y no 8080 por defecto: ver docs\fases\FASE-01-infraestructura.md
    @{ Carpeta = "ms-api-gateway";    Nombre = "api-gateway";        Variable = "GATEWAY_PORT";           Defecto = 8090 }
)

foreach ($s in $CATALOGO) {
    $s.Puerto = [int](Get-Config $s.Variable $s.Defecto)
}
