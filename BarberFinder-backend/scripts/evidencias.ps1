<#
  Genera las evidencias de compilación y pruebas del backend en docs/evidencias/.
  1) Versiones del entorno
  2) Pruebas unitarias e integración de los 3 microservicios (mvn test)
  3) Construcción de las imágenes Docker
  4) Levanta el stack y guarda el estado de los contenedores
  5) Prueba de endpoints (scripts/smoke-test.ps1)

  Uso (desde la raíz del repo backend, con Docker Desktop abierto):
    .\scripts\evidencias.ps1
#>
$ErrorActionPreference = "Continue"
$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz
$dir = Join-Path $raiz "docs/evidencias"
New-Item -ItemType Directory -Force -Path $dir | Out-Null
$sello = Get-Date -Format "yyyyMMdd-HHmmss"
$resumen = @()

function Guardar([string]$archivo, $lineas) {
    $lineas | ForEach-Object { "$_" } | Out-File -FilePath (Join-Path $dir $archivo) -Encoding utf8
}

function Ejecutar([string]$titulo, [string]$archivo, [scriptblock]$comando) {
    Write-Host "`n=== $titulo ===" -ForegroundColor Cyan
    $global:LASTEXITCODE = 0
    & $comando *>&1 | ForEach-Object { "$_" } | Tee-Object -Variable capturado | Out-Host
    $codigo = $LASTEXITCODE
    Guardar $archivo (@("# $titulo", "# Fecha: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')", "# Código de salida: $codigo", "") + $capturado)
    $estado = if ($codigo -eq 0) { "OK" } else { "FALLA (código $codigo)" }
    $script:resumen += "{0,-45} {1,-18} {2}" -f $titulo, $estado, $archivo
    return $codigo
}

if (-not (Test-Path (Join-Path $raiz ".env"))) {
    Write-Host "No existe .env. Copia .env.example a .env y completa los valores (docs/DOCKER.md)." -ForegroundColor Yellow
    exit 1
}

Ejecutar "Versiones del entorno" "entorno-$sello.txt" {
    "SO: $([System.Environment]::OSVersion.VersionString)"
    docker --version
    docker compose version
    if (Get-Command java -ErrorAction SilentlyContinue) { java -version }
    if (Get-Command mvn -ErrorAction SilentlyContinue) { mvn -v }
    $global:LASTEXITCODE = 0
} | Out-Null

if (Get-Command mvn -ErrorAction SilentlyContinue) {
    Ejecutar "Pruebas unitarias backend (mvn test)" "backend-tests-$sello.log" { mvn -B test } | Out-Null
} else {
    Write-Host "Maven no está instalado: se ejecutan las pruebas dentro de un contenedor maven:3.9-eclipse-temurin-17" -ForegroundColor Yellow
    Ejecutar "Pruebas unitarias backend (mvn test en Docker)" "backend-tests-$sello.log" {
        docker run --rm -v "${raiz}:/app" -v barberfinder-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-17 mvn -B test
    } | Out-Null
}

$reportes = Join-Path $dir "surefire-$sello"
New-Item -ItemType Directory -Force -Path $reportes | Out-Null
Get-ChildItem -Path $raiz -Recurse -Filter "*.txt" -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -match "surefire-reports" } |
    ForEach-Object { Copy-Item $_.FullName -Destination $reportes }

Ejecutar "Construcción de imágenes Docker" "docker-build-$sello.log" { docker compose build --progress plain } | Out-Null
Ejecutar "Levantar contenedores" "docker-up-$sello.log" { docker compose up -d } | Out-Null

Write-Host "`nEsperando a que los microservicios respondan (máx. 3 minutos)..."
$listos = $false
for ($i = 0; $i -lt 36 -and -not $listos; $i++) {
    Start-Sleep -Seconds 5
    try {
        $null = Invoke-RestMethod "http://localhost:8081/api/estado" -TimeoutSec 3
        $null = Invoke-RestMethod "http://localhost:8082/api/estado" -TimeoutSec 3
        $null = Invoke-RestMethod "http://localhost:8083/api/estado" -TimeoutSec 3
        $listos = $true
    } catch { }
}

Ejecutar "Estado de contenedores" "docker-ps-$sello.txt" { docker compose ps } | Out-Null

if ($listos) {
    Ejecutar "Prueba de endpoints (smoke test)" "smoke-resumen-$sello.log" { & "$PSScriptRoot/smoke-test.ps1" } | Out-Null
} else {
    Write-Host "Los microservicios no respondieron a tiempo. Revisa: docker compose logs" -ForegroundColor Red
    Ejecutar "Logs de contenedores" "docker-logs-$sello.log" { docker compose logs --tail=200 } | Out-Null
    $resumen += "Prueba de endpoints: NO EJECUTADA (servicios no respondieron)"
}

$cabecera = @("Evidencias BarberFinder Backend", "Fecha: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')", "Equipo: $env:COMPUTERNAME", "")
Guardar "RESUMEN-$sello.txt" ($cabecera + $resumen)
Write-Host "`n=== RESUMEN ===" -ForegroundColor Cyan
$resumen | ForEach-Object { Write-Host $_ }
Write-Host "`nArchivos en: $dir"
