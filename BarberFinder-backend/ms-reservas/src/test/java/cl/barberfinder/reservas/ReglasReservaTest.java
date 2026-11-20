package cl.barberfinder.reservas;

import cl.barberfinder.reservas.service.ReglasReserva;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReglasReservaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 20, 12, 30);

    @Test
    void aceptaHoraFuturaDeLaLista() {
        assertNull(ReglasReserva.validarHorario(LocalDate.of(2026, 11, 20), "15:00", AHORA));
        assertNull(ReglasReserva.validarHorario(LocalDate.of(2026, 11, 21), "10:00", AHORA));
    }

    @Test
    void rechazaHoraYaPasadaDeHoy() {
        assertEquals("Esa hora ya pasó.", ReglasReserva.validarHorario(LocalDate.of(2026, 11, 20), "11:00", AHORA));
    }

    @Test
    void rechazaFechaPasada() {
        assertEquals("La fecha ya pasó.", ReglasReserva.validarHorario(LocalDate.of(2026, 11, 19), "15:00", AHORA));
    }

    @Test
    void rechazaHoraFueraDeLaLista() {
        assertNotNull(ReglasReserva.validarHorario(LocalDate.of(2026, 11, 21), "14:00", AHORA));
        assertNotNull(ReglasReserva.validarHorario(LocalDate.of(2026, 11, 21), null, AHORA));
    }

    @Test
    void rechazaFechaMuyLejana() {
        LocalDate limite = AHORA.toLocalDate().plusDays(ReglasReserva.DIAS_MAXIMOS);
        assertNull(ReglasReserva.validarHorario(limite, "10:00", AHORA));
        assertNotNull(ReglasReserva.validarHorario(limite.plusDays(1), "10:00", AHORA));
    }

    @Test
    void rechazaFechaNula() {
        assertEquals("Selecciona una fecha.", ReglasReserva.validarHorario(null, "10:00", AHORA));
    }
}
