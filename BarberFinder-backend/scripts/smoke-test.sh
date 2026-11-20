#!/usr/bin/env bash
# Prueba de humo de los 3 microservicios de BarberFinder (versión Linux / macOS / Git Bash).
# Requiere el backend levantado (docker compose up -d --build) y curl.
# Uso: bash scripts/smoke-test.sh [servidor] [password_de_prueba]

SERVIDOR="${1:-localhost}"
PASS="${2:-Demo12345}"
U="http://$SERVIDOR:8081"; B="http://$SERVIDOR:8082"; R="http://$SERVIDOR:8083"

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$RAIZ/docs/evidencias"
SELLO="$(date +%Y%m%d-%H%M%S)"
LOG="$RAIZ/docs/evidencias/smoke-endpoints-$SELLO.log"
TMP="$(mktemp)"
OK=0; FALLAS=0

escribir() { echo -e "$1" | tee -a "$LOG"; }

# llamar METODO URL [JSON] [TOKEN]  -> deja STATUS y BODY
llamar() {
  local args=(-s -o "$TMP" -w "%{http_code}" -X "$1" "$2" -H "Content-Type: application/json")
  [ -n "${4:-}" ] && args+=(-H "Authorization: Bearer $4")
  [ -n "${3:-}" ] && args+=(--data "$3")
  STATUS="$(curl "${args[@]}")" || STATUS="000"
  BODY="$(cat "$TMP")"
}

paso() { # nombre esperado [detalle]
  if [ "$STATUS" = "$2" ]; then
    OK=$((OK+1)); escribir "[OK]    $1 -> HTTP $STATUS${3:+ | $3}"
  else
    FALLAS=$((FALLAS+1)); escribir "[FALLA] $1 -> HTTP $STATUS (esperado $2) $BODY"
  fi
}

