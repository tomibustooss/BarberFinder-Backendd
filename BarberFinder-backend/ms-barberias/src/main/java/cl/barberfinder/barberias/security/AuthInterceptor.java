package cl.barberfinder.barberias.security;

import cl.barberfinder.barberias.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Lee el header "Authorization: Bearer <token>", deja el usuario en la request
 * y valida los roles declarados con @Requiere.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATRIBUTO = "authUser";

    private final JwtService jwtService;

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Optional<AuthUser> usuario = leerToken(request);
        usuario.ifPresent(u -> request.setAttribute(ATRIBUTO, u));

        if (!(handler instanceof HandlerMethod metodo)) {
            return true;
        }
        Requiere requiere = metodo.getMethodAnnotation(Requiere.class);
        if (requiere == null) {
            return true;
        }
        if (usuario.isEmpty()) {
            throw ApiException.noAutorizado("Debes iniciar sesión (token ausente, inválido o expirado).");
        }
        String[] roles = requiere.roles();
        if (roles.length > 0 && !usuario.get().tieneRol(roles)) {
            throw ApiException.prohibido("Tu rol no tiene permiso para esta acción.");
        }
        return true;
    }

    private Optional<AuthUser> leerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return jwtService.leer(header.substring(7).trim());
    }
}
