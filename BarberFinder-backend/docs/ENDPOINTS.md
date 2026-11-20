# Documentación de endpoints

Tres microservicios Spring Boot, cada uno con su propia base de datos MySQL:

| Microservicio | Puerto | Base de datos | Responsabilidad |
|---|---|---|---|
| ms-usuarios | 8081 | barber_usuarios | Registro, login (JWT), recuperación de contraseña, perfil, roles y administración de usuarios |
| ms-barberias | 8082 | barber_barberias | CRUD de barberías, precios por servicio y cálculo del combo |
| ms-reservas | 8083 | barber_reservas | CRUD de reservas, disponibilidad de horas y estados de la atención |

`ms-reservas` se comunica con `ms-barberias` por HTTP (`GET /api/barberias/{id}/precio` y `GET /api/barberias/{id}`) para obtener el precio real del servicio y validar quién es el dueño de la barbería. El precio de una reserva **nunca** lo manda la app: lo calcula el backend.

Hay una colección de Postman lista para importar en `postman/BarberFinder.postman_collection.json` (guarda el token automáticamente al hacer login).

## Convenciones

- Formato: JSON (`Content-Type: application/json`). Fechas `yyyy-MM-dd`, horas `HH:mm`.
- Autenticación: `Authorization: Bearer <token>`. El token se obtiene en `/api/auth/login` o `/api/auth/registro` y lo aceptan los 3 microservicios (mismo `JWT_SECRET`). Dura `JWT_EXPIRATION_MINUTES` (120 por defecto).
- Contenido del token: `sub` (id del usuario), `email`, `rol` y, para barberos, `barberiaId`.
- Errores: siempre con el mismo formato.

```json
{
  "timestamp": "2026-11-20T10:15:30.123",
  "status": 400,
  "error": "Bad Request",
  "mensaje": "Hay campos con errores.",
  "ruta": "/api/auth/registro",
  "campos": {
    "telefono": "Ingresa un teléfono válido (8 a 12 dígitos).",
    "password": "La contraseña debe tener entre 8 y 64 caracteres, con al menos una letra y un número."
  }
}
```

| Código | Cuándo |
|---|---|
| 200 / 201 / 204 | OK / creado / eliminado |
| 400 | Validación de campos o regla de negocio (hora pasada, precio fuera de rango, etc.). `campos` trae el error de cada campo para mostrarlo en el formulario |
| 401 | Falta el token, es inválido o expiró; o credenciales incorrectas en el login |
| 403 | El rol no tiene permiso o el recurso no es tuyo |
| 404 | No existe |
| 409 | Conflicto: correo ya registrado, hora ya reservada, reserva no confirmada |
| 503 | ms-reservas no pudo comunicarse con ms-barberias |

## Roles

| Rol | Puede |
|---|---|
| ADMIN | Todo: administrar usuarios y roles, editar cualquier barbería, ver y eliminar cualquier reserva |
| DUENO | Crear barberías y editar/desactivar/eliminar **las suyas**; ver y gestionar las reservas de sus barberías |
| BARBERO | Ver la agenda de **su** barbería (`barberiaId` asignado) y marcar reservas como ATENDIDA o NO_ASISTIO |
| CLIENTE | Reservar, ver, reprogramar y cancelar **sus** reservas |

Todos los roles pueden ver y editar su perfil y cambiar su contraseña.

---

