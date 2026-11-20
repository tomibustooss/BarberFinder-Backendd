# Credenciales de prueba

La primera vez que se levanta el backend (base de datos vacía), `ms-usuarios` crea **un usuario por rol** y `ms-barberias` crea las **6 barberías ficticias** de la app. Son datos de demostración, no cuentas reales.

La contraseña de los 4 usuarios es el valor de `SEED_PASSWORD` en `.env` (por defecto `Demo12345`).

| Rol | Correo | Contraseña | Notas |
|---|---|---|---|
| ADMIN | admin@barberfinder.cl | Demo12345 | Administra usuarios, roles y todo el sistema |
| DUENO | dueno@barberfinder.cl | Demo12345 | Dueño de las 6 barberías de prueba (id de usuario 2) |
| BARBERO | barbero@barberfinder.cl | Demo12345 | Trabaja en la barbería 1, "Barbería El Trébol" |
| CLIENTE | cliente@barberfinder.cl | Demo12345 | Puede reservar, reprogramar y cancelar |

> Los usuarios solo se crean si la tabla `usuarios` está vacía. Si cambias `SEED_PASSWORD` después, ejecuta `docker compose down -v` y vuelve a levantar para que se regeneren.

## Barberías de prueba

| Id | Nombre | Categoría | Corte clásico | Corte moderno | Barba | Combo corte y barba |
|---|---|---|---|---|---|---|
| 1 | Barbería El Trébol | Económica | $6.000 | $7.000 | $3.000 | $10.500 |
| 2 | Corte Rápido Norte | Económica | $5.500 | $6.500 | $2.500 | $9.500 |
| 3 | Don Filo | Intermedia | $11.000 | $13.000 | $5.000 | $17.500 |
| 4 | Navaja & Estilo | Intermedia | $12.000 | $14.000 | $6.000 | $19.500 |
| 5 | The Gentlemen's Room | Premium | $20.000 | $24.000 | $9.000 | $30.500 |
| 6 | Royal Blade Studio | Premium | $22.000 | $26.000 | $10.000 | $33.500 |

Todas las direcciones son ficticias.

## Probar cada rol (ejemplos con curl)

```bash
# 1. Login (cambia el correo para probar otro rol)
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"cliente@barberfinder.cl","password":"Demo12345"}'

# 2. Copia el "token" de la respuesta y úsalo así:
curl http://localhost:8081/api/usuarios/me -H "Authorization: Bearer TOKEN_AQUI"
```

En PowerShell:

```powershell
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/auth/login `
  -ContentType "application/json" -Body '{"email":"cliente@barberfinder.cl","password":"Demo12345"}'
$h = @{ Authorization = "Bearer $($login.token)" }
Invoke-RestMethod -Uri http://localhost:8081/api/usuarios/me -Headers $h
```

| Prueba | Con qué usuario | Resultado esperado |
|---|---|---|
| `GET :8081/api/usuarios` | admin | 200, lista de usuarios |
| `GET :8081/api/usuarios` | cliente | 403 |
| `POST :8082/api/barberias` | dueno | 201 |
| `POST :8082/api/barberias` | cliente | 403 |
| `POST :8083/api/reservas` | cliente | 201 |
| `GET :8083/api/reservas/barberia/1` | barbero | 200 (agenda de su barbería) |
| `GET :8083/api/reservas/barberia/2` | barbero | 403 (no es su barbería) |
| `PATCH :8083/api/reservas/{id}/estado` con `{"estado":"ATENDIDA"}` | barbero | 200 |

## Crear más usuarios

**Clientes:** cualquiera se registra con `POST http://localhost:8081/api/auth/registro` (o desde la pantalla de registro de la app). Siempre quedan con rol `CLIENTE`.

```json
{ "nombre": "Ana Rojas", "email": "ana@correo.cl", "telefono": "+56911223344", "password": "Clave1234" }
```

**Dueños, barberos o admins:** los crea un ADMIN con `POST http://localhost:8081/api/usuarios`:

```json
{
  "nombre": "Pedro Soto",
  "email": "pedro@barberfinder.cl",
  "telefono": "+56955556666",
  "password": "Clave1234",
  "rol": "BARBERO",
  "barberiaId": 3
}
```

O se le cambia el rol a un cliente existente con `PUT http://localhost:8081/api/usuarios/{id}/rol` y el cuerpo `{"rol":"DUENO"}`. El usuario debe volver a iniciar sesión para que su nuevo token tenga el rol actualizado.

## Recuperar contraseña (modo demo)

1. `POST http://localhost:8081/api/auth/recuperar` con `{"email":"cliente@barberfinder.cl"}`.
2. Como el proyecto no envía correos, la respuesta trae `codigoDemo` (también aparece en `docker compose logs ms-usuarios`).
3. `POST http://localhost:8081/api/auth/restablecer` con `{"email":"cliente@barberfinder.cl","codigo":"<codigo>","nuevaPassword":"Nueva12345"}`.

Si cambias la contraseña de un usuario de prueba y quieres volver a `Demo12345`, usa este mismo flujo o reinicia la base con `docker compose down -v`.
