# Compatible con Windows PowerShell 5.1. Solo realiza peticiones GET.
[CmdletBinding()]
param(
    [Uri]$BaseUrl = 'http://127.0.0.1:8088',
    [Guid]$LoteId = '7331eec8-e447-4868-bdef-8eca3fff8d44',
    [PSCredential]$Credential
)
$ErrorActionPreference = 'Stop'
if ($BaseUrl.Scheme -ne 'https' -and -not ($BaseUrl.Scheme -eq 'http' -and $BaseUrl.IsLoopback)) {
    throw 'Usa HTTPS o el acceso local mediante tunel SSH para enviar las credenciales.'
}
if ($BaseUrl.UserInfo -or $BaseUrl.Query -or $BaseUrl.Fragment -or $BaseUrl.AbsolutePath -ne '/') {
    throw 'BaseUrl debe contener solo el esquema, servidor y puerto.'
}
if (-not $Credential) {
    $Credential = Get-Credential -UserName 'modulo3_consulta' -Message 'Credenciales de integracion de M3 en M2'
}
if (-not $Credential) { throw 'No se recibieron credenciales.' }
$basic = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes(
    $Credential.UserName + ':' + $Credential.GetNetworkCredential().Password))
$headers = @{ Authorization = 'Basic ' + $basic; Accept = 'application/json' }
$base = $BaseUrl.AbsoluteUri.TrimEnd('/') + '/api/integracion/modulo3/lotes/' + $LoteId
try {
    foreach ($consulta in @('resultado-sacrificio', 'alimentos', 'medicamentos')) {
        try {
            $respuesta = Invoke-WebRequest -Uri ($base + '/' + $consulta) -Headers $headers `
                -UseBasicParsing -MaximumRedirection 0 -TimeoutSec 20
        } catch {
            $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
            if ($status -eq 404 -and $consulta -eq 'resultado-sacrificio') {
                Write-Output 'Resultado de sacrificio: no encontrado (HTTP 404). Verificar lote, datos y version desplegada.'
                continue
            }
            if ($status -eq 401) { throw 'HTTP 401: revisar usuario, password y activacion de la cuenta M3.' }
            if ($status -eq 403) { throw 'HTTP 403: revisar permisos del usuario y version de Nginx/backend.' }
            throw ('Fallo al consultar {0}; HTTP {1}. Revisar servicio, tunel o certificado.' -f $consulta, $status)
        }
        if ($respuesta.StatusCode -ne 200 -or $respuesta.Headers['Content-Type'] -notmatch 'application/json') {
            throw ('Respuesta inesperada para {0}; no es la API JSON de M2.' -f $consulta)
        }
        $datos = $respuesta.Content | ConvertFrom-Json
        if ([string]$datos.loteId -ne $LoteId.ToString()) {
            throw ('El lote de la respuesta de {0} no coincide con el solicitado.' -f $consulta)
        }
        if ($consulta -eq 'resultado-sacrificio') {
            Write-Output ('Resultado: {0}; estado: {1}; aves: {2}; peso total kg: {3}' -f `
                $datos.id, $datos.estado, $datos.cantidadFinalPollos, $datos.pesoTotalKg)
        } else {
            if (-not ($datos.PSObject.Properties.Name -contains 'partidas')) {
                throw ('La respuesta de {0} no contiene partidas.' -f $consulta)
            }
            $partidas = @($datos.partidas)
            $incompletas = @($partidas | Where-Object { $_.datosCompletos -ne $true }).Count
            Write-Output ('{0}: {1} tramos; {2} con datos incompletos.' -f $consulta, $partidas.Count, $incompletas)
        }
    }
    Write-Output 'Consultas terminadas. No se envio la confirmacion de uso ni se modificaron datos.'
} finally {
    $headers.Clear()
    $basic = $null
    $Credential = $null
}