## ms-usuarios (http://localhost:8081)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/estado` | Público | Estado del servicio |
| POST | `/api/auth/registro` | Público | Registro de cliente. Devuelve token |
| POST | `/api/auth/login` | Público | Inicio de sesión. Devuelve token |
| POST | `/api/auth/recuperar` | Público | Genera código de recuperación (15 min) |
| POST | `/api/auth/restablecer` | Público | Cambia la contraseña usando el código |
| GET | `/api/usuarios/me` | Autenticado | Ver mi perfil |
| PUT | `/api/usuarios/me` | Autenticado | Modificar mi perfil (nombre, teléfono) |
| PUT | `/api/usuarios/me/password` | Autenticado | Cambiar mi contraseña |
| GET | `/api/usuarios?rol=BARBERO` | ADMIN | Listar usuarios (filtro de rol opcional) |
| GET | `/api/usuarios/{id}` | ADMIN | Ver un usuario |
| POST | `/api/usuarios` | ADMIN | Crear usuario con cualquier rol |
| PUT | `/api/usuarios/{id}/rol` | ADMIN | Cambiar rol (y barbería si es BARBERO) |
| PATCH | `/api/usuarios/{id}/estado?activo=false` | ADMIN | Activar o desactivar una cuenta |
| DELETE | `/api/usuarios/{id}` | ADMIN | Eliminar usuario |

### POST /api/auth/registro

```json
{
  "nombre": "Juan Pérez",
  "email": "juan@correo.cl",
  "telefono": "+56912345678",
  "password": "Clave1234"
}
```

Reglas: nombre solo letras (2 a 60), correo válido y no repetido, teléfono de 8 a 12 dígitos (opcional `+`), contraseña de 8 a 64 caracteres con al menos una letra y un número. Siempre crea el rol `CLIENTE`.

Respuesta `201`:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiraEnMinutos": 120,
  "usuario": {
    "id": 5,
    "nombre": "Juan Pérez",
    "email": "juan@correo.cl",
    "telefono": "+56912345678",
    "rol": "CLIENTE",
    "rolDescripcion": "Cliente",
    "barberiaId": null,
    "activo": true,
    "creadoEn": "2026-11-20T10:15:30"
  }
}
```

Errores: `400` (campos), `409` (correo ya registrado).

### POST /api/auth/login

```json
{ "email": "cliente@barberfinder.cl", "password": "Demo12345" }
```

Respuesta `200`: igual que el registro. Errores: `401` correo o contraseña incorrectos, `403` cuenta desactivada.

### POST /api/auth/recuperar

```json
{ "email": "cliente@barberfinder.cl" }
```

Respuesta `200` (siempre la misma, exista o no el correo, para no revelar qué cuentas existen):

```json
{
  "mensaje": "Si el correo está registrado, se generó un código de recuperación válido por 15 minutos.",
  "codigoDemo": "482913",
  "expiraEnMinutos": 15
}
```

`codigoDemo` solo aparece con `RECOVERY_DEMO_MODE=true` (el proyecto no envía correos). Con `false` el campo viene `null` y el código queda en el log del servicio.

### POST /api/auth/restablecer

```json
{ "email": "cliente@barberfinder.cl", "codigo": "482913", "nuevaPassword": "Nueva12345" }
```

Respuesta `200`: `{ "mensaje": "Contraseña actualizada. Ya puedes iniciar sesión." }`. Error `400` si el código es incorrecto o expiró. Tras 5 intentos fallidos el código se invalida.

### PUT /api/usuarios/me

```json
{ "nombre": "Juan Andrés Pérez", "telefono": "+56987654321" }
```

### PUT /api/usuarios/me/password

```json
{ "passwordActual": "Demo12345", "nuevaPassword": "Nueva12345" }
```

### POST /api/usuarios (ADMIN)

```json
{
  "nombre": "Pedro Soto",
  "email": "pedro@barberfinder.cl",
  "telefono": "+56955556666",
  "password": "Clave1234",
  "rol": "BARBERO",
  "barberiaId": 1
}
```

`rol`: `ADMIN`, `DUENO`, `BARBERO` o `CLIENTE`. `barberiaId` es obligatorio solo para `BARBERO`.

### PUT /api/usuarios/{id}/rol (ADMIN)

```json
{ "rol": "BARBERO", "barberiaId": 3 }
```

Un admin no puede quitarse su propio rol, desactivarse ni eliminarse.

---

## ms-barberias (http://localhost:8082)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/estado` | Público | Estado del servicio |
| GET | `/api/barberias?categoria=PREMIUM&q=trebol` | Público | Listar barberías activas. `q` busca por nombre ignorando tildes y mayúsculas |
| GET | `/api/barberias/{id}` | Público | Detalle de una barbería |
| GET | `/api/barberias/{id}/servicios` | Público | Los 3 servicios con su precio y desglose |
| GET | `/api/barberias/{id}/precio?servicio=CORTE_Y_BARBA` | Público | Precio de un servicio con desglose |
| GET | `/api/barberias/mias` | DUENO, ADMIN | Barberías del usuario conectado |
| POST | `/api/barberias` | DUENO, ADMIN | Crear barbería (queda a nombre del dueño conectado) |
| PUT | `/api/barberias/{id}` | DUENO dueño de esa barbería, ADMIN | Modificar |
| PATCH | `/api/barberias/{id}/estado?activa=false` | DUENO dueño, ADMIN | Activar o desactivar (desactivada no aparece en el listado ni acepta reservas) |
| DELETE | `/api/barberias/{id}` | DUENO dueño, ADMIN | Eliminar |

