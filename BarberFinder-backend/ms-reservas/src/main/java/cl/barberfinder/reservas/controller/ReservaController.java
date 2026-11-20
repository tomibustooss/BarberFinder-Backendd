package cl.barberfinder.reservas.controller;

import cl.barberfinder.reservas.dto.ReservaDtos.CambiarEstadoRequest;
import cl.barberfinder.reservas.dto.ReservaDtos.DisponibilidadResponse;
import cl.barberfinder.reservas.dto.ReservaDtos.ReprogramarRequest;
import cl.barberfinder.reservas.dto.ReservaDtos.ReservaRequest;
import cl.barberfinder.reservas.dto.ReservaDtos.ReservaResponse;
import cl.barberfinder.reservas.model.EstadoReserva;
import cl.barberfinder.reservas.security.AuthInterceptor;
import cl.barberfinder.reservas.security.AuthUser;
import cl.barberfinder.reservas.security.Requiere;
import cl.barberfinder.reservas.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService servicio;

    public ReservaController(ReservaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/disponibilidad")
    public DisponibilidadResponse disponibilidad(@RequestParam Long barberiaId,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return servicio.disponibilidad(barberiaId, fecha);
    }

    @Requiere(roles = {"CLIENTE", "ADMIN"})
    @PostMapping
    public ResponseEntity<ReservaResponse> crear(@RequestAttribute(AuthInterceptor.ATRIBUTO) AuthUser auth,
                                                 @Valid @RequestBody ReservaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(req, auth));
    }

    @Requiere
    @GetMapping("/mias")
    public List<ReservaResponse> mias(@RequestAttribute(AuthInterceptor.ATRIBUTO) AuthUser auth,
                                      @RequestParam(required = false) EstadoReserva estado) {
        return servicio.mias(auth, estado);
    }

    @Requiere(roles = {"ADMIN"})
    @GetMapping
    public List<ReservaResponse> todas() {
        return servicio.todas();
    }

    @Requiere(roles = {"ADMIN", "DUENO", "BARBERO"})
    @GetMapping("/barberia/{barberiaId}")
    public List<ReservaResponse> deBarberia(@RequestAttribute(AuthInterceptor.ATRIBUTO) AuthUser auth,
                                            @PathVariable Long barberiaId,
                                            @RequestParam(required = false)
                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return servicio.deBarberia(barberiaId, fecha, auth);
    }

    @Requiere
    @GetMapping("/{id}")
    public ReservaResponse obtener(@RequestAttribute(AuthInterceptor.ATRIBUTO) AuthUser auth,
                                   @PathVariable Long id) {
        return servicio.obtener(id, auth);
    }

    @Requiere(roles = {"CLIENTE", "ADMIN"})
    @PutMapping("/{id}")
    public ReservaResponse reprogramar(@RequestAttribute(AuthInterceptor.ATRIBUTO) AuthUser auth,
                                       @PathVariable Long id,
                                       @Valid @RequestBody ReprogramarRequest req) {
        return servicio.reprogramar(id, req, auth);
    }

    @Requiere
    @PatchMapping("/{id}/cancelar")
    public ReservaResponse cancelar(@RequestAttribute(AuthInterceptor.ATRIBUTO) AuthUser auth,
                                    @PathVariable Long id) {
        return servicio.cancelar(id, auth);
    }

    @Requiere(roles = {"ADMIN", "DUENO", "BARBERO"})
    @PatchMapping("/{id}/estado")
    public ReservaResponse cambiarEstado(@RequestAttribute(AuthInterceptor.ATRIBUTO) AuthUser auth,
                                         @PathVariable Long id,
                                         @Valid @RequestBody CambiarEstadoRequest req) {
        return servicio.cambiarEstado(id, req.estado(), auth);
    }

    @Requiere(roles = {"ADMIN"})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
