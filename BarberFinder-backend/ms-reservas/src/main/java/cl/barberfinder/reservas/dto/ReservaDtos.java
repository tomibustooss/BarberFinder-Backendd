package cl.barberfinder.reservas.dto;

import cl.barberfinder.reservas.model.EstadoReserva;
import cl.barberfinder.reservas.model.Reserva;
import cl.barberfinder.reservas.model.TipoServicio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ReservaDtos {

    /** Mismas reglas que BookingValidator en la app. */
    public static final String REGEX_NOMBRE = "^[\\p{L}][\\p{L} .'-]{1,59}$";
    public static final String REGEX_TELEFONO = "^\\+?\\d{8,12}$";
    public static final String REGEX_HORA = "^([01]\\d|2[0-3]):[0-5]\\d$";

    private ReservaDtos() {
    }

    public record ReservaRequest(
            @NotNull(message = "Selecciona una barbería.")
            Long barberiaId,

            @NotNull(message = "Selecciona un servicio.")
            TipoServicio servicio,

            @NotNull(message = "Selecciona una fecha.")
            LocalDate fecha,

            @NotBlank(message = "Selecciona una hora.")
            @Pattern(regexp = REGEX_HORA, message = "La hora debe tener formato HH:mm.")
            String hora,

            @NotBlank(message = "El nombre es obligatorio.")
            @Pattern(regexp = REGEX_NOMBRE, message = "Ingresa un nombre válido (solo letras, 2 a 60 caracteres).")
            String nombreCliente,

            @NotBlank(message = "El teléfono es obligatorio.")
            @Pattern(regexp = REGEX_TELEFONO, message = "Ingresa un teléfono válido (8 a 12 dígitos).")
            String telefonoCliente
    ) {
    }

    public record ReprogramarRequest(
            @NotNull(message = "Selecciona una fecha.")
            LocalDate fecha,

            @NotBlank(message = "Selecciona una hora.")
            @Pattern(regexp = REGEX_HORA, message = "La hora debe tener formato HH:mm.")
            String hora
    ) {
    }

    public record CambiarEstadoRequest(
            @NotNull(message = "El estado es obligatorio (CONFIRMADA, ATENDIDA o NO_ASISTIO).")
            EstadoReserva estado
    ) {
    }

    public record ReservaResponse(
            Long id,
            Long clienteId,
            Long barberiaId,
            String barberiaNombre,
            TipoServicio servicio,
            String servicioEtiqueta,
            Integer precio,
            LocalDate fecha,
            String hora,
            String nombreCliente,
            String telefonoCliente,
            EstadoReserva estado,
            String estadoEtiqueta,
            LocalDateTime creadaEn,
            LocalDateTime actualizadaEn
    ) {
        public static ReservaResponse desde(Reserva r) {
            return new ReservaResponse(
                    r.getId(),
                    r.getClienteId(),
                    r.getBarberiaId(),
                    r.getBarberiaNombre(),
                    r.getServicio(),
                    r.getServicio().getEtiqueta(),
                    r.getPrecio(),
                    r.getFecha(),
                    r.getHora(),
                    r.getNombreCliente(),
                    r.getTelefonoCliente(),
                    r.getEstado(),
                    r.getEstado().getEtiqueta(),
                    r.getCreadaEn(),
                    r.getActualizadaEn()
            );
        }
    }

    public record HoraDisponible(String hora, boolean disponible) {
    }

    public record DisponibilidadResponse(Long barberiaId, LocalDate fecha, List<HoraDisponible> horas) {
    }
}