Categorías: `ECONOMICA`, `INTERMEDIA`, `PREMIUM`. Servicios: `CORTE_CLASICO`, `CORTE_MODERNO`, `CORTE_Y_BARBA`.

### POST / PUT /api/barberias

```json
{
  "nombre": "Barbería Los Andes",
  "categoria": "INTERMEDIA",
  "direccion": "Av. Ejemplo 123 (dirección ficticia)",
  "comuna": "Providencia",
  "descripcion": "Cortes clásicos y modernos.",
  "telefono": "+56922223333",
  "precioCorteClasico": 9000,
  "precioCorteModerno": 11000,
  "precioBarba": 5000,
  "latitud": -33.4263,
  "longitud": -70.6150
}
```

Reglas: precios entre $1.000 y $100.000, y el combo (corte clásico + barba + $1.500) tampoco puede superar $100.000. `latitud`, `longitud`, `comuna`, `descripcion` y `telefono` son opcionales. Un ADMIN puede enviar `"duenoId"` para asignar la barbería a otro dueño.

Respuesta `201`:

```json
{
  "id": 7,
  "nombre": "Barbería Los Andes",
  "categoria": "INTERMEDIA",
  "categoriaEtiqueta": "Intermedia",
  "direccion": "Av. Ejemplo 123 (dirección ficticia)",
  "comuna": "Providencia",
  "descripcion": "Cortes clásicos y modernos.",
  "telefono": "+56922223333",
  "precioCorteClasico": 9000,
  "precioCorteModerno": 11000,
  "precioBarba": 5000,
  "precioDesde": 9000,
  "latitud": -33.4263,
  "longitud": -70.615,
  "duenoId": 2,
  "activa": true
}
```

### GET /api/barberias/1/precio?servicio=CORTE_Y_BARBA

```json
{
  "barberiaId": 1,
  "barberiaNombre": "Barbería El Trébol",
  "duenoId": 2,
  "servicio": "CORTE_Y_BARBA",
  "servicioEtiqueta": "Corte y barba",
  "servicioDescripcion": "Corte clásico más arreglo de barba.",
  "lineas": [
    { "etiqueta": "Corte clásico", "monto": 6000 },
    { "etiqueta": "Arreglo de barba", "monto": 3000 },
    { "etiqueta": "Adicional por combo", "monto": 1500 }
  ],
  "total": 10500
}
```

---

