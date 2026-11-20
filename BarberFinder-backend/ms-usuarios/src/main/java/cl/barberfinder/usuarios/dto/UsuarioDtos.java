package cl.barberfinder.usuarios.dto;

import cl.barberfinder.usuarios.model.Rol;
import cl.barberfinder.usuarios.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

/**
 * DTOs de entrada y salida del microservicio de usuarios.
 * Las reglas de nombre y teléfono son las mismas que usa la app (BookingValidator).
 */
public final class UsuarioDtos {

    public static final String REGEX_NOMBRE = "^[\\p{L}][\\p{L} .'-]{1,59}$";
    public static final String REGEX_TELEFONO = "^\\+?\\d{8,12}$";
    public static final String REGEX_PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";

    public static final String MSG_NOMBRE = "Ingresa un nombre válido (solo letras, 2 a 60 caracteres).";
    public static final String MSG_TELEFONO = "Ingresa un teléfono válido (8 a 12 dígitos).";
    public static final String MSG_PASSWORD = "La contraseña debe tener entre 8 y 64 caracteres, con al menos una letra y un número.";

    private UsuarioDtos() {
    }

    public record RegistroRequest(
            @NotBlank(message = "El nombre es obligatorio.")
            @Pattern(regexp = REGEX_NOMBRE, message = MSG_NOMBRE)
            String nombre,

            @NotBlank(message = "El correo es obligatorio.")
            @Email(message = "Ingresa un correo válido.")
            String email,

            @NotBlank(message = "El teléfono es obligatorio.")
            @Pattern(regexp = REGEX_TELEFONO, message = MSG_TELEFONO)
            String telefono,

            @NotBlank(message = "La contraseña es obligatoria.")
            @Pattern(regexp = REGEX_PASSWORD, message = MSG_PASSWORD)
            String password
    ) {
    }

    public record LoginRequest(
            @NotBlank(message = "El correo es obligatorio.")
            @Email(message = "Ingresa un correo válido.")
            String email,

            @NotBlank(message = "La contraseña es obligatoria.")
            String password
    ) {
    }

    public record AuthResponse(String token, String tipo, long expiraEnMinutos, UsuarioResponse usuario) {
    }

    public record RecuperarRequest(
            @NotBlank(message = "El correo es obligatorio.")
            @Email(message = "Ingresa un correo válido.")
            String email
    ) {
    }

    /**
     * codigoDemo solo viene informado cuando RECOVERY_DEMO_MODE=true, porque el proyecto no envía correos reales.
     */
    public record RecuperarResponse(String mensaje, String codigoDemo, int expiraEnMinutos) {
    }

    public record RestablecerRequest(
            @NotBlank(message = "El correo es obligatorio.")
            @Email(message = "Ingresa un correo válido.")
            String email,

            @NotBlank(message = "El código es obligatorio.")
            @Pattern(regexp = "^\\d{6}$", message = "El código tiene 6 dígitos.")
            String codigo,

            @NotBlank(message = "La nueva contraseña es obligatoria.")
            @Pattern(regexp = REGEX_PASSWORD, message = MSG_PASSWORD)
            String nuevaPassword
    ) {
    }

    public record ActualizarPerfilRequest(
            @NotBlank(message = "El nombre es obligatorio.")
            @Pattern(regexp = REGEX_NOMBRE, message = MSG_NOMBRE)
            String nombre,

            @NotBlank(message = "El teléfono es obligatorio.")
            @Pattern(regexp = REGEX_TELEFONO, message = MSG_TELEFONO)
            String telefono
    ) {
    }

    public record CambiarPasswordRequest(
            @NotBlank(message = "La contraseña actual es obligatoria.")
            String passwordActual,

            @NotBlank(message = "La nueva contraseña es obligatoria.")
            @Pattern(regexp = REGEX_PASSWORD, message = MSG_PASSWORD)
            String nuevaPassword
    ) {
    }

    public record CrearUsuarioRequest(
            @NotBlank(message = "El nombre es obligatorio.")
            @Pattern(regexp = REGEX_NOMBRE, message = MSG_NOMBRE)
            String nombre,

            @NotBlank(message = "El correo es obligatorio.")
            @Email(message = "Ingresa un correo válido.")
            String email,

            @NotBlank(message = "El teléfono es obligatorio.")
            @Pattern(regexp = REGEX_TELEFONO, message = MSG_TELEFONO)
            String telefono,

            @NotBlank(message = "La contraseña es obligatoria.")
            @Pattern(regexp = REGEX_PASSWORD, message = MSG_PASSWORD)
            String password,

            @NotNull(message = "El rol es obligatorio.")
            Rol rol,

            Long barberiaId
    ) {
    }

    public record CambiarRolRequest(
            @NotNull(message = "El rol es obligatorio.")
            Rol rol,

            Long barberiaId
    ) {
    }

    public record UsuarioResponse(
            Long id,
            String nombre,
            String email,
            String telefono,
            Rol rol,
            String rolDescripcion,
            Long barberiaId,
            boolean activo,
            LocalDateTime creadoEn
    ) {
        public static UsuarioResponse desde(Usuario u) {
            return new UsuarioResponse(
                    u.getId(),
                    u.getNombre(),
                    u.getEmail(),
                    u.getTelefono(),
                    u.getRol(),
                    u.getRol().getDescripcion(),
                    u.getBarberiaId(),
                    u.isActivo(),
                    u.getCreadoEn()
            );
        }
    }

    public record MensajeResponse(String mensaje) {
    }
}
