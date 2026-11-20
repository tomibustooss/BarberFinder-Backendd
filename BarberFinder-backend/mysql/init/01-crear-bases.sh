#!/bin/bash
# Se ejecuta UNA sola vez, cuando el volumen de MySQL está vacío.
# Crea una base de datos por microservicio y le da permisos al usuario de la app (DB_USER).
set -e

mysql -uroot -p"$MYSQL_ROOT_PASSWORD" <<-EOSQL
    CREATE DATABASE IF NOT EXISTS barber_usuarios  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    CREATE DATABASE IF NOT EXISTS barber_barberias CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    CREATE DATABASE IF NOT EXISTS barber_reservas  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

    GRANT ALL PRIVILEGES ON barber_usuarios.*  TO '${MYSQL_USER}'@'%';
    GRANT ALL PRIVILEGES ON barber_barberias.* TO '${MYSQL_USER}'@'%';
    GRANT ALL PRIVILEGES ON barber_reservas.*  TO '${MYSQL_USER}'@'%';
    FLUSH PRIVILEGES;
EOSQL

echo "Bases de BarberFinder creadas y asignadas a ${MYSQL_USER}."
