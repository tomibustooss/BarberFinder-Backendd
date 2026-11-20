# Cómo levantar el backend con Docker

Con Docker no necesitas instalar Java, Maven ni MySQL en tu PC: todo se compila y se ejecuta dentro de contenedores.

## 1. Requisitos

| Herramienta | Versión | Para qué |
|---|---|---|
| Docker Desktop (Windows/macOS) o Docker Engine + Compose v2 (Linux) | Docker 24 o superior | Levantar MySQL y los 3 microservicios |
| Git | cualquiera | Clonar el repositorio |
| Internet (solo la primera vez) | | Descargar las imágenes `mysql:8.4`, `maven:3.9-eclipse-temurin-17` y `eclipse-temurin:17-jre` y las dependencias Maven |

Comprueba que Docker funciona:

```bash
docker --version
docker compose version
```

En Windows, Docker Desktop tiene que estar **abierto** (icono de la ballena en verde) antes de ejecutar los comandos.

## 2. Configurar las variables

Desde la carpeta raíz del repositorio (donde está `docker-compose.yml`):

```bash
# Linux / macOS / Git Bash
cp .env.example .env

# Windows PowerShell
Copy-Item .env.example .env
```

Abre `.env` y cambia los valores que dicen `CAMBIAR` (contraseña root de MySQL, contraseña del usuario de la app y `JWT_SECRET`). El `JWT_SECRET` debe tener **mínimo 32 caracteres**.

El archivo `.env` está en `.gitignore`, así que tus contraseñas no se suben a GitHub. En el repositorio solo queda `.env.example`.

## 3. Levantar todo

```bash
docker compose up -d --build
```

La primera vez demora varios minutos porque descarga imágenes y compila los 3 microservicios. Las siguientes veces es mucho más rápido.

Ver el estado:

```bash
docker compose ps
```

Deberías ver 4 contenedores en estado `running` (MySQL aparece como `healthy`):

| Contenedor | Puerto en tu PC | Base de datos |
|---|---|---|
| barberfinder-mysql | 3307 | (servidor MySQL 8.4) |
| barberfinder-ms-usuarios | 8081 | barber_usuarios |
| barberfinder-ms-barberias | 8082 | barber_barberias |
| barberfinder-ms-reservas | 8083 | barber_reservas |

## 4. Verificar que responden

Abre en el navegador o usa curl:

```bash
curl http://localhost:8081/api/estado
curl http://localhost:8082/api/estado
curl http://localhost:8083/api/estado
curl http://localhost:8082/api/barberias
```

Cada `/api/estado` responde `{"servicio":"ms-...","estado":"OK",...}` y `/api/barberias` devuelve las 6 barberías de prueba.

Para una prueba completa de los endpoints (login con los 4 roles, CRUD de reservas, recuperación de contraseña, permisos) ejecuta el script de humo, que además deja un log como evidencia:

```powershell
# Windows PowerShell
.\scripts\smoke-test.ps1
```

```bash
# Linux / macOS / Git Bash
bash scripts/smoke-test.sh
```

## 5. Ver logs

```bash
docker compose logs -f ms-usuarios
docker compose logs -f ms-reservas
docker compose logs --tail=100
```

En modo demo, el código de recuperación de contraseña también aparece en el log de `ms-usuarios` (línea que empieza con `[DEMO] Código de recuperación`).

## 6. Ver la base de datos (opcional)

```bash
docker compose --profile herramientas up -d
```

Abre http://localhost:8090 (Adminer) y entra con:

- Sistema: MySQL
- Servidor: `mysql`
- Usuario: el `DB_USER` de tu `.env` (por defecto `barber`)
- Contraseña: el `DB_PASSWORD` de tu `.env`

También puedes conectarte desde MySQL Workbench o DBeaver a `localhost:3307`.

## 7. Detener, reiniciar y borrar datos

```bash
docker compose stop          # detiene los contenedores (los datos se mantienen)
docker compose start         # los vuelve a iniciar
docker compose down          # elimina contenedores (los datos se mantienen en el volumen)
docker compose down -v       # elimina contenedores Y la base de datos (se recargan los datos de prueba)
docker compose up -d --build ms-reservas   # recompila solo un microservicio tras cambiar su código
```

## 8. Conectar la app Android

Desde el **emulador** de Android Studio, `localhost` apunta al propio emulador. Para llegar a tu PC se usa `10.0.2.2`:

| Microservicio | URL base desde el emulador | URL base desde un celular físico en la misma red Wi-Fi |
|---|---|---|
| ms-usuarios | `http://10.0.2.2:8081/` | `http://IP_DE_TU_PC:8081/` |
| ms-barberias | `http://10.0.2.2:8082/` | `http://IP_DE_TU_PC:8082/` |
| ms-reservas | `http://10.0.2.2:8083/` | `http://IP_DE_TU_PC:8083/` |

Como es HTTP (sin HTTPS), la app necesita permitir tráfico en texto plano hacia esas direcciones (`network_security_config.xml` o `android:usesCleartextTraffic="true"` solo para desarrollo) y el permiso `android.permission.INTERNET`.

## 9. Problemas comunes

| Síntoma | Causa probable | Solución |
|---|---|---|
| `Falta JWT_SECRET en el archivo .env` | No creaste `.env` | Paso 2 |
| `port is already allocated` en 3307/8081/8082/8083 | Otro programa usa ese puerto | Cambia `MYSQL_PORT` o `*_PORT` en `.env` |
| `Access denied for user 'barber'` | Cambiaste contraseñas en `.env` después de crear el volumen | `docker compose down -v` y vuelve a levantar |
| Un microservicio se reinicia en bucle | MySQL aún no termina de iniciar o hay un error de configuración | `docker compose logs ms-usuarios` |
| `JWT_SECRET debe tener al menos 32 caracteres` | Secreto muy corto | Alarga `JWT_SECRET` en `.env` |
| La app Android no conecta | Usaste `localhost` en vez de `10.0.2.2`, o falta permiso INTERNET / cleartext | Ver sección 8 |
| `exec /docker-entrypoint-initdb.d/01-crear-bases.sh: no such file` o errores con `\r` | El `.sh` quedó con saltos de línea de Windows (CRLF) | El `.gitattributes` lo evita al clonar; si lo editaste, guárdalo con LF y ejecuta `docker compose down -v` |

## 10. Sin Docker (opcional, para desarrollo)

Si tienes JDK 17, Maven 3.9 y un MySQL local:

1. Crea las bases `barber_usuarios`, `barber_barberias` y `barber_reservas` y un usuario con permisos sobre ellas.
2. Define las variables `DB_USER`, `DB_PASSWORD`, `DB_PORT` y `JWT_SECRET` en tu terminal o en la configuración de ejecución de IntelliJ.
3. En cada carpeta `ms-*` ejecuta `mvn spring-boot:run`.
