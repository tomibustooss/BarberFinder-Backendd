package cl.barberfinder.barberias.config;

import cl.barberfinder.barberias.model.Barberia;
import cl.barberfinder.barberias.model.Categoria;
import cl.barberfinder.barberias.repository.BarberiaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Carga las mismas 6 barberías ficticias que trae la app (SampleData) si la tabla está vacía.
 * Todas quedan a nombre del usuario dueño de prueba (SEED_DUENO_ID, por defecto 2).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final BarberiaRepository repositorio;
    private final boolean habilitado;
    private final Long duenoId;

    public DataSeeder(BarberiaRepository repositorio,
                      @Value("${app.seed.enabled:true}") boolean habilitado,
                      @Value("${app.seed.dueno-id:2}") Long duenoId) {
        this.repositorio = repositorio;
        this.habilitado = habilitado;
        this.duenoId = duenoId;
    }

    @Override
    public void run(String... args) {
        if (!habilitado || repositorio.count() > 0) {
            return;
        }
        crear("Barbería El Trébol", Categoria.ECONOMICA, "Calle Ejemplo 101 (dirección ficticia)",
                "Atención rápida y precios accesibles.", 6_000, 7_000, 3_000);
        crear("Corte Rápido Norte", Categoria.ECONOMICA, "Av. Demo 245 (dirección ficticia)",
                "Cortes sin reserva previa y buen precio.", 5_500, 6_500, 2_500);
        crear("Don Filo", Categoria.INTERMEDIA, "Pasaje Muestra 78 (dirección ficticia)",
                "Barbería de barrio con estilo clásico.", 11_000, 13_000, 5_000);
        crear("Navaja & Estilo", Categoria.INTERMEDIA, "Av. Prueba 560 (dirección ficticia)",
                "Especialistas en degradados y barba.", 12_000, 14_000, 6_000);
        crear("The Gentlemen's Room", Categoria.PREMIUM, "Calle Ficticia 900 (dirección ficticia)",
                "Experiencia premium con toalla caliente.", 20_000, 24_000, 9_000);
        crear("Royal Blade Studio", Categoria.PREMIUM, "Av. Imaginaria 1200 (dirección ficticia)",
                "Estudio de autor con atención personalizada.", 22_000, 26_000, 10_000);
        log.info("Barberías de prueba creadas (6) a nombre del dueño id={}", duenoId);
    }

    private void crear(String nombre, Categoria categoria, String direccion, String descripcion,
                       int clasico, int moderno, int barba) {
        Barberia b = new Barberia();
        b.setNombre(nombre);
        b.setCategoria(categoria);
        b.setDireccion(direccion);
        b.setComuna("Santiago");
        b.setDescripcion(descripcion);
        b.setPrecioCorteClasico(clasico);
        b.setPrecioCorteModerno(moderno);
        b.setPrecioBarba(barba);
        b.setDuenoId(duenoId);
        b.setActiva(true);
        repositorio.save(b);
    }
}
