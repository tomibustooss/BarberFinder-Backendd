package cl.barberfinder.reservas;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Genera tokens iguales a los que emite ms-usuarios, para probar este servicio de forma aislada. */
final class TokenPrueba {

    static final String SECRETO = "clave-de-pruebas-unitarias-de-32-caracteres-o-mas";

    private TokenPrueba() {
    }

    static String bearer(long id, String rol) {
        return bearer(id, rol, null);
    }

    static String bearer(long id, String rol, Long barberiaId) {
        var builder = Jwts.builder()
                .subject(String.valueOf(id))
                .claim("email", rol.toLowerCase() + id + "@test.cl")
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 600_000));
        if (barberiaId != null) {
            builder.claim("barberiaId", barberiaId);
        }
        return "Bearer " + builder.signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8))).compact();
    }
}
