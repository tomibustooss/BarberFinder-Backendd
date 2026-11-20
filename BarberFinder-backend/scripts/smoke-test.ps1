<#
  Prueba de humo de los 3 microservicios de BarberFinder.
  Requiere el backend levantado (docker compose up -d --build).
  Ejecuta flujos reales contra los endpoints con los 4 roles y deja un log en docs/evidencias/.

  Uso (desde la raíz del repo backend):
    .\scripts\smoke-test.ps1
    .\scripts\smoke-test.ps1 -Servidor 192.168.1.10 -Password Demo12345
#>
param(
    [string]$Servidor = "localhost",
    [string]$Password = "Demo12345"
)

$ErrorActionPreference = "Stop"
$U = "http://${Servidor}:8081"
$B = "http://${Servidor}:8082"
$R = "http://${Servidor}:8083"

$raiz = Split-Path -Parent $PSScriptRoot
$carpeta = Join-Path $raiz "docs/evidencias"
New-Item -ItemType Directory -Force -Path $carpeta | Out-Null
$sello = Get-Date -Format "yyyyMMdd-HHmmss"
$log = Join-Path $carpeta "smoke-endpoints-$sello.log"
$script:ok = 0
$script:fallas = 0

function Escribir([string]$texto) {
    Write-Host $texto
    Add-Content -Path $log -Value $texto -Encoding UTF8
}

function Llamar([string]$metodo, [string]$url, $cuerpo = $null, [string]$token = $null) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    $p = @{ Method = $metodo; Uri = $url; Headers = $headers; UseBasicParsing = $true; ContentType = "application/json; charset=utf-8" }
    if ($null -ne $cuerpo) {
        $p["Body"] = [System.Text.Encoding]::UTF8.GetBytes(($cuerpo | ConvertTo-Json -Depth 6 -Compress))
    }
    try {
        $r = Invoke-WebRequest @p
        $json = $null
        if ($r.Content) { $json = $r.Content | ConvertFrom-Json }
        return [pscustomobject]@{ Status = [int]$r.StatusCode; Json = $json; Raw = [string]$r.Content }
    } catch {
        $resp = $_.Exception.Response
        if ($null -eq $resp) {
            return [pscustomobject]@{ Status = 0; Json = $null; Raw = $_.Exception.Message }
        }
        return [pscustomobject]@{ Status = [int]$resp.StatusCode; Json = $null; Raw = [string]$_.ErrorDetails.Message }
    }
}

function Paso([string]$nombre, [int]$esperado, $resp, [string]$detalle = "") {
    if ($resp.Status -eq $esperado) {
        $script:ok++
        $linea = "[OK]    {0} -> HTTP {1}" -f $nombre, $resp.Status
    } else {
        $script:fallas++
        $linea = "[FALLA] {0} -> HTTP {1} (esperado {2}) {3}" -f $nombre, $resp.Status, $esperado, $resp.Raw
    }
    if ($detalle) { $linea = "$linea | $detalle" }
    Escribir $linea
}

function Token([string]$email, [string]$clave) {
    $r = Llamar POST "$U/api/auth/login" @{ email = $email; password = $clave }
    Paso "Login $email" 200 $r ("rol=" + $r.Json.usuario.rol)
    return $r.Json.token
}

Escribir "=== Prueba de endpoints BarberFinder | $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') | servidor $Servidor ==="

Escribir "`n--- Estado de los microservicios ---"
Paso "ms-usuarios /api/estado"  200 (Llamar GET "$U/api/estado")
Paso "ms-barberias /api/estado" 200 (Llamar GET "$B/api/estado")
Paso "ms-reservas /api/estado"  200 (Llamar GET "$R/api/estado")

Escribir "`n--- Login de los 4 roles ---"
$tAdmin   = Token "admin@barberfinder.cl" $Password
$tDueno   = Token "dueno@barberfinder.cl" $Password
$tBarbero = Token "barbero@barberfinder.cl" $Password
$tCliente = Token "cliente@barberfinder.cl" $Password
Paso "Login con contraseña incorrecta" 401 (Llamar POST "$U/api/auth/login" @{ email = "cliente@barberfinder.cl"; password = "Incorrecta99" })
Paso "Perfil sin token" 401 (Llamar GET "$U/api/usuarios/me")

