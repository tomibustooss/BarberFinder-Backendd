package cl.barberfinder.usuarios.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class EstadoController {

    @GetMapping("/api/estado")
    public Map<String, String> estado() {
        return Map.of(
                "servicio", "ms-usuarios",
                "estado", "OK",
                "hora", LocalDateTime.now().toString()
        );
    }
}
