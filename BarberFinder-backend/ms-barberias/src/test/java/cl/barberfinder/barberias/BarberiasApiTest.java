package cl.barberfinder.barberias;

import cl.barberfinder.barberias.model.Barberia;
import cl.barberfinder.barberias.model.Categoria;
import cl.barberfinder.barberias.repository.BarberiaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BarberiasApiTest {

    private static final long DUENO_ID = 2L;
    private static final long OTRO_DUENO_ID = 9L;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private BarberiaRepository repositorio;

    private Long trebolId;

    @BeforeEach
    void preparar() {
        repositorio.deleteAll();
        trebolId = guardar("Barbería El Trébol", Categoria.ECONOMICA, 6_000, 7_000, 3_000).getId();
        guardar("Don Filo", Categoria.INTERMEDIA, 11_000, 13_000, 5_000);
        guardar("Royal Blade Studio", Categoria.PREMIUM, 22_000, 26_000, 10_000);
    }

    @Test
    void listadoEsPublico() throws Exception {
        mvc.perform(get("/api/barberias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void buscaIgnorandoTildesYMayusculas() throws Exception {
        mvc.perform(get("/api/barberias").param("q", "TREBOL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(trebolId.intValue()));
    }

    @Test
    void filtraPorCategoria() throws Exception {
        mvc.perform(get("/api/barberias").param("categoria", "PREMIUM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].categoria").value("PREMIUM"));
    }

    @Test
    void precioDelComboTieneDesglose() throws Exception {
        mvc.perform(get("/api/barberias/" + trebolId + "/precio").param("servicio", "CORTE_Y_BARBA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10_500))
                .andExpect(jsonPath("$.lineas.length()").value(3))
                .andExpect(jsonPath("$.duenoId").value((int) DUENO_ID));
    }

    @Test
    void serviciosDevuelveLosTres() throws Exception {
        mvc.perform(get("/api/barberias/" + trebolId + "/servicios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void barberiaInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/barberias/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearRequiereSesionYRolDeDueno() throws Exception {
        mvc.perform(post("/api/barberias").contentType(MediaType.APPLICATION_JSON).content(nuevaJson()))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/barberias").header("Authorization", TokenPrueba.bearer(4, "CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(nuevaJson()))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/barberias").header("Authorization", TokenPrueba.bearer(DUENO_ID, "DUENO"))
                        .contentType(MediaType.APPLICATION_JSON).content(nuevaJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duenoId").value((int) DUENO_ID))
                .andExpect(jsonPath("$.precioDesde").value(8_000));
    }

    @Test
    void validaElFormulario() throws Exception {
        String json = """
                {"nombre":"","categoria":null,"direccion":"","precioCorteClasico":10,
                 "precioCorteModerno":200000,"precioBarba":null}
                """;
        mvc.perform(post("/api/barberias").header("Authorization", TokenPrueba.bearer(DUENO_ID, "DUENO"))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists())
                .andExpect(jsonPath("$.campos.categoria").exists())
                .andExpect(jsonPath("$.campos.precioCorteClasico").exists())
                .andExpect(jsonPath("$.campos.precioCorteModerno").exists())
                .andExpect(jsonPath("$.campos.precioBarba").exists());
    }

    @Test
    void soloElDuenoOAdminPuedenEditarYEliminar() throws Exception {
        mvc.perform(put("/api/barberias/" + trebolId).header("Authorization", TokenPrueba.bearer(OTRO_DUENO_ID, "DUENO"))
                        .contentType(MediaType.APPLICATION_JSON).content(nuevaJson()))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/barberias/" + trebolId).header("Authorization", TokenPrueba.bearer(DUENO_ID, "DUENO"))
                        .contentType(MediaType.APPLICATION_JSON).content(nuevaJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Barberia Nueva"));

        mvc.perform(patch("/api/barberias/" + trebolId + "/estado").param("activa", "false")
                        .header("Authorization", TokenPrueba.bearer(1, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false));

        mvc.perform(delete("/api/barberias/" + trebolId).header("Authorization", TokenPrueba.bearer(OTRO_DUENO_ID, "DUENO")))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/api/barberias/" + trebolId).header("Authorization", TokenPrueba.bearer(DUENO_ID, "DUENO")))
                .andExpect(status().isNoContent());
    }

    @Test
    void duenoVeSoloSusBarberias() throws Exception {
        MvcResult r = mvc.perform(post("/api/barberias").header("Authorization", TokenPrueba.bearer(OTRO_DUENO_ID, "DUENO"))
                        .contentType(MediaType.APPLICATION_JSON).content(nuevaJson()))
                .andExpect(status().isCreated())
                .andReturn();
        Integer id = JsonPath.read(r.getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/barberias/mias").header("Authorization", TokenPrueba.bearer(OTRO_DUENO_ID, "DUENO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));
    }

    private Barberia guardar(String nombre, Categoria categoria, int clasico, int moderno, int barba) {
        Barberia b = new Barberia();
        b.setNombre(nombre);
        b.setCategoria(categoria);
        b.setDireccion("Dirección de prueba 123");
        b.setPrecioCorteClasico(clasico);
        b.setPrecioCorteModerno(moderno);
        b.setPrecioBarba(barba);
        b.setDuenoId(DUENO_ID);
        b.setActiva(true);
        return repositorio.save(b);
    }

    private static String nuevaJson() {
        return """
                {"nombre":"Barberia Nueva","categoria":"INTERMEDIA","direccion":"Calle Prueba 456",
                 "comuna":"Providencia","descripcion":"Prueba","telefono":"+56912345678",
                 "precioCorteClasico":9000,"precioCorteModerno":8000,"precioBarba":4000}
                """;
    }
}
