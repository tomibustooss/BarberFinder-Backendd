package cl.barberfinder.usuarios.controller;

import cl.barberfinder.usuarios.dto.UsuarioDtos.ActualizarPerfilRequest;
import cl.barberfinder.usuarios.dto.UsuarioDtos.CambiarPasswordRequest;
import cl.barberfinder.usuarios.dto.UsuarioDtos.CambiarRolRequest;
import cl.barberfinder.usuarios.dto.UsuarioDtos.CrearUsuarioRequest;
import cl.barberfinder.usuarios.dto.UsuarioDtos.MensajeResponse;
import cl.barberfinder.usuarios.dto.UsuarioDtos.UsuarioResponse;
import cl.barberfinder.usuarios.model.Rol;
import cl.barberfinder.usuarios.security.AuthInterceptor;
import cl.barberfinder.usuarios.security.AuthUser;
import cl.barberfinder.usuarios.security.Requiere;
import cl.barberfinder.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @Requiere
    @GetMapping("/me")
    public UsuarioResponse miPerfil(@RequestAttribute(AuthInterceptor.ATRIBUTO) AuthUser auth) {
        return servicio.obtener(auth.id());
    }

    @Requiere
    @PutMapping("/me")
    public UsuarioResponse actualizarPerfil(@RequestAttribute(AuthInterceptor.ATRIBUTO) AuthUser auth,
                                            @Valid @RequestBody ActualizarPerfilRequest req) {
        return servicio.actualizarPerfil(auth.id(), req);
    }

    @Requiere
    @PutMapping("/me/password")
    public MensajeResponse cambiarPassword(@RequestAttribute(AuthInterceptor.ATRIBUTO) AuthUser auth,
                                           @Valid @RequestBody CambiarPasswordRequest req) {
        servicio.cambiarPassword(auth.id(), req);
        return new MensajeResponse("Contraseña actualizada.");
    }

    @Requiere(roles = {"ADMIN"})
    @GetMapping
    public List<UsuarioResponse> listar(@RequestParam(required = false) Rol rol) {
        return servicio.listar(rol);
    }

    @Requiere(roles = {"ADMIN"})
    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @Requiere(roles = {"ADMIN"})
    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearComoAdmin(req));
    }

    @Requiere(roles = {"ADMIN"})
    @PutMapping("/{id}/rol")
    public UsuarioResponse cambiarRol(@RequestAttribute(AuthInterceptor.ATRIBUTO) AuthUser auth,
                                      @PathVariable Long id,
                                      @Valid @RequestBody CambiarRolRequest req) {
        return servicio.cambiarRol(id, req, auth.id());
    }

    @Requiere(roles = {"ADMIN"})
    @PatchMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@RequestAttribute(AuthInterceptor.ATRIBUTO) AuthUser auth,
                                         @PathVariable Long id,
                                         @RequestParam boolean activo) {
        return servicio.cambiarEstado(id, activo, auth.id());
    }

    @Requiere(roles = {"ADMIN"})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@RequestAttribute(AuthInterceptor.ATRIBUTO) AuthUser auth,
                                         @PathVariable Long id) {
        servicio.eliminar(id, auth.id());
        return ResponseEntity.noContent().build();
    }
}
