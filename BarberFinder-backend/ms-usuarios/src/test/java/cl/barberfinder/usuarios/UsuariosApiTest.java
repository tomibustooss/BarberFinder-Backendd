package cl.barberfinder.usuarios;

import cl.barberfinder.usuarios.model.Rol;
import cl.barberfinder.usuarios.model.Usuario;
import cl.barberfinder.usuarios.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UsuariosApiTest {

    private static final String PASSWORD = "Clave1234";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository repositorio;

    @Autowired
    private PasswordEncoder encoder;

    @BeforeEach
    void limpiar() {
        repositorio.deleteAll();
    }

    @Test
    void registroCreaClienteYDevuelveToken() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson("Juan Pérez", "juan@test.cl")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.usuario.email").value("juan@test.cl"));
    }

    @Test
    void registroConCorreoRepetidoDevuelve409() throws Exception {
        registrar("Juan Pérez", "juan@test.cl");
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson("Otro Juan", "JUAN@test.cl")))
                .andExpect(status().isConflict());
    }

    @Test
    void registroConDatosInvalidosDevuelveErroresPorCampo() throws Exception {
        String json = """
                {"nombre":"1234","email":"no-es-correo","telefono":"123","password":"corta"}
                """;
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.telefono").exists())
                .andExpect(jsonPath("$.campos.password").exists());
    }

    @Test
    void loginCorrectoEIncorrecto() throws Exception {
        registrar("Juan Pérez", "juan@test.cl");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("juan@test.cl", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("juan@test.cl", "Incorrecta99")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void perfilRequiereTokenYSePuedeModificar() throws Exception {
        String token = registrar("Juan Pérez", "juan@test.cl");

        mvc.perform(get("/api/usuarios/me"))
                .andExpect(status().isUnauthorized());

        mvc.perform(put("/api/usuarios/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Juan Andres Perez\",\"telefono\":\"+56987654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Juan Andres Perez"))
                .andExpect(jsonPath("$.telefono").value("+56987654321"));
    }

    @Test
    void cambiarPasswordValidaLaActual() throws Exception {
        String token = registrar("Juan Pérez", "juan@test.cl");

        mvc.perform(put("/api/usuarios/me/password").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"Mala12345\",\"nuevaPassword\":\"Nueva12345\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/usuarios/me/password").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"" + PASSWORD + "\",\"nuevaPassword\":\"Nueva12345\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("juan@test.cl", "Nueva12345")))
                .andExpect(status().isOk());
    }

    @Test
    void recuperacionDeContrasenaConCodigo() throws Exception {
        registrar("Juan Pérez", "juan@test.cl");

        MvcResult r = mvc.perform(post("/api/auth/recuperar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"juan@test.cl\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoDemo").isNotEmpty())
                .andReturn();
        String codigo = JsonPath.read(r.getResponse().getContentAsString(), "$.codigoDemo");

        mvc.perform(post("/api/auth/restablecer").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"juan@test.cl\",\"codigo\":\"000000\",\"nuevaPassword\":\"Otra12345\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/restablecer").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"juan@test.cl\",\"codigo\":\"" + codigo + "\",\"nuevaPassword\":\"Otra12345\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("juan@test.cl", "Otra12345")))
                .andExpect(status().isOk());
    }

    @Test
    void recuperarConCorreoInexistenteNoRevelaNada() throws Exception {
        mvc.perform(post("/api/auth/recuperar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@test.cl\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoDemo").doesNotExist());
    }

    @Test
    void soloAdminPuedeAdministrarUsuarios() throws Exception {
        String tokenCliente = registrar("Juan Pérez", "juan@test.cl");
        String tokenAdmin = crearYLoguear("Admin Test", "admin@test.cl", Rol.ADMIN, null);

        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        String json = """
                {"nombre":"Pedro Barbero","email":"pedro@test.cl","telefono":"+56911112222",
                 "password":"Clave1234","rol":"BARBERO","barberiaId":1}
                """;
        MvcResult creado = mvc.perform(post("/api/usuarios").header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("BARBERO"))
                .andExpect(jsonPath("$.barberiaId").value(1))
                .andReturn();
        Integer id = JsonPath.read(creado.getResponse().getContentAsString(), "$.id");

        mvc.perform(put("/api/usuarios/" + id + "/rol").header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"DUENO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("DUENO"));

        mvc.perform(delete("/api/usuarios/" + id).header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());
    }

    @Test
    void barberoSinBarberiaEsRechazado() throws Exception {
        String tokenAdmin = crearYLoguear("Admin Test", "admin@test.cl", Rol.ADMIN, null);
        String json = """
                {"nombre":"Pedro Barbero","email":"pedro@test.cl","telefono":"+56911112222",
                 "password":"Clave1234","rol":"BARBERO"}
                """;
        mvc.perform(post("/api/usuarios").header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tokenInvalidoEsRechazado() throws Exception {
        mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer token.falso.123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void estadoEsPublico() throws Exception {
        mvc.perform(get("/api/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OK"));
    }

    private String registrar(String nombre, String email) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson(nombre, email)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.token");
    }

    private String crearYLoguear(String nombre, String email, Rol rol, Long barberiaId) throws Exception {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setTelefono("+56900000000");
        u.setPasswordHash(encoder.encode(PASSWORD));
        u.setRol(rol);
        u.setBarberiaId(barberiaId);
        repositorio.save(u);
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.token");
    }

    private static String registroJson(String nombre, String email) {
        return "{\"nombre\":\"" + nombre + "\",\"email\":\"" + email
                + "\",\"telefono\":\"+56912345678\",\"password\":\"" + PASSWORD + "\"}";
    }

    private static String loginJson(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}