Escribir "`n--- Registro, perfil y recuperación de contraseña ---"
$correo = "smoke$sello@test.cl"
$reg = Llamar POST "$U/api/auth/registro" @{ nombre = "Cliente Smoke"; email = $correo; telefono = "+56912345678"; password = "Clave1234" }
Paso "Registro de cliente nuevo" 201 $reg ("id=" + $reg.Json.usuario.id)
$tNuevo = $reg.Json.token
$idNuevo = $reg.Json.usuario.id
Paso "Registro con correo repetido" 409 (Llamar POST "$U/api/auth/registro" @{ nombre = "Cliente Smoke"; email = $correo; telefono = "+56912345678"; password = "Clave1234" })
Paso "Registro con datos inválidos" 400 (Llamar POST "$U/api/auth/registro" @{ nombre = "1"; email = "malo"; telefono = "12"; password = "x" })
Paso "Ver mi perfil" 200 (Llamar GET "$U/api/usuarios/me" $null $tNuevo)
Paso "Modificar mi perfil" 200 (Llamar PUT "$U/api/usuarios/me" @{ nombre = "Cliente Smoke Editado"; telefono = "+56998765432" } $tNuevo)
$rec = Llamar POST "$U/api/auth/recuperar" @{ email = $correo }
Paso "Solicitar código de recuperación" 200 $rec ("codigoDemo=" + $rec.Json.codigoDemo)
if ($rec.Json.codigoDemo) {
    Paso "Restablecer con código incorrecto" 400 (Llamar POST "$U/api/auth/restablecer" @{ email = $correo; codigo = "000000"; nuevaPassword = "Nueva12345" })
    Paso "Restablecer contraseña" 200 (Llamar POST "$U/api/auth/restablecer" @{ email = $correo; codigo = $rec.Json.codigoDemo; nuevaPassword = "Nueva12345" })
    $tNuevo = Token $correo "Nueva12345"
} else {
    Escribir "[AVISO] RECOVERY_DEMO_MODE=false: el código está en 'docker compose logs ms-usuarios'"
}

Escribir "`n--- Permisos por rol (ms-usuarios) ---"
Paso "CLIENTE lista usuarios (debe negarse)" 403 (Llamar GET "$U/api/usuarios" $null $tCliente)
$lista = Llamar GET "$U/api/usuarios" $null $tAdmin
Paso "ADMIN lista usuarios" 200 $lista ("total=" + @($lista.Json).Count)
Paso "ADMIN cambia rol a DUENO" 200 (Llamar PUT "$U/api/usuarios/$idNuevo/rol" @{ rol = "DUENO" } $tAdmin)
Paso "ADMIN devuelve rol a CLIENTE" 200 (Llamar PUT "$U/api/usuarios/$idNuevo/rol" @{ rol = "CLIENTE" } $tAdmin)

Escribir "`n--- Barberías (ms-barberias) ---"
$barberias = Llamar GET "$B/api/barberias"
Paso "Listar barberías" 200 $barberias ("total=" + @($barberias.Json).Count)
Paso "Buscar 'trebol'" 200 (Llamar GET "$B/api/barberias?q=trebol")
$precio = Llamar GET "$B/api/barberias/1/precio?servicio=CORTE_Y_BARBA"
Paso "Precio combo barbería 1" 200 $precio ("total=" + $precio.Json.total)
$nueva = @{ nombre = "Barberia Smoke"; categoria = "INTERMEDIA"; direccion = "Calle Prueba 123"; comuna = "Santiago";
            precioCorteClasico = 9000; precioCorteModerno = 10000; precioBarba = 4000 }
Paso "CLIENTE crea barbería (debe negarse)" 403 (Llamar POST "$B/api/barberias" $nueva $tCliente)
$creada = Llamar POST "$B/api/barberias" $nueva $tDueno
Paso "DUENO crea barbería" 201 $creada ("id=" + $creada.Json.id)
$idBarberia = $creada.Json.id
$nueva.precioBarba = 4500
Paso "DUENO modifica su barbería" 200 (Llamar PUT "$B/api/barberias/$idBarberia" $nueva $tDueno)
Paso "Barbería con precios inválidos" 400 (Llamar POST "$B/api/barberias" @{ nombre = ""; categoria = "PREMIUM"; direccion = "x"; precioCorteClasico = 10; precioCorteModerno = 10; precioBarba = 10 } $tDueno)