## ms-reservas (http://localhost:8083)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/estado` | Público | Estado del servicio |
| GET | `/api/reservas/disponibilidad?barberiaId=1&fecha=2026-11-21` | Público | Horas libres y ocupadas de un día |
| POST | `/api/reservas` | CLIENTE, ADMIN | Crear reserva |
| GET | `/api/reservas/mias?estado=CONFIRMADA` | Autenticado | Mis reservas (filtro opcional) |
| GET | `/api/reservas` | ADMIN | Todas las reservas |
| GET | `/api/reservas/barberia/{barberiaId}?fecha=2026-11-21` | ADMIN, DUENO de esa barbería, BARBERO de esa barbería | Agenda de una barbería |
| GET | `/api/reservas/{id}` | Cliente de la reserva o personal de la barbería | Detalle |
| PUT | `/api/reservas/{id}` | CLIENTE (propia), ADMIN | Reprogramar fecha y hora |
| PATCH | `/api/reservas/{id}/cancelar` | Cliente de la reserva, personal de la barbería, ADMIN | Cancelar (la hora queda libre) |
| PATCH | `/api/reservas/{id}/estado` | BARBERO, DUENO (de esa barbería), ADMIN | Marcar `ATENDIDA`, `NO_ASISTIO` o volver a `CONFIRMADA` |
| DELETE | `/api/reservas/{id}` | ADMIN | Eliminar definitivamente |

Horas disponibles: `10:00, 11:00, 12:00, 13:00, 15:00, 16:00, 17:00, 18:00` (las mismas de la app). Se puede reservar desde hoy hasta 30 días hacia adelante y nunca en una hora que ya pasó (zona horaria America/Santiago). No se permiten dos reservas activas (no canceladas) en la misma barbería, fecha y hora; al cancelar, la hora queda libre.

Estados: `CONFIRMADA`, `CANCELADA`, `ATENDIDA`, `NO_ASISTIO`.

### POST /api/reservas

```json
{
  "barberiaId": 1,
  "servicio": "CORTE_Y_BARBA",
  "fecha": "2026-11-21",
  "hora": "15:00",
  "nombreCliente": "Cliente Demo",
  "telefonoCliente": "+56944444444"
}
```

Respuesta `201`:

```json
{
  "id": 1,
  "clienteId": 4,
  "barberiaId": 1,
  "barberiaNombre": "Barbería El Trébol",
  "servicio": "CORTE_Y_BARBA",
  "servicioEtiqueta": "Corte y barba",
  "precio": 10500,
  "fecha": "2026-11-21",
  "hora": "15:00",
  "nombreCliente": "Cliente Demo",
  "telefonoCliente": "+56944444444",
  "estado": "CONFIRMADA",
  "estadoEtiqueta": "Confirmada",
  "creadaEn": "2026-11-20T10:20:00",
  "actualizadaEn": null
}
```

Errores: `400` (campos, hora fuera de la lista, fecha u hora pasada, barbería desactivada), `404` (barbería no existe), `409` (hora ya reservada), `503` (ms-barberias caído).

### GET /api/reservas/disponibilidad?barberiaId=1&fecha=2026-11-21

```json
{
  "barberiaId": 1,
  "fecha": "2026-11-21",
  "horas": [
    { "hora": "10:00", "disponible": true },
    { "hora": "11:00", "disponible": false },
    { "hora": "12:00", "disponible": true }
  ]
}
```

(la respuesta real trae las 8 horas)

### PUT /api/reservas/{id}

```json
{ "fecha": "2026-11-22", "hora": "16:00" }
```

### PATCH /api/reservas/{id}/estado

```json
{ "estado": "ATENDIDA" }
```

---

## API externa

La app consume la API pública **Open-Meteo** directamente con Retrofit, sin pasar por estos microservicios y con un cliente HTTP que no envía el token JWT. No requiere llave ni registro.

| API | URL base | Endpoint usado | Pantalla |
|---|---|---|---|
| Open-Meteo (clima) | `https://api.open-meteo.com/` | `GET v1/forecast?latitude&longitude&current=temperature_2m,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=America/Santiago&forecast_days=7` | Reservar hora (pronóstico del día elegido) y Ubicación (clima en las coordenadas del GPS) |
