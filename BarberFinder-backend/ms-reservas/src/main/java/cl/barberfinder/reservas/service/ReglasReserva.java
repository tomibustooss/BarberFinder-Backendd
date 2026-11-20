package cl.barberfinder.reservas.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Reglas de horario equivalentes a DemoAvailability de la app.
 * Se mantienen separadas del servicio para poder probarlas sin base de datos.
 */
public final class ReglasReserva {

    public static final List<String> HORAS = List.of("10:00", "11:00", "12:00", "13:00", "15:00", "16:00", "17:00", "18:00");
    public static final int DIAS_MAXIMOS = 30;

    private ReglasReserva() {
    }

    /** Devuelve el mensaje de error, o null si la fecha y hora son válidas. */
    public static String validarHorario(LocalDate fecha, String hora, LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        if (fecha == null) {
            return "Selecciona una fecha.";
        }
        if (fecha.isBefore(hoy)) {
            return "La fecha ya pasó.";
        }
        if (fecha.isAfter(hoy.plusDays(DIAS_MAXIMOS))) {
            return "Solo se puede reservar hasta " + DIAS_MAXIMOS + " días hacia adelante.";
        }
        if (hora == null || !HORAS.contains(hora)) {
            return "La hora no es válida. Horas disponibles: " + String.join(", ", HORAS) + ".";
        }
        if (LocalDateTime.of(fecha, LocalTime.parse(hora)).isBefore(ahora)) {
            return "Esa hora ya pasó.";
        }
        return null;
    }
}
