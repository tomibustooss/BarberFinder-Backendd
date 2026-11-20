package cl.barberfinder.usuarios.controller;

import cl.barberfinder.usuarios.dto.UsuarioDtos.AuthResponse;
import cl.barberfinder.usuarios.dto.UsuarioDtos.LoginRequest;
import cl.barberfinder.usuarios.dto.UsuarioDtos.MensajeResponse;
import cl.barberfinder.usuarios.dto.UsuarioDtos.RecuperarRequest;
import cl.barberfinder.usuarios.dto.UsuarioDtos.RecuperarResponse;
import cl.barberfinder.usuarios.dto.UsuarioDtos.RegistroRequest;
import cl.barberfinder.usuarios.dto.UsuarioDtos.RestablecerRequest;
import cl.barberfinder.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService servicio;

    public AuthController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegistroRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrar(req));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return servicio.login(req);
    }

    @PostMapping("/recuperar")
    public RecuperarResponse recuperar(@Valid @RequestBody RecuperarRequest req) {
        return servicio.solicitarRecuperacion(req.email());
    }

    @PostMapping("/restablecer")
    public MensajeResponse restablecer(@Valid @RequestBody RestablecerRequest req) {
        servicio.restablecerPassword(req);
        return new MensajeResponse("Contraseña actualizada. Ya puedes iniciar sesión.");
    }
}
