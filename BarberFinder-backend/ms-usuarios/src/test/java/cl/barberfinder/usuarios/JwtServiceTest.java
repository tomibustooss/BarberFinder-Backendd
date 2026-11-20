package cl.barberfinder.usuarios;

import cl.barberfinder.usuarios.model.Rol;
import cl.barberfinder.usuarios.model.Usuario;
import cl.barberfinder.usuarios.security.AuthUser;
import cl.barberfinder.usuarios.security.JwtService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRETO = "clave-de-pruebas-unitarias-de-32-caracteres-o-mas";

    @Test
    void generaYLeeElTokenConSusDatos() {
        JwtService jwt = new JwtService(SECRETO, 30);
        Usuario u = new Usuario();
        u.setId(7L);
        u.setEmail("barbero@test.cl");
        u.setRol(Rol.BARBERO);
        u.setBarberiaId(3L);

        Optional<AuthUser> leido = jwt.leer(jwt.generar(u));

        assertTrue(leido.isPresent());
        assertEquals(7L, leido.get().id());
        assertEquals("BARBERO", leido.get().rol());
        assertEquals(3L, leido.get().barberiaId());
    }

    @Test
    void rechazaTokenFirmadoConOtraClave() {
        JwtService emisor = new JwtService("otra-clave-distinta-de-al-menos-32-caracteres", 30);
        JwtService receptor = new JwtService(SECRETO, 30);
        Usuario u = new Usuario();
        u.setId(1L);
        u.setEmail("a@test.cl");
        u.setRol(Rol.CLIENTE);

        assertTrue(receptor.leer(emisor.generar(u)).isEmpty());
    }

    @Test
    void rechazaSecretoCorto() {
        assertThrows(IllegalStateException.class, () -> new JwtService("corto", 30));
    }
}
