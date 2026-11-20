package cl.barberfinder.barberias.dto;

import cl.barberfinder.barberias.model.Barberia;
import cl.barberfinder.barberias.model.Categoria;
import cl.barberfinder.barberias.model.TipoServicio;
import cl.barberfinder.barberias.service.CalculadoraPrecios.LineaPrecio;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class BarberiaDtos {

    private BarberiaDtos() {
    }

    public record BarberiaRequest(
            @NotBlank(message = "El nombre es obligatorio.")
            @Size(min = 3, max = 80, message = "El nombre debe tener entre 3 y 80 caracteres.")
            String nombre,

            @NotNull(message = "La categoría es obligatoria (ECONOMICA, INTERMEDIA o PREMIUM).")
            Categoria categoria,

            @NotBlank(message = "La dirección es obligatoria.")
            @Size(max = 150, message = "La dirección admite hasta 150 caracteres.")
            String direccion,

            @Size(max = 60, message = "La comuna admite hasta 60 caracteres.")
            String comuna,

            @Size(max = 300, message = "La descripción admite hasta 300 caracteres.")
            String descripcion,

            @Pattern(regexp = "^$|^\\+?\\d{8,12}$", message = "Ingresa un teléfono válido (8 a 12 dígitos).")
            String telefono,

            @NotNull(message = "El precio del corte clásico es obligatorio.")
            @Min(value = 1000, message = "El precio mínimo es $1.000.")
            @Max(value = 100000, message = "El precio máximo es $100.000.")
            Integer precioCorteClasico,

            @NotNull(message = "El precio del corte moderno es obligatorio.")
            @Min(value = 1000, message = "El precio mínimo es $1.000.")
            @Max(value = 100000, message = "El precio máximo es $100.000.")
            Integer precioCorteModerno,

            @NotNull(message = "El precio de la barba es obligatorio.")
            @Min(value = 1000, message = "El precio mínimo es $1.000.")
            @Max(value = 100000, message = "El precio máximo es $100.000.")
            Integer precioBarba,

            @DecimalMin(value = "-90.0", message = "Latitud fuera de rango.")
            @DecimalMax(value = "90.0", message = "Latitud fuera de rango.")
            Double latitud,

            @DecimalMin(value = "-180.0", message = "Longitud fuera de rango.")
            @DecimalMax(value = "180.0", message = "Longitud fuera de rango.")
            Double longitud,

            Long duenoId
    ) {
    }

    public record BarberiaResponse(
            Long id,
            String nombre,
            Categoria categoria,
            String categoriaEtiqueta,
            String direccion,
            String comuna,
            String descripcion,
            String telefono,
            Integer precioCorteClasico,
            Integer precioCorteModerno,
            Integer precioBarba,
            Integer precioDesde,
            Double latitud,
            Double longitud,
            Long duenoId,
            boolean activa
    ) {
        public static BarberiaResponse desde(Barberia b) {
            return new BarberiaResponse(
                    b.getId(),
                    b.getNombre(),
                    b.getCategoria(),
                    b.getCategoria().getEtiqueta(),
                    b.getDireccion(),
                    b.getComuna(),
                    b.getDescripcion(),
                    b.getTelefono(),
                    b.getPrecioCorteClasico(),
                    b.getPrecioCorteModerno(),
                    b.getPrecioBarba(),
                    Math.min(b.getPrecioCorteClasico(), b.getPrecioCorteModerno()),
                    b.getLatitud(),
                    b.getLongitud(),
                    b.getDuenoId(),
                    b.isActiva()
            );
        }
    }

    public record PrecioResponse(
            Long barberiaId,
            String barberiaNombre,
            Long duenoId,
            TipoServicio servicio,
            String servicioEtiqueta,
            String servicioDescripcion,
            List<LineaPrecio> lineas,
            int total
    ) {
    }
}
