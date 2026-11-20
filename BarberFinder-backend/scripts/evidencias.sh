#!/usr/bin/env bash
# Genera las evidencias de compilación y pruebas del backend en docs/evidencias/ (Linux / macOS / Git Bash).
# Uso: bash scripts/evidencias.sh
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
cd "$RAIZ" || exit 1
DIR="$RAIZ/docs/evidencias"; mkdir -p "$DIR"
SELLO="$(date +%Y%m%d-%H%M%S)"
RESUMEN=()

ejecutar() { # titulo archivo comando...
  local titulo="$1" archivo="$2"; shift 2
  echo -e "\n=== $titulo ==="
  { echo "# $titulo"; echo "# Fecha: $(date '+%Y-%m-%d %H:%M:%S')"; echo; } > "$DIR/$archivo"
  "$@" 2>&1 | tee -a "$DIR/$archivo"
  local codigo=${PIPESTATUS[0]}
  echo "# Código de salida: $codigo" >> "$DIR/$archivo"
  if [ "$codigo" -eq 0 ]; then estado="OK"; else estado="FALLA (código $codigo)"; fi
  RESUMEN+=("$(printf '%-45s %-18s %s' "$titulo" "$estado" "$archivo")")
}

[ -f "$RAIZ/.env" ] || { echo "No existe .env. Copia .env.example a .env y completa los valores (docs/DOCKER.md)."; exit 1; }

entorno() { uname -a; docker --version; docker compose version; command -v java >/dev/null && java -version; command -v mvn >/dev/null && mvn -v; return 0; }
ejecutar "Versiones del entorno" "entorno-$SELLO.txt" entorno

if command -v mvn >/dev/null; then
  ejecutar "Pruebas unitarias backend (mvn test)" "backend-tests-$SELLO.log" mvn -B test
else
  ejecutar "Pruebas unitarias backend (mvn test en Docker)" "backend-tests-$SELLO.log" \
    docker run --rm -v "$RAIZ:/app" -v barberfinder-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-17 mvn -B test
fi
mkdir -p "$DIR/surefire-$SELLO"
find "$RAIZ" -path "*surefire-reports*" -name "*.txt" -exec cp {} "$DIR/surefire-$SELLO/" \; 2>/dev/null

ejecutar "Construcción de imágenes Docker" "docker-build-$SELLO.log" docker compose build --progress plain
ejecutar "Levantar contenedores" "docker-up-$SELLO.log" docker compose up -d

echo -e "\nEsperando a que los microservicios respondan (máx. 3 minutos)..."
LISTOS=0
for _ in $(seq 1 36); do
  sleep 5
  if curl -sf localhost:8081/api/estado >/dev/null && curl -sf localhost:8082/api/estado >/dev/null && curl -sf localhost:8083/api/estado >/dev/null; then
    LISTOS=1; break
  fi
done
ejecutar "Estado de contenedores" "docker-ps-$SELLO.txt" docker compose ps

if [ "$LISTOS" -eq 1 ]; then
  ejecutar "Prueba de endpoints (smoke test)" "smoke-resumen-$SELLO.log" bash "$RAIZ/scripts/smoke-test.sh"
else
  echo "Los microservicios no respondieron a tiempo. Revisa: docker compose logs"
  ejecutar "Logs de contenedores" "docker-logs-$SELLO.log" docker compose logs --tail=200
fi

{ echo "Evidencias BarberFinder Backend"; echo "Fecha: $(date '+%Y-%m-%d %H:%M:%S')"; echo "Equipo: $(hostname)"; echo; printf '%s\n' "${RESUMEN[@]}"; } > "$DIR/RESUMEN-$SELLO.txt"
echo -e "\n=== RESUMEN ==="; printf '%s\n' "${RESUMEN[@]}"
echo -e "\nArchivos en: $DIR"
