package cl.barberfinder.barberias.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Valida los JWT emitidos por ms-usuarios (mismo JWT_SECRET).
 */
@Service
public class JwtService {

    private final SecretKey clave;

    public JwtService(@Value("${app.jwt.secret}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres.");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
    }

    public Optional<AuthUser> leer(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Object barberia = claims.get("barberiaId");
            Long barberiaId = barberia instanceof Number n ? n.longValue() : null;
            return Optional.of(new AuthUser(
                    Long.parseLong(claims.getSubject()),
                    claims.get("email", String.class),
                    claims.get("rol", String.class),
                    barberiaId
            ));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
