package cl.barberfinder.usuarios.security;

/**
 * Datos del usuario autenticado que viajan dentro del JWT.
 */
public record AuthUser(Long id, String email, String rol, Long barberiaId) {

    public boolean tieneRol(String... roles) {
        for (String r : roles) {
            if (r.equals(rol)) {
                return true;
            }
        }
        return false;
    }

    public boolean esAdmin() {
        return "ADMIN".equals(rol);
    }
}