Escribir "`n--- Reservas (ms-reservas, se comunica con ms-barberias) ---"
$manana = (Get-Date).AddDays(1).ToString("yyyy-MM-dd")
$disp = Llamar GET "$R/api/reservas/disponibilidad?barberiaId=1&fecha=$manana"
Paso "Disponibilidad barbería 1 para $manana" 200 $disp
$libres = @($disp.Json.horas | Where-Object { $_.disponible } | ForEach-Object { $_.hora })
if ($libres.Count -lt 3) {
    Escribir "[AVISO] Quedan menos de 3 horas libres el $manana en la barbería 1; se omiten las pruebas de reserva."
} else {
    $reserva = @{ barberiaId = 1; servicio = "CORTE_Y_BARBA"; fecha = $manana; hora = $libres[0]; nombreCliente = "Cliente Demo"; telefonoCliente = "+56944444444" }
    Paso "BARBERO intenta reservar (debe negarse)" 403 (Llamar POST "$R/api/reservas" $reserva $tBarbero)
    $res = Llamar POST "$R/api/reservas" $reserva $tCliente
    Paso "CLIENTE crea reserva $($libres[0])" 201 $res ("id=" + $res.Json.id + " precio=" + $res.Json.precio)
    $idReserva = $res.Json.id
    Paso "Doble reserva misma hora" 409 (Llamar POST "$R/api/reservas" $reserva $tNuevo)
    Paso "Reserva con hora inválida" 400 (Llamar POST "$R/api/reservas" @{ barberiaId = 1; servicio = "CORTE_CLASICO"; fecha = $manana; hora = "14:00"; nombreCliente = "Cliente Demo"; telefonoCliente = "+56944444444" } $tCliente)
    Paso "Mis reservas" 200 (Llamar GET "$R/api/reservas/mias" $null $tCliente)
    Paso "Reprogramar a $($libres[1])" 200 (Llamar PUT "$R/api/reservas/$idReserva" @{ fecha = $manana; hora = $libres[1] } $tCliente)
    Paso "BARBERO ve agenda de su barbería" 200 (Llamar GET "$R/api/reservas/barberia/1?fecha=$manana" $null $tBarbero)
    Paso "BARBERO ve agenda de otra barbería (debe negarse)" 403 (Llamar GET "$R/api/reservas/barberia/2" $null $tBarbero)
    Paso "DUENO ve agenda de su barbería" 200 (Llamar GET "$R/api/reservas/barberia/1" $null $tDueno)
    Paso "Otro cliente cancela reserva ajena (debe negarse)" 403 (Llamar PATCH "$R/api/reservas/$idReserva/cancelar" $null $tNuevo)
    Paso "BARBERO marca ATENDIDA" 200 (Llamar PATCH "$R/api/reservas/$idReserva/estado" @{ estado = "ATENDIDA" } $tBarbero)

    $res2 = Llamar POST "$R/api/reservas" @{ barberiaId = 1; servicio = "CORTE_CLASICO"; fecha = $manana; hora = $libres[2]; nombreCliente = "Cliente Smoke"; telefonoCliente = "+56912345678" } $tNuevo
    Paso "Cliente nuevo crea reserva $($libres[2])" 201 $res2 ("id=" + $res2.Json.id)
    Paso "Cliente nuevo cancela su reserva" 200 (Llamar PATCH "$R/api/reservas/$($res2.Json.id)/cancelar" $null $tNuevo)
    Paso "ADMIN lista todas las reservas" 200 (Llamar GET "$R/api/reservas" $null $tAdmin)
    Paso "ADMIN elimina reserva cancelada de prueba" 204 (Llamar DELETE "$R/api/reservas/$($res2.Json.id)" $null $tAdmin)
    Paso "ADMIN elimina reserva atendida de prueba" 204 (Llamar DELETE "$R/api/reservas/$idReserva" $null $tAdmin)
}

Escribir "`n--- Limpieza ---"
Paso "DUENO elimina la barbería de prueba" 204 (Llamar DELETE "$B/api/barberias/$idBarberia" $null $tDueno)
Paso "ADMIN elimina el usuario de prueba" 204 (Llamar DELETE "$U/api/usuarios/$idNuevo" $null $tAdmin)

Escribir "`n=== Resultado: $($script:ok) OK, $($script:fallas) fallas ==="
Escribir "Log guardado en: $log"
if ($script:fallas -gt 0) { exit 1 }
