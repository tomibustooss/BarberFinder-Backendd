# Evidencias de compilación y pruebas (backend)

Las evidencias se generan **en el computador del equipo** con un solo comando y quedan guardadas en `docs/evidencias/` con fecha y hora en el nombre. Así el profesor puede revisar los logs reales en el repositorio.

## Cómo generarlas

Requisitos: Docker Desktop abierto y el archivo `.env` creado (ver `docs/DOCKER.md`).

```powershell
# Windows PowerShell, desde la raíz del repo backend
.\scripts\evidencias.ps1
```

```bash
# Linux / macOS / Git Bash
bash scripts/evidencias.sh
```

Si PowerShell bloquea el script por la política de ejecución, ejecútalo así una sola vez:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\evidencias.ps1
```

El script hace, en orden:

| Paso | Qué ejecuta | Archivo que genera |
|---|---|---|
| 1 | Versiones de Docker, Java y Maven | `entorno-<fecha>.txt` |
| 2 | Pruebas JUnit 5 + MockMvc + Mockito de los 3 microservicios (`mvn test`; si no tienes Maven, las corre dentro de un contenedor) | `backend-tests-<fecha>.log` y `surefire-<fecha>/` |
| 3 | Compilación de las imágenes Docker | `docker-build-<fecha>.log` |
| 4 | `docker compose up -d` y estado de los contenedores | `docker-up-<fecha>.log`, `docker-ps-<fecha>.txt` |
| 5 | Prueba real de 48 llamadas a los endpoints con los 4 roles | `smoke-endpoints-<fecha>.log` |
| 6 | Resumen con OK / FALLA de cada paso | `RESUMEN-<fecha>.txt` |

Después de ejecutarlo, sube la carpeta `docs/evidencias/` al repositorio (`git add docs/evidencias`).

## Qué cubren las pruebas automatizadas

| Microservicio | Clase de prueba | Qué valida |
|---|---|---|
| ms-usuarios | `UsuariosApiTest` | Registro, correo repetido, validaciones por campo, login correcto e incorrecto, perfil con y sin token, cambio de contraseña, recuperación con código, permisos de ADMIN, barbero sin barbería, token inválido |
| ms-usuarios | `JwtServiceTest` | Generación y lectura del JWT, rechazo de token firmado con otra clave y de secretos cortos |
| ms-barberias | `CalculadoraPreciosTest` | Precio de cada servicio, combo con adicional aplicado una sola vez, límites de $1.000 a $100.000 |
| ms-barberias | `BarberiasApiTest` | Listado público, búsqueda sin tildes, filtro por categoría, desglose de precio, 404, permisos de DUENO/CLIENTE, validación del formulario, solo el dueño edita/elimina, "mis barberías" |
| ms-reservas | `ReglasReservaTest` | Horas válidas, hora pasada, fecha pasada, hora fuera de la lista, límite de 30 días |
| ms-reservas | `ReservasApiTest` | Crear reserva con precio obtenido de ms-barberias (mock), roles, doble reserva, disponibilidad, validaciones, errores 404/503 del otro microservicio, cancelar, reprogramar, agenda de barbero y dueño, permisos de ADMIN |

Las pruebas usan una base H2 en memoria, así que no necesitan MySQL ni Docker para ejecutarse con `mvn test`.

## Registro de ejecuciones

Completa esta tabla cada vez que generes evidencias (o pega el contenido de `RESUMEN-<fecha>.txt`).

| Fecha | Quién | Pruebas backend | Imágenes Docker | Smoke test | Archivo resumen |
|---|---|---|---|---|---|
| | | | | | |

## Capturas recomendadas para el informe o la defensa

1. Docker Desktop con los 4 contenedores `barberfinder-*` en verde.
2. Terminal con el `RESUMEN` del script mostrando todo en OK.
3. Postman con la colección `postman/BarberFinder.postman_collection.json` ejecutada (Runner) sin errores.
4. Adminer (`docker compose --profile herramientas up -d`, http://localhost:8090) mostrando las tablas `usuarios`, `barberias` y `reservas` con datos.
