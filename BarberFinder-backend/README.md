# BarberFinder Backend

Microservicios de **BarberFinder**, la app Android para encontrar barberías, comparar precios y reservar horas. Este repositorio contiene el backend: 3 microservicios Spring Boot, cada uno con su propia base de datos MySQL, listos para levantarse con Docker Compose.

Repositorio de la app Android: https://github.com/benzunigaa-del/BarberFinder

## Integrantes

- [Nombre integrante 1]
- [Nombre integrante 2]

## Arquitectura

```
                   App Android (Retrofit, JWT)
                 /            |              \
     :8081      /     :8082   |       :8083   \
 ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
 │  ms-usuarios   │ │  ms-barberias  │ │  ms-reservas   │
 │ auth, perfil,  │ │ barberías,     │◄┤ reservas,      │
 │ roles, admin   │ │ precios        │ │ disponibilidad │
 └───────┬────────┘ └───────┬────────┘ └───────┬────────┘
         │                  │                  │
 ┌───────▼──────────────────▼──────────────────▼───────┐
 │ MySQL 8.4: barber_usuarios | barber_barberias |     │
 │            barber_reservas                          │
 └─────────────────────────────────────────────────────┘
```

| Microservicio | Puerto | Base de datos | Responsabilidad |
|---|---|---|---|
| `ms-usuarios` | 8081 | `barber_usuarios` | Registro, login con JWT, recuperación de contraseña, perfil y administración de usuarios y roles |
| `ms-barberias` | 8082 | `barber_barberias` | CRUD de barberías y cálculo de precios por servicio (misma regla que la app) |
| `ms-reservas` | 8083 | `barber_reservas` | CRUD de reservas, disponibilidad de horas y estados de atención. Consulta a `ms-barberias` por HTTP para obtener el precio real |

Cada microservicio sigue la misma estructura en capas:

```
ms-xxx/src/main/java/cl/barberfinder/xxx/
├── controller/   Endpoints REST
├── service/      Lógica de negocio
├── repository/   Spring Data JPA
├── model/        Entidades JPA y enums
├── dto/          Records de entrada/salida con validaciones (Jakarta Validation)
├── security/     Lectura del JWT, anotación @Requiere(roles) e interceptor
├── exception/    ApiException y manejo global de errores
├── config/       Configuración y datos de prueba
└── client/       (solo ms-reservas) cliente HTTP hacia ms-barberias
```

**Tecnologías:** Java 17 · Spring Boot 3.3.5 (Web, Data JPA, Validation) · MySQL 8.4 · JJWT 0.12 · BCrypt (spring-security-crypto) · JUnit 5, MockMvc, Mockito y H2 para pruebas · Docker y Docker Compose.

## Roles

| Rol | Privilegios |
|---|---|
| ADMIN | Administra usuarios y roles; acceso total a barberías y reservas |
| DUENO | Crea barberías y gestiona las suyas; ve y gestiona las reservas de sus barberías |
| BARBERO | Ve la agenda de su barbería y marca reservas como atendidas o no asistió |
| CLIENTE | Reserva, reprograma y cancela sus propias horas |

## Inicio rápido

```bash
git clone <url-de-este-repo>
cd BarberFinder-backend
cp .env.example .env          # en PowerShell: Copy-Item .env.example .env
# edita .env y cambia los valores CAMBIAR
docker compose up -d --build
```

Comprueba: http://localhost:8082/api/barberias debe mostrar 6 barberías.

Instrucciones completas, problemas comunes y cómo conectar el emulador: **[docs/DOCKER.md](docs/DOCKER.md)**.

## Documentación

| Documento | Contenido |
|---|---|
| [docs/DOCKER.md](docs/DOCKER.md) | Cómo levantar, detener, ver logs, ver la base de datos y conectar la app |
| [docs/ENDPOINTS.md](docs/ENDPOINTS.md) | Todos los endpoints con método, ruta, rol requerido, ejemplos de request/response y códigos de error |
| [docs/CREDENCIALES.md](docs/CREDENCIALES.md) | Usuarios de prueba por rol y cómo crear más usuarios |
| [docs/EVIDENCIAS.md](docs/EVIDENCIAS.md) | Cómo generar las evidencias de compilación y pruebas, y qué cubre cada prueba |
| [postman/BarberFinder.postman_collection.json](postman/BarberFinder.postman_collection.json) | Colección Postman con las 42 peticiones (guarda los tokens de cada rol automáticamente) |

## Credenciales de prueba

Se crean automáticamente la primera vez. Contraseña: `Demo12345` (configurable con `SEED_PASSWORD`).

| Rol | Correo |
|---|---|
| ADMIN | admin@barberfinder.cl |
| DUENO | dueno@barberfinder.cl |
| BARBERO | barbero@barberfinder.cl |
| CLIENTE | cliente@barberfinder.cl |

## Pruebas

```bash
mvn test                       # los 3 microservicios (requiere JDK 17 y Maven)
bash scripts/smoke-test.sh     # prueba los endpoints con el stack levantado
.\scripts\evidencias.ps1       # Windows: pruebas + build Docker + smoke test, guarda logs en docs/evidencias
```

## Variables de entorno

Todas están en [.env.example](.env.example). El archivo `.env` real no se sube al repositorio.

| Variable | Uso |
|---|---|
| `MYSQL_ROOT_PASSWORD`, `DB_USER`, `DB_PASSWORD` | Credenciales de MySQL |
| `MYSQL_PORT`, `USUARIOS_PORT`, `BARBERIAS_PORT`, `RESERVAS_PORT` | Puertos publicados en tu PC |
| `JWT_SECRET`, `JWT_EXPIRATION_MINUTES` | Firma y duración del token (mismo secreto en los 3 servicios) |
| `SEED_ENABLED`, `SEED_PASSWORD`, `SEED_DUENO_ID` | Datos de prueba |
| `RECOVERY_DEMO_MODE` | Devuelve el código de recuperación en la respuesta (no hay envío de correo) |

## Evidencia de trabajo colaborativo

Ver el historial de commits por integrante con:

```bash
git shortlog -sne
```