campo() { echo "$BODY" | sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p" | head -1; }
numero() { echo "$BODY" | sed -n "s/.*\"$1\":\([0-9][0-9]*\).*/\1/p" | head -1; }

token() {
  llamar POST "$U/api/auth/login" "{\"email\":\"$1\",\"password\":\"$2\"}"
  paso "Login $1" 200 "rol=$(campo rol)"
  TOKEN="$(campo token)"
}

escribir "=== Prueba de endpoints BarberFinder | $(date '+%Y-%m-%d %H:%M:%S') | servidor $SERVIDOR ==="

escribir "\n--- Estado de los microservicios ---"
llamar GET "$U/api/estado"; paso "ms-usuarios /api/estado" 200
llamar GET "$B/api/estado"; paso "ms-barberias /api/estado" 200
llamar GET "$R/api/estado"; paso "ms-reservas /api/estado" 200

escribir "\n--- Login de los 4 roles ---"
token admin@barberfinder.cl "$PASS";   T_ADMIN="$TOKEN"
token dueno@barberfinder.cl "$PASS";   T_DUENO="$TOKEN"
token barbero@barberfinder.cl "$PASS"; T_BARBERO="$TOKEN"
token cliente@barberfinder.cl "$PASS"; T_CLIENTE="$TOKEN"
llamar POST "$U/api/auth/login" '{"email":"cliente@barberfinder.cl","password":"Incorrecta99"}'; paso "Login con contraseña incorrecta" 401
llamar GET "$U/api/usuarios/me"; paso "Perfil sin token" 401

escribir "\n--- Registro, perfil y recuperación de contraseña ---"
CORREO="smoke$SELLO@test.cl"
REG="{\"nombre\":\"Cliente Smoke\",\"email\":\"$CORREO\",\"telefono\":\"+56912345678\",\"password\":\"Clave1234\"}"
llamar POST "$U/api/auth/registro" "$REG"; ID_NUEVO="$(numero id)"; T_NUEVO="$(campo token)"
paso "Registro de cliente nuevo" 201 "id=$ID_NUEVO"
llamar POST "$U/api/auth/registro" "$REG"; paso "Registro con correo repetido" 409
llamar POST "$U/api/auth/registro" '{"nombre":"1","email":"malo","telefono":"12","password":"x"}'; paso "Registro con datos inválidos" 400
llamar GET "$U/api/usuarios/me" "" "$T_NUEVO"; paso "Ver mi perfil" 200
llamar PUT "$U/api/usuarios/me" '{"nombre":"Cliente Smoke Editado","telefono":"+56998765432"}' "$T_NUEVO"; paso "Modificar mi perfil" 200
llamar POST "$U/api/auth/recuperar" "{\"email\":\"$CORREO\"}"; CODIGO="$(campo codigoDemo)"
paso "Solicitar código de recuperación" 200 "codigoDemo=$CODIGO"
if [ -n "$CODIGO" ]; then
  llamar POST "$U/api/auth/restablecer" "{\"email\":\"$CORREO\",\"codigo\":\"000000\",\"nuevaPassword\":\"Nueva12345\"}"; paso "Restablecer con código incorrecto" 400
  llamar POST "$U/api/auth/restablecer" "{\"email\":\"$CORREO\",\"codigo\":\"$CODIGO\",\"nuevaPassword\":\"Nueva12345\"}"; paso "Restablecer contraseña" 200
  token "$CORREO" Nueva12345; T_NUEVO="$TOKEN"
else
  escribir "[AVISO] RECOVERY_DEMO_MODE=false: el código está en 'docker compose logs ms-usuarios'"
fi

escribir "\n--- Permisos por rol (ms-usuarios) ---"
llamar GET "$U/api/usuarios" "" "$T_CLIENTE"; paso "CLIENTE lista usuarios (debe negarse)" 403
llamar GET "$U/api/usuarios" "" "$T_ADMIN"; paso "ADMIN lista usuarios" 200
llamar PUT "$U/api/usuarios/$ID_NUEVO/rol" '{"rol":"DUENO"}' "$T_ADMIN"; paso "ADMIN cambia rol a DUENO" 200
llamar PUT "$U/api/usuarios/$ID_NUEVO/rol" '{"rol":"CLIENTE"}' "$T_ADMIN"; paso "ADMIN devuelve rol a CLIENTE" 200

escribir "\n--- Barberías (ms-barberias) ---"
llamar GET "$B/api/barberias"; paso "Listar barberías" 200
llamar GET "$B/api/barberias?q=trebol"; paso "Buscar 'trebol'" 200
llamar GET "$B/api/barberias/1/precio?servicio=CORTE_Y_BARBA"; paso "Precio combo barbería 1" 200 "total=$(numero total)"
NUEVA='{"nombre":"Barberia Smoke","categoria":"INTERMEDIA","direccion":"Calle Prueba 123","comuna":"Santiago","precioCorteClasico":9000,"precioCorteModerno":10000,"precioBarba":4000}'
llamar POST "$B/api/barberias" "$NUEVA" "$T_CLIENTE"; paso "CLIENTE crea barbería (debe negarse)" 403
llamar POST "$B/api/barberias" "$NUEVA" "$T_DUENO"; ID_BARBERIA="$(numero id)"; paso "DUENO crea barbería" 201 "id=$ID_BARBERIA"
llamar PUT "$B/api/barberias/$ID_BARBERIA" "${NUEVA/4000/4500}" "$T_DUENO"; paso "DUENO modifica su barbería" 200
llamar POST "$B/api/barberias" '{"nombre":"","categoria":"PREMIUM","direccion":"x","precioCorteClasico":10,"precioCorteModerno":10,"precioBarba":10}' "$T_DUENO"; paso "Barbería con precios inválidos" 400

escribir "\n--- Reservas (ms-reservas, se comunica con ms-barberias) ---"
MANANA="$(date -d tomorrow +%F 2>/dev/null || date -v+1d +%F)"
llamar GET "$R/api/reservas/disponibilidad?barberiaId=1&fecha=$MANANA"; paso "Disponibilidad barbería 1 para $MANANA" 200
LIBRES=($(echo "$BODY" | grep -o '"hora":"[0-9:]*","disponible":true' | sed 's/"hora":"\([0-9:]*\)".*/\1/'))
if [ "${#LIBRES[@]}" -lt 3 ]; then
  escribir "[AVISO] Quedan menos de 3 horas libres el $MANANA en la barbería 1; se omiten las pruebas de reserva."
else
  RES="{\"barberiaId\":1,\"servicio\":\"CORTE_Y_BARBA\",\"fecha\":\"$MANANA\",\"hora\":\"${LIBRES[0]}\",\"nombreCliente\":\"Cliente Demo\",\"telefonoCliente\":\"+56944444444\"}"
  llamar POST "$R/api/reservas" "$RES" "$T_BARBERO"; paso "BARBERO intenta reservar (debe negarse)" 403
  llamar POST "$R/api/reservas" "$RES" "$T_CLIENTE"; ID_RES="$(numero id)"; paso "CLIENTE crea reserva ${LIBRES[0]}" 201 "id=$ID_RES precio=$(numero precio)"
  llamar POST "$R/api/reservas" "$RES" "$T_NUEVO"; paso "Doble reserva misma hora" 409
  llamar POST "$R/api/reservas" "${RES/${LIBRES[0]}/14:00}" "$T_CLIENTE"; paso "Reserva con hora inválida" 400
  llamar GET "$R/api/reservas/mias" "" "$T_CLIENTE"; paso "Mis reservas" 200
  llamar PUT "$R/api/reservas/$ID_RES" "{\"fecha\":\"$MANANA\",\"hora\":\"${LIBRES[1]}\"}" "$T_CLIENTE"; paso "Reprogramar a ${LIBRES[1]}" 200
  llamar GET "$R/api/reservas/barberia/1?fecha=$MANANA" "" "$T_BARBERO"; paso "BARBERO ve agenda de su barbería" 200
  llamar GET "$R/api/reservas/barberia/2" "" "$T_BARBERO"; paso "BARBERO ve agenda de otra barbería (debe negarse)" 403
  llamar GET "$R/api/reservas/barberia/1" "" "$T_DUENO"; paso "DUENO ve agenda de su barbería" 200
  llamar PATCH "$R/api/reservas/$ID_RES/cancelar" "" "$T_NUEVO"; paso "Otro cliente cancela reserva ajena (debe negarse)" 403
  llamar PATCH "$R/api/reservas/$ID_RES/estado" '{"estado":"ATENDIDA"}' "$T_BARBERO"; paso "BARBERO marca ATENDIDA" 200
  llamar POST "$R/api/reservas" "{\"barberiaId\":1,\"servicio\":\"CORTE_CLASICO\",\"fecha\":\"$MANANA\",\"hora\":\"${LIBRES[2]}\",\"nombreCliente\":\"Cliente Smoke\",\"telefonoCliente\":\"+56912345678\"}" "$T_NUEVO"
  ID_RES2="$(numero id)"; paso "Cliente nuevo crea reserva ${LIBRES[2]}" 201 "id=$ID_RES2"
  llamar PATCH "$R/api/reservas/$ID_RES2/cancelar" "" "$T_NUEVO"; paso "Cliente nuevo cancela su reserva" 200
  llamar GET "$R/api/reservas" "" "$T_ADMIN"; paso "ADMIN lista todas las reservas" 200
  llamar DELETE "$R/api/reservas/$ID_RES2" "" "$T_ADMIN"; paso "ADMIN elimina reserva cancelada de prueba" 204
  llamar DELETE "$R/api/reservas/$ID_RES" "" "$T_ADMIN"; paso "ADMIN elimina reserva atendida de prueba" 204
fi

escribir "\n--- Limpieza ---"
llamar DELETE "$B/api/barberias/$ID_BARBERIA" "" "$T_DUENO"; paso "DUENO elimina la barbería de prueba" 204
llamar DELETE "$U/api/usuarios/$ID_NUEVO" "" "$T_ADMIN"; paso "ADMIN elimina el usuario de prueba" 204

rm -f "$TMP"
escribir "\n=== Resultado: $OK OK, $FALLAS fallas ==="
escribir "Log guardado en: $LOG"
[ "$FALLAS" -eq 0 ]
