package cl.barberfinder.reservas;

import cl.barberfinder.reservas.client.CatalogoBarberias;
import cl.barberfinder.reservas.client.CatalogoBarberias.BarberiaInfo;
import cl.barberfinder.reservas.client.CatalogoBarberias.PrecioServicio;
import cl.barberfinder.reservas.exception.ApiException;
import cl.barberfinder.reservas.model.TipoServicio;
import cl.barberfinder.reservas.repository.ReservaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReservasApiTest {

    private static final long CLIENTE_ID = 4L;
    private static final long OTRO_CLIENTE_ID = 5L;
    private static final long DUENO_ID = 2L;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ReservaRepository repositorio;

    @MockBean
    private CatalogoBarberias catalogo;

    private final LocalDate manana = LocalDate.now(ZoneId.of("America/Santiago")).plusDays(1);

    @BeforeEach
    void preparar() {
        repositorio.deleteAll();
        when(catalogo.precio(anyLong(), any(TipoServicio.class))).thenAnswer(inv ->
                new PrecioServicio(inv.getArgument(0), "Barbería Test", DUENO_ID, inv.getArgument(1), 10_500));
        when(catalogo.barberia(anyLong())).thenAnswer(inv ->
                new BarberiaInfo(inv.getArgument(0), "Barbería Test", DUENO_ID, true));
    }

    @Test
    void clienteCreaReservaConPrecioDelMicroservicioDeBarberias() throws Exception {
        mvc.perform(post("/api/reservas").header("Authorization", TokenPrueba.bearer(CLIENTE_ID, "CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(reservaJson(1, "10:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precio").value(10_500))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.clienteId").value((int) CLIENTE_ID))
                .andExpect(jsonPath("$.fecha").value(manana.toString()));
    }

    @Test
    void crearRequiereSesionYRolCliente() throws Exception {
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(reservaJson(1, "10:00")))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/reservas").header("Authorization", TokenPrueba.bearer(3, "BARBERO", 1L))
                        .contentType(MediaType.APPLICATION_JSON).content(reservaJson(1, "10:00")))
                .andExpect(status().isForbidden());
    }

    @Test
    void evitaDobleReservaYMuestraDisponibilidad() throws Exception {
        crearReserva(CLIENTE_ID, 1, "11:00");

        mvc.perform(post("/api/reservas").header("Authorization", TokenPrueba.bearer(OTRO_CLIENTE_ID, "CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(reservaJson(1, "11:00")))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/reservas/disponibilidad").param("barberiaId", "1").param("fecha", manana.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horas.length()").value(8))
                .andExpect(jsonPath("$.horas[1].hora").value("11:00"))
                .andExpect(jsonPath("$.horas[1].disponible").value(false))
                .andExpect(jsonPath("$.horas[0].disponible").value(true));
    }

    @Test
    void validaFormularioYHorario() throws Exception {
        String json = """
                {"barberiaId":1,"servicio":"CORTE_CLASICO","fecha":"%s","hora":"10:00",
                 "nombreCliente":"1234","telefonoCliente":"123"}
                """.formatted(manana);
        mvc.perform(post("/api/reservas").header("Authorization", TokenPrueba.bearer(CLIENTE_ID, "CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombreCliente").exists())
                .andExpect(jsonPath("$.campos.telefonoCliente").exists());

        mvc.perform(post("/api/reservas").header("Authorization", TokenPrueba.bearer(CLIENTE_ID, "CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(reservaJson(1, "14:00")))
                .andExpect(status().isBadRequest());

        String pasada = reservaJson(1, "10:00").replace(manana.toString(), manana.minusDays(3).toString());
        mvc.perform(post("/api/reservas").header("Authorization", TokenPrueba.bearer(CLIENTE_ID, "CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(pasada))
                .andExpect(status().isBadRequest());
    }

    @Test
    void errorDelOtroMicroservicioSeInformaAlCliente() throws Exception {
        when(catalogo.precio(eq(99L), any(TipoServicio.class))).thenThrow(ApiException.noEncontrado("La barbería no existe."));
        when(catalogo.precio(eq(98L), any(TipoServicio.class)))
                .thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de barberías no está disponible."));

        mvc.perform(post("/api/reservas").header("Authorization", TokenPrueba.bearer(CLIENTE_ID, "CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(reservaJson(99, "10:00")))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/reservas").header("Authorization", TokenPrueba.bearer(CLIENTE_ID, "CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(reservaJson(98, "10:00")))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void clienteVeSoloSusReservasYPuedeCancelarlas() throws Exception {
        long id = crearReserva(CLIENTE_ID, 1, "12:00");
        crearReserva(OTRO_CLIENTE_ID, 1, "13:00");

        mvc.perform(get("/api/reservas/mias").header("Authorization", TokenPrueba.bearer(CLIENTE_ID, "CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(patch("/api/reservas/" + id + "/cancelar").header("Authorization", TokenPrueba.bearer(OTRO_CLIENTE_ID, "CLIENTE")))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/reservas/" + id + "/cancelar").header("Authorization", TokenPrueba.bearer(CLIENTE_ID, "CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        crearReserva(OTRO_CLIENTE_ID, 1, "12:00");
    }

    @Test
    void clienteReprogramaSuReserva() throws Exception {
        long id = crearReserva(CLIENTE_ID, 1, "15:00");
        String body = "{\"fecha\":\"" + manana.plusDays(1) + "\",\"hora\":\"16:00\"}";

        mvc.perform(put("/api/reservas/" + id).header("Authorization", TokenPrueba.bearer(CLIENTE_ID, "CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hora").value("16:00"))
                .andExpect(jsonPath("$.fecha").value(manana.plusDays(1).toString()));
    }

    @Test
    void barberoVeYAtiendeSoloLaAgendaDeSuBarberia() throws Exception {
        long id = crearReserva(CLIENTE_ID, 1, "17:00");

        mvc.perform(get("/api/reservas/barberia/1").header("Authorization", TokenPrueba.bearer(3, "BARBERO", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(get("/api/reservas/barberia/1").header("Authorization", TokenPrueba.bearer(6, "BARBERO", 2L)))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/reservas/" + id + "/estado").header("Authorization", TokenPrueba.bearer(3, "BARBERO", 1L))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ATENDIDA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ATENDIDA"));

        mvc.perform(post("/api/reservas").header("Authorization", TokenPrueba.bearer(OTRO_CLIENTE_ID, "CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(reservaJson(1, "17:00")))
                .andExpect(status().isConflict());
    }

    @Test
    void duenoVeLasReservasDeSusBarberias() throws Exception {
        crearReserva(CLIENTE_ID, 1, "18:00");

        mvc.perform(get("/api/reservas/barberia/1").header("Authorization", TokenPrueba.bearer(DUENO_ID, "DUENO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(get("/api/reservas/barberia/1").header("Authorization", TokenPrueba.bearer(9, "DUENO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void soloAdminListaTodoYElimina() throws Exception {
        long id = crearReserva(CLIENTE_ID, 1, "10:00");

        mvc.perform(get("/api/reservas").header("Authorization", TokenPrueba.bearer(CLIENTE_ID, "CLIENTE")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/reservas").header("Authorization", TokenPrueba.bearer(1, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(delete("/api/reservas/" + id).header("Authorization", TokenPrueba.bearer(1, "ADMIN")))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/reservas/" + id).header("Authorization", TokenPrueba.bearer(1, "ADMIN")))
                .andExpect(status().isNotFound());
    }

    private long crearReserva(long clienteId, long barberiaId, String hora) throws Exception {
        MvcResult r = mvc.perform(post("/api/reservas").header("Authorization", TokenPrueba.bearer(clienteId, "CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(reservaJson(barberiaId, hora)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private String reservaJson(long barberiaId, String hora) {
        return """
                {"barberiaId":%d,"servicio":"CORTE_Y_BARBA","fecha":"%s","hora":"%s",
                 "nombreCliente":"Cliente Prueba","telefonoCliente":"+56912345678"}
                """.formatted(barberiaId, manana, hora);
    }
}
