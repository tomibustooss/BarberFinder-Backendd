package cl.barberfinder.barberias.controller;

import cl.barberfinder.barberias.dto.BarberiaDtos.BarberiaRequest;
import cl.barberfinder.barberias.dto.BarberiaDtos.BarberiaResponse;
import cl.barberfinder.barberias.dto.BarberiaDtos.PrecioResponse;
import cl.barberfinder.barberias.model.Categoria;
import cl.barberfinder.barberias.model.TipoServicio;
import cl.barberfinder.barberias.security.AuthInterceptor;
import cl.barberfinder.barberias.security.AuthUser;
import cl.barberfinder.barberias.security.Requiere;
import cl.barberfinder.barberias.service.BarberiaService;
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
@RequestMapping("/api/barberias")
public class BarberiaController {

    private final BarberiaService servicio;

    public BarberiaController(BarberiaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<BarberiaResponse> listar(@RequestParam(required = false) Categoria categoria,
                                         @RequestParam(required = false) String q) {
        return servicio.listar(categoria, q);
    }

    @Requiere(roles = {"DUENO", "ADMIN"})
    @GetMapping("/mias")
    public List<BarberiaResponse> mias(@RequestAttribute(AuthInterceptor.ATRIBUTO) AuthUser auth) {
        return servicio.listarDeDueno(auth.id());
    }

    @GetMapping("/{id}")
    public BarberiaResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @GetMapping("/{id}/servicios")
    public List<PrecioResponse> servicios(@PathVariable Long id) {
        return servicio.servicios(id);
    }

    @GetMapping("/{id}/precio")
    public PrecioResponse precio(@PathVariable Long id, @RequestParam TipoServicio servicio) {
        return this.servicio.precio(id, servicio);
    }

    @Requiere(roles = {"DUENO", "ADMIN"})
    @PostMapping
    public ResponseEntity<BarberiaResponse> crear(@RequestAttribute(AuthInterceptor.ATRIBUTO) AuthUser auth,
                                                  @Valid @RequestBody BarberiaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(req, auth));
    }

    @Requiere(roles = {"DUENO", "ADMIN"})
    @PutMapping("/{id}")
    public BarberiaResponse actualizar(@RequestAttribute(AuthInterceptor.ATRIBUTO) AuthUser auth,
                                       @PathVariable Long id,
                                       @Valid @RequestBody BarberiaRequest req) {
        return servicio.actualizar(id, req, auth);
    }

    @Requiere(roles = {"DUENO", "ADMIN"})
    @PatchMapping("/{id}/estado")
    public BarberiaResponse cambiarEstado(@RequestAttribute(AuthInterceptor.ATRIBUTO) AuthUser auth,
                                          @PathVariable Long id,
                                          @RequestParam boolean activa) {
        return servicio.cambiarEstado(id, activa, auth);
    }

    @Requiere(roles = {"DUENO", "ADMIN"})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@RequestAttribute(AuthInterceptor.ATRIBUTO) AuthUser auth,
                                         @PathVariable Long id) {
        servicio.eliminar(id, auth);
        return ResponseEntity.noContent().build();
    }
}
