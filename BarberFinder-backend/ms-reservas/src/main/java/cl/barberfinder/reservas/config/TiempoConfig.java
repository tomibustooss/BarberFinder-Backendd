package cl.barberfinder.reservas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TiempoConfig {

    /** Reloj con la zona horaria de Chile para validar fechas y horas pasadas. */
    @Bean
    public Clock reloj(@Value("${app.zona-horaria:America/Santiago}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
