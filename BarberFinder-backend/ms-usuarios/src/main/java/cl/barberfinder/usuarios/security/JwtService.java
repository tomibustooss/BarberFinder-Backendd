package cl.barberfinder.usuarios.security;

import cl.barberfinder.usuarios.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/**
 * Emite y valida los JWT. Los otros microservicios validan con el mismo JWT_SECRET.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long expiraMinutos;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                      @Value("${app.jwt.expiration-minutes:120}") long expiraMinutos) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres.");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.expiraMinutos = expiraMinutos;
    }

    public long getExpiraMinutos() {
        return expiraMinutos;
    }

    public String generar(Usuario usuario) {
        Date ahora = new Date();
        Date expira = new Date(ahora.getTime() + expiraMinutos * 60_000L);
        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("email", usuario.getEmail())
                .claim("rol", usuario.getRol().name())
                .issuedAt(ahora)
                .expiration(expira);
        if (usuario.getBarberiaId() != null) {
            builder.claim("barberiaId", usuario.getBarberiaId());
        }
        return builder.signWith(clave).compact();
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
