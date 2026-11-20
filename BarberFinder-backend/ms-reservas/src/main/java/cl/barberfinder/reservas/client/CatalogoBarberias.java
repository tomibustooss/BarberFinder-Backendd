package cl.barberfinder.reservas.client;

import cl.barberfinder.reservas.model.TipoServicio;

/**
 * Lo que ms-reservas necesita saber de ms-barberias.
 * La implementación real llama por HTTP (BarberiasRestClient); en las pruebas se reemplaza por un mock.
 */
public interface CatalogoBarberias {

    record PrecioServicio(Long barberiaId, String barberiaNombre, Long duenoId, TipoServicio servicio, int total) {
    }

    record BarberiaInfo(Long id, String nombre, Long duenoId, boolean activa) {
    }

    PrecioServicio precio(Long barberiaId, TipoServicio servicio);

    BarberiaInfo barberia(Long barberiaId);
}
