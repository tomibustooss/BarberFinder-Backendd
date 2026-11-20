package cl.barberfinder.usuarios.config;

import cl.barberfinder.usuarios.model.Rol;
import cl.barberfinder.usuarios.model.Usuario;
import cl.barberfinder.usuarios.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea un usuario de prueba por cada rol la primera vez que parte el servicio (tabla vacía).
 * Se desactiva con SEED_ENABLED=false.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UsuarioRepository repositorio;
    private final PasswordEncoder encoder;
    private final boolean habilitado;
    private final String password;

    public DataSeeder(UsuarioRepository repositorio,
                      PasswordEncoder encoder,
                      @Value("${app.seed.enabled:true}") boolean habilitado,
                      @Value("${app.seed.password:Demo12345}") String password) {
        this.repositorio = repositorio;
        this.encoder = encoder;
        this.habilitado = habilitado;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (!habilitado || repositorio.count() > 0) {
            return;
        }
        crear("Administrador Demo", "admin@barberfinder.cl", "+56911111111", Rol.ADMIN, null);
        crear("Dueño Demo", "dueno@barberfinder.cl", "+56922222222", Rol.DUENO, null);
        crear("Barbero Demo", "barbero@barberfinder.cl", "+56933333333", Rol.BARBERO, 1L);
        crear("Cliente Demo", "cliente@barberfinder.cl", "+56944444444", Rol.CLIENTE, null);
        log.info("Usuarios de prueba creados (uno por rol). Revisa docs/CREDENCIALES.md");
    }

    private void crear(String nombre, String email, String telefono, Rol rol, Long barberiaId) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setTelefono(telefono);
        u.setPasswordHash(encoder.encode(password));
        u.setRol(rol);
        u.setBarberiaId(barberiaId);
        u.setActivo(true);
        repositorio.save(u);
    }
}
